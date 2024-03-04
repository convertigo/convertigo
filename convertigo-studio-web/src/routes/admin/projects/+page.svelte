<script>
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import {
		deleteProject,
		projectsCheck,
		projectsStore,
		reloadProject
	} from '$lib/admin/stores/projectsStore';
	import Icon from '@iconify/svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import { call } from '$lib/utils/service';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';

	export const modalStore = getModalStore();

	onMount(() => {
		projectsCheck();
	});

	/**
	 * @param {string} mode
	 */
	function openModal(mode) {
		modalStore.trigger({
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

	let tableStyle = 'margin-top: 30px';
</script>

<Card title="Projects">
	<div class="flex flex-wrap gap-5">
		<div class="flex-1">
			<button class="w-full" on:click={() => openModal('deploy')}>Deploy project</button>
		</div>

		<div class="flex-1">
			<button class="w-full" on:click={() => openModal('import')}
				>Import a Remote Project URL</button
			>
		</div>

		<div class="flex-1">
			<button class="w-full">Delete All</button>
		</div>
	</div>
</Card>

<Card customStyle={tableStyle}>
	{#if $projectsStore.length >= 0}
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
				<button on:click={() => deleteProject(row['@_name'])}>
					<Icon icon="fluent:delete-28-regular" class="w-6 h-6" />
				</button>
			{:else if def.name == 'Reload'}
				<button on:click={() => reloadProject(row['@_name'])}>
					<Icon icon="simple-line-icons:reload" rotate={1} class="w-6 h-6" />
				</button>
			{:else if def.name == 'Export'}
				<button on:click={() => exportProject(row['@_name'])}>
					<Icon icon="bytesize:export" class="w-6 h-6" />
				</button>
			{:else if def.name == 'Test'}
				<a href="/admin-console">
					<Icon icon="fluent-mdl2:test-plan" class="w-6 h-6" />
				</a>
			{/if}
		</TableAutoCard>
	{:else}
		<div>No projects data available</div>
	{/if}
</Card>
