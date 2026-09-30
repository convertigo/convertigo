#!/usr/bin/env python3
"""Read-only supplement to Gradle update reports for Convertigo version overrides."""

import argparse
import re
import subprocess
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path


# Representative artifacts for the two known override rules. Read versions from
# source, never duplicate their current value here. Unknown rules require review.
RULES = (
    ("io.netty", "netty-handler", r"details\.requested\.name\.startsWith\('netty-'\)"),
    ("org.apache.neethi", "neethi", r"details\.requested\.name == 'neethi'"),
)
OVERRIDE = re.compile(r"\b(useVersion|force|strictly|enforcedPlatform|substitute)\b")


def version_key(version):
    """Only stable numeric Maven versions, optionally with the Final suffix."""
    match = re.fullmatch(r"(\d+(?:\.\d+){2,})(?:\.Final)?", version)
    return tuple(map(int, match[1].split('.'))) if match else None


def candidates(current, versions, lane):
    base = version_key(current)
    if base is None:
        raise ValueError(f"Unsupported version syntax: {current}; manual review required")
    return sorted(
        (v for v in versions if version_key(v) is not None
         and version_key(v) > base
         and (lane != 'hotfix' or version_key(v)[:2] == base[:2])),
        key=version_key,
    )


def check(repo, lane):
    paths = subprocess.check_output(
        ['git', 'ls-files', '-z', '*.gradle', '*.gradle.kts'], cwd=repo
    ).decode().split('\0')
    covered = set()
    incomplete = False
    source = (repo / 'engine/build.gradle').read_text()
    for group, artifact, name_pattern in RULES:
        pattern = (r"if\s*\(details\.requested\.group == '" + re.escape(group)
                   + r"' && " + name_pattern + r"\)\s*\{[^{}]*?"
                   + r"details\.useVersion\s+'([^']+)'")
        match = re.search(pattern, source)
        if not match:
            print(f"REVIEW {group}:{artifact}: override absent or changed; inspect source")
            incomplete = True
            continue
        line = source.count('\n', 0, source.index('details.useVersion', match.start())) + 1
        covered.add(('engine/build.gradle', line))
        current = match[1]
        url = (f"https://repo.maven.apache.org/maven2/{group.replace('.', '/')}/"
               f"{artifact}/maven-metadata.xml")
        try:
            with urllib.request.urlopen(url, timeout=20) as response:
                versions = [e.text for e in ET.fromstring(response.read()).findall('./versioning/versions/version')]
            if not versions:
                raise ValueError('No versions in Maven metadata')
            newer = candidates(current, versions, lane)
            status = f"UPDATE {current} -> {newer[-1]}" if newer else f"CURRENT {current}"
            print(f"{status}: {group}:{artifact} (engine/build.gradle:{line})")
        except Exception as error:
            incomplete = True
            print(f"INCOMPLETE {group}:{artifact}: {error}")
    for path in filter(None, paths):
        for line, text in enumerate((repo / path).read_text().splitlines(), 1):
            stripped = text.strip()
            if stripped.startswith(('//', '*')) or 'npm ' in stripped:
                continue
            if OVERRIDE.search(text) and (path, line) not in covered:
                incomplete = True
                print(f"REVIEW {path}:{line}: {stripped}")
    return 2 if incomplete else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', type=Path, required=True)
    parser.add_argument('--lane', choices=['hotfix', 'develop'], required=True)
    args = parser.parse_args()
    return check(args.repo.resolve(), args.lane)


if __name__ == '__main__':
    raise SystemExit(main())
