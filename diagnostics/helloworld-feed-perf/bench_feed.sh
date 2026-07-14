#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${C8O_BASE_URL:-http://127.0.0.1:19080/convertigo}"
ITERATIONS="${ITERATIONS:-25}"
WARMUP="${WARMUP:-3}"
RESULT_DIR="${RESULT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/results}"
COOKIE_FILE="${C8O_COOKIE_FILE:-$RESULT_DIR/c8o-cookie.txt}"
USER_NAME="${C8O_USER:-admin}"
PASSWORD="${C8O_PASSWORD:-admin}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
OUT="$RESULT_DIR/feed-benchmark-$STAMP.csv"

mkdir -p "$RESULT_DIR"

login() {
  curl -fsS -c "$COOKIE_FILE" \
    --data-urlencode "authType=login" \
    --data-urlencode "authUserName=$USER_NAME" \
    --data-urlencode "authPassword=$PASSWORD" \
    "$BASE_URL/admin/services/engine.Authenticate" >/dev/null
}

request() {
  local name="$1"
  local url="$2"
  local iteration="$3"

  curl -sS -L -b "$COOKIE_FILE" -o /dev/null \
    -w "$name,$iteration,%{http_code},%{time_namelookup},%{time_connect},%{time_appconnect},%{time_starttransfer},%{time_total},%{size_download},%{url_effective}\n" \
    "$url"
}

bench_url() {
  local name="$1"
  local url="$2"

  for ((i = 1; i <= WARMUP; i++)); do
    request "$name" "$url" "warmup-$i" >/dev/null
  done

  for ((i = 1; i <= ITERATIONS; i++)); do
    request "$name" "$url" "$i" | tee -a "$OUT"
  done
}

login

echo "name,iteration,http_code,time_namelookup,time_connect,time_appconnect,time_starttransfer,time_total,size_download,url_effective" > "$OUT"
echo "# start_utc=$STAMP base_url=$BASE_URL iterations=$ITERATIONS warmup=$WARMUP" >&2

bench_url "direct_apod" "https://apod.com/feed.rss"
bench_url "direct_nasa_iotd" "https://www.nasa.gov/feeds/iotd-feed/"
bench_url "direct_nasa_lg_image" "https://www.nasa.gov/rss/dyn/lg_image_of_the_day.rss"
bench_url "legacy_getfeed" "$BASE_URL/projects/sample_HelloWorld/.json?__sequence=GetFeed"
bench_url "flow_readnasafeed" "$BASE_URL/projects/sample_HelloWorldFlowRun4/.json?__sequence=ReadNasaFeed"
bench_url "flow_exact_getfeed" "$BASE_URL/projects/sample_HelloWorld_flow/.json?__sequence=GetFeed"

python3 - "$OUT" <<'PY'
import csv
import statistics
import sys
from collections import defaultdict

path = sys.argv[1]
groups = defaultdict(list)
sizes = defaultdict(list)
codes = defaultdict(set)

with open(path, newline="") as handle:
    reader = csv.DictReader(handle)
    for row in reader:
        name = row["name"]
        groups[name].append(float(row["time_total"]) * 1000.0)
        sizes[name].append(int(row["size_download"]))
        codes[name].add(row["http_code"])

print("name,count,http_codes,median_ms,mean_ms,p95_ms,min_ms,max_ms,median_size")
for name in sorted(groups):
    values = sorted(groups[name])
    p95 = values[int(round((len(values) - 1) * 0.95))]
    print(
        f"{name},{len(values)},{'/'.join(sorted(codes[name]))},"
        f"{statistics.median(values):.1f},{statistics.mean(values):.1f},"
        f"{p95:.1f},{values[0]:.1f},{values[-1]:.1f},"
        f"{int(statistics.median(sizes[name]))}"
    )
PY

echo "# csv=$OUT" >&2
