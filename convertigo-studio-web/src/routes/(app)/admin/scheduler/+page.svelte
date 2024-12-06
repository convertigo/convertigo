<script>
	import Scheduler from '$lib/admin/Scheduler.svelte';
	import Project from '$lib/common/Projects.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import { onDestroy } from 'svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import CheckState from '$lib/admin/components/CheckState.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import { capitalize } from '$lib/utils/service';
	import TestPlatform from '$lib/common/TestPlatform.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import ModalYesNo from '$lib/common/components/ModalYesNo.svelte';
	import CronWizard from '$lib/admin/components/CronWizard.svelte';

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
			jobName: '…',
			scheduleName: '…',
			cron: '0 * * * * ?',
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

	let modal, yesNo;
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
						{#if mode == 'ScheduledJob'}
							<PropertyType
								name="name"
								description="Name"
								value="{rowSelected.jobName ?? '…'}@{rowSelected.scheduleName ?? '…'}"
								readonly={true}
							/>
						{:else}
							<PropertyType
								name="name"
								description="Name"
								bind:value={rowSelected.name}
								originalValue={name}
							/>
						{/if}
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
					<div class="layout-y max-md:w-full !items-stretch">
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
					<button class="basic-button" disabled={!rowSelected.name}
						><span><Ico icon={jobTypes[mode].icon} size="btn" /></span><span>Save</span></button
					>
					<button type="button" onclick={close} class="cancel-button"
						><span><Ico icon="material-symbols-light:cancel-outline" size="btn" /></span><span
							>Cancel</span
						></button
					>
				</div>
			</form>
		</Card>
	{/snippet}
</ModalDynamic>

<div class="layout-y !items-stretch">
	{#each cards as { title, range, next, data }}
		<Card {title}>
			{#snippet cornerOption()}
				<ButtonsContainer>
					{#each Object.entries(jobTypes).slice(...range) as [mode, { name, icon }]}
						<button class="basic-button" onclick={(event) => open({ event, mode })}>
							<Ico {icon} />
							<p>New {name}</p>
						</button>
					{/each}
				</ButtonsContainer>
			{/snippet}

			<TableAutoCard
				definition={[
					{
						name: 'Actions',
						class: 'max-w-32',
						custom: true
					},
					{ name: 'Name', key: 'name' },
					{ name: 'Description', key: 'description' },
					{ name: 'Info', key: 'info', class: 'max-w-40 break-all' }
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
							<button
								class="basic-button"
								onclick={(event) => open({ event, mode: row.type, row })}
							>
								<Ico icon="mdi:edit-outline" />
							</button>
							<button
								class="delete-button"
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
							>
								<Ico icon="mingcute:delete-line" />
							</button>
							<!-- {#if next}
								<AutoPlaceholder {loading}
									><button
										class="violet-button"
										onclick={() => /*modalStore.trigger({
										title: 'Next triggers',
										body: `<div class="overflow-y-auto max-h-[30vh]">${row.next.join('</br>')}</div>`,
										type: 'alert',
										modalClasses: 'text-center overflow-y-scroll'
									})*/ {}}>{row.next[0]}</button
									></AutoPlaceholder
								>
							{/if} -->
						</div>
					{/if}
				{/snippet}
			</TableAutoCard>
		</Card>
	{/each}
</div>
