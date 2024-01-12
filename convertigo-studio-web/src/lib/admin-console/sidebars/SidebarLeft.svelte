<script lang="ts">
	import Icon from '@iconify/svelte';
	import { AppRail, AppRailTile} from '@skeletonlabs/skeleton';
	import { browser } from '$app/environment';
	import { goto } from '$app/navigation';
	import { initializeStores } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { tilesStore, tilesStoreMiscleanous } from '../stores/tilesStore';


	initializeStores();

	let currentTileMain: number = 0;
	$: tiles = $tilesStore;
	$: miscellaneous = $tilesStoreMiscleanous;

	onMount(() => {});


	function navigate(url) {
		if (browser) {
			goto(url);
		}
	}
</script>

<div class="hidden md:block">
	<AppRail
		id="main-apprail"
		height="h-full"
		width="w-auto"
		background="bg-surface-800"
		class="flex flex-col justify-center border-r-[0.5px] border-surface-500"
		active="bg-surface-900"
		hover="hover:bg-surface-900"
	>
		<div class="flex grid grid-cols-2 mt-10 mb-5">
			{#each tiles as tile}
				<AppRailTile
					bind:group={currentTileMain}
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

		<div class="flex justify-center">
			<separator class="border-[1px] flex w-[70%] border-surface-100"/>
		</div>
		


		<div class="flex grid grid-cols-2 mt-5 mb-10">
			{#each miscellaneous as tile}
			<AppRailTile
				bind:group={currentTileMain}
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
</div>

<style>
	.nav-title {
		font-weight: 200;
		font-size: 10px;
	}
</style>
