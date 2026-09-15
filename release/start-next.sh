#!/usr/bin/env bash
# Starts the next beta on hotfix after a release:
#   sets convertigoVersion to X.Y.Z and convertigoTag to 'beta' in build.gradle,
#   regenerates the versioned files (ProductVersion, Eclipse poms and manifests,
#   Dockerfile) and commits "Starting Convertigo X.Y.Z beta!".
#
# Usage: release/start-next.sh X.Y.Z
# Run from a clean checkout of hotfix. The commit is not pushed.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
. "$here/common.sh"
repo="$(cd "$here/.." && pwd)"
cd "$repo"

version="${1:-}"; [ -n "$version" ] || die "usage: $0 X.Y.Z"
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "version must be X.Y.Z"
require_cmd git

[ "$(git branch --show-current)" = "hotfix" ] || die "run this script from the hotfix branch"
git diff --quiet && git diff --cached --quiet || die "working tree has uncommitted changes"

sed -i.bak -E \
  -e "s/^ext.convertigoVersion = '.*'$/ext.convertigoVersion = '$version'/" \
  -e "s/^ext.convertigoTag = '.*'$/ext.convertigoTag = 'beta'/" build.gradle && rm -f build.gradle.bak
grep -q "^ext.convertigoVersion = '$version'$" build.gradle || die "failed to set convertigoVersion"
grep -q "^ext.convertigoTag = 'beta'$" build.gradle || die "failed to set convertigoTag"

./gradlew -q generateEclipseConfigurationWithManifest generateDockerfile
git add -u
git diff --cached --stat
git commit -q -m "Starting Convertigo $version beta!"
echo "committed $(git log -1 --format=%h) on hotfix; push it with: git push upstream hotfix"
