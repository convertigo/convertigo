<script>
	import Topbar from '$lib/studio/appbar/Topbar.svelte';
	import SidebarLeft from '$lib/studio/sidebars/SidebarLeft.svelte';
	import { AppShell } from '@skeletonlabs/skeleton';
	/** @type {{children?: import('svelte').Snippet}} */
	let { children } = $props();
</script>

<AppShell>
	<svelte:fragment slot="header"><Topbar /></svelte:fragment>
	<svelte:fragment slot="sidebarLeft"><SidebarLeft /></svelte:fragment>
	<svelte:fragment slot="sidebarRight"></svelte:fragment>
	<svelte:fragment slot="pageHeader"></svelte:fragment>
	{@render children?.()}
	<svelte:fragment slot="pageFooter"></svelte:fragment>
	<svelte:fragment slot="footer"></svelte:fragment>
</AppShell>
