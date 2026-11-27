#!/usr/bin/env python3
"""Publish counts and verdicts from JMeter XML without copying its contents."""

import argparse
import json
import os
from pathlib import Path
import re
import subprocess
from tempfile import NamedTemporaryFile
from xml.etree import ElementTree

from qualif_render import DIAGNOSTICS, INFRASTRUCTURE_LABELS, LICENSES, PHASES, ci_links, report_subject, write_reports

REQUIRED_SUITES = ("qualif-jmx-1", "qualif-jmx-2", "qualif-jmx-3")
REDIS_SUITE = "qualif-jmx-4"
MOBILE_SUITE = "qualif-jmx-mobile"
SUITES = (*REQUIRED_SUITES, REDIS_SUITE, MOBILE_SUITE)
RUN_PHASES = {
    "image_preparing", "compose_starting", "compose_started", "jmeter_started", "jmeter_exited",
}
INFRASTRUCTURE = set(INFRASTRUCTURE_LABELS)
LICENSE_MODES = set(LICENSES)
REPORT_FIELDS = {
    "license": LICENSE_MODES,
    "phase": PHASES,
    "infrastructure": INFRASTRUCTURE,
    "diagnostics": DIAGNOSTICS,
}


def count_samples(path, allow_partial):
    tests = failures = 0
    elapsed = 0
    has_timings = True
    try:
        with path.open("rb") as source:
            events = ElementTree.iterparse(source, events=("start", "end"))
            _, root = next(events)
            if root.tag != "testResults":
                raise ValueError
            depth = 1
            for event, element in events:
                if event == "start":
                    depth += 1
                    continue
                if element.tag in ("httpSample", "sample"):
                    result = element.get("s")
                    if result not in ("true", "false"):
                        raise ValueError
                    tests += 1
                    failures += result == "false"
                    duration = element.get("t", "")
                    if re.fullmatch(r"[0-9]{1,19}", duration):
                        elapsed += int(duration)
                    else:
                        has_timings = False
                depth -= 1
                element.clear()
                if depth == 1:
                    root.clear()
    except (OSError, ValueError, ElementTree.ParseError):
        if not allow_partial:
            return 0, 0, 0, False
        return tests, failures, elapsed if has_timings else None, False
    return tests, failures, elapsed if has_timings else None, bool(tests)


def collect(suite, repo, status, output, outcome=None):
    plans = (["mobileAppBuild"] if suite == MOBILE_SUITE
             else sorted(plan.stem for plan in (repo / "jmx").glob("*.jmx")))
    tests = failures = missing = 0
    total_time = 0
    # The Redis runner checks readiness of both nodes before running its plans.
    init_plans = [] if suite == REDIS_SUITE else ["A_init"]
    allow_partial = outcome is not None and outcome["phase"] == "deadline_exceeded"
    for name in [*init_plans, *plans]:
        if status is None:
            missing += 1
        else:
            count, failed, elapsed, complete = count_samples(repo / "out" / "jtl" / f"{name}.jtl", allow_partial)
            tests += count
            failures += failed
            missing += not complete
            total_time = total_time + elapsed if total_time is not None and elapsed is not None else None

    fields = {key: outcome.get(key) for key in REPORT_FIELDS} if outcome is not None else {}
    phase = fields.get("phase")
    if failures or (status and phase in (None, "jmeter_exited")):
        verdict = "failed"
    elif (missing or not plans or fields.get("license") == "evaluation"
          or fields.get("infrastructure") or phase not in (None, "jmeter_exited")):
        verdict = "incomplete"
    else:
        verdict = "passed"
    output.mkdir(parents=True, exist_ok=True)
    report = dict(tests=tests, failures=failures, missingPlans=missing, status=verdict, **fields)
    if tests and total_time is not None:
        report["totalTimeMs"] = total_time
    (output / f"{suite}.json").write_text(json.dumps(report) + "\n")
    warning = "Warning: " if verdict != "passed" or fields.get("diagnostics") in ("partial", "unavailable") else ""
    print(f"{warning}{suite}: {verdict}")


def read_suite(path, name):
    try:
        data = json.loads(path.read_text())
        counts = {key: data[key] for key in ("tests", "failures", "missingPlans")}
        status = data["status"]
        if (status in ("passed", "failed", "incomplete")
                and all(type(value) is int and value >= 0 for value in counts.values())
                and counts["failures"] <= counts["tests"]
                and (status != "passed" or counts["tests"] > 0
                     and not counts["failures"] and not counts["missingPlans"])):
            report = dict(name=name, status=status, **counts)
            elapsed = data.get("totalTimeMs")
            if counts["tests"] and type(elapsed) is int and elapsed >= 0:
                report["totalTimeMs"] = elapsed
            for field, allowed in REPORT_FIELDS.items():
                value = data.get(field)
                if isinstance(value, str) and value in allowed:
                    report[field] = value
            return report
    except (OSError, ValueError, TypeError, KeyError):
        pass
    return dict(name=name, status="unavailable", tests=None, failures=None, missingPlans=None)


def summarize(source, output):
    try:
        exit_code = (source / "qualif-engine-check.exit").read_text().strip()
    except OSError:
        exit_code = ""
    engine = "incomplete"
    if re.fullmatch(r"[0-9]{1,3}", exit_code):
        engine = "failed" if int(exit_code) else "passed"
    reports = [read_suite(source / "qualif-reports" / f"{name}.json", name) for name in SUITES]
    required = {report["status"] for report in reports if report["name"] in REQUIRED_SUITES}
    suites = "failed" if "failed" in required else "passed" if required == {"passed"} else "incomplete"
    mobile = reports[-1]["status"]
    redis = next(report["status"] for report in reports if report["name"] == REDIS_SUITE)
    qualification = engine
    if engine == "passed" and suites != "passed":
        qualification = "warning"
    data = dict(
        buildName=os.environ["CONVERTIGO_BUILD_NAME"], engineCheck=engine, suites=suites, redis=redis, mobile=mobile,
        status=qualification, totalFailures=sum(r["failures"] or 0 for r in reports),
        reports=reports, **ci_links(os.environ.get("CIRCLE_WORKFLOW_ID"), os.environ.get("CIRCLE_SHA1")),
    )
    data["subject"] = report_subject(data)
    write_reports(data, output, published=True)
    (output / "qualif-summary.json").write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print(data["subject"])


def aws_succeeded(*args):
    try:
        return subprocess.run(["aws", *args], check=False, timeout=60,
                              stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode == 0
    except (OSError, subprocess.TimeoutExpired):
        return False


def notify(output):
    subject = json.loads((output / "qualif-summary.json").read_text(encoding="utf-8"))["subject"]
    text_file = output / "qualif-summary.txt"
    sender = os.environ.get("QUALIF_NOTIFY_SES_FROM", "").strip()
    recipients = [address.strip() for address in os.environ.get("QUALIF_NOTIFY_SES_TO", "").split(",")
                  if address.strip()]
    if sender and recipients:
        request = {
            "Source": sender,
            "Destination": {"ToAddresses": recipients},
            "Message": {
                "Subject": {"Data": subject, "Charset": "UTF-8"},
                "Body": {
                    "Text": {"Data": text_file.read_text(encoding="utf-8"), "Charset": "UTF-8"},
                    "Html": {"Data": (output / "qualif-summary.html").read_text(encoding="utf-8"), "Charset": "UTF-8"},
                },
            },
        }
        with NamedTemporaryFile(mode="w", encoding="utf-8") as source:
            json.dump(request, source)
            source.flush()
            command = ["ses", "send-email", "--cli-input-json", f"file://{source.name}"]
            if region := os.environ.get("QUALIF_NOTIFY_SES_REGION", "").strip():
                command += ["--region", region]
            if aws_succeeded(*command):
                return
        print("Warning: qualification SES notification failed")
    elif sender or recipients:
        print("Warning: qualification SES notification needs both a sender and recipients")

    topic = os.environ.get("QUALIF_NOTIFY_SNS_TOPIC_ARN")
    if topic and not aws_succeeded("sns", "publish", "--topic-arn", topic,
                                   "--subject", subject.encode("utf-8")[:99].decode("utf-8", errors="ignore"),
                                   "--message", f"file://{text_file}"):
        print("Warning: qualification SNS notification failed")


def main():
    parser = argparse.ArgumentParser()
    commands = parser.add_subparsers(dest="command", required=True)
    collect_parser = commands.add_parser("collect")
    collect_parser.add_argument("suite", choices=SUITES)
    collect_parser.add_argument("repo", type=Path)
    collect_parser.add_argument("status", type=int)
    collect_parser.add_argument("output", type=Path)
    summary_parser = commands.add_parser("summary")
    summary_parser.add_argument("source", type=Path)
    summary_parser.add_argument("output", type=Path)
    notify_parser = commands.add_parser("notify")
    notify_parser.add_argument("output", type=Path)
    args = parser.parse_args()
    if args.command == "collect":
        collect(args.suite, args.repo, args.status, args.output)
    elif args.command == "summary":
        summarize(args.source, args.output)
    else:
        notify(args.output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
