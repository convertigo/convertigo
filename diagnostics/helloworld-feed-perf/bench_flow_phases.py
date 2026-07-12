#!/usr/bin/env python3
import csv
import http.cookiejar
import json
import os
import socket
import statistics
import subprocess
import time
import urllib.parse
import urllib.request
from pathlib import Path


BASE_URL = os.environ.get("C8O_BASE_URL", "http://127.0.0.1:19080/convertigo")
PROJECT = os.environ.get("FLOW_PROJECT", "sample_HelloWorldFlowRun4")
QNAME = os.environ.get("FLOW_QNAME", PROJECT + ".ReadNasaFeed")
ITERATIONS = int(os.environ.get("ITERATIONS", "12"))
WARMUP = int(os.environ.get("WARMUP", "3"))
STAMP = os.environ.get("STAMP", time.strftime("%Y%m%dT%H%M%SZ", time.gmtime()))
RESULT_DIR = Path(os.environ.get("RESULT_DIR", Path(__file__).resolve().parent / "results"))
MCP_CONTEXT_PREFIX = os.environ.get("MCP_CONTEXT", "codex_flow_phase_" + STAMP)
HTTP_CONTEXT = os.environ.get("HTTP_CONTEXT", "codex_flow_http_" + STAMP)

MCP_BASE_URL = f"{BASE_URL}/projects/lib_flow_mcp/.json?__sequence=McpServer"
FLOW_URL = f"{BASE_URL}/projects/{PROJECT}/.json?__sequence={QNAME.split('.')[-1]}&__context={urllib.parse.quote(HTTP_CONTEXT)}"

VARIANT_CSV = RESULT_DIR / f"flow-variant-{STAMP}.csv"
VARIANT_SUMMARY_CSV = RESULT_DIR / f"flow-variant-summary-{STAMP}.csv"
VARIANT_DERIVED_JSON = RESULT_DIR / f"flow-variant-derived-{STAMP}.json"
HTTP_CSV = RESULT_DIR / f"flow-http-{STAMP}.csv"
HTTP_SUMMARY_JSON = RESULT_DIR / f"flow-http-summary-{STAMP}.json"
COOKIE_FILE = RESULT_DIR / f"flow-http-cookie-{STAMP}.txt"

OUTPUTS = '''const _flow = {
  outputs: {
    status: { type: "integer" },
    responseBytes: { type: "integer" },
    items: { type: "integer" },
    count: { type: "integer" },
    news: {
      type: "array",
      items: {
        type: "object",
        properties: {
          title: { type: "string" },
          description: { type: "string" },
          imageUrl: { type: "string" },
          link: { type: "string" },
          pubDate: { type: "string" }
        }
      }
    }
  },
  tests: {
    smoke: { input: {} }
  }
}
'''

VARIANT_BODIES = {
    "empty": '''
function ReadNasaFeed({ input, config, result }) {
  result.status = 0
  result.responseBytes = 0
  result.items = 0
  result.count = 0
  return result
}
''',
    "http": '''
function ReadNasaFeed({ input, config, result }) {
  var response = http.get({ url: config.feeds.nasaImageOfTheDayUrl })
  result.status = response.status
  result.responseBytes = response.text.length
  result.items = 0
  result.count = 0
  return result
}
''',
    "http_xml": '''
function ReadNasaFeed({ input, config, result }) {
  var response = http.get({ url: config.feeds.nasaImageOfTheDayUrl })
  var feed = xml.parse({ text: response.text })
  result.status = response.status
  result.responseBytes = response.text.length
  result.items = feed.rss.channel.item.length
  result.count = result.items
  return result
}
''',
    "http_xml_sort": '''
function ReadNasaFeed({ input, config, result }) {
  var response = http.get({ url: config.feeds.nasaImageOfTheDayUrl })
  var feed = xml.parse({ text: response.text })
  var sorted = list.sort({
    items: feed.rss.channel.item,
    by: current.title,
    direction: "asc"
  })
  result.status = response.status
  result.responseBytes = response.text.length
  result.items = sorted.length
  result.count = sorted.length
  return result
}
''',
    "http_xml_sort_map": '''
function ReadNasaFeed({ input, config, result }) {
  var response = http.get({ url: config.feeds.nasaImageOfTheDayUrl })
  var feed = xml.parse({ text: response.text })
  var sorted = list.sort({
    items: feed.rss.channel.item,
    by: current.title,
    direction: "asc"
  })
  var news = list.map({
    items: sorted,
    select: {
      title: current.title,
      description: current.description,
      imageUrl: current.enclosure.attr.url,
      link: current.link,
      pubDate: current.pubDate
    }
  })
  result.status = response.status
  result.responseBytes = response.text.length
  result.items = sorted.length
  result.count = news.length
  return result
}
''',
    "full_result": '''
function ReadNasaFeed({ input, config, result }) {
  var response = http.get({ url: config.feeds.nasaImageOfTheDayUrl })
  var feed = xml.parse({ text: response.text })
  var sorted = list.sort({
    items: feed.rss.channel.item,
    by: current.title,
    direction: "asc"
  })
  var news = list.map({
    items: sorted,
    select: {
      title: current.title,
      description: current.description,
      imageUrl: current.enclosure.attr.url,
      link: current.link,
      pubDate: current.pubDate
    }
  })
  result.news = news
  result.count = news.length
  result.responseBytes = response.text.length
  result.items = sorted.length
  result.status = response.status
  return result
}
''',
}


def flow_code(variant):
    return OUTPUTS + "\n" + VARIANT_BODIES[variant]


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


def latest_rss_fixture():
    files = sorted(RESULT_DIR.glob("nasa-iotd-*.rss"))
    if not files:
        raise RuntimeError(f"No NASA RSS fixture found in {RESULT_DIR}")
    return files[-1]


def free_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(("", 0))
        return sock.getsockname()[1]


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
                return ".".join(str(part) for part in reversed(raw))
    except Exception:
        pass
    return "172.17.0.1"


def start_fixture_server():
    fixture = latest_rss_fixture()
    port = free_port()
    process = subprocess.Popen([
        "python3", "-m", "http.server", str(port),
        "--bind", "0.0.0.0",
        "--directory", str(RESULT_DIR),
    ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    url = f"http://{docker_gateway()}:{port}/{fixture.name}"
    deadline = time.time() + 10
    while time.time() < deadline:
        probe = subprocess.run([
            "docker", "exec", "c8o-agent-runtime",
            "curl", "--max-time", "2", "-fsS", "-o", "/dev/null", url,
        ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        if probe.returncode == 0:
            return process, url, fixture
        time.sleep(0.25)
    process.terminate()
    raise RuntimeError(f"Fixture server was not reachable from container: {url}")


def median(values):
    return statistics.median(values) if values else 0.0


def percentile(values, pct):
    if not values:
        return 0.0
    ordered = sorted(values)
    return ordered[int(round((len(ordered) - 1) * pct))]


def summarize(values):
    return {
        "count": len(values),
        "median": median(values),
        "mean": statistics.mean(values) if values else 0.0,
        "p95": percentile(values, 0.95),
        "min": min(values) if values else 0.0,
        "max": max(values) if values else 0.0,
    }


def run_variant(client, variant, revision, config_url):
    try:
        client.tool("code-set", {
            "project": PROJECT,
            "qname": QNAME,
            "revision": revision,
            "code": flow_code(variant),
            "maxDiagnostics": 25,
        })
    except Exception:
        client.tool("code-discard", {"project": PROJECT, "qname": QNAME}, allow_error=True)
        raise

    rows = []
    try:
        for i in range(1, WARMUP + ITERATIONS + 1):
            phase = "warmup" if i <= WARMUP else "measured"
            ordinal = i if phase == "warmup" else i - WARMUP
            started = time.perf_counter()
            result = client.tool("code-run", {
                "project": PROJECT,
                "qname": QNAME,
                "config": {"feeds": {"nasaImageOfTheDayUrl": config_url}},
            })
            wall_ms = round((time.perf_counter() - started) * 1000.0, 3)
            flow_result = result.get("result", {})
            row = {
                "variant": variant,
                "phase": phase,
                "ordinal": ordinal,
                "wall_ms": wall_ms,
                "status": flow_result.get("status", ""),
                "responseBytes": flow_result.get("responseBytes", ""),
                "items": flow_result.get("items", ""),
                "count": flow_result.get("count", ""),
            }
            rows.append(row)
            print(json.dumps(row), flush=True)
    finally:
        client.tool("code-discard", {"project": PROJECT, "qname": QNAME}, allow_error=True)
    return rows


def run_variant_benchmark(client, config_url):
    original = client.tool("code-get", {"project": PROJECT, "qname": QNAME})
    revision = original.get("revision", "")
    rows = []
    for variant in VARIANT_BODIES:
        rows.extend(run_variant(client, variant, revision, config_url))

    with VARIANT_CSV.open("w", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=[
            "variant", "phase", "ordinal", "wall_ms", "status",
            "responseBytes", "items", "count",
        ], lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)

    summaries = {}
    with VARIANT_SUMMARY_CSV.open("w", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=[
            "variant", "count", "median_ms", "mean_ms", "p95_ms", "min_ms", "max_ms",
        ], lineterminator="\n")
        writer.writeheader()
        for variant in VARIANT_BODIES:
            values = [
                float(row["wall_ms"])
                for row in rows
                if row["variant"] == variant and row["phase"] == "measured"
            ]
            stats = summarize(values)
            summaries[variant] = stats
            writer.writerow({
                "variant": variant,
                "count": stats["count"],
                "median_ms": f"{stats['median']:.3f}",
                "mean_ms": f"{stats['mean']:.3f}",
                "p95_ms": f"{stats['p95']:.3f}",
                "min_ms": f"{stats['min']:.3f}",
                "max_ms": f"{stats['max']:.3f}",
            })

    med = {variant: summaries[variant]["median"] for variant in summaries}
    derived = {
        "project": PROJECT,
        "qname": QNAME,
        "fixtureUrlFromContainer": config_url,
        "iterations": ITERATIONS,
        "warmup": WARMUP,
        "mediansMs": med,
        "deltasMs": {
            "flow_empty_envelope": med["empty"],
            "http_local_over_empty": med["http"] - med["empty"],
            "xml_over_http": med["http_xml"] - med["http"],
            "sort_over_xml": med["http_xml_sort"] - med["http_xml"],
            "map_over_sort": med["http_xml_sort_map"] - med["http_xml_sort"],
            "result_news_over_count_only": med["full_result"] - med["http_xml_sort_map"],
            "full_count_only_over_empty": med["http_xml_sort_map"] - med["empty"],
            "full_result_over_empty": med["full_result"] - med["empty"],
        },
    }
    VARIANT_DERIVED_JSON.write_text(json.dumps(derived, indent=2) + "\n", encoding="utf-8")
    return derived


def bridge_run_stats(cache_info):
    return cache_info.get("bridge", {}).get("methods", {}).get("run", {
        "calls": 0,
        "totalMs": 0.0,
        "avgMs": 0.0,
        "maxMs": 0.0,
    })


def stat_delta(after, before):
    return {
        "calls": int(after.get("calls", 0)) - int(before.get("calls", 0)),
        "totalMs": float(after.get("totalMs", 0.0)) - float(before.get("totalMs", 0.0)),
        "maxMsAfter": float(after.get("maxMs", 0.0)),
    }


def curl_flow(phase, ordinal):
    metrics = subprocess.check_output([
        "curl", "-sS", "-L",
        "-b", str(COOKIE_FILE),
        "-c", str(COOKIE_FILE),
        "-o", "/dev/null",
        "-w", "%{http_code},%{time_starttransfer},%{time_total},%{size_download}",
        FLOW_URL,
    ], text=True).strip()
    code, ttfb, total, size = metrics.split(",", 3)
    return {
        "phase": phase,
        "ordinal": ordinal,
        "http_code": code,
        "ttfb_ms": round(float(ttfb) * 1000.0, 3),
        "total_ms": round(float(total) * 1000.0, 3),
        "size_download": int(float(size)),
    }


def cleanup_http_context():
    subprocess.run([
        "curl", "-sS", "-L",
        "-b", str(COOKIE_FILE),
        "-c", str(COOKIE_FILE),
        "-o", "/dev/null",
        FLOW_URL + "&__removeContext=true&__removeSession=true",
    ], check=False)
    try:
        COOKIE_FILE.unlink()
    except FileNotFoundError:
        pass


def run_http_bridge_benchmark(client):
    rows = []
    try:
        for i in range(1, WARMUP + 1):
            row = curl_flow("warmup", i)
            rows.append(row)
            print(json.dumps(row), flush=True)

        s0 = bridge_run_stats(client.tool("flow-cache-info", {}))
        s1 = bridge_run_stats(client.tool("flow-cache-info", {}))
        cache_info_delta = stat_delta(s1, s0)

        for i in range(1, ITERATIONS + 1):
            row = curl_flow("measured", i)
            rows.append(row)
            print(json.dumps(row), flush=True)

        s2 = bridge_run_stats(client.tool("flow-cache-info", {}))
        measured_delta = stat_delta(s2, s1)
        app_bridge_calls = measured_delta["calls"] - cache_info_delta["calls"]
        app_bridge_total = measured_delta["totalMs"] - cache_info_delta["totalMs"]
        app_bridge_avg = app_bridge_total / app_bridge_calls if app_bridge_calls > 0 else 0.0

        with HTTP_CSV.open("w", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=[
                "phase", "ordinal", "http_code", "ttfb_ms", "total_ms", "size_download"
            ], lineterminator="\n")
            writer.writeheader()
            writer.writerows(rows)

        measured = [row for row in rows if row["phase"] == "measured" and row["http_code"] == "200"]
        summary = {
            "project": PROJECT,
            "qname": QNAME,
            "iterations": ITERATIONS,
            "warmup": WARMUP,
            "http": {
                "count": len(measured),
                "medianTtfbMs": median([row["ttfb_ms"] for row in measured]),
                "medianTotalMs": median([row["total_ms"] for row in measured]),
                "meanTotalMs": statistics.mean([row["total_ms"] for row in measured]) if measured else 0.0,
                "sizes": sorted(set(row["size_download"] for row in measured)),
            },
            "bridgeRunStats": {
                "s0": s0,
                "s1": s1,
                "s2": s2,
                "cacheInfoDelta": cache_info_delta,
                "measuredDeltaIncludingFinalCacheInfo": measured_delta,
                "estimatedAppRunCalls": app_bridge_calls,
                "estimatedAppRunTotalMs": round(app_bridge_total, 3),
                "estimatedAppRunAvgMs": round(app_bridge_avg, 3),
            },
        }
        HTTP_SUMMARY_JSON.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
        return summary
    finally:
        cleanup_http_context()


def main():
    ensure_result_dir()
    server = None
    client = McpClient()
    try:
        server, fixture_url, fixture = start_fixture_server()
        print(f"# fixture={fixture} url={fixture_url}", flush=True)
        derived = run_variant_benchmark(client, fixture_url)
        http_summary = run_http_bridge_benchmark(client)
        print("# variant_csv=" + str(VARIANT_CSV))
        print("# variant_summary=" + str(VARIANT_SUMMARY_CSV))
        print("# variant_derived=" + str(VARIANT_DERIVED_JSON))
        print("# http_csv=" + str(HTTP_CSV))
        print("# http_summary=" + str(HTTP_SUMMARY_JSON))
        print("# variant_medians_ms=" + json.dumps(derived["mediansMs"], sort_keys=True))
        print("# variant_deltas_ms=" + json.dumps(derived["deltasMs"], sort_keys=True))
        print("# measured_http_median_total_ms=%.3f" % http_summary["http"]["medianTotalMs"])
        print("# estimated_bridge_app_avg_ms=%.3f" % http_summary["bridgeRunStats"]["estimatedAppRunAvgMs"])
    finally:
        client.cleanup()
        if server is not None:
            server.terminate()
            try:
                server.wait(timeout=5)
            except subprocess.TimeoutExpired:
                server.kill()


if __name__ == "__main__":
    main()
