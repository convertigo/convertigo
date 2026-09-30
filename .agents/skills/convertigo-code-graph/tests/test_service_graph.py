import sys
from pathlib import Path
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import service_graph as graph


def node(identity, kind='function'):
    return {'id': identity, 'label': identity, 'entity_type': kind,
            'source_file': identity + '.js', 'source_location': 'L1'}


def edge(source, target, relation):
    return {'source': source, 'target': target, 'relation': relation,
            'confidence': 'EXTRACTED', 'source_file': source + '.js', 'source_location': 'L2'}


class ServiceGraphTests(unittest.TestCase):
    def fixture(self):
        return {'nodes': [node('Component', 'component'), node('rename'), node('remove'),
                          node('ServiceModule', 'module'), node('Rename', 'http_service'),
                          node('Remove', 'http_service')],
                'edges': [edge('Component', 'rename', 'calls'),
                          edge('Component', 'ServiceModule', 'imports'),
                          edge('ServiceModule', 'rename', 'contains'),
                          edge('ServiceModule', 'remove', 'contains'),
                          edge('rename', 'Rename', 'calls_http'),
                          edge('remove', 'Remove', 'calls_http')],
                'unresolved': [{'owner': 'rename', 'expression': 'dynamic', 'reason': 'dynamic',
                                'source_file': 'rename.js', 'source_location': 'L3'}], 'warnings': []}

    def test_import_does_not_call_other_module_helpers(self):
        result = graph.services(self.fixture(), 'Component')
        self.assertEqual([r['service']['label'] for r in result['results']], ['Rename'])
        self.assertEqual(len(result['unresolved']), 1)
        self.assertIn('not observed runtime', graph.describe(result))

    def test_reverse_impact_is_directed_and_excludes_unrelated_components(self):
        result = graph.impact(self.fixture(), 'Remove')
        self.assertEqual([r['caller']['label'] for r in result['results']], ['ServiceModule'])

    def test_limit_and_ambiguity(self):
        index = self.fixture()
        self.assertTrue(graph.services(index, 'ServiceModule', limit=1)['truncated'])
        index['nodes'].append(dict(node('another'), label='Component'))
        # A root component wins over a same-named function; two components do not.
        self.assertEqual(graph.select(index, 'Component')['id'], 'Component')
        index['nodes'][-1]['entity_type'] = 'component'
        with self.assertRaisesRegex(ValueError, 'exactly one'):
            graph.select(index, 'Component')

    def test_shared_transport_does_not_pollute_focused_rpc_queries(self):
        index = self.fixture()
        index['nodes'].extend([dict(node('transport'), transport_boundary=True),
                               node('Auth', 'http_service')])
        index['edges'].extend([edge('rename', 'transport', 'calls'),
                               edge('transport', 'Auth', 'calls_http')])
        self.assertEqual(graph.services(index, 'Component')['total'], 1)
        self.assertEqual(graph.impact(index, 'Auth')['total'], 0)

    def test_missing_handler_is_reported_not_invented(self):
        web = {'nodes': [node('Component', 'component')], 'edges': [], 'warnings': [],
               'files': 1, 'components': 1, 'parse_errors': 0, 'parsers': {},
               'requests': [{'owner': 'Component', 'services': ['Missing'], 'relation': 'calls_http',
                             'source_file': 'Component.js', 'source_location': 'L2', 'expression': 'Missing'}]}
        index = graph.connect(web, {}, [])
        self.assertEqual(index['edges'], [])
        self.assertEqual(index['unresolved'][0]['reason'], 'no verified Java admin handler')

    def test_java_dispatch_requires_annotation_and_matching_package(self):
        try:
            import tree_sitter_java  # noqa: F401
        except ImportError:
            self.skipTest('Run with graphifyy for Java AST fixtures')
        file = 'engine/src/com/twinsoft/convertigo/engine/admin/services/demo/Run.java'
        source = b'package com.twinsoft.convertigo.engine.admin.services.demo;\n@ServiceDefinition(roles={Role.WEB_ADMIN}) public class Run extends JSonService {}'
        dispatch = b'package com.twinsoft.convertigo.engine.admin; class AdminServlet {void f(){Class.forName(myPackage+".services."+serviceName);}}'
        class_node = dict(node('javaRun'), source_file=file, label='Run', _callable_class=True)
        endpoints, warnings = graph.java_services({file: source, graph.DISPATCH_FILE: dispatch}, [class_node])
        self.assertFalse(warnings)
        self.assertEqual(endpoints['demo.Run']['handler'], 'javaRun')
        self.assertEqual(endpoints['demo.Run']['roles'], ['WEB_ADMIN'])
        endpoints, warnings = graph.java_services({file: source, graph.DISPATCH_FILE: b'class AdminServlet {}'}, [class_node])
        self.assertFalse(endpoints)
        self.assertTrue(warnings)

    def test_java_constructor_does_not_overwrite_class_or_interface_kind(self):
        try:
            import tree_sitter_java  # noqa: F401
        except ImportError:
            self.skipTest('Run with graphifyy for Java AST fixtures')
        file = 'engine/src/Example.java'
        types = [dict(node('class'), label='Example', source_file=file, _callable_class=True),
                 dict(node('constructor'), label='Example', source_file=file, _callable=True),
                 dict(node('interface'), label='Contract', source_file=file, _callable_class=True)]
        graph.java_services({file: b'class Example {Example(){}} interface Contract {}'}, types)
        self.assertEqual([n['entity_type'] for n in types], ['class', 'constructor', 'interface'])


if __name__ == '__main__':
    unittest.main()
