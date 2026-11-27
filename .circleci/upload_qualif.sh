#!/usr/bin/env bash
set +x
set -eu

suite=$1
build=$2
archive="$3/qualif-private/$suite.tar.gz"
test -f "$archive" || exit 0

install_aws() {
    local arch
    arch=$(uname -m) || return 1
    case "$arch" in x86_64|aarch64) ;; *) return 1 ;; esac
    curl --fail --silent --show-error --location --max-time 90 --output "$tools/aws.zip" \
        "https://awscli.amazonaws.com/awscli-exe-linux-$arch.zip" &&
        timeout 90s unzip -q "$tools/aws.zip" -d "$tools" &&
        timeout 90s "$tools/aws/install" --install-dir "$tools/install" --bin-dir "$tools/bin"
}

if ! command -v aws >/dev/null 2>&1; then
    if ! tools=$(mktemp -d); then
        echo "Warning: could not prepare AWS CLI for $suite diagnostics"
        exit 0
    fi
    trap 'rm -rf -- "$tools"' EXIT
    if ! install_aws >/dev/null 2>&1; then
        echo "Warning: could not install AWS CLI for $suite diagnostics"
        exit 0
    fi
    export PATH="$tools/bin:$PATH"
fi

timeout 180s aws s3 cp --only-show-errors --acl private "$archive" \
    "s3://convertigo-ci/convertigo-$build/qualif/private/$suite.tar.gz" \
    >/dev/null 2>&1 || echo "Warning: could not upload $suite diagnostics"
