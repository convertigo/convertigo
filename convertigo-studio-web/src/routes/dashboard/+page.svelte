<script>
	import { projectsCheck, projectsStore } from '$lib/admin/stores/projectsStore';
	import Icon from '@iconify/svelte';
	import { onMount } from 'svelte';
	import { writable } from 'svelte/store';
	import { Accordion, AccordionItem, getModalStore, popup } from '@skeletonlabs/skeleton';
	import Ico from '$lib/utils/Ico.svelte';
	import { call } from '$lib/utils/service';
	import { goto } from '$app/navigation';
	import CardD from '$lib/dashboard/components/Card-D.svelte';

	onMount(() => {
		projectsCheck();
	});

	const modalStore = getModalStore();

	// Create a writable store for the search query
	const searchQuery = writable('');
	const selectedProject = writable('');

	const settingsPopup = [
		{ icon: 'mdi:edit-outline', title: 'Rename Project' },
		{ icon: 'fluent:open-24-filled', title: 'Open In New Tab' },
		{ icon: 'fad:duplicate', title: 'Duplicate Project' },
		{ icon: 'grommet-icons:add', title: 'Add Tag' },
		{ icon: 'mingcute:delete-line', title: 'Delete Project' }
	];

	const popupClick = {
		event: 'click',
		target: 'popupClick',
		placement: 'bottom'
	};

	function openModalSettings(mode, project) {
		modalStore.trigger({
			type: 'component',
			component: 'modalSettingsProject',
			meta: { mode, project }
		});
	}

	async function handleProjectClick(projectName) {
		const payload = { projectName };
		await call('projects.GetTestPlatform', payload);
		goto(`/dashboard/test-platform/${projectName}`);
	}

	$: filteredProjects = $projectsStore.filter((project) =>
		project['@_name'].toLowerCase().includes($searchQuery.toLowerCase())
	);

	$: finalFilteredProjects = filteredProjects.filter((project) =>
		$selectedProject ? project['@_name'] === $selectedProject : true
	);
</script>

<CardD>
	<div class="relative flex gap-10">
		<input
			type="text"
			placeholder="Search projects..."
			class="w-72 p-2 pl-10 border rounded-md input-common-dash"
			bind:value={$searchQuery}
		/>
		<Icon
			icon="mdi:magnify"
			class="absolute top-1/2 left-3 transform -translate-y-1/2 text-gray-500 w-5 h-5"
		/>

		<select class="w-72 p-2 border rounded-md input-common-dash w-72" bind:value={$selectedProject}>
			<option value="">All Projects</option>
			{#each filteredProjects as project}
				<option value={project['@_name']}>{project['@_name']}</option>
			{/each}
		</select>
	</div>
</CardD>

<CardD>
	<div class="grid gap-5 grid-cols-1 sm:grid-cols-2 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
		{#each finalFilteredProjects as project}
			<!-- svelte-ignore a11y-no-static-element-interactions -->
			<!-- svelte-ignore a11y-click-events-have-key-events -->
			<div class="flex flex-col">
				<div
					class="flex justify-between items-center p-2 border-[1px] rounded-t-md dark:border-surface-700 border-surface-200"
				>
					<span class="text-md font-semibold">{project['@_name']}</span>
					<div class="flex space-x-2">
						<button use:popup={popupClick}>
							<Icon icon="iconamoon:menu-kebab-vertical-bold" />
						</button>
					</div>
					<div class="px-2 py-3 z-10 bg-surface-600 rounded-token gap-2" data-popup="popupClick">
						{#each settingsPopup as setting}
							<button
								on:click={() => {
									console.log(setting.title);
									if (setting.title === 'Delete Project') {
										console.log('Project name:', project['@_name']);
									} else {
										openModalSettings(setting.title);
									}
								}}
								class="flex text-light text-[12px] justify-start w-full px-3 hover:bg-surface-400"
							>
								<Ico icon={setting.icon} />
								<p>{setting.title}</p>
							</button>
						{/each}
						<div class="arrow bg-surface-600" />
					</div>
				</div>
				<div on:click={() => handleProjectClick(project['@_name'])}>
					<img
						src="https://www.impactplus.com/hubfs/Fensea.jpg"
						class="object-cover border-[1px] border-t-0 rounded-b-md dark:border-surface-700 border-surface-200 dark:opacity-70"
						alt="project"
					/>
				</div>
				<Accordion>
					<AccordionItem close>
						<svelte:fragment slot="summary"
							>{project['@_comment'].match(/(\w+\s*){1,5}/)[0] + '...'}</svelte:fragment
						>
						<svelte:fragment slot="content">{project['@_comment']}</svelte:fragment>
					</AccordionItem>
				</Accordion>
			</div>
		{/each}
	</div>
</CardD>

<style lang="postcss">
	.search-bar {
		@apply placeholder:text-[16px] placeholder:dark:text-surface-500 placeholder:text-surface-200 text-token placeholder:font-light font-normal border-none dark:bg-surface-800 rounded-token;
		border-bottom: surface-200;
	}
</style>
