#!/usr/bin/env python3
"""Opt-in HTTP regression probe; use only the disposable #1235 test stack."""
import concurrent.futures
import http.cookiejar
import time
import urllib.error
import urllib.parse
import urllib.request


def client():
    jar = http.cookiejar.CookieJar()
    return jar, urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))


def call(opener, base, project="SessionTest", **parameters):
    url = base + "/projects/" + project + "/.json?" + urllib.parse.urlencode(parameters)
    try:
        response = opener.open(url, timeout=20)
    except urllib.error.HTTPError as error:
        response = error
    return response.headers, response.read().decode()


def run(base):
    invalid = [
        ("Missing", {"__sequence": "Check"}),
        ("SessionTest", {"__project": "Missing", "__sequence": "Check"}),
        ("SessionTest", {"__sequence": "Missing"}),
        ("SessionTest", {"__connector": "Missing"}),
        ("SessionTest", {"__transaction": "Missing"}),
    ]
    for project, parameters in invalid:
        jar, opener = client()
        for _ in range(3):
            headers, body = call(opener, base, project, **parameters)
            assert '"error"' in body, body
            assert "NullPointerException" not in body, body
            assert not headers.get_all("Set-Cookie"), headers
            assert not list(jar), list(jar)
    print(base, "PASS invalid targets: no cookie/session, meaningful errors", flush=True)

    for authenticate in (False, True):
        jar, opener = client()
        _, body = call(opener, base, __sequence="Login" if authenticate else "Anonymous", __removeContext="false")
        assert "keep" in body or "session-test-user" in body, body
        original = [(cookie.name, cookie.value) for cookie in jar]
        assert original, body
        for project, parameters in invalid:
            _, body = call(opener, base, project, **parameters)
            assert '"error"' in body, body
        # Exceeds the old automatic one-second timeout.
        time.sleep(2.2)
        _, body = call(opener, base, __sequence="Check" if authenticate else "State", __removeContext="false")
        assert '"error"' not in body and "keep" in body, body
        if authenticate:
            assert "session-test-user" in body, body
        assert original == [(cookie.name, cookie.value) for cookie in jar]

        cookie_header = "; ".join(name + "=" + value for name, value in original)

        def concurrent_call(index):
            worker = urllib.request.build_opener()
            worker.addheaders = [("Cookie", cookie_header)]
            if index % 2:
                return call(worker, base, "Missing", __sequence="Check")[1], True
            return call(worker, base, __sequence="State", __removeContext="false")[1], False

        with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
            for result, rejected in pool.map(concurrent_call, range(12)):
                assert ('"error"' in result) if rejected else ("keep" in result), result
        time.sleep(2.2)
        _, body = call(opener, base, __sequence="State", __removeContext="false")
        assert "keep" in body and '"error"' not in body, body
        print(base, "PASS", "authenticated" if authenticate else "anonymous", "state/context survives, including concurrent calls", flush=True)

        if authenticate:
            # SOAP target comes from the method name, not HTTP parameters.
            soap = b'<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body><Check xmlns="urn:SessionTest"/></soap:Body></soap:Envelope>'
            request = urllib.request.Request(base + "/projects/SessionTest/.wsl", soap,
                                             {"Content-Type": "text/xml; charset=utf-8", "SOAPAction": "Check"})
            with opener.open(request, timeout=20) as response:
                body = response.read().decode()
            assert "session-test-user" in body and "keep" in body, body
            print(base, "PASS SOAP target resolution", flush=True)

        call(opener, base, __sequence="State", __removeContext="true", __removeSession="1")
        time.sleep(2.2)
        _, body = call(opener, base, __sequence="State")
        assert "keep" not in body, body
        print(base, "PASS explicit session termination", flush=True)


if __name__ == "__main__":
    for port in (29135, 29136):
        run(f"http://localhost:{port}/convertigo")
