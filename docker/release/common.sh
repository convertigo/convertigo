# Shared helpers for the release scripts (sourced, not executed).

die() { echo "error: $*" >&2; exit 1; }

require_cmd() { command -v "$1" >/dev/null 2>&1 || die "'$1' is required"; }

# sync_fork <dir> <default-branch>
# Fast-forwards the fork's default branch from the upstream remote and pushes it to origin.
sync_fork() {
  local dir="$1" branch="$2"
  git -C "$dir" remote get-url upstream >/dev/null 2>&1 || die "no 'upstream' remote in $dir"
  git -C "$dir" diff --quiet && git -C "$dir" diff --cached --quiet || die "$dir has uncommitted changes"
  git -C "$dir" fetch -q upstream "$branch"
  git -C "$dir" checkout -q "$branch"
  git -C "$dir" merge -q --ff-only "upstream/$branch"
  git -C "$dir" push -q origin "$branch"
}

# open_pr <dir> <upstream-repo> <base> <title> <body>
open_pr() {
  local dir="$1" repo="$2" base="$3" title="$4" body="$5"
  local head
  head="$(git -C "$dir" remote get-url origin | sed -E 's#.*[:/]([^/]+)/[^/]+$#\1#')"
  gh pr create -R "$repo" --base "$base" --head "$head:$(git -C "$dir" branch --show-current)" --title "$title" --body "$body"
}
