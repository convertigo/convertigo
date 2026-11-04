#!/usr/bin/env python3
"""Probe the disposable SiteClipper session stack without printing session cookies."""
import http.cookiejar
import json
import urllib.error
import urllib.parse
import urllib.request

PROJECT = '/convertigo/projects/SiteClipperStoreTest'


def client():
    jar = http.cookiejar.CookieJar()
    return jar, urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))


def get(opener, url):
    try:
        response = opener.open(url, timeout=30)
    except urllib.error.HTTPError as error:
        response = error
    return response.status, response.read().decode(errors='replace')


def run(first, second=None):
    jar, opener = client()
    base = f'http://127.0.0.1:{first}{PROJECT}'
    status, body = get(opener, base + '/.json?__sequence=Initialize&__context=clip&__removeContext=false')
    assert status == 200, (first, 'initialization status', status)
    payload = json.loads(body)
    assert 'url' in payload, (first, 'initialization response', payload)
    assert list(jar), (first, 'missing session cookie')
    clip_url = payload['url']
    for port_index, port in enumerate((first, second) if second is not None else (first,)):
        current_url = clip_url.replace(f':{first}/', f':{port}/')
        for index in (1, 2):
            status, body = get(opener, current_url)
            assert status == 200 and 'target-ok' in body, (first, port, index, status, body[:180])
            expected_cookie = 'cookie=missing' if port_index == 0 and index == 1 else 'cookie=present'
            assert expected_cookie in body, (first, port, index, 'target cookie', body[:180])
    state_port = second or first
    status, body = get(opener, f'http://127.0.0.1:{state_port}{PROJECT}/.json?__sequence=State&__connector=void&__context=clip&__removeContext=false')
    assert status == 200 and json.loads(body).get('state') == '1', (first, second, 'following transaction', status, body[:180])
    print(f'{first}->{state_port}: PASS initialization, two calls per engine, target cookie, following transaction', flush=True)


if __name__ == '__main__':
    failed = False
    for start, follow in ((29481, None), (29482, 29483)):
        try:
            run(start, follow)
        except Exception as error:
            print(f'{start}->{follow or start}: FAIL {error}', flush=True)
            failed = True
    raise SystemExit(1 if failed else 0)
