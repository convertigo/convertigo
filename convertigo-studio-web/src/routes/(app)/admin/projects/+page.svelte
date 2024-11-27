<script>
	import { onMount } from 'svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import Projects from '$lib/common/Projects.svelte';
	import { base } from '$app/paths';
	import ModalYesNo from '$lib/common/components/ModalYesNo.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import CheckState from '$lib/admin/components/CheckState.svelte';
	import { getUrl } from '$lib/utils/service';
	import { preventDefault } from 'svelte/legacy';

	onMount(() => {
		Projects.refresh();
	});

	let exportChoices = $state({});

	let modalDelete;
	let modalExport;
</script>

<ModalYesNo bind:this={modalDelete} />
<ModalDynamic bind:this={modalExport}>
	{#snippet children({ close, params: { options, project } })}
		<Card title="Exporting {project}" class="!items-stretch">
			{#each options as { name, display }}
				<CheckState {name} bind:value={exportChoices[name]}>{display}</CheckState>
			{/each}
			<ResponsiveButtons
				class="w-full"
				buttons={[
					{
						icon: 'bytesize:export',
						label: 'Export',
						cls: 'green-button',
						onclick: () => {
							location.href = `${getUrl()}projects.Export?__xsrfToken=${encodeURIComponent(localStorage.getItem('x-xsrf') ?? '')}&projectName=${encodeURIComponent(project)}&exportOptions=${encodeURIComponent(JSON.stringify(exportChoices))}`;
							close();
						}
					},
					{
						icon: 'material-symbols-light:cancel-outline',
						label: 'Cancel',
						cls: 'cancel-button',
						onclick: close
					}
				]}
			/>
		</Card>
	{/snippet}
</ModalDynamic>
<Card title="Projects">
	{#snippet cornerOption()}
		{@const onclick = (e) => alert(e?.target?.value)}
		<ResponsiveButtons
			buttons={[
				{
					icon: 'carbon:application',
					value: 'deploy',
					cls: 'basic-button',
					label: 'Deploy project',
					onclick
				},
				{
					icon: 'bytesize:import',
					value: 'export',
					cls: 'basic-button',
					label: 'Import a Remote Project URL',
					onclick
				}
			]}
			class="max-w-4xl"
		/>
	{/snippet}
	<TableAutoCard
		definition={[
			{ name: 'Actions', custom: true },
			{ name: 'Name', key: '@_name', class: 'font-medium' },
			{ name: 'Comment', key: '@_comment' },
			{ name: 'Version', key: '@_version', class: 'break-words opacity-80 min-w-32' },
			{ name: 'Exported', key: '@_exported', class: 'text-sm min-w-32' },
			{ name: 'Deployment', key: '@_deployDate', class: 'text-sm min-w-32' }
		]}
		data={Projects.projects}
		class="rounded"
	>
		{#snippet children({ row, def })}
			{@const disabled = row['@_name'] == null}
			{@const project = disabled ? '_' : row['@_name']}
			{#if def?.name == 'Actions'}
				<ResponsiveButtons
					buttons={[
						{
							icon: 'mingcute:delete-line',
							cls: 'delete-button',
							disabled,
							onclick: async (event) => {
								if (
									await modalDelete.open({
										event,
										title: 'Delete project',
										message: `${project}?`
									})
								) {
									Projects.remove(project);
								}
							}
						},
						{
							icon: 'simple-line-icons:reload',
							cls: 'green-button',
							disabled,
							onclick: () => {
								Projects.reload(project);
							}
						},
						{
							icon: 'bytesize:export',
							cls: 'basic-button',
							disabled,
							onclick: async (event) => {
								event.currentTarget?.blur();
								const options = await Projects.exportOptions(project);
								exportChoices = options.reduce((acc, option) => {
									acc[option.name] = 'true';
									return acc;
								}, {});
								modalExport.open({ project, options });
							}
						},
						{
							icon: 'file-icons:test-ruby',
							cls: 'yellow-button',
							disabled,
							[disabled ? '_' : 'href']: `${base}/dashboard/${project}/backend/`
						}
					]}
					size="4"
					class="min-w-32 w-full"
				/>
			{/if}
		{/snippet}
	</TableAutoCard>
</Card>
