<script>
	import { RadioGroup, RadioItem, getModalStore } from '@skeletonlabs/skeleton';
	import { call } from '$lib/utils/service';
	import Card from '../components/Card.svelte';
	import CronWizard from '../components/CronWizard2.svelte';
	import { onMount } from 'svelte';
	import { jobsStore, schedulerList, schedulesStore } from '../stores/schedulerStore';
	import { projectsCheck, projectsStore } from '../stores/projectsStore';
	import {
		getProjectTestPlatform,
		connectorsStore,
		transactionsStore,
		sequencesStore
	} from '../stores/testPlatformStore';
	import ResponsiveContainer from '../components/ResponsiveContainer.svelte';
	import SchedulerForm from '../components/SchedulerForm.svelte';
	import ModalButtons from '../components/ModalButtons.svelte';

	const modalStore = getModalStore();

	export let parent;
	let selectedProjectId;
	let projectConnector;
	let projectTransaction;
	let projectSequence;
	let selectedJob;
	let jobCount = 1;
	let cronExpression;

	onMount(async () => {
		await schedulerList();
		await projectsCheck();
		selectedProjectId = $projectsStore[0]['@_name'];
		await getProjectTestPlatform(selectedProjectId);
		selectedJob = $jobsStore[0]['@_name'];
	});

	function handleProjectChange(event) {
		selectedProjectId = event.target.value;
		getProjectTestPlatform(selectedProjectId);

		console.log('selectedProject', selectedProjectId);
	}
	function getType(mode) {
		switch (mode) {
			case 'TransactionConvertigoJob':
				return 'schedulerNewTransactionConvertigoJob';
			case 'SequenceConvertigoJob':
				return 'schedulerNewSequenceConvertigoJob';
			case 'schedulerNewJobGroupJob':
				return 'schedulerNewJobGroupJob';
			case 'schedulerNewScheduleCron':
				return 'schedulerNewScheduleCron';
			case 'schedulerNewScheduleRunNow':
				return 'schedulerNewScheduleRunNow';
			case 'schedulerNewScheduledJob':
				return 'schedulerNewScheduledJob';
			default:
				return 'schedulerUnknownType';
		}
	}

	async function createScheduledElements(e) {
		e.preventDefault();
		const fd = new FormData(e.target);

		const mode = $modalStore[0]?.meta?.mode || 'Unknown';
		fd.append('type', getType(mode));
		try {
			await call('scheduler.CreateScheduledElements', fd);
			await schedulerList();
		} catch (err) {
			console.error('Error creating scheduled elements:', err);
		} finally {
			modalStore.close();
		}
	}

	function adjustJobCount(change) {
		let newCount = jobCount + change;
		if (newCount > 0) {
			jobCount = newCount;
		}
	}
</script>

{#if $modalStore[0]?.meta?.mode === 'TransactionConvertigoJob' || $modalStore[0]?.meta?.mode === 'SequenceConvertigoJob'}
	<Card title={$modalStore[0].title} class="w-[60vw]">
		<form class="p-5" on:submit={createScheduledElements}>
			<div class="grid grid-cols-2 gap-10">
				<div class="col-span-1 flex flex-col">
					<SchedulerForm />
				</div>

				<div class="col-span-1 flex flex-col gap-5">
					<label class="border-common">
						<p class="label-common">Context</p>
						<input name="context" value="" class="input-common" />
					</label>
					<div class="border-common">
						<p class="label-common w-full">Project</p>
						<select
							name="project"
							bind:value={selectedProjectId}
							class="input-common"
							on:change={handleProjectChange}
						>
							{#each $projectsStore as project}
								<option value={project['@_name']} selected={selectedProjectId}
									>{project['@_name']}</option
								>
							{/each}
						</select>
					</div>
					{#if $modalStore[0]?.meta?.mode === 'TransactionConvertigoJob'}
						<div class="border-common mt-5">
							<p class="label-common w-full">Connector</p>
							{#if $connectorsStore.length > 0}
								<select name="connector" bind:value={projectConnector} class="input-common">
									{#each $connectorsStore as connector}
										<option value={connector['@_name']} selected={projectConnector}
											>{connector['@_name']}</option
										>
									{/each}
								</select>
							{:else}
								No connectors
							{/if}
						</div>

						<div class="border-common mt-5">
							<p class="label-common w-full">Transaction</p>
							{#if $transactionsStore.length > 0}
								<select name="transaction" bind:value={projectTransaction} class="input-common">
									{#each $transactionsStore as transaction}
										<option value={transaction['@_name']} selected={projectTransaction}
											>{transaction['@_name']}</option
										>
									{/each}
								</select>
							{:else}
								No transaction
							{/if}
						</div>
					{:else if $modalStore[0]?.meta?.mode === 'SequenceConvertigoJob'}
						<div class="border-common mt-5">
							<p class="label-common w-full">Sequence</p>
							{#if $sequencesStore.length > 0}
								<select name="sequence" bind:value={projectSequence} class="input-common">
									{#each $sequencesStore as sequence}
										<option value={sequence['@_name']} selected={projectSequence}
											>{sequence['@_name']}</option
										>
									{/each}
								</select>
							{:else}
								No Sequences
							{/if}
						</div>
					{/if}
				</div>
			</div>
			<ModalButtons />
		</form>
	</Card>
{:else if $modalStore[0]?.meta?.mode === 'schedulerNewJobGroupJob'}
	<Card title={$modalStore[0].title} class="w-[60vw]">
		<form class="p-5" on:submit={createScheduledElements}>
			<div class="grid grid-cols-2 gap-10">
				<div class="col-span-1 flex flex-col gap-5">
					<SchedulerForm />
				</div>
				<div class="col-span-1 flex flex-col gap-5 max-h-[30vh]">
					<p class="label-common">Parallel Job execution</p>
					<input type="hidden" name="parallelJob" value={jobCount} />

					<div class="flex shadow-md justify-between items-center rounded-token bg-surface-500">
						<button type="button" on:click|preventDefault={() => adjustJobCount(-1)}>-</button>
						<span>{jobCount}</span>
						<button type="button" on:click|preventDefault={() => adjustJobCount(1)}>+</button>
					</div>

					<p class="font-bold">Select a job</p>
					<ResponsiveContainer
						scrollable={true}
						smCols="sm:grid-cols-1"
						mdCols="md:grid-cols-1"
						lgCols="lg:grid-cols-1"
					>
						<RadioGroup rounded="rounded-container-token" flexDirection="flex-col">
							{#each $jobsStore as jobName}
								<RadioItem
									class=""
									active="bg-success-400-500-token"
									bind:group={selectedJob}
									name="jobsname"
									value={jobName['@_name']}>{jobName['@_name']}</RadioItem
								>
							{/each}
						</RadioGroup>
					</ResponsiveContainer>
				</div>
			</div>
			<ModalButtons />
		</form></Card
	>
{:else if $modalStore[0]?.meta?.mode === 'schedulerNewScheduleCron'}
	<Card title={$modalStore[0].title}>
		<form on:submit={createScheduledElements}>
			<ResponsiveContainer
				scrollable={true}
				smCols="sm:grid-cols-1"
				mdCols="md:grid-cols-6"
				lgCols="lg:grid-cols-6"
			>
				<div class="col-span-2">
					<SchedulerForm />
				</div>

				<div class="col-span-4 flex flex-col gap-5">
					<label class="border-common">
						<p class="label-common">Cron Expression</p>
						<input name="cron" bind:value={cronExpression} class="input-common" />
					</label>
					<CronWizard bind:cronExpression />
				</div>
			</ResponsiveContainer>
			<ModalButtons />
		</form>
	</Card>
{:else if $modalStore[0]?.meta?.mode === 'schedulerNewScheduleRunNow'}
	<Card title={$modalStore[0].title} class="w-[60vw]">
		<form on:submit={createScheduledElements}>
			<div class="col-span-2 flex flex-col gap-5">
				<SchedulerForm />
			</div>
			<ModalButtons />
		</form>
	</Card>
{:else if $modalStore[0]?.meta?.mode === 'schedulerNewScheduledJob'}
	<Card title={$modalStore[0].title}>
		<form class="" on:submit={createScheduledElements}>
			<ResponsiveContainer
				scrollable={true}
				smCols="sm:grid-cols-4"
				mdCols="md:grid-cols-6"
				lgCols="lg:grid-cols-6"
			>
				<div class="col-span-2 flex flex-col gap-5">
					<SchedulerForm />
				</div>

				<div class="col-span-2 flex flex-col gap-5">
					<ResponsiveContainer
						scrollable={true}
						smCols="sm:grid-cols-2"
						mdCols="md:grid-cols-2"
						lgCols="lg:grid-cols-2"
					>
						<select class="select h-[40vh] rounded-token" multiple={true} name="jobName">
							{#each $jobsStore as jobName}
								<option class="font-extralight" selected={jobName} value={jobName['@_name']}
									>{jobName['@_name']}</option
								>
							{/each}
						</select>

						<select class="select h-[40vh] rounded-token" multiple={true} name="jobName">
							{#each $schedulesStore as schedule, i}
								<option class="font-extralight" selected={schedule} value={schedule['@_name']}
									>{schedule['@_name']}</option
								>
							{/each}
						</select>
					</ResponsiveContainer>
				</div>
			</ResponsiveContainer>
			<ModalButtons />
		</form>
	</Card>
{/if}
