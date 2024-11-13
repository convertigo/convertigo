<script>
	import { onMount } from 'svelte';
	import { projectsCheck, projectsStore } from '$lib/admin/stores/projectsStore';
	import Card from '$lib/admin/components/Card.svelte';
	import { call } from '$lib/utils/service';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';

	onMount(() => {
		projectsCheck();
	});

	/**
	 * @param {string} mode
	 */
	function openModal(mode) {
		// projectModalStore.trigger({
		// 	type: 'component',
		// 	component: 'modalProjects',
		// 	meta: { mode }
		// });
	}

	/**
	 * @param {string} projectName
	 */
	async function exportProject(projectName) {
		try {
			const response = await call('projects.ExportOptions', { projectName });

			if (response !== undefined) {
			}
		} catch (err) {
			console.error(err);
		}
	}

	function openExportProjectModal(projectName) {
		// projectModalStore.trigger({
		// 	type: 'component',
		// 	component: 'modalProjects',
		// 	meta: { mode: 'Export' },
		// 	response: () => {
		// 		exportProject(projectName);
		// 	}
		// });
	}

	export async function deleteProject(projectName) {
		try {
			const response = await call('projects.Delete', { projectName });
			await projectsCheck();
		} catch (err) {
			console.error('Error deleting project:', err);
		}
	}

	export async function deleteAllProjects() {
		try {
			const response = await call('projects.DeleteAll');
			await projectsCheck();
		} catch (err) {
			console.error('Error deleting all project:', err);
		}
	}

	async function reloadProject(projectName) {
		try {
			const response = await call('projects.Reload', { projectName });

			if (response.admin['@_service'] === 'projects.Reload') {
				// projectModalStore.trigger({
				// 	type: 'component',
				// 	component: 'modalWarning',
				// 	meta: { mode: 'Success' },
				// 	title: 'Success',
				// 	body: 'Project Reloaded Successfully',
				// 	response: () => {}
				// });
			} else if (response.error) {
				throw new Error('Unexpected response structure from reload service.');
			}
		} catch (err) {
			console.error(err);
			//@ts-ignore
			const errorMessage =
				//@ts-ignore
				err.response?.admin?.error?.message || 'An error occurred while reloading the project.';
			// projectModalStore.trigger({
			// 	type: 'component',
			// 	component: 'modalWarning',
			// 	meta: { mode: 'Error' },
			// 	title: 'Error',
			// 	body: errorMessage,
			// 	response: () => {}
			// });
		}
	}

	function openReloadProjectModal(projectName) {
		// projectModalStore.trigger({
		// 	type: 'component',
		// 	component: 'modalWarning',
		// 	meta: { mode: 'Confirm' },
		// 	title: 'Please Confirm',
		// 	body: `Reload the Project '${projectName}' ?`,
		// 	response: (confirmed) => {
		// 		if (confirmed) {
		// 			reloadProject(projectName);
		// 		}
		// 	}
		// });
	}

	function openDeleteProjectModal(projectName) {
		// projectModalStore.trigger({
		// 	type: 'component',
		// 	title: 'Please Confirm',
		// 	body: 'Are you sure you want to delete this project ?',
		// 	component: 'modalWarning',
		// 	meta: { mode: 'Confirm' },
		// 	response: (confirmed) => {
		// 		if (confirmed) {
		// 			deleteProject(projectName);
		// 		}
		// 	}
		// });
	}

	function openDeleteAllProjectModal(projectName) {
		// projectModalStore.trigger({
		// 	type: 'component',
		// 	title: 'Please Confirm',
		// 	body: 'Are you sure you want to delete this project ?',
		// 	component: 'modalWarning',
		// 	meta: { mode: 'Confirm' },
		// 	response: (confirmed) => {
		// 		if (confirmed) {
		// 			deleteProject(projectName);
		// 		}
		// 	}
		// });
	}

	const projectActions = {
		deploy: {
			name: 'Deploy project',
			icon: 'carbon:application'
		},
		export: {
			name: 'Import a Remote Project URL',
			icon: 'bytesize:import'
		}
	};
</script>

<Card title="Projects">
	{#snippet cornerOption()}
		<button class="delete-button">
			<Ico icon="mingcute:delete-line" />
			<p>Delete All Projects</p></button
		>
	{/snippet}
	<ButtonsContainer>
		{#each Object.entries(projectActions) as [type, { name, icon }]}
			<button class="basic-button" onclick={() => openModal(type)}>
				<p>{name}</p>
				<Ico {icon} size="nav" />
			</button>
		{/each}
	</ButtonsContainer>
</Card>

<Card>
	{#if $projectsStore.length > 0}
		<TableAutoCard
			definition={[
				{ name: 'Name', key: '@_name' },
				{ name: 'Comment', key: '@_comment' },
				{ name: 'Version', key: '@_version' },
				{ name: 'Exported', key: '@_exported' },
				{ name: 'Deployment', key: '@_deployDate' },
				{ name: 'Delete', custom: true },
				{ name: 'Reload', custom: true },
				{ name: 'Export', custom: true },
				{ name: 'Test', custom: true }
			]}
			data={$projectsStore}
		>
			{#snippet children({ row, def })}
				{#if def?.name == 'Delete'}
					<button onclick={() => openDeleteProjectModal(row['@_name'])} class="delete-button">
						<Ico icon="mingcute:delete-line" />
					</button>
				{:else if def?.name == 'Reload'}
					<button onclick={() => openReloadProjectModal(row['@_name'])} class="green-button">
						<Ico icon="simple-line-icons:reload" />
					</button>
				{:else if def?.name == 'Export'}
					<button onclick={() => openExportProjectModal(row['@_name'])} class="basic-button">
						<Ico icon="bytesize:export" />
					</button>
				{:else if def?.name == 'Test'}
					<button class="yellow-button">
						<a href="/admin">
							<Ico icon="file-icons:test-ruby" />
						</a>
					</button>
				{/if}
			{/snippet}
		</TableAutoCard>
	{/if}
</Card>
