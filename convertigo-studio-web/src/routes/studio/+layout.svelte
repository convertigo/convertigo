<script>
	import { browser } from '$app/environment';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import Authentication from '$lib/common/Authentication.svelte.js';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import { resolve } from '$lib/utils/route';
	import { onMount } from 'svelte';

	/** @type {{ children?: import('svelte').Snippet }} */
	let { children } = $props();

	let authorized = $derived(Authentication.hasRole('WEB_ADMIN'));

	function fallbackUrl() {
		if (!Authentication.authenticated) {
			const redirect = encodeURIComponent(page.url.pathname + page.url.search + page.url.hash);
			return `${resolve('/login/')}${redirect ? `?redirect=${redirect}` : ''}`;
		}
		if (Authentication.defaultAdminPage) {
			return resolve(/** @type {any} */ (Authentication.defaultAdminPage));
		}
		if (Authentication.canAccessDashboard) {
			return resolve('/dashboard/');
		}
		return resolve('/login/');
	}

	onMount(() => {
		if (!authorized) {
			void goto(fallbackUrl());
		}
	});
</script>

{#if !browser || authorized}
	<div class="studio-route">
		{@render children?.()}
	</div>
{:else}
	<div class="studio-route studio-route--pending">
		<AutoPlaceholder loading={true} class="w-64" />
	</div>
{/if}

<style>
	.studio-route {
		min-height: 100vh;
		background: var(--color-surface-50-950);
		color: var(--color-surface-950-50);
	}

	.studio-route--pending {
		display: grid;
		place-items: center;
	}
</style>
