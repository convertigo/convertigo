<script>
	import { Accordion } from '@skeletonlabs/skeleton-svelte';
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{ children?: import('svelte').Snippet, iconClosed?: import('svelte').Snippet, iconOpen?: import('svelte').Snippet } & Record<string, any>} */
	let {
		multiple = false,
		value,
		class: cls = '',
		width,
		iconClosed,
		iconOpen,
		children,
		...rest
	} = $props();

	const content = children ?? (() => null);
</script>

{#snippet defaultIconClosed()}
	<span class="accordion-chevron">
		<Ico icon="mdi:chevron-right" size={3} />
	</span>
{/snippet}

{#snippet defaultIconOpen()}
	<span class="accordion-chevron accordion-chevron-open">
		<Ico icon="mdi:chevron-right" size={3} />
	</span>
{/snippet}

<Accordion
	{multiple}
	{value}
	classes={cls}
	{width}
	iconClosed={iconClosed ?? defaultIconClosed}
	iconOpen={iconOpen ?? defaultIconOpen}
	{...rest}
>
	{@render content()}
</Accordion>

<style>
	.accordion-chevron {
		display: inline-flex;
		align-items: center;
		color: var(--color-surface-500, currentColor);
		transition: transform 0.2s ease;
	}

	.accordion-chevron-open {
		transform: rotate(90deg);
	}
</style>
