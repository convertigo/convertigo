#!/bin/sh
# Fetch the public engine fixture at an immutable revision without credentials.
set -eu

fixture_dir="${1:?usage: fetch-engine-test-fixture.sh DIRECTORY}"
revision=b2a843f34fd14a41aa2897856f598dce31f72323
repository=https://github.com/convertigo/c8oprj-lib-flow-engine.git
temporary=$(mktemp -d)
trap 'rm -rf -- "$temporary"' EXIT

git init -q "$temporary/repository"
GIT_TERMINAL_PROMPT=0 git -C "$temporary/repository" -c credential.helper= \
    fetch -q --depth 1 --no-tags "$repository" "$revision"
test "$(git -C "$temporary/repository" rev-parse FETCH_HEAD)" = "$revision"
git -C "$temporary/repository" archive --output="$temporary/fixture.tar" "$revision" \
    _flow/modules/runtime-handle-utils.js _flow/modules/row-producer-runtime.js
mkdir -p "$fixture_dir"
tar -xf "$temporary/fixture.tar" -C "$fixture_dir"
