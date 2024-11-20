<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { flip } from 'svelte/animate';
	import { fade } from 'svelte/transition';
	import Projects from '$lib/common/Projects.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import { page } from '$app/stores';

	let searchQuery = $state('');
	let rootProject = $derived(
		Projects.projects.find((project) => project['@_name'] == $page.params.project)
	);
	let prefix = $derived(rootProject ? '../' : '');

	let filters = $state([
		{ icon: 'ph:video-thin', count: 0, filter: (project) => project['@_hasFrontend'] == 'true' },
		{
			icon: 'ph:books-thin',
			count: 0,
			filter: (project) => project['@_name'].startsWith('lib')
		}
	]);

	let filteredProjects = $derived(
		Projects.projects.filter((project) => {
			if (rootProject) {
				return (
					project['@_name'] == rootProject['@_name'] || rootProject.ref?.includes(project['@_name'])
				);
			}
			let ok = project['@_name']?.toLowerCase().includes(searchQuery.toLowerCase()) ?? true;
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

<Card>
	<div
		class="w-full input-group bg-surface-200-800 divide-surface-700-300 preset-outlined-surface-700-300 divide-x grid-cols-[auto_1fr_auto]"
	>
		<div class="input-group-cell"><Ico icon="mdi:magnify" /></div>
		<input
			type="search"
			placeholder="Search projects..."
			bind:value={searchQuery}
			disabled={rootProject}
		/>
		<span class="layout-y-none !gap-[1px]">
			{#each filters as { icon, count }, i}
				<span class="layout-x-none !gap-[1px]">
					<button
						class="btn rounded-none preset-filled p-1"
						class:!bg-secondary-100={count != 1}
						class:!bg-secondary-500={count == 1}
						onclick={() => {
							filters[i].count = count == 1 ? 0 : 1;
						}}
					>
						<Ico {icon} size="nav" />
					</button>
					<button
						class="btn rounded-none preset-filled p-1"
						class:!bg-warning-100={count != 2}
						class:!bg-warning-500={count == 2}
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
	<div class="grid gap grid-cols-1 sm:grid-cols-2 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
		{#each filteredProjects as project, i (project['@_name'] ?? i)}
			{@const name = project['@_name']}
			{@const loading = name == null}
			<div
				class="layout-y-none !items-stretch bg-surface-200-800 preset-outlined-surface-700-300 rounded"
				animate:flip={{ duration: 500 }}
				transition:fade
			>
				<div class="layout-x-p-low !py-1 !justify-between">
					<span class="text-md font-semibold truncate"
						><AutoPlaceholder {loading}>{name}</AutoPlaceholder></span
					>
					<span class="text-sm truncate opacity-50"
						><AutoPlaceholder {loading}>{project['@_version']}</AutoPlaceholder></span
					>
				</div>
				<div class="relative">
					<div class="img-hover-zoom flex justify-center dark:opacity-70">
						<img
							src="https://www.impactplus.com/hubfs/Fensea.jpg"
							class="object-cover"
							alt="project"
						/>
					</div>
					<div class="absolute top-0 w-full flex">
						<div class="grow flex">
							<a
								href="{prefix}{name}/backend/"
								class="p-3 bg-secondary-300 hover:bg-secondary-500 h-fit rounded-br-lg"
							>
								<Ico icon="ph:gear-six-thin" size="nav" />
							</a>
						</div>
						{#if project['@_hasFrontend'] == 'true'}
							<div class="grow flex justify-end">
								<a
									href="{prefix}{name}/frontend/"
									class="p-3 bg-secondary-300 hover:bg-secondary-500 h-fit rounded-bl-lg"
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
									href="{prefix}{rootProject == project ? '' : `${name}/`}"
									class="p-3 hover:bg-secondary-500 h-fit rounded-tr-lg"
									class:bg-secondary-300={rootProject != project}
									class:bg-secondary-500={rootProject == project}
								>
									<Ico icon="ph:plugs-connected-thin" size="nav" />
								</a>
							</div>
						{/if}
						{#if project['@_hasPlatform'] == 'true'}
							<div class="grow flex justify-end">
								<a
									href="{prefix}{name}/platforms/"
									class="p-3 bg-secondary-300 hover:bg-secondary-500 h-fit rounded-tl-lg"
								>
									<Ico icon="ph:package-thin" size="nav" />
								</a>
							</div>
						{/if}
					</div>
				</div>
				<button
					class="px-2 truncate cursor-help text-start opacity-70"
					onclick={(e) => {
						e?.target?.['classList']?.toggle('truncate');
						e?.target?.['classList']?.toggle('opacity-70');
					}}
				>
					<AutoPlaceholder loading={project['@_comment'] == null}
						>{project['@_comment']}</AutoPlaceholder
					>
				</button>
			</div>
		{/each}
	</div>
</Card>

<style lang="postcss">
	.img-hover-zoom {
		height: 200px;
		overflow: hidden;
		transition:
			margin 0.5s,
			height 0.5s;
	}

	.img-hover-zoom img {
		transform-origin: 50% 50%;
		transition: transform 0.5s;
	}

	.img-hover-zoom:hover img {
		transform: scale(1.1);
	}
</style>
