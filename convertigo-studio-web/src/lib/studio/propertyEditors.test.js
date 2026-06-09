import { describe, expect, it } from 'vitest';
import { getPropertyLanguage, isCodeEditorProperty, isMonacoProperty } from './propertyEditors';

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
});
