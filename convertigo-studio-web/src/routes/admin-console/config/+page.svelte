<script>
	import Icon from '@iconify/svelte';
	import { initializeStores, localStorageStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import MainParameters from '$lib/admin-console/config-comp/MainParameters.svelte';
	import AccountSecurity from '$lib/admin-console/config-comp/AccountSecurity.svelte';
	import Analytics from '$lib/admin-console/config-comp/Analytics.svelte';
	import Cache from '$lib/admin-console/config-comp/Cache.svelte';

	initializeStores();

	let activeComponent = null;
	let theme = localStorageStore('studio.theme', 'skeleton');

	onMount(() => {
		changeTheme($theme);
		document.body.setAttribute('data-theme', 'dark-theme');

		activeComponent = MainParameters;
	});

	function changeTheme(e) {
		$theme = typeof e == 'string' ? e : e.target?.value;
		document.body.setAttribute('data-theme', $theme);
	}

	const components = [
		{ component: MainParameters, label: 'Main parameters' },
		{ component: AccountSecurity, label: 'Account & security' },
		{ component: Analytics, label: 'Analytics' },
		{ component: Cache, label: 'Cache' },
		{ component: null, label: 'Full sync' },
		{ component: null, label: 'HTTP client ' },
		{ component: null, label: 'Legacy Carioca portal' },
		{ component: null, label: 'Logs' },
		{ component: null, label: 'Mobile builder' },
		{ component: null, label: 'Network' },
		{ component: null, label: 'Notifications' },
		{ component: null, label: 'Proxy' },
		{ component: null, label: 'Real-time activity monitoring' },
		{ component: null, label: 'SSL' },
		{ component: null, label: 'XML generation' }
	];

	function setActiveComponent(component) {
		if (component) {
			activeComponent = component;
		} else {
			console.log("Ce composant n'est pas encore implémenté.");
		}
	}
</script>

<div class="flex flex-col h-full p-10 w-full">
	<div class="flex flex-col grid grid-cols-6 gap-10">
		<div class="nav-sidebar">
			{#each components as { component, label }}
				<button class="navbutton" on:click={() => setActiveComponent(component)}>
					{label}
					<Icon icon="uil:arrow-up" rotate={1} class="text-xl" />
				</button>
			{/each}
		</div>

		<div class="content-area">
			{#if activeComponent}
				<svelte:component this={activeComponent} />
			{/if}
		</div>
	</div>
</div>

<style>
	.navbutton {
		@apply flex text-[12px] font-light text-start p-1 border-b-[0.5px] border-b-surface-500 justify-between items-center;
	}
	.nav-sidebar {
		@apply flex flex-col h-auto col-span-1 gap-2 p-4 bg-surface-800;
	}
	.content-area {
		@apply flex flex-col h-auto col-span-5 p-5 bg-surface-800;
	}
</style>
