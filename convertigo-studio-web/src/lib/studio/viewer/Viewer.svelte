<script>
	import { selectedId } from '$lib/studio/treeview/treeStore';
	import { getUrl } from '$lib/utils/service';
	import { run } from 'svelte/legacy';

	let src = $state('about:blank');

	run(() => {
		src = `${getUrl()}../../projects/${$selectedId.replace(
			/(.*?)[\.:].*/,
			'$1'
		)}/DisplayObjects/mobile/`;
	});
</script>

<iframe title="viewer" {src} class="h-full w-full"></iframe>
