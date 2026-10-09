#!/usr/bin/env python3
"""Lane-aware helper for the Convertigo Studio dependency update routine."""

from __future__ import annotations

import argparse
import json
import re
import shlex
import subprocess
import sys
from pathlib import Path


LANES = {
    "hotfix": {
        "branch": "hotfix",
        "maintenance_version": "8.4.6",
        "ticket": "#1170",
        "gradle_task": "dependencyUpdatesPatch",
        "npm_update": "deps:minor-update",
    },
    "develop": {
        "branch": "develop",
        "maintenance_version": "8.5.0",
        "ticket": "#1039",
        "gradle_task": "dependencyUpdates",
        "npm_update": "deps:update",
    },
}

NPM_VERIFY_SCRIPTS = ["format", "lint", "check:admin", "build"]
EXECUTABLE_PHASES = [
    "preflight",
    "gradle-report",
    "eclipse-config",
    "npm-update",
    "npm-audit-fix",
    "npm-verify",
    "status",
]


def run_capture(cmd: list[str], cwd: Path) -> str:
    result = subprocess.run(cmd, cwd=cwd, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    return result.stdout.strip()


def print_command(cmd: list[str], cwd: Path) -> None:
    pretty = " ".join(shlex.quote(part) for part in cmd)
    print(f"$ cd {cwd} && {pretty}")


def run_command(cmd: list[str], cwd: Path, execute: bool) -> None:
    print_command(cmd, cwd)
    if execute:
        subprocess.run(cmd, cwd=cwd, check=True)


def find_git_root(path: Path) -> Path | None:
    # the exit code, not the message: git localizes it ("fatal :" in French)
    result = subprocess.run(["git", "rev-parse", "--show-toplevel"], cwd=path, text=True,
                            stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    output = result.stdout.strip()
    return Path(output).resolve() if result.returncode == 0 and output else None


def read_build_versions(repo: Path) -> dict[str, str]:
    build_gradle = repo / "build.gradle"
    values: dict[str, str] = {}
    if build_gradle.is_file():
        text = build_gradle.read_text(encoding="utf-8", errors="replace")
        for key in [
            "convertigoVersion",
            "jxBrowserVersion",
            "swaggerUiVersion",
            "tomcatVersion",
            "eclipseVersion",
            "eclipseBase",
            "studioWebNodeVersion",
            "soapuiVersion",
            "log4jVersion",
            "commonsIo",
            "commonsLang3",
        ]:
            match = re.search(rf"ext\.{re.escape(key)}\s*=\s*'([^']+)'", text)
            if match:
                values[key] = match.group(1)
        wrapper_match = re.search(r"gradleVersion\s*=\s*'([^']+)'", text)
        if wrapper_match:
            values["gradleVersion"] = wrapper_match.group(1)

    wrapper_properties = repo / "gradle" / "wrapper" / "gradle-wrapper.properties"
    if wrapper_properties.is_file():
        wrapper_text = wrapper_properties.read_text(encoding="utf-8", errors="replace")
        distribution = re.search(r"(?m)^distributionUrl=(.+)$", wrapper_text)
        if distribution:
            values["gradleWrapperDistribution"] = distribution.group(1)

    jdk_script = repo / "installers" / "nsis" / "download-jdk.sh"
    if jdk_script.is_file():
        jdk_text = jdk_script.read_text(encoding="utf-8", errors="replace")
        jdk_release = re.search(r"(?m)^JDK_RELEASE=([0-9]+(?:\.[0-9]+){2}\+[0-9]+)\s*$", jdk_text)
        if jdk_release:
            values["studioJdkRelease"] = jdk_release.group(1)
    return values


def package_scripts(repo: Path) -> dict[str, str]:
    package_json = repo / "convertigo-studio-web" / "package.json"
    if not package_json.is_file():
        return {}
    data = json.loads(package_json.read_text(encoding="utf-8"))
    scripts = data.get("scripts", {})
    return scripts if isinstance(scripts, dict) else {}


def detect_lane(repo: Path) -> str | None:
    branch = run_capture(["git", "branch", "--show-current"], repo)
    for lane, cfg in LANES.items():
        if branch == cfg["branch"]:
            return lane

    version = read_build_versions(repo).get("convertigoVersion", "")
    if version.startswith("8.4."):
        return "hotfix"
    if version.startswith("8.5."):
        return "develop"
    return None


def resolve_repo_and_lane(args: argparse.Namespace) -> tuple[Path, str, dict[str, str]]:
    if args.repo:
        repo = Path(args.repo).expanduser().resolve()
        if not repo.is_dir():
            raise SystemExit(f"Repository directory does not exist: {repo}")
        repo = find_git_root(repo) or repo
    else:
        repo = find_git_root(Path.cwd())
        if repo is None:
            raise SystemExit("Run inside a Convertigo checkout or pass --repo <path>")

    if not (repo / "build.gradle").is_file() or not (repo / "convertigo-studio-web" / "package.json").is_file():
        raise SystemExit(f"Not a Convertigo repository root: {repo}")

    lane = args.lane if args.lane != "auto" else detect_lane(repo)
    if lane not in LANES:
        raise SystemExit(f"Could not detect lane for {repo}; pass --lane hotfix or --lane develop")

    return repo, lane, LANES[lane]


def maintenance_ticket(repo: Path, cfg: dict[str, str]) -> str:
    version = read_build_versions(repo).get("convertigoVersion", "")
    if version == cfg["maintenance_version"]:
        return cfg["ticket"]
    return f"confirm maintenance issue for {version or 'unknown version'}"


def phase_plan(repo: Path, lane: str, cfg: dict[str, str]) -> None:
    studio = repo / "convertigo-studio-web"
    print(f"Lane: {lane} ({maintenance_ticket(repo, cfg)})")
    print(f"Repository: {repo}")
    print(f"Expected branch: {cfg['branch']}")
    if lane == "hotfix":
        print("Guardrail: keep Gradle and Java/JDK pinned unless a minor update is explicitly approved.")
    else:
        print("Guardrail: put risky or uncertain library candidates in a review bucket instead of updating blindly.")
    print("")
    print_command(["git", "status", "--short", "--branch"], repo)
    print_command(["git", "remote", "-v"], repo)
    print_command(["./gradlew", cfg["gradle_task"], "--console=plain"], repo)
    print_command(forced_versions_command(repo, lane), repo)
    if lane == "develop":
        print("# Check released Eclipse train/platform and stable Tycho independently; standing approval on develop.")
    print("# Review the dependency report and edit build.gradle files manually.")
    if lane == "develop":
        print("# Apply selected Eclipse versions in build.gradle and <tycho-version> in pom.xml before generation.")
    print_command(["./gradlew", "generateEclipseConfigurationWithManifest", "--console=plain"], repo)
    if lane == "develop":
        print("# Only if Eclipse or Tycho changed: preserve local product test edits, then run these sequentially.")
        print_command(["mvn", "-version"], repo)
        print_command(["./gradlew", "buildStudioClean", "--console=plain"], repo)
        print_command(["./gradlew", "buildStudio", "--console=plain"], repo)
    print_command(["npm", "run", cfg["npm_update"]], studio)
    print_command(["npm", "audit", "fix"], studio)
    for script in NPM_VERIFY_SCRIPTS:
        print_command(["npm", "run", script], studio)
    print_command(["git", "status", "--short"], repo)


def phase_preflight(repo: Path, lane: str, cfg: dict[str, str]) -> None:
    print(f"Lane: {lane} ({maintenance_ticket(repo, cfg)})")
    print(f"Repository: {repo}")
    print(f"Expected branch: {cfg['branch']}")
    if lane == "hotfix":
        print("Guardrail: keep Gradle and Java/JDK pinned unless a minor update is explicitly approved.")
    else:
        print("Guardrail: put risky or uncertain library candidates in a review bucket instead of updating blindly.")
    print("")
    for cmd in [
        ["git", "status", "--short", "--branch"],
        ["git", "branch", "--show-current"],
        ["git", "remote", "-v"],
    ]:
        print_command(cmd, repo)
        print(run_capture(cmd, repo) or "(no output)")
        print("")

    versions = read_build_versions(repo)
    print("build.gradle versions:")
    for key in sorted(versions):
        print(f"  {key}: {versions[key]}")
    print("")

    scripts = package_scripts(repo)
    required_scripts = [cfg["npm_update"], *NPM_VERIFY_SCRIPTS]
    print("npm scripts:")
    for script in required_scripts:
        marker = "ok" if script in scripts else "missing"
        print(f"  {script}: {marker}")


def forced_versions_command(repo: Path, lane: str) -> list[str]:
    return [sys.executable, str(Path(__file__).with_name("check_forced_versions.py")),
            "--repo", str(repo), "--lane", lane]


def phase_gradle_report(repo: Path, lane: str, cfg: dict[str, str], execute: bool) -> None:
    run_command(["./gradlew", cfg["gradle_task"], "--console=plain"], repo, execute)
    run_command(forced_versions_command(repo, lane), repo, execute)


def phase_eclipse_config(repo: Path, execute: bool) -> None:
    run_command(["./gradlew", "generateEclipseConfigurationWithManifest", "--console=plain"], repo, execute)


def phase_npm_update(repo: Path, cfg: dict[str, str], execute: bool) -> None:
    run_command(["npm", "run", cfg["npm_update"]], repo / "convertigo-studio-web", execute)


def phase_npm_audit_fix(repo: Path, execute: bool) -> None:
    run_command(["npm", "audit", "fix"], repo / "convertigo-studio-web", execute)


def phase_npm_verify(repo: Path, execute: bool) -> None:
    studio = repo / "convertigo-studio-web"
    for script in NPM_VERIFY_SCRIPTS:
        run_command(["npm", "run", script], studio, execute)


def phase_status(repo: Path, execute: bool) -> None:
    run_command(["git", "status", "--short"], repo, execute)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--lane", choices=["auto", "hotfix", "develop"], default="auto")
    parser.add_argument("--repo", help="Convertigo checkout path. Defaults to the current Git root, regardless of --lane.")
    parser.add_argument(
        "--phase",
        action="append",
        choices=["plan", *EXECUTABLE_PHASES, "all"],
        default=None,
        help="Phase to print or run. Can be repeated. Defaults to plan.",
    )
    parser.add_argument("--execute", action="store_true", help="Actually run executable phases. Without this, commands are printed only.")
    args = parser.parse_args()

    repo, lane, cfg = resolve_repo_and_lane(args)
    phases = args.phase or ["plan"]
    expanded: list[str] = []
    for phase in phases:
        if phase == "all":
            expanded.extend(EXECUTABLE_PHASES)
        else:
            expanded.append(phase)

    writing_phases = set(EXECUTABLE_PHASES) - {"preflight", "status"}
    if args.execute and writing_phases.intersection(expanded):
        branch = run_capture(["git", "branch", "--show-current"], repo)
        if branch != cfg["branch"]:
            parser.error(f"Refusing to execute {lane} updates on branch {branch or '(detached HEAD)'}; expected {cfg['branch']}")

    for index, phase in enumerate(expanded):
        if index:
            print("")
        print(f"== {phase} ==")
        if phase == "plan":
            phase_plan(repo, lane, cfg)
        elif phase == "preflight":
            phase_preflight(repo, lane, cfg)
        elif phase == "gradle-report":
            phase_gradle_report(repo, lane, cfg, args.execute)
        elif phase == "eclipse-config":
            phase_eclipse_config(repo, args.execute)
        elif phase == "npm-update":
            phase_npm_update(repo, cfg, args.execute)
        elif phase == "npm-audit-fix":
            phase_npm_audit_fix(repo, args.execute)
        elif phase == "npm-verify":
            phase_npm_verify(repo, args.execute)
        elif phase == "status":
            phase_status(repo, args.execute)
        else:
            raise AssertionError(f"Unhandled phase {phase}")

    command_phases = set(EXECUTABLE_PHASES) - {"preflight"}
    if not args.execute and any(phase in command_phases for phase in expanded):
        print("")
        print("Dry-run only. Re-run with --execute to execute command phases.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
