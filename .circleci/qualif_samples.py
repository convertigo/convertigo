"""Read the public fields of JMeter results and keep diagnostic files private."""

from html import escape, unescape
import json
import re
from urllib.parse import quote, quote_plus
from xml.etree import ElementTree

CREDENTIAL = re.compile(rb'\b(?:github_pat_|gh[pousr]_)[A-Za-z0-9_]{20,}\b')
XML_KEYS = re.compile(
    rb'<(?:[\w.-]+:)?keys(?=[\s/>])[^>]*?(?:/>|>.*?</(?:[\w.-]+:)?keys\s*>)',
    re.DOTALL | re.IGNORECASE,
)
XPATH_PREFIXES = ('No Nodes Matched ', 'Specified XPath was found ')
XPATH_LITERAL = re.compile(r'''"(?:[^"]|"")*(?:"|\Z)|'(?:[^']|'')*(?:'|\Z)''')
RESPONSE_ASSERTION = re.compile(
    r'^Test failed: (?:text|code|message|headers|URL|document) expected to '
    r'(?:contain|match|equal|substrings?|not contain|not match|not equal)\b'
)
ACTUAL_VALUE = re.compile(r'\b(?:actual|got|obtained|received|but\s+was)\b', re.IGNORECASE)
MOBILE_ASSERTIONS = {
    'Mobile build launch was not accepted',
    'Mobile build still pending at the final check',
    'Mobile build still pending at the polling deadline',
    'Mobile build failed',
    'Mobile build status was unavailable',
    'Timed out waiting for the Android build to complete',
}


def secret_values(environment):
    token = environment.get('QUALIF_GITHUB_TOKEN')
    license_xml = environment.get('QUALIF_LAB_KEYS_XML')
    values = set(filter(None, (token, license_xml)))
    if license_xml:
        try:
            root = ElementTree.fromstring(license_xml)
        except ElementTree.ParseError:
            pass
        else:
            values.update(value.strip() for node in root.iter()
                          for value in (node.text or '', *node.attrib.values()) if len(value.strip()) >= 8)
    variants = {variant for value in values for variant in
                (value, escape(value), escape(value, quote=False), escape(escape(value)),
                 quote(value, safe=''), quote_plus(value), json.dumps(value, ensure_ascii=True)[1:-1])}
    return tuple(value.encode('utf-8') for value in sorted(variants, key=len, reverse=True))


def redact(data, secrets):
    for value in secrets:
        data = data.replace(value, b'[redacted]')
    return XML_KEYS.sub(b'[redacted license]', CREDENTIAL.sub(b'[redacted]', data))


def public_label(value, secrets):
    value = redact(value.encode('utf-8'), secrets).decode('utf-8')
    decoded = unescape(value)
    if (len(value) > 250 or any(not char.isprintable() for char in decoded)
            or re.search(r'[<>]|:/{2}|\?[^ ]*=|\b\w+=\S', decoded)):
        return 'Label omitted'
    return value


def configured_xpaths(*plans):
    xpaths = set()
    for plan in plans:
        try:
            root = ElementTree.parse(plan).getroot()
        except (OSError, ElementTree.ParseError):
            continue
        for node in root.iter('stringProp'):
            if node.get('name') == 'XPath.xpath' and node.text:
                xpaths.add(node.text)
    return xpaths


def assertion_message(raw, xpaths, secrets):
    for prefix in XPATH_PREFIXES:
        if raw.startswith(prefix):
            expected = raw[len(prefix):]
            # Static expectations can contain literals copied from recorded responses.
            if expected in xpaths and '${' not in expected:
                masked = XPATH_LITERAL.sub(lambda match: f'{match[0][0]}…{match[0][0]}', expected)
                safe = redact(masked.encode('utf-8'), secrets).decode('utf-8')
                return prefix + safe
            return prefix.strip()
    if raw in MOBILE_ASSERTIONS:
        return raw
    if match := RESPONSE_ASSERTION.match(raw):
        return match[0] + ' …'
    if re.match(r'^expected\b', raw, re.IGNORECASE):
        return 'Expected …, actual …' if ACTUAL_VALUE.search(raw) else 'Expected …'
    if ACTUAL_VALUE.match(raw):
        return 'Actual …'
    if raw:
        return 'Assertion failed (details in private JTL)'
    return 'Assertion failed'


def sample_row(element, plan, duration, xpaths, secrets):
    code = element.get('rc', '')
    if not re.fullmatch(r'(?:[0-9]{1,9}|Non HTTP response code: [\w.$]{1,150})', code, re.ASCII):
        code = ''
    return {
        'plan': public_label(plan, secrets),
        'label': public_label(element.get('lb', ''), secrets),
        'code': public_label(code, secrets),
        'duration': duration,
        'passed': element.get('s') == 'true',
        'assertions': [
            assertion_message(assertion.findtext('failureMessage') or '', xpaths, secrets)
            for assertion in element.findall('assertionResult')
            if assertion.findtext('failure') == 'true' or assertion.findtext('error') == 'true'
        ],
    }


def read_samples(path, allow_partial, xpaths, secrets):
    rows = []
    elapsed = 0
    has_timings = True
    try:
        with path.open('rb') as source:
            events = ElementTree.iterparse(source, events=('start', 'end'))
            _, root = next(events)
            if root.tag != 'testResults':
                raise ValueError
            depth = 1
            for event, element in events:
                if event == 'start':
                    depth += 1
                    continue
                if element.tag in ('httpSample', 'sample'):
                    if element.get('s') not in ('true', 'false'):
                        raise ValueError
                    timing = element.get('t', '')
                    duration = int(timing) if re.fullmatch(r'[0-9]{1,19}', timing) else None
                    elapsed += duration or 0
                    has_timings &= duration is not None
                    rows.append(sample_row(element, path.stem, duration, xpaths, secrets))
                # Keep assertion children until their enclosing sample is processed.
                if depth == 2 or element.tag in ('httpSample', 'sample'):
                    element.clear()
                depth -= 1
                if depth == 1:
                    root.clear()
    except (OSError, ValueError, ElementTree.ParseError):
        return (rows, elapsed if has_timings else None, False) if allow_partial else ([], 0, False)
    return rows, elapsed if has_timings else None, bool(rows)


def plan_samples(jtl, plans, allow_partial, secrets):
    xpaths = configured_xpaths(*(folder / (jtl.stem + '.jmx') for folder in
                                 (plans, plans / 'before', plans / 'after')))
    return read_samples(jtl, allow_partial, xpaths, secrets)
