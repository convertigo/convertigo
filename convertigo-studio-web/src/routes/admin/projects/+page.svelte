<script>
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { projectsCheck, projectsStore, reloadProject } from '$lib/admin/stores/projectsStore';
	import Icon from '@iconify/svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import { call } from '$lib/utils/service';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import { writable } from 'svelte/store';

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

	export async function deleteProject(projectName) {
		let notification = {
			title: '',
			body: ''
		};

		try {
			const response = await call('projects.Delete', { projectName });
			console.log('deleted project', response);

			if (response?.admin) {
				notification.title = 'Project Deleted Successfully';
				projectsStore.update((projects) =>
					projects.filter((project) => project['@_name'] !== projectName)
				);
			} else {
				throw new Error(
					response?.error?.message || 'Unknown error occurred while deleting the project.'
				);
			}
		} catch (err) {
			console.error('Error deleting project:', err);
			notification.title = 'Error Deleting Project';
			//@ts-ignore
			notification.body = err.message || 'An unknown error occurred during the deletion process.';
		} finally {
			//@ts-ignore
			projectModalStore.trigger(notification);
		}
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
			<button class="w-full bg-primary-400-500-token" on:click={() => openModal('deploy')}
				>Deploy project</button
			>
		</div>

		<div class="flex-1">
			<button class="w-full bg-primary-400-500-token" on:click={() => openModal('import')}
				>Import a Remote Project URL</button
			>
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
					<Icon icon="fluent:delete-28-regular" class="w-6 h-6" />
				</button>
			{:else if def.name == 'Reload'}
				<button on:click={() => reloadProject(row['@_name'])} class="bg-tertiary-400-500-token">
					<Icon icon="simple-line-icons:reload" rotate={1} class="w-6 h-6" />
				</button>
			{:else if def.name == 'Export'}
				<button on:click={() => exportProject(row['@_name'])} class="bg-primary-400-500-token">
					<Icon icon="bytesize:export" class="w-6 h-6" />
				</button>
			{:else if def.name == 'Test'}
				<a href="/admin-console" class="bg-secondary-400-500-token btn">
					<Icon icon="fluent-mdl2:test-plan" class="w-6 h-6" />
				</a>
			{/if}
		</TableAutoCard>
	{/if}
</Card>
