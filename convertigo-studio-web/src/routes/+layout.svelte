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
	import { getContext, setContext } from 'svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import { setModalAlert } from '$lib/utils/service';
	import Card from '$lib/admin/components/Card.svelte';
	import { slide } from 'svelte/transition';
	/** @type {{children?: import('svelte').Snippet}} */
	let { children } = $props();

	let modalYesNo = $state();

	setContext('modalYesNo', {
		open: async (...props) => await modalYesNo.open(...props)
	});

	let modalAlert = $state();
	setContext('modalAlert', {
		open: async (...params) => {
			modalAlert.showStack = params?.[0]?.showStack;
			await modalAlert.open(...params);
		}
	});
	setModalAlert(getContext('modalAlert'));

	afterNavigate(async () => {
		if (page.route.id == '/logout') {
			return;
		}
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
<ModalDynamic bind:this={modalAlert}>
	{#snippet children({ close, params: { message, exception, stacktrace } })}
		<Card title="Error" class="preset-tonal-error">
			{#snippet cornerOption()}
				<div class="text-end">{exception}</div>
			{/snippet}
			<pre class="text-wrap">{message}</pre>
			{#if modalAlert.showStack}
				<pre transition:slide class="text-wrap">{stacktrace}</pre>
			{/if}
			<div class="w-full layout-x justify-end">
				{#if stacktrace}
					<button
						onclick={() => (modalAlert.showStack = !modalAlert.showStack)}
						class="yellow-button">Show Details</button
					>
				{/if}
				<button onclick={close} class="cancel-button">Close</button>
			</div>
		</Card>
	{/snippet}
</ModalDynamic>
<ToastProvider groupClasses="w-full !items-center !right-0 z-[1000]">
	<ToastSetter />
	{@render children?.()}
</ToastProvider>
