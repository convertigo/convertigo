<script>
	/** @type {{children?: import('svelte').Snippet}} */
	let { children } = $props();
</script>

<div>
	<div class="flex space-x-4">
		{@render children?.()}
	</div>
</div>
