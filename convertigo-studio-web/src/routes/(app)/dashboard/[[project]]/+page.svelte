<script>
	import { assets, resolveRoute } from '$app/paths';
	import { page } from '$app/state';
	import Card from '$lib/admin/components/Card.svelte';
	import LightSvelte from '$lib/common/Light.svelte';
	import Projects from '$lib/common/Projects.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { getThumbnailUrl, getUrl } from '$lib/utils/service';
	import { onDestroy } from 'svelte';
	import { flip } from 'svelte/animate';
	import { fade } from 'svelte/transition';

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

	function stringToColor(str, isDark) {
		let h = 2166136261; // FNV-1a 32bit offset basis
		for (let i = 0; i < str.length; i++) {
			h ^= str.charCodeAt(i);
			h += (h << 1) + (h << 4) + (h << 7) + (h << 8) + (h << 24);
		}
		let hue = (h >>> 0) % 360;
		let sat = 40 + ((h >> 10) % 30);

		return hslToHex(hue, sat, isDark ? 30 : 70);
	}

	function hslToHex(h, s, l) {
		s /= 100;
		l /= 100;

		const k = (n) => (n + h / 30) % 12;
		const a = s * Math.min(l, 1 - l);
		const f = (n) => l - a * Math.max(-1, Math.min(k(n) - 3, Math.min(9 - k(n), 1)));

		const toHex = (x) => {
			const hex = Math.round(255 * x).toString(16);
			return hex.length === 1 ? '0' + hex : hex;
		};

		return `#${toHex(f(0))}${toHex(f(8))}${toHex(f(4))}`;
	}

	onDestroy(Projects.stop);
</script>

<Card>
	<div
		class="input-group w-full grid-cols-[auto_1fr_auto] divide-x divide-surface-700-300 preset-outlined-surface-700-300 bg-surface-200-800"
	>
		<label for="search" class="ig-cell"><Ico icon="mdi:magnify" /></label>
		<input
			id="search"
			type="search"
			class="ig-input placeholder-surface-500"
			placeholder="Search projects..."
			bind:value={searchQuery}
			disabled={rootProject}
		/>
		<span class="layout-y-none gap-[1px]!">
			{#each filters as { icon, count }, i}
				<span class="layout-x-none gap-[1px]!">
					<button
						class="btn rounded-none p-1"
						class:preset-filled-success-100-900={count != 1}
						class:preset-filled-success-600-400={count == 1}
						onclick={() => {
							filters[i].count = count == 1 ? 0 : 1;
						}}
					>
						<Ico {icon} size="nav" />
					</button>
					<button
						class="btn rounded-none p-1"
						class:preset-filled-warning-100-900={count != 2}
						class:preset-filled-warning-600-400={count == 2}
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
					<div
						class="img-hover-zoom flex justify-center dark:opacity-70"
						style="background-color: {stringToColor(
							name ?? `_${i}`,
							LightSvelte.dark
						)}; color: {stringToColor(name ?? `_${i}`, !LightSvelte.dark)}"
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
					<div class="absolute top-0 flex w-full">
						<div class="flex grow">
							<a
								href={resolveRoute('/(app)/dashboard/[[project]]/backend', params)}
								class="h-fit rounded-br-lg preset-filled-warning-100-900 p-3 hover:preset-filled-warning-300-700"
							>
								<Ico icon="ph:gear-six-thin" size="nav" />
							</a>
						</div>
						{#if hasFrontend == 'true'}
							<div class="flex grow justify-end">
								<a
									href={resolveRoute('/(app)/dashboard/[[project]]/frontend', params)}
									class="h-fit rounded-bl-lg preset-filled-success-100-900 p-3 hover:preset-filled-success-300-700"
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
									class="h-fit rounded-tr-lg p-3 hover:preset-filled-secondary-300-700"
									class:preset-filled-secondary-100-900={rootProject != project}
									class:preset-filled-secondary-400-600={rootProject == project}
								>
									<Ico icon="ph:plugs-connected-thin" size="nav" />
								</a>
							</div>
						{/if}
						{#if hasPlatform == 'true'}
							<div class="flex grow justify-end">
								<a
									href={resolveRoute('/(app)/dashboard/[[project]]/platforms', params)}
									class="h-fit rounded-tl-lg preset-filled-primary-100-900 p-3 hover:preset-filled-primary-300-700"
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

<style>
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

	:global(.img-hover-zoom svg) {
		transform-origin: 50% 50%;
		transition: transform 0.5s;
	}

	:global(.img-hover-zoom:hover svg) {
		transform: scale(1.1);
	}
</style>
