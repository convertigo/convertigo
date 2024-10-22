<script>
	import { projectsStore } from '$lib/admin/stores/projectsStore';

	import Icon from '@iconify/svelte';
	import { writable } from 'svelte/store';
	// Create a writable store for the search query
	const searchQuery = writable('');
	const selectedProject = writable('');

	// Derived store to filter projects based on search query
	let filteredProjects = $derived(
		$projectsStore.filter((project) =>
			project['@_name'].toLowerCase().includes($searchQuery.toLowerCase())
		)
	);

	let finalFilteredProjects = $derived(
		filteredProjects.filter((project) =>
			$selectedProject ? project['@_name'] === $selectedProject : true
		)
	);
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

		<select class="w-full p-2 border rounded-md input-common w-72">
			<option value="">All Projects</option>
			{#each filteredProjects as project}
				<option value={project['@_name']}>{project['@_name']}</option>
			{/each}
		</select>
	</div>
</div>
