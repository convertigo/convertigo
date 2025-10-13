<script>
	import { Accordion } from '@skeletonlabs/skeleton-svelte';

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

	const itemClasses = [surface, cls].filter(Boolean).join(' ');
	const controlClasses = ['group', 'text-left', interactive ? hoverClass : '', controlClass]
		.filter(Boolean)
		.join(' ');
	const bodyClasses = [bodyBase, bodyClass, panelClass].filter(Boolean).join(' ');
	const resolvedPanel = panelSnippet ?? children;
</script>

<Accordion.Item
	{value}
	classes={itemClasses}
	{controlClasses}
	{controlPadding}
	{panelPadding}
	panelClasses={bodyClasses}
	{...rest}
>
	{#snippet control()}
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
	{/snippet}
	{#snippet panel()}
		{#if resolvedPanel}
			{@render resolvedPanel()}
		{/if}
	{/snippet}
</Accordion.Item>
