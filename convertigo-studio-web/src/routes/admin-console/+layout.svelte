<script>
	import { AppShell, initializeStores } from '@skeletonlabs/skeleton';
	import SidebarLeft from '$lib/admin-console/sidebars/SidebarLeft.svelte';
	import Topbar from '$lib/admin-console/sidebars/Topbar.svelte';
	import { Modal } from '@skeletonlabs/skeleton';

	initializeStores();
</script>

<AppShell>
	<Modal />

	<svelte:fragment slot="header">
		<Topbar />
	</svelte:fragment>
	<svelte:fragment slot="sidebarLeft"><SidebarLeft /></svelte:fragment>
	<svelte:fragment slot="sidebarRight"></svelte:fragment>

	<slot />
</AppShell>
