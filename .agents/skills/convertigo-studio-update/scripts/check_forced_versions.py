#!/usr/bin/env python3
"""Read-only supplement to Gradle update reports for Convertigo version overrides."""

import argparse
import json
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
# The Node.js the engine installs when nothing else selects one (Flow frontbuilder,
# NGX builder and templates without nodeJsVersion, local builds).
NODE_SOURCE = 'engine/src/com/twinsoft/convertigo/engine/util/ProcessUtils.java'
NODE_INDEX = 'https://nodejs.org/dist/index.json'
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


def node_candidate(current, releases, lane):
    """develop takes the latest LTS release, hotfix the latest LTS release of its major."""
    base = version_key(current.lstrip('v'))
    if base is None:
        raise ValueError(f"Unsupported Node.js version syntax: {current}; manual review required")
    lts = [key for key in (version_key(str(release.get('version', '')).lstrip('v'))
                           for release in releases if release.get('lts')) if key]
    pool = [key for key in lts if lane != 'hotfix' or key[0] == base[0]]
    best = max(pool, default=None)
    return 'v' + '.'.join(map(str, best)) if best and best > base else None


def check_node(repo, lane):
    """Prints the default Node.js status; returns True when the check is incomplete."""
    source = (repo / NODE_SOURCE).read_text()
    match = re.search(r'defaultNodeVersion\s*=\s*"([^"]+)"', source)
    if not match:
        print(f"REVIEW Node.js default: defaultNodeVersion absent or changed in {NODE_SOURCE}")
        return True
    line = source.count('\n', 0, match.start()) + 1
    try:
        with urllib.request.urlopen(NODE_INDEX, timeout=20) as response:
            newer = node_candidate(match[1], json.load(response), lane)
        status = f"UPDATE {match[1]} -> {newer}" if newer else f"CURRENT {match[1]}"
        print(f"{status}: Node.js default ({NODE_SOURCE}:{line})")
        return False
    except Exception as error:
        print(f"INCOMPLETE Node.js default: {error}")
        return True


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
    incomplete = check_node(repo, lane) or incomplete
    return 2 if incomplete else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', type=Path, required=True)
    parser.add_argument('--lane', choices=['hotfix', 'develop'], required=True)
    args = parser.parse_args()
    return check(args.repo.resolve(), args.lane)


if __name__ == '__main__':
    raise SystemExit(main())
