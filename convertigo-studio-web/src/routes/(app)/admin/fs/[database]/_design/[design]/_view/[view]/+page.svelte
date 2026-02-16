<script>
	import { page } from '$app/state';
	import { decodeRouteParam } from '../../../../../fullsync-route';
	import FullSyncDatabasePage from '../../../../../FullSyncDatabasePage.svelte';

	let database = $derived(decodeRouteParam(page.params.database ?? ''));
	let designDocId = $derived(`_design/${decodeRouteParam(page.params.design ?? '')}`);
	let viewName = $derived(decodeRouteParam(page.params.view ?? ''));
</script>

<FullSyncDatabasePage {database} section="all" {designDocId} {viewName} />
