<script>
	import { AppShell } from '@skeletonlabs/skeleton';
	import Sidebarleft from '../../adminconsol-lib/sidebars/Sidebarleft.svelte';
	import Topbar from '../../adminconsol-lib/sidebars/Topbar.svelte';
	import Sidebarright from '../../adminconsol-lib/sidebars/Sidebarright.svelte';
</script>

<AppShell>
	<svelte:fragment slot="header">
		<Topbar />
	</svelte:fragment>
	<svelte:fragment slot="sidebarLeft"><Sidebarleft /></svelte:fragment>
	<svelte:fragment slot="sidebarRight"></svelte:fragment>
	<!-- (sidebarRight) -->
	<!-- (pageHeader) -->
	<!-- Router Slot -->
	<slot />
	<!-- ---- / ---- -->
	<svelte:fragment slot="pageFooter"></svelte:fragment>
	<!-- (footer) -->
</AppShell>
