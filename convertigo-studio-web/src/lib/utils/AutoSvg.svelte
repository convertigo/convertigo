<script>
	import { onMount } from 'svelte';

	/** @type {any} */
	let { src, ...rest } = $props();
	let svg = $state('');
	let viewBox = $state();

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
	<svg xmlns="http://www.w3.org/2000/svg" {viewBox} {...rest}>{@html svg}</svg>
{:else}
	<!-- svelte-ignore a11y_missing_attribute -->
	<img {src} {...rest} />
{/if}
