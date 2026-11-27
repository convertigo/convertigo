import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from qualif_report import REDIS_SUITE, REQUIRED_SUITES, SUITES, collect, summarize


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

    def test_only_counters_cross_the_workspace_and_publication_boundary(self):
        self.write_result("A_init", "true")
        self.write_result("main", "true")
        for suite in SUITES:
            self.assertFalse(collect(suite, self.repo, 0, self.reports))
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
        self.assertTrue(collect(SUITES[0], self.repo, 0, self.reports))
        self.write_result("A_init", "true")
        (self.jtl / "main.jtl").write_text("<testResults/>")
        self.assertTrue(collect(SUITES[1], self.repo, 0, self.reports))
        self.write_result("main", "true")
        self.assertFalse(collect(SUITES[-1], self.repo, 1, self.reports))
        result = self.summary()
        self.assertEqual(result["engineCheck"], "incomplete")
        self.assertEqual(result["suites"], "failed")
        self.assertEqual(result["mobile"], "failed")
        self.assertEqual([r["status"] for r in result["reports"]],
                         ["failed", "incomplete", "unavailable", "unavailable", "failed"])
        (self.reports / f"{SUITES[0]}.json").unlink()
        result = self.summary()
        self.assertEqual(result["status"], "incomplete")
        self.assertEqual(result["suites"], "incomplete")

    def test_redis_is_reported_without_affecting_required_qualification(self):
        self.write_result("A_init", "true")
        self.write_result("main", "true")
        for suite in REQUIRED_SUITES:
            self.assertFalse(collect(suite, self.repo, 0, self.reports))
        (self.root / "qualif-engine-check.exit").write_text("0\n")
        (self.jtl / "A_init.jtl").unlink()
        self.assertFalse(collect(REDIS_SUITE, self.repo, 0, self.reports))
        self.assertEqual(self.summary()["redis"], "passed")
        self.write_result("main", "false")
        self.assertFalse(collect(REDIS_SUITE, self.repo, 1, self.reports))
        result = self.summary()
        self.assertEqual(result["redis"], "failed")
        self.assertEqual(result["status"], "passed")
        self.assertIn("Redis warning", result["subject"])


if __name__ == "__main__":
    unittest.main()
