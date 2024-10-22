<script>
	/** @type {{class?: string, children?: import('svelte').Snippet}} */
	let { class: cls = '', children } = $props();
</script>

<div class="flex flex-col grid md:grid-cols-2 grid-cols-1 gap-5 {cls}">
	{@render children?.()}
</div>
