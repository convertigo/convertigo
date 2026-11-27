"""Render the public qualification data as email-ready HTML and plain text."""

from html import escape
import re
from urllib.parse import quote

S3 = "https://convertigo-ci.s3-eu-west-3.amazonaws.com"
STATUSES = {
    "passed": "Passed",
    "failed": "Failed",
    "warning": "Warning",
    "incomplete": "Incomplete",
    "unavailable": "Unavailable",
}
CHECKS = {"engineCheck": "Engine", "suites": "Suites 1–3", "redis": "Redis", "mobile": "Mobile"}
BANNERS = {
    "passed": ("#13523C", "#E8F6EE"),
    "warning": ("#603C00", "#FFF1D2"),
    "failed": ("#8D2433", "#FDECEE"),
    "incomplete": ("#42377D", "#F0EDFF"),
}
WARNING_ROW_COLORS = ("#FFF4DF", "#A56411", "#3C3020", "#F2B65F")
# Light background and accent, then dark background and accent.
ROW_COLORS = {
    "passed": ("#EAF6EF", "#2D8054", "#20372D", "#6BCC91"),
    "warning": WARNING_ROW_COLORS,
    "incomplete": WARNING_ROW_COLORS,
    "unavailable": WARNING_ROW_COLORS,
    "failed": ("#FCEEF0", "#B74656", "#43272F", "#F18593"),
}
LICENSES = {"licensed": "Active", "evaluation": "Evaluation", "indeterminate": "Undetermined"}
PHASES = {
    "image_preparing": "Preparing image",
    "compose_starting": "Starting services",
    "compose_started": "Services started",
    "jmeter_started": "JMeter started",
    "jmeter_exited": "JMeter finished",
    "image_unconfigured": "Image not configured",
    "mobile_plan_missing": "Mobile plan missing",
    "optional_source_unavailable": "Suite sources unavailable",
    "runner_missing": "Runner missing",
    "deadline_exceeded": "Time limit reached",
    "runner_unreported": "Runner phase unavailable",
    "runner_launch_failed": "Runner could not start",
}
INFRASTRUCTURE_LABELS = {
    "docker_unreachable": "Docker unavailable",
    "image_pull_failed": "Image pull failed",
    "image_lookup_failed": "Image lookup failed",
    "engine_lookup_failed": "Engine container lookup failed",
    "compose_up_failed": "Services could not start",
    "jmeter_lookup_failed": "JMeter container lookup failed",
    "jmeter_not_started": "JMeter did not start",
    "jmeter_wait_failed": "Could not wait for JMeter",
    "cleanup_failed": "Cleanup failed",
    "runner_launch_failed": "Runner could not start",
}
DIAGNOSTICS = {"complete": "Complete", "partial": "Partial", "unavailable": "Unavailable"}
PRIVATE_NOTE = "Private diagnostics (when available): S3 access required."
MISSING = "—"
LINK_STYLE = "color:#1554AD;text-decoration:underline;"
DETAIL_STYLE = "display:block;line-height:1.5;"
CELL_STYLE = "padding:10px 8px;border-bottom:1px solid #D9E1EC;vertical-align:top;text-align:left;overflow-wrap:anywhere;"
COLUMN_HEADINGS = ("Suite", "Results", "Run details")


def ci_links(workflow, sha):
    links = {}
    if workflow and re.fullmatch(r"[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}", workflow):
        links["workflowUrl"] = f"https://app.circleci.com/pipelines/workflows/{workflow}"
    if sha and re.fullmatch(r"[a-f0-9]{40}", sha):
        links["commitUrl"] = f"https://github.com/convertigo/convertigo/commit/{sha}"
    return links


def display_status(data):
    if "status" not in data:
        return "warning" if data["totalFailures"] or data.get("summaryErrors") else "passed"
    status = data["status"]
    if status == "passed" and any(data.get(check, "passed") != "passed" for check in ("redis", "mobile")):
        return "warning"
    return status


def report_subject(data):
    return f"Convertigo qualification {display_status(data)} | {data['buildName']}"


def report_links(data):
    prefix = f"convertigo-{quote(data['buildName'], safe='/')}/"
    return [
        ("Build files (S3)", f"{S3}/index.html?prefix={prefix}"),
        ("Qualification files (S3)", f"{S3}/index.html?prefix={prefix}qualif/"),
        ("CircleCI workflow", data.get("workflowUrl")),
        ("GitHub commit", data.get("commitUrl")),
    ]


def published_suite_url(data, suite):
    build = quote(data["buildName"], safe="/")
    name = quote(suite["name"], safe="")
    url = f"{S3}/convertigo-{build}/qualif/{name}_result.html"
    return url if suite.get("reportUrl") == url else None


def suite_status(suite):
    status = suite.get("status")
    if status is None:
        status = "failed" if int(suite.get("failures") or 0) else "passed" if suite.get("tests") else "unavailable"
    return status


def suite_results(suite):
    tests, failures = suite.get("tests"), suite.get("failures")
    rate = suite.get("successRate")
    average, total = suite.get("averageTime"), suite.get("totalTime")
    if type(tests) is int and tests > 0:
        rate = f"{(tests - failures) / tests:.2%}"
        elapsed = suite.get("totalTimeMs")
        if elapsed is not None:
            average_ms = (elapsed + tests // 2) // tests
            average, total = f"{average_ms:,} ms", f"{elapsed:,} ms"
    return [
        ("Tests", tests), ("Failures", failures), ("Success rate", rate),
        ("Average time", average), ("Total time", total),
    ]


def suite_details(suite):
    details = [
        ("Missing plans", suite.get("missingPlans")),
        ("License", LICENSES.get(suite.get("license"))),
        ("Phase", PHASES.get(suite.get("phase"))),
        ("Diagnostics", DIAGNOSTICS.get(suite.get("diagnostics"))),
    ]
    if infrastructure := INFRASTRUCTURE_LABELS.get(suite.get("infrastructure")):
        details.append(("Infrastructure", infrastructure))
    return details


def display_value(value):
    return MISSING if value is None or value == "" else str(value)


def checks_label(data):
    checks = []
    for key, name in CHECKS.items():
        if key in data:
            status = data[key]
            if key != "engineCheck" and status != "passed":
                status = "warning"
            checks.append(f"{name} {STATUSES[status].lower()}")
    return " · ".join(checks)


def render_text(data, published):
    lines = [report_subject(data)]
    if checks := checks_label(data):
        lines.append(checks)
    lines.extend([f"Recorded test failures: {data['totalFailures']}", ""])
    if published:
        build = quote(data["buildName"], safe="/")
        lines.append(f"HTML report: {S3}/convertigo-{build}/qualif/qualif-summary.html")
    lines.extend(f"{label}: {url}" for label, url in report_links(data) if url)
    lines.append("")
    for suite in data["reports"]:
        details = [f"{label}: {display_value(value)}" for label, value in suite_results(suite) + suite_details(suite)]
        if url := published_suite_url(data, suite):
            details.append(f"JMeter report: {url}")
        lines.append(f"{suite['name']} · {STATUSES[suite_status(suite)]} | " + " | ".join(details))
    if published or any("diagnostics" in suite for suite in data["reports"]):
        lines.extend(["", PRIVATE_NOTE])
    return "\n".join(lines) + "\n"


def link(label, url):
    return f'<a class="link" style="{LINK_STYLE}" href="{escape(url)}">{escape(label)}</a>'


def detail_html(fields):
    return "".join(
        f'<span style="{DETAIL_STYLE}"><strong>{escape(label)}</strong> {escape(display_value(value))}</span>'
        for label, value in fields
    )


def row_html(data, suite):
    status = suite_status(suite)
    background, accent, _, _ = ROW_COLORS[status]
    fill = f"background-color:{background};color:#17253A;"
    border = f"border-left:4px solid {accent};"
    cell = CELL_STYLE + fill
    url = published_suite_url(data, suite)
    jmeter = f'<br><span class="suite-report">{link("JMeter report", url)}</span>' if url else ""
    return (
        f'<tr class="suite-{status}" style="{fill}{border}">'
        f'<th scope="row" style="{cell}{border}">{escape(suite["name"])}'
        f'<br><span class="suite-status" style="font-weight:normal;">{STATUSES[status]}</span>{jmeter}</th>'
        f'<td style="{cell}">{detail_html(suite_results(suite))}</td>'
        f'<td style="{cell}">{detail_html(suite_details(suite))}</td></tr>'
    )


def render_html(data, published):
    status = display_status(data)
    foreground, background = BANNERS[status]
    links = " &nbsp;·&nbsp; ".join(link(label, url) for label, url in report_links(data) if url)
    checks = checks_label(data)
    checks_html = f'<p style="margin:8px 0 0;">{escape(checks)}</p>' if checks else ""
    headings = "".join(f'<th scope="col" style="{CELL_STYLE}">{label}</th>' for label in COLUMN_HEADINGS)
    rows = "\n".join(row_html(data, suite) for suite in data["reports"])
    dark_rows = "\n      ".join(
        f".suite-{state}, .suite-{state} > th, .suite-{state} > td {{ "
        f"background-color:{dark_background} !important; color:#F0F4FA !important; "
        f"border-left-color:{dark_accent} !important; }}"
        for state, (_, _, dark_background, dark_accent) in ROW_COLORS.items()
    )
    footer = ""
    if published or any("diagnostics" in suite for suite in data["reports"]):
        footer = f'<p style="margin:14px 0 0;">{escape(PRIVATE_NOTE)}</p>'
    return f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="color-scheme" content="light dark">
  <title>{escape(report_subject(data))}</title>
  <style>
    @media (max-width: 440px) {{
      .suite-table thead {{ position:absolute; width:1px; height:1px; overflow:hidden; clip:rect(0,0,0,0); }}
      .suite-table tbody, .suite-table tr {{ display:block; }}
      .suite-table tr {{ padding:6px 0; border-bottom:1px solid #D9E1EC; }}
      .suite-table th[scope="row"] {{ display:block; border:0 !important; padding:4px !important; }}
      .suite-table th[scope="row"] br {{ display:none; }}
      .suite-status, .suite-report {{ display:inline-block; margin-left:8px; }}
      .suite-table td {{ display:inline-block; width:50%; box-sizing:border-box; border:0 !important; padding:4px !important; }}
    }}
    @media (prefers-color-scheme: dark) {{
      .canvas {{ background-color:#121A27 !important; color:#F0F4FA !important; }}
      .panel {{ background-color:#192335 !important; color:#F0F4FA !important; }}
      .band {{ background-color:{foreground} !important; color:{background} !important; }}
      .link {{ color:#A9CCFF !important; }}
      th, td, .suite-table tr {{ border-bottom-color:#46526A !important; }}
      {dark_rows}
    }}
  </style>
</head>
<body class="canvas" style="margin:0;padding:12px;background-color:#EDF1F7;color:#17253A;font-family:Arial,sans-serif;">
  <table role="presentation" width="100%" align="center" cellpadding="0" cellspacing="0"
         class="panel" style="max-width:680px;background-color:#FFFFFF;color:#17253A;">
    <tr><td class="band" style="padding:20px;background-color:{background};color:{foreground};">
      <p style="margin:0 0 5px;font-size:14px;font-weight:bold;">Qualification {STATUSES[status].lower()}</p>
      <h1 style="margin:0;font-size:21px;overflow-wrap:anywhere;">Convertigo {escape(data['buildName'])}</h1>
      {checks_html}
      <p style="margin:6px 0 0;font-size:14px;">Recorded test failures: {data['totalFailures']}</p>
    </td></tr>
    <tr><td style="padding:16px;font-size:14px;">
      <p style="margin:0 0 12px;line-height:1.7;">{links}</p>
      <table width="100%" cellpadding="0" cellspacing="0" class="suite-table" aria-label="Qualification suites"
             style="border-collapse:collapse;font-size:13px;">
        <thead><tr>{headings}</tr></thead>
        <tbody>
{rows}
        </tbody>
      </table>
      {footer}
    </td></tr>
  </table>
</body>
</html>
"""


def write_reports(data, output, published=False):
    output.mkdir(parents=True, exist_ok=True)
    (output / "qualif-summary.txt").write_text(render_text(data, published), encoding="utf-8")
    (output / "qualif-summary.html").write_text(render_html(data, published), encoding="utf-8")
