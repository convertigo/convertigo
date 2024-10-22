<script>
	/** @type {{class?: string, children?: import('svelte').Snippet}} */
	let { class: cls = '', children } = $props();
</script>

<div class={`flex flex-wrap gap-3 justify-end mb-3 ${cls}`}>
	{@render children?.()}
</div>
