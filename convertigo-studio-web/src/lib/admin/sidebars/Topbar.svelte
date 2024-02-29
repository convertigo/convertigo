<script>
	import { AppBar, LightSwitch } from '@skeletonlabs/skeleton';
	import Icon from '@iconify/svelte';
	import PagesRailToggle from '../components/PagesRailToggle.svelte';
	import { assets, base } from '$app/paths';
	import { monitorData } from '../stores/monitorStore';
	import Ico from '$lib/utils/Ico.svelte';
</script>

<AppBar
    class="app-bar border-b-[0.5px] dark:border-surface-500 border-surface-200"
	background='dark:bg-surface-700 bg-white'
>
    <svelte:fragment slot="lead">
        <PagesRailToggle />
        
        <h1 class="app-title">
            Convertigo Admin Console
        </h1>
        {#if $monitorData.time > 0}
            <span class="monitor-time"
                >{new Date($monitorData.time).toTimeString().split(' ')[0]}</span
            >
        {/if}
    </svelte:fragment>

    <svelte:fragment slot="trail">
        <a
            href="https://github.com/convertigo/convertigo"
            target="_blank"
            class="github-link"
        >
            <p class="mr-2">Star us on</p>
            <Ico icon="~icons/octicon/logo-github-16" />
        </a>

        <div class="trail-links">
            <a type="button" class="studio-link" href="{base}/studio/">
                <span><Icon icon="material-symbols-light:design-services-outline-sharp" /></span>
                <span class="text-[13px] font-light">Go to studio</span>
            </a>
            <LightSwitch class='light-switch' />
    
			<img src="{assets}/logo.png" alt="logo convertigo" class="logo" />
        </div>
    </svelte:fragment>
</AppBar>

<style lang="postcss">
    .app-bar {
        @apply flex w-full justify-between p-3 dark:bg-surface-800 bg-white;
    }
    .logo {
        @apply w-10 h-full
    }
    .app-title {
        @apply font-normal dark:text-surface-200 text-surface-800 hidden md:block;
    }
    .monitor-time {
        @apply m-2 p-1 border rounded font-mono text-xs;
    }
    .github-link {
        @apply flex items-center btn font-normal hidden sm:flex md:hidden;
    }
    .trail-links {
        @apply pr-4 p-1 flex items-center gap-5;
    }
    .studio-link {
        @apply btn p-1 pl-5 pr-5 btn variant-filled hidden sm:flex md:hidden;
    }
    .icon-size {
        @apply w-7 h-7 ;
    }
	.light-switch {
		@apply mr-5
	}
</style>
