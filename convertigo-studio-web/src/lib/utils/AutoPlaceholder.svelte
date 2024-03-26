<script>
	export let loading = true;
	let cls = '';
	export { cls as class };
</script>

{#if loading}
	<div class={`placeholder animate-pulse w-full min-w-32 ${cls}`}></div>
{:else}
	<slot />
{/if}
