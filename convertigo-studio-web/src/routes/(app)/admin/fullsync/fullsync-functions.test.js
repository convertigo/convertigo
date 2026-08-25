import { describe, expect, it } from 'vitest';
import { anonymizeFullSyncFunction, nameFullSyncFunction } from './fullsync-functions';

describe('FullSync view functions', () => {
	it('names anonymous map and reduce functions for the JavaScript editor', () => {
		expect(nameFullSyncFunction('function (doc) {\n  emit(doc._id, doc);\n}', 'map')).toBe(
			'function map(doc) {\n  emit(doc._id, doc);\n}'
		);
		expect(
			nameFullSyncFunction('function (keys, values) { return values.length; }', 'reduce')
		).toBe('function reduce(keys, values) { return values.length; }');
	});

	it('uses the expected editor name when a stored function is already named', () => {
		expect(nameFullSyncFunction('  function custom (doc) { return doc; }', 'map')).toBe(
			'  function map(doc) { return doc; }'
		);
	});

	it('removes editor function names before saving the design document', () => {
		expect(anonymizeFullSyncFunction('function map(doc) { return doc; }')).toBe(
			'function (doc) { return doc; }'
		);
		expect(anonymizeFullSyncFunction('  function reduce (keys, values) { return values; }')).toBe(
			'  function (keys, values) { return values; }'
		);
	});

	it('leaves other JavaScript expressions and built-in reducers unchanged', () => {
		expect(nameFullSyncFunction('(doc) => emit(doc._id, doc)', 'map')).toBe(
			'(doc) => emit(doc._id, doc)'
		);
		expect(anonymizeFullSyncFunction('_sum')).toBe('_sum');
	});
});
