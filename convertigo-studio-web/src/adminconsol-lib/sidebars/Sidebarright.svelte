<script lang="ts">
	import Icon from '@iconify/svelte';
	import { AppRail, AppRailAnchor, AppRailTile } from '@skeletonlabs/skeleton';

	let currentTile: number = 0;
</script>

<AppRail width="w-auto p-4" background="bg-surface-600">

        <AppRailTile bind:group={currentTile} name="tile-2" value={1} title="tile-2" class="mt-5">
			<svelte:fragment slot="lead">
				<div>
					<Icon icon="ph:gear-thin" class="w-[35px] h-[35px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Config</span>
		</AppRailTile>

        <AppRailTile bind:group={currentTile} name="tile-2" value={1} title="tile-2" class="mt-5">
			<svelte:fragment slot="lead">
				<div>
					<Icon icon="ph:gear-thin" class="w-[35px] h-[35px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Config</span>
		</AppRailTile>

        <AppRailTile bind:group={currentTile} name="tile-2" value={1} title="tile-2" class="mt-5">
			<svelte:fragment slot="lead">
				<div>
					<Icon icon="ph:gear-thin" class="w-[35px] h-[35px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Config</span>
		</AppRailTile>

        <AppRailTile bind:group={currentTile} name="tile-2" value={1} title="tile-2" class="mt-5">
			<svelte:fragment slot="lead">
				<div>
					<Icon icon="ph:gear-thin" class="w-[35px] h-[35px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Config</span>
		</AppRailTile>

        <AppRailTile bind:group={currentTile} name="tile-2" value={1} title="tile-2" class="mt-5">
			<svelte:fragment slot="lead">
				<div>
					<Icon icon="ph:gear-thin" class="w-[35px] h-[35px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Config</span>
		</AppRailTile>

        <AppRailTile bind:group={currentTile} name="tile-2" value={1} title="tile-2" class="mt-5">
			<svelte:fragment slot="lead">
				<div>
					<Icon icon="ph:gear-thin" class="w-[35px] h-[35px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Config</span>
		</AppRailTile>

        <AppRailTile bind:group={currentTile} name="tile-2" value={1} title="tile-2" class="mt-5">
			<svelte:fragment slot="lead">
				<div>
					<Icon icon="ph:gear-thin" class="w-[35px] h-[35px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Config</span>
		</AppRailTile>

</AppRail>

<style>
	.nav-title {
		font-weight: 300;
		font-size: smaller;
	}
</style>
