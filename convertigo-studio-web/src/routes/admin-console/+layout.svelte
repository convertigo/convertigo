<script>
	import { AppShell } from '@skeletonlabs/skeleton';
	import SidebarLeft from '$lib/admin-console/sidebars/SidebarLeft.svelte';
	import Topbar from '$lib/admin-console/sidebars/Topbar.svelte';
</script>

<AppShell>
	<svelte:fragment slot="header">
		<Topbar />
	</svelte:fragment>
	<svelte:fragment slot="sidebarLeft"><SidebarLeft /></svelte:fragment>
	<svelte:fragment slot="sidebarRight"></svelte:fragment>
	<!-- (sidebarRight) -->
	<!-- (pageHeader) -->
	<!-- Router Slot -->
	<slot />
	<!-- ---- / ---- -->
	<svelte:fragment slot="pageFooter"></svelte:fragment>
	<!-- (footer) -->
</AppShell>
