<script>
	import Card from '$lib/admin/components/Card.svelte';
	import Tables from '$lib/admin/components/Tables.svelte';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import Icon from '@iconify/svelte';
	import { schedulerList, jobsStore } from '$lib/admin/stores/schedulerStore';
	import { call } from '$lib/utils/service';

	const modalStore = getModalStore();

	onMount(async () => {
		await schedulerList();
	});

	function openModals(mode) {
		let component = 'modalScheduler';
		let title = '';

		switch (mode) {
			case 'newJobs':
				break;
		}
		modalStore.trigger({
			type: 'component',
			component: component,
			meta: { mode },
			title: title
		});
	}

	//Service do not include any response fro that
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
		<div class="mb-5 flex space-x-1">
			<button
				class="bg-primary-400-500-token min-w-auto w-40"
				on:click={() => openModals('newJobs')}
			>
				<p>New transaction</p>
			</button>
			<button
				class="bg-primary-400-500-token min-w-auto w-40"
				on:click={() => openModals('newJobs')}
			>
				<p>New sequence</p>
			</button>
			<button class="bg-primary-400-500-token min-w-auto w-40">
				<p>New job group</p>
			</button>
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
		data={$jobsStore}
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

<Card title="Schedules" class="mt-5">
	<div slot="cornerOption">
		<div class="mb-5 flex space-x-1">
			<button class="bg-primary-400-500-token min-w-auto w-40">
				<p>New Cron</p>
			</button>

			<button class="bg-primary-400-500-token min-w-auto w-40">
				<p>New run now</p>
			</button>
		</div>
	</div>

	<Tables headers={['Enabled', 'Name', 'Description', 'Info', 'Next', 'Edit', 'Delete']}></Tables>
</Card>

<Card title="Scheduled jobs" class="mt-5">
	<div slot="cornerOption">
		<div class="mb-5 flex space-x-5">
			<button class="bg-primary-400-500-token min-w-auto w-60">
				<p>New scheduled job</p>
			</button>
		</div>
	</div>

	<Tables headers={['Enabled', 'Name', 'Description', 'Info', 'Edit', 'Delete']}></Tables>
</Card>
