import { describe, expect, it } from 'vitest';
import {
	PROPERTY_TABLE_EDITOR_NAMES,
	PROPERTY_TABLE_EDITORS,
	propertyTableEditor
} from './propertyTables';

describe('propertyTables', () => {
	it('gives each editor a cell per column in a new row', () => {
		for (const editor of Object.values(PROPERTY_TABLE_EDITORS)) {
			expect(editor.template).toHaveLength(editor.columns.length);
			for (const [column, choices] of Object.entries(editor.choices ?? {})) {
				expect(choices).toContain(editor.template[Number(column)]);
			}
		}
	});

	it('names only known editors for the properties', () => {
		for (const name of Object.values(PROPERTY_TABLE_EDITOR_NAMES)) {
			expect(PROPERTY_TABLE_EDITORS[name]).toBeDefined();
		}
	});

	it('finds the editor by its class, then by the name of the property', () => {
		const headers = PROPERTY_TABLE_EDITORS.HttpHeaderForwardEditor;
		expect(
			propertyTableEditor(
				'com.twinsoft.convertigo.eclipse.property_editors.HttpHeaderForwardEditor',
				[]
			)
		).toBe(headers);
		expect(propertyTableEditor('', [], 'httpHeaderForward')).toBe(headers);
	});

	it('edits the sources of the XML action steps with the source picker', () => {
		const editor = propertyTableEditor('', [], 'sourcesDefinition');
		expect(editor.columns).toEqual(['Description', 'Source', 'Default value']);
		expect(editor.sources).toEqual([1]);
		expect(editor.template[1]).toEqual([]);
	});

	it('names the columns of the tables the engine gives the editor of', () => {
		expect(propertyTableEditor('TransformStepReplacementEditor', []).columns).toEqual([
			'Regular exp',
			'Replacement'
		]);
		expect(propertyTableEditor('IsInStepTestEditor', []).choices).toEqual({ 0: ['AND', 'NOT'] });
		expect(propertyTableEditor('', [], 'tags').title).toBe('Table');
		expect(propertyTableEditor('SplitStepTagsEditor', []).columns).toEqual(['Tag name']);
		expect(propertyTableEditor('', [], 'applicationIcons').columns).toEqual(['Icon']);
	});

	it('numbers the columns of an unknown table', () => {
		const editor = propertyTableEditor(undefined, [['a'], ['b', 'c', 'd']], 'unknown');
		expect(editor.columns).toEqual(['Column 1', 'Column 2', 'Column 3']);
		expect(editor.template).toEqual(['', '', '']);
	});
});
