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
	let {
		title = '',
		class: cls = '',
		cardStyle = 'flex flex-col dark:bg-surface-800 bg-surface-100 h-auto p-4 font-extralight text-[13.5px]',
		cardBorder = 'border-[0.5px] border-color rounded-container-token',
		cornerOption,
		children,
		...rest
	} = $props();
</script>

<div class="card-style {cardBorder} {cardStyle} {cls}" {...rest}>
	<div class="flex flex-wrap gap">
		{#if title?.length > 0}
			<span class="text-xl font-bold">{title}</span>
		{/if}
		{#if cornerOption}
			<div class="grow">
				{@render cornerOption()}
			</div>
		{/if}
	</div>

	{@render children?.()}
</div>
