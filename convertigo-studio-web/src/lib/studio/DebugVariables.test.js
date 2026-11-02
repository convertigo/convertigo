import { render } from 'svelte/server';
import { expect, it, vi } from 'vitest';
import DebugVariables from './DebugVariables.svelte';

it('renders shallow property rows, escaped values, type and an accessible expansion control', () => {
	const load = vi.fn();
	const html = render(DebugVariables, {
		props: {
			load,
			variables: [
				{ name: 'object', value: '[Object]', type: 'object', ref: '1', expandable: true },
				{
					name: '<name>',
					value: '<script>private</script>',
					type: 'string',
					fullValue: true,
					ref: '2'
				}
			]
		}
	}).body;
	for (const text of [
		'variable-branch',
		'object',
		'data-type="string"',
		'Expand object',
		'aria-expanded="false"',
		'Show value of',
		'&lt;script'
	])
		expect(html).toContain(text);
	expect(html).not.toContain('<script>private');
	expect(html).not.toContain('Inspection limit reached');
	expect(load).not.toHaveBeenCalled();
});
it('offers the next page of a large scope instead of losing its remaining bindings', () => {
	const html = render(DebugVariables, {
		props: {
			variables: [{ name: 'item99', value: '99', type: 'number' }],
			reference: 'scope',
			nextOffset: 100
		}
	}).body;
	expect(html).toContain('Load next 100 properties');
});
