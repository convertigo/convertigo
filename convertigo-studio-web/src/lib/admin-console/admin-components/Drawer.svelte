<script lang="ts">
	import Icon from '@iconify/svelte';
	import { AppRail, AppRailTile, Drawer, getDrawerStore } from '@skeletonlabs/skeleton';
	import { browser } from '$app/environment';
	import { goto } from '$app/navigation';
	import { initializeStores } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { tilesStore } from '../stores/tilesStore';

	initializeStores();

	let currentTileDrawer: number = 0;
    $: tiles = $tilesStore;

	const drawerStore = getDrawerStore();

	onMount(() => {});

	function triggerDrawer() {
		const drawerSettings: any = {
			id: 'monDrawer',
			position: 'left',
			bgDrawer: 'bg-surface-800',
			bgBackdrop: 'bg-black/50',
			width: 'w-auto',
			meta: {
				info: 'Informations supplémentaires'
			}
		};

		drawerStore.open(drawerSettings);
	}

	function navigate(url) {
		if (browser) {
			goto(url);
		}
	}
</script>

<button class="md:hidden pl-2 pt-2" on:click={triggerDrawer}
	><Icon icon="iconamoon:menu-burger-horizontal-thin" class="w-8 h-8" /></button
>



<Drawer class="">
	<AppRail
		id="drawer-apprail"
		width="w-auto"
		background="bg-surface-800"
		class="flex flex-col justify-center border-r-[0.5px] border-surface-500 mt-10"
		active="bg-surface-900"
		hover="hover:bg-surface-900"
	>
		<div class="flex grid grid-cols-2">
			{#each tiles as tile}
				<AppRailTile
					bind:group={currentTileDrawer}
					on:click={() => navigate(tile.url)}
					value={tile.value}
					name={tile.title.toLowerCase()}
					title={tile.title}
					class="border border-surface-900"
				>
					<svelte:fragment slot="lead">
						<div class="flex justify-center flex-col items-center">
							<Icon icon={tile.icon} class="w-[30px] h-[30px]" />
						</div>
					</svelte:fragment>
					<span class="nav-title">{tile.title}</span>
				</AppRailTile>
			{/each}
		</div>
	</AppRail>
</Drawer>

<style>
	.nav-title {
		font-weight: 200;
		font-size: 10px;
	}
</style>
