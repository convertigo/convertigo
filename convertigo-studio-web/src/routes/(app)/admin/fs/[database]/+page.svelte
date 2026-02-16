<script>
	import { page } from '$app/state';
	import { decodeRouteParam } from '../fullsync-route';
	import FullSyncDatabasePage from '../FullSyncDatabasePage.svelte';

	let database = $derived(decodeRouteParam(page.params.database ?? ''));
</script>

<FullSyncDatabasePage {database} section="all" />
