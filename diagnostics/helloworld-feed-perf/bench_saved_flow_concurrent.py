#!/usr/bin/env python3
import concurrent.futures
import hashlib
import http.cookiejar
import json
import os
import statistics
import threading
import time
import urllib.parse
import urllib.request
from pathlib import Path

import bench_flow_phases as phases
import bench_saved_flow_local as saved


CONCURRENCY = int(os.environ.get("CONCURRENCY", "8"))
ITERATIONS = int(os.environ.get("ITERATIONS", "80"))
WARMUP = int(os.environ.get("WARMUP", "16"))
LABEL = os.environ.get("ENGINE_LABEL", "working-tree")
STAMP = os.environ.get("STAMP", time.strftime("%Y%m%dT%H%M%SZ", time.gmtime()))
RESULT_DIR = Path(os.environ.get("RESULT_DIR", Path(__file__).resolve().parent / "results"))
THREAD_STATE = threading.local()
SESSIONS = []
SESSIONS_LOCK = threading.Lock()


def request_once(index):
    if not hasattr(THREAD_STATE, "url"):
        context = f"codex_concurrent_{STAMP}_{threading.get_ident()}"
        THREAD_STATE.url = (
            f"{saved.BASE_URL}/projects/{saved.PROJECT}/.json?__sequence={saved.SEQUENCE}"
            f"&__context={urllib.parse.quote(context)}"
        )
        THREAD_STATE.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar())
        )
        with SESSIONS_LOCK:
            SESSIONS.append((THREAD_STATE.opener, THREAD_STATE.url))
    started = time.perf_counter()
    with THREAD_STATE.opener.open(THREAD_STATE.url, timeout=60) as response:
        body = response.read()
        status = response.status
    payload = json.loads(body.decode("utf-8"))
    return {
        "index": index,
        "status": status,
        "totalMs": round((time.perf_counter() - started) * 1000.0, 3),
        "size": len(body),
        "sha256": hashlib.sha256(body).hexdigest(),
        "count": payload.get("count"),
    }


def run_batch(executor, count, offset):
    futures = [executor.submit(request_once, offset + index) for index in range(count)]
    return [future.result() for future in futures]


def percentile(values, ratio):
    ordered = sorted(values)
    return ordered[int(round((len(ordered) - 1) * ratio))]


def main():
    RESULT_DIR.mkdir(parents=True, exist_ok=True)
    server = None
    restore = None
    try:
        server, fixture_url, fixture = phases.start_fixture_server()
        restore = saved.patch_fixture_url(fixture_url)
        with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENCY) as executor:
            run_batch(executor, WARMUP, -WARMUP)
            started = time.perf_counter()
            rows = run_batch(executor, ITERATIONS, 0)
            wall_seconds = time.perf_counter() - started
        values = [row["totalMs"] for row in rows if row["status"] == 200 and row["count"] == 60]
        summary = {
            "label": LABEL,
            "concurrency": CONCURRENCY,
            "iterations": ITERATIONS,
            "warmup": WARMUP,
            "fixture": str(fixture),
            "count": len(values),
            "medianMs": statistics.median(values),
            "p95Ms": percentile(values, 0.95),
            "maxMs": max(values),
            "wallMs": round(wall_seconds * 1000.0, 3),
            "throughputPerSecond": round(len(values) / wall_seconds, 3),
            "sizes": sorted(set(row["size"] for row in rows)),
            "sha256": sorted(set(row["sha256"] for row in rows)),
            "resultCounts": sorted(set(row["count"] for row in rows if row["count"] is not None)),
        }
        output = RESULT_DIR / f"saved-concurrent-{LABEL}-c{CONCURRENCY}-{STAMP}.json"
        output.write_text(json.dumps({"summary": summary, "rows": rows}, indent=2) + "\n", encoding="utf-8")
        print(json.dumps(summary, sort_keys=True))
        print("# json=" + str(output))
    finally:
        for opener, url in SESSIONS:
            try:
                opener.open(url + "&__removeContext=true&__removeSession=true", timeout=30).read()
            except Exception:
                pass
        if restore is not None:
            restore()
        if server is not None:
            server.terminate()
            try:
                server.wait(timeout=5)
            except Exception:
                server.kill()


if __name__ == "__main__":
    main()
