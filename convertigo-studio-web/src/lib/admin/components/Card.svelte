<script>
	export let title = '';
	let cls = '';
	export { cls as class };
</script>

<div class="card-style {cls}" {...$$restProps}>
	<div class="flex justify-between">
		{#if title.length > 0}
			<h1 class="text-xl mb-5 font-bold">{title}</h1>
		{/if}

		<slot name="cornerOption" />
	</div>

	<slot />
</div>

<style lang="postcss">
	.card-style {
		@apply flex flex-col border-[0.5px] border-surface-200 dark:border-surface-700 rounded-container-token dark:bg-surface-800 bg-white h-auto p-4 font-extralight text-[13.5px];
	}
</style>
