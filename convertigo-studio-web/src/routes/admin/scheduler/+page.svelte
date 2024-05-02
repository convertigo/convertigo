<script>
	import Card from '$lib/admin/components/Card.svelte';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import Icon from '@iconify/svelte';
	import {
		schedulerList,
		jobsStore,
		schedulesStore,
		scheduledStore
	} from '$lib/admin/stores/schedulerStore';
	import { call } from '$lib/utils/service';

	const modalStore = getModalStore();

	const jobTypes = [
		'TransactionConvertigoJob',
		'SequenceConvertigoJob',
		'schedulerNewJobGroupJob',
		'schedulerNewScheduleCron',
		'schedulerNewScheduleRunNow',
		'schedulerNewScheduledJob'
	];

	const jobNames = [
		'Job Transaction',
		'Job Sequence',
		'Jobs Group',
		'Cron',
		'Run Now',
		'Scheduled Job'
	];

	onMount(async () => {
		await schedulerList();
	});

	function openModals(jobTypeFromRow, context = 'create', existingCron = '* * * * *') {
		const jobType = jobTypeFromRow || 'defaultType';
		const action = context === 'edit' ? 'Edit' : 'New';

		const jobName = jobNames[jobTypes.indexOf(jobType)];

		modalStore.trigger({
			type: 'component',
			component: 'modalScheduler',
			meta: { mode: jobType, context, cronExpression: existingCron },
			title: `${action} ${jobName}`
		});
	}

	//Service do not include any response for that
	async function deleteScheduledElement(row) {
		try {
			await call('scheduler.CreateScheduledElements', {
				del: 'true',
				exname: row['@_name'],
				type: 'schedulerNew' + row['@_type']
			});
			await schedulerList();
		} catch (err) {
			console.error(err);
		}
	}
</script>

<Card title="Jobs" class="">
	<div slot="cornerOption">
		<div class="mb-5 flex flex-wrap gap-2 pl-5">
			{#each jobTypes.slice(0, 3) as jobType, i}
				<div class="flex-1">
					<button
						class="bg-primary-400-500-token min-w-auto md:w-60 w-full"
						on:click={() => openModals(jobType)}
					>
						<p>New {jobNames[i]}</p>
					</button>
				</div>
			{/each}
		</div>
	</div>

	<TableAutoCard
		definition={[
			{
				name: 'Enabled',
				key: '@_enabled',
				class: (row) =>
					row['@_enabled'] === 'true' ? 'bg-success-400-500-token' : 'bg-error-400-500-token'
			},
			{ name: 'Type', key: '@_type', custom: true },
			{ name: 'Name', key: '@_name' },
			{ name: 'Description', key: '@_description' },
			{ name: 'Info', key: '@_info' },
			{ name: 'Edit', custom: true },
			{ name: 'Delete', custom: true }
		]}
		data={$jobsStore}
		let:def
		let:row
	>
		{#if def.name === 'Type'}
			{row['@_type'].replace('ConvertigoJob', '')}
		{:else if def.name === 'Edit'}
			<button
				class="btn p-1 px-2 shadow-md bg-tertiary-400-500-token"
				on:click={() => openModals(row['@_type'], 'edit')}
			>
				<Icon icon="bitcoin-icons:edit-outline" class="w-7 h-7" />
			</button>
		{:else if def.name === 'Delete'}
			<button
				class="btn p-1 px-2 shadow-md bg-error-400-500-token"
				on:click={() => deleteScheduledElement(row)}
			>
				<Icon icon="material-symbols-light:delete-outline" class="w-7 h-7" />
			</button>
		{/if}
	</TableAutoCard>
</Card>

<Card title="Schedules" class="mt-5">
	<div slot="cornerOption">
		<div class="mb-5 flex flex-wrap gap-2">
			{#each jobTypes.slice(3, 5) as jobType, i}
				<div class="flex-1">
					<button
						class="bg-primary-400-500-token min-w-auto md:w-60 w-full"
						on:click={() => openModals(jobType)}
					>
						<p>New {jobNames[i + 3]}</p>
					</button>
				</div>
			{/each}
		</div>
	</div>

	<TableAutoCard
		definition={[
			{
				name: 'Enabled',
				key: '@_enabled',
				class: (row) =>
					row['@_enabled'] === 'true' ? 'bg-success-400-500-token' : 'bg-error-400-500-token'
			},
			{ name: 'Type', key: '@_type' },
			{ name: 'Name', key: '@_name' },
			{ name: 'Description', key: '@_description' },
			{ name: 'Info', key: '@_info' },
			{ name: 'Next', key: '@_info' },
			{ name: 'Edit', custom: true },
			{ name: 'Delete', custom: true }
		]}
		data={$schedulesStore}
		let:def
		let:row
	>
		{#if def.name === 'Edit'}
			<button
				class="btn p-1 px-2 shadow-md bg-tertiary-400-500-token"
				on:click={() => openModals(row['@_type'], 'edit')}
			>
				<Icon icon="bitcoin-icons:edit-outline" class="w-7 h-7" />
			</button>
		{:else if def.name === 'Delete'}
			<button
				class="btn p-1 px-2 shadow-md bg-error-400-500-token"
				on:click={() => deleteScheduledElement(row)}
			>
				<Icon icon="material-symbols-light:delete-outline" class="w-7 h-7" />
			</button>
		{/if}
	</TableAutoCard>
</Card>

<Card title="Scheduled jobs" class="mt-5">
	<div slot="cornerOption">
		<div class="mb-5 flex space-x-5">
			{#each jobTypes.slice(5, 6) as jobType, i}
				<div class="flex-1">
					<button
						class="bg-primary-400-500-token min-w-auto md:w-60 w-full"
						on:click={() => openModals(jobType)}
					>
						<p>New {jobNames[i + 5]}</p>
					</button>
				</div>
			{/each}
		</div>
	</div>

	<TableAutoCard
		definition={[
			{
				name: 'Enabled',
				key: '@_enabled',
				class: (row) =>
					row['@_enabled'] === 'true' ? 'bg-success-400-500-token' : 'bg-error-400-500-token'
			},
			{ name: 'Name', key: '@_name' },
			{ name: 'Description', key: '@_description' },
			{ name: 'Info', key: '@_info' },
			{ name: 'Edit', custom: true },
			{ name: 'Delete', custom: true }
		]}
		data={$scheduledStore}
		let:def
		let:row
	>
		{#if def.name === 'Edit'}
			<button class="btn p-1 px-2 shadow-md bg-tertiary-400-500-token">
				<Icon icon="bitcoin-icons:edit-outline" class="w-7 h-7" />
			</button>
		{:else if def.name === 'Delete'}
			<button
				class="btn p-1 px-2 shadow-md bg-error-400-500-token"
				on:click={() => deleteScheduledElement(row)}
			>
				<Icon icon="material-symbols-light:delete-outline" class="w-7 h-7" />
			</button>
		{/if}
	</TableAutoCard>
</Card>
