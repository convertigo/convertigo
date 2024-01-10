<script>
	import Icon from '@iconify/svelte';
	import { initializeStores, localStorageStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import MainParameters from '$lib/admin-console/config-comp/MainParameters.svelte';
	import AccountSecurity from '$lib/admin-console/config-comp/AccountSecurity.svelte';
	import Analytics from '$lib/admin-console/config-comp/Analytics.svelte';
	import Cache from '$lib/admin-console/config-comp/Cache.svelte';
	import {
		refreshConfigurations,
		configurations
	} from '$lib/admin-console/stores/configurationStore';

	initializeStores();

	let activeComponent = null;
	let theme = localStorageStore('studio.theme', 'skeleton');

	onMount(() => {
		refreshConfigurations();
		changeTheme($theme);
		document.body.setAttribute('data-theme', 'dark-theme');

		activeComponent = MainParameters;
	});

	function changeTheme(e) {
		$theme = typeof e == 'string' ? e : e.target?.value;
		document.body.setAttribute('data-theme', $theme);
	}

	let selectedIndex = 0;
</script>

{#if 'admin' in $configurations}
	{@const category = $configurations?.admin?.category[selectedIndex]}
	<!-- {@debug $configurations} -->
	<div class="flex flex-col h-full p-10 w-full">
		<div class="flex flex-col grid grid-cols-6 gap-10">
			<div class="nav-sidebar">
				{#each $configurations?.admin?.category as category, index}
					<button class="navbutton" on:click={() => (selectedIndex = index)}>
						{category['@_displayName']}
						<Icon icon="uil:arrow-up" rotate={1} class="text-xl" />
					</button>
				{/each}
			</div>
			<div class="content-area">
				<div>
					<h1 class="text-[15px]">{category['@_displayName']}</h1>
					{#each category.property as property}
						<h2 class="mt-5 text-[14px]">{property['@_description']}</h2>
						<!-- {#if typeof value === 'string'}
						<input
							type="text"
							class="text-black w-[60%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
							placeholder={key}
							bind:value={settings[key]}
							on:blur={() => handleUpdateSetting(key)}
						/>
					{:else}
						<label class="mt-10 items-center flex">
							<input
								type="checkbox"
								bind:checked={settings[key]}
								on:click={() => toggleXsrfSetting(key)}
							/>
							<p class="ml-5">
								Enable XSRF protection for {key === 'xsrfAdminEnabled'
									? 'Administration Console'
									: 'projects'}
							</p>
						</label>
					{/if} -->
					{/each}
				</div>
			</div>
		</div>
	</div>
{/if}

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
