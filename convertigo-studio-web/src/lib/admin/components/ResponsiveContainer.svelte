<script>
	import { Container } from 'postcss';

	let cls = '';
	export { cls as class };

	// THESE ARE DEFAULT VALUES FOR THE COMP
	export let smCols = 'sm:grid-cols-2';
	export let mdCols = 'md:grid-cols-2';
	export let lgCols = 'lg:grid-cols-3';

	export let scrollable = true;

	export let maxHeight = '60vh';
	export let containerGap = 'gap-10';
</script>

<div
	class="grid {containerGap} {cls} {smCols} {mdCols} {lgCols} {scrollable ? 'overflow-y-auto' : ''}"
	style="max-height: {maxHeight};"
>
	<slot />
</div>

<style lang="postcss">
	.responsive-container {
		@apply grid;
	}
</style>
