#!/bin/sh

set -eu

if [ "$#" -ne 1 ]; then
	echo "Usage: $0 <output-dir>" >&2
	exit 1
fi

output_dir="$1"
report_root_url="${CONVERTIGO_URL}/qualif"
report_listing_url="https://convertigo-ci.s3-eu-west-3.amazonaws.com/index.html?prefix=convertigo-${CONVERTIGO_BUILD_NAME}/qualif/"
summary_txt="${output_dir}/qualif-summary.txt"
summary_json="${output_dir}/qualif-summary.json"

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

for index in 1 2 3; do
	name="qualif-jmx-${index}"
	report_url="${report_root_url}/${name}_result.html"
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
