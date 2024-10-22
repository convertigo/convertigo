<script>
	let {
		title = '',
		class: cls = '',
		cardStyle = 'flex flex-col dark:bg-surface-800 bg-surface-100 h-auto p-4 font-extralight text-[13.5px]',
		cardBorder = 'border-[0.5px] border-surface-200 dark:border-surface-600 rounded-container-token',
		cornerOption,
		children,
		...rest
	} = $props();
</script>

<div class="card-style {cardBorder} {cardStyle} {cls}" {...rest}>
	<div class="flex justify-between">
		{#if title?.length > 0}
			<h1 class="text-xl mb-5 font-bold">{title}</h1>
		{/if}
		{@render cornerOption?.()}
	</div>

	{@render children?.()}
</div>
