<script>
	import '../app.postcss';
	import { ToastProvider } from '@skeletonlabs/skeleton-svelte';
	import { afterNavigate, goto } from '$app/navigation';
	import { base } from '$app/paths';
	import { page } from '$app/state';
	import Authentication from '$lib/common/Authentication.svelte';
	import Light from '$lib/common/Light.svelte';
	import ToastSetter from '$lib/utils/ToastSetter.svelte';
	/** @type {{children?: import('svelte').Snippet}} */
	let { children } = $props();

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

<ToastProvider groupClasses="w-full !items-center !right-0">
	<ToastSetter />
	{@render children?.()}
</ToastProvider>
