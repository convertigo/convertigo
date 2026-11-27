import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

from qualif_report import MOBILE_SUITE, REDIS_SUITE, REQUIRED_SUITES, SUITES, collect, summarize


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

    def write_result(self, name, success):
        (self.jtl / f"{name}.jtl").write_text(
            f'<testResults><httpSample s="{success}" lb="REQUEST_DATA_MARKER">'
            '<responseData>PRIVATE_RESPONSE_MARKER</responseData>'
            '</httpSample></testResults>'
        )

    def summary(self):
        with patch.dict(os.environ, {"CONVERTIGO_BUILD_NAME": "123-develop"}, clear=True):
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

    def test_only_counters_cross_the_workspace_and_publication_boundary(self):
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
        self.assertIn("Engine failed | Suites passed", result["subject"])
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
        self.assertIn("Suites warning", result["subject"])
        self.assertIn("Mobile warning", result["subject"])
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
        self.assertIn("Redis warning", result["subject"])


if __name__ == "__main__":
    unittest.main()
