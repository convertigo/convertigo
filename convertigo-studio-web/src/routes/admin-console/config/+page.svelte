<script>
	import Icon from '@iconify/svelte';
	import {
		AppRail,
		AppRailAnchor,
		AppRailTile,
		TabAnchor,
		TabGroup,
		initializeStores,
		localStorageStore
	} from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { writable } from 'svelte/store';
	import Mainparameters from '../../../adminconsol-lib/config-comp/Mainparameters.svelte';
	import Accountsecurity from '../../../adminconsol-lib/config-comp/Accountsecurity.svelte';

	initializeStores();

	let activeComponent = null;

	let theme = localStorageStore('studio.theme', 'skeleton');

	onMount(() => {
		changeTheme($theme);
		document.body.setAttribute('data-theme', 'dark-theme');
	});

	function changeTheme(e) {
		$theme = typeof e == 'string' ? e : e.target?.value;
		document.body.setAttribute('data-theme', $theme);
	}

	let currentTile = 0;
</script>

<div class="flex flex-col h-full p-10 w-full">
	<div class="flex flex-col grid grid-cols-6 gap-10">
		<div class="flex flex-col h-auto col-span-1 gap-2 p-4">
			<button class="flex navbutton" on:click={() => (activeComponent = Mainparameters)}
				>Main parameters <Icon icon="uil:arrow-up" rotate={1} class="text-xl" /></button
			>
			<button class="flex navbutton" on:click={() => (activeComponent = Accountsecurity)}
				>Accounts and security <Icon icon="uil:arrow-up" rotate={1} class="text-xl" /></button
			>
			<button class="flex navbutton"
				>Analytics <Icon icon="uil:arrow-up" rotate={1} class="text-xl" /></button
			>
			<button class="flex navbutton"
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

		<div class="flex flex-col h-auto col-span-5 p-5 border-l-2 border-surface-100 bg-surface-800">
            
			{#if activeComponent === Mainparameters}
				<Mainparameters />
			{:else if activeComponent === Accountsecurity}
				<Accountsecurity />
            
			{/if}
		</div>
	</div>
</div>

<style>
	.navbutton {
		@apply text-[12px] font-light text-start p-1 border-b-[0.5px] border-b-surface-500 justify-between items-center;
	}
</style>
