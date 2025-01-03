<script>
	import '../app.postcss';
	import { ToastProvider } from '@skeletonlabs/skeleton-svelte';
	import { afterNavigate, goto } from '$app/navigation';
	import { base } from '$app/paths';
	import { page } from '$app/state';
	import Authentication from '$lib/common/Authentication.svelte';
	import Light from '$lib/common/Light.svelte';
	import ToastSetter from '$lib/utils/ToastSetter.svelte';
	import ModalYesNo from '$lib/common/components/ModalYesNo.svelte';
	import { setContext } from 'svelte';
	/** @type {{children?: import('svelte').Snippet}} */
	let { children } = $props();

	let modalYesNo = $state();

	setContext('modalYesNo', {
		async open(...props) {
			return await modalYesNo.open(...props);
		}
	});

	afterNavigate(async () => {
		await Authentication.checkAuthentication();
		if (!Authentication.authenticated && page.route.id != '/login') {
			goto(`${base}/login/?redirect=${page.url.pathname}`);
		} else if (
			Authentication.authenticated &&
			(page.route.id == '/' || page.route.id == '/login')
		) {
			goto(`${base}/admin/`);
		}
	});

	Light.light;
</script>

<ModalYesNo bind:this={modalYesNo} />
<ToastProvider groupClasses="w-full !items-center !right-0 z-[1000]">
	<ToastSetter />
	{@render children?.()}
</ToastProvider>
