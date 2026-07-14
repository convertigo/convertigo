#!/usr/bin/env python3
"""Serialize Flow MCP calls through one Convertigo HTTP session.

The local Standard Edition runtime accepts five concurrent sessions, while the
Codex Streamable HTTP client can initialize several transports concurrently.
This diagnostic proxy keeps a single backend cookie jar and serializes calls so
authoring benchmarks measure Flow rather than licence-session exhaustion.
"""

import argparse
import http.cookiejar
import threading
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


HOP_BY_HOP_HEADERS = {
    "connection",
    "content-length",
    "host",
    "keep-alive",
    "proxy-authenticate",
    "proxy-authorization",
    "te",
    "trailer",
    "transfer-encoding",
    "upgrade",
}


class Backend:
    def __init__(self, url):
        self.url = url
        self.lock = threading.Lock()
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar())
        )

    def request(self, method, headers, body):
        forwarded = {
            key: value
            for key, value in headers.items()
            if key.lower() not in HOP_BY_HOP_HEADERS
        }
        request = urllib.request.Request(
            self.url,
            data=body if method not in {"GET", "HEAD"} else None,
            headers=forwarded,
            method=method,
        )
        with self.lock:
            try:
                response = self.opener.open(request, timeout=300)
            except urllib.error.HTTPError as error:
                response = error
            with response:
                return response.status, response.headers, response.read()


class ProxyHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_GET(self):
        self._proxy("GET")

    def do_POST(self):
        self._proxy("POST")

    def do_DELETE(self):
        self._proxy("DELETE")

    def _proxy(self, method):
        if self.path.rstrip("/"):
            payload = b"Not found"
            self.send_response(404)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(payload)))
            self.end_headers()
            self.wfile.write(payload)
            return

        length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(length) if length else None
        try:
            status, headers, payload = self.server.backend.request(
                method, self.headers, body
            )
        except Exception as error:
            payload = ("Flow MCP proxy error: %s" % error).encode("utf-8")
            self.send_response(502)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(payload)))
            self.end_headers()
            self.wfile.write(payload)
            return

        self.send_response(status)
        for key, value in headers.items():
            if key.lower() not in HOP_BY_HOP_HEADERS and key.lower() != "set-cookie":
                self.send_header(key, value)
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        if method != "HEAD":
            self.wfile.write(payload)

    def log_message(self, message, *args):
        print("%s - %s" % (self.log_date_time_string(), message % args), flush=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--listen", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=19081)
    parser.add_argument(
        "--backend",
        default="http://127.0.0.1:19080/convertigo/api/flow-mcp",
    )
    args = parser.parse_args()

    server = ThreadingHTTPServer((args.listen, args.port), ProxyHandler)
    server.backend = Backend(args.backend)
    print("Flow MCP session proxy listening on %s:%d" % (args.listen, args.port), flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
