#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${C8O_BETA_BASE_URL:-https://beta.convertigo.net/convertigo}"
ITERATIONS="${ITERATIONS:-12}"
WARMUP="${WARMUP:-2}"
RESULT_DIR="${RESULT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/results}"
STAMP="${STAMP:-$(date -u +%Y%m%dT%H%M%SZ)}"
CONTEXT="${C8O_CONTEXT:-codex_bench_$STAMP}"
COOKIE_FILE="${C8O_COOKIE_FILE:-$RESULT_DIR/beta-cookie-$STAMP.txt}"
OUT="$RESULT_DIR/beta-benchmark-$STAMP.csv"
SUMMARY="$RESULT_DIR/beta-summary-$STAMP.csv"

LEGACY_URL="$BASE_URL/projects/sample_HelloWorld/.json?__sequence=GetFeed&__context=$CONTEXT"
FLOW_URL="$BASE_URL/projects/sample_HelloWorld_flow/.json?__sequence=GetFeed&__context=$CONTEXT"

mkdir -p "$RESULT_DIR"

cleanup() {
  curl -sS -L -c "$COOKIE_FILE" -b "$COOKIE_FILE" -o /dev/null \
    "$LEGACY_URL&__removeContext=true" || true
  curl -sS -L -c "$COOKIE_FILE" -b "$COOKIE_FILE" -o /dev/null \
    "$FLOW_URL&__removeContext=true&__removeSession=true" || true
  if [[ "${KEEP_BODIES:-0}" != "1" ]]; then
    rm -f "$RESULT_DIR/$STAMP-legacy.body" "$RESULT_DIR/$STAMP-flow.body"
  fi
}
trap cleanup EXIT

request() {
  local phase="$1"
  local pair="$2"
  local ordinal="$3"
  local target="$4"
  local url body metrics bytes sha

  if [[ "$target" == "legacy" ]]; then
    url="$LEGACY_URL"
  else
    url="$FLOW_URL"
  fi

  body="$RESULT_DIR/$STAMP-$target.body"
  metrics="$(
    curl -sS -L -c "$COOKIE_FILE" -b "$COOKIE_FILE" -o "$body" \
      -w "%{http_code},%{time_namelookup},%{time_connect},%{time_appconnect},%{time_pretransfer},%{time_starttransfer},%{time_total},%{size_download},%{url_effective}" \
      "$url"
  )"
  bytes="$(wc -c < "$body" | tr -d ' ')"
  sha="$(sha256sum "$body" | awk '{print $1}')"
  printf "%s,%s,%s,%s,%s,%s,%s\n" "$phase" "$pair" "$ordinal" "$target" "$metrics" "$bytes" "$sha" | tee -a "$OUT"
}

echo "phase,pair,ordinal,target,http_code,time_namelookup,time_connect,time_appconnect,time_pretransfer,time_starttransfer,time_total,size_download,url_effective,body_bytes,body_sha256" > "$OUT"
echo "# start_utc=$STAMP base_url=$BASE_URL context=$CONTEXT iterations=$ITERATIONS warmup=$WARMUP" >&2

request cold 0 1 legacy
request cold 0 2 flow

for ((i = 1; i <= WARMUP; i++)); do
  request warmup "$i" 1 legacy >/dev/null
  request warmup "$i" 2 flow >/dev/null
done

for ((i = 1; i <= ITERATIONS; i++)); do
  if (( i % 2 == 1 )); then
    request measured "$i" 1 legacy
    request measured "$i" 2 flow
  else
    request measured "$i" 1 flow
    request measured "$i" 2 legacy
  fi
done

python3 - "$OUT" "$SUMMARY" <<'PY'
import csv
import statistics
import sys
from collections import Counter, defaultdict

path, summary_path = sys.argv[1], sys.argv[2]
rows = []
with open(path, newline="") as handle:
    reader = csv.DictReader(handle)
    for row in reader:
        for key in (
            "time_namelookup",
            "time_connect",
            "time_appconnect",
            "time_pretransfer",
            "time_starttransfer",
            "time_total",
        ):
            row[key] = float(row[key]) * 1000.0
        row["size_download"] = int(row["size_download"])
        row["body_bytes"] = int(row["body_bytes"])
        rows.append(row)

measured = [row for row in rows if row["phase"] == "measured" and row["http_code"] == "200"]
sha_counts = Counter(row["body_sha256"] for row in measured)
expected_sha = sha_counts.most_common(1)[0][0] if sha_counts else ""
valid = [row for row in measured if row["body_sha256"] == expected_sha and row["body_bytes"] == row["size_download"]]

def percentile(sorted_values, pct):
    if not sorted_values:
        return 0.0
    return sorted_values[int(round((len(sorted_values) - 1) * pct))]

def stats(values):
    values = sorted(values)
    return {
        "count": len(values),
        "median": statistics.median(values) if values else 0.0,
        "mean": statistics.mean(values) if values else 0.0,
        "p95": percentile(values, 0.95),
        "min": values[0] if values else 0.0,
        "max": values[-1] if values else 0.0,
    }

summary_rows = []
groups = defaultdict(list)
for row in valid:
    groups[row["target"]].append(row)

for target, target_rows in sorted(groups.items()):
    metrics = {
        "connect": [row["time_connect"] for row in target_rows],
        "tls": [row["time_appconnect"] for row in target_rows],
        "ttfb": [row["time_starttransfer"] for row in target_rows],
        "total": [row["time_total"] for row in target_rows],
        "server_after_tls": [row["time_starttransfer"] - row["time_appconnect"] for row in target_rows],
    }
    for metric, values in metrics.items():
        s = stats(values)
        summary_rows.append({
            "target": target,
            "metric": metric,
            "count": s["count"],
            "median_ms": f'{s["median"]:.1f}',
            "mean_ms": f'{s["mean"]:.1f}',
            "p95_ms": f'{s["p95"]:.1f}',
            "min_ms": f'{s["min"]:.1f}',
            "max_ms": f'{s["max"]:.1f}',
        })

with open(summary_path, "w", newline="") as handle:
    fieldnames = ["target", "metric", "count", "median_ms", "mean_ms", "p95_ms", "min_ms", "max_ms"]
    writer = csv.DictWriter(handle, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(summary_rows)

print(f"expected_sha={expected_sha}")
print(f"valid_measured_rows={len(valid)} / {len(measured)}")
print(open(summary_path).read(), end="")
PY

echo "# csv=$OUT" >&2
echo "# summary=$SUMMARY" >&2
