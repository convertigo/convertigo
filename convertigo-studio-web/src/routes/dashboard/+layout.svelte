<script>
	import PageHeader from '$lib/dashboard/components/PageHeader.svelte';
	import PagesRail from '$lib/dashboard/components/PagesRail.svelte';
	import Topbar from '$lib/dashboard/components/Topbar.svelte';
	import { AppShell } from '@skeletonlabs/skeleton';
	import { fade } from 'svelte/transition';
	import { computePosition, autoUpdate, offset, shift, flip, arrow } from '@floating-ui/dom';
	import { storePopup } from '@skeletonlabs/skeleton';
	import { initializeStores, Modal, getModalStore } from '@skeletonlabs/skeleton';
	import ModalProjectSettings from '$lib/dashboard/modals/ModalProjectSettings.svelte';
	import { call, setModalStore, setToastStore } from '$lib/utils/service';
	import ModalConfirm from '$lib/dashboard/modals/ModalConfirm.svelte';

	initializeStores();
	setModalStore(getModalStore());

	storePopup.set({ computePosition, autoUpdate, offset, shift, flip, arrow });

	const modalComponentRegistry = {
		modalSettingsProject: { ref: ModalProjectSettings },
		modalConfirm: { ref: ModalConfirm }
	};
</script>

<Modal components={modalComponentRegistry} />

<AppShell>
	<svelte:fragment slot="header">
		<Topbar />
	</svelte:fragment>
	<svelte:fragment slot="sidebarLeft">
		<div class="hidden md:block bg-surface-800 h-full">
			<PagesRail />
		</div>
	</svelte:fragment>

	<div class="p-10 gap-5 flex flex-col h-full" in:fade>
		<slot />
	</div>
</AppShell>
