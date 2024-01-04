<script>
	import { localStorageStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { fetchProjectsList } from '$lib/admin-console/stores/Store';

	let theme = localStorageStore('studio.theme', 'skeleton');

	let projectsData = {};

	async function updateProjects() {
		const response = await fetchProjectsList();
		projectsData = response.admin.projects;
	}

	onMount(() => {
		changeTheme($theme);
		document.body.setAttribute('data-theme', 'dark-theme');

		updateProjects();
		const interval = setInterval(updateProjects, 3000);

		return () => {
			clearInterval(interval);
		};
	});

	function changeTheme(e) {
		$theme = typeof e == 'string' ? e : e.target?.value;
		document.body.setAttribute('data-theme', $theme);
	}
</script>

<div class="flex flex-col h-full p-10 w-full">
	{#if Object.keys(projectsData).length}
		<h1 class="text-[15px] mt-10">Projects</h1>

		<table class="mt-5 w-full">
			<thead>
				<tr class="bg-gray-700">
					<th class="px-4 py-2">Name</th>
					<th class="px-4 py-2">Comment</th>
					<th class="px-4 py-2">Version</th>
					<th class="px-4 py-2">Exported</th>
					<th class="px-4 py-2">Deployment</th>
					<th class="px-4 py-2">Delete</th>
					<th class="px-4 py-2">Reload</th>
					<th class="px-4 py-2">Export</th>
					<th class="px-4 py-2">Test</th>
				</tr>
			</thead>
			<tbody>
				{#each projectsData.project as project}
					<tr>
						<td class="border px-4 py-2">{project['@_name']}</td>
						<td class="border px-4 py-2">{project['@_comment']}</td>
						<td class="border px-4 py-2">{project['@_version']}</td>
						<td class="border px-4 py-2">{project['@_exported']}</td>
						<td class="border px-4 py-2">{project['@_deployDate']}</td>
						<td class="border px-4 py-2">Delete Action</td>
						<td class="border px-4 py-2">Reload Action</td>
						<td class="border px-4 py-2">Export Action</td>
						<td class="border px-4 py-2">Test Action</td>
						<!-- Add more <td> elements as needed -->
					</tr>
				{/each}
			</tbody>
		</table>
	{:else}
		<div>No connection data available</div>
	{/if}
</div>

<style>
	th,
	td {
		border: 1px solid #616161;
		padding: 4px;
		text-align: left;
		font-weight: 300;
		font-size: 13px;
	}

	table {
		border-collapse: collapse;
	}
</style>
