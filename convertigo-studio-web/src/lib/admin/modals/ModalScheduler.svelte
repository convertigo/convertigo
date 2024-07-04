<script>
	import { getModalStore, ListBox, ListBoxItem } from '@skeletonlabs/skeleton';
	import { call, capitalize } from '$lib/utils/service';
	import Card from '../components/Card.svelte';
	import CronWizard from '../components/CronWizard.svelte';
	import ModalButtons from '../components/ModalButtons.svelte';
	import { projectsCheck, projectsStore } from '../stores/projectsStore';
	import { onMount } from 'svelte';
	import { jobsStore, schedulerList, schedulesStore } from '../stores/schedulerStore';
	import CheckState from '../components/CheckState.svelte';

	export let parent;
	const modalStore = getModalStore();
	const { mode, row } = $modalStore[0].meta ?? {};
	let binds = {
		name: row?.['@_name'] ?? '',
		description: row?.['@_description'] ?? '',
		enabled: row?.['@_enabled'] == 'true' ?? false,
		writeOutput: row?.['@_writeOutput'] == 'true' ?? false,
		project: row?.['@_project'],
		connector: row?.['@_connector'],
		transaction: row?.['@_transaction'],
		sequence: row?.['@_sequence'],
		parallelJob: row?.['@_parallelJob'] ?? '1',
		cron: row?.['@_cron'] ?? '0 0 0 * * ?',
		jobsname: row?.job_group_member ?? [],
		jobName: row?.['@_jobName'] ?? '',
		scheduleName: row?.['@_scheduleName'] ?? '',
		parameter: row?.parameter ?? {}
	};

	let selected = {
		project: /** @type any */ (null),
		connector: /** @type any */ (null),
		transaction: /** @type any */ (null),
		sequence: /** @type any */ (null)
	};

	onMount(async () => {
		await projectsCheck();
		await handleChange('project', binds.project ?? $projectsStore[0]['@_name']);
	});

	async function handleChange(type, name) {
		if (type == 'project') {
			handleChange('connector', binds.connector);
			handleChange('sequence', binds.sequence);
		} else if (type == 'connector') {
			selected.connector =
				selected.project?.connector?.[name] ?? Object.values(selected.project?.connector ?? {})[0];
			handleChange('transaction', binds.transaction);
		} else if (type == 'transaction') {
			selected.transaction =
				selected.connector?.transaction?.[name] ??
				Object.values(selected.connector?.transaction ?? {})[0];
		} else if (type == 'sequence') {
			selected.sequence =
				selected.project?.sequence?.[name] ?? Object.values(selected.project?.sequence ?? {})[0];
		}
		binds[type] = selected[type]['@_name'];
	}

	async function createScheduledElements(e) {
		await call('scheduler.CreateScheduledElements', new FormData(e.target));
		await schedulerList();
		modalStore.close();
	}
</script>

<Card title={$modalStore[0]?.title} class="max-w-full">
	<form on:submit|preventDefault={createScheduledElements}>
		<input type="hidden" name="type" value="schedulerNew{mode}" />
		<div class="flex flew-row flex-wrap gap-5 justify-stretch">
			<div class="flex flex-col gap-5">
				<label class="border-common">
					{#if row}
						<input type="hidden" name="exname" value={row['@_name']} />
						<input type="hidden" name="edit" value={true} />
					{/if}
					<p class="label-common">Name</p>
					{#if mode == 'ScheduledJob'}
						<input
							name="name"
							value="{binds.jobName ?? '…'}@{binds.scheduleName ?? '…'}"
							class="input-common"
							readonly={true}
						/>
					{:else}
						<input name="name" bind:value={binds.name} class="input-common" />
					{/if}
				</label>
				<label class="border-common">
					<p class="label-common">Description</p>
					<input name="description" bind:value={binds.description} class="input-common" />
				</label>
				<CheckState name="enabled" bind:checked={binds.enabled} size="md">Enable</CheckState>
				{#if mode.endsWith('ConvertigoJob')}
					<CheckState name="writeOutput" bind:checked={binds.writeOutput} size="md"
						>Write Output</CheckState
					>
				{/if}
			</div>
			<div class="flex flex-col gap-5">
				{#if mode.endsWith('ConvertigoJob')}
					{@const types = [
						{
							name: 'project',
							obj: $projectsStore.reduce((acc, val) => {
								acc[val['@_name']] = val;
								return acc;
							}, {})
						},
						{
							name: 'connector',
							obj: selected.project?.connector ?? {},
							starts: 'Tr'
						},
						{
							name: 'transaction',
							obj: selected.connector?.transaction ?? {},
							starts: 'Tr'
						},
						{
							name: 'sequence',
							obj: selected.project?.sequence ?? {},
							starts: 'Se'
						}
					].filter((type) => !type.starts || mode.startsWith(type.starts))}
					{#each types as { name, obj }}
						<div class="border-common">
							<p class="label-common w-full">{capitalize(name)}</p>
							<select
								{name}
								class="input-common"
								on:change={(/** @type any */ e) => handleChange(name, e?.target?.value)}
								bind:value={binds[name]}
							>
								{#each Object.keys(obj) as k}
									<option>{k}</option>
								{/each}
							</select>
						</div>
					{/each}
					<label class="border-common">
						<p class="label-common">Context</p>
						<input name="context" value="" class="input-common" />
					</label>
				{:else if mode == 'JobGroupJob'}
					<p class="label-common">Parallel Job execution</p>

					<input
						class="input"
						type="number"
						name="parallelJob"
						bind:value={binds.parallelJob}
						on:change={(/** @type any */ e) => (e.target.value = Math.max(e.target.value, 1))}
					/>

					<p class="font-bold">Select jobs</p>

					<ListBox
						rounded="rounded-container-token"
						multiple={true}
						class="max-h-52 overflow-y-auto"
					>
						{#each $jobsStore as job}
							{#if job['@_name'] != binds.name}
								<ListBoxItem
									class=""
									active="bg-success-400-500-token"
									bind:group={binds.jobsname}
									name="jobsname"
									value={job['@_name']}>{job['@_name']}</ListBoxItem
								>
							{/if}
						{/each}
					</ListBox>
				{:else if mode == 'ScheduleCron'}
					<label class="border-common">
						<p class="label-common">Cron Expression</p>
						<input name="cron" bind:value={binds.cron} class="input-common" />
					</label>
					<CronWizard bind:cronExpression={binds.cron} />
				{:else if mode == 'ScheduledJob'}
					{@const def = [
						{ label: 'Job', name: 'jobName', store: $jobsStore },
						{ label: 'Schedule', name: 'scheduleName', store: $schedulesStore }
					]}
					<p class="label-common">Association</p>

					<div class="flex flex-row flew-wrap">
						{#each def as { label, name, store }}
							<div>
								<p class="font-bold">{label}</p>
								<select {name} class="select" size="10" bind:value={binds[name]}>
									{#each store as item}
										<option>{item['@_name']}</option>
									{/each}
								</select>
							</div>
						{/each}
					</div>
				{/if}
			</div>
			{#if mode.endsWith('ConvertigoJob')}
				{@const requestable = mode.startsWith('Tr') ? selected.transaction : selected.sequence}
				{#if requestable?.variable}
					<div class="flex flex-col gap-5">
						<p class="font-bold">Variables</p>
						<div class="overflow-y-auto max-h-80">
							{#each Object.keys(requestable?.variable) as name}
								<label class="border-common">
									<p class="label-common">{name}</p>
									<input
										class="input-common"
										type="text"
										{name}
										value={binds.parameter[name]?.value ?? ''}
									/>
								</label>
							{/each}
						</div>
					</div>
				{/if}
			{/if}
		</div>
		<ModalButtons />
	</form>
</Card>
