<script module>
	export const accordionIconsKey = Symbol('convertigo-accordion-icons');
</script>

<script>
	import { Accordion } from '@skeletonlabs/skeleton-svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { setContext } from 'svelte';

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

	const resolvedClosed = iconClosed ?? defaultIconClosed;
	const resolvedOpen = iconOpen ?? defaultIconOpen;

	setContext(accordionIconsKey, { iconClosed: resolvedClosed, iconOpen: resolvedOpen });
</script>

{#snippet defaultIconClosed({ attributes })}
	{@const attr = {
		...attributes,
		class: [attributes?.class, 'transition-transform duration-200'].filter(Boolean).join(' ')
	}}
	<span {...attr}>
		<Ico icon="mdi:chevron-right" size={3} />
	</span>
{/snippet}

{#snippet defaultIconOpen({ attributes })}
	{@const attr = {
		...attributes,
		class: [attributes?.class, 'transition-transform duration-200 rotate-90']
			.filter(Boolean)
			.join(' ')
	}}
	<span {...attr}>
		<Ico icon="mdi:chevron-right" size={3} />
	</span>
{/snippet}

<Accordion {multiple} {value} class={[cls, width].filter(Boolean).join(' ')} {...rest}>
	{@render content()}
</Accordion>
