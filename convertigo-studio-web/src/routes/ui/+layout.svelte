<script>
	import Topbar from '$lib/appbar/Topbar.svelte';
	import SidebarLeft from '$lib/sidebars/SidebarLeft.svelte';
	import { AppShell } from '@skeletonlabs/skeleton';
</script>

<AppShell>
	<svelte:fragment slot="header"><Topbar /></svelte:fragment>
	<svelte:fragment slot="sidebarLeft"><SidebarLeft /></svelte:fragment>
	<svelte:fragment slot="sidebarRight"></svelte:fragment>
	<svelte:fragment slot="pageHeader"></svelte:fragment>
	<!-- Router Slot -->
	<slot />
	<!-- ---- / ---- -->
	<svelte:fragment slot="pageFooter"></svelte:fragment>
	<svelte:fragment slot="footer"></svelte:fragment>

	<slot />

	<!-- ---- / ---- -->
	<!-- (pageFooter) -->
	<!-- (footer) -->
</AppShell>
