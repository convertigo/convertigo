<script>
	/** @type {{
	 * 	title?: string,
	 * 	class?: string,
	 * 	cardStyle?: string,
	 * 	cardBorder?: string,
	 * 	cornerOption?: import('svelte').Snippet,
	 * 	children?: import('svelte').Snippet
	}} */
	let {
		title = '',
		class: cls = '',
		cardStyle = 'flex flex-col dark:bg-surface-800 bg-surface-100 h-auto p-5 text-[13.5px]',
		cardBorder = 'rounded',
		cornerOption,
		children,
		...rest
	} = $props();
</script>

<div class="card-style {cardBorder} {cardStyle} {cls}" {...rest}>
	{#if title?.length > 0 && cornerOption}
		<div class="flex justify-between">
			{#if title?.length > 0}
				<h1 class="text-xl mb-5 font-bold">{title}</h1>
			{/if}

			{@render cornerOption?.()}
		</div>
	{/if}

	{@render children?.()}
</div>
