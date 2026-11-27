import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import call, patch

from qualif_render import S3, render_html, render_text
from qualif_report import MOBILE_SUITE, REDIS_SUITE, REQUIRED_SUITES, SUITES, collect, notify, summarize


class QualificationReportTest(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name)
        self.repo = self.root / "suite"
        (self.repo / "jmx").mkdir(parents=True)
        (self.repo / "jmx" / "main.jmx").touch()
        self.jtl = self.repo / "out" / "jtl"
        self.jtl.mkdir(parents=True)
        self.reports = self.root / "qualif-reports"
        self.public = self.root / "public"

    def write_result(self, name, success, duration="20"):
        (self.jtl / f"{name}.jtl").write_text(
            f'<testResults><httpSample s="{success}" t="{duration}" lb="Sample label">'
            '<samplerData>REQUEST_DATA_MARKER</samplerData>'
            '<responseData>PRIVATE_RESPONSE_MARKER</responseData>'
            '</httpSample></testResults>'
        )

    def summary(self, **environment):
        with patch.dict(os.environ, {"CONVERTIGO_BUILD_NAME": "123-develop", **environment}, clear=True):
            summarize(self.root, self.public)
        files = [path.read_text() for path in self.public.iterdir()]
        for output in files:
            self.assertNotIn("REQUEST_DATA_MARKER", output)
            self.assertNotIn("PRIVATE_RESPONSE_MARKER", output)
        return json.loads((self.public / "qualif-summary.json").read_text())

    def collect_cli(self, suite, status):
        result = subprocess.run([
            sys.executable, str(Path(__file__).with_name("qualif_report.py")),
            "collect", suite, str(self.repo), str(status), str(self.reports),
        ], capture_output=True, text=True, check=True)
        self.assertIn(f"Warning: {suite}", result.stdout)

    def test_suite_verdicts_preserve_engine_status(self):
        self.write_result("A_init", "true")
        self.write_result("main", "true")
        self.write_result("mobileAppBuild", "true")
        for suite in SUITES:
            collect(suite, self.repo, 0, self.reports)
        check = self.root / "qualif-engine-check.exit"
        check.write_text("1\n")
        result = self.summary()
        self.assertEqual(result["status"], "failed")
        self.assertEqual(result["totalFailures"], 0)
        self.assertEqual(result["subject"], "Convertigo qualification failed | 123-develop")
        check.write_text("0\n")
        self.assertEqual(self.summary()["status"], "passed")

    def test_initialization_error_and_missing_or_empty_results(self):
        self.write_result("A_init", "false")
        self.collect_cli(SUITES[0], 0)
        self.write_result("A_init", "true")
        (self.jtl / "main.jtl").write_text("<testResults/>")
        self.collect_cli(SUITES[1], 0)
        self.write_result("main", "true")
        self.collect_cli(MOBILE_SUITE, 1)
        result = self.summary()
        self.assertEqual(result["engineCheck"], "incomplete")
        self.assertEqual(result["suites"], "failed")
        self.assertEqual(result["mobile"], "failed")
        self.assertEqual(result["reports"][-1]["missingPlans"], 1)
        self.assertEqual([r["status"] for r in result["reports"]],
                         ["failed", "incomplete", "unavailable", "unavailable", "failed"])
        (self.root / "qualif-engine-check.exit").write_text("0\n")
        result = self.summary()
        self.assertEqual(result["status"], "warning")
        self.assertEqual(result["subject"], "Convertigo qualification warning | 123-develop")
        self.assertIn("Suites 1–3 warning", (self.public / "qualif-summary.txt").read_text())
        self.assertIn("Mobile warning", (self.public / "qualif-summary.txt").read_text())
        (self.root / "qualif-engine-check.exit").unlink()
        (self.reports / f"{SUITES[0]}.json").unlink()
        result = self.summary()
        self.assertEqual(result["status"], "incomplete")
        self.assertEqual(result["suites"], "incomplete")

    def test_evaluation_license_keeps_a_warning_with_passing_samples(self):
        self.write_result("A_init", "true")
        self.write_result("main", "true")
        collect(SUITES[0], self.repo, 0, self.reports,
                outcome={"phase": "jmeter_exited", "license": "evaluation"})
        report = self.summary()["reports"][0]
        self.assertEqual(report["status"], "incomplete")
        self.assertEqual(report["license"], "evaluation")
        self.assertEqual(report["tests"], 2)

    def test_redis_is_reported_without_affecting_required_qualification(self):
        self.write_result("A_init", "true")
        self.write_result("main", "true")
        for suite in REQUIRED_SUITES:
            collect(suite, self.repo, 0, self.reports)
        (self.root / "qualif-engine-check.exit").write_text("0\n")
        (self.jtl / "A_init.jtl").unlink()
        collect(REDIS_SUITE, self.repo, 0, self.reports)
        self.assertEqual(self.summary()["redis"], "passed")
        self.write_result("main", "false")
        collect(REDIS_SUITE, self.repo, 1, self.reports)
        result = self.summary()
        self.assertEqual(result["redis"], "failed")
        self.assertEqual(result["status"], "passed")
        self.assertEqual(result["subject"], "Convertigo qualification warning | 123-develop")
        self.assertIn("Redis warning", (self.public / "qualif-summary.txt").read_text())

    def test_html_and_plain_text_render_the_same_public_results(self):
        self.write_result("A_init", "true", "20")
        self.write_result("main", "false", "40")
        collect(SUITES[0], self.repo, 1, self.reports, outcome={
            "phase": "jmeter_exited", "license": "licensed", "diagnostics": "complete",
        })
        (self.root / "qualif-engine-check.exit").write_text("0\n")
        workflow = "9eefa4f4-0906-4e0d-9e1a-41077fdf0ad6"
        sha = "a" * 40
        self.summary(CIRCLE_WORKFLOW_ID=workflow, CIRCLE_SHA1=sha)
        html = (self.public / "qualif-summary.html").read_text()
        text = (self.public / "qualif-summary.txt").read_text()
        for output in (html, text):
            self.assertIn("qualification warning", output.lower())
            self.assertIn("50.00%", output)
            self.assertIn("30 ms", output)
            self.assertIn("60 ms", output)
            self.assertIn("JMeter finished", output)
            self.assertIn(f"{S3}/index.html?prefix=convertigo-123-develop/qualif/", output)
            self.assertIn(f"https://app.circleci.com/pipelines/workflows/{workflow}", output)
            self.assertIn(f"https://github.com/convertigo/convertigo/commit/{sha}", output)
            self.assertNotIn("_result.html", output)
        self.assertIn("S3 access required", text)
        self.assertIn("Connexion AWS requise", html)
        self.assertIn(f"{S3}/convertigo-123-develop/qualif/qualif-summary.html", text)
        self.assertEqual(html.count('<th scope="row"'), len(SUITES))

    def test_suite_rows_have_inline_colors_dark_overrides_and_status_labels(self):
        colors = {
            "passed": ("#EAF6EF", "#2D8054", "#20372D", "#6BCC91"),
            "warning": ("#FFF4DF", "#A56411", "#3C3020", "#F2B65F"),
            "incomplete": ("#FFF4DF", "#A56411", "#3C3020", "#F2B65F"),
            "unavailable": ("#FFF4DF", "#A56411", "#3C3020", "#F2B65F"),
            "failed": ("#FCEEF0", "#B74656", "#43272F", "#F18593"),
        }
        data = dict(buildName="123-develop", status="warning", totalFailures=1, reports=[
            dict(name=f"suite-{state}", status=state) for state in colors
        ])
        html = render_html(data, published=False)
        dark_css = html.split("@media (prefers-color-scheme: dark)", 1)[1].split("</style>", 1)[0]
        for state, (background, accent, dark_background, dark_accent) in colors.items():
            with self.subTest(state=state):
                row = html.split(f'<tr class="suite-{state}"', 1)[1].split("</tr>", 1)[0]
                self.assertEqual(row.count(f"background-color:{background};color:#17253A;"), 4)
                self.assertEqual(row.count(f"border-left:4px solid {accent};"), 2)
                self.assertIn(f">{state.capitalize()}</span>", row)
                self.assertIn(
                    f".suite-{state}, .suite-{state} > th, .suite-{state} > td {{ "
                    f"background-color:{dark_background} !important; color:#F0F4FA !important; "
                    f"border-left-color:{dark_accent} !important;",
                    dark_css,
                )

    def test_missing_times_keep_counts_and_leave_the_duration_unknown(self):
        self.write_result("A_init", "true", "10")
        self.write_result("main", "true", "")
        collect(SUITES[0], self.repo, 0, self.reports)
        result = self.summary()["reports"][0]
        self.assertEqual(result["status"], "passed")
        self.assertEqual(result["tests"], 2)
        self.assertNotIn("totalTimeMs", result)
        self.assertIn("Success rate: 100.00% | Average time: — | Total time: —",
                      (self.public / "qualif-summary.txt").read_text())

    def test_legacy_render_links_published_suite_reports(self):
        build = "123-develop"
        url = f"{S3}/convertigo-{build}/qualif/qualif-jmx-1_result.html"
        data = dict(buildName=build, totalFailures=0, summaryErrors=0, reports=[
            dict(name="qualif-jmx-1", tests="401", failures="0", successRate="100.00%",
                 averageTime="240 ms", totalTime="96268 ms", reportUrl=url),
            dict(name="<unknown>", reportUrl="javascript:alert(1)"),
        ])
        html = render_html(data, published=False)
        self.assertIn(f'href="{url}"', html)
        self.assertIn("&lt;unknown&gt;", html)
        self.assertNotIn("javascript:", html)
        self.assertIn('<tr class="suite-passed"', html)
        self.assertIn('<tr class="suite-unavailable"', html)
        text = render_text(data, published=False)
        self.assertIn(f"JMeter report: {url}", text)
        self.assertIn("Average time: 240 ms | Total time: 96268 ms", text)

    def test_suite_page_lists_failures_first_and_only_public_sample_fields(self):
        (self.repo / "jmx" / "main.jmx").write_text(
            '<plan><stringProp name="XPath.xpath">/result[@status="ready"]</stringProp>'
            '<stringProp name="script">AssertionResult.setFailureMessage("Static assertion detail")</stringProp></plan>'
        )
        self.write_result("A_init", "true")
        self.write_result("setup", "true")
        (self.jtl / "main.jtl").write_text(
            """<testResults>
              <httpSample s="true" lb="First passing sample" rc="200" t="10"/>
              <sample s="false" lb="Failed &amp; recorded" rc="200" t="42" rm="PRIVATE_RESPONSE_MESSAGE">
                <httpSample s="true" lb="Nested passing sample" rc="302" t="2"/>
                <assertionResult><failure>true</failure><error>false</error>
                  <failureMessage>No Nodes Matched /result[@status="ready"]</failureMessage>
                </assertionResult>
                <assertionResult><failure>false</failure><error>true</error>
                  <failureMessage>Test failed: text expected to contain PRIVATE_BODY_DIFF</failureMessage>
                </assertionResult>
                <assertionResult><failure>true</failure><error>false</error>
                  <failureMessage>Unknown message PRIVATE_LICENSE_VALUE</failureMessage>
                </assertionResult>
                <assertionResult><failure>true</failure><error>false</error>
                  <failureMessage>No Nodes Matched /PRIVATE_DYNAMIC_XPATH</failureMessage>
                </assertionResult>
                <assertionResult><failure>true</failure><error>false</error>
                  <failureMessage>Static assertion detail</failureMessage>
                </assertionResult>
                <responseData>PRIVATE_RESPONSE_MARKER</responseData>
                <requestHeader>PRIVATE_HEADER</requestHeader><queryString>PRIVATE_PARAMETERS</queryString>
                <samplerData>REQUEST_DATA_MARKER</samplerData>
              </sample>
              <sample s="true" lb="https://example.test/?id=PRIVATE_QUERY" rc="200" t="0"/>
              <sample s="true" lb="PRIVATE_GITHUB_VALUE" rc="200"/>
            </testResults>"""
        )
        with patch.dict(os.environ, {
            "QUALIF_GITHUB_TOKEN": "PRIVATE_GITHUB_VALUE",
            "QUALIF_LAB_KEYS_XML": "<keys>PRIVATE_LICENSE_VALUE</keys>",
        }):
            collect(SUITES[0], self.repo, 1, self.reports)
        result = self.summary()
        self.assertEqual(result["reports"][0]["tests"], 6)
        self.assertEqual(result["reports"][0]["failures"], 1)
        self.assertNotIn("totalTimeMs", result["reports"][0])
        html = (self.public / f"{SUITES[0]}.html").read_text()
        self.assertIn("<h2>Failed samples (1)</h2>", html)
        self.assertIn("<td>main</td><td>Failed &amp; recorded</td><td>200</td><td>42 ms</td>", html)
        self.assertIn("No Nodes Matched /result[@status=&quot;…&quot;]", html)
        self.assertIn("Test failed: text expected to contain …", html)
        self.assertIn("Assertion failed (details in private JTL)", html)
        self.assertNotIn("Static assertion detail", html)
        self.assertIn("<details><summary>Other samples (6)</summary>", html)
        self.assertLess(html.index("Failed &amp; recorded"), html.index("First passing sample"))
        self.assertIn("Nested passing sample", html)
        self.assertIn("setup or teardown", html)
        self.assertIn("S3 folder", html)
        self.assertIn("aws s3 presign", html)
        receipt = (self.reports / f"{SUITES[0]}-samples.json").read_text()
        for output in (html, receipt):
            self.assertNotIn("PRIVATE_", output)
            self.assertNotIn("REQUEST_DATA_MARKER", output)
        overview = (self.public / "qualif-summary.html").read_text()
        for suite in result["reports"]:
            self.assertIn(f'href="{suite["reportUrl"]}"', overview)
            self.assertTrue((self.public / (suite["name"] + ".html")).exists())

    def test_public_suite_masks_literals_and_expected_or_actual_values(self):
        expression = '''/screen/field[@name="PRIVATE_DOUBLE's text" and @value='PRIVATE_SINGLE "text"' and @code]'''
        (self.repo / "jmx" / "main.jmx").write_text(
            f'<plan><stringProp name="XPath.xpath">{expression}</stringProp></plan>'
        )
        messages = (
            f"No Nodes Matched {expression}",
            'Expected: PRIVATE_EXPECTED but got "PRIVATE_ACTUAL"',
            'Actual: PRIVATE_ACTUAL_ONLY',
            'Test failed: text expected to equal "PRIVATE_COMPARISON" but was PRIVATE_RESPONSE',
        )
        assertions = "".join(
            f"<assertionResult><failure>true</failure><failureMessage>{message}</failureMessage></assertionResult>"
            for message in messages
        )
        jtl = self.jtl / "main.jtl"
        source = f'<testResults><sample s="false">{assertions}</sample></testResults>'
        jtl.write_text(source)
        collect(SUITES[0], self.repo, 1, self.reports)
        self.summary()
        rows = json.loads((self.reports / f"{SUITES[0]}-samples.json").read_text())
        self.assertEqual(rows[0]["assertions"], [
            '''No Nodes Matched /screen/field[@name="…" and @value='…' and @code]''',
            "Expected …, actual …",
            "Actual …",
            "Test failed: text expected to equal …",
        ])
        html = (self.public / f"{SUITES[0]}.html").read_text()
        self.assertIn('@name=&quot;…&quot; and @value=&#x27;…&#x27; and @code', html)
        self.assertNotIn("PRIVATE_", html)
        self.assertEqual(jtl.read_text(), source)

    def test_short_mail_has_one_row_per_suite_and_links_only(self):
        self.write_result("A_init", "true", "10")
        self.write_result("main", "false", "40")
        (self.root / "qualif-engine-check.exit").write_text("0")
        collect(SUITES[0], self.repo, 1, self.reports,
                outcome={"phase": "jmeter_exited", "license": "licensed", "diagnostics": "complete"})
        result = self.summary()
        html = (self.public / "qualif-mail.html").read_text()
        text = (self.public / "qualif-mail.txt").read_text()
        self.assertEqual(html.count('<th scope="row"'), len(SUITES))
        self.assertIn("qualif-jmx-1 | 2 | 1 | 50 ms | Failed", text)
        for suite in result["reports"]:
            for output in (html, text):
                self.assertEqual(output.count(suite["reportUrl"]), 1)
        for output in (html, text):
            self.assertIn("qualification warning", output)
            self.assertIn("Full qualification report", output)
            for detail in ("JMeter finished", "Success rate", "License", "Missing plans",
                           "Recorded test failures", "Sample label"):
                self.assertNotIn(detail, output)

    def test_private_console_links_use_only_the_encoded_keys_and_region(self):
        uploads = self.root / "qualif-uploads"
        uploads.mkdir()
        paths = [
            "./run.log", "./out/engine.log", "./out/jtl/plan é & # +.jtl", "./out/result.html",
            "./run.log", "./../INVALID_PARENT.log", "/INVALID_ABSOLUTE.log", "./INVALID\nCONTROL.log",
        ]
        (uploads / f"{SUITES[0]}.files").write_bytes(("\x00".join(paths) + "\x00").encode("utf-8"))
        self.summary()
        console = "https://s3.console.aws.amazon.com/s3"
        prefix = "convertigo-123-develop/qualif/private/qualif-jmx-1/"
        folder = f"{console}/buckets/convertigo-ci?region=eu-west-3&prefix={prefix}"
        objects = [f"{console}/object/convertigo-ci?region=eu-west-3&prefix={prefix}{name}"
                   for name in ("run.log", "out/engine.log", "out/jtl/plan%20%C3%A9%20%26%20%23%20%2B.jtl",
                                "out/result.html")]
        for filename in ("qualif-jmx-1.html", "qualif-summary.html", "qualif-mail.html", "qualif-mail.txt"):
            with self.subTest(filename=filename):
                result = (self.public / filename).read_text(encoding="utf-8")
                self.assertIn("Connexion AWS requise", result)
                for url in (folder, *objects):
                    expected = f'href="{url.replace("&", "&amp;")}"' if filename.endswith(".html") else url
                    self.assertEqual(result.count(expected), 1)
                self.assertNotIn("INVALID", result)
                self.assertNotIn("X-Amz-", result)
        other_suite = (self.public / "qualif-jmx-2.html").read_text(encoding="utf-8")
        self.assertIn("/qualif/private/qualif-jmx-2/", other_suite)
        self.assertNotIn("/s3/object/", other_suite)

    def test_ses_sends_both_formats_with_sns_as_a_transport_fallback(self):
        result = self.summary()
        env = dict(QUALIF_NOTIFY_SES_FROM="ci@example.test", QUALIF_NOTIFY_SES_TO="a@example.test, b@example.test",
                   QUALIF_NOTIFY_SNS_TOPIC_ARN="test-topic")

        def read_request(*args):
            self.assertEqual(args[:3], ("ses", "send-email", "--cli-input-json"))
            request = json.loads(Path(args[3].removeprefix("file://")).read_text())
            self.assertEqual(request["Destination"]["ToAddresses"], ["a@example.test", "b@example.test"])
            self.assertEqual(request["Message"]["Subject"]["Data"], result["subject"])
            for part, extension in (("Text", "txt"), ("Html", "html")):
                self.assertEqual(request["Message"]["Body"][part]["Data"],
                                 (self.public / f"qualif-mail.{extension}").read_text())
            return True

        with patch.dict(os.environ, env, clear=True), patch("qualif_report.aws_succeeded") as aws:
            aws.side_effect = read_request
            notify(self.public)
            aws.assert_called_once()
            aws.reset_mock(side_effect=True)
            aws.side_effect = [False, True]
            notify(self.public)
            self.assertEqual(aws.call_count, 2)
            self.assertEqual(aws.call_args, call("sns", "publish", "--topic-arn", "test-topic",
                                               "--subject", result["subject"], "--message",
                                               f"file://{self.public / 'qualif-mail.txt'}"))
            aws.reset_mock(side_effect=True)
            os.environ["QUALIF_NOTIFY_SES_REGION"] = "us-east-1"
            aws.side_effect = None
            aws.return_value = True
            notify(self.public)
            aws.assert_called_once()
            self.assertEqual(aws.call_args.args[-2:], ("--region", "us-east-1"))
            aws.reset_mock(return_value=True)
            del os.environ["QUALIF_NOTIFY_SES_FROM"], os.environ["QUALIF_NOTIFY_SES_TO"]
            notify(self.public)
            aws.assert_called_once()
            self.assertEqual(aws.call_args.args[:2], ("sns", "publish"))


if __name__ == "__main__":
    unittest.main()
