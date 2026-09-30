"""Source-backed admin service bridges and bounded, directed navigation."""

from collections import Counter, deque
import json
from pathlib import Path


SERVICE_PACKAGE = 'com.twinsoft.convertigo.engine.admin.services.'
DISPATCH_FILE = 'engine/src/com/twinsoft/convertigo/engine/admin/AdminServlet.java'


def descendants(node):
    yield node
    for child in node.named_children:
        yield from descendants(child)


def java_services(files, java_nodes):
    from tree_sitter import Language, Parser
    import tree_sitter_java
    parser = Parser(Language(tree_sitter_java.language()))
    endpoints, warnings = {}, []
    by_file = {}
    for node in java_nodes:
        source = node.get('source_file', '')
        by_file.setdefault(source, []).append(node)
        node['language'] = 'java'
        node['entity_type'] = ('type' if node.get('_callable_class') else
                               'method' if node.get('_callable') else
                               'module' if node.get('label', '').endswith('.java') else 'symbol')
    dispatch = parser.parse(files.get(DISPATCH_FILE, b'')).root_node
    dispatch_package = next((child.named_children[0].text.decode() for child in dispatch.named_children
                             if child.type == 'package_declaration'), '')
    valid_dispatch = dispatch_package + '.services.' == SERVICE_PACKAGE and any(
        node.type == 'method_invocation'
        and node.child_by_field_name('object') is not None
        and node.child_by_field_name('object').text == b'Class'
        and node.child_by_field_name('name').text == b'forName'
        and b'".services."' in node.child_by_field_name('arguments').text
        for node in descendants(dispatch))
    if not valid_dispatch:
        warnings.append({'source_file': DISPATCH_FILE, 'reason': 'admin dispatch rule not verified; no HTTP bridges'})
    declarations = {'class_declaration': 'class', 'interface_declaration': 'interface',
                    'enum_declaration': 'enum', 'record_declaration': 'record',
                    'annotation_type_declaration': 'annotation',
                    'method_declaration': 'method', 'constructor_declaration': 'constructor'}

    def declared_kinds(node):
        if node.type in declarations:
            name = node.child_by_field_name('name')
            if name:
                yield name.text.decode(), declarations[node.type]
        if node.type in {'method_declaration', 'constructor_declaration'}:
            return
        for child in node.named_children:
            yield from declared_kinds(child)

    for name, data in files.items():
        if not name.endswith('.java'):
            continue
        tree = parser.parse(data).root_node
        kinds = {}
        for label, kind in declared_kinds(tree):
            kinds.setdefault(label, set()).add(kind)
        for node in by_file.get(name, []):
            if node.get('_callable') or node.get('_callable_class'):
                candidates = kinds.get(node.get('label', '').split('.')[-1], set())
                member_kinds = {'method', 'constructor'}
                matches = candidates - member_kinds if node.get('_callable_class') else candidates & member_kinds
                if len(matches) == 1:
                    node['entity_type'] = next(iter(matches))
        if '/engine/admin/services/' not in name:
            continue
        if tree.has_error:
            warnings.append({'source_file': name, 'reason': 'Java service parse error; handler omitted'})
            continue
        package = next((n.named_children[0].text.decode() for n in tree.named_children
                        if n.type == 'package_declaration'), '')
        if not package.startswith(SERVICE_PACKAGE.rstrip('.')):
            continue
        for cls in [n for n in tree.named_children if n.type == 'class_declaration']:
            modifiers = next((n for n in cls.named_children if n.type == 'modifiers'), None)
            annotation = next((n for n in (modifiers.named_children if modifiers else [])
                               if n.type == 'annotation' and n.child_by_field_name('name').text == b'ServiceDefinition'), None)
            if annotation is None:
                continue
            class_name = cls.child_by_field_name('name').text.decode()
            fqn = package + '.' + class_name
            service = fqn.removeprefix(SERVICE_PACKAGE)
            candidates = [n for n in by_file.get(name, []) if n.get('label') == class_name]
            if len(candidates) != 1:
                warnings.append({'source_file': name, 'reason': 'handler class missing or ambiguous in Java graph'})
                continue
            node = candidates[0]
            node.update(entity_type='class', qualified_name=fqn)
            roles = []
            for pair in descendants(annotation):
                if pair.type == 'element_value_pair' and pair.child_by_field_name('key').text == b'roles':
                    roles = [n.child_by_field_name('field').text.decode() for n in descendants(pair)
                             if n.type == 'field_access']
            superclass = cls.child_by_field_name('superclass')
            base = superclass.text.decode().removeprefix('extends').strip() if superclass else ''
            body = cls.child_by_field_name('body')
            entry_points = [{'name': method.child_by_field_name('name').text.decode(),
                             'source_location': f'L{method.child_by_field_name("name").start_point.row + 1}'}
                            for method in body.named_children if method.type == 'method_declaration'
                            and method.child_by_field_name('name').text in {b'getServiceResult', b'run'}]
            if valid_dispatch:
                endpoints[service] = {'id': f'http:{service}', 'label': service,
                                      'entity_type': 'http_service', 'language': 'http/java',
                                      'file_type': 'code', 'source_file': name,
                                      'source_location': f'L{cls.child_by_field_name("name").start_point.row + 1}',
                                      'confidence': 'EXTRACTED', '_origin': 'ast',
                                      'handler': node['id'], 'handler_class': fqn,
                                      'base_class': base, 'roles': roles,
                                      'entry_points': entry_points,
                                      'dispatch_source': DISPATCH_FILE}
    return endpoints, warnings


def connect(web, endpoints, warnings):
    nodes = web['nodes'] + list(endpoints.values())
    edges = list(web['edges'])
    unresolved, resolved = [], []
    for request in web['requests']:
        if not request.get('services'):
            unresolved.append(request)
            continue
        for name in request['services']:
            endpoint = endpoints.get(name)
            if endpoint is None:
                unresolved.append(dict(request, service=name, reason='no verified Java admin handler'))
                continue
            edge = dict(request, source=request['owner'], target=endpoint['id'], service=name,
                        weight=1, _origin='ast', dispatch_source=DISPATCH_FILE)
            edges.append(edge)
            resolved.append(edge)
    for endpoint in endpoints.values():
        edges.append({'source': endpoint['id'], 'target': endpoint['handler'],
                      'relation': 'handled_by', 'confidence': 'EXTRACTED', 'weight': 1,
                      'source_file': endpoint['source_file'], 'source_location': endpoint['source_location'],
                      'evidence': 'AdminServlet reflective dispatch by package and class name', '_origin': 'ast'})
    return {'nodes': nodes, 'edges': edges, 'unresolved': unresolved,
            'warnings': web['warnings'] + warnings,
            'metrics': {'web_files': web['files'], 'svelte_components': web['components'],
                        'web_parse_errors': web['parse_errors'], 'parsers': web['parsers'],
                        'java_handlers': len(endpoints),
                        'request_expressions': sum(not r.get('derived') for r in web['requests']),
                        'derived_request_candidates': sum(bool(r.get('derived')) for r in web['requests']),
                        'resolved_service_edges': len(resolved), 'unresolved_expressions': len(unresolved),
                        'relations': dict(Counter(e['relation'] for e in resolved)),
                        'warning_reasons': dict(Counter(w['reason'] for w in web['warnings'] + warnings))}}


# Imports do not imply a call. Component/module roots expose their local functions;
# an imported function only follows calls, nested callbacks, and default candidates.
FORWARD = {'contains', 'calls', 'default_callback', 'passes_callback'}
RPC = {'calls_http', 'configures_service', 'references_resource'}


def labeled_path(route, nodes):
    return [dict(edge, from_label=nodes.get(edge['source'], {}).get('label', edge['source']),
                 to_label=nodes.get(edge['target'], {}).get('label', edge['target'])) for edge in route]


def select(index, selector, kind=None):
    nodes = index['nodes']
    if kind:
        nodes = [n for n in nodes if n['entity_type'] == kind]
    exact = [n for n in nodes if selector in (n['id'], n['label'], n.get('source_file'))]
    roots = [n for n in exact if n['entity_type'] in {'component', 'module', 'http_service'}]
    if roots and not kind:
        exact = roots
    if len(exact) != 1:
        candidates = exact or [n for n in nodes if selector.lower() in n['label'].lower()][:12]
        choices = '\n'.join(f'  {n["id"]} ({n["entity_type"]})' for n in candidates)
        raise ValueError(f'Select exactly one symbol ({len(exact)} exact matches):\n{choices}')
    return exact[0]


def services(index, selector, limit=30):
    root = select(index, selector)
    nodes = {n['id']: n for n in index['nodes']}
    outgoing = {}
    for edge in index['edges']:
        outgoing.setdefault(edge['source'], []).append(edge)
    paths = {root['id']: []}
    queue = deque([root['id']])
    hits = {}
    while queue:
        owner = queue.popleft()
        for edge in outgoing.get(owner, []):
            if edge['relation'] in RPC:
                key = (edge['target'], edge['relation'])
                hits.setdefault(key, paths[owner] + [edge])
            elif edge['relation'] in FORWARD and edge['target'] not in paths:
                if nodes.get(edge['target'], {}).get('transport_boundary'):
                    continue
                paths[edge['target']] = paths[owner] + [edge]
                queue.append(edge['target'])
    ordered = sorted(hits.items())
    return {'root': root, 'total': len(ordered), 'truncated': len(ordered) > limit,
            'results': [{'service': nodes[key[0]], 'relation': key[1], 'path': labeled_path(route, nodes)}
                        for key, route in ordered[:limit]],
            'unresolved': [r for r in index['unresolved'] if r['owner'] in paths],
            'warnings': [w for w in index['warnings'] if w['source_file'] in
                         {nodes[n]['source_file'] for n in paths if n in nodes}]}


def impact(index, selector, limit=30):
    root = select(index, selector, 'http_service')
    incoming = {}
    nodes = {n['id']: n for n in index['nodes']}
    for edge in index['edges']:
        if edge['relation'] in FORWARD | RPC:
            incoming.setdefault(edge['target'], []).append(edge)
    paths, queue = {root['id']: []}, deque([root['id']])
    while queue:
        target = queue.popleft()
        for edge in incoming.get(target, []):
            if nodes.get(edge['source'], {}).get('transport_boundary'):
                continue
            if edge['source'] not in paths:
                paths[edge['source']] = [edge] + paths[target]
                queue.append(edge['source'])
    callers = sorted((nodes[n] for n in paths if n != root['id'] and n in nodes
                      and nodes[n]['entity_type'] in {'component', 'module'}), key=lambda n: n['source_file'])
    return {'root': root, 'total': len(callers), 'truncated': len(callers) > limit,
            'results': [{'caller': n, 'path': labeled_path(paths[n['id']], nodes)} for n in callers[:limit]]}


def describe(result):
    lines = [f'{result["root"]["label"]} [{result["root"]["entity_type"]}]',
             f'{result["total"]} static potential relations; not observed runtime traffic.']
    for hit in result['results']:
        node = hit.get('service', hit.get('caller'))
        lines.append(f'\n{node["label"]} [{node["entity_type"]}] {node["source_file"]}:{node["source_location"]}')
        for edge in hit['path']:
            lines.append(f'  {edge["from_label"]} -> {edge["to_label"]}: {edge["relation"]} '
                         f'[{edge["confidence"]}] {edge["source_file"]}:{edge["source_location"]}')
        for entry in node.get('entry_points', []):
            lines.append(f'  Java entry: {entry["name"]} {node["source_file"]}:{entry["source_location"]}')
    unresolved = result.get('unresolved', [])
    lines.append(f'\nUnresolved reachable expressions: {len(unresolved)}')
    for row in unresolved[:10]:
        lines.append(f'  {row["source_file"]}:{row["source_location"]}: {row["expression"]} ({row["reason"]})')
    for row in result.get('warnings', [])[:5]:
        lines.append(f'  Warning {row["source_file"]}:{row.get("line", "?")}: {row["reason"]}')
    if result['truncated']:
        lines.append('Results truncated; increase --limit or select a specific function ID.')
    return '\n'.join(lines)


def load(path):
    return json.loads(Path(path).read_text(encoding='utf-8'))
