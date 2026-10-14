import { describe, expect, it } from 'vitest';
import { indentedJson, readableEditorValue } from './propertyEditors.js';

const body = JSON.stringify({
	code: '{{ input.code }}',
	client_id: '{{ default(input.client_id, config.oauth.google.clientId) }}',
	redirect_uri: '{{ input.redirect_uri }}'
});

describe('JSON values of the property editor', () => {
	it('opens a long one-line JSON object indented', () => {
		expect(readableEditorValue(body)).toBe(JSON.stringify(JSON.parse(body), null, 2));
	});

	it('keeps short, multi-line, non-JSON or lexically different values as they are', () => {
		expect(readableEditorValue('{"a":1}')).toBe('{"a":1}');
		expect(readableEditorValue(body.replace(',', ',\n'))).toBe(body.replace(',', ',\n'));
		const template = '{"count": {{ local.count }}, "label": "' + 'x'.repeat(80) + '"}';
		expect(readableEditorValue(template)).toBe(template);
		const escaped = '{"text":"\\u00e9' + 'x'.repeat(80) + '"}';
		expect(readableEditorValue(escaped)).toBe(escaped);
	});

	it('indents on demand only JSON objects and arrays', () => {
		expect(indentedJson('[1,2]')).toBe('[\n  1,\n  2\n]');
		expect(indentedJson('"text"')).toBe('');
		expect(indentedJson('{{ input.code }}')).toBe('');
	});
});
