<script>
	import { projectsCheck, projectsStore } from '$lib/admin/stores/projectsStore';
	import { onMount } from 'svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import CardD from '$lib/dashboard/components/Card-D.svelte';
	import { flip } from 'svelte/animate';
	import { fade } from 'svelte/transition';

	onMount(() => {
		projectsCheck();
	});

	let searchQuery = '';

	$: filteredProjects = $projectsStore.filter((project) =>
		project['@_name'].toLowerCase().includes(searchQuery.toLowerCase())
	);
</script>

<CardD class="gap-5">
	<div class="input-group input-group-divider grid-cols-[auto_1fr_auto]">
		<div class="input-group-shim"><Ico icon="mdi:magnify" /></div>
		<input type="search" placeholder="Search projects..." bind:value={searchQuery} />
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
					<div class="relative img-hover-zoom img-hover-zoom--quick-zoom">
						<img
							src="https://www.impactplus.com/hubfs/Fensea.jpg"
							class="object-cover"
							alt="project"
						/>
						<div class="absolute top-0 h-full w-full flex">
							<div class="grow flex">
								<a
									href="{project['@_name']}/backend/"
									class="p-3 variant-ghost-secondary hover:variant-filled-secondary h-fit rounded-br-lg"
								>
									<Ico icon="ph:gear-six-thin" size="nav" />
								</a>
							</div>
							<div class="grow flex justify-end">
								<a
									href="{project['@_name']}/frontend/"
									class="p-3 variant-ghost-secondary hover:variant-filled-secondary h-fit rounded-bl-lg"
								>
									<Ico icon="ph:video-thin" size="nav" />
								</a>
							</div>
						</div>
					</div>
					<div
						class="px-2 truncate cursor-help"
						on:click={(e) => {
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
		@apply placeholder:text-[16px] placeholder:dark:text-surface-500 placeholder:text-surface-200 text-token placeholder:font-light font-normal border-none dark:bg-surface-800 rounded-token;
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
