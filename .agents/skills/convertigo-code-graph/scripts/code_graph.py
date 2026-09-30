#!/usr/bin/env python3
"""Build a disposable, code-only Graphify index without touching source files."""

import argparse
from collections import Counter
from datetime import datetime, timezone
import hashlib
import importlib.metadata
import json
import os
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import sys
import tempfile
import time


GRAPHIFY_VERSION = '0.9.51'
SCHEMA_VERSION = 2
WEB_SUFFIXES = {'.js', '.jsx', '.ts', '.tsx', '.mjs', '.cjs', '.mts', '.cts', '.svelte', '.json'}
ALIAS_CONFIG = 'convertigo-studio-web/.svelte-kit/tsconfig.json'


def git(repo, *args):
    return subprocess.check_output(['git', '-C', str(repo), *args])


def is_source(name):
    path = PurePosixPath(name)
    return path.suffix == '.java' or (
        name.startswith('convertigo-studio-web/') and path.suffix in WEB_SUFFIXES)


def read_corpus(repo):
    names = sorted(os.fsdecode(name) for name in git(repo, 'ls-files', '-z').split(b'\0')
                   if name and is_source(os.fsdecode(name)))
    files = {}
    for name in [*names, ALIAS_CONFIG]:
        path = repo / name
        if path.is_symlink():
            raise ValueError(f'Refusing symlink in graph inputs: {name}')
        if path.is_file():
            files[name] = path.read_bytes()
    return files


def fingerprint(files):
    digest = hashlib.sha256()
    for name, data in sorted(files.items()):
        digest.update(os.fsencode(name) + b'\0' + hashlib.sha256(data).digest())
    return digest.hexdigest()


def recipe_fingerprint():
    scripts = Path(__file__).resolve().parent
    return fingerprint({p.name: p.read_bytes() for p in scripts.iterdir()
                        if p.suffix in {'.py', '.cjs', '.html'}})


def snapshot(files, destination):
    for name, data in files.items():
        path = destination / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)


def source_area(name):
    parts = PurePosixPath(name).parts
    namespace = ('com', 'twinsoft', 'convertigo')
    for offset in range(len(parts) - 2):
        if parts[offset:offset + 3] == namespace:
            return '.'.join(parts[offset + 3:-1]) or 'convertigo'
    if parts and parts[0] == 'convertigo-studio-web':
        if 'src' in parts:
            parts = parts[parts.index('src') + 1:-1]
            if parts and parts[0] == 'lib':
                parts = parts[1:]
            area = '.'.join(part for part in parts if not part.startswith('('))
            return f'web.{area}' if area else 'web'
        return 'web.config'
    return '.'.join(parts[:-1]) or 'external'


def named_communities(graph, communities):
    from graphify.cluster import label_communities_by_hub
    hubs = label_communities_by_hub(graph, communities)
    labels = {}
    for cid, members in communities.items():
        sources = {graph.nodes[node].get('source_file', '') for node in members if node in graph}
        areas = Counter(source_area(name) for name in sources
                        if name.endswith('.java') or name.startswith('convertigo-studio-web/'))
        area = min(areas, key=lambda key: (-areas[key], key)) if areas else 'external'
        labels[cid] = f'{area} / {hubs[cid]}'
    duplicate_names = Counter(labels.values())
    return {cid: f'{label} [{cid}]' if duplicate_names[label] > 1 else label
            for cid, label in labels.items()}


def package_groups(graph):
    families = {}
    for node, data in graph.nodes(data=True):
        source = data.get('source_file', '')
        if not (source.endswith('.java') or source.startswith('convertigo-studio-web/')):
            continue
        area = source_area(source)
        depth = 3 if area.startswith('web.routes.') else 2
        family = '.'.join(area.split('.')[:depth])
        families.setdefault(family, []).append(node)
    names = dict(enumerate(sorted(families)))
    return {cid: families[name] for cid, name in names.items()}, names


def write_json(path, data):
    path.write_text(json.dumps(data, indent=2, ensure_ascii=True) + '\n', encoding='utf-8')


def render_graph(raw, output, repo, commit, communities=None):
    from graphify.analyze import god_nodes, suggest_questions, surprising_connections
    from graphify.build import build_from_json
    from graphify.cluster import cluster, community_member_sigs, score_all
    from graphify.export import to_html, to_json
    from graphify.report import generate

    graph = build_from_json(raw, directed=bool(raw.get('directed', False)))
    if communities is None:
        communities = cluster(graph)
    labels = named_communities(graph, communities)
    output.mkdir(parents=True, exist_ok=True)
    if not to_json(graph, communities, str(output / 'graph.json'),
                   built_at_commit=commit, community_labels=labels):
        raise RuntimeError('Graphify refused the graph export')
    report = generate(graph, communities, score_all(graph, communities), labels,
                      god_nodes(graph), surprising_connections(graph, communities),
                      {'warning': 'Local AST-only index; community names are structural hints.'},
                      {'input': 0, 'output': 0}, str(repo),
                      suggested_questions=suggest_questions(graph, communities, labels))
    report += ('\n## Local Freshness\n\n'
               'Use the repository code-graph recipe `status`/`build` commands. '
               'Do not use incremental update/watch for this pinned version.\n')
    (output / 'GRAPH_REPORT.md').write_text(report, encoding='utf-8')
    write_json(output / '.graphify_labels.json', labels)
    write_json(output / '.graphify_labels.json.sig', community_member_sigs(communities))
    to_html(graph, communities, str(output / 'graph.html'),
            community_labels=labels, node_limit=5000)
    groups, group_names = package_groups(graph)
    if len(groups) > 1:
        to_html(graph, groups, str(output / 'overview.html'),
                community_labels=group_names, node_limit=1)
    return {'nodes': graph.number_of_nodes(), 'edges': graph.number_of_edges(),
            'communities': len(communities), 'package_groups': len(groups)}


def publish(generated, target):
    previous = generated.parent / 'previous-index'
    if target.exists():
        target.rename(previous)
    try:
        generated.rename(target)
    except OSError:
        if previous.exists():
            previous.rename(target)
        raise


def status(repo, quiet=False):
    output = repo / 'build/graphify/graphify-out'
    state_path = output / 'index-state.json'
    if not state_path.is_file() or not (output / 'graph.json').is_file():
        if not quiet:
            print('ABSENT: generate the optional local index when needed.')
        return 1
    state = json.loads(state_path.read_text(encoding='utf-8'))
    fresh = (state.get('schema_version') == SCHEMA_VERSION
             and state.get('graphify_version') == GRAPHIFY_VERSION
             and state.get('recipe_fingerprint') == recipe_fingerprint()
             and state.get('source_fingerprint') == fingerprint(read_corpus(repo)))
    if not quiet:
        print('FRESH: selected source contents and recipe match.' if fresh else
              'STALE: rebuild the local index before relying on it.')
    return 0 if fresh else 1


def build(repo):
    version = importlib.metadata.version('graphifyy')
    if version != GRAPHIFY_VERSION:
        raise ValueError(f'Use the recipe pin: graphifyy=={GRAPHIFY_VERSION}, not {version}')
    base = repo / 'build/graphify'
    for path in [repo / 'build', base, base / 'graphify-out']:
        if path.is_symlink():
            raise ValueError(f'Refusing symlink at output path: {path}')
    base.mkdir(parents=True, exist_ok=True)
    files = read_corpus(repo)
    code = {name: data for name, data in files.items() if name != ALIAS_CONFIG}
    if not code:
        raise ValueError('No tracked Java or Studio web code found')
    if ALIAS_CONFIG not in files:
        print('NOTE: generated SvelteKit tsconfig absent; standard $lib resolution remains available.',
              flush=True)
    state = {'schema_version': SCHEMA_VERSION, 'graphify_version': version,
             'commit': git(repo, 'rev-parse', 'HEAD').decode().strip(),
             'source_fingerprint': fingerprint(files), 'recipe_fingerprint': recipe_fingerprint(),
             'files': len(code),
             'by_extension': dict(Counter(PurePosixPath(name).suffix for name in code)),
             'source_bytes': sum(map(len, code.values())),
             'alias_config_preserved': ALIAS_CONFIG in files,
             'created_at_utc': datetime.now(timezone.utc).isoformat()}
    started = time.perf_counter()
    with tempfile.TemporaryDirectory(prefix='run-', dir=base) as temporary:
        root = Path(temporary)
        source = root / 'source'
        snapshot(files, source)
        web_names = [name for name in code if name.startswith('convertigo-studio-web/')]
        write_json(root / 'web-files.json', web_names)
        print('Parsing Studio web with the installed Svelte/Babel parsers...', flush=True)
        subprocess.run(['node', str(Path(__file__).with_name('web_graph.cjs')), str(source),
                        str(repo), str(root / 'web-files.json'), str(root / 'web.json')], check=True)
        web = json.loads((root / 'web.json').read_text(encoding='utf-8'))
        java_source = root / 'java'
        snapshot({name: data for name, data in code.items() if name.endswith('.java')}, java_source)
        env = os.environ.copy()
        env.update(GRAPHIFY_OUT='graphify-out', GRAPHIFY_NO_AUTO_REFRESH='1',
                   GRAPHIFY_QUERY_LOG_DISABLE='1')
        print('Extracting Java in an isolated snapshot...', flush=True)
        subprocess.run([sys.executable, '-m', 'graphify', 'extract', str(java_source),
                        '--code-only', '--no-cluster', '--max-workers', '4',
                        '--out', str(root / 'extract')], cwd=root, env=env, check=True)
        raw = json.loads((root / 'extract/graphify-out/graph.json').read_text(encoding='utf-8'))
        # Normalize the isolated Java paths before joining them to repository RPC facts.
        for node in raw['nodes']:
            location = node.get('source_file', '')
            if location.startswith(str(java_source) + os.sep):
                node['source_file'] = Path(location).relative_to(java_source).as_posix()
        from service_graph import java_services, connect
        endpoints, warnings = java_services(files, raw['nodes'])
        service_index = connect(web, endpoints, warnings)
        service_index['provenance'] = {key: state[key] for key in
                                       ['commit', 'created_at_utc', 'source_fingerprint']}
        handlers = {e['handler'] for e in endpoints.values()}
        service_index['nodes'].extend(n for n in raw['nodes'] if n['id'] in handlers)
        service_ids = {n['id'] for n in service_index['nodes']}
        dangling = sum(e['source'] not in service_ids or e['target'] not in service_ids
                       for e in service_index['edges'])
        service_index['metrics']['dangling_edges'] = dangling
        if dangling:
            raise RuntimeError(f'Service graph has {dangling} dangling relations; refusing publication.')
        raw['nodes'].extend(web['nodes'] + list(endpoints.values()))
        edge_key = 'links' if 'links' in raw else 'edges'
        raw[edge_key].extend(service_index['edges'])
        from graphify.diagnostics import diagnose_extraction
        health = diagnose_extraction(raw, directed=True, root=str(java_source))
        generated = root / 'published'
        state['graph'] = render_graph(raw, generated, repo, state['commit'])
        write_json(generated / 'service-map.json', service_index)
        state['services'] = service_index['metrics']
        template = Path(__file__).with_name('services.html').read_text(encoding='utf-8')
        payload = json.dumps(service_index, ensure_ascii=True).replace('<', '\\u003c')
        (generated / 'services.html').write_text(template.replace('__SERVICE_DATA__', payload), encoding='utf-8')
        state['seconds'] = time.perf_counter() - started
        write_json(generated / 'health.json', health)
        write_json(generated / 'index-state.json', state)
        publish(generated, base / 'graphify-out')
    print(f'Local index ready in {state["seconds"]:.1f}s: {base / "graphify-out"}')
    print(json.dumps(state['services'], indent=2))
    print('Open services.html for source-backed detail; review unresolved expressions and health.json.')
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['status', 'build', 'services', 'impact'])
    parser.add_argument('selector', nargs='?')
    parser.add_argument('--limit', type=int, default=30)
    parser.add_argument('--json', action='store_true')
    parser.add_argument('--repo', type=Path, default=Path.cwd())
    args = parser.parse_args()
    try:
        repo = Path(git(args.repo, 'rev-parse', '--show-toplevel').decode().strip())
        if args.command == 'status':
            return status(repo)
        if args.command == 'build':
            return build(repo)
        if status(repo, quiet=True):
            raise ValueError('Index absent or stale; rebuild before querying.')
        if not args.selector or not 1 <= args.limit <= 200:
            raise ValueError('Provide an exact file/symbol/service selector and --limit in 1..200.')
        from service_graph import load, services, impact, describe
        index = load(repo / 'build/graphify/graphify-out/service-map.json')
        result = (services if args.command == 'services' else impact)(index, args.selector, args.limit)
        print(json.dumps(result, indent=2) if args.json else describe(result))
        return 0
    except (OSError, ValueError, RuntimeError, subprocess.CalledProcessError,
            importlib.metadata.PackageNotFoundError) as error:
        print(f'ERROR: {error}', file=sys.stderr)
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
