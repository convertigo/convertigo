<script>
	import { AppShell, initializeStores } from '@skeletonlabs/skeleton';
	import SidebarLeft from '$lib/admin-console/sidebars/SidebarLeft.svelte';
	import Topbar from '$lib/admin-console/sidebars/Topbar.svelte';
	import { Modal, Toast } from '@skeletonlabs/skeleton';
	import ModalAddSymbol from '$lib/admin-console/modals/ModalAddSymbol.svelte';

	initializeStores();

	const modalComponentRegistry = {
		modalAddSymbols: { ref: ModalAddSymbol }
	};

</script>

<AppShell>
	<Modal components={modalComponentRegistry} />
	<Toast/>

	<svelte:fragment slot="header">
		<Topbar />
	</svelte:fragment>
	<svelte:fragment slot="sidebarLeft"><SidebarLeft /></svelte:fragment>
	<svelte:fragment slot="sidebarRight"></svelte:fragment>

	<slot />
</AppShell>
