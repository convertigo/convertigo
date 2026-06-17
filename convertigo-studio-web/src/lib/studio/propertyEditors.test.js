import { describe, expect, it } from 'vitest';
import {
	canOpenCodeProperty,
	getPropertyLanguage,
	isCodeEditorProperty,
	isMonacoProperty,
	SMART_TYPE_MODES
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

	it('uses the Convertigo SmartType mode labels', () => {
		expect(SMART_TYPE_MODES.map((mode) => mode.text)).toEqual(['TX', 'JS', 'SC']);
	});
});
