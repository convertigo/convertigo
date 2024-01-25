<script>
	import { getModalStore, initializeStores, localStorageStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import {
		deleteProject,
		projectsCheck,
		projectsStore,
		reloadProject
	} from '$lib/admin-console/stores/projectsStore';
	import Icon from '@iconify/svelte';
	import Card from '$lib/admin-console/admin-components/Card.svelte';
	import { call } from '$lib/utils/service';

	let theme = localStorageStore('studio.theme', 'skeleton');

	export const modalStore = getModalStore();

	onMount(() => {
		changeTheme($theme);
		document.body.setAttribute('data-theme', 'dark-theme');

		projectsCheck();
	});

	function changeTheme(e) {
		$theme = typeof e == 'string' ? e : e.target?.value;
		document.body.setAttribute('data-theme', $theme);
	}

	export async function exportProject(projectName) {
		const exportModalSettings = {
			title: 'Export options',
			body: `Select options for exporting the project "${projectName}".`,
			options: []
		};

		try {
			const response = await call('projects.ExportOptions', { projectName });
			if (response) {
				console.log(response);
				exportModalSettings.options = response.admin.options.option.map((opt) => {
					return {
						display: opt['@_display'],
						checked: true
					};
					console.log(response);
				});
				// @ts-ignore
				modalStore.trigger(exportModalSettings);
			} else {
				console.error('There was an error fetching export options.');
			}
		} catch (error) {
			console.error(`An error occurred: ${error}`);
		}
	}

	export let property;
	export let propertyIndex;
	export let selectedIndex;
	export let id = `property-input-${propertyIndex}`;
	export let hasUnsavedChanges;
</script>

<div class="flex flex-col h-full p-10 w-full">
	<Card>
		<div class="flex">
			<button class="mr-10 btn variant-filled p-1 pl-4 pr-4">Deploy project</button>
			<button class="mr-10 btn variant-filled p-1 pl-4 pr-4">Import a Remote Project URL</button>
			<button class="mr-10 btn variant-filled p-1 pl-4 pr-4">Delete</button>
		</div>
	</Card>
	<Card>
		{#if $projectsStore.length >= 0}
			<table class="w-full">
				<thead>
					<tr class="bg-surface-900">
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
					{#each $projectsStore as project}
						<tr>
							<td class="border px-4 py-2">{project['@_name']}</td>
							<td class="border px-4 py-2">{project['@_comment']}</td>
							<td class="border px-4 py-2">{project['@_version']}</td>
							<td class="border px-4 py-2">{project['@_exported']}</td>
							<td class="border px-4 py-2">{project['@_deployDate']}</td>
							<td class="border px-4 py-2">
								<button on:click={() => deleteProject(project['@_name'])}>
									<Icon icon="fluent:delete-28-regular" class="w-6 h-6" />
								</button>
							</td>
							<td class="border px-4 py-2">
								<button on:click={() => reloadProject(project['@_name'])}>
									<Icon icon="simple-line-icons:reload" rotate={1} class="w-6 h-6" />
								</button>
							</td>

							<td class="border px-4 py-2">
								<button on:click={() => exportProject(project['@_name'])}>
									<Icon icon="bytesize:export" class="w-6 h-6" />
								</button>
							</td>
							<td class="border px-4 py-2">
								<a href="/admin-console">
									<Icon icon="fluent-mdl2:test-plan" class="w-6 h-6" />
								</a>
							</td>
						</tr>
					{/each}
				</tbody>
			</table>
		{:else}
			<div>No projects data available</div>
		{/if}
	</Card>
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
