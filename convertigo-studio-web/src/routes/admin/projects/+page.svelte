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
</script>

<Card title="Projects">
	<div slot="cornerOption">
		<button class="w-full bg-error-400-500-token">
			<Icon icon="material-symbols-light:delete-outline" class="w-7 h-7 mr-3" />
			Delete All Projects</button
		>
	</div>
	<div class="flex flex-wrap gap-5 mt-10">
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

<Card class="mt-10">
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
				<button on:click={() => deleteProject(row['@_name'])} class="bg-error-400-500-token">
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
	{:else}
		<div class="table-container">
			<table class="rounded-token table">
				<thead class="rounded-token">
					<tr>
						{#each Array(9) as _}
							<th class="header dark:bg-surface-800">
								<div class="my-2 h-8 placeholder animate-pulse"></div>
							</th>
						{/each}
					</tr>
				</thead>
				<tbody>
					{#each Array(5) as _}
						<tr>
							{#each Array(9) as _}
								<td>
									<div class="my-2 h-8 placeholder animate-pulse"></div>
								</td>
							{/each}
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{/if}
</Card>
