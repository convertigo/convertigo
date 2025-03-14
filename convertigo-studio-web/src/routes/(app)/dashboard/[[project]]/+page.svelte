<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { flip } from 'svelte/animate';
	import { fade } from 'svelte/transition';
	import Projects from '$lib/common/Projects.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import { page } from '$app/state';
	import { resolveRoute } from '$app/paths';
	import { onDestroy } from 'svelte';

	let { projects } = $derived(Projects);
	let searchQuery = $state('');
	let rootProject = $derived(projects.find(({ name }) => name == page.params.project));

	let filters = $state([
		{ icon: 'ph:video-thin', count: 0, filter: ({ hasFrontend }) => hasFrontend == 'true' },
		{
			icon: 'ph:books-thin',
			count: 0,
			filter: ({ name }) => name.startsWith('lib')
		}
	]);

	let filteredProjects = $derived(
		projects.filter((project) => {
			if (rootProject) {
				return project.name == rootProject.name || rootProject.ref?.includes(project.name);
			}
			let ok = project.name?.toLowerCase().includes(searchQuery.toLowerCase()) ?? true;
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

	onDestroy(Projects.stop);
</script>

<Card>
	<div
		class="input-group w-full grid-cols-[auto_1fr_auto] divide-x divide-surface-700-300 preset-outlined-surface-700-300 bg-surface-200-800"
	>
		<div class="input-group-cell"><Ico icon="mdi:magnify" /></div>
		<input
			type="search"
			placeholder="Search projects..."
			bind:value={searchQuery}
			disabled={rootProject}
		/>
		<span class="layout-y-none gap-[1px]!">
			{#each filters as { icon, count }, i}
				<span class="layout-x-none gap-[1px]!">
					<button
						class="btn rounded-none preset-filled p-1"
						class:!bg-success-100={count != 1}
						class:!bg-success-600={count == 1}
						onclick={() => {
							filters[i].count = count == 1 ? 0 : 1;
						}}
					>
						<Ico {icon} size="nav" />
					</button>
					<button
						class="btn rounded-none preset-filled p-1"
						class:!bg-warning-100={count != 2}
						class:!bg-warning-600={count == 2}
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
	<div class="grid grid-cols-1 gap sm:grid-cols-2 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
		{#each filteredProjects as project, i (project.name ?? i)}
			{@const { name, version, comment, hasFrontend, hasPlatform, ref } = project}
			{@const loading = name == null}
			{@const params = { project: name ? name : '_' }}
			<div
				class="layout-y-stretch-none rounded-sm preset-outlined-surface-700-300 bg-surface-200-800"
				animate:flip={{ duration: 500 }}
				transition:fade
			>
				<div class="layout-x-p-low justify-between! py-1!">
					<span class="text-md truncate font-semibold"
						><AutoPlaceholder {loading}>{name}</AutoPlaceholder></span
					>
					<span class="truncate text-sm opacity-50"
						><AutoPlaceholder {loading}>{version}</AutoPlaceholder></span
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
					<div class="absolute top-0 flex w-full">
						<div class="flex grow">
							<a
								href={resolveRoute('/(app)/dashboard/[[project]]/backend', params)}
								class="h-fit rounded-br-lg preset-filled-warning-300-700 p-3 hover:preset-filled-warning-500"
							>
								<Ico icon="ph:gear-six-thin" size="nav" />
							</a>
						</div>
						{#if hasFrontend == 'true'}
							<div class="flex grow justify-end">
								<a
									href={resolveRoute('/(app)/dashboard/[[project]]/frontend', params)}
									class="h-fit rounded-bl-lg preset-filled-success-300-700 p-3 hover:preset-filled-success-500"
								>
									<Ico icon="ph:video-thin" size="nav" />
								</a>
							</div>
						{/if}
					</div>
					<div class="absolute inset-x-0 bottom-0 flex">
						{#if ref?.length > 0}
							<div class="flex grow">
								<a
									href={resolveRoute(
										`/(app)/dashboard/${rootProject == project ? '' : '[[project]]'}`,
										params
									)}
									class="h-fit rounded-tr-lg p-3 hover:preset-filled-secondary-500"
									class:preset-filled-secondary-300-700={rootProject != project}
									class:preset-filled-secondary-500={rootProject == project}
								>
									<Ico icon="ph:plugs-connected-thin" size="nav" />
								</a>
							</div>
						{/if}
						{#if hasPlatform == 'true'}
							<div class="flex grow justify-end">
								<a
									href={resolveRoute('/(app)/dashboard/[[project]]/platforms', params)}
									class="h-fit rounded-tl-lg preset-filled-primary-300-700 p-3 hover:preset-filled-primary-500"
								>
									<Ico icon="ph:package-thin" size="nav" />
								</a>
							</div>
						{/if}
					</div>
				</div>
				<button
					class="cursor-help truncate px-2 text-start opacity-70"
					onclick={(e) => {
						e?.target?.['classList']?.toggle('truncate');
						e?.target?.['classList']?.toggle('opacity-70');
					}}
				>
					<AutoPlaceholder {loading}>{comment}</AutoPlaceholder>
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
