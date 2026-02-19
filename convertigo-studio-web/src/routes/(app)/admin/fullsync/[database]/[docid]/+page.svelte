<script>
	import { page } from '$app/state';
	import { decodeRouteParam } from '../../fullsync-route';
	import FullSyncDocumentPage from '../../FullSyncDocumentPage.svelte';

	let database = $derived(decodeRouteParam(page.params.database ?? ''));
	let docId = $derived(decodeRouteParam(page.params.docid ?? ''));
</script>

<FullSyncDocumentPage {database} {docId} />
