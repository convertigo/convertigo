<script>
	import '../app.postcss';

	// Floating UI for Popups
	import { computePosition, autoUpdate, flip, shift, offset, arrow } from '@floating-ui/dom';
	import {
		Modal,
		Toast,
		getModalStore,
		getToastStore,
		initializeStores,
		storePopup
	} from '@skeletonlabs/skeleton';
	import { afterNavigate, goto } from '$app/navigation';
	import { call, setModalStore, setToastStore } from '$lib/utils/service';
	import { authenticated } from '$lib/utils/loadingStore';
	import { base } from '$app/paths';
	import { page } from '$app/stores';
	import ModalProjects from '$lib/admin/modals/ModalProjects.svelte';
	import ModalSymbols from '$lib/admin/modals/ModalSymbols.svelte';
	import ModalRoles from '$lib/admin/modals/ModalRoles.svelte';
	import ModalScheduler from '$lib/admin/modals/ModalScheduler.svelte';
	import ModalWarning from '$lib/admin/modals/ModalWarning.svelte';
	import ModalHome from '$lib/admin/modals/ModalHome.svelte';
	import ModalSessionLegend from '$lib/admin/modals/ModalSessionLegend.svelte';
	import ModalCertificates from '$lib/admin/modals/ModalCertificates.svelte';
	import ModalLoading from '$lib/admin/modals/ModalLoading.svelte';
	import ModalLogs from '$lib/admin/modals/ModalLogs.svelte';
	import ModalProjectSettings from '$lib/dashboard/modals/ModalProjectSettings.svelte';
	import ModalConfirm from '$lib/dashboard/modals/ModalConfirm.svelte';
	import ModalInfo from '$lib/dashboard/modals/ModalInfo.svelte';

	initializeStores();
	setToastStore(getToastStore());
	setModalStore(getModalStore());

	afterNavigate(() => {
		call('engine.CheckAuthentication').then((res) => {
			$authenticated = res.admin.authenticated;
			if (!$authenticated && $page.route.id != '/login') {
				goto(`${base}/login/?redirect=${$page.url.pathname}`);
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
		modalSessionLegend: { ref: ModalSessionLegend },
		modalCertificates: { ref: ModalCertificates },
		modalLoading: { ref: ModalLoading },
		modalLogs: { ref: ModalLogs },
		modalSettingsProject: { ref: ModalProjectSettings },
		modalConfirm: { ref: ModalConfirm },
		modalInfo: { ref: ModalInfo }
	};

	storePopup.set({ computePosition, autoUpdate, flip, shift, offset, arrow });
</script>

<Modal components={modalComponentRegistry} />

<Toast zIndex="z-[1000]" />

<slot />
