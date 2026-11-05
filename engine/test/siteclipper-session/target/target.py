from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from threading import Lock
from urllib.parse import parse_qs, urlsplit
from time import sleep

counts = {}
lock = Lock()


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        url = urlsplit(self.path)
        case = parse_qs(url.query).get('case', ['default'])[0]
        with lock:
            counts[case] = counts.get(case, 0) + 1
            call = counts[case]
        if url.path.startswith('/asset/'):
            sleep(0.3)
            body = b'asset-ok\n'
            content_type = 'text/plain'
        else:
            assets = ''.join(f'<script src="/asset/{i}.js?case={case}"></script>' for i in range(8))
            body = (f'<html><body>{assets}</body></html>').encode()
            content_type = 'text/html'
        cookie_seen = 'clip-target=seen' in self.headers.get('Cookie', '')
        self.send_response(200)
        self.send_header('Content-Type', content_type)
        self.send_header('X-Target-Call', str(call))
        if call == 2:
            self.send_header('Set-Cookie', 'clip-target=seen; Path=/')
        self.send_header('X-Target-Cookie-Seen', 'yes' if cookie_seen else 'no')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)


ThreadingHTTPServer(('0.0.0.0', 8080), Handler).serve_forever()
