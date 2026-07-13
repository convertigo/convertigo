#!/usr/bin/env python3
import csv
import hashlib
import http.cookiejar
import json
import os
import statistics
import time
import urllib.parse
import urllib.request
from pathlib import Path

import bench_flow_phases as phases


BASE_URL = os.environ.get("C8O_BASE_URL", "http://127.0.0.1:19080/convertigo")
PROJECT = os.environ.get("FLOW_PROJECT", "sample_HelloWorldFlowRun4")
SEQUENCE = os.environ.get("FLOW_SEQUENCE", "ReadNasaFeed")
ITERATIONS = int(os.environ.get("ITERATIONS", "30"))
WARMUP = int(os.environ.get("WARMUP", "5"))
LABEL = os.environ.get("ENGINE_LABEL", "working-tree")
STAMP = os.environ.get("STAMP", time.strftime("%Y%m%dT%H%M%SZ", time.gmtime()))
RESULT_DIR = Path(os.environ.get("RESULT_DIR", Path(__file__).resolve().parent / "results"))
WORKSPACE = Path(os.environ.get("C8O_WORKSPACE", "/home/nicolas/docker/convertigo-agent/workspace"))
ENGINE_CONFIG = WORKSPACE / "projects" / PROJECT / "libs" / "flow" / "engine.yaml"
CONFIG_KEY = os.environ.get("FLOW_CONFIG_KEY", "nasaImageOfTheDayUrl")
CONTEXT = "codex_saved_local_" + STAMP
FLOW_URL = (
    f"{BASE_URL}/projects/{PROJECT}/.json?__sequence={SEQUENCE}"
    f"&__context={urllib.parse.quote(CONTEXT)}"
)


def percentile(values, ratio):
    ordered = sorted(values)
    return ordered[int(round((len(ordered) - 1) * ratio))]


def patch_fixture_url(url):
    original = ENGINE_CONFIG.read_bytes()
    stat = ENGINE_CONFIG.stat()
    text = original.decode("utf-8")
    lines = text.splitlines(keepends=True)
    matches = [
        index for index, line in enumerate(lines)
        if line.lstrip().startswith(CONFIG_KEY + ":")
    ]
    if len(matches) != 1:
        raise RuntimeError(f"Expected one {CONFIG_KEY} entry in {ENGINE_CONFIG}")
    index = matches[0]
    indent = lines[index][:len(lines[index]) - len(lines[index].lstrip())]
    original_value = lines[index].split(":", 1)[1].strip()
    value = json.dumps(url) if original_value.startswith(('"', "'")) else url
    newline = "\n" if lines[index].endswith("\n") else ""
    lines[index] = indent + CONFIG_KEY + ": " + value + newline
    ENGINE_CONFIG.write_text("".join(lines), encoding="utf-8")

    def restore():
        ENGINE_CONFIG.write_bytes(original)
        os.chmod(ENGINE_CONFIG, stat.st_mode)
        os.utime(ENGINE_CONFIG, ns=(stat.st_atime_ns, stat.st_mtime_ns))

    return restore


def main():
    RESULT_DIR.mkdir(parents=True, exist_ok=True)
    server = None
    restore = None
    jar = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
    rows = []
    try:
        server, fixture_url, fixture = phases.start_fixture_server()
        restore = patch_fixture_url(fixture_url)
        for index in range(1, WARMUP + ITERATIONS + 1):
            phase = "warmup" if index <= WARMUP else "measured"
            ordinal = index if phase == "warmup" else index - WARMUP
            started = time.perf_counter()
            with opener.open(FLOW_URL, timeout=60) as response:
                body = response.read()
                status = response.status
            elapsed_ms = (time.perf_counter() - started) * 1000.0
            row = {
                "label": LABEL,
                "phase": phase,
                "ordinal": ordinal,
                "status": status,
                "total_ms": round(elapsed_ms, 3),
                "size": len(body),
                "sha256": hashlib.sha256(body).hexdigest(),
            }
            rows.append(row)
            print(json.dumps(row), flush=True)
        measured = [row for row in rows if row["phase"] == "measured" and row["status"] == 200]
        values = [row["total_ms"] for row in measured]
        summary = {
            "label": LABEL,
            "project": PROJECT,
            "sequence": SEQUENCE,
            "fixture": str(fixture),
            "iterations": ITERATIONS,
            "warmup": WARMUP,
            "count": len(measured),
            "medianMs": statistics.median(values),
            "meanMs": statistics.mean(values),
            "p95Ms": percentile(values, 0.95),
            "minMs": min(values),
            "maxMs": max(values),
            "sizes": sorted(set(row["size"] for row in measured)),
            "sha256": sorted(set(row["sha256"] for row in measured)),
        }
        csv_file = RESULT_DIR / f"saved-local-{LABEL}-{STAMP}.csv"
        json_file = RESULT_DIR / f"saved-local-{LABEL}-{STAMP}.json"
        with csv_file.open("w", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=rows[0].keys(), lineterminator="\n")
            writer.writeheader()
            writer.writerows(rows)
        json_file.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
        print("# summary=" + json.dumps(summary, sort_keys=True))
        print("# csv=" + str(csv_file))
        print("# json=" + str(json_file))
    finally:
        try:
            opener.open(FLOW_URL + "&__removeContext=true&__removeSession=true", timeout=30).read()
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
