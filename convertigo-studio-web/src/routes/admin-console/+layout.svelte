<script>
	import { AppShell, Drawer } from '@skeletonlabs/skeleton';
	import Topbar from '$lib/admin-console/sidebars/Topbar.svelte';
	import { Modal, Toast } from '@skeletonlabs/skeleton';
	import ModalAddSymbol from '$lib/admin-console/modals/ModalAddSymbol.svelte';
	import PagesRail from '$lib/admin-console/admin-components/PagesRail.svelte';
	import PagesRailToggle from '$lib/admin-console/admin-components/PagesRailToggle.svelte';

	const modalComponentRegistry = {
		modalAddSymbols: { ref: ModalAddSymbol }
	};
</script>

<Modal components={modalComponentRegistry} />
<Toast />
<Drawer>
	<PagesRailToggle open={false} />
	<PagesRail />
</Drawer>

<AppShell>
	<Topbar slot="header" />
	<svelte:fragment slot="sidebarLeft">
		<div class="hidden md:block bg-surface-800 h-full">
			<PagesRail />
		</div>
	</svelte:fragment>
	<div class="h-full flex flex-col p-10">
		<slot />
	</div>
</AppShell>
