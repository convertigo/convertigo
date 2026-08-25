<script>
	import { getAdminPageDocHref } from '$lib/admin/AdminDocumentation.svelte';
	import ActionBar from '$lib/admin/components/ActionBar.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import CheckState from '$lib/admin/components/CheckState.svelte';
	import CronWizard from '$lib/admin/components/CronWizard.svelte';
	import FileUploadField from '$lib/admin/components/FileUploadField.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import RequestableParameters from '$lib/admin/components/RequestableParameters.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import Scheduler from '$lib/admin/Scheduler.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import Project from '$lib/common/Projects.svelte';
	import TestPlatform from '$lib/common/TestPlatform.svelte';
	import Time from '$lib/common/Time.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import { capitalize, checkArray } from '$lib/utils/service';
	import { getContext, onDestroy } from 'svelte';
	import { slide } from 'svelte/transition';

	let {
		jobs,
		schedules,
		scheduled,
		configure,
		remove,
		importScheduler,
		exportScheduler,
		selectForExport,
		waiting,
		init
	} = $derived(Scheduler);
	let { projects } = $derived(Project);
	const schedulerDocHref = getAdminPageDocHref('/admin/scheduler');

	onDestroy(() => {
		Scheduler.stop();
		Project.stop();
	});

	const jobTypes = {
		SequenceConvertigoJob: { name: 'Job Sequence', icon: 'mdi:api' },
		TransactionConvertigoJob: { name: 'Job Transaction', icon: 'mdi:database' },
		JobGroupJob: { name: 'Jobs Group', icon: 'mdi:layers-outline' },
		ScheduleCron: { name: 'Cron', icon: 'mdi:calendar-clock' },
		ScheduleRunNow: { name: 'Run Now', icon: 'mdi:play-circle-outline' },
		ScheduledJob: { name: 'Scheduled Job', icon: 'mdi:invoice-scheduled-outline' }
	};

	function open({ event, mode, row = undefined }) {
		rowSelected = {
			name: '',
			project: '',
			sequence: '',
			connector: '',
			transaction: '',
			cron: '0 * * * * ?',
			...(mode == 'ScheduledJob' ? { jobName: '…', scheduleName: '…' } : {}),
			...(row ?? {})
		};
		modal.open({ event, mode, row });
	}

	const cards = $derived([
		{
			title: 'Jobs',
			category: 'jobs',
			range: [0, 3],
			data: jobs
		},
		{
			title: 'Schedules',
			category: 'schedules',
			range: [3, 5],
			next: true,
			data: schedules
		},
		{
			title: 'Scheduled jobs',
			category: 'scheduledJobs',
			range: [5, 6],
			data: scheduled
		}
	]);

	let modal, nextCron;
	let modalImport = $state();
	let actionImport = $state('on');
	let exporting = $state(false);
	/*** @type {any} */
	let rowSelected = $state(null);
	/*** @type {any} */
	let project = $state({});

	let modalYesNo = getContext('modalYesNo');
	let exportRows = $derived(
		cards.flatMap(({ category, data }) =>
			data.filter(({ name }) => name != null).map((row) => ({ category, row }))
		)
	);
	let selectedExportCount = $derived(exportRows.filter(({ row }) => row.export).length);
	let allExported = $derived(exportRows.length > 0 && selectedExportCount == exportRows.length);

	function selectAllForExport(selected) {
		for (const { category, row } of exportRows) {
			selectForExport(category, row, selected);
		}
	}

	function selectByName(values, name) {
		const list = checkArray(values);
		return list.find((item) => item.name == name) || (!name ? list[0] : undefined);
	}

	function comboItems(values) {
		return checkArray(values)
			.filter(({ name }) => name != null)
			.map(({ name }) => ({ value: name, text: name }));
	}

	function resetChildren(name, value) {
		rowSelected[name] = value;
		if (name == 'project') {
			rowSelected.connector = '';
			rowSelected.transaction = '';
			rowSelected.sequence = '';
		} else if (name == 'connector') {
			rowSelected.transaction = '';
		}
	}

	let sequence = $derived(selectByName(project?.sequence, rowSelected?.sequence));
	let connector = $derived(selectByName(project?.connector, rowSelected?.connector));
	let transaction = $derived(selectByName(connector?.transaction, rowSelected?.transaction));

	let requestable = $derived({ project, connector, transaction, sequence });

	$effect(() => {
		if (!rowSelected) {
			return;
		}

		const prj = selectByName(projects, rowSelected.project);
		project = prj?.name ? TestPlatform(prj?.name) : {};
		if (rowSelected?.jobName || rowSelected?.scheduleName) {
			rowSelected.name = `${rowSelected.jobName}@${rowSelected.scheduleName}`;
		}
		for (const k of Object.keys(requestable)) {
			if (requestable[k]?.name) {
				rowSelected[k] = requestable[k]?.name;
			}
		}
	});

	async function onsubmit(e) {
		if (await configure(e)) {
			modal.close();
		}
	}

	/**
	 * Convert scheduler "next" textual date to a sortable timestamp.
	 * Expected format starts with dd/mm/yyyy hh:mm:ss.
	 * @param {string | undefined} nextValue
	 * @returns {number | ''}
	 */
	function toNextSortValue(nextValue) {
		const value = (nextValue ?? '').trim();
		if (!value || value.toLowerCase() === 'n/a') return '';
		const match = value.match(/^(\d{2})\/(\d{2})\/(\d{4})\s+(\d{2}):(\d{2}):(\d{2})/);
		if (!match) return '';
		const [, dd, mm, yyyy, hh, mi, ss] = match;
		const date = new Date(
			Number(yyyy),
			Number(mm) - 1,
			Number(dd),
			Number(hh),
			Number(mi),
			Number(ss)
		);
		return Number.isNaN(date.getTime()) ? '' : date.getTime();
	}
</script>

<ModalDynamic bind:this={modal}>
	{#snippet children({ close, params: { mode, row } })}
		{@const {
			name,
			description,
			enabled,
			writeOutput,
			context,
			parallelJob = 1,
			jobsname = [],
			cron
		} = row ?? {}}
		<Card title="{row ? 'Edit' : 'New'} {jobTypes[mode].name}" class="max-w-full">
			<form {onsubmit} class="layout-y-stretch">
				<input type="hidden" name="type" value="schedulerNew{mode}" />
				{#if row}
					<input type="hidden" name="exname" value={name} />
					<input type="hidden" name="edit" value={true} />
				{/if}
				<div class="layout-x max-md:flex-wrap">
					<div class="layout-y max-md:w-full">
						<PropertyType
							name="name"
							description="Name"
							bind:value={rowSelected.name}
							originalValue={mode == 'ScheduledJob' ? undefined : name}
							readonly={mode == 'ScheduledJob'}
						/>
						<PropertyType
							name="description"
							description="Description"
							value={description}
							originalValue={description}
						/>
						<PropertyType
							type="boolean"
							name="enabled"
							description="Enabled"
							value={enabled}
							originalValue={enabled}
						/>
						{#if mode.endsWith('ConvertigoJob')}
							<PropertyType
								type="boolean"
								name="writeOutput"
								description="Write Output"
								value={writeOutput}
								originalValue={writeOutput}
							/>
						{/if}
					</div>
					<div class="layout-y-stretch max-md:w-full">
						{#if mode.endsWith('ConvertigoJob')}
							{@const types = [
								{
									name: 'project',
									values: projects
								},
								{
									name: 'connector',
									values: project?.connector,
									starts: 'Tr'
								},
								{
									name: 'transaction',
									values: connector?.transaction,
									starts: 'Tr'
								},
								{
									name: 'sequence',
									values: project?.sequence,
									starts: 'Se'
								}
							].filter((type) => !type.starts || mode.startsWith(type.starts))}
							{#each types as type (type.name)}
								<PropertyType
									type="combo"
									name={type.name}
									description={capitalize(type.name)}
									bind:value={rowSelected[type.name]}
									originalValue={row?.[type.name]}
									item={comboItems(type.values)}
									multiple={false}
									onchange={(event) => resetChildren(type.name, event.target.value)}
								/>
							{/each}
							<PropertyType
								name="context"
								description="Context"
								value={context}
								originalValue={context}
							/>
						{:else if mode == 'JobGroupJob'}
							<p class="label-common">Parallel Job execution</p>

							<PropertyType
								type="number"
								name="parallelJob"
								value={parallelJob}
								min="1"
								max="100"
							/>

							<PropertyType
								type="combo"
								name="jobsname"
								description="Select jobs"
								orientation="vertical"
								value={jobsname}
								originalValue={jobsname}
								multiple
								size="6"
								item={jobs.map(({ name }) => ({ value: name, text: name }))}
							/>
						{:else if mode == 'ScheduleCron'}
							<PropertyType
								name="cron"
								description="Cron Expression"
								bind:value={rowSelected.cron}
								originalValue={cron}
							/>
							<CronWizard bind:cronExpression={rowSelected.cron} />
						{:else if mode == 'ScheduledJob'}
							{@const def = [
								{ label: 'Job', name: 'jobName', store: jobs },
								{ label: 'Schedule', name: 'scheduleName', store: schedules }
							]}
							<div class="flew-wrap layout-x">
								{#each def as { label, name, store } (name)}
									<PropertyType
										type="segment"
										{name}
										description={label}
										orientation="vertical"
										bind:value={rowSelected[name]}
										originalValue={row?.[name]}
										item={store.map(({ name }) => ({ value: name, text: name }))}
									/>
								{/each}
							</div>
						{/if}
					</div>
					{#if mode.endsWith('ConvertigoJob')}
						{@const selectedRequestable = mode.startsWith('Tr') ? transaction : sequence}
						{#if selectedRequestable}
							<RequestableParameters
								requestable={selectedRequestable}
								savedParameters={rowSelected.parameterMap ?? {}}
								class="min-w-80 flex-1"
							/>
						{/if}
					{/if}
				</div>
				<ActionBar>
					<Button
						label="Save"
						type="submit"
						icon={jobTypes[mode].icon}
						class="button-primary w-fit!"
						disabled={!rowSelected.name ||
							(mode == 'ScheduledJob' &&
								(rowSelected.jobName == '…' || rowSelected.scheduleName == '…'))}
					/>
					<Button
						label="Cancel"
						icon="mdi:close-circle-outline"
						class="button-secondary w-fit!"
						onclick={close}
					/>
				</ActionBar>
			</form>
		</Card>
	{/snippet}
</ModalDynamic>

<ModalDynamic bind:this={nextCron}>
	{#snippet children({
		close,
		params: {
			row: { next }
		}
	})}
		<Card title="Next Schedules" class="max-w-xs">
			<p class="break-break-word"><b>Now</b> {Time.server}</p>
			<ul>
				{#each next as n, i (i)}
					<li>{n}</li>
				{/each}
			</ul>
			<ActionBar>
				<Button
					label="Close"
					icon="mdi:close-circle-outline"
					class="button-primary"
					onclick={close}
				/>
			</ActionBar>
		</Card>
	{/snippet}
</ModalDynamic>

<ModalDynamic bind:this={modalImport}>
	<Card title="Drop or choose a scheduler XML file and Import">
		<form
			onsubmit={async (event) => {
				if (await importScheduler(event)) {
					modalImport.close();
				}
			}}
		>
			<fieldset class="layout-y-stretch" disabled={waiting}>
				<FileUploadField
					name="file"
					accept={{ 'application/xml': ['.xml'], 'text/xml': ['.xml'] }}
					required
					allowDrop
					dropIcon="mdi:import"
					title="Drop or choose a scheduler XML file"
					hint="then press Import"
				/>
				<div>
					Import policy
					<PropertyType
						type="segment"
						name="action-import"
						item={[
							{ text: 'Replace scheduler', value: 'clear-import' },
							{ text: 'Merge scheduler', value: 'on' }
						]}
						bind:value={actionImport}
						orientation="vertical"
					/>
				</div>
				{#if actionImport == 'on'}
					<div transition:slide>
						In case of name conflict, priority
						<PropertyType
							type="segment"
							name="priority"
							item={[
								{ text: 'Server', value: 'priority-server' },
								{ text: 'File', value: 'priority-import' }
							]}
							value="priority-import"
							orientation="vertical"
						/>
					</div>
					<div>Elements that only exist on the server will be kept.</div>
				{:else}
					<div>All current scheduler elements will be replaced by the file content.</div>
				{/if}
				<div>The current scheduler.xml file will be saved aside as a dated backup.</div>
				<ActionBar>
					<Button label="Import" icon="mdi:import" type="submit" class="button-primary w-fit!" />
					<Button
						label="Cancel"
						icon="mdi:close-circle-outline"
						class="button-secondary w-fit!"
						onclick={modalImport?.close}
					/>
				</ActionBar>
			</fieldset>
		</form>
	</Card>
</ModalDynamic>

<div class="layout-y-stretch">
	{#each cards as { title, category, range, next, data, size = "6" }, i (title)}
		<Card {title} docHref={i == 0 ? schedulerDocHref : undefined}>
			{#snippet cornerOption()}
				<ResponsiveButtons
					class="max-w-4xl"
					buttons={[
						...Object.entries(jobTypes)
							.slice(...range)
							.map(([mode, { name, icon }]) => ({
								label: `New ${name}`,
								icon,
								cls: 'button-primary',
								hidden: exporting,
								onclick: (event) => open({ event, mode })
							})),
						...(i == 0
							? [
									{
										label: 'Import',
										icon: 'mdi:import',
										tooltip: 'Import a scheduler XML file',
										cls: 'button-secondary',
										hidden: exporting,
										onclick: modalImport?.open
									},
									{
										label: 'Select All',
										icon: 'mdi:check-all',
										tooltip: 'Select all scheduler elements for export',
										cls: 'button-secondary',
										hidden: !exporting || allExported,
										onclick: () => selectAllForExport(true)
									},
									{
										label: 'Unselect All',
										icon: 'mdi:check-all',
										tooltip: 'Clear scheduler selection for export',
										cls: 'button-secondary',
										hidden: !exporting || selectedExportCount == 0,
										onclick: () => selectAllForExport(false)
									},
									{
										label: 'Export',
										icon: 'mdi:export',
										tooltip: 'Choose scheduler elements to export',
										cls: 'button-secondary',
										hidden: exporting,
										onclick: () => (exporting = true)
									},
									{
										label: `Export [${selectedExportCount}]`,
										icon: 'mdi:export',
										tooltip: 'Export selected scheduler elements and their dependencies',
										cls: 'button-primary',
										hidden: !exporting,
										disabled: selectedExportCount == 0,
										onclick: exportScheduler
									},
									{
										label: 'Cancel',
										icon: 'mdi:close-circle-outline',
										tooltip: 'Exit export mode without exporting',
										cls: 'button-secondary',
										hidden: !exporting,
										onclick: () => (exporting = false)
									}
								]
							: [])
					]}
					disabled={!init || waiting}
				/>
			{/snippet}

			{@const tableData =
				next && !exporting
					? data.map((row) => ({ ...row, nextSort: toNextSortValue(row.next?.[0]) }))
					: data}
			<TableAutoCard
				class="text-left"
				definition={[
					{ name: 'Name', key: 'name', class: 'w-50' },
					{ name: 'Description', key: 'description', class: 'w-60 break-all' },
					{
						name: 'Next',
						key: 'nextSort',
						sortable: true,
						custom: true,
						class: 'w-32'
					},
					{ name: 'Info', key: 'info', class: 'min-w-32 break-all' },
					{
						name: 'Actions',
						class: 'w-32',
						custom: true
					}
				].filter((elt) => next || elt.name != 'Next')}
				data={tableData}
			>
				{#snippet children({ row, def })}
					{#if def.name == 'Actions'}
						<fieldset class="layout-x-low" disabled={!init}>
							{#if exporting}
								<PropertyType
									values={[false, true]}
									type="boolean"
									name="export"
									bind:value={
										() => row.export, (selected) => selectForExport(category, row, selected)
									}
								/>
							{:else}
								<CheckState
									name={row.name}
									title={row.enabled == 'true' || row.enabled === true
										? 'Disable schedule'
										: 'Enable schedule'}
									aria-label={row.enabled == 'true' || row.enabled === true
										? 'Disable schedule'
										: 'Enable schedule'}
									bind:value={
										() => row.enabled,
										(enabled) =>
											configure({
												...row,
												enabled,
												edit: true,
												exname: row.name,
												type: `schedulerNew${row.type}`
											})
									}
									disabled={!init}
								/>
								<Button
									class="button-ico-primary"
									{size}
									icon="mdi:edit-outline"
									title="Edit schedule"
									onclick={(event) => open({ event, mode: row.type, row })}
								/>
								<Button
									class="button-ico-primary"
									{size}
									icon="mdi:delete-outline"
									title="Delete schedule"
									onclick={async (event) => {
										if (
											await modalYesNo.open({
												event,
												title: 'Please Confirm',
												message: `Are you sure you want to delete this ${title.slice(0, -1)} ?`
											})
										) {
											remove(row.name, row.type);
										}
									}}
								/>
							{/if}
						</fieldset>
					{:else if row.next?.length > 1}
						<Button
							label={`${row.next?.[0]} …`}
							class="button-ico-primary w-fit! p-none"
							icon="mdi:calendar-clock"
							{size}
							onclick={(event) => nextCron.open({ event, row })}
						/>
					{:else}
						<AutoPlaceholder loading={row.next == null}>{row.next?.[0]}</AutoPlaceholder>
					{/if}
				{/snippet}
			</TableAutoCard>
		</Card>
	{/each}
</div>
