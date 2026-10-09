import argparse
import contextlib
import importlib.util
import io
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch


SCRIPTS = Path(__file__).resolve().parents[1] / "scripts"


def load_script(name):
    spec = importlib.util.spec_from_file_location(name, SCRIPTS / f"{name}.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


studio_update = load_script("studio_update")
forced_versions = load_script("check_forced_versions")


class CheckoutTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(prefix="studio-update-test-")
        self.addCleanup(self.directory.cleanup)
        self.repo = Path(self.directory.name).resolve()
        subprocess.run(["git", "init", "-q", "--initial-branch=develop", str(self.repo)], check=True)
        (self.repo / "build.gradle").write_text("ext.convertigoVersion = '8.5.0'\n", encoding="utf-8")
        self.studio = self.repo / "convertigo-studio-web"
        self.studio.mkdir()
        (self.studio / "package.json").write_text('{"scripts": {}}\n', encoding="utf-8")

    def resolve(self, lane="auto", repo=None, cwd=None):
        args = argparse.Namespace(lane=lane, repo=str(repo) if repo else None)
        with patch.object(studio_update.Path, "cwd", return_value=cwd or self.studio):
            return studio_update.resolve_repo_and_lane(args)

    def test_discovers_checkout_from_subdirectory(self):
        repo, lane, _ = self.resolve()
        self.assertEqual(repo, self.repo)
        self.assertEqual(lane, "develop")

    def test_explicit_lane_does_not_choose_another_clone(self):
        repo, lane, _ = self.resolve(lane="hotfix")
        self.assertEqual(repo, self.repo)
        self.assertEqual(lane, "hotfix")

    def test_explicit_repo_resolves_checkout_root(self):
        repo, lane, _ = self.resolve(repo=self.studio, cwd=Path("/"))
        self.assertEqual(repo, self.repo)
        self.assertEqual(lane, "develop")

    def test_outside_git_requires_explicit_repo(self):
        with tempfile.TemporaryDirectory() as outside:
            with self.assertRaisesRegex(SystemExit, "pass --repo"):
                self.resolve(cwd=Path(outside))

    def test_hotfix_branch_takes_hotfix_policy(self):
        subprocess.run(["git", "symbolic-ref", "HEAD", "refs/heads/hotfix"], cwd=self.repo, check=True)
        _, lane, cfg = self.resolve()
        self.assertEqual(lane, "hotfix")
        self.assertEqual(cfg["gradle_task"], "dependencyUpdatesPatch")

    def test_maintenance_ticket_is_not_reused_after_version_change(self):
        cfg = studio_update.LANES["develop"]
        self.assertEqual(studio_update.maintenance_ticket(self.repo, cfg), "#1039")
        (self.repo / "build.gradle").write_text("ext.convertigoVersion = '8.6.0'\n", encoding="utf-8")
        self.assertIn("confirm maintenance issue", studio_update.maintenance_ticket(self.repo, cfg))

    def test_plan_includes_conditional_studio_build_only_on_develop(self):
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            studio_update.phase_plan(self.repo, "develop", studio_update.LANES["develop"])
        plan = output.getvalue()
        self.assertLess(plan.index("<tycho-version>"), plan.index("generateEclipseConfigurationWithManifest"))
        self.assertLess(plan.index("generateEclipseConfigurationWithManifest"), plan.index("buildStudioClean"))
        self.assertLess(plan.index("buildStudioClean"), plan.index("buildStudio --console=plain"))
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            studio_update.phase_plan(self.repo, "hotfix", studio_update.LANES["hotfix"])
        self.assertNotIn("buildStudioClean", output.getvalue())

    def test_execute_refuses_wrong_branch_before_commands(self):
        argv = ["studio_update.py", "--repo", str(self.repo), "--lane", "hotfix",
                "--phase", "npm-update", "--execute"]
        with patch.object(sys, "argv", argv), patch.object(studio_update, "run_command") as run:
            with contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit) as error:
                studio_update.main()
        self.assertEqual(error.exception.code, 2)
        run.assert_not_called()

    def test_update_phase_is_dry_run_by_default(self):
        argv = ["studio_update.py", "--repo", str(self.repo), "--phase", "npm-update"]
        with patch.object(sys, "argv", argv), patch.object(studio_update, "run_command") as run:
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(studio_update.main(), 0)
        run.assert_called_once_with(["npm", "run", "deps:update"], self.studio, False)

    def test_dry_run_command_does_not_start_process(self):
        with patch.object(studio_update.subprocess, "run") as run, contextlib.redirect_stdout(io.StringIO()):
            studio_update.run_command(["npm", "run", "format"], self.studio, False)
        run.assert_not_called()


class ForcedVersionTests(unittest.TestCase):
    def test_stable_final_is_allowed_but_prereleases_are_not(self):
        self.assertEqual(forced_versions.version_key("4.1.130.Final"), (4, 1, 130))
        self.assertIsNone(forced_versions.version_key("4.1.131.CR1"))

    def test_hotfix_stays_on_same_minor_line(self):
        versions = ["4.1.129.Final", "4.1.130.Final", "4.2.1.Final", "4.1.131.CR1"]
        self.assertEqual(forced_versions.candidates("4.1.129.Final", versions, "hotfix"), ["4.1.130.Final"])
        self.assertEqual(forced_versions.candidates("4.1.129.Final", versions, "develop"),
                         ["4.1.130.Final", "4.2.1.Final"])

    def test_node_default_takes_the_latest_lts(self):
        releases = [{"version": "v25.1.0", "lts": False}, {"version": "v24.21.0", "lts": "Krypton"},
                    {"version": "v22.23.3", "lts": "Jod"}, {"version": "v22.16.0", "lts": "Jod"}]
        self.assertEqual(forced_versions.node_candidate("v22.16.0", releases, "develop"), "v24.21.0")
        self.assertEqual(forced_versions.node_candidate("v22.16.0", releases, "hotfix"), "v22.23.3")
        self.assertIsNone(forced_versions.node_candidate("v24.21.0", releases, "develop"))


if __name__ == "__main__":
    unittest.main()
