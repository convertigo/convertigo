import { render } from 'svelte/server';
import { expect, it } from 'vitest';
import DebugVariables from './DebugVariables.svelte';

it('renders nested snapshots and limits while escaping names and values', () => {
	const html = render(DebugVariables, {
		props: {
			variables: [
				{
					name: 'obj',
					value: '[Object]',
					children: [
						{
							name: 'items',
							value: '[Array]',
							children: [{ name: '0', value: '<script>private</script>', limited: true }]
						}
					]
				}
			]
		}
	}).body;
	expect(html.match(/<details/g)).toHaveLength(2);
	expect(html).toContain('Inspection limit reached');
	expect(html).toContain('&lt;script');
	expect(html).not.toContain('<script>private');
});
