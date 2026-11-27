#!/usr/bin/env bash
set -eu

report=$1
repo=$2
work=$3
umask 077
private="$work/qualif-private"
logs="$private/$report"
mkdir -p "$logs"

case "$report" in
    qualif-jmx-1) rm -f -- "$repo/jmx/mobileAppBuild.jmx" "$repo/jmx/ibm3270_ssl.jmx" ;;
    qualif-jmx-mobile)
        for plan in "$repo"/jmx/*.jmx; do
            if [ "${plan##*/}" != mobileAppBuild.jmx ]; then
                rm -f -- "$plan"
            fi
        done
        ;;
esac

status=0
collect_options=()
if [ -f "$repo/run.sh" ]; then
    if [ "$report" != qualif-jmx-4 ] && [ -z "${QUALIF_LAB_KEYS_XML:-}" ]; then
        echo "Warning: $report will use the evaluation license (QUALIF_LAB_KEYS_XML is unset)"
        export QUALIF_LAB_KEYS_XML='<keys/>'
        collect_options+=(--evaluation-license)
    fi
    (
        cd "$repo"
        unset QUALIF_GITHUB_TOKEN "${!AWS_@}"
        if [ "$report" != qualif-jmx-4 ]; then
            export QUALIF_ADMIN_USER="${QUALIF_ADMIN_USER:-admin}"
            export QUALIF_ADMIN_PASSWORD="${QUALIF_ADMIN_PASSWORD:-$(openssl rand -hex 24)}"
        fi
        sh run.sh
    ) > "$logs/run.log" 2>&1 || status=$?
else
    printf '%s\n' 'Qualification runner is unavailable' > "$logs/run.log"
fi
printf '%s\n' "$status" > "$logs/run.exit"

python3 .circleci/qualif_report.py collect "$report" "$repo" "$status" \
    "$work/qualif-reports" "${collect_options[@]}"

files=(-C "$logs" run.log run.exit)
if [ -d "$repo/out" ]; then
    files+=(-C "$repo" out)
fi
archive="$private/$report.tar.gz"
if ! tar -czf "$archive" "${files[@]}" >/dev/null 2>&1; then
    rm -f -- "$archive"
    echo "Warning: could not archive $report diagnostics"
fi
