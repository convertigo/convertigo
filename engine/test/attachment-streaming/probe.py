#!/usr/bin/env python3
"""Streams downloads into SHA-256, never into a file-sized Python buffer."""
import concurrent.futures
import hashlib
import sys
import urllib.request

SIZE = 1024 * 1024 * 1024


def expected_digest():
    digest = hashlib.sha256()
    digest.update(bytes([1, 2, 3, 4]))
    block = bytes(64 * 1024)
    remaining = SIZE - 8
    while remaining:
        count = min(remaining, len(block))
        digest.update(block[:count])
        remaining -= count
    digest.update(bytes([5, 6, 7, 8]))
    return digest.hexdigest()


def download(base, expected, sequence="Local", route=None):
    url = base + (route if route else "/projects/AttachmentStreamingTest/.bin?__removeSession=true&__sequence=" + sequence)
    digest = hashlib.sha256()
    count = 0
    with urllib.request.urlopen(url, timeout=180) as response:
        assert response.status == 200, response.status
        headers = response.headers
        while block := response.read(64 * 1024):
            digest.update(block)
            count += len(block)
    assert digest.hexdigest() == expected, (url, count, digest.hexdigest(), expected)
    assert count == (SIZE if sequence == "Local" else 5), count
    assert int(headers["Content-Length"]) == count, headers
    assert headers["Content-Type"] == "application/octet-stream", headers
    assert headers["Content-Disposition"] == ('attachment; filename="payload.bin"'
                                               if sequence == "Local" else 'attachment; filename="inline.bin"'), headers
    print("PASS", route or sequence, count, "bytes, SHA-256", expected, flush=True)


if __name__ == "__main__":
    base = sys.argv[1].rstrip("/")
    expected = expected_digest()
    download(base, expected)
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        list(pool.map(lambda _: download(base, expected), range(3)))
    download(base, hashlib.sha256(bytes([0, 1, 255, 13, 10])).hexdigest(), "Base64")
    if "--rest" in sys.argv[2:]:
        for prefix in ("/openapi", "/api"):
            download(base, expected, route=prefix + "/attachment-streaming/local")
            with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
                list(pool.map(lambda _: download(base, expected, route=prefix + "/attachment-streaming/local"), range(3)))
            download(base, hashlib.sha256(bytes([0, 1, 255, 13, 10])).hexdigest(), "Base64",
                     route=prefix + "/attachment-streaming/inline")
            for output in ("json", "xml"):
                with urllib.request.urlopen(base + prefix + "/attachment-streaming/" + output, timeout=20) as response:
                    content = response.read(65536).decode()
                    assert response.status == 200
                    assert "AAH/DQo=" in content and "inline.bin" in content, content
                    assert output in response.headers["Content-Type"], response.headers
                print("PASS", prefix, output, "keeps the serialized attachment document", flush=True)
