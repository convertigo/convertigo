<script>
	import { projectsCheck, projectsStore } from '$lib/admin/stores/projectsStore';
	import Icon from '@iconify/svelte';
	import { onMount } from 'svelte';
	import { writable } from 'svelte/store';
	import { getModalStore, popup } from '@skeletonlabs/skeleton';
	import Ico from '$lib/utils/Ico.svelte';
	import { call } from '$lib/utils/service';

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
			meta: { mode, project },
			response: (confirmed) => {
				if (confirmed && mode == 'Delete Project') {
					deleteProject(project);
				}
			}
		});
	}

	// Derived store to filter projects based on search query
	$: filteredProjects = $projectsStore.filter((project) =>
		project['@_name'].toLowerCase().includes($searchQuery.toLowerCase())
	);

	$: finalFilteredProjects = filteredProjects.filter((project) =>
		$selectedProject ? project['@_name'] === $selectedProject : true
	);

	async function deleteProject(projectName) {
		modalStore.trigger({
			type: 'confirm',
			title: 'Please Confirm',
			body: `Are you sure you want to delete this Project ?`,
			response: async (confirmed) => {
				if (confirmed) {
					try {
						const response = await call('projects.Delete', { projectName });
						console.log('deleted project', response);
						projectsCheck();
					} catch (err) {
						console.error('Error deleting project:', err);
					}
				}
			}
		});
	}
</script>

<div class="bg-surface-700 p-4 rounded-token">
	<div class="relative flex gap-10">
		<input
			type="text"
			placeholder="Search projects..."
			class="w-72 p-2 pl-10 border rounded-md input-common"
			bind:value={$searchQuery}
		/>
		<Icon
			icon="mdi:magnify"
			class="absolute top-1/2 left-3 transform -translate-y-1/2 text-gray-500 w-5 h-5"
		/>

		<select class="w-72 p-2 border rounded-md input-common w-72" bind:value={$selectedProject}>
			<option value="">All Projects</option>
			{#each filteredProjects as project}
				<option value={project['@_name']}>{project['@_name']}</option>
			{/each}
		</select>
	</div>
</div>

<div
	class="grid gap-5 mt-10 p-5 grid-cols-1 sm:grid-cols-2 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 bg-surface-800 rounded-token"
>
	{#each finalFilteredProjects as project}
		<div class="flex flex-col">
			<div
				class="flex justify-between items-center p-2 border-[1px] rounded-t-md border-surface-700"
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
			<div class="">
				<img
					src="https://www.impactplus.com/hubfs/Fensea.jpg"
					class="object-cover border-[1px] rounded-b-md border-surface-700 opacity-70"
					alt="project"
				/>
			</div>
		</div>
	{/each}
</div>
