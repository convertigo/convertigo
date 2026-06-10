import { describe, expect, it } from 'vitest';
import { loadPaletteContext, parentPaletteId } from './paletteContext';

describe('Studio palette parent context', () => {
	it('uses visible typed folders before their owning database object', () => {
		expect(parentPaletteId('Project.sq:Sequence.st:Step')).toBe('Project.sq:Sequence:st');
		expect(parentPaletteId('Project.sq:Sequence:st')).toBe('Project.sq:Sequence');
		expect(parentPaletteId('Project.sq:Sequence')).toBe('Project:sq');
		expect(parentPaletteId('Project:sq')).toBe('Project');
	});

	it('keeps structured children below their source container first', () => {
		expect(parentPaletteId('Project.sq:Sequence.st:"field1" : ""')).toBe('Project.sq:Sequence:st');
		expect(parentPaletteId('Project.sq:Sequence.st:object.field')).toBe(
			'Project.sq:Sequence.st:object'
		);
		expect(parentPaletteId('Project.sq:Sequence.st:object.st:field')).toBe(
			'Project.sq:Sequence.st:object'
		);
		expect(parentPaletteId('Project.sq:Sequence.st:object')).toBe('Project.sq:Sequence:st');
	});

	it('falls back until the first parent with palette items', async () => {
		const calls = [];
		const categoriesById = new Map([
			['Project.sq:Sequence.st:Step', []],
			['Project.sq:Sequence:st', [{ name: 'Steps', items: [{ name: 'Simple step' }] }]]
		]);

		const context = await loadPaletteContext('Project.sq:Sequence.st:Step', async (id) => {
			calls.push(id);
			return categoriesById.get(id) ?? [];
		});

		expect(calls).toEqual(['Project.sq:Sequence.st:Step', 'Project.sq:Sequence:st']);
		expect(context).toEqual({
			id: 'Project.sq:Sequence:st',
			fallbackFrom: 'Project.sq:Sequence.st:Step',
			categories: [{ name: 'Steps', items: [{ name: 'Simple step' }] }]
		});
	});
});
