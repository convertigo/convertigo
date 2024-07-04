<script>
	export let title = '';
	let cls = '';
	export { cls as class };
	export let cardStyle =
		'flex flex-col dark:bg-surface-800 bg-surface-100 h-auto p-4 font-extralight text-[13.5px]';
	export let cardBorder =
		'border-[0.5px] border-surface-200 dark:border-surface-600 rounded-container-token';
</script>

<div class="card-style {cardBorder} {cardStyle} {cls}" {...$$restProps}>
	<div class="flex justify-between">
		{#if title?.length > 0}
			<h1 class="text-xl mb-5 font-bold">{title}</h1>
		{/if}

		<slot name="cornerOption" />
	</div>

	<slot />
</div>
