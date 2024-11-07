<script>
	import { projectsCheck, projectsStore } from '$lib/admin/stores/projectsStore';
	import { onMount } from 'svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import CardD from '$lib/dashboard/components/Card-D.svelte';
	import { flip } from 'svelte/animate';
	import { fade } from 'svelte/transition';
	import { replaceState } from '$app/navigation';

	onMount(() => {
		projectsCheck();
	});

	let searchQuery = $state('');
	let rootProject = $state();

	let filters = $state([
		{ icon: 'ph:video-thin', count: 0, filter: (project) => project['@_hasFrontend'] == 'true' },
		{
			icon: 'ph:books-thin',
			count: 0,
			filter: (project) => project['@_name'].startsWith('lib')
		}
	]);

	function handleHashChange() {
		const hash = window.location.hash?.substring(1);
		rootProject = hash ? $projectsStore.find((project) => project['@_name'] == hash) : null;
		if (rootProject == null) {
			if (window.location.href.endsWith('#')) {
				replaceState('', '');
			}
		}
	}

	onMount(() => {
		window.addEventListener('hashchange', handleHashChange);
		handleHashChange();
		return () => {
			window.removeEventListener('hashchange', handleHashChange);
		};
	});

	let filteredProjects = $derived(
		$projectsStore.filter((project) => {
			if (rootProject) {
				return (
					project['@_name'] == rootProject['@_name'] || rootProject.ref?.includes(project['@_name'])
				);
			}
			let ok = project['@_name'].toLowerCase().includes(searchQuery.toLowerCase());
			if (ok) {
				for (let { count, filter } of filters) {
					if ((count == 1 && !filter(project)) || (count == 2 && filter(project))) {
						return false;
					}
				}
			}
			return ok;
		})
	);
</script>

<CardD class="gap-5">
	<div class="input-group input-group-divider grid-cols-[auto_1fr_auto]">
		<div class="input-group-shim"><Ico icon="mdi:magnify" /></div>
		<input
			type="search"
			placeholder="Search projects..."
			bind:value={searchQuery}
			disabled={rootProject}
		/>
		<span class="flex flex-col">
			{#each filters as { icon, count }, i}
				<span class="flex">
					<button
						class="btn rounded-none"
						style="padding: 2px"
						class:preset-ghost-secondary={count != 1}
						class:preset-filled-secondary={count == 1}
						onclick={() => {
							filters[i].count = count == 1 ? 0 : 1;
						}}
					>
						<Ico {icon} size="nav" />
					</button>
					<button
						class="btn rounded-none"
						style="padding: 2px"
						class:preset-ghost-warning={count != 2}
						class:preset-filled-warning={count == 2}
						onclick={() => {
							filters[i].count = count == 2 ? 0 : 2;
						}}
					>
						<Ico {icon} size="nav" />
					</button>
				</span>
			{/each}
		</span>
	</div>
	<div class="grid gap-5 grid-cols-1 sm:grid-cols-2 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
		{#each filteredProjects as project (project['@_name'])}
			<div class="flex flex-col" animate:flip={{ duration: 500 }} transition:fade>
				<div
					class="flex gap-3 justify-between items-center p-2 border-[1px] rounded-t-md dark:border-surface-700 border-surface-200"
				>
					<span class="text-md font-semibold truncate">{project['@_name']}</span>
				</div>
				<div
					class="border-[1px] border-t-0 rounded-b-md dark:border-surface-700 border-surface-200 dark:opacity-70"
				>
					<div class="relative img-hover-zoom img-hover-zoom--quick-zoom flex justify-center">
						<img
							src="https://www.impactplus.com/hubfs/Fensea.jpg"
							class="object-cover"
							alt="project"
						/>
						<div class="absolute top-0 w-full flex">
							<div class="grow flex">
								<a
									href="{project['@_name']}/backend/"
									class="p-3 preset-ghost-secondary hover:preset-filled-secondary h-fit rounded-br-lg"
								>
									<Ico icon="ph:gear-six-thin" size="nav" />
								</a>
							</div>
							{#if project['@_hasFrontend'] == 'true'}
								<div class="grow flex justify-end">
									<a
										href="{project['@_name']}/frontend/"
										class="p-3 preset-ghost-secondary hover:preset-filled-secondary h-fit rounded-bl-lg"
									>
										<Ico icon="ph:video-thin" size="nav" />
									</a>
								</div>
							{/if}
						</div>
						<div class="absolute inset-x-0 bottom-0 flex">
							{#if project.ref?.length > 0}
								<div class="grow flex">
									<a
										href={rootProject != project ? `#${project['@_name']}` : '#'}
										class="p-3 hover:preset-filled-secondary h-fit rounded-tr-lg"
										class:preset-ghost-secondary={rootProject != project}
										class:preset-filled-secondary={rootProject == project}
									>
										<Ico icon="ph:plugs-connected-thin" size="nav" />
									</a>
								</div>
							{/if}
							{#if project['@_hasPlatform'] == 'true'}
								<div class="grow flex justify-end">
									<a
										href="{project['@_name']}/platforms/"
										class="p-3 preset-ghost-secondary hover:preset-filled-secondary h-fit rounded-tl-lg"
									>
										<Ico icon="ph:package-thin" size="nav" />
									</a>
								</div>
							{/if}
						</div>
					</div>
					<div
						class="px-2 truncate cursor-help"
						onclick={(e) => {
							e?.target?.['classList']?.toggle('truncate');
						}}
					>
						{project['@_comment']}
					</div>
				</div>
			</div>
		{/each}
	</div>
</CardD>

<style lang="postcss">
	.search-bar {
		@apply placeholder:text-[16px] placeholder:dark:text-surface-500 placeholder:text-surface-200 text placeholder:font-light font-normal border-none dark:bg-surface-800 rounded;
		border-bottom: surface-200;
	}

	.img-hover-zoom {
		height: 200px;
		overflow: hidden;
		transition:
			margin 0.5s,
			height 0.5s;
	}

	.img-hover-zoom:hover {
		margin: 2px;
		height: 196px;
	}

	.img-hover-zoom--quick-zoom img {
		transform-origin: 50% 50%;
		transition: transform 0.5s;
	}

	.img-hover-zoom--quick-zoom:hover img {
		transform: scale(1.1);
	}
</style>
