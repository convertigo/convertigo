<script>
	import { getQuery, getUrl } from '$lib/utils/service';

	/** @type {{href: any, contentType?: string, e?: string, s?: string, link?: boolean, target?: string, class?: string}} */
	let {
		href,
		contentType = 'image/png',
		e = 'L',
		s = '4',
		link = true,
		target = '_blank',
		class: cls = ''
	} = $props();

	let src = $derived(
		getUrl('/qrcode') +
			getQuery({
				o: contentType,
				e,
				s,
				d: href
			})
	);
</script>

{#if link}
	<a {href} {target}> <img class={cls} {src} /></a>
{:else}
	<img class={cls} {src} />
{/if}
