<script>
	import { selectedId } from '$lib/treeview/treeStore';
	import { getUrl } from '$lib/utils/service';

	let url = 'about:blank';

	$: url = `${getUrl()}../../projects/${$selectedId.replace(
		/.*?\..*/,
		'$1'
	)}/DisplayObjects/mobile/`;
</script>

<iframe src={url} class="w-full h-full" />
