#!/usr/bin/env bash
set -uo pipefail

if [[ $# -lt 5 || "$4" != "--" ]]; then
  echo "usage: $0 <run> <segment> <trace.jsonl> -- <command> [args...]" >&2
  exit 2
fi

RUN_ID="$1"
SEGMENT_ID="$2"
TRACE_FILE="$3"
shift 4

if [[ -e "$TRACE_FILE" ]]; then
  echo "refusing to overwrite existing trace: $TRACE_FILE" >&2
  exit 2
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
mkdir -p "$(dirname "$TRACE_FILE")"
STARTED_MS="$(date +%s%3N)"

if [[ -n "${CAMPAIGN_STDIN_FILE:-}" ]]; then
  "$@" < "$CAMPAIGN_STDIN_FILE" \
    | node "$SCRIPT_DIR/campaign-trace.mjs" stamp --run "$RUN_ID" --segment "$SEGMENT_ID" \
    | tee "$TRACE_FILE"
else
  "$@" \
    | node "$SCRIPT_DIR/campaign-trace.mjs" stamp --run "$RUN_ID" --segment "$SEGMENT_ID" \
    | tee "$TRACE_FILE"
fi
PIPE_STATUS=("${PIPESTATUS[@]}")
COMMAND_STATUS="${PIPE_STATUS[0]}"

node "$SCRIPT_DIR/campaign-trace.mjs" segment-exit \
  --run "$RUN_ID" \
  --segment "$SEGMENT_ID" \
  --status "$COMMAND_STATUS" \
  --started-ms "$STARTED_MS" >> "$TRACE_FILE"

exit "$COMMAND_STATUS"
