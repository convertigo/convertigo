#!/usr/bin/env python3
"""Measure SiteClipper concurrency and cookie propagation without printing cookies."""
import concurrent.futures
import http.cookiejar
import json
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request

PROJECT = '/convertigo/projects/SiteClipperStoreTest'
PORTS = (29591, 29592, 29593)


def client():
    jar = http.cookiejar.CookieJar()
    return jar, urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))


def get(opener, url):
    try:
        response = opener.open(url, timeout=30)
    except urllib.error.HTTPError as error:
        response = error
    return response.status, response.read().decode(errors='replace'), response.headers.get('X-Target-Cookie-Seen'), response.headers.get('X-Target-Call')


def redis_exists(session_id, context_name):
    key = f'eph-f4e5d644:context:{session_id}_{context_name}'
    commands = f'AUTH siteclipper-test-only\nEXISTS {key}\n'
    response = subprocess.run(['docker', 'exec', '-i', 'eph-f4e5d644-redis-1', 'redis-cli', '--raw'],
                              input=commands, text=True, capture_output=True, check=True)
    return response.stdout.splitlines()[-1] == '1'



def redis_cookie_count(session_id):
    key = f'eph-f4e5d644:context:{session_id}_clip'
    commands = f'AUTH siteclipper-test-only\nHGET {key} __meta:httpState\n'
    response = subprocess.run(['docker', 'exec', '-i', 'eph-f4e5d644-redis-1', 'redis-cli', '--raw'],
                              input=commands, text=True, capture_output=True, check=True)
    raw = response.stdout.split('\n', 1)[1].strip()
    return len(json.loads(raw).get('cookies', [])) if raw else 0

def run(first, second=None, mode='after'):
    jar, opener = client()
    base = f'http://127.0.0.1:{first}{PROJECT}'
    status, body, _, _ = get(opener, base + '/.json?__sequence=Initialize&__context=clip&__removeContext=false')
    assert status == 200, ('initialization', status)
    clip_url = json.loads(body)['url']
    assert list(jar), 'missing session cookie'
    if first != PORTS[0]:
        session_id = next(cookie.value for cookie in jar if cookie.name == 'SITECLIPPERTESTSESSION')
        assert redis_exists(session_id, 'clip'), 'initialized context missing from Redis'
    case = str(time.time_ns())
    page_url = clip_url + '?case=' + case
    status, body, seen, call = get(opener, page_url)
    assert status == 200 and len(re.findall(r'<script ', body)) == 8, ('page', status)
    assert seen == 'no' and call == '1', ('first cookie', seen, call)
    status, _, seen, call = get(opener, page_url)
    assert status == 200 and seen == 'no' and call == '2', ('second cookie', seen, call)
    other_url = page_url.replace(f':{first}/', f':{second}/') if second else page_url
    status, _, seen, call = get(opener, other_url)
    assert status == 200 and seen == 'yes' and call == '3', ('cross-engine cookie', seen, call, redis_cookie_count(session_id) if first != PORTS[0] else None)
    asset_root = clip_url.replace('/index.html', '/asset/')
    urls = [asset_root + f'{i}.js?case={case}' for i in range(8)]
    started = time.monotonic()
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        results = list(executor.map(lambda url: get(opener, url), urls))
    elapsed = time.monotonic() - started
    assert all(status == 200 and 'asset-ok' in body for status, body, _, _ in results), 'asset failure'
    assert (elapsed >= 1.5 if mode == 'before' else elapsed < 0.6), ('parallel duration', round(elapsed, 3))
    if first != PORTS[0]:
        absent_url = clip_url.replace('context=clip', 'context=absent')
        status, _, _, _ = get(opener, absent_url)
        assert status >= 400, ('absent context status', status)
        assert not redis_exists(session_id, 'absent'), 'SiteClipper created absent context'
        assert redis_exists(session_id, 'clip'), 'SiteClipper removed initialized context'
    state_port = second or first
    status, body, _, _ = get(opener, f'http://127.0.0.1:{state_port}{PROJECT}/.json?__sequence=State&__connector=void&__context=clip&__removeContext=false')
    assert status == 200 and json.loads(body).get('state') == '1', ('context retained', status)
    print(f'{first}->{state_port}: PASS {elapsed:.3f}s for 8 assets, cookie propagated, context retained', flush=True)


if __name__ == '__main__':
    mode = sys.argv[1] if len(sys.argv) > 1 else 'after'
    failed = False
    for start, follow in ((PORTS[0], None), (PORTS[1], None), (PORTS[1], PORTS[2])):
        try:
            run(start, follow, mode)
        except Exception as error:
            print(f'{start}->{follow or start}: FAIL {error}', flush=True)
            failed = True
    raise SystemExit(1 if failed else 0)
