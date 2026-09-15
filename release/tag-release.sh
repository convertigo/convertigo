#!/usr/bin/env bash
# Turns the current hotfix state into the official X.Y.Z release:
#   1. sets convertigoTag to '' in build.gradle and regenerates the versioned files;
#   2. commits "Official Convertigo X.Y.Z release!" on hotfix;
#   3. fast-forwards master to hotfix, pushes master to origin and upstream;
#   4. creates the X.Y.Z tag and pushes it to origin and upstream.
# The tag pipeline then builds the artifacts and creates the GitHub release draft.
#
# Usage: release/tag-release.sh X.Y.Z
# Run from a clean checkout of hotfix whose build.gradle already declares
# ext.convertigoVersion = 'X.Y.Z'. Set DRY_RUN=1 to stop before any push.
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
grep -q "^ext.convertigoVersion = '$version'$" build.gradle || die "build.gradle does not declare convertigoVersion '$version'"
git rev-parse -q --verify "refs/tags/$version" >/dev/null && die "tag $version already exists"

git fetch -q upstream master hotfix
[ "$(git rev-parse HEAD)" = "$(git rev-parse upstream/hotfix)" ] || die "local hotfix differs from upstream/hotfix: push or pull first"
git merge-base --is-ancestor upstream/master HEAD || die "upstream/master is not an ancestor of hotfix"

# 1. release mode
sed -i.bak -E "s/^ext.convertigoTag = '.*'$/ext.convertigoTag = ''/" build.gradle && rm -f build.gradle.bak
./gradlew -q generateEclipseConfigurationWithManifest generateDockerfile
git add -u
git diff --cached --stat

# 2. release commit on hotfix
git commit -q -m "Official Convertigo $version release!"
echo "committed $(git log -1 --format=%h) on hotfix"

if [ "${DRY_RUN:-0}" = 1 ]; then echo "DRY_RUN: stopping before push"; exit 0; fi

# 3. master = hotfix
git push -q upstream hotfix
git checkout -q master
git merge -q --ff-only hotfix
git push -q origin master
git push -q upstream master

# 4. tag
git tag "$version"
git push -q origin "$version"
git push -q upstream "$version"
git checkout -q hotfix
echo "tag $version pushed; watch the tag pipeline, then run release/start-next.sh"
