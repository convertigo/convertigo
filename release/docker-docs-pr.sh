#!/usr/bin/env bash
# Opens the docker-library/docs pull request that syncs convertigo/content.md
# with docker/README.md of this repository.
#
# Usage: docker/release/docker-docs-pr.sh [--force] X.Y.Z [path-to-docs-fork]
#
# Precondition: the official image convertigo:X.Y.Z is available on Docker Hub
# (the docs describe the published image). --force skips that check, for
# example to open the docs pull request while the image one is under review.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
. "$here/common.sh"

force=0
if [ "${1:-}" = "--force" ]; then force=1; shift; fi
version="${1:-}"; [ -n "$version" ] || die "usage: $0 [--force] X.Y.Z [fork-dir]"
fork="${2:-$here/../../docker-docs}"
[ -d "$fork/.git" ] || die "fork not found: $fork"
require_cmd gh; require_cmd curl; require_cmd git

repo_dir="$(cd "$here/.." && pwd)"

# 1. The official image must exist.
code="$(curl -s -o /dev/null -w '%{http_code}' "https://hub.docker.com/v2/repositories/library/convertigo/tags/$version")"
if [ "$code" != "200" ]; then
  [ "$force" = 1 ] || die "convertigo:$version is not on Docker Hub yet (HTTP $code); use --force to open the docs pull request anyway"
  echo "warning: convertigo:$version is not on Docker Hub yet (HTTP $code), continuing because of --force" >&2
fi

# 2. Sync the fork and prepare the branch.
sync_fork "$fork" master
branch="convertigo-$(date +%Y.%m.%d)"
git -C "$fork" checkout -q -B "$branch" master

# 3. Regenerate convertigo/content.md from docker/README.md.
(cd "$repo_dir" && ./gradlew -q updateDockerDocsOfficial -PdockerDocsDir="$fork")
if git -C "$fork" diff --quiet -- convertigo/content.md; then
  echo "convertigo/content.md is already up to date"; exit 0
fi

# 4. The Docker docs CI rejects files that markdownfmt would change.
if [ -x "$fork/markdownfmt.sh" ]; then
  need="$(cd "$fork" && ./markdownfmt.sh -l convertigo/content.md 2>&1 || true)"
  [ -z "$need" ] || die "convertigo/content.md needs markdownfmt (fix docker/README.md, list items use '-<tab>'):
$(cd "$fork" && ./markdownfmt.sh -d convertigo/content.md 2>&1 | head -40)"
fi

git -C "$fork" add convertigo/content.md
git -C "$fork" commit -q -m "Updated documentation for Convertigo $version"
git -C "$fork" push -q -u origin "$branch"

open_pr "$fork" docker-library/docs master \
  "Updated documentation for Convertigo $version" \
  "Sync the Convertigo documentation with the $version release."
