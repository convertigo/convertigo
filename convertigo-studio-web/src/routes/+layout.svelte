<script>
	import '../app.postcss';

	// Floating UI for Popups
	import { computePosition, autoUpdate, flip, shift, offset, arrow } from '@floating-ui/dom';
	import { Modal, initializeStores, storePopup } from '@skeletonlabs/skeleton';
	import { afterNavigate, goto } from '$app/navigation';
	import { call } from '$lib/utils/service';
	import { authenticated } from '$lib/utils/loadingStore';
	import { base } from '$app/paths';
	import { page } from '$app/stores';
	import ModalProjects from '$lib/admin/modals/ModalProjects.svelte';
	import ModalSymbols from '$lib/admin/modals/ModalSymbols.svelte';
	import ModalRoles from '$lib/admin/modals/ModalRoles.svelte';
	import ModalScheduler from '$lib/admin/modals/ModalScheduler.svelte';
	import ModalWarning from '$lib/admin/modals/ModalWarning.svelte';

	initializeStores();

	afterNavigate(() => {
		call('engine.CheckAuthentication').then((res) => {
			$authenticated = res.admin.authenticated;
			if (!$authenticated) {
				goto(`${base}/login/`);
			} else if ($page.route.id == '/') {
				goto(`${base}/admin/`);
			}
		});
	});

	const modalComponentRegistry = {
		modalSymbols: { ref: ModalSymbols },
		modalProjects: { ref: ModalProjects },
		modalRoles: { ref: ModalRoles },
		modalScheduler: { ref: ModalScheduler },
		modalWarning: { ref: ModalWarning }
	};

	storePopup.set({ computePosition, autoUpdate, flip, shift, offset, arrow });
</script>

<Modal components={modalComponentRegistry} />

<slot />
