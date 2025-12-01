<script>
	import { getQuery, getUrl } from '$lib/utils/service';

	/** @type {{href: any, contentType?: string, e?: string, s?: string, link?: boolean, target?: string, class?: string, label?: string, alt?: string}} */
	let {
		href,
		contentType = 'image/png',
		e = 'L',
		s = '4',
		link = true,
		target = '_blank',
		class: cls = '',
		label = 'Open QR code',
		alt = 'QR code'
	} = $props();

	let src = $derived(
		getUrl('qrcode') +
			getQuery({
				o: contentType,
				e,
				s,
				d: href
			})
	);
</script>

{#if link}
	<a rel="external" class={cls} {href} {target} aria-label={label}>
		<img class={cls} {src} {alt} />
	</a>
{:else}
	<img class={cls} {src} {alt} />
{/if}
