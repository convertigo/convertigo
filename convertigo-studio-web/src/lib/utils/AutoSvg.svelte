<script>
	import { onMount } from 'svelte';

	export let src;
	let svg = null;
	let viewBox;

	onMount(() => {
		fetch(src).then(async (r) => {
			if ('image/svg+xml' == r.headers.get('content-type')) {
				let t = await r.text();
				viewBox = t.match(/viewBox="(.*?)"/)?.[1] ?? '0 0 24 24';
				svg = t.replace(/^<svg.*?>/, '').replace(/<\/svg>$/, '');
			}
		});
	});
</script>

{#if svg}
	<svg xmlns="http://www.w3.org/2000/svg" {viewBox} {...$$restProps}>{@html svg}</svg>
{:else}
	<!-- svelte-ignore a11y-missing-attribute -->
	<img {src} {...$$restProps} />
{/if}
