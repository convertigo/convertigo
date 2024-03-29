<script>
	import '../app.postcss';

	// Floating UI for Popups
	import { computePosition, autoUpdate, flip, shift, offset, arrow } from '@floating-ui/dom';
	import { Modal, Toast, initializeStores, storePopup } from '@skeletonlabs/skeleton';
	import { goto } from '$app/navigation';
	import { call } from '$lib/utils/service';
	import { authenticated } from '$lib/utils/loadingStore';
	import { base } from '$app/paths';
	import { page } from '$app/stores';
	import ModalProjects from '$lib/admin/modals/ModalProjects.svelte';
	import ModalSymbols from '$lib/admin/modals/ModalSymbols.svelte';
	import ModalRoles from '$lib/admin/modals/ModalRoles.svelte';
	import ModalScheduler from '$lib/admin/modals/ModalScheduler.svelte';
	import ModalWarning from '$lib/admin/modals/ModalWarning.svelte';
	import ModalHome from '$lib/admin/modals/ModalHome.svelte';
	import { onMount } from 'svelte';
	import ModalSessionLegend from '$lib/admin/modals/ModalSessionLegend.svelte';

	initializeStores();

	onMount(() => {
		call('engine.CheckAuthentication').then((res) => {
			$authenticated = res.admin.authenticated;
			if (!$authenticated && $page.route.id != '/login') {
				goto(`${base}/login/`);
			} else if ($authenticated && ($page.route.id == '/' || $page.route.id == '/login')) {
				goto(`${base}/admin/`);
			}
		});
	});

	const modalComponentRegistry = {
		modalHome: { ref: ModalHome },
		modalProjects: { ref: ModalProjects },
		modalRoles: { ref: ModalRoles },
		modalScheduler: { ref: ModalScheduler },
		modalSymbols: { ref: ModalSymbols },
		modalWarning: { ref: ModalWarning },
		modalSessionLegend: { ref: ModalSessionLegend }
	};

	storePopup.set({ computePosition, autoUpdate, flip, shift, offset, arrow });
</script>

<Modal components={modalComponentRegistry} />

<Toast />

<slot />
