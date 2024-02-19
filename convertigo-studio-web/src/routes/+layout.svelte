<script>
	import '../app.postcss';

	// Floating UI for Popups
	import { computePosition, autoUpdate, flip, shift, offset, arrow } from '@floating-ui/dom';
	import { initializeStores, storePopup } from '@skeletonlabs/skeleton';
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { call } from '$lib/utils/service';
	import { authenticated } from '$lib/utils/loadingStore';
	import { base } from '$app/paths';
	import { page } from '$app/stores';
	initializeStores();

	onMount(() => {
		call('engine.CheckAuthentication').then((res) => {
			$authenticated = res.admin.authenticated;
			if (!$authenticated) {
				goto(`${base}/login/`);
			} else if ($page.route.id == '/') {
				goto(`${base}/admin/`);
			}
		});
	});
	storePopup.set({ computePosition, autoUpdate, flip, shift, offset, arrow });
</script>

<slot />
