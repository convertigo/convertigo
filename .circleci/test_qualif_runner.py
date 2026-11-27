import json
import os
from pathlib import Path
import subprocess
import tempfile
import textwrap
import unittest

ROOT = Path(__file__).resolve().parent.parent
SHA = 'a' * 40
TOKEN = 'synthetic-github-value'
KEY_XML = '<keys><key>synthetic-license-value</key></keys>'
JTL = b'<testResults><httpSample s="false"><responseData>synthetic-response</responseData></httpSample></testResults>\n'


class WrapperTest(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.work = Path(directory.name)
        self.repo = self.work / 'sources'
        (self.repo / 'jmx').mkdir(parents=True)
        (self.repo / 'jmx' / 'ordinary.jmx').touch()
        (self.repo / '.qualif-source-sha').write_text(SHA + '\n')
        self.env = {
            'PATH': os.environ['PATH'],
            'PYTHONDONTWRITEBYTECODE': '1',
            'QUALIF_ADMIN_USER': 'synthetic-user',
            'QUALIF_ADMIN_PASSWORD': 'synthetic-password',
            'QUALIF_GITHUB_TOKEN': TOKEN,
            'QUALIF_LAB_KEYS_XML': KEY_XML,
        }

    def run_wrapper(self, suite, shell=None, *timings):
        if shell is not None:
            (self.repo / 'run.sh').write_text(textwrap.dedent(shell))
        result = subprocess.run([
            '/bin/bash', '-x', '.circleci/run_qualif.sh', suite, str(self.repo), str(self.work), *timings,
        ], cwd=ROOT, env=self.env, text=True, capture_output=True, check=True, timeout=8)
        self.assertNotIn(TOKEN, result.stdout + result.stderr)
        self.assertNotIn(KEY_XML, result.stdout + result.stderr)
        report = json.loads((self.work / 'qualif-reports' / f'{suite}.json').read_text())
        folder = self.work / 'qualif-private' / suite
        files = {str(path.relative_to(folder)): path.read_bytes() for path in folder.rglob('*') if path.is_file()}
        contents = b''.join(files.values())
        self.assertNotIn(TOKEN.encode(), contents)
        self.assertNotIn(b'synthetic-license-value', contents)
        return result, report, files

    def test_jmeter_result_and_separate_private_files(self):
        result, report, files = self.run_wrapper('qualif-jmx-2', f"""\
            test -z "${{QUALIF_GITHUB_TOKEN-}}" || exit 99
            mkdir -p out/jtl
            printf '%s\\n' compose_started jmeter_started jmeter_exited collected > out/runner-events
            printf '%s' evaluation > out/license-mode
            printf '%s\\n' "$QUALIF_ADMIN_USER" "$QUALIF_ADMIN_PASSWORD" "$QUALIF_LAB_KEYS_XML"
            printf '%s\\n' '&lt;keys&gt;&lt;key&gt;synthetic-license-value&lt;/key&gt;&lt;/keys&gt;' '<keys/>' 'Log after license'
            printf '%s\\n' "$QUALIF_ADMIN_PASSWORD" 'License value: synthetic-license-value' > out/engine.log
            printf '%s\\n' '<html>synthetic report</html>' > out/result.html
            printf '%s\\n' '{JTL.decode().rstrip()}' > out/jtl/ordinary.jtl
            exit 7
            """)
        self.assertNotIn('synthetic-response', result.stdout + result.stderr)
        self.assertEqual(report['tests'], 1)
        self.assertEqual(report['failures'], 1)
        self.assertEqual(report['status'], 'failed')
        self.assertEqual(report['license'], 'evaluation')
        self.assertEqual(report['phase'], 'jmeter_exited')
        self.assertIsNone(report['infrastructure'])
        self.assertEqual(files['run.exit'], b'7\n')
        self.assertIn(b'[redacted license]\nLog after license', files['run.log'])
        self.assertIn(b'synthetic-password', files['run.log'])
        self.assertEqual(files['out/engine.log'], b'synthetic-password\nLicense value: [redacted]\n')
        self.assertEqual(files['out/result.html'], b'<html>synthetic report</html>\n')
        self.assertEqual(files['out/jtl/ordinary.jtl'], JTL)

    def test_deadline_keeps_heartbeat_and_partial_jtl(self):
        result, report, files = self.run_wrapper('qualif-jmx-2', """\
            mkdir -p out/jtl
            printf '%s\\n' compose_started jmeter_started > out/runner-events
            trap 'printf "%s\\n" collecting diagnostics_partial collected >> out/runner-events; exit 124' TERM
            printf '%s' '<testResults><httpSample s="true"/>' > out/jtl/ordinary.jtl
            sleep 20
            """, '--deadline', '0.5', '--grace', '1', '--heartbeat', '0.05')
        self.assertIn('Qualification in progress', result.stdout)
        self.assertEqual(report['phase'], 'deadline_exceeded')
        self.assertEqual(report['diagnostics'], 'partial')
        self.assertIsNone(report['infrastructure'])
        self.assertEqual(report['tests'], 1)
        self.assertEqual(report['missingPlans'], 2)
        self.assertEqual(report['status'], 'incomplete')
        self.assertEqual(files['run.exit'], b'124\n')

    def test_grace_expiry_does_not_block_collection(self):
        _, report, files = self.run_wrapper('qualif-jmx-2', """\
            mkdir -p out
            printf '%s\\n' compose_starting > out/runner-events
            trap '' TERM
            sleep 20
            """, '--deadline', '0.3', '--grace', '0.2')
        self.assertEqual(report['phase'], 'deadline_exceeded')
        self.assertEqual(report['diagnostics'], 'partial')
        self.assertEqual(files['run.exit'], b'-9\n')

    def test_mobile_compose_failure_preserves_sha_and_stage(self):
        (self.repo / 'jmx' / 'mobileAppBuild.jmx').touch()
        _, report, files = self.run_wrapper('qualif-jmx-mobile', """\
            test ! -f jmx/ordinary.jmx || exit 99
            mkdir -p out
            printf '%s\\n' compose_starting compose_up_failed diagnostics_partial collected > out/runner-events
            echo 'synthetic compose message'
            exit 1
            """)
        self.assertEqual(report['infrastructure'], 'compose_up_failed')
        self.assertEqual(report['phase'], 'compose_starting')
        self.assertEqual(report['status'], 'incomplete')
        self.assertEqual(report['missingPlans'], 2)
        evidence = json.loads(files['diagnostics.json'])
        self.assertEqual(evidence['sourceSha'], SHA)
        self.assertIn('mobile_plan_present', evidence['events'])
        self.assertNotIn('jmeter_started', evidence['events'])
        self.assertIn(b'synthetic compose message', files['run.log'])

    def test_missing_mobile_plan_does_not_start_runner(self):
        _, report, files = self.run_wrapper('qualif-jmx-mobile', 'echo "unexpected runner"\n')
        self.assertEqual(report['phase'], 'mobile_plan_missing')
        self.assertEqual(report['missingPlans'], 2)
        self.assertEqual(files['run.exit'], b'not_run\n')
        self.assertEqual(files['run.log'], b'')

    def test_optional_redis_checkout_remains_nonblocking(self):
        self.repo = self.work / 'unavailable-checkout'
        _, report, files = self.run_wrapper('qualif-jmx-4')
        self.assertEqual(report['phase'], 'optional_source_unavailable')
        self.assertEqual(report['license'], 'indeterminate')
        self.assertIsNone(report['infrastructure'])
        self.assertEqual(files['run.exit'], b'not_run\n')

    def test_combined_runner_releases_final_check_after_redis_and_keeps_both_verdicts(self):
        mobile = self.work / 'mobile'
        (mobile / 'jmx').mkdir(parents=True)
        (mobile / 'jmx' / 'mobileAppBuild.jmx').touch()
        (mobile / 'run.sh').write_text(textwrap.dedent("""\
            set -eu
            : > "$QUALIF_MOBILE_STARTED"
            while [ ! -f "$QUALIF_MOBILE_CHECK" ]; do sleep 0.05; done
            test -f ../sources/redis-finished
            mkdir -p out/jtl
            printf '%s\\n' jmeter_exited collected > out/runner-events
            printf '%s' '<testResults><sample s="false"/></testResults>' > out/jtl/mobileAppBuild.jtl
            printf '%s' '<testResults><sample s="true"/></testResults>' > out/jtl/A_init.jtl
            exit 1
            """))
        self.env['C8O_IMAGE'] = 'standard-image'
        _, redis, _ = self.run_wrapper('qualif-jmx-4', """\
            set -eu
            test -f ../qualif-mobile-*/launched
            test -z "${QUALIF_LAB_KEYS_XML:-}"
            test ! -e "$(dirname ../qualif-mobile-*/launched)/check"
            mkdir -p out/jtl
            printf '%s\\n' jmeter_exited collected > out/runner-events
            printf '%s' '<testResults><sample s="true"/></testResults>' > out/jtl/ordinary.jtl
            : > redis-finished
            """, '--mobile-repo', str(mobile), '--mobile-image', 'aks-image',
            '--deadline', '4', '--grace', '0.5')
        report = json.loads((self.work / 'qualif-reports' / 'qualif-jmx-mobile.json').read_text())
        self.assertEqual((redis['status'], redis['tests']), ('passed', 1))
        self.assertEqual((report['status'], report['tests'], report['failures']), ('failed', 2, 1))

    def test_nonempty_license_request_without_observation_is_indeterminate(self):
        _, report, _ = self.run_wrapper('qualif-jmx-2', """\
            mkdir -p out/jtl
            printf '%s\\n' jmeter_exited collected > out/runner-events
            printf '%s' '<testResults><httpSample s="true"/></testResults>' > out/jtl/A_init.jtl
            cp out/jtl/A_init.jtl out/jtl/ordinary.jtl
            """)
        self.assertEqual(report['license'], 'indeterminate')
        self.assertEqual(report['status'], 'passed')


class CheckoutTest(unittest.TestCase):
    def test_checkout_guard_and_askpass_do_not_trace_github_token(self):
        config = (ROOT / '.circleci/config.yml').read_text()
        block = config.split('        name: Checkout the qualification suite\n        command: |\n', 1)[1]
        block = block.split('\n\n  run-qualif:', 1)[0]
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            bin_dir = root / 'bin'
            bin_dir.mkdir()
            git = bin_dir / 'git'
            git.write_text(f"""#!/bin/sh
if [ "$1" = -C ]; then
    echo {SHA}
else
    answer=$(sh -x "$GIT_ASKPASS")
    [ "$answer" = "$QUALIF_GITHUB_TOKEN" ] || exit 1
    mkdir -p convertigo-qualif-jmx-1/.git
fi
""")
            git.chmod(0o700)
            replacements = {
                '/tmp/qualification-sources': str(root / 'sources'),
                '<< parameters.suite >>': '1',
                '<< parameters.branch >>': '8.4.x',
                '<< parameters.optional >>': 'false',
            }
            for old, new in replacements.items():
                block = block.replace(old, new)
            result = subprocess.run(
                ['/bin/bash', '-xe', '-c', textwrap.dedent(block)],
                env={'PATH': f"{bin_dir}:{os.environ['PATH']}", 'QUALIF_GITHUB_TOKEN': TOKEN},
                text=True, capture_output=True, check=True, timeout=5,
            )
            self.assertNotIn(TOKEN, result.stdout + result.stderr)
            repo = root / 'sources' / 'convertigo-qualif-jmx-1'
            self.assertEqual((repo / '.qualif-source-sha').read_text().strip(), SHA)
            self.assertFalse((repo / '.git').exists())


class UploadTest(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.work = Path(directory.name)
        self.bin = self.work / 'bin'
        self.bin.mkdir()
        private = self.work / 'qualif-private'
        private.mkdir()
        (private / 'qualif-jmx-2').mkdir()
        (private / 'qualif-jmx-2' / 'run.log').touch()
        self.fake('mkdir', 'exec /bin/mkdir "$@"\n')
        self.fake('find', 'exec /usr/bin/find "$@"\n')

    def fake(self, name, code):
        path = self.bin / name
        path.write_text('#!/bin/sh\n' + code)
        path.chmod(0o700)

    def upload(self):
        return subprocess.run([
            '/bin/bash', '-x', '.circleci/upload_qualif.sh', 'qualif-jmx-2', '123-hotfix', str(self.work),
        ], cwd=ROOT, env={'PATH': str(self.bin), 'AWS_TEST_ARGS': str(self.work / 'arguments')},
            text=True, capture_output=True, check=True, timeout=5)

    def test_aws_installation_failure_warns_without_failing(self):
        self.fake('mktemp', f'exec /usr/bin/mktemp -d "{self.work}/aws.XXXXXX"\n')
        self.fake('rm', 'exec /bin/rm "$@"\n')
        self.fake('uname', 'echo x86_64\n')
        self.fake('curl', 'echo "synthetic download failure" >&2\nexit 1\n')
        result = self.upload()
        self.assertIn('could not install AWS CLI', result.stdout)
        self.assertNotIn('synthetic download failure', result.stderr)
        self.assertEqual((self.work / 'qualif-uploads/qualif-jmx-2.files').read_bytes(), b'')

    def test_aws_upload_failure_keeps_private_acl(self):
        self.fake('timeout', 'shift\nexec "$@"\n')
        self.fake('aws', 'printf "%s\\n" "$@" "END" >> "$AWS_TEST_ARGS"\nexit 1\n')
        result = self.upload()
        self.assertIn('could not upload', result.stdout)
        html, plain, end = (self.work / 'arguments').read_text().split('END\n')
        self.assertEqual(end, '')
        for args, mime in ((html, 'text/html'), (plain, 'text/plain')):
            flags = args.splitlines()
            self.assertEqual(flags[:5], ['s3', 'cp', '--only-show-errors', '--acl', 'private'])
            self.assertIn('--no-follow-symlinks', flags)
            self.assertIn(f'{mime}; charset=utf-8', flags)
            self.assertEqual(flags[-1], 's3://convertigo-ci/convertigo-123-hotfix/qualif/private/qualif-jmx-2/')
        self.assertIn('--include\n*.html', html)
        self.assertIn('--exclude\n*.html', plain)

    def test_upload_records_only_files_in_successful_batches(self):
        folder = self.work / 'qualif-private/qualif-jmx-2'
        (folder / 'out').mkdir()
        (folder / 'out/another test.jtl').touch()
        (folder / 'out/report.html').touch()
        (folder / 'other.log').symlink_to(folder / 'run.log')
        self.fake('timeout', 'shift\nexec "$@"\n')
        self.fake('aws', 'case "$*" in *text/html*) exit 1;; esac\n')
        result = self.upload()
        self.assertIn('could not upload qualif-jmx-2 private HTML diagnostics', result.stdout)
        manifest = (self.work / 'qualif-uploads/qualif-jmx-2.files').read_bytes()
        self.assertEqual(set(manifest.split(b'\x00')), {b'./run.log', b'./out/another test.jtl', b''})


if __name__ == '__main__':
    unittest.main()
