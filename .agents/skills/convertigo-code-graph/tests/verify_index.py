"""Opt-in acceptance checks against a freshly generated real Convertigo index."""

import json
from pathlib import Path
import sys
import time

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
from code_graph import status
from service_graph import load, services, impact


def verify(repo):
    assert status(repo, quiet=True) == 0, 'Index must be fresh'
    index = load(repo / 'build/graphify/graphify-out/service-map.json')
    started = time.perf_counter()
    checks = []
    copybook = services(index, 'StudioCopybookDialog.svelte')
    assert {r['service']['label'] for r in copybook['results']} == {'studio.dbo.ImportCopybook'}
    handler = copybook['results'][0]['service']
    assert handler['handler_class'].endswith('.studio.dbo.ImportCopybook')
    assert handler['entry_points'][0]['name'] == 'getServiceResult'
    checks.append('Copybook component -> submit -> ImportCopybook Java handler and entry point')
    palette = services(index, 'StudioPalettePanel.svelte')
    rpc = next(r for r in palette['results'] if r['service']['label'] == 'studio.palette.Get')
    assert any(e['relation'] == 'default_callback' for e in rpc['path'])
    assert any(e['to_label'] == 'loadPaletteContext' for e in rpc['path'])
    checks.append('Palette -> local/imported function chain -> default callback -> studio.palette.Get')
    rename = services(index, 'renameDbo')
    assert {r['service']['label'] for r in rename['results']} == {'studio.dbo.Rename'}
    checks.append('renameDbo does not inherit other RPCs from service.js')
    reverse = impact(index, 'studio.dbo.ImportCopybook')
    assert any(r['caller']['label'] == 'StudioCopybookDialog.svelte' for r in reverse['results'])
    checks.append('Reverse impact identifies the copybook component')
    roles = services(index, 'convertigo-studio-web/src/routes/(app)/admin/roles/+page.svelte')
    assert {'roles.List', 'roles.Add', 'roles.Edit'} <= {r['service']['label'] for r in roles['results']}
    assert any('roles.${action}' in r['expression'] for r in roles['unresolved'])
    checks.append('Admin roles page -> state/method candidates, with generic dynamic wrapper still visible')
    builder = services(index, 'StudioBuilderPanel.svelte')
    ws = next(r for r in builder['results'] if r['service']['label'] == 'studio.ngxbuilder.WsBuilder')
    assert ws['relation'] == 'references_resource'
    assert ws['service']['base_class'] == 'WebSocketService'
    checks.append('WebSocket URL remains a resource reference, not a fabricated HTTP execution')
    assert index['metrics']['web_parse_errors'] == 0
    assert index['metrics']['dangling_edges'] == 0
    for query in [copybook, palette, rename, reverse, roles, builder]:
        for result in query['results']:
            node = result.get('service', result.get('caller'))
            lines = (repo / node['source_file']).read_text().splitlines()
            assert 1 <= int(node['source_location'][1:]) <= len(lines)
            for edge in result['path']:
                lines = (repo / edge['source_file']).read_text().splitlines()
                assert 1 <= int(edge['source_location'][1:]) <= len(lines)
    checks.append('All displayed acceptance paths reference existing source files and valid lines')
    return {'checks': checks, 'count': len(checks), 'query_seconds': time.perf_counter() - started,
            'metrics': index['metrics'], 'caveat': 'Curated static checks, not proof of exhaustive semantics or agent productivity gains.'}


if __name__ == '__main__':
    repo = Path(sys.argv[1] if len(sys.argv) > 1 else Path.cwd()).resolve()
    result = verify(repo)
    (repo / 'build/graphify/graphify-out/acceptance.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))
