<script>
	/** @type {{loading?: boolean, class?: string, children?: import('svelte').Snippet}} */
	let { loading = true, class: cls = '', children } = $props();
</script>

{#if loading}
	<div class={`placeholder animate-pulse w-full min-w-32 ${cls}`}></div>
{:else}
	{@render children?.()}
{/if}
