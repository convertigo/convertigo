<script>
	import Icon from '@iconify/svelte';
	import {
		initializeStores,
		localStorageStore
	} from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import MainParameters from '../../../adminconsol-lib/config-comp/MainParameters.svelte';
	import AccountSecurity from '../../../adminconsol-lib/config-comp/AccountSecurity.svelte';
	import Analytics from '../../../adminconsol-lib/config-comp/Analytics.svelte';
	import Cache from '../../../adminconsol-lib/config-comp/Cache.svelte';

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

	let currentTile = 0;
</script>

<div class="flex flex-col h-full p-10 w-full">
	<div class="flex flex-col grid grid-cols-6 gap-10">
		<div class="flex flex-col h-auto col-span-1 gap-2 p-4 bg-surface-600">
			<button class="flex navbutton" on:click={() => (activeComponent = MainParameters)}
				>Main parameters <Icon icon="uil:arrow-up" rotate={1} class="text-xl" /></button
			>
			<button class="flex navbutton" on:click={() => (activeComponent = AccountSecurity)}
				>Accounts and security <Icon icon="uil:arrow-up" rotate={1} class="text-xl" /></button
			>
			<button class="flex navbutton" on:click={() => (activeComponent = Analytics)}
				>Analytics <Icon icon="uil:arrow-up" rotate={1} class="text-xl" /></button
			>
			<button class="flex navbutton" on:click={() => (activeComponent = Cache)}
				>Cache <Icon icon="uil:arrow-up" rotate={1} class="text-xl" /></button
			>
			<button class="flex navbutton"
				>Full sync <Icon icon="uil:arrow-up" rotate={1} class="text-xl" /></button
			>
			<button class="flex navbutton"
				>HTTP client <Icon icon="uil:arrow-up" rotate={1} class="text-xl" /></button
			>
			<button class="flex navbutton"
				>Legacy Carioca portal <Icon icon="uil:arrow-up" rotate={1} class="text-xl" /></button
			>
			<button class="flex navbutton"
				>Logs <Icon icon="uil:arrow-up" rotate={1} class="text-xl" /></button
			>
			<button class="flex navbutton"
				>Mobile builder <Icon icon="uil:arrow-up" rotate={1} class="text-xl" /></button
			>
			<button class="flex navbutton"
				>Network <Icon icon="uil:arrow-up" rotate={1} class="text-xl" /></button
			>
			<button class="flex navbutton"
				>Notifications <Icon icon="uil:arrow-up" rotate={1} class="text-xl" /></button
			>
			<button class="flex navbutton"
				>Proxy <Icon icon="uil:arrow-up" rotate={1} class="text-xl" /></button
			>
			<button class="flex navbutton"
				>Real-time activity monitoring <Icon
					icon="uil:arrow-up"
					rotate={1}
					class="text-xl"
				/></button
			>
			<button class="flex navbutton"
				>SSL <Icon icon="uil:arrow-up" rotate={1} class="text-xl" /></button
			>
			<button class="flex navbutton"
				>XML generation <Icon icon="uil:arrow-up" rotate={1} class="text-xl" /></button
			>
		</div>

		<div class="flex flex-col h-auto col-span-5 p-5 bg-surface-600">
			{#if activeComponent === MainParameters}
				<MainParameters />
			{:else if activeComponent === AccountSecurity}
				<AccountSecurity />
			{:else if activeComponent === Analytics}
				<Analytics />
			{:else if activeComponent === Cache}
				<Cache />
			{/if}
		</div>
	</div>
</div>

<style>
	.navbutton {
		@apply text-[12px] font-light text-start p-1 border-b-[0.5px] border-b-surface-500 justify-between items-center;
	}
</style>
