#!/usr/bin/env python3
"""Run a qualification with a bounded wait and retain its private diagnostics."""

import argparse
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import signal
import subprocess
import tempfile
import threading
from concurrent.futures import ThreadPoolExecutor
from time import monotonic

from qualif_report import INFRASTRUCTURE, LICENSE_MODES, MOBILE_SUITE, REDIS_SUITE, RUN_PHASES, SUITES, collect
from qualif_samples import redact, secret_values

RUN_EVENTS = RUN_PHASES | INFRASTRUCTURE | {'collecting', 'collected', 'diagnostics_partial'}


def keep_alive(done, interval):
    while not done.wait(interval):
        print('Qualification in progress', flush=True)


def send_signal(process, sig):
    try:
        os.killpg(process.pid, sig)
    except ProcessLookupError:
        pass


def run(repo, env, log, deadline, grace):
    try:
        runner = subprocess.Popen(
            ['sh', 'run.sh'], cwd=repo, env=env, start_new_session=True,
            stdout=log, stderr=subprocess.STDOUT,
        )
    except OSError:
        return None, 'runner_launch_failed'
    try:
        end = monotonic() + deadline
        if marker := env.get('QUALIF_MOBILE_CHECK'):
            # The final status request and cleanup get a bounded wait after Redis.
            while not Path(marker).is_file() and monotonic() < end:
                try:
                    return runner.wait(timeout=min(1, max(0, end - monotonic()))), None
                except subprocess.TimeoutExpired:
                    pass
            end = min(end, monotonic() + 90)
        return runner.wait(timeout=max(0, end - monotonic())), None
    except subprocess.TimeoutExpired:
        send_signal(runner, signal.SIGTERM)
        try:
            runner.wait(timeout=grace)
        except subprocess.TimeoutExpired:
            send_signal(runner, signal.SIGKILL)
            runner.wait()
        return runner.returncode, 'deadline_exceeded'


def runner_environment(suite):
    env = {key: value for key, value in os.environ.items()
           if not key.startswith('AWS_') and key not in {'QUALIF_GITHUB_TOKEN', 'QUALIF_OUT_DIR',
                           'QUALIF_MOBILE_STARTED', 'QUALIF_MOBILE_CHECK', 'QUALIF_MOBILE_IMAGE'}}
    if suite == REDIS_SUITE:
        for name in ('QUALIF_ADMIN_USER', 'QUALIF_ADMIN_PASSWORD', 'QUALIF_LAB_KEYS_XML'):
            env.pop(name, None)
    else:
        env['QUALIF_ADMIN_USER'] = env.get('QUALIF_ADMIN_USER') or 'admin'
        env['QUALIF_ADMIN_PASSWORD'] = env.get('QUALIF_ADMIN_PASSWORD') or secrets.token_hex(24)
        env['QUALIF_LAB_KEYS_XML'] = env.get('QUALIF_LAB_KEYS_XML') or '<keys/>'
    return env


def prepare_plans(suite, repo, events):
    if suite == MOBILE_SUITE:
        if not (repo / 'jmx' / 'mobileAppBuild.jmx').is_file():
            return 'mobile_plan_missing'
        events.append('mobile_plan_present')
        for plan in (repo / 'jmx').glob('*.jmx'):
            if plan.name != 'mobileAppBuild.jmx':
                plan.unlink()
    elif suite == 'qualif-jmx-1':
        for name in ('mobileAppBuild.jmx', 'ibm3270_ssl.jmx'):
            (repo / 'jmx' / name).unlink(missing_ok=True)
    return None


def read_lines(path):
    try:
        return path.read_text(errors='replace').splitlines()
    except OSError:
        return []


def save_diagnostics(scratch, repo, status, evidence, secrets):
    log = scratch / 'run.log'
    log.write_bytes(redact(log.read_bytes(), secrets))
    (scratch / 'run.exit').write_text(f'{status if status is not None else "not_run"}\n')
    (scratch / 'diagnostics.json').write_text(json.dumps(evidence, indent=2) + '\n')
    if status is None:
        return
    out = repo / 'out'
    for path in out.rglob('*'):
        if not path.is_file() or path.is_symlink():
            continue
        if path.suffix not in {'.log', '.html', '.jtl', '.txt'} and path.name not in {
                'runner-events', 'mobile-events', 'license-mode'}:
            continue
        destination = scratch / 'out' / path.relative_to(out)
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(redact(path.read_bytes(), secrets))


def qualification(suite, repo, work, deadline, grace, overrides=None):
    private = work / 'qualif-private'
    private.mkdir(parents=True, exist_ok=True)
    staged = private / suite
    if staged.is_dir():
        shutil.rmtree(staged)
    events = []
    status = None
    sha = next(iter(read_lines(repo / '.qualif-source-sha')), '')
    sha = sha if re.fullmatch(r'[0-9a-f]{40}', sha) else None
    env = runner_environment(suite)
    env.update(overrides or {})
    if not (repo / 'run.sh').is_file():
        problem = 'optional_source_unavailable' if suite == REDIS_SUITE and not repo.is_dir() else 'runner_missing'
    else:
        problem = prepare_plans(suite, repo, events)
        if suite == REDIS_SUITE and not env.get('C8O_IMAGE'):
            problem = 'image_unconfigured'

    scratch = Path(tempfile.mkdtemp(prefix=f'.{suite}-', dir=private))
    try:
        with (scratch / 'run.log').open('wb') as log:
            if problem is None:
                events.append('preconditions_passed')
                status, problem = run(repo, env, log, deadline, grace)

        out = repo / 'out'
        run_events = [event for event in read_lines(out / 'runner-events') if event in RUN_EVENTS] if status is not None else []
        infrastructure = next((event for event in run_events if event in INFRASTRUCTURE), None)
        if problem == 'runner_launch_failed':
            infrastructure = problem
        phase = problem or next((event for event in reversed(run_events) if event in RUN_PHASES), 'runner_unreported')
        mode = next(iter(read_lines(out / 'license-mode')), '') if status is not None else ''
        license_mode = mode if suite != REDIS_SUITE and mode in LICENSE_MODES else 'indeterminate'
        complete = 'collected' in run_events and 'diagnostics_partial' not in run_events
        evidence = dict(events=events + run_events, phase=phase, infrastructure=infrastructure,
                        sourceSha=sha, license=license_mode, diagnostics='complete' if complete else 'partial')
        try:
            save_diagnostics(scratch, repo, status, evidence, secret_values(os.environ))
            scratch.rename(staged)
        except OSError:
            evidence['diagnostics'] = 'unavailable'
            print(f'Warning: could not stage {suite} diagnostics', flush=True)
    finally:
        # A staged directory is renamed into place only after all copies are safe.
        if scratch.exists():
            shutil.rmtree(scratch, ignore_errors=True)

    collect(suite, repo, status, work / 'qualif-reports', outcome=evidence)


def run_combined(redis_repo, mobile_repo, work, deadline, grace, mobile_image):
    with tempfile.TemporaryDirectory(prefix='qualif-mobile-', dir=work) as folder:
        started = Path(folder) / 'launched'
        check = Path(folder) / 'check'
        overrides = {
            'QUALIF_MOBILE_STARTED': str(started),
            'QUALIF_MOBILE_CHECK': str(check),
            'QUALIF_MOBILE_IMAGE': mobile_image,
        }
        handoff_window = min(deadline, 120)
        mobile_deadline = handoff_window + deadline + grace + 90
        with ThreadPoolExecutor(max_workers=1) as pool:
            mobile = pool.submit(qualification, MOBILE_SUITE, mobile_repo, work, mobile_deadline, grace, overrides)
            done = threading.Event()
            mobile.add_done_callback(lambda _: done.set())
            # Let Redis proceed even when the mobile stack cannot start.
            handoff = monotonic() + handoff_window
            while not started.is_file() and monotonic() < handoff and not done.wait(0.25):
                pass
            try:
                qualification(REDIS_SUITE, redis_repo, work, deadline, grace)
            finally:
                check.touch()
            mobile.result()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('suite', choices=SUITES)
    parser.add_argument('repo', type=Path)
    parser.add_argument('work', type=Path)
    parser.add_argument('--deadline', type=float, default=35 * 60)
    parser.add_argument('--grace', type=float, default=120)
    parser.add_argument('--mobile-repo', type=Path)
    parser.add_argument('--mobile-image')
    parser.add_argument('--heartbeat', type=float, default=60)
    args = parser.parse_args()
    if min(args.deadline, args.grace, args.heartbeat) <= 0:
        parser.error('timings must be positive')
    if args.mobile_repo is not None and (args.suite != REDIS_SUITE or not args.mobile_image):
        parser.error('the combined run needs the Redis suite and the mobile image')
    os.umask(0o077)
    done = threading.Event()
    heartbeat = threading.Thread(target=keep_alive, args=(done, args.heartbeat), daemon=True)
    print('Qualification started', flush=True)
    heartbeat.start()
    try:
        if args.mobile_repo is not None:
            run_combined(args.repo, args.mobile_repo, args.work, args.deadline, args.grace, args.mobile_image)
        else:
            qualification(args.suite, args.repo, args.work, args.deadline, args.grace)
    finally:
        done.set()
        heartbeat.join()


if __name__ == '__main__':
    main()
