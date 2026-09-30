import importlib.util
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch


SCRIPT = Path(__file__).resolve().parents[1] / 'scripts/code_graph.py'
SPEC = importlib.util.spec_from_file_location('code_graph', SCRIPT)
graph = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(graph)


class CodeGraphTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.repo = Path(self.temporary.name)
        subprocess.run(['git', 'init', '-q', str(self.repo)], check=True)

    def file(self, name, text, tracked=True):
        path = self.repo / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding='utf-8')
        if tracked:
            subprocess.run(['git', '-C', str(self.repo), 'add', '--', name], check=True)
        return path

    def test_scope_and_working_tree_edits(self):
        self.file('engine/src/Foo.java', 'class Foo {}')
        component = self.file('convertigo-studio-web/src/Foo.svelte', '<p>before</p>')
        component.write_text('<p>after</p>')
        self.file('convertigo-studio-web/src/style.css', 'body {}')
        self.file('convertigo-studio-web/README.md', 'documentation')
        self.file('convertigo-studio-web/.env', 'SECRET=value')
        self.file('convertigo-studio-web/src/Untracked.js', 'export {}', tracked=False)
        self.file('other/tool.py', 'pass')
        files = graph.read_corpus(self.repo)
        self.assertEqual(set(files), {'engine/src/Foo.java',
                                     'convertigo-studio-web/src/Foo.svelte'})
        self.assertEqual(files['convertigo-studio-web/src/Foo.svelte'], b'<p>after</p>')

    def test_snapshot_preserves_generated_alias_config(self):
        self.file('engine/src/Foo.java', 'class Foo {}')
        self.file(graph.ALIAS_CONFIG, '{"compilerOptions":{"paths":{"$lib":["../src/lib"]}}}',
                  tracked=False)
        files = graph.read_corpus(self.repo)
        target = self.repo / 'build/snapshot'
        graph.snapshot(files, target)
        self.assertEqual((target / graph.ALIAS_CONFIG).read_bytes(), files[graph.ALIAS_CONFIG])

    def test_fingerprint_tracks_contents_names_and_alias_configuration(self):
        before = {'engine/src/Foo.java': b'class Foo {}'}
        value = graph.fingerprint(before)
        self.assertNotEqual(value, graph.fingerprint({'engine/src/Foo.java': b'class Foo { int n; }'}))
        self.assertNotEqual(value, graph.fingerprint({'engine/src/Bar.java': b'class Foo {}'}))
        self.assertNotEqual(value, graph.fingerprint(before | {graph.ALIAS_CONFIG: b'{}'}))
        self.assertEqual(graph.fingerprint(before | {'engine/src/Bar.java': b'other'}),
                         graph.fingerprint({'engine/src/Bar.java': b'other'} | before))

    def test_symlink_input_is_rejected(self):
        self.file('engine/src/Foo.java', 'class Foo {}')
        target = self.file('other.txt', 'private', tracked=False)
        path = self.repo / 'engine/src/Foo.java'
        path.unlink()
        path.symlink_to(target)
        with self.assertRaisesRegex(ValueError, 'symlink'):
            graph.read_corpus(self.repo)

    def test_source_area_names_are_meaningful(self):
        self.assertEqual(graph.source_area(
            'engine/src/com/twinsoft/convertigo/beans/ngx/components/IonIcon.java'),
            'beans.ngx.components')
        self.assertEqual(graph.source_area(
            'eclipse-plugin-studio/src/com/twinsoft/convertigo/eclipse/actions/Foo.java'),
            'eclipse.actions')
        self.assertEqual(graph.source_area(
            'convertigo-studio-web/src/lib/admin/components/Foo.svelte'),
            'web.admin.components')
        self.assertEqual(graph.source_area(
            'convertigo-studio-web/src/routes/(app)/admin/+page.svelte'),
            'web.routes.admin')
        self.assertEqual(graph.source_area('convertigo-studio-web/src/hooks.server.js'), 'web')

    def test_package_overview_groups_code_and_omits_external_placeholders(self):
        class Nodes:
            def nodes(self, data):
                return [
                    ('a', {'source_file': 'engine/src/com/twinsoft/convertigo/beans/ngx/components/A.java'}),
                    ('b', {'source_file': 'engine/src/com/twinsoft/convertigo/beans/ngx/components/dynamic/B.java'}),
                    ('c', {'source_file': 'convertigo-studio-web/src/routes/(app)/admin/+page.svelte.js'}),
                    ('d', {'source_file': 'convertigo-studio-web/src/lib/studio/Foo.svelte'}),
                    ('e', {'source_file': '@sveltejs/kit'}),
                ]

        groups, names = graph.package_groups(Nodes())
        by_name = {names[key]: nodes for key, nodes in groups.items()}
        self.assertEqual(by_name, {'beans.ngx': ['a', 'b'],
                                  'web.routes.admin': ['c'], 'web.studio': ['d']})

    def test_status_detects_edits_deletions_and_ignores_unrelated_files(self):
        source = self.file('engine/src/Foo.java', 'class Foo {}')
        output = self.repo / 'build/graphify/graphify-out'
        output.mkdir(parents=True)
        graph.write_json(output / 'graph.json', {'nodes': [], 'links': []})
        state = {'schema_version': graph.SCHEMA_VERSION, 'graphify_version': graph.GRAPHIFY_VERSION,
                 'recipe_fingerprint': graph.recipe_fingerprint(),
                 'source_fingerprint': graph.fingerprint(graph.read_corpus(self.repo))}
        graph.write_json(output / 'index-state.json', state)
        self.assertEqual(graph.status(self.repo), 0)
        self.file('README.md', 'unrelated')
        self.assertEqual(graph.status(self.repo), 0)
        source.write_text('class Foo { int n; }')
        self.assertEqual(graph.status(self.repo), 1)
        source.unlink()
        self.assertEqual(graph.status(self.repo), 1)

    def test_missing_index_is_optional(self):
        self.assertEqual(graph.status(self.repo), 1)
        self.assertFalse((self.repo / 'build').exists())

    def test_publish_rolls_back_if_replacement_fails(self):
        run = self.repo / 'build/graphify/run-test'
        generated = run / 'published'
        target = run.parent / 'graphify-out'
        generated.mkdir(parents=True)
        target.mkdir()
        (target / 'graph.json').write_text('old index')
        original = Path.rename

        def rename(path, destination):
            if path == generated:
                raise OSError('simulated replacement failure')
            return original(path, destination)

        with patch.object(Path, 'rename', rename), self.assertRaises(OSError):
            graph.publish(generated, target)
        self.assertEqual((target / 'graph.json').read_text(), 'old index')
        self.assertFalse((run / 'previous-index').exists())


if __name__ == '__main__':
    unittest.main()
