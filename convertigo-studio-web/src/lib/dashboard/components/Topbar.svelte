<script>
	import { AppBar } from '@skeletonlabs/skeleton-svelte';
	import { page } from '$app/stores';
	import Ico from '$lib/utils/Ico.svelte';
	import { slide } from 'svelte/transition';
	import PagesRailToggle from '$lib/admin/components/PagesRailToggle.svelte';
	import { onMount } from 'svelte';
	import { projectsCheck, projectsStore } from '$lib/admin/stores/projectsStore';
	import Time from '$lib/common/Time.svelte';
	import LightSwitch from '$lib/common/components/LightSwitch.svelte';

	let { showLeft = $bindable(), showDrawer = $bindable() } = $props();

	let project = $state();

	let isBackend = $derived($page.url.pathname.includes('backend'));
	let isFrontend = $derived($page.url.pathname.includes('frontend'));
	let isPlatforms = $derived($page.url.pathname.includes('platforms'));
	let hasFrontend = $derived(project?.['@_hasFrontend'] == 'true');
	let hasPlatforms = $derived(project?.['@_hasPlatform'] == 'true');

	onMount(() => {
		const unsubscribe = page.subscribe(($page) => {
			projectsCheck().then(() => {
				project = $projectsStore.find((project) => project['@_name'] == $page.params?.project);
			});
		});
		return () => unsubscribe();
	});
</script>

<AppBar background="bg-surface-200-800" padding="p-0">
	{#snippet lead()}
		<PagesRailToggle class="show-md" bind:state={showDrawer} />
		<PagesRailToggle class="hide-md" bind:state={showLeft} />
		{#if Time.isSameTime}
			<span class="monitor-time">{Time.browserTime}</span>
		{:else}
			<span class="monitor-time hide-md">you {Time.browserTime}</span>
			<span class="monitor-time">server {Time.serverTime} {Time.serverTimezone}</span>
		{/if}
	{/snippet}

	{#if project}
		{@const parts = [
			{
				href: '../../',
				icon: 'lucide:layout-panel-top',
				state: false,
				show: true
			},
			{
				href: `../../#${$page.params.project}`,
				icon: 'ph:plugs-connected-thin',
				state: false,
				show: project.ref?.length > 0
			},
			{
				name: 'Backend',
				href: '../backend/',
				icon: 'ph:gear-six-thin',
				state: isBackend,
				show: true
			},
			{
				name: 'Frontend',
				href: '../frontend/',
				icon: 'ph:video-thin',
				state: isFrontend,
				show: hasFrontend
			},
			{
				name: 'Platforms',
				href: '../platforms/',
				icon: 'ph:package-thin',
				state: isPlatforms,
				show: hasPlatforms
			}
		].filter((part) => part.show)}
		<div class="flex flex-wrap" transition:slide={{ axis: 'y' }}>
			{#each parts as { name, href, cls, icon, state }, i}
				<a
					{href}
					class:preset-filled-secondary={state}
					class:preset-ghost-secondary={!state}
					class:rounded-none={i > 0 && i < parts.length - 1}
					class:rounded-r-none={i == 0}
					class:rounded-l-none={i == parts.length - 1}
					class="btn hover:preset-filled-secondary"
					><span><Ico {icon} size="nav" /></span>{#if name}<span>{name}</span>{/if}</a
				>
			{/each}
		</div>
	{/if}
	{#snippet trail()}
		<LightSwitch />
	{/snippet}
</AppBar>

<style lang="postcss">
	.app-bar {
		@apply flex w-full justify-between dark:bg-secondary-800 bg-white;
	}
	.logo-mobile {
		@apply w-10 h-full md:hidden;
	}
	.logo-desktop {
		@apply w-10 h-full hidden md:block;
	}
	.app-title {
		@apply font-normal dark:text-surface-200 text-surface-800 hidden md:block;
	}
	.monitor-time {
		@apply m-2 p-1 border rounded font-mono text-xs hidden md:block;
	}
	.github-link {
		@apply flex items-center btn font-normal hidden md:inline-flex;
	}
	.trail-links {
		@apply pr-4 p-1 flex items-center gap-5;
	}
	.studio-link {
		@apply items-center p-1 pl-5 pr-5 hidden md:inline-flex rounded;
	}

	.icon-size {
		@apply w-7 h-7;
	}
	.light-switch {
		@apply mr-5;
	}
</style>
