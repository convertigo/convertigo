<script lang="ts">
	import Icon from '@iconify/svelte';
	import { AppRail, AppRailAnchor, AppRailTile } from '@skeletonlabs/skeleton';
	import { browser } from '$app/environment';
    import { goto } from '$app/navigation';

	function navigate(url) {
        if (browser) {
            goto(url);
        }
    }

	let currentTile: number = 0;
</script>

<AppRail width="w-auto" background="bg-surface-700" class="flex flex-col justify-center border-r-[0.5px] border-surface-100" active="bg-surface-900" hover="hover:bg-surface-900">
	
	<!-- --- -->
	<div class="flex grid grid-cols-2">
            <AppRailTile bind:group={currentTile} on:click={() => navigate('/admin-console')} value={1} name="home" title="Home" class="border border-surface-900">
            <svelte:fragment slot="lead">
                <div class="flex justify-center flex-col items-center">
                    <Icon icon="iconoir:home" class="w-[30px] h-[30px]" />
                </div>
            </svelte:fragment>
            <span class="nav-title">Home</span>
        </AppRailTile>

        <!-- Config Tile -->
        <AppRailTile bind:group={currentTile} on:click={() => navigate('/admin-console/config')} value={2} name="config" title="Config" class="border border-surface-900">
            <svelte:fragment slot="lead">
                <div class="flex justify-center flex-col items-center">
                    <Icon icon="ph:gear-thin" class="w-[30px] h-[30px]" />
                </div>
            </svelte:fragment>
            <span class="nav-title">Config</span>
        </AppRailTile>
    </div>

    <div class="flex grid grid-cols-2 ">
        <AppRailTile bind:group={currentTile} name="tile-2" value={3} title="tile-2" class="border border-surface-900">
			<svelte:fragment slot="lead">
				<div class="flex justify-center flex-col items-center">
					<Icon icon="ph:plugs-connected-thin" class="w-[30px] h-[30px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Connections</span>
		</AppRailTile>

        <AppRailTile bind:group={currentTile} name="tile-2" value={4} title="tile-2" class="border border-surface-900">
			<svelte:fragment slot="lead">
				<div class="flex justify-center flex-col items-center">
					<Icon icon="ph:folder-thin" class="w-[30px] h-[30px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Projects</span>
		</AppRailTile>
    </div>


    <div class="flex grid grid-cols-2 ">
        <AppRailTile bind:group={currentTile} name="tile-2" value={5} title="tile-2" class="border border-surface-900">
			<svelte:fragment slot="lead">
				<div class="flex justify-center flex-col items-center">
					<Icon icon="ph:certificate-thin" class="w-[30px] h-[30px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Certificate</span>
		</AppRailTile>

        <AppRailTile bind:group={currentTile} name="tile-2" value={6} title="tile-2" class="border border-surface-900">
			<svelte:fragment slot="lead">
				<div class="flex justify-center flex-col items-center">
					<Icon icon="lets-icons:search-light" class="w-[30px] h-[30px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Logs</span>
		</AppRailTile>
    </div>

    <div class="flex grid grid-cols-2 ">
        <AppRailTile bind:group={currentTile} name="tile-2" value={7} title="tile-2" class="border border-surface-900">
			<svelte:fragment slot="lead">
				<div class="flex justify-center flex-col items-center">
					<Icon icon="ph:video-thin" class="w-[30px] h-[30px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Trace Player</span>
		</AppRailTile>

        <AppRailTile bind:group={currentTile} name="tile-2" value={8} title="tile-2" class="border border-surface-900">
			<svelte:fragment slot="lead">
				<div class="flex justify-center flex-col items-center">
					<Icon icon="material-symbols-light:update" class="w-[30px] h-[30px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Cache</span>
		</AppRailTile>
    </div>

    <div class="flex grid grid-cols-2 ">
        <AppRailTile bind:group={currentTile} name="tile-2" value={9} title="tile-2" class="border border-surface-900">
			<svelte:fragment slot="lead">
				<div class="flex justify-center flex-col items-center">
					<Icon icon="material-symbols-light:schedule-outline" class="w-[30px] h-[30px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Schedular</span>
		</AppRailTile>

        <AppRailTile bind:group={currentTile} name="tile-2" value={10} title="tile-2" class="border border-surface-900">
			<svelte:fragment slot="lead">
				<div class="flex justify-center flex-col items-center">
					<Icon icon="material-symbols-light:key-outline" class="w-[30px] h-[30px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Keys</span>
		</AppRailTile>
    </div>

    <div class="flex grid grid-cols-2 ">
        <AppRailTile bind:group={currentTile} name="tile-2" value={11} title="tile-2" class="border border-surface-900">
			<svelte:fragment slot="lead">
				<div class="flex justify-center flex-col items-center">
					<Icon icon="lets-icons:user-alt-light"  class="w-[30px] h-[30px]"/>
				</div>
			</svelte:fragment>
			<span class="nav-title">Roles</span>
		</AppRailTile>

        <AppRailTile bind:group={currentTile} name="tile-2" value={12} title="tile-2" class="border border-surface-900">
			<svelte:fragment slot="lead">
				<div class="flex justify-center flex-col items-center">
					<Icon icon="et:tools-2" class="w-[30px] h-[30px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Symbols</span>
		</AppRailTile>
    </div>

    <div class="flex grid grid-cols-2 ">
        <AppRailTile bind:group={currentTile} name="tile-2" value={13} title="tile-2" class="border border-surface-900">
			<svelte:fragment slot="lead">
				<div class="flex justify-center flex-col items-center">
					<Icon icon="material-symbols-light:cloud-outline" class="w-[30px] h-[30px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Store</span>
		</AppRailTile>

        <AppRailTile bind:group={currentTile} name="tile-2" value={14} title="tile-2" class="border border-surface-900">
			<svelte:fragment slot="lead">
				<div class="flex justify-center flex-col items-center">
					<Icon icon="material-symbols-light:sync-outline" rotate={1} class="w-[30px] h-[30px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Full Sync</span>
		</AppRailTile>
    </div>

    <div class="flex grid grid-cols-2 ">
        <AppRailTile bind:group={currentTile} name="tile-2" value={15} title="tile-2" class="border border-surface-900">
			<svelte:fragment slot="lead">
				<div class="flex justify-center flex-col items-center">
					<Icon icon="file-icons:test-ruby" class="w-[30px] h-[30px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Test Platform</span>
		</AppRailTile>

        <AppRailTile bind:group={currentTile} name="tile-2" value={16} title="tile-2" class="border border-surface-900">
			<svelte:fragment slot="lead">
				<div class="flex justify-center flex-col items-center">
					<Icon icon="codicon:json" class="w-[30px] h-[30px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Swagger</span>
		</AppRailTile>
    </div>

    <div class="flex grid grid-cols-2 ">
        <AppRailTile bind:group={currentTile} name="tile-2" value={17} title="tile-2" class="border border-surface-900">
			<svelte:fragment slot="lead">
				<div class="flex justify-center flex-col items-center">
					<Icon icon="material-symbols-light:cloud-outline" class="w-[30px] h-[30px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Convertigo</span>
		</AppRailTile>

        <AppRailTile bind:group={currentTile} name="tile-2" value={18} title="tile-2" class="border border-surface-900">
			<svelte:fragment slot="lead">
				<div class="flex justify-center flex-col items-center">
					<Icon icon="material-symbols-light:sync-outline" rotate={1} class="w-[30px] h-[30px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Dev Network</span>
		</AppRailTile>
    </div>

    <div class="flex grid grid-cols-2 ">
        <AppRailTile bind:group={currentTile} name="tile-2" value={19} title="tile-2" class="border border-surface-900">
			<svelte:fragment slot="lead">
				<div class="flex justify-center flex-col items-center">
					<Icon icon="material-symbols-light:cloud-outline" class="w-[30px] h-[30px]" />
				</div>
			</svelte:fragment>
			<span class="nav-title">Documentation</span>
		</AppRailTile>

       
    </div>


</AppRail>

<style>
	.nav-title {
		font-weight: 200;
		font-size: 10px;
	}

</style>
