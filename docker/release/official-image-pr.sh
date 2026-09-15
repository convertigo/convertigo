#!/usr/bin/env bash
# Opens the docker-library/official-images pull request for a Convertigo release.
#
# Usage: docker/release/official-image-pr.sh X.Y.Z [path-to-official-images-fork]
#
# Preconditions: the tag X.Y.Z exists on the convertigo repository, the GitHub
# release is published and convertigo-X.Y.Z.war is attached to it (the Docker
# Library bot downloads it while building the image).
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
. "$here/common.sh"

version="${1:-}"; [ -n "$version" ] || die "usage: $0 X.Y.Z [fork-dir]"
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "version must be X.Y.Z"
fork="${2:-$here/../../../docker-official-images}"
[ -d "$fork/.git" ] || die "fork not found: $fork"
require_cmd gh; require_cmd curl; require_cmd git

minor="${version%.*}"
repo_dir="$(cd "$here/../.." && pwd)"

# 1. The WAR must be downloadable from the published release.
war_url="https://github.com/convertigo/convertigo/releases/download/$version/convertigo-$version.war"
code="$(curl -sIL -o /dev/null -w '%{http_code}' "$war_url")"
[ "$code" = "200" ] || die "$war_url answers HTTP $code: publish the release with the WAR attached first"

# 2. Commit of the tag.
git -C "$repo_dir" fetch -q --tags
commit="$(git -C "$repo_dir" rev-list -n 1 "$version" 2>/dev/null)" || die "tag $version not found"

# 3. Sync the fork and prepare the branch.
sync_fork "$fork" master
branch="convertigo-$version"
git -C "$fork" checkout -q -B "$branch" master

lib="$fork/library/convertigo"
[ -f "$lib" ] || die "missing $lib"
sed -i.bak -E \
  -e "s/^GitCommit: .*/GitCommit: $commit/" \
  -e "s/^Tags: .*/Tags: $version, $minor, latest/" "$lib"
rm -f "$lib.bak"
grep -q "^GitCommit: $commit$" "$lib" || die "GitCommit not updated in $lib"
grep -q "^Tags: $version, $minor, latest$" "$lib" || die "Tags not updated in $lib"

echo "--- library/convertigo"; cat "$lib"; echo "---"

git -C "$fork" add library/convertigo
git -C "$fork" commit -q -m "Official Convertigo $version release!"
git -C "$fork" push -q -u origin "$branch"

open_pr "$fork" docker-library/official-images master \
  "Official Convertigo $version release!" \
  "Update the Convertigo image to $version (tag commit $commit). Changelog: https://github.com/convertigo/convertigo/blob/$version/CHANGELOG.md"
