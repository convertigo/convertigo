#!/usr/bin/env python3
import csv
import functools
import http.cookiejar
import http.server
import json
import os
import socket
import statistics
import subprocess
import threading
import time
import urllib.parse
import urllib.request
from pathlib import Path


BASE_URL = os.environ.get("C8O_BASE_URL", "http://127.0.0.1:19080/convertigo")
PROJECT = os.environ.get("FLOW_PROJECT", "sample_HelloWorld_flow")
QNAME = os.environ.get("FLOW_QNAME", PROJECT + ".GetFeed")
FEED_URL = os.environ.get("FEED_URL", "https://www.nasa.gov/rss/dyn/lg_image_of_the_day.rss")
ITERATIONS = int(os.environ.get("ITERATIONS", "12"))
WARMUP = int(os.environ.get("WARMUP", "3"))
STAMP = os.environ.get("STAMP", time.strftime("%Y%m%dT%H%M%SZ", time.gmtime()))
RESULT_DIR = Path(os.environ.get("RESULT_DIR", Path(__file__).resolve().parent / "results"))

MCP_CONTEXT_PREFIX = os.environ.get("MCP_CONTEXT", "codex_exact_flow_" + STAMP)
MCP_BASE_URL = f"{BASE_URL}/projects/lib_flow_mcp/.json?__sequence=McpServer"

FIXTURE_FILE = RESULT_DIR / f"nasa-lg-image-{STAMP}.rss"
VARIANT_CSV = RESULT_DIR / f"exact-flow-variant-{STAMP}.csv"
VARIANT_SUMMARY_CSV = RESULT_DIR / f"exact-flow-variant-summary-{STAMP}.csv"
VARIANT_DERIVED_JSON = RESULT_DIR / f"exact-flow-derived-{STAMP}.json"

OUTPUTS = '''const _flow = {
  outputs: {
    status: { type: "integer" },
    responseBytes: { type: "integer" },
    count: { type: "integer" },
    news: {
      type: "array",
      items: {
        type: "object",
        properties: {
          title: { type: "string" },
          description: { type: "string" },
          imageUrl: { type: "string" }
        }
      }
    }
  },
  tests: {
    default: { input: {} }
  }
}
'''

VARIANT_BODIES = {
    "empty": '''
function GetFeed({ input, config, result }) {
  result.status = 0
  result.responseBytes = 0
  result.count = 0
  return result
}
''',
    "http": '''
function GetFeed({ input, config, result }) {
  var feed = http.get({ url: config.services.nasa.feedUrl })
  result.status = feed.status
  result.responseBytes = feed.text.length
  result.count = 0
  return result
}
''',
    "http_rhino_count": '''
function GetFeed({ input, config, result }) {
  var feed = http.get({ url: config.services.nasa.feedUrl })
  var news = nasa.imageFeedItems({ xml: feed.text, limit: 20 })
  result.status = feed.status
  result.responseBytes = feed.text.length
  result.count = news.length
  return result
}
''',
    "full_result": '''
function GetFeed({ input, config, result }) {
  var feed = http.get({ url: config.services.nasa.feedUrl })
  var news = nasa.imageFeedItems({ xml: feed.text, limit: 20 })
  result.news = news
  result.count = news.length
  result.responseBytes = feed.text.length
  result.status = feed.status
  return result
}
''',
}


def flow_code(variant):
    return OUTPUTS + "\n" + VARIANT_BODIES[variant]


class QuietHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, fmt, *args):
        pass


class FixtureServer:
    def __init__(self, directory):
        self.directory = Path(directory)
        self.httpd = None
        self.thread = None
        self.port = None

    def __enter__(self):
        handler = functools.partial(QuietHandler, directory=str(self.directory))
        self.httpd = http.server.ThreadingHTTPServer(("0.0.0.0", 0), handler)
        self.port = self.httpd.server_address[1]
        self.thread = threading.Thread(target=self.httpd.serve_forever, daemon=True)
        self.thread.start()
        return self

    def __exit__(self, exc_type, exc, tb):
        if self.httpd:
            self.httpd.shutdown()
            self.httpd.server_close()
        if self.thread:
            self.thread.join(timeout=5)


class McpClient:
    def __init__(self):
        jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
        self.next_id = 1
        self.next_context = 1

    def rpc(self, method, params=None, remove_session=False):
        context = f"{MCP_CONTEXT_PREFIX}_{self.next_context}"
        self.next_context += 1
        url = (
            MCP_BASE_URL
            + "&__context="
            + urllib.parse.quote(context)
            + "&__removeContext=true"
        )
        if remove_session:
            url += "&__removeSession=true"
        request = {
            "jsonrpc": "2.0",
            "id": self.next_id,
            "method": method,
            "params": params or {},
        }
        self.next_id += 1
        body = urllib.parse.urlencode({"request": json.dumps(request)}).encode()
        http_request = urllib.request.Request(
            url,
            data=body,
            headers={"Content-Type": "application/x-www-form-urlencoded"},
        )
        with self.opener.open(http_request, timeout=180) as response:
            payload = json.loads(response.read().decode("utf-8"))
        if "document" in payload and "error" in payload["document"]:
            raise RuntimeError(payload["document"]["error"])
        if "error" in payload:
            raise RuntimeError(payload["error"])
        return payload["result"]

    def tool(self, name, arguments=None, allow_error=False):
        result = self.rpc("tools/call", {
            "name": name,
            "arguments": arguments or {},
        })
        if "structuredContent" not in result:
            raise RuntimeError(result)
        structured = result["structuredContent"]
        if structured.get("ok") is False and not allow_error:
            raise RuntimeError(structured)
        return structured

    def cleanup(self):
        try:
            self.rpc("notifications/initialized", {}, remove_session=True)
        except Exception:
            pass


def ensure_result_dir():
    RESULT_DIR.mkdir(parents=True, exist_ok=True)


def fetch_fixture():
    existing = os.environ.get("FEED_FIXTURE")
    if existing:
        return Path(existing)
    request = urllib.request.Request(FEED_URL, headers={"User-Agent": "Convertigo exact flow benchmark"})
    with urllib.request.urlopen(request, timeout=60) as response:
        data = response.read()
    FIXTURE_FILE.write_bytes(data)
    return FIXTURE_FILE


def docker_gateway():
    try:
        out = subprocess.check_output([
            "docker", "inspect", "c8o-agent-runtime",
            "--format", "{{range .NetworkSettings.Networks}}{{.Gateway}}{{end}}",
        ], text=True, stderr=subprocess.DEVNULL).strip()
        if out:
            return out
    except Exception:
        pass
    try:
        route = subprocess.check_output([
            "docker", "exec", "c8o-agent-runtime", "cat", "/proc/net/route",
        ], text=True, stderr=subprocess.DEVNULL)
        for line in route.splitlines()[1:]:
            fields = line.split()
            if len(fields) >= 3 and fields[1] == "00000000":
                raw = bytes.fromhex(fields[2])
                return ".".join(str(b) for b in raw)
    except Exception:
        pass
    return "host.docker.internal"


def median(values):
    return statistics.median(values) if values else 0.0


def p95(values):
    if not values:
        return 0.0
    ordered = sorted(values)
    return ordered[int(round((len(ordered) - 1) * 0.95))]


def write_summary(rows):
    by_variant = {}
    for row in rows:
        by_variant.setdefault(row["variant"], []).append(float(row["elapsed_ms"]))

    with VARIANT_SUMMARY_CSV.open("w", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["variant", "count", "median_ms", "mean_ms", "p95_ms", "min_ms", "max_ms"])
        for variant in VARIANT_BODIES:
            values = by_variant.get(variant, [])
            if not values:
                continue
            writer.writerow([
                variant,
                len(values),
                f"{median(values):.3f}",
                f"{statistics.mean(values):.3f}",
                f"{p95(values):.3f}",
                f"{min(values):.3f}",
                f"{max(values):.3f}",
            ])

    medians = {variant: median(values) for variant, values in by_variant.items()}
    deltas = {
        "flow_empty_envelope": medians.get("empty", 0.0),
        "http_local_over_empty": medians.get("http", 0.0) - medians.get("empty", 0.0),
        "rhino_xml_over_http": medians.get("http_rhino_count", 0.0) - medians.get("http", 0.0),
        "result_news_over_count_only": medians.get("full_result", 0.0) - medians.get("http_rhino_count", 0.0),
        "full_result_over_empty": medians.get("full_result", 0.0) - medians.get("empty", 0.0),
    }
    VARIANT_DERIVED_JSON.write_text(json.dumps({
        "project": PROJECT,
        "qname": QNAME,
        "fixture": str(FIXTURE_FILE),
        "iterations": ITERATIONS,
        "warmup": WARMUP,
        "mediansMs": medians,
        "deltasMs": deltas,
    }, indent=2), encoding="utf-8")


def run_variants(fixture_url):
    client = McpClient()
    rows = []
    original = None
    try:
        original = client.tool("code-get", {"project": PROJECT, "qname": QNAME})
        current_revision = original.get("revision")
        config = {"services": {"nasa": {"feedUrl": fixture_url}}}
        with VARIANT_CSV.open("w", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=[
                "variant", "iteration", "elapsed_ms", "ok", "result_chars", "count", "response_bytes", "status"
            ])
            writer.writeheader()
            for variant in VARIANT_BODIES:
                code_set = client.tool("code-set", {
                    "project": PROJECT,
                    "qname": QNAME,
                    "revision": current_revision,
                    "code": flow_code(variant),
                    "maxDiagnostics": 12,
                })
                current_revision = code_set.get("revision", current_revision)
                for i in range(1, WARMUP + ITERATIONS + 1):
                    label = f"warmup-{i}" if i <= WARMUP else str(i - WARMUP)
                    start = time.perf_counter()
                    result = client.tool("code-run", {
                        "project": PROJECT,
                        "qname": QNAME,
                        "config": config,
                        "maxDiagnostics": 12,
                    })
                    elapsed = (time.perf_counter() - start) * 1000.0
                    payload = result.get("result", {})
                    compact_news = payload.get("news")
                    count = payload.get("count")
                    row = {
                        "variant": variant,
                        "iteration": label,
                        "elapsed_ms": f"{elapsed:.3f}",
                        "ok": str(result.get("ok", False)).lower(),
                        "result_chars": result.get("resultChars", ""),
                        "count": count if count is not None else (
                            compact_news.get("length") if isinstance(compact_news, dict) else ""
                        ),
                        "response_bytes": payload.get("responseBytes", ""),
                        "status": payload.get("status", ""),
                    }
                    if i > WARMUP:
                        rows.append(row)
                        writer.writerow(row)
                        handle.flush()
                    print(f"{variant},{label},{elapsed:.3f} ms")
        write_summary(rows)
    finally:
        try:
            client.tool("code-discard", {"project": PROJECT, "qname": QNAME}, allow_error=True)
        finally:
            client.cleanup()


def main():
    ensure_result_dir()
    fixture = fetch_fixture()
    with FixtureServer(fixture.parent) as server:
        fixture_url = f"http://{docker_gateway()}:{server.port}/{urllib.parse.quote(fixture.name)}"
        print(f"# fixture={fixture}")
        print(f"# fixture_url_from_container={fixture_url}")
        run_variants(fixture_url)
    print(f"# variant_csv={VARIANT_CSV}")
    print(f"# summary_csv={VARIANT_SUMMARY_CSV}")
    print(f"# derived_json={VARIANT_DERIVED_JSON}")


if __name__ == "__main__":
    main()
