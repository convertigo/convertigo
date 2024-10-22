<script>
	/** @type {{Record<string, any>}} */
	let {
		title = '',
		class: cls = '',
		cardStyle = 'flex flex-col dark:bg-surface-800 bg-surface-100 h-auto p-5 text-[13.5px]',
		cardBorder = 'rounded-token',
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
