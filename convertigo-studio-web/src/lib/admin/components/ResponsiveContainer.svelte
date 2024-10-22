<script>
	// THESE ARE DEFAULT VALUES FOR THE COMP

	/** @type {{class?: string, smCols?: string, mdCols?: string, lgCols?: string, scrollable?: boolean, maxHeight?: string, containerGap?: string, children?: import('svelte').Snippet}} */
	let {
		class: cls = '',
		smCols = 'sm:grid-cols-2',
		mdCols = 'md:grid-cols-2',
		lgCols = 'lg:grid-cols-3',
		scrollable = true,
		maxHeight = '60vh',
		containerGap = 'gap-10',
		children
	} = $props();
</script>

<div
	class="grid {containerGap} {cls} {smCols} {mdCols} {lgCols} {scrollable ? 'overflow-y-auto' : ''}"
	style="max-height: {maxHeight};"
>
	{@render children?.()}
</div>

<style lang="postcss">
	.responsive-container {
		@apply grid;
	}
</style>
