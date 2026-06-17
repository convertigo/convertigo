<script>
	import { fromAction } from 'svelte/attachments';

	/** @type {any} */
	let { src, alt = '', ...rest } = $props();
	let svg = $state('');
	let viewBox = $state();
	let loadToken = 0;
	const renderSvg = $derived(fromAction(setSvg, () => svg));

	$effect(() => {
		const currentSrc = src;
		const token = ++loadToken;
		svg = '';
		viewBox = undefined;
		if (!currentSrc) {
			return;
		}
		fetch(currentSrc)
			.then(async (r) => {
				if (token !== loadToken) {
					return;
				}
				if (r.headers.get('content-type')?.toLowerCase().startsWith('image/svg+xml')) {
					const t = await r.text();
					if (token !== loadToken) {
						return;
					}
					viewBox = t.match(/viewBox="(.*?)"/)?.[1] ?? '0 0 24 24';
					svg = t.replace(/^<svg.*?>/, '').replace(/<\/svg>$/, '');
				}
			})
			.catch(() => {
				if (token === loadToken) {
					svg = '';
					viewBox = undefined;
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
