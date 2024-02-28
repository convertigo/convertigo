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
	import ModalAddSymbol from '$lib/admin/modals/ModalAddSymbol.svelte';
	import ModalProjects from '$lib/admin/modals/ModalProjects.svelte';
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
		modalAddSymbols: { ref: ModalAddSymbol },
		modalProjects: { ref: ModalProjects }
	};

	storePopup.set({ computePosition, autoUpdate, flip, shift, offset, arrow });
</script>

<Modal components={modalComponentRegistry} />
<slot />
