<script>
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { projectsCheck, projectsStore } from '$lib/admin/stores/projectsStore';
	import Icon from '@iconify/svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import { call } from '$lib/utils/service';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';

	const projectModalStore = getModalStore();

	onMount(() => {
		projectsCheck();
	});

	/**
	 * @param {string} mode
	 */
	function openModal(mode) {
		projectModalStore.trigger({
			type: 'component',
			component: 'modalProjects',
			meta: { mode }
		});
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
		projectModalStore.trigger({
			type: 'component',
			component: 'modalProjects',
			meta: { mode: 'Export' },
			response: () => {
				exportProject(projectName);
			}
		});
	}

	export async function deleteProject(projectName) {
		try {
			const response = await call('projects.Delete', { projectName });
			console.log('deleted project', response);
			await projectsCheck();
		} catch (err) {
			console.error('Error deleting project:', err);
		}
	}

	export async function deleteAllProjects() {
		try {
			const response = await call('projects.DeleteAll');
			console.log('deleted project', response);
			await projectsCheck();
		} catch (err) {
			console.error('Error deleting all project:', err);
		}
	}

	async function reloadProject(projectName) {
		try {
			const response = await call('projects.Reload', { projectName });
			console.log('Reload service', response);

			if (response.admin['@_service'] === 'projects.Reload') {
				projectModalStore.trigger({
					type: 'component',
					component: 'modalWarning',
					meta: { mode: 'Success' },
					title: 'Success',
					body: 'Project Reloaded Successfully',
					response: () => {}
				});
			} else if (response.error) {
				throw new Error('Unexpected response structure from reload service.');
			}
		} catch (err) {
			console.error(err);
			//@ts-ignore
			const errorMessage =
				//@ts-ignore
				err.response?.admin?.error?.message || 'An error occurred while reloading the project.';
			projectModalStore.trigger({
				type: 'component',
				component: 'modalWarning',
				meta: { mode: 'Error' },
				title: 'Error',
				body: errorMessage,
				response: () => {}
			});
		}
	}

	function openReloadProjectModal(projectName) {
		projectModalStore.trigger({
			type: 'component',
			component: 'modalWarning',
			meta: { mode: 'Confirm' },
			title: 'Please Confirm',
			body: `Reload the Project '${projectName}' ?`,
			response: (confirmed) => {
				if (confirmed) {
					reloadProject(projectName);
				}
			}
		});
	}

	function openDeleteProjectModal(projectName) {
		projectModalStore.trigger({
			type: 'component',
			title: 'Please Confirm',
			body: 'Are you sure you want to delete this project ?',
			component: 'modalWarning',
			meta: { mode: 'Confirm' },
			response: (confirmed) => {
				if (confirmed) {
					deleteProject(projectName);
					console.log('key deleted', { projectName });
				}
			}
		});
	}

	function openDeleteAllProjectModal(projectName) {
		projectModalStore.trigger({
			type: 'component',
			title: 'Please Confirm',
			body: 'Are you sure you want to delete this project ?',
			component: 'modalWarning',
			meta: { mode: 'Confirm' },
			response: (confirmed) => {
				if (confirmed) {
					deleteProject(projectName);
					console.log('key deleted', { projectName });
				}
			}
		});
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
	<div slot="cornerOption">
		<button class="w-full bg-error-400-500-token">
			<Ico icon="mingcute:delete-line" />
			Delete All Projects</button
		>
	</div>
	<ButtonsContainer>
		{#each Object.entries(projectActions) as [type, { name, icon }]}
			<button class="basic-button" on:click={() => openModal(type)}>
				<p>{name}</p>
				<Icon {icon} class="w-4 h-4" />
			</button>
		{/each}
		<!-- <button class="basic-button" on:click={() => openModal('deploy')}>
			<Icon icon="carbon:application" class="w-4 h-4" />
			Deploy project
		</button>

		<button class="basic-button" on:click={() => openModal('import')}>
			<Icon icon="solar:import-line-duotone" class="w-6 h-6" />
			Import a Remote Project URL
		</button> -->
	</ButtonsContainer>
</Card>

<Card class="mt-5">
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
			let:row
			let:def
		>
			{#if def.name == 'Delete'}
				<button on:click={() => openDeleteProjectModal(row['@_name'])} class="delete-button">
					<Ico icon="mingcute:delete-line" />
				</button>
			{:else if def.name == 'Reload'}
				<button on:click={() => openReloadProjectModal(row['@_name'])} class="cancel-button">
					<Ico icon="simple-line-icons:reload" />
				</button>
			{:else if def.name == 'Export'}
				<button on:click={() => openExportProjectModal(row['@_name'])} class="basic-button">
					<Ico icon="bytesize:export" />
				</button>
			{:else if def.name == 'Test'}
				<button class="yellow-button">
					<a href="/admin">
						<Ico icon="file-icons:test-ruby" />
					</a>
				</button>
			{/if}
		</TableAutoCard>
	{/if}
</Card>
