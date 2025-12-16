<script>
	import { onMount } from 'svelte';
	import { fromAction } from 'svelte/attachments';

	/** @type {any} */
	let { src, alt = '', ...rest } = $props();
	let svg = $state('');
	let viewBox = $state();
	const renderSvg = $derived(fromAction(setSvg, () => svg));

	onMount(() => {
		if (!src) return;
		fetch(src).then(async (r) => {
			if ('image/svg+xml' == r.headers.get('content-type')) {
				let t = await r.text();
				viewBox = t.match(/viewBox="(.*?)"/)?.[1] ?? '0 0 24 24';
				svg = t.replace(/^<svg.*?>/, '').replace(/<\/svg>$/, '');
			}
		});
	});

	/** @param {SVGSVGElement} node */
	function setSvg(node, content) {
		node.innerHTML = content ?? '';
		return {
			update(value) {
				node.innerHTML = value ?? '';
			}
		};
	}
</script>

{#if svg}
	<svg xmlns="http://www.w3.org/2000/svg" {viewBox} {...rest} {@attach renderSvg}></svg>
{:else}
	<img {src} {alt} {...rest} />
{/if}
