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

it('announces opaque and partial scopes without claiming they are empty', () => {
	const opaque = render(DebugVariables, { props: { reason: 'opaque', variables: [] } }).body;
	expect(opaque).toContain('cannot be inspected safely');
	expect(opaque).not.toContain('No variables in this scope');
	const partial = render(DebugVariables, {
		props: { reason: 'candidates', variables: [], reference: 'scope', nextOffset: 1048577 }
	}).body;
	expect(partial).toContain('Inspection work limit reached');
	expect(partial).toContain('Load next 100 properties');
});
it('announces limited values without mistaking an opaque object for a full reference registry', () => {
	const html = render(DebugVariables, {
		props: { variables: [{ name: 'host', value: '[Host object]', limited: true }] }
	}).body;
	expect(html).toContain('This value is opaque, too large');
	expect(html).not.toContain('Variable reference capacity reached');
});
