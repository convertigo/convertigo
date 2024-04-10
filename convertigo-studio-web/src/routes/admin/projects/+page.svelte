<script>
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { projectsCheck, projectsStore } from '$lib/admin/stores/projectsStore';
	import Icon from '@iconify/svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import { call } from '$lib/utils/service';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import Ico from '$lib/utils/Ico.svelte';

	const projectModalStore = getModalStore();

	onMount(() => {
		projectsCheck();
		exportProject();
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

	/**
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
	}*/

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
			await projectsCheck(true);
		} catch (err) {
			console.error('Error deleting project:', err);
		}
	}

	export async function deleteAllProjects() {
		try {
			const response = await call('projects.DeleteAll');
			console.log('deleted project', response);
			await projectsCheck(true);
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
</script>

<Card title="Projects">
	<div slot="cornerOption">
		<button class="w-full bg-error-400-500-token">
			<Icon icon="material-symbols-light:delete-outline" class="w-7 h-7 mr-3" />
			Delete All Projects</button
		>
	</div>
	<div class="flex flex-wrap gap-5 mt-5">
		<div class="flex-1">
			<button class="w-full bg-primary-400-500-token" on:click={() => openModal('deploy')}>
				<Icon icon="carbon:application" class="w-6 h-6 mr-3" />
				Deploy project
			</button>
		</div>

		<div class="flex-1">
			<button class="w-full bg-primary-400-500-token" on:click={() => openModal('import')}>
				<Icon icon="solar:import-line-duotone" class="w-6 h-6 mr-4" />
				Import a Remote Project URL
			</button>
		</div>
	</div>
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
				<button
					on:click={() => openDeleteProjectModal(row['@_name'])}
					class="bg-error-400-500-token"
				>
					<Ico icon="material-symbols-light:delete-outline" class="h-6 w-6 " />
				</button>
			{:else if def.name == 'Reload'}
				<button
					on:click={() => openReloadProjectModal(row['@_name'])}
					class="bg-tertiary-400-500-token"
				>
					<Icon icon="simple-line-icons:reload" rotate={1} class="w-6 h-6" />
				</button>
			{:else if def.name == 'Export'}
				<button
					on:click={() => openExportProjectModal(row['@_name'])}
					class="bg-primary-400-500-token"
				>
					<Icon icon="solar:export-line-duotone" class="w-6 h-6" />
				</button>
			{:else if def.name == 'Test'}
				<a href="/admin" class="bg-secondary-400-500-token btn">
					<Icon icon="fluent-mdl2:test-plan" class="w-6 h-6" />
				</a>
			{/if}
		</TableAutoCard>
	{/if}
</Card>
