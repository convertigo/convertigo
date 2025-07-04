<script>
	import { FileUpload } from '@skeletonlabs/skeleton-svelte';
	import { base } from '$app/paths';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import CheckState from '$lib/admin/components/CheckState.svelte';
	import ProjectEditor from '$lib/admin/components/ProjectEditor.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import Projects from '$lib/common/Projects.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { getContext, onDestroy } from 'svelte';

	let {
		projects,
		deploy,
		exportProject,
		importURL,
		remove,
		refresh,
		reload,
		exportOptions,
		undefinedSymbols,
		createSymbols,
		waiting,
		init
	} = $derived(Projects);
	let exportChoices = $state({});

	onDestroy(Projects.stop);

	let modalYesNo = getContext('modalYesNo');
	let modalExport = $state();
	let modalDeployUpload = $state();
	let modalDeployURL = $state();
	let modalSymbols;

	let filter = $state('');
	let fprojects = $derived(
		projects.filter((s) => JSON.stringify(s).toLowerCase().includes(filter.toLowerCase()))
	);

	let editedProject = $state('');
</script>

<ModalDynamic bind:this={modalExport}>
	{#snippet children({ close, params: { options, project } })}
		<Card title="Exporting {project}">
			{#each options as { name, display }}
				<CheckState {name} bind:value={exportChoices[name]}>{display}</CheckState>
			{/each}
			<ResponsiveButtons
				class="w-full"
				buttons={[
					{
						icon: 'bytesize:export',
						label: 'Export',
						cls: 'button-primary',
						onclick: async () => {
							if (
								await exportProject({
									projectName: project,
									exportOptions: JSON.stringify(exportChoices)
								})
							) {
								close();
							}
						}
					},
					{
						icon: 'material-symbols-light:cancel-outline',
						label: 'Cancel',
						cls: 'button-error',
						onclick: close
					}
				]}
			/>
		</Card>
	{/snippet}
</ModalDynamic>

<ModalDynamic bind:this={modalDeployUpload}>
	<Card title="Drop or choose a .car/.zip file and Deploy">
		<form
			onsubmit={async (event) => {
				await deploy(event);
				modalDeployUpload.close();
			}}
		>
			<fieldset disabled={waiting} class="layout-y-stretch">
				<FileUpload
					name="file"
					accept={{ 'application/zip': ['.car', '.zip'] }}
					maxFiles={1}
					subtext="then press Deploy"
					classes="w-full preset-filled-surface-300-700"
					required
					allowDrop
				>
					{#snippet iconInterface()}<Ico icon="carbon:application" size="8" />{/snippet}
					{#snippet iconFile()}<Ico icon="mdi:briefcase-upload-outline" size="8" />{/snippet}
					{#snippet iconFileRemove()}<Ico
							icon="material-symbols-light:delete-outline"
							size="8"
						/>{/snippet}
				</FileUpload>
				<CheckState name="bAssembleXsl" value="false"
					>Assemble XSL files included in style sheets when deploying</CheckState
				>
				<div class="layout-x justify-end">
					<Button
						label="Deploy"
						icon="carbon:application"
						type="submit"
						class="button-primary w-fit!"
					/>
					<Button
						label="Cancel"
						icon="material-symbols-light:cancel-outline"
						class="button-error w-fit!"
						onclick={modalDeployUpload.close}
					/>
				</div>
			</fieldset>
		</form>
	</Card>
</ModalDynamic>

<ModalDynamic bind:this={modalDeployURL}>
	<Card title="Import from a Remote Project URL">
		<form
			onsubmit={async (event) => {
				await importURL(event);
				modalDeployURL.close();
			}}
		>
			<fieldset disabled={waiting} class="layout-y-stretch">
				<p>Import a project from url like:</p>
				<p class="font-bold">
					{'<project name>=<git or http URL>[:path=<optional subpath>][:branch=<optional branch>]'}
				</p>
				<p>Or a Convertigo Archive HTTP(S) URL.</p>
				<PropertyType
					name="url"
					placeholder="<project name>=<git or http URL>[:path=<optional subpath>][:branch=<optional branch>]"
					required
				/>
				<div class="layout-x justify-end">
					<Button
						label="Import"
						icon="bytesize:import"
						type="submit"
						class="button-secondary w-fit!"
					/>
					<Button
						label="Cancel"
						icon="material-symbols-light:cancel-outline"
						class="button-error w-fit!"
						onclick={modalDeployURL.close}
					/>
				</div>
			</fieldset>
		</form>
	</Card>
</ModalDynamic>

<ModalDynamic bind:this={modalSymbols} class="w-[800px]">
	{#snippet children({ close, params: { symbols, project } })}
		<Card title="Undefined Global Symbols" class="w-full">
			<p>Find here the undefined Global Symbols for the project <b>{project}</b>:</p>
			<div class="layout-x w-full flex-wrap">
				{#each symbols as symbol}
					<div class="rounded-sm preset-filled-secondary-200-800 px-low">{symbol}</div>
				{/each}
			</div>
			<div class="layout-x w-full justify-end">
				<Button
					label="Create symbols"
					icon="et:tools-2"
					class="button-primary"
					onclick={() => close(true)}
				/>
				<Button
					label="Cancel"
					icon="material-symbols-light:cancel-outline"
					class="button-error"
					onclick={() => close(false)}
				/>
			</div>
		</Card>
	{/snippet}
</ModalDynamic>

<Card title="Projects">
	{#snippet cornerOption()}
		<ResponsiveButtons
			buttons={[
				{
					icon: 'carbon:application',
					value: 'deploy',
					cls: 'button-primary',
					label: 'Deploy project',
					onclick: modalDeployUpload?.open
				},
				{
					icon: 'bytesize:import',
					value: 'export',
					cls: 'button-secondary',
					label: 'Import a Remote Project URL',
					onclick: modalDeployURL?.open
				}
			]}
			class="max-w-4xl"
			disabled={!init}
		/>
	{/snippet}
	<div
		class="input-group w-full grid-cols-[auto_1fr_auto] divide-x divide-surface-700-300 preset-outlined-surface-700-300 bg-surface-200-800"
	>
		<label for="projectsFilter" class="ig-cell"><Ico icon="mdi:magnify" /> </label>
		<input
			class="ig-input placeholder:text-surface-500"
			type="search"
			id="projectsFilter"
			placeholder="Filter projects..."
			bind:value={filter}
		/>
	</div>
	<TableAutoCard
		definition={[
			{ name: 'Actions', custom: true },
			{ name: 'Project', key: 'name', class: 'font-medium' },
			{ name: 'Comment', key: 'comment' },
			{
				name: 'Version',
				key: 'version',
				class: 'break-words text-[12px]  font-mono opacity-80 min-w-34'
			},
			{ name: 'Exported', key: 'exported', class: 'text-[12px] font-mono min-w-34' },
			{ name: 'Deployment', key: 'deployDate', class: 'text-[12px] font-mono min-w-34' }
		]}
		data={fprojects}
		class="rounded-sm"
	>
		{#snippet children({ row: { name, undefined_symbols }, def })}
			{@const project = name ? name : '_'}
			{#if def?.name == 'Actions'}
				<ResponsiveButtons
					buttons={[
						{
							icon: 'mingcute:delete-line',
							cls: 'button-error',
							onclick: async (event) => {
								if (
									await modalYesNo.open({
										event,
										title: 'Delete project',
										message: `${project}?`
									})
								) {
									remove(project);
								}
							}
						},
						{
							icon: 'simple-line-icons:reload',
							cls: 'button-success',
							onclick: () => {
								reload(project);
							}
						},
						{
							icon: 'bytesize:export',
							cls: 'button-primary',
							onclick: async (event) => {
								event.currentTarget?.blur();
								const options = await exportOptions(project);
								exportChoices = options.reduce((acc, option) => {
									acc[option.name] = 'true';
									return acc;
								}, {});
								modalExport.open({ project, options });
							}
						},
						{
							icon: 'mdi:edit-outline',
							cls: `button-warning ${editedProject == project ? 'opacity-50' : ''}`,
							onclick: () => (editedProject = editedProject == project ? '' : project),
							disabled: false
						},
						{
							icon: 'file-icons:test-ruby',
							cls: 'button-tertiary',
							href: `${base}/dashboard/${project}/backend/`,
							disabled: false
						},
						{
							icon: 'mdi:warning-outline',
							cls: 'button preset-tonal-warning',
							hidden: !undefined_symbols,
							onclick: async (event) => {
								event.currentTarget?.blur();
								const symbols = await undefinedSymbols(project);
								if (await modalSymbols.open({ project, symbols })) {
									await createSymbols(project);
									refresh();
								}
							}
						}
					]}
					size="4"
					class="w-full min-w-33"
					disabled={!init}
				/>
			{/if}
		{/snippet}
		{#snippet rowChildren({ row, rowIdx, definition, rowRender })}
			{#if editedProject == row.name}
				<td
					colspan={definition.length}
					class:preset-filled-surface-100-900={rowIdx % 2 == 1}
					class:preset-filled-surface-200-800={rowIdx % 2 == 0}
				>
					<table class="w-full"><tbody><tr> {@render rowRender()}</tr></tbody></table>
					<ProjectEditor project={row.name} class="!-m-low p" />
				</td>
			{:else}
				{@render rowRender()}
			{/if}
		{/snippet}
	</TableAutoCard>
</Card>
