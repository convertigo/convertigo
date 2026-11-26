#!/bin/sh

set -eu

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
	echo "Usage: $0 <output-dir> [engine-check-exit-file]" >&2
	exit 1
fi

output_dir="$1"
report_root_url="${CONVERTIGO_URL}/qualif"
report_listing_url="https://convertigo-ci.s3-eu-west-3.amazonaws.com/index.html?prefix=convertigo-${CONVERTIGO_BUILD_NAME}/qualif/"
summary_txt="${output_dir}/qualif-summary.txt"
summary_json="${output_dir}/qualif-summary.json"
engine_check_exit_file="${2:-}"

mkdir -p "$output_dir"

total_failures=0
summary_errors=0
json_reports=""

append_json_report() {
	name="$1"
	tests="$2"
	failures="$3"
	success_rate="$4"
	average_time="$5"
	total_time="$6"
	report_url="$7"

	entry=$(cat <<EOF
{
  "name": "$name",
  "tests": "$tests",
  "failures": "$failures",
  "successRate": "$success_rate",
  "averageTime": "$average_time",
  "totalTime": "$total_time",
  "reportUrl": "$report_url"
}
EOF
)

	if [ -n "$json_reports" ]; then
		json_reports="${json_reports},
${entry}"
	else
		json_reports="$entry"
	fi
}

{
	printf 'Convertigo %s build %s finished with %s failures\n' "${CIRCLE_BRANCH:-tag}" "$CONVERTIGO_BUILD_NAME" "$total_failures"
	printf 'S3 reports: %s\n' "$report_listing_url"
	printf '\n'
} > "$summary_txt"

for name in qualif-jmx-1 qualif-jmx-2 qualif-jmx-3 qualif-jmx-mobile; do
	report_path="${name}_result.html"
	if [ "$name" = qualif-jmx-mobile ]; then
		report_path="mobile/qualif-jmx-1_result.html"
	fi
	report_url="${report_root_url}/${report_path}"
	report_file="${output_dir}/${name}_result.html"

	if ! curl -fsSL --retry 5 --retry-delay 2 --retry-all-errors "$report_url" -o "$report_file"; then
		summary_errors=$((summary_errors + 1))
		printf '%s: summary unavailable (download error)\n' "$name" >> "$summary_txt"
		append_json_report "$name" "" "" "" "" "" "$report_url"
		continue
	fi

	if ! summary_row=$(perl -0ne 'if (m{<h2>Summary</h2>.*?<tr[^>]*>\s*<td><strong>([^<]+)</strong></td><td><strong>([^<]+)</strong></td><td><strong>([^<]+)</strong></td><td><strong>([^<]+)</strong></td><td><strong>([^<]+)</strong></td>}s) { print join("\t", $1, $2, $3, $4, $5), "\n"; exit 0 } exit 1' "$report_file"); then
		summary_errors=$((summary_errors + 1))
		printf '%s: summary unavailable (parse error)\n' "$name" >> "$summary_txt"
		append_json_report "$name" "" "" "" "" "" "$report_url"
		continue
	fi

	IFS='	' read -r tests failures success_rate average_time total_time <<EOF
$summary_row
EOF

	case "$failures" in
		''|*[!0-9]*)
			summary_errors=$((summary_errors + 1))
			;;
		*)
			total_failures=$((total_failures + failures))
			;;
	esac

	printf '%s: tests=%s failures=%s success=%s average=%s total=%s\n' \
		"$name" "$tests" "$failures" "$success_rate" "$average_time" "$total_time" >> "$summary_txt"
	append_json_report "$name" "$tests" "$failures" "$success_rate" "$average_time" "$total_time" "$report_url"
done

engine_check_status=unavailable
if [ -f "$engine_check_exit_file" ]; then
	case "$(cat "$engine_check_exit_file")" in
		0) engine_check_status=passed ;;
		[1-9]|[1-9][0-9]|[1-9][0-9][0-9]) engine_check_status=failed ;;
	esac
fi
printf 'engine-check: %s\n' "$engine_check_status" >> "$summary_txt"

subject="Convertigo ${CIRCLE_BRANCH:-tag} build ${CONVERTIGO_BUILD_NAME} finished with ${total_failures} failures"
if [ "$summary_errors" -gt 0 ]; then
	subject="${subject} (${summary_errors} summary errors)"
fi

tmp_summary="$(mktemp)"
{
	printf 'Convertigo %s build %s finished with %s failures\n' "${CIRCLE_BRANCH:-tag}" "$CONVERTIGO_BUILD_NAME" "$total_failures"
	if [ "$summary_errors" -gt 0 ]; then
		printf 'Summary errors: %s\n' "$summary_errors"
	fi
	printf 'S3 reports: %s\n' "$report_listing_url"
	printf '\n'
	sed '1,3d' "$summary_txt"
} > "$tmp_summary"
mv "$tmp_summary" "$summary_txt"

cat > "$summary_json" <<EOF
{
  "branch": "${CIRCLE_BRANCH:-}",
  "buildName": "${CONVERTIGO_BUILD_NAME}",
  "totalFailures": ${total_failures},
  "engineCheck": "${engine_check_status}",
  "summaryErrors": ${summary_errors},
  "reportsUrl": "${report_listing_url}",
  "subject": "${subject}",
  "reports": [
${json_reports}
  ]
}
EOF

if [ -n "${QUALIF_NOTIFY_SNS_TOPIC_ARN:-}" ]; then
	if ! aws sns publish \
		--topic-arn "$QUALIF_NOTIFY_SNS_TOPIC_ARN" \
		--subject "$subject" \
		--message "$(cat "$summary_txt")"; then
		echo "Warning: failed to publish qualif summary notification" >&2
	fi
fi
