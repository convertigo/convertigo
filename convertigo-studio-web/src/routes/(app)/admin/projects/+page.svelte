<script>
	import { getAdminPageDocHref } from '#lib/admin/AdminDocumentation.svelte.js';
	import ActionBar from '#lib/admin/components/ActionBar.svelte';
	import Button from '#lib/admin/components/Button.svelte';
	import Card from '#lib/admin/components/Card.svelte';
	import CheckState from '#lib/admin/components/CheckState.svelte';
	import FileUploadField from '#lib/admin/components/FileUploadField.svelte';
	import ProjectEditor from '#lib/admin/components/ProjectEditor.svelte';
	import PropertyType from '#lib/admin/components/PropertyType.svelte';
	import ResponsiveButtons from '#lib/admin/components/ResponsiveButtons.svelte';
	import TableAutoCard from '#lib/admin/components/TableAutoCard.svelte';
	import InputGroup from '#lib/common/components/InputGroup.svelte';
	import ModalDynamic from '#lib/common/components/ModalDynamic.svelte';
	import Projects from '#lib/common/Projects.svelte.js';
	import { resolve } from '#lib/utils/route.js';
	import { checkArray } from '#lib/utils/service.js';
	import { browser } from '$app/env';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { getContext, onDestroy, tick } from 'svelte';
	import { persistedState } from 'svelte-persisted-state';

	let {
		projects,
		deploy,
		deployBundle,
		exportBundle,
		previewBundle,
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
	const projectsDocHref = getAdminPageDocHref('/admin/projects');
	let exportChoices = $state({});
	let selectingExport = $state(false);
	let selectedProjects = $state({});
	let bundleDependencies = $state('false');
	let dependencyCandidates = $state([]);
	let dependencyChoices = $state({});
	let bundlePreviewReady = $state(false);
	/** @type {{ projects: string[], excluded: string[] } | null} */
	let bundleExportReport = $state(null);
	let includedDependencies = $derived(
		dependencyCandidates
			.filter((project) => dependencyChoices[project.name] === 'true')
			.map((project) => project.name)
	);
	let excludedDependencies = $derived(
		dependencyCandidates
			.filter((project) => dependencyChoices[project.name] !== 'true')
			.map((project) => project.name)
	);

	const loadBundleDependencies = async (names) => {
		bundlePreviewReady = false;
		const preview = await previewBundle({
			projects: JSON.stringify(names),
			includeDependencies: 'true'
		});
		if (!preview) return;
		dependencyCandidates = preview.filter((project) => !names.includes(project.name));
		dependencyChoices = Object.fromEntries(
			dependencyCandidates.map((project) => [project.name, 'true'])
		);
		bundlePreviewReady = true;
	};
	let bundleDeploy = $state('false');
	/** @type {{success: boolean | string, project?: {name: string, version: string, status: string, warning?: string, detail?: string} | {name: string, version: string, status: string, warning?: string, detail?: string}[]} | null} */
	let deploymentReport = $state(null);
	let deploymentWarnings = $state([]);
	let selectedNames = $derived(
		projects
			.filter((project) => selectedProjects[project.name] === 'true')
			.map((project) => project.name)
	);

	const openBundleExport = async () => {
		if (!selectedNames.length) return;
		const options = await exportOptions(selectedNames[0]);
		exportChoices = Object.fromEntries(options.map((option) => [option.name, 'true']));
		bundleDependencies = 'false';
		dependencyCandidates = [];
		dependencyChoices = {};
		bundlePreviewReady = false;
		modalExport.open({ bundle: true, names: [...selectedNames], options });
	};

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
				replace: true,
				reset: false,
				refreshAll: false
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
	{#snippet children({ close, params: { options = [], project, bundle = false, names = [] } = {} })}
		<Card
			title={bundle ? `Exporting ${names.length} projects as a bundle` : `Exporting ${project}`}
		>
			<fieldset disabled={waiting} class="layout-y-stretch">
				{#if bundle}
					<CheckState
						bind:value={
							() => bundleDependencies,
							(value) => {
								bundleDependencies = value;
								if (value === 'true') void loadBundleDependencies(names);
							}
						}
					>
						Include declared dependencies
					</CheckState>
					{#if bundleDependencies === 'true'}
						{#if bundlePreviewReady}
							{#each dependencyCandidates as dependency (dependency.name)}
								<CheckState bind:value={dependencyChoices[dependency.name]}>
									{dependency.name}
									{dependency.version}
								</CheckState>
							{/each}
							<p role="status">
								{names.length + includedDependencies.length} projects to export. Excluded dependencies:
								{excludedDependencies.join(', ') || 'none'}.
							</p>
							{#if excludedDependencies.length}<p class="text-sm text-muted">
									Excluded dependencies must already be available on the destination server.
								</p>{/if}
						{:else}
							<p role="status">
								{waiting
									? 'Loading dependencies...'
									: 'Dependencies could not be listed. Disable and re-enable this option to retry.'}
							</p>
						{/if}
					{/if}
					<p class="text-sm text-muted">
						A ZIP bundle contains one CAR per project and a manifest with versions and dependency
						order.
					</p>
				{/if}
				{#each options as { name, display } (name)}
					<CheckState {name} bind:value={exportChoices[name]}>{display}</CheckState>
				{/each}
				<ActionBar>
					<Button
						icon={waiting ? 'mdi:sync' : 'mdi:export'}
						label={waiting ? 'Exporting...' : 'Export'}
						class="button-primary w-fit!"
						disabled={waiting || (bundle && bundleDependencies === 'true' && !bundlePreviewReady)}
						onclick={async () => {
							if (
								await (bundle
									? exportBundle({
											projects: JSON.stringify(names),
											includeDependencies: bundleDependencies,
											includeProjects: JSON.stringify(
												bundleDependencies === 'true' ? includedDependencies : []
											),
											excludeProjects: JSON.stringify(
												bundleDependencies === 'true' ? excludedDependencies : []
											),
											exportOptions: JSON.stringify(exportChoices)
										})
									: exportProject({
											projectName: project,
											exportOptions: JSON.stringify(exportChoices)
										}))
							) {
								if (bundle) {
									bundleExportReport = {
										projects: [
											...names,
											...(bundleDependencies === 'true' ? includedDependencies : [])
										],
										excluded: bundleDependencies === 'true' ? [...excludedDependencies] : []
									};
									selectingExport = false;
									selectedProjects = {};
								}
								close();
							}
						}}
					/>
					<Button
						icon="mdi:close-circle-outline"
						label="Cancel"
						class="button-secondary w-fit!"
						disabled={waiting}
						onclick={close}
					/>
				</ActionBar>
			</fieldset>
		</Card>
	{/snippet}
</ModalDynamic>

<ModalDynamic bind:this={modalDeployUpload}>
	<Card title="Drop or choose a .car/.zip file and Deploy">
		<form
			onsubmit={async (event) => {
				if (bundleDeploy === 'true') {
					deploymentReport = null;
					const result = await deployBundle(event);
					if (result?.admin?.deployment) {
						deploymentReport = result.admin.deployment;
						deploymentWarnings = checkArray(result.admin.warning);
						modalDeployUpload?.close();
					}
				} else {
					await deploy(event);
					modalDeployUpload?.close();
				}
			}}
		>
			<fieldset disabled={waiting} class="layout-y-stretch">
				<CheckState bind:value={bundleDeploy}>Deploy several projects or a bundle</CheckState>
				{#if bundleDeploy === 'true'}
					<p class="text-sm text-muted">
						Choose CAR/ZIP files or one distribution ZIP. The complete selection is checked first.
						Projects use the server's normal backup setting and automatic starts. All sources of a
						cyclic group are installed before loading it. A failure stops the remaining projects;
						already installed sources and deployed projects are kept.
					</p>
				{/if}
				{#key bundleDeploy}
					<FileUploadField
						name="file"
						maxFiles={bundleDeploy === 'true' ? 64 : 1}
						accept={{ 'application/zip': ['.car', '.zip'] }}
						required
						allowDrop
						dropIcon="mdi:application-outline"
						title={bundleDeploy === 'true'
							? 'Drop or choose .car/.zip files or a bundle'
							: 'Drop or choose a .car/.zip file'}
						hint={bundleDeploy === 'true'
							? 'Up to 64 files and 1 GiB in total, then press Deploy'
							: 'then press Deploy'}
					/>
				{/key}
				<CheckState name="bAssembleXsl" value="false">
					Assemble XSL files included in style sheets when deploying
				</CheckState>
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
					<project name>
						=<git or http URL>
							[:path=<optional subpath>][:branch=<optional branch>]</optional></optional>
						</git>
					</project>
				</p>
				<p>Or a Convertigo Archive HTTP(S) URL.</p>
				<PropertyType
					name="url"
					placeholder="<project name>=<git or http URL>[:path=<optional subpath>][:branch=<optional branch>]"
					required
				/>
				<ActionBar>
					<Button label="Import" icon="mdi:import" type="submit" class="button-primary w-fit!" />
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

<Card title="Projects" docHref={projectsDocHref}>
	{#snippet cornerOption()}
		<ResponsiveButtons
			buttons={selectingExport
				? [
						{
							icon: 'mdi:check-all',
							label: 'Select visible',
							cls: 'button-secondary',
							onclick: () => {
								for (const project of fprojects)
									if (project.name) selectedProjects[project.name] = 'true';
							}
						},
						{
							icon: 'mdi:export',
							label: `Export [${selectedNames.length}]`,
							cls: 'button-primary',
							disabled: !selectedNames.length,
							onclick: openBundleExport
						},
						{
							icon: 'mdi:close-circle-outline',
							label: 'Cancel',
							cls: 'button-secondary',
							onclick: () => {
								selectingExport = false;
								selectedProjects = {};
							}
						}
					]
				: [
						{
							icon: 'mdi:application-outline',
							value: 'deploy',
							cls: 'button-primary',
							label: 'Deploy project',
							onclick: () => {
								bundleDeploy = 'false';
								modalDeployUpload?.open({});
							}
						},
						{
							icon: 'mdi:import',
							value: 'export',
							cls: 'button-secondary',
							label: 'Import a Remote Project URL',
							onclick: modalDeployURL?.open
						},
						{
							icon: 'mdi:export',
							cls: 'button-secondary',
							label: 'Export projects',
							onclick: () => {
								selectingExport = true;
								selectedProjects = {};
							}
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
	{#each fprojects.filter((project) => project.migrationSaveError) as project (project.name)}
		<p role="alert" class="rounded-sm preset-tonal-warning p break-words">
			<strong>{project.name}</strong> is loaded, but its migrated version could not be saved:
			{project.migrationSaveError}
		</p>
	{/each}
	{#if bundleExportReport}
		<Card title="Bundle export result">
			<p role="status">Exported projects: {bundleExportReport.projects.join(', ')}.</p>
			<p>Excluded dependencies: {bundleExportReport.excluded.join(', ') || 'none'}.</p>
		</Card>
	{/if}
	{#if deploymentReport}
		<Card title="Bundle deployment result">
			<p role="status">
				{deploymentReport.success === true || deploymentReport.success === 'true'
					? 'All projects were deployed.'
					: 'Deployment stopped. Installed sources and deployed projects were kept.'}
			</p>
			{#each deploymentWarnings as warning, index (index)}<p class="text-sm text-warning-700-300">
					{typeof warning === 'string' ? warning : (warning['#text'] ?? '')}
				</p>{/each}
			<TableAutoCard
				definition={[
					{ name: 'Project', key: 'name' },
					{ name: 'Version', key: 'version' },
					{ name: 'Status', custom: true },
					{ name: 'Details', custom: true }
				]}
				data={checkArray(deploymentReport.project)}
			>
				{#snippet children({ row, def })}
					{#if def.name === 'Details'}
						{row.warning ?? row.detail ?? ''}
					{:else if def.name === 'Status'}
						{{
							deployed: 'Deployed',
							failed: 'Failed',
							installed: 'Installed, not finalized',
							not_attempted: 'Not attempted'
						}[row.status] ?? row.status}
					{/if}
				{/snippet}
			</TableAutoCard>
		</Card>
	{/if}
	<TableAutoCard
		definition={[
			...(selectingExport ? [{ name: 'Select', custom: true }] : []),
			{ name: 'Project', key: 'name', class: 'font-normal w-80', sortable: true },
			{ name: 'Comment', key: 'comment', sortable: true },
			{
				name: 'Version',
				key: 'version',
				class: 'break-words text-[12px] min-w-34',
				sortable: true
			},
			{ name: 'Exported', key: 'exported', class: 'text-[12px] min-w-34', sortable: true },
			{ name: 'Deployment', key: 'deployDate', class: 'text-[12px] min-w-34', sortable: true },
			{ name: 'Actions', custom: true, class: 'w-56' }
		]}
		data={fprojects}
		class="projects-table rounded-sm"
	>
		{#snippet children({ row: { name, undefined_symbols }, def })}
			{@const project = name ? name : '_'}
			{#if def?.name === 'Select'}
				<CheckState
					ariaLabel={`Select ${project} for export`}
					bind:value={
						() => selectedProjects[project] ?? 'false',
						(value) => (selectedProjects[project] = value)
					}
				>
					<span class="sr-only">Select {project} for export</span>
				</CheckState>
			{:else if def?.name == 'Actions'}
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

	:global(.projects-table.autocard td[data-label='Actions'] > div) {
		min-width: 0 !important;
	}
</style>
