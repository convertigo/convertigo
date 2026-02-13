<script>
	import { browser } from '$app/environment';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import ActionBar from '$lib/admin/components/ActionBar.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import CheckState from '$lib/admin/components/CheckState.svelte';
	import FileUploadField from '$lib/admin/components/FileUploadField.svelte';
	import ProjectEditor from '$lib/admin/components/ProjectEditor.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import InputGroup from '$lib/common/components/InputGroup.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import Projects from '$lib/common/Projects.svelte';
	import { getContext, onDestroy, tick } from 'svelte';
	import { persistedState } from 'svelte-persisted-state';

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

	const filterState = persistedState('admin.projects.filter', '', { syncTabs: false });

	let fprojects = $derived(
		projects.filter((s) =>
			JSON.stringify(s).toLowerCase().includes(filterState.current.toLowerCase())
		)
	);

	let editedProject = $state('');
	let scrollJob = 0;

	const findProjectRow = (project) => {
		return [...document.querySelectorAll('.projects-table tr[data-custom]')].find(
			(row) => row.getAttribute('data-custom') === project
		);
	};

	/** @param {Element} element @param {ScrollBehavior} [behavior='smooth'] */
	const centerInViewport = (element, behavior = 'smooth') => {
		const { top, height } = element.getBoundingClientRect();
		const offset = Math.max(0, (window.innerHeight - height) / 2);
		window.scrollTo({
			top: Math.max(0, window.scrollY + top - offset),
			behavior
		});
	};

	/** @param {string} project @param {{behavior?: ScrollBehavior, retries?: number, delayMs?: number}} [options] */
	const scrollToProjectRow = async (
		project,
		{ behavior = 'smooth', retries = 120, delayMs = 50 } = {}
	) => {
		if (!browser || !project) return false;
		const currentJob = ++scrollJob;
		for (let attempt = 0; attempt < retries; attempt++) {
			if (currentJob !== scrollJob) return false;
			await tick();
			const row = findProjectRow(project);
			if (row) {
				const anchor = row.querySelector('table tbody tr') ?? row;
				centerInViewport(anchor, behavior);
				return true;
			}
			await new Promise((resolve) => setTimeout(resolve, delayMs));
		}
		return false;
	};

	const syncEditFromRoute = () => {
		const fromRoute = page.params.project ?? '';
		if (fromRoute === editedProject) return;
		editedProject = fromRoute;
		if (fromRoute) {
			void scrollToProjectRow(fromRoute, { behavior: 'auto' });
		}
	};

	const setEditedProject = async (project) => {
		const next = editedProject == project ? '' : project;
		editedProject = next;
		const nextUrl = next
			? resolve('/(app)/admin/projects/[project]', { project: next })
			: resolve('/(app)/admin/projects');
		const currentUrl = `${page.url.pathname}${page.url.search}${page.url.hash}`;
		if (nextUrl !== currentUrl) {
			await goto(nextUrl, {
				replaceState: true,
				noScroll: true,
				keepFocus: true,
				invalidateAll: false
			});
		}
		if (next) {
			void scrollToProjectRow(next);
		}
	};

	$effect(() => {
		page.params.project;
		syncEditFromRoute();
	});
</script>

<ModalDynamic bind:this={modalExport}>
	{#snippet children({ close, params: { options, project } })}
		<Card title="Exporting {project}">
			{#each options as { name, display } (name)}
				<CheckState {name} bind:value={exportChoices[name]}>{display}</CheckState>
			{/each}
			<ActionBar>
				<Button
					icon="mdi:export"
					label="Export"
					class="button-primary w-fit!"
					onclick={async () => {
						if (
							await exportProject({
								projectName: project,
								exportOptions: JSON.stringify(exportChoices)
							})
						) {
							close();
						}
					}}
				/>
				<Button
					icon="mdi:close-circle-outline"
					label="Cancel"
					class="button-secondary w-fit!"
					onclick={close}
				/>
			</ActionBar>
		</Card>
	{/snippet}
</ModalDynamic>

<ModalDynamic bind:this={modalDeployUpload}>
	<Card title="Drop or choose a .car/.zip file and Deploy">
		<form
			onsubmit={async (event) => {
				await deploy(event);
				modalDeployUpload?.close();
			}}
		>
			<fieldset disabled={waiting} class="layout-y-stretch">
				<FileUploadField
					name="file"
					accept={{ 'application/zip': ['.car', '.zip'] }}
					required
					allowDrop
					dropIcon="mdi:application-outline"
					title="Drop or choose a .car/.zip file"
					hint="then press Deploy"
				/>
				<CheckState name="bAssembleXsl" value="false"
					>Assemble XSL files included in style sheets when deploying</CheckState
				>
				<ActionBar>
					<Button
						label="Deploy"
						icon="mdi:application-outline"
						type="submit"
						class="button-primary w-fit!"
					/>
					<Button
						label="Cancel"
						icon="mdi:close-circle-outline"
						class="button-secondary w-fit!"
						onclick={modalDeployUpload?.close}
					/>
				</ActionBar>
			</fieldset>
		</form>
	</Card>
</ModalDynamic>

<ModalDynamic bind:this={modalDeployURL}>
	<Card title="Import from a Remote Project URL">
		<form
			onsubmit={async (event) => {
				await importURL(event);
				modalDeployURL?.close();
			}}
		>
			<fieldset disabled={waiting} class="layout-y-stretch">
				<p>Import a project from url like:</p>
				<p class="font-medium">
					&lt;project name&gt;=&lt;git or http URL&gt;[:path=&lt;optional
					subpath&gt;][:branch=&lt;optional branch&gt;]
				</p>
				<p>Or a Convertigo Archive HTTP(S) URL.</p>
				<PropertyType
					name="url"
					placeholder="<project name>=<git or http URL>[:path=<optional subpath>][:branch=<optional branch>]"
					required
				/>
				<ActionBar>
					<Button label="Import" icon="mdi:import" type="submit" class="button-secondary w-fit!" />
					<Button
						label="Cancel"
						icon="mdi:close-circle-outline"
						class="button-secondary w-fit!"
						onclick={modalDeployURL?.close}
					/>
				</ActionBar>
			</fieldset>
		</form>
	</Card>
</ModalDynamic>

<ModalDynamic bind:this={modalSymbols} class="w-[800px]">
	{#snippet children({ close, params: { symbols, project } })}
		<Card title="Undefined Global Symbols" class="w-full">
			<p>Find here the undefined Global Symbols for the project <b>{project}</b>:</p>
			<div class="layout-x-wrap w-full">
				{#each symbols as symbol (symbol)}
					<div class="rounded-sm preset-filled-secondary-200-800 px-low">{symbol}</div>
				{/each}
			</div>
			<ActionBar>
				<Button
					label="Create symbols"
					icon="mdi:wrench"
					class="button-primary w-fit!"
					onclick={() => close(true)}
				/>
				<Button
					label="Cancel"
					icon="mdi:close-circle-outline"
					class="button-secondary w-fit!"
					onclick={() => close(false)}
				/>
			</ActionBar>
		</Card>
	{/snippet}
</ModalDynamic>

<Card title="Projects">
	{#snippet cornerOption()}
		<ResponsiveButtons
			buttons={[
				{
					icon: 'mdi:application-outline',
					value: 'deploy',
					cls: 'button-primary',
					label: 'Deploy project',
					onclick: modalDeployUpload?.open
				},
				{
					icon: 'mdi:import',
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
	<InputGroup
		id="projectsFilter"
		type="search"
		placeholder="Filter projects..."
		autofocus
		icon="mdi:magnify"
		bind:value={filterState.current}
	/>
	<TableAutoCard
		definition={[
			{ name: 'Project', key: 'name', class: 'font-normal w-80' },
			{ name: 'Comment', key: 'comment' },
			{
				name: 'Version',
				key: 'version',
				class: 'break-words text-[12px] min-w-34'
			},
			{ name: 'Exported', key: 'exported', class: 'text-[12px] min-w-34' },
			{ name: 'Deployment', key: 'deployDate', class: 'text-[12px] min-w-34' },
			{ name: 'Actions', custom: true, class: 'w-56' }
		]}
		data={fprojects}
		class="projects-table rounded-sm"
	>
		{#snippet children({ row: { name, undefined_symbols }, def })}
			{@const project = name ? name : '_'}
			{#if def?.name == 'Actions'}
				<ResponsiveButtons
					buttons={[
						{
							icon: 'mdi:warning-outline',
							title: 'Show undefined global symbols',
							cls: undefined_symbols
								? 'button-ico-warning'
								: 'button-ico-warning opacity-0 pointer-events-none',
							disabled: !undefined_symbols,
							onclick: async (event) => {
								if (!undefined_symbols) {
									return;
								}
								event.currentTarget?.blur();
								const symbols = await undefinedSymbols(project);
								if (await modalSymbols.open({ project, symbols })) {
									await createSymbols(project);
									refresh();
								}
							}
						},
						{
							icon: 'mdi:edit-outline',
							title: 'Edit project configuration',
							cls: `button-ico-primary ${editedProject == project ? 'opacity-50' : ''}`,
							onclick: () => setEditedProject(project),
							disabled: false
						},
						{
							icon: 'mdi:reload',
							title: 'Reload project',
							cls: 'button-ico-primary',
							onclick: () => {
								reload(project);
							}
						},
						{
							icon: 'mdi:export',
							title: 'Export project',
							cls: 'button-ico-primary',
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
							icon: 'mdi:cog',
							title: 'Open project dashboard',
							cls: 'button-ico-primary',
							href: resolve(`/dashboard/${project}/backend/`),
							disabled: false
						},
						{
							icon: 'mdi:delete-outline',
							title: 'Delete project',
							cls: 'button-ico-primary',
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
						}
					]}
					size="6"
					layout="layout-grid-low-6"
					class="w-full min-w-56"
					disabled={!init}
				/>
			{/if}
		{/snippet}
		{#snippet rowChildren({ row, definition, rowRender })}
			{#if editedProject == row.name}
				<td colspan={definition.length}>
					<table class="w-full">
						<tbody>
							<tr>{@render rowRender()}</tr>
						</tbody>
					</table>
					<div class="mt-low max-w-full overflow-x-auto">
						<ProjectEditor project={row.name} class="!m-0 min-w-0" />
					</div>
				</td>
			{:else}
				{@render rowRender()}
			{/if}
		{/snippet}
	</TableAutoCard>
</Card>

<style lang="postcss">
	@reference "../../../../app.css";

	:global(.projects-table tbody tr:has(> td[colspan]):hover) {
		background-color: transparent !important;
	}
</style>
