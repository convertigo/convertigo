<script>
	import { getModalStore } from '@skeletonlabs/skeleton';
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
	import TableFac from '$lib/admin-console/admin-components/TableFac.svelte';
	import Tables from '$lib/admin-console/admin-components/Tables.svelte';

	export const modalStore = getModalStore();

	onMount(() => {
		projectsCheck();
	});

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

	let tableStyle = 'margin-top: 30px';
</script>

<div class="p-5">
	<Card>
		<div class="flex">
			<button class="mr-10 btn variant-filled p-1 pl-4 pr-4">Deploy project</button>
			<button class="mr-10 btn variant-filled p-1 pl-4 pr-4">Import a Remote Project URL</button>
			<button class="mr-10 btn variant-filled p-1 pl-4 pr-4">Delete</button>
		</div>
	</Card>

	<Card customStyle={tableStyle}>
		{#if $projectsStore.length >= 0}
			<Tables
				headers={[
					'Name',
					'Comment',
					'Version',
					'Exported',
					'Deployment',
					'Delete',
					'Reload',
					'Export',
					'Test'
				]}
			>
				{#each $projectsStore as project}
					<tr>
						<td>{project['@_name']}</td>
						<td>{project['@_comment']}</td>
						<td>{project['@_version']}</td>
						<td>{project['@_exported']}</td>
						<td>{project['@_deployDate']}</td>
						<td>
							<button on:click={() => deleteProject(project['@_name'])}>
								<Icon icon="fluent:delete-28-regular" class="w-6 h-6" />
							</button>
						</td>
						<td>
							<button on:click={() => reloadProject(project['@_name'])}>
								<Icon icon="simple-line-icons:reload" rotate={1} class="w-6 h-6" />
							</button>
						</td>
						<td>
							<button on:click={() => exportProject(project['@_name'])}>
								<Icon icon="bytesize:export" class="w-6 h-6" />
							</button>
						</td>
						<td>
							<a href="/admin-console">
								<Icon icon="fluent-mdl2:test-plan" class="w-6 h-6" />
							</a>
						</td>
					</tr>
				{/each}
			</Tables>
		{:else}
			<div>No projects data available</div>
		{/if}
	</Card>
</div>
