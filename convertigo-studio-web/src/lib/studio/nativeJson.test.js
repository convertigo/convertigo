import { describe, expect, it } from 'vitest';
import {
	childrenOf,
	formatDuration,
	nodeLabel,
	parseJavaScript,
	parseJsonPath,
	rowItems,
	sampleSummary,
	toJavaScript,
	toJsonPath,
	valueAt,
	xpathToJsonPath
} from './nativeJson.js';

describe('nativeJson', () => {
	it('names a value as JavaScript and as a JSON path', () => {
		expect(toJavaScript('rows', [0, 'name'])).toBe('rows[0].name');
		expect(toJavaScript('rows', [0, 'a b'])).toBe('rows[0]["a b"]');
		expect(toJsonPath('rows', [0, 'name'])).toBe('$.rows[0].name');
		expect(toJsonPath('', [0, 'a b'])).toBe("$[0]['a b']");
		expect(toJsonPath('', [])).toBe('$');
		expect(toJsonPath('', ["it's"])).toBe("$['it\\'s']");
	});

	it('reads back a JavaScript expression under a variable', () => {
		expect(parseJavaScript('rows', 'rows[0].name')).toEqual([0, 'name']);
		expect(parseJavaScript('rows', ' rows[2]["a \\"b"] ')).toEqual([2, 'a "b']);
		expect(parseJavaScript('rows', 'rows')).toEqual([]);
		expect(parseJavaScript('rows', 'rows.filter(x => x)')).toBeNull();
		expect(parseJavaScript('rows', 'other[0]')).toBeNull();
	});

	it('reads the segments of a JSON path', () => {
		expect(parseJsonPath('$')).toEqual([]);
		expect(parseJsonPath('$.results[0].id')).toEqual(['results', 0, 'id']);
		expect(parseJsonPath("$['a b'][*].x")).toEqual(['a b', null, 'x']);
		expect(parseJsonPath('$.results.*')).toEqual(['results', null]);
		expect(parseJsonPath('results')).toBeNull();
		expect(parseJsonPath('$..x')).toBeNull();
	});

	it('converts a simple XPath of an XML view to a JSON path', () => {
		expect(xpathToJsonPath('.')).toBe('$');
		expect(xpathToJsonPath('./item[1]/name/text()')).toBe('$[0].name');
		expect(xpathToJsonPath('./rows')).toBe('$.rows');
		expect(xpathToJsonPath('./item')).toBeNull();
		expect(xpathToJsonPath('//rows')).toBeNull();
		expect(xpathToJsonPath('./item[0]')).toBeNull();
		expect(xpathToJsonPath('./a/@b')).toBeNull();
	});

	it('labels the nodes as the JSON tree of the Eclipse Studio', () => {
		expect(nodeLabel('rows', [1, 2])).toBe('rows: [2]');
		expect(nodeLabel('[0]', { a: 1 })).toBe('[0]: {1}');
		expect(nodeLabel('name', 'Ann')).toBe('name: "Ann"');
		expect(nodeLabel('', null)).toBe('null');
		expect(nodeLabel('t', 'x'.repeat(300))).toHaveLength(3 + 200 + 1);
		expect(childrenOf(Array.from({ length: 502 }, (_, i) => i)).more).toBe(2);
		expect(valueAt({ a: [{ b: 3 }] }, ['a', 0, 'b'])).toBe(3);
	});

	it('summarizes the samples and the rows', () => {
		expect(sampleSummary({ found: false })).toBe('none');
		expect(sampleSummary({ found: true, count: 3, json: '[]' })).toBe('3 items');
		expect(sampleSummary({ found: true, count: -1, json: '1' })).toBe('value');
		expect(sampleSummary({ found: true, note: 'stream', json: '' })).toBe('stream');
		expect(formatDuration(1203)).toBe('1203 µs');
		expect(formatDuration(25400)).toBe('25 ms');
		expect(rowItems({ count: -1, note: 'output', size: 90 })).toBe('output');
		expect(rowItems({ count: 2, size: 10 })).toBe('2');
		expect(rowItems({ count: -1, note: 'stream', size: 0 })).toBe('stream');
	});
});

describe('nativeJson lists and paths', () => {
	it('reads and writes the columns and the aggregations as the Eclipse list editors', async () => {
		const { parseJsonList, jsonListValue } = await import('./nativeJson.js');
		expect(parseJsonList('["name", "city"]', 'columns')).toEqual({
			mode: 'list',
			rows: [{ column: 'name' }, { column: 'city' }]
		});
		expect(parseJsonList('', 'columns')).toEqual({ mode: 'list', rows: [] });
		expect(parseJsonList('null', 'columns').mode).toBe('null');
		expect(parseJsonList('rows.map(r => r.k)', 'columns').mode).toBe('expression');
		expect(parseJsonList('[{type: "sum"}]', 'aggregations').mode).toBe('expression');
		expect(parseJsonList('[{"type": "sum", "other": 1}]', 'aggregations').mode).toBe('expression');
		expect(parseJsonList('[{"type": "sum", "column": "amount"}]', 'aggregations')).toEqual({
			mode: 'list',
			rows: [{ type: 'sum', column: 'amount' }]
		});
		expect(jsonListValue([{ column: 'a' }, { column: '' }, { column: 'b "c"' }], 'columns')).toBe(
			'["a", "b \\"c\\""]'
		);
		expect(
			jsonListValue([{ type: 'sum', column: 'x', key: '', label: 'Total' }], 'aggregations')
		).toBe('[{"type": "sum", "column": "x", "label": "Total"}]');
	});

	it('previews what a JSON path reads', async () => {
		const { jsonPathPreview } = await import('./nativeJson.js');
		const value = { results: [{ id: 1 }, { id: 2 }] };
		expect(jsonPathPreview(value, '$.results')).toBe('Value: array of 2');
		expect(jsonPathPreview(value, '$.results[0]')).toBe('Value: object of 1');
		expect(jsonPathPreview(value, '$.results[1].id')).toBe('Value: 2');
		expect(jsonPathPreview(value, '$.results[*].id')).toBe('Value: array of 2');
		expect(jsonPathPreview(value, '$.missing')).toBe('Value: nothing (the default value)');
		expect(jsonPathPreview(value, 'results')).toBe('Invalid JSON path: results');
	});
});
