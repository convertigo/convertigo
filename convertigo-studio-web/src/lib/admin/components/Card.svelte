<script>
	/**
	 * @type {{
	 * 	title?: string,
	 * 	class?: string,
	 * 	cardStyle?: string,
	 * 	cardBorder?: string,
	 * 	cornerOption?: import('svelte').Snippet,
	 * 	children?: import('svelte').Snippet
	 * }}
	 */
	let { title = '', class: cls = '', cornerOption, children } = $props();
</script>

<div class="layout-y-p bg-surface-100-800 border-[0.5px] border-color rounded-container {cls}">
	{#if title?.length > 0 || cornerOption}
		<div class="layout-x flex-wrap w-full">
			{#if title?.length > 0}
				<span class="text-xl font-bold">{title}</span>
			{/if}
			{#if cornerOption}
				<div class="grow">
					{@render cornerOption()}
				</div>
			{/if}
		</div>
	{/if}
	{@render children?.()}
</div>
