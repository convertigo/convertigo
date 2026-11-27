#!/usr/bin/env python3
"""Publish counts and verdicts from JMeter XML without copying its contents."""

import argparse
import json
import os
from pathlib import Path
import re
import subprocess
from xml.etree import ElementTree

REQUIRED_SUITES = ("qualif-jmx-1", "qualif-jmx-2", "qualif-jmx-3")
REDIS_SUITE = "qualif-jmx-4"
MOBILE_SUITE = "qualif-jmx-mobile"
SUITES = (*REQUIRED_SUITES, REDIS_SUITE, MOBILE_SUITE)


def count_samples(path):
    tests = failures = 0
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
            depth -= 1
            element.clear()
            if depth == 1:
                root.clear()
    if not tests:
        raise ValueError
    return tests, failures


def collect(suite, repo, status, output):
    plans = sorted((repo / "jmx").glob("*.jmx"))
    tests = failures = missing = 0
    # The Redis runner checks readiness of both nodes before running its plans.
    init_plans = [] if suite == REDIS_SUITE else ["A_init"]
    for name in [*init_plans, *(plan.stem for plan in plans)]:
        try:
            count, failed = count_samples(repo / "out" / "jtl" / f"{name}.jtl")
        except (OSError, ValueError, ElementTree.ParseError):
            missing += 1
        else:
            tests += count
            failures += failed
    if status or failures:
        verdict = "failed"
    elif missing or not plans:
        verdict = "incomplete"
    else:
        verdict = "passed"
    output.mkdir(parents=True, exist_ok=True)
    report = dict(tests=tests, failures=failures, missingPlans=missing, status=verdict)
    (output / f"{suite}.json").write_text(json.dumps(report) + "\n")
    warning = "Warning: " if verdict != "passed" and suite == REDIS_SUITE else ""
    print(f"{warning}{suite}: {verdict}")
    return verdict != "passed" and suite in REQUIRED_SUITES


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
            return dict(name=name, status=status, **counts)
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
    qualification = ("failed" if "failed" in (engine, suites)
                     else "passed" if engine == suites == "passed" else "incomplete")
    build = re.sub(r"[^a-zA-Z0-9._-]", "-", os.environ["CONVERTIGO_BUILD_NAME"])
    redis_label = "passed" if redis == "passed" else "warning"
    subject = f"Engine {engine} | Suites {suites} | Redis {redis_label} | Mobile {mobile} | {build}"[:100]
    lines = [subject, f"Qualification: {qualification} (engine and suites 1–3 required)", ""]
    lines.extend(f"{r['name']}: {r['status']} (samples={r['tests']}, failures={r['failures']}, "
                 f"missing plans={r['missingPlans']})" for r in reports)
    message = "\n".join(lines) + "\n"
    output.mkdir(parents=True, exist_ok=True)
    (output / "qualif-summary.txt").write_text(message)
    (output / "qualif-summary.json").write_text(json.dumps(dict(
        buildName=build, engineCheck=engine, suites=suites, redis=redis, mobile=mobile,
        status=qualification, totalFailures=sum(r["failures"] or 0 for r in reports),
        subject=subject, reports=reports), indent=2) + "\n")
    topic = os.environ.get("QUALIF_NOTIFY_SNS_TOPIC_ARN")
    if topic:
        if subprocess.run(["aws", "sns", "publish", "--topic-arn", topic,
                           "--subject", subject, "--message", message], check=False).returncode:
            print("Warning: qualification notification failed")
    print(subject)


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
    args = parser.parse_args()
    if args.command == "collect":
        return collect(args.suite, args.repo, args.status, args.output)
    summarize(args.source, args.output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
