import { describe, expect, it } from 'vitest';
import { loadPaletteContext, paletteContextLabel, parentPaletteId } from './paletteContext';

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

	it('formats the pending target without exposing virtual authoring prefixes', () => {
		expect(paletteContextLabel('Project.FlowEngine.frontends.authoring_route_large_lists')).toBe(
			'Large lists'
		);
		expect(paletteContextLabel('Project.FlowEngine.frontends.authoring_introTitle')).toBe(
			'Intro Title'
		);
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

	it('forwards cancellation and timeout options through parent fallback requests', async () => {
		const controller = new AbortController();
		const calls = [];
		const options = { signal: controller.signal, timeoutMs: 20_000 };

		await loadPaletteContext(
			'Project.sq:Sequence.st:Step',
			async (id, receivedOptions) => {
				calls.push({ id, options: receivedOptions });
				return id.endsWith(':st') ? [{ name: 'Steps', items: [{ name: 'Simple step' }] }] : [];
			},
			options
		);

		expect(calls).toEqual([
			{ id: 'Project.sq:Sequence.st:Step', options },
			{ id: 'Project.sq:Sequence:st', options }
		]);
	});
});
