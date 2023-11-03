<script>
	import { selectedId } from '$lib/treeview/treeStore';
	import { getUrl } from '$lib/utils/service';

	let src = 'about:blank';

	$: src = `${getUrl()}../../projects/${$selectedId.replace(
		/(.*?)[\.:].*/,
		'$1'
	)}/DisplayObjects/mobile/`;
</script>

<iframe title="viewer" {src} class="w-full h-full" />
