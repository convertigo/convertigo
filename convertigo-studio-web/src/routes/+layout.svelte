<script>
	import '../app.css';
	import { Toaster } from '@skeletonlabs/skeleton-svelte';
	import { afterNavigate, goto } from '$app/navigation';
	import { base } from '$app/paths';
	import { page } from '$app/state';
	import Card from '$lib/admin/components/Card.svelte';
	import Authentication from '$lib/common/Authentication.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import ModalYesNo from '$lib/common/components/ModalYesNo.svelte';
	import Light from '$lib/common/Light.svelte';
	import { setModalAlert, toaster } from '$lib/utils/service';
	import { getContext, setContext } from 'svelte';
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
		var authToken = page.url.hash.match(new RegExp('#authToken=(.*)'));

		if (authToken != null) {
			await Authentication.authenticate({ authToken: authToken[1], authType: 'login' });
		} else {
			await Authentication.checkAuthentication();
		}
		if (!Authentication.authenticated && page.route.id != '/login') {
			console.log('page', JSON.stringify(page, null, 2));
			if (page.url.pathname.endsWith('.html/') || page.error) {
				goto(`${base}/login/`);
			} else {
				goto(`${base}/login/?redirect=${encodeURIComponent(page.url.pathname)}`);
			}
		} else if (
			Authentication.authenticated &&
			(page.route.id == null || page.route.id == '/' || page.route.id == '/login')
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
			<div class="layout-x w-full justify-end">
				{#if stacktrace}
					<button
						onclick={() => (modalAlert.showStack = !modalAlert.showStack)}
						class="button-tertiary">Show Details</button
					>
				{/if}
				<button onclick={close} class="button-error">Close</button>
			</div>
		</Card>
	{/snippet}
</ModalDynamic>
<Toaster {toaster}></Toaster>
{@render children?.()}
