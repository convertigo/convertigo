<script>
	import { getQuery, getUrl } from '$lib/utils/service';

	export let href;
	export let contentType = 'image/png';
	export let e = 'L';
	export let s = '4';
	export let link = true;
	export let target = '_blank';

	let cls = '';
	export { cls as class };

	$: src =
		getUrl('/qrcode') +
		getQuery({
			o: contentType,
			e,
			s,
			d: href
		});
</script>

{#if link}
	<a {href} {target}> <img class={cls} {src} /></a>
{:else}
	<img class={cls} {src} />
{/if}
