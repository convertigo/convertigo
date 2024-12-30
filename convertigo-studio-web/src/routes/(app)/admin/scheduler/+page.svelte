<script>
	import Scheduler from '$lib/admin/Scheduler.svelte';
	import Project from '$lib/common/Projects.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import { onDestroy } from 'svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import CheckState from '$lib/admin/components/CheckState.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import { capitalize } from '$lib/utils/service';
	import TestPlatform from '$lib/common/TestPlatform.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import ModalYesNo from '$lib/common/components/ModalYesNo.svelte';
	import CronWizard from '$lib/admin/components/CronWizard.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Time from '$lib/common/Time.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';

	let { jobs, schedules, scheduled, configure, remove } = $derived(Scheduler);
	let { projects } = $derived(Project);

	onDestroy(() => {
		Scheduler.stop();
		Project.stop();
	});

	const jobTypes = {
		SequenceConvertigoJob: { name: 'Job Sequence', icon: 'material-symbols:api-rounded' },
		TransactionConvertigoJob: { name: 'Job Transaction', icon: 'carbon:data-regular' },
		JobGroupJob: { name: 'Jobs Group', icon: 'uim:layer-group' },
		ScheduleCron: { name: 'Cron', icon: 'eos-icons:cronjob' },
		ScheduleRunNow: { name: 'Run Now', icon: 'codicon:run-all' },
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
			range: [0, 3],
			data: jobs
		},
		{
			title: 'Schedules',
			range: [3, 5],
			next: true,
			data: schedules
		},
		{
			title: 'Scheduled jobs',
			range: [5, 6],
			data: scheduled
		}
	]);

	let modal, yesNo, nextCron;
	/*** @type {any} */
	let selected = $state({});
	/*** @type {any} */
	let rowSelected = $state(null);
	/*** @type {any} */
	let project = $state({});

	let sequence = $derived(
		project?.sequence?.find((s) => s.name == rowSelected.sequence) || project?.sequence?.[0]
	);
	let connector = $derived(
		project?.connector?.find((c) => c.name == rowSelected.connector) || project?.connector?.[0]
	);
	let transaction = $derived(
		connector?.transaction?.find((t) => t.name == rowSelected.transaction) ||
			connector?.transaction?.[0]
	);

	let requestable = $derived({ project, connector, transaction, sequence });

	$effect(() => {
		if (rowSelected) {
			const prj = projects?.find((p) => p.name == rowSelected.project) || projects?.[0];
			project = prj?.name ? TestPlatform(prj?.name) : {};
			if (rowSelected?.jobName || rowSelected?.scheduleName) {
				rowSelected.name = `${rowSelected.jobName}@${rowSelected.scheduleName}`;
			}
		}
		for (const k of Object.keys(requestable)) {
			if (requestable[k]?.name) {
				rowSelected[k] = requestable[k]?.name;
			}
		}
	});

	async function onsubmit(e) {
		await configure(e);
		modal.close();
	}
</script>

<ModalYesNo bind:this={yesNo} />
<ModalDynamic bind:this={modal}>
	{#snippet children({ close, params: { mode, row } })}
		{@const {
			name,
			description,
			enabled,
			writeOutput,
			context,
			parallelJob = 1,
			jobsname,
			cron
		} = row ?? {}}
		<Card title="{row ? 'Edit' : 'New'} {jobTypes[mode].name}" class="max-w-full">
			<form {onsubmit} class="layout-y">
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
							{#each types as { name, values = [] }}
								<PropertyType
									type="combo"
									{name}
									description={capitalize(name)}
									bind:value={rowSelected[name]}
									originalValue={row?.[name]}
									item={values.map(({ name }) => ({ value: name, text: name }))}
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
							<div class="layout-x flew-wrap">
								{#each def as { label, name, store }}
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
						{@const requestable = mode.startsWith('Tr') ? selected.transaction : selected.sequence}
						{#if requestable?.variable}
							<div class="layout-y">
								<p class="font-bold">Variables</p>
								{#each Object.keys(requestable?.variable) as name}
									<label class="border-common">
										<p class="label-common">{name}</p>
										<input
											class="input-common"
											type="text"
											{name}
											value={row.parameter[name]?.value ?? ''}
										/>
									</label>
								{/each}
							</div>
						{/if}
					{/if}
				</div>
				<div class="w-full layout-x justify-end">
					<Button
						label="Save"
						icon={jobTypes[mode].icon}
						class="basic-button"
						disabled={!rowSelected.name ||
							(mode == 'ScheduledJob' &&
								(rowSelected.jobName == '…' || rowSelected.scheduleName == '…'))}
					/>
					<Button
						label="Cancel"
						icon="material-symbols-light:cancel-outline"
						class="cancel-button"
						onclick={close}
					/>
				</div>
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
			<p class="break-words"><b>Now</b> {Time.server}</p>
			<ul>
				{#each next as n}
					<li>{n}</li>
				{/each}
			</ul>
			<div class="w-full layout-x justify-end">
				<Button
					label="Close"
					icon="material-symbols-light:cancel-outline"
					class="cancel-button"
					onclick={close}
				/>
			</div>
		</Card>
	{/snippet}
</ModalDynamic>
<div class="layout-y-stretch">
	{#each cards as { title, range, next, data, size = "4" }}
		<Card {title}>
			{#snippet cornerOption()}
				<ResponsiveButtons
					class="max-w-2xl"
					buttons={Object.entries(jobTypes)
						.slice(...range)
						.map(([mode, { name, icon }]) => ({
							label: `New ${name}`,
							icon,
							cls: 'basic-button',
							onclick: (event) => open({ event, mode })
						}))}
				/>
			{/snippet}

			<TableAutoCard
				definition={[
					{
						name: 'Actions',
						class: 'w-32',
						custom: true
					},
					{ name: 'Name', key: 'name' },
					{ name: 'Description', key: 'description', class: 'min-w-32 break-all' },
					{
						name: 'Next',
						custom: true
					},
					{ name: 'Info', key: 'info', class: 'min-w-32 break-all' }
				].filter((elt) => next || elt.name != 'Next')}
				{data}
			>
				{#snippet children({ row, def })}
					{#if def.name == 'Actions'}
						<div class="layout-x-low">
							<CheckState
								name={row.name}
								value={row.enabled}
								onchange={(e) => {
									configure({
										...row,
										enabled: e.target.value,
										edit: true,
										exname: row.name,
										type: `schedulerNew${row.type}`
									});
								}}
							/>
							<Button
								class="basic-button"
								{size}
								icon="mdi:edit-outline"
								onclick={(event) => open({ event, mode: row.type, row })}
							/>
							<Button
								class="delete-button"
								{size}
								icon="mingcute:delete-line"
								onclick={async (event) => {
									if (
										await yesNo.open({
											event,
											title: 'Please Confirm',
											message: `Are you sure you want to delete this ${title.slice(0, -1)} ?`
										})
									) {
										remove(row.name, row.type);
									}
								}}
							/>
						</div>
					{:else if row.next?.length > 1}
						<Button
							label={`${row.next?.[0]} …`}
							class="yellow-button"
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
