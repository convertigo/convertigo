<script>
	import { SegmentedControl } from '@skeletonlabs/skeleton-svelte';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import InputGroup from '$lib/common/components/InputGroup.svelte';
	import Projects from '$lib/common/Projects.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { getFrontendUrl, getThumbnailUrl } from '$lib/utils/service';
	import { onDestroy } from 'svelte';
	import { flip } from 'svelte/animate';
	import { fade } from 'svelte/transition';

	let { projects } = $derived(Projects);
	let searchQuery = $state('');
	let rootProject = $derived(projects.find(({ name }) => name == page.params.project));

	let filters = $state([
		{ icon: 'mdi:smartphone-link', count: 0, filter: ({ hasFrontend }) => hasFrontend == 'true' },
		{
			icon: 'mdi:book-open-variant',
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

	const primaryShades = [100, 200, 300, 400, 500, 600, 700, 800, 900];
	function getPrimaryShade(str) {
		let h = 2166136261; // FNV-1a 32bit offset basis
		for (let i = 0; i < str.length; i++) {
			h ^= str.charCodeAt(i);
			h += (h << 1) + (h << 4) + (h << 7) + (h << 8) + (h << 24);
		}
		return primaryShades[(h >>> 0) % primaryShades.length];
	}

	onDestroy(Projects.stop);
</script>

<div class="layout-y-stretch">
	<div class="flex flex-wrap items-center gap-2">
		<InputGroup
			id="search"
			type="search"
			placeholder="Search projects..."
			class="min-w-[240px] grow !border-surface-200-800 !bg-surface-100-900 md:flex-1"
			inputClass="text-strong placeholder:text-surface-600-400"
			iconClass="text-surface-700-300"
			icon="mdi:magnify"
			disabled={rootProject}
			autofocus
			bind:value={searchQuery}
		/>
		<div class="flex flex-wrap items-center gap-2 md:ml-auto">
			<SegmentedControl
				value={`${filters[0].count}`}
				onValueChange={(event) => {
					filters[0].count = Number(event.value);
				}}
				class="w-fit"
			>
				<SegmentedControl.Control
					class="relative flex items-center gap-0.5 rounded-base border border-surface-200-800 bg-surface-100-900 p-0.5 shadow-none"
				>
					<SegmentedControl.Indicator
						class="rounded-[0.3rem] bg-primary-500 opacity-100 shadow-none"
					/>
					{#each [2, 0, 1] as value (value)}
						<SegmentedControl.Item value={`${value}`} class="relative">
							<SegmentedControl.ItemText
								class={[
									filters[0].count == value ? 'text-primary-contrast-500' : 'text-surface-950-50',
									'px-2 py-1 text-[13px] font-medium'
								]}
							>
								{#if value == 2}
									<span class="layout-x-none gap-1">
										<Ico icon="mdi:cog" size="nav" />
										Backend
									</span>
								{:else if value == 0}
									<span class="layout-x-none gap-1">
										<span aria-hidden="true" class="text-base leading-none font-semibold"
											>&harr;</span
										>
										<span class="sr-only">Both</span>
									</span>
								{:else}
									<span class="layout-x-none gap-1">
										Frontend
										<Ico icon="mdi:smartphone-link" size="nav" />
									</span>
								{/if}
							</SegmentedControl.ItemText>
							<SegmentedControl.ItemHiddenInput />
						</SegmentedControl.Item>
					{/each}
				</SegmentedControl.Control>
			</SegmentedControl>
			<SegmentedControl
				value={`${filters[1].count}`}
				onValueChange={(event) => {
					filters[1].count = Number(event.value);
				}}
				class="w-fit"
			>
				<SegmentedControl.Control
					class="relative flex items-center gap-0.5 rounded-base border border-surface-200-800 bg-surface-100-900 p-0.5 shadow-none"
				>
					<SegmentedControl.Indicator
						class="rounded-[0.3rem] bg-primary-500 opacity-100 shadow-none"
					/>
					{#each [1, 0, 2] as value (value)}
						<SegmentedControl.Item value={`${value}`} class="relative">
							<SegmentedControl.ItemText
								class={[
									filters[1].count == value ? 'text-primary-contrast-500' : 'text-surface-950-50',
									'px-2 py-1 text-[13px] font-medium'
								]}
							>
								{#if value == 1}
									<span class="layout-x-none gap-1">
										<Ico icon="mdi:book-open-variant" size="nav" />
										Library
									</span>
								{:else if value == 0}
									<span class="layout-x-none gap-1">
										<span aria-hidden="true" class="text-base leading-none font-semibold"
											>&harr;</span
										>
										<span class="sr-only">Both</span>
									</span>
								{:else}
									<span class="layout-x-none gap-1">
										Project
										<Ico icon="mdi:folder-outline" size="nav" />
									</span>
								{/if}
							</SegmentedControl.ItemText>
							<SegmentedControl.ItemHiddenInput />
						</SegmentedControl.Item>
					{/each}
				</SegmentedControl.Control>
			</SegmentedControl>
		</div>
	</div>
	<div class="grid grid-cols-1 gap sm:grid-cols-2 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
		{#each filteredProjects as project, i (project.name ?? i)}
			{@const { name, version, comment, hasFrontend, hasPlatform, ref } = project}
			{@const loading = name == null}
			{@const params = { project: name ? name : '_' }}
			{@const hasFrontendProject = hasFrontend == 'true'}
			{@const cardHref = hasFrontendProject
				? getFrontendUrl(name)
				: resolve('/(app)/dashboard/[[project]]/backend', params)}
			{@const cardShade = getPrimaryShade(name ?? `_${i}`)}
			{@const cardShadeInverse = 1000 - cardShade}
			<div
				class="layout-y-stretch-none rounded-container preset-filled-surface-100-900 p-low shadow-follow"
				animate:flip={loading ? undefined : { duration: 400 }}
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
					{#if name}
						<a
							href={cardHref}
							target={hasFrontendProject ? '_blank' : undefined}
							rel={hasFrontendProject ? 'noopener noreferrer' : undefined}
							class="block"
							title={hasFrontendProject ? 'Open frontend in new tab' : 'Open backend'}
							aria-label={hasFrontendProject
								? `Open ${name} frontend in new tab`
								: `Open ${name} backend`}
						>
							<div
								class="img-hover-zoom project-media layout-x-none justify-center rounded-lg dark:opacity-70"
								style="--card-tint: var(--color-primary-{cardShade}); --card-tint-contrast: var(--color-primary-{cardShadeInverse});"
							>
								<Ico icon="convertigo:logo" class="max-h-full object-cover" size={128} />
								<img
									src={getThumbnailUrl(name)}
									onload={(/** @type {any} */ e) => {
										if (e.target.naturalWidth <= 1 && e.target.naturalHeight <= 1) {
											e.target.remove();
										} else {
											e.target.previousElementSibling.remove();
										}
									}}
									class="object-cover"
									alt="project"
								/>
							</div>
						</a>
					{:else}
						<div
							class="img-hover-zoom project-media layout-x-none justify-center rounded-lg dark:opacity-70"
							style="--card-tint: var(--color-primary-{cardShade}); --card-tint-contrast: var(--color-primary-{cardShadeInverse});"
						>
							<Ico icon="convertigo:logo" class="max-h-full object-cover" size={128} />
							<img
								src={getThumbnailUrl(name)}
								onload={(/** @type {any} */ e) => {
									if (e.target.naturalWidth <= 1 && e.target.naturalHeight <= 1) {
										e.target.remove();
									} else {
										e.target.previousElementSibling.remove();
									}
								}}
								class="object-cover"
								alt="project"
							/>
						</div>
					{/if}
					<div class="absolute top-0 layout-y-start-none overflow-clip rounded-tl-lg rounded-br-lg">
						<a href={resolve('/(app)/dashboard/[[project]]/backend', params)} class="icon-link">
							<Ico icon="mdi:cog" size="nav" />
							<span class="icon-link-text">Backend</span>
						</a>
						{#if hasFrontend == 'true'}
							<a href={resolve('/(app)/dashboard/[[project]]/frontend', params)} class="icon-link">
								<Ico icon="mdi:smartphone-link" size="nav" />
								<span class="icon-link-text">Frontend</span>
							</a>
						{/if}

						{#if ref?.length > 0}
							<a
								href={rootProject == project
									? resolve('/(app)/dashboard')
									: resolve('/(app)/dashboard/[[project]]', params)}
								class="icon-link"
							>
								<Ico icon="mdi:power-plug" size="nav" />
								<span class="icon-link-text">References</span>
							</a>
						{/if}
						{#if hasPlatform == 'true'}
							<a href={resolve('/(app)/dashboard/[[project]]/platforms', params)} class="icon-link">
								<Ico icon="mdi:package-variant-closed" size="nav" />
								<span class="icon-link-text">Platforms</span>
							</a>
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
</div>

<style>
	.img-hover-zoom {
		height: 200px;
		overflow: hidden;
		transition:
			margin 0.5s,
			height 0.5s;
	}

	.project-media {
		background-color: color-mix(in oklab, var(--card-tint) 10%, var(--color-surface-100) 90%);
		color: color-mix(in oklab, var(--card-tint-contrast) 70%, var(--color-surface-900) 30%);
	}

	:global(.dark) .project-media {
		background-color: color-mix(in oklab, var(--card-tint) 18%, var(--color-surface-900) 82%);
		color: color-mix(in oklab, var(--card-tint-contrast) 60%, var(--color-surface-50) 40%);
	}

	.img-hover-zoom img {
		transform-origin: 50% 50%;
		transition: transform 0.5s;
	}

	.img-hover-zoom:hover img {
		transform: scale(1.1);
	}

	:global(.img-hover-zoom svg) {
		transform-origin: 50% 50%;
		transition: transform 0.5s;
	}

	:global(.img-hover-zoom:hover svg) {
		transform: scale(1.1);
	}

	.icon-link {
		display: flex;
		align-items: center;
		padding: 0.5rem;
		background-color: var(--convertigo-text);
		color: #fff;
	}

	.icon-link-text {
		max-width: 0;
		overflow: hidden;
		white-space: nowrap;
		transition:
			max-width 0.3s cubic-bezier(0.4, 0, 0.2, 1),
			margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
	}

	.icon-link:hover .icon-link-text {
		max-width: 200px;
		margin-left: 5px;
	}
</style>
