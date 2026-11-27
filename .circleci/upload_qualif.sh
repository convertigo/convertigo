#!/usr/bin/env bash
set +x
set -eu

suite=$1
build=$2
private="$3/qualif-private"
uploads="$3/qualif-uploads"
shift 3
mkdir -p "$uploads"
for report in "$suite" "$@"; do
    : > "$uploads/$report.files"
done
test -d "$private" || exit 0

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

for report in "$suite" "$@"; do
    folder="$private/$report"
    test -d "$folder" || continue
    target="s3://convertigo-ci/convertigo-$build/qualif/private/$report/"
    if timeout 180s aws s3 cp --only-show-errors --acl private --recursive --no-follow-symlinks \
        --exclude '*' --include '*.html' --content-type 'text/html; charset=utf-8' "$folder/" "$target" \
        >/dev/null 2>&1; then
        (cd "$folder" && find . -type f -name '*.html' -print0) >> "$uploads/$report.files" ||
            echo "Warning: could not list $report private HTML diagnostics"
    else
        echo "Warning: could not upload $report private HTML diagnostics"
    fi
    if timeout 180s aws s3 cp --only-show-errors --acl private --recursive --no-follow-symlinks \
        --exclude '*.html' --content-type 'text/plain; charset=utf-8' "$folder/" "$target" \
        >/dev/null 2>&1; then
        (cd "$folder" && find . -type f ! -name '*.html' -print0) >> "$uploads/$report.files" ||
            echo "Warning: could not list $report private text diagnostics"
    else
        echo "Warning: could not upload $report private text diagnostics"
    fi
done
