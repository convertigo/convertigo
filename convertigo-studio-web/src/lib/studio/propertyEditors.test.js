import { describe, expect, it } from 'vitest';
import {
	canOpenCodeProperty,
	flowBindingPreview,
	getPropertyLanguage,
	hasPropertyPossibleValues,
	isCodeEditorProperty,
	isIonProperty,
	isMonacoProperty,
	isSamePropertyPickerTarget,
	isSemanticColorProperty,
	isSmartSourceProperty,
	SMART_TYPE_MODES,
	togglePropertyPickerTarget
} from './propertyEditors';

describe('Studio property editor language detection', () => {
	it('opens Convertigo expressions as JavaScript even before they contain code', () => {
		const row = {
			name: 'expression',
			displayName: 'Expression',
			class: 'java.lang.String',
			value: ''
		};

		expect(getPropertyLanguage(row, 'Project.sq:Sequence.st:Init')).toBe('javascript');
		expect(isMonacoProperty(row, 'Project.sq:Sequence.st:Init')).toBe(true);
		expect(isCodeEditorProperty(row, 'Project.sq:Sequence.st:Init')).toBe(true);
	});

	it('opens Convertigo conditions as JavaScript', () => {
		expect(
			getPropertyLanguage(
				{
					name: 'condition',
					displayName: 'Condition',
					class: 'java.lang.String',
					value: 'context.getAuthenticatedUser() != null'
				},
				'Project.sq:Sequence.st:if'
			)
		).toBe('javascript');
	});

	it('keeps stronger structured language hints before expression defaults', () => {
		expect(
			getPropertyLanguage({
				displayName: 'XPath expression',
				class: 'java.lang.String',
				value: '/document/item'
			})
		).toBe('xml');
		expect(
			getPropertyLanguage({
				displayName: 'SQL query',
				class: 'java.lang.String',
				value: 'select * from table'
			})
		).toBe('sql');
		expect(
			getPropertyLanguage({
				displayName: 'JSON body',
				class: 'java.lang.String',
				value: '{}'
			})
		).toBe('json');
	});

	it('allows explicit code actions for non-java textual values without auto-selecting code', () => {
		const row = {
			name: 'sourceDefinition',
			displayName: 'Source definition',
			class: 'xmlizable',
			value: ['Project.sq:Sequence.st:Step', './document/item']
		};

		expect(isMonacoProperty(row, 'Project.sq:Sequence.st:Step')).toBe(false);
		expect(isCodeEditorProperty(row, 'Project.sq:Sequence.st:Step')).toBe(false);
		expect(canOpenCodeProperty(row, 'Project.sq:Sequence.st:Step')).toBe(true);
	});

	it('opens step SmartType script values as JavaScript', () => {
		const row = {
			name: 'value',
			displayName: 'Value',
			class: 'xmlizable',
			smartType: true,
			mode: 'script',
			value: 'context.getAuthenticatedUser()'
		};

		expect(getPropertyLanguage(row, 'Project.sq:Sequence.st:field')).toBe('javascript');
		expect(canOpenCodeProperty(row, 'Project.sq:Sequence.st:field')).toBe(true);
	});

	it('keeps structured Flow bindings out of the raw code editor', () => {
		const row = {
			name: 'loadingText',
			displayName: 'Loading text',
			class: 'java.lang.String',
			editorClass: 'flow-binding-editor',
			description: 'Literal, source or composed loading text',
			value: JSON.stringify({ mode: 'literal', value: 'Loading signature pad…' })
		};

		expect(isMonacoProperty(row, 'Project.frontend.SignaturePad')).toBe(false);
		expect(canOpenCodeProperty(row, 'Project.frontend.SignaturePad')).toBe(false);
	});

	it('treats NGX smart sources and scripted Ion properties as smart source values', () => {
		expect(
			isSmartSourceProperty({
				editorClass: 'NgxSmartSourcePropertyDescriptor',
				mode: 'script'
			})
		).toBe(true);
		expect(
			isSmartSourceProperty({
				kind: 'ion',
				mode: 'source'
			})
		).toBe(true);
		expect(
			isSmartSourceProperty({
				kind: 'ion',
				mode: 'plain'
			})
		).toBe(false);
	});

	it('detects Ion properties', () => {
		expect(isIonProperty({ kind: 'ion' })).toBe(true);
		expect(isIonProperty({ kind: 'dbo' })).toBe(false);
	});

	it('uses the Convertigo SmartType mode labels', () => {
		expect(SMART_TYPE_MODES.map((mode) => mode.text)).toEqual(['TX', 'JS', 'SC']);
	});
});

describe('Studio inline property picker', () => {
	it('renders source and composed bindings as human-readable expressions', () => {
		expect(
			flowBindingPreview({
				mode: 'source',
				source: { category: 'local', name: 'language' },
				path: []
			})
		).toBe('local.language');
		expect(
			flowBindingPreview({
				mode: 'expression',
				parts: [
					{ kind: 'source', source: { category: 'local', name: 'language' }, path: [] },
					{ kind: 'literal', value: ' is ok !' }
				]
			})
		).toBe('local.language + " is ok !"');
	});

	it('closes when the same property button is clicked again', () => {
		const current = { id: 'Project.Page.Text', propertyName: 'text', serial: 1 };

		expect(isSamePropertyPickerTarget(current, current)).toBe(true);
		expect(togglePropertyPickerTarget(current, current, 2)).toBeNull();
	});

	it('moves to another property without exposing a global property list', () => {
		const current = { id: 'Project.Page.Text', propertyName: 'text', serial: 1 };
		const next = { id: 'Project.Page.Text', propertyName: 'classes', value: '' };

		expect(togglePropertyPickerTarget(current, next, 2)).toEqual({ ...next, serial: 2 });
	});
});

describe('Studio property choices', () => {
	it('uses declared choices for plain and literal binding values', () => {
		expect(hasPropertyPossibleValues({ value: 'medium', values: ['small', 'medium'] })).toBe(true);
		expect(
			hasPropertyPossibleValues({
				editorClass: 'flow-binding-editor',
				value: JSON.stringify({ mode: 'literal', value: 'medium' }),
				values: ['small', 'medium']
			})
		).toBe(true);
	});

	it('keeps legacy values editable when they are outside a closed vocabulary', () => {
		expect(hasPropertyPossibleValues({ value: 'legacy', values: ['small', 'medium'] })).toBe(false);
	});

	it('recognizes semantic color vocabularies without property-name heuristics', () => {
		expect(
			isSemanticColorProperty({
				value: 'primary',
				values: ['neutral', 'primary', 'secondary', 'success', 'warning', 'danger']
			})
		).toBe(true);
		expect(
			isSemanticColorProperty({ value: 'primary', values: ['primary', 'outline', 'ghost'] })
		).toBe(false);
	});
});
