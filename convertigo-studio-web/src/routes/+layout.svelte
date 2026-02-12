<script>
	import '../app.css';
	import { Toast } from '@skeletonlabs/skeleton-svelte';
	import { afterNavigate, goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import ActionBar from '$lib/admin/components/ActionBar.svelte';
	import Button from '$lib/admin/components/Button.svelte';
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
		if (page.route.id == '/(root)/logout') {
			return;
		}
		var authToken = page.url.hash.match(new RegExp('#authToken=(.*)'));

		if (authToken != null) {
			await Authentication.authenticate({ authToken: authToken[1], authType: 'login' });
		} else {
			await Authentication.checkAuthentication();
		}
		const routeId = page.route.id;
		const isLoginRoute = routeId == '/(root)/login';
		const isRootRoute = routeId == null || routeId == '/(root)';
		const isAdminRoute = page.url.pathname.startsWith(resolve('/admin/'));
		if (!Authentication.canAccessDashboard && !isLoginRoute) {
			if (page.url.pathname.endsWith('.html/') || page.error) {
				goto(resolve('/login/'));
			} else {
				goto(`${resolve('/login/')}?redirect=${encodeURIComponent(page.url.pathname)}`);
			}
			return;
		}
		if (Authentication.canAccessDashboard && (isRootRoute || isLoginRoute)) {
			const target = Authentication.canAccessAdmin ? resolve('/admin/') : resolve('/dashboard/');
			if (page.url.pathname != target) {
				goto(target);
			}
			return;
		}
		if (Authentication.canAccessDashboard && !Authentication.canAccessAdmin && isAdminRoute) {
			goto(resolve('/dashboard/'));
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
			{#if message}
				<pre class="text-wrap">{message}</pre>
			{/if}
			{#if modalAlert?.showStack}
				<pre transition:slide class="text-wrap">{stacktrace}</pre>
			{/if}
			<ActionBar>
				{#if stacktrace}
					<Button
						onclick={() => (modalAlert.showStack = !modalAlert.showStack)}
						class="button-secondary"
						label="Show Details"
					/>
				{/if}
				<Button onclick={close} class="button-primary" label="Close" />
			</ActionBar>
		</Card>
	{/snippet}
</ModalDynamic>
{#snippet toastItem(toast)}
	<Toast {toast}>
		<div class="layout-y-low pr-7">
			{#if toast.title}
				<Toast.Title class="text-sm font-semibold">{toast.title}</Toast.Title>
			{/if}
			{#if toast.description}
				<Toast.Description class="text-sm leading-snug">{toast.description}</Toast.Description>
			{/if}
		</div>
		<Toast.CloseTrigger class="absolute right-2 text-error-500">
			<span aria-hidden="true">✕</span>
		</Toast.CloseTrigger>
	</Toast>
{/snippet}
<Toast.Group {toaster}>
	{#snippet children(toast)}
		{@render toastItem(toast)}
	{/snippet}
</Toast.Group>
{@render children?.()}
