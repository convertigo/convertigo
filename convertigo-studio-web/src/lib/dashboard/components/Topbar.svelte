<script>
	import { AppBar, LightSwitch } from '@skeletonlabs/skeleton';
	import { monitorData } from '$lib/admin/stores/monitorStore';
	import { page } from '$app/stores';
	import Ico from '$lib/utils/Ico.svelte';
	import { slide } from 'svelte/transition';
	import PagesRailToggle from '$lib/admin/components/PagesRailToggle.svelte';
	import { onMount } from 'svelte';
	import { projectsCheck, projectsStore } from '$lib/admin/stores/projectsStore';
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

<AppBar
	class="app-bar border-b-[0.5px] dark:border-surface-500 border-surface-200 py-2 px-10"
	background="dark:bg-surface-700 bg-surface-100"
	slotDefault="flex justify-center"
	padding="p-0"
>
	<svelte:fragment slot="lead">
		<PagesRailToggle />
		{#if $monitorData.time > 0}
			<span class="monitor-time">{new Date($monitorData.time).toTimeString().split(' ')[0]}</span>
		{/if}
	</svelte:fragment>

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
					class:variant-filled-secondary={state}
					class:variant-ghost-secondary={!state}
					class:rounded-none={i > 0 && i < parts.length - 1}
					class:rounded-r-none={i == 0}
					class:rounded-l-none={i == parts.length - 1}
					class="btn hover:variant-filled-secondary"
					><span><Ico {icon} size="nav" /></span>{#if name}<span>{name}</span>{/if}</a
				>
			{/each}
		</div>
	{/if}
	<svelte:fragment slot="trail">
		<LightSwitch />
	</svelte:fragment>
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
		@apply items-center p-1 pl-5 pr-5 hidden md:inline-flex rounded-token;
	}

	.icon-size {
		@apply w-7 h-7;
	}
	.light-switch {
		@apply mr-5;
	}
</style>
