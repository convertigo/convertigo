<script>
	import { AppShell, Drawer } from '@skeletonlabs/skeleton';
	import Topbar from '$lib/admin/sidebars/Topbar.svelte';
	import { Modal, Toast } from '@skeletonlabs/skeleton';
	import ModalAddSymbol from '$lib/admin/modals/ModalAddSymbol.svelte';
	import PagesRail from '$lib/admin/components/PagesRail.svelte';
	import PagesRailToggle from '$lib/admin/components/PagesRailToggle.svelte';

	const modalComponentRegistry = {
		modalAddSymbols: { ref: ModalAddSymbol }
	};
</script>

<AppShell>
	<Drawer>
		<PagesRailToggle open={false} />
		<PagesRail />
	</Drawer>
	<Modal components={modalComponentRegistry} />
	<Toast />

	<svelte:fragment slot="header">
		<Topbar />
	</svelte:fragment>
	<svelte:fragment slot="sidebarLeft">
		<div class="hidden md:block bg-surface-800 h-full">
			<PagesRail />
		</div>
	</svelte:fragment>
	<svelte:fragment slot="sidebarRight"></svelte:fragment>
	<div class="p-5 flex flex-col h-full">
		<slot />
	</div>
</AppShell>
