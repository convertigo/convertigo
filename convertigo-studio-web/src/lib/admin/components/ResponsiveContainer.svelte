<script>
	let cls = '';
	export { cls as class };

	// THESE ARE DEFAULT VALUES FOR THE COMP
	export let smCols = 'sm:grid-cols-2';
	export let mdCols = 'md:grid-cols-2';
	export let lgCols = 'lg:grid-cols-3';

	export let scrollable = true;

	export let maxHeight = '60vh';
</script>

<div
	class={`responsive-container ${cls} ${smCols} ${mdCols} ${lgCols} ${scrollable ? 'overflow-y-auto' : ''}`}
	style={`max-height: ${maxHeight};`}
>
	<slot />
</div>

<style lang="postcss">
	.responsive-container {
		@apply grid gap-10;
	}
</style>
