<script>
	import { Accordion } from '@skeletonlabs/skeleton-svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { getContext } from 'svelte';
	import { accordionIconsKey } from './AccordionGroup.svelte';

	/** @type {{
		value?: any,
		class?: string,
		surface?: string,
		interactive?: boolean,
		layout?: string,
		hoverClass?: string,
		controlClass?: string,
		controlPadding?: string,
		bodyBase?: string,
		bodyClass?: string,
		panelPadding?: string,
		panelClass?: string,
		title?: import('svelte').Snippet,
		subtitle?: import('svelte').Snippet,
		meta?: import('svelte').Snippet,
		control?: import('svelte').Snippet,
		panel?: import('svelte').Snippet,
		children?: import('svelte').Snippet
	} & Record<string, any>} */
	let {
		value,
		class: cls = '',
		surface = 'rounded-container bg-surface-100-900 shadow-follow',
		interactive = true,
		layout = 'layout-x-between layout-x-wrap w-full',
		hoverClass = 'transition-colors duration-200 hover:bg-surface-100/60 dark:hover:bg-surface-800/40',
		controlClass = '',
		controlPadding = 'p',
		bodyBase = 'layout-y-stretch gap-low',
		bodyClass = '',
		panelPadding = 'px-low pb-low',
		panelClass = '',
		title: titleSnippet,
		subtitle: subtitleSnippet,
		meta: metaSnippet,
		control: controlSnippet,
		panel: panelSnippet,
		children,
		...rest
	} = $props();

	const { iconClosed, iconOpen } = getContext(accordionIconsKey) ?? {};
	const resolvedClosed = iconClosed ?? fallbackIconClosed;
	const resolvedOpen = iconOpen ?? fallbackIconOpen;

	const itemClasses = [surface, cls].filter(Boolean).join(' ');
	const triggerClasses = [
		'group flex w-full items-center justify-between gap-3 text-left',
		controlPadding,
		interactive ? hoverClass : '',
		controlClass
	]
		.filter(Boolean)
		.join(' ');
	const bodyClasses = [bodyBase, bodyClass, panelClass].filter(Boolean).join(' ');
	const contentClasses = [panelPadding, 'w-full'].filter(Boolean).join(' ');
	const resolvedPanel = panelSnippet ?? children;
</script>

{#snippet fallbackIconClosed({ attributes })}
	{@const merged = {
		...attributes,
		class: [attributes?.class, 'transition-transform duration-200'].filter(Boolean).join(' ')
	}}
	<span {...merged}>
		<Ico icon="mdi:chevron-right" size={3} />
	</span>
{/snippet}

{#snippet fallbackIconOpen({ attributes })}
	{@const merged = {
		...attributes,
		class: [attributes?.class, 'transition-transform duration-200 rotate-90']
			.filter(Boolean)
			.join(' ')
	}}
	<span {...merged}>
		<Ico icon="mdi:chevron-right" size={3} />
	</span>
{/snippet}

{#snippet indicator({ attributes })}
	{@const state = attributes?.['data-state']}
	{@const merged = {
		...attributes,
		class: [
			attributes?.class,
			'flex items-center text-surface-500 transition-transform duration-200'
		]
			.filter(Boolean)
			.join(' ')
	}}
	{@const icon = state === 'open' ? resolvedOpen : resolvedClosed}
	{@render icon({ attributes: merged })}
{/snippet}

<Accordion.Item {value} class={itemClasses} {...rest}>
	<Accordion.ItemTrigger class={triggerClasses}>
		<div class="flex w-full items-center justify-between gap-3">
			<div class="grow">
				{#if controlSnippet}
					{@render controlSnippet()}
				{:else}
					<div class={layout}>
						<div class="layout-y-low">
							{@render titleSnippet?.()}
							{#if subtitleSnippet}
								<span class="text-surface-500-300 text-xs">{@render subtitleSnippet()}</span>
							{/if}
						</div>
						{#if metaSnippet}
							<div class="layout-x-low items-center">{@render metaSnippet()}</div>
						{/if}
					</div>
				{/if}
			</div>
			<Accordion.ItemIndicator element={indicator} />
		</div>
	</Accordion.ItemTrigger>
	<Accordion.ItemContent class={`${contentClasses} overflow-hidden`}>
		{#if resolvedPanel}
			<div class={`${bodyClasses} origin-top animate-acrd-expand`}>
				{@render resolvedPanel()}
			</div>
		{/if}
	</Accordion.ItemContent>
</Accordion.Item>

<style>
	@keyframes acrd-expand {
		from {
			transform: scaleY(0.96);
			opacity: 0;
		}
		to {
			transform: scaleY(1);
			opacity: 1;
		}
	}

	.animate-acrd-expand {
		animation: acrd-expand 160ms ease-out;
	}
</style>
