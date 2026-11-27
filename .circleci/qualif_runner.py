#!/usr/bin/env python3
"""Run a qualification with a bounded wait and retain its private diagnostics."""

import argparse
import json
import os
from pathlib import Path
import re
import secrets
import signal
import subprocess
import tarfile
import tempfile
import threading

from qualif_report import INFRASTRUCTURE, LICENSE_MODES, MOBILE_SUITE, REDIS_SUITE, RUN_PHASES, SUITES, collect

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
        return runner.wait(timeout=deadline), None
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
           if not key.startswith('AWS_') and key not in {'QUALIF_GITHUB_TOKEN', 'QUALIF_OUT_DIR'}}
    if suite != REDIS_SUITE:
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


def save_archive(path, scratch, repo, status, evidence):
    (scratch / 'run.exit').write_text(f'{status if status is not None else "not_run"}\n')
    (scratch / 'diagnostics.json').write_text(json.dumps(evidence, indent=2) + '\n')
    with tarfile.open(path, 'w:gz') as archive:
        for name in ('run.log', 'run.exit', 'diagnostics.json'):
            archive.add(scratch / name, arcname=name)
        if status is not None and (repo / 'out').is_dir():
            archive.add(repo / 'out', arcname='out')


def qualification(suite, repo, work, deadline, grace):
    private = work / 'qualif-private'
    private.mkdir(parents=True, exist_ok=True)
    events = []
    status = None
    sha = next(iter(read_lines(repo / '.qualif-source-sha')), '')
    sha = sha if re.fullmatch(r'[0-9a-f]{40}', sha) else None
    env = runner_environment(suite)
    if not (repo / 'run.sh').is_file():
        problem = 'optional_source_unavailable' if suite == REDIS_SUITE and not repo.is_dir() else 'runner_missing'
    else:
        problem = prepare_plans(suite, repo, events)
        if suite == REDIS_SUITE and not env.get('C8O_IMAGE'):
            problem = 'image_unconfigured'

    with tempfile.TemporaryDirectory(dir=private) as folder:
        scratch = Path(folder)
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
        archive = private / f'{suite}.tar.gz'
        try:
            pending = scratch / 'diagnostics.tar.gz'
            save_archive(pending, scratch, repo, status, evidence)
            pending.replace(archive)
        except (OSError, tarfile.TarError):
            evidence['diagnostics'] = 'unavailable'
            print(f'Warning: could not archive {suite} diagnostics', flush=True)

    collect(suite, repo, status, work / 'qualif-reports', outcome=evidence)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('suite', choices=SUITES)
    parser.add_argument('repo', type=Path)
    parser.add_argument('work', type=Path)
    parser.add_argument('--deadline', type=float, default=35 * 60)
    parser.add_argument('--grace', type=float, default=120)
    parser.add_argument('--heartbeat', type=float, default=60)
    args = parser.parse_args()
    if min(args.deadline, args.grace, args.heartbeat) <= 0:
        parser.error('timings must be positive')
    os.umask(0o077)
    done = threading.Event()
    heartbeat = threading.Thread(target=keep_alive, args=(done, args.heartbeat), daemon=True)
    print('Qualification started', flush=True)
    heartbeat.start()
    try:
        qualification(args.suite, args.repo, args.work, args.deadline, args.grace)
    finally:
        done.set()
        heartbeat.join()


if __name__ == '__main__':
    main()
