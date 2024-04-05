<script>
	import Card from '$lib/admin/components/Card.svelte';
	import Tables from '$lib/admin/components/Tables.svelte';
	import ModalScheduler from '$lib/admin/modals/ModalScheduler.svelte';
	import { call } from '$lib/utils/service';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { writable } from 'svelte/store';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import Icon from '@iconify/svelte';

	const schedulerModalStore = getModalStore();

	let jobs = writable([]);

	onMount(() => {
		schedulerList();
	});

	async function schedulerList() {
		const res = await call('scheduler.List');

		let elementArray = res?.admin?.element ?? [];
		if (!Array.isArray(elementArray)) {
			elementArray = [elementArray];
		}

		jobs.set(elementArray);
		console.log('scheduler list res:', res);
	}

	async function createScheduleElement() {
		const res = await call('scheduler.CreateScheduledElements');
		console.log('scheduler create:', res);
	}

	function openNewTransactionModal() {
		schedulerModalStore.trigger({
			type: 'component',
			component: 'modalScheduler',
			meta: { mode: 'newTransaction' }
		});
	}
</script>

<Card title="Jobs" class="">
	<div slot="cornerOption">
		<div class="mb-5 flex space-x-5">
			<button class="bg-primary-400-500-token" on:click={openNewTransactionModal}>
				<p>New transaction</p>
			</button>

			<button class="bg-primary-400-500-token">
				<p>New sequence</p>
			</button>

			<button class="bg-primary-400-500-token">
				<p>New job group</p>
			</button>
		</div>
	</div>
	<TableAutoCard
		definition={[
			{
				name: 'Enabled',
				key: '@_enabled',
				class: (row) => (row['@_enabled'] ? 'bg-success-400-500-token' : 'bg-error-400-500-token')
			},
			{ name: 'Name', key: '@_name' },
			{ name: 'Description', key: '@_description' },
			{ name: 'Info', key: '@_info' },
			{ name: 'Edit', custom: true },
			{ name: 'Delete', custom: true }
		]}
		data={$jobs}
		let:def
		let:row
	>
		{#if def.name === 'Edit'}
			<button class="btn p-1 px-2 shadow-md bg-tertiary-400-500-token">
				<Icon icon="bitcoin-icons:edit-outline" class="w-7 h-7" />
			</button>
		{:else if def.name === 'Delete'}
			<button class="btn p-1 px-2 shadow-md bg-error-400-500-token">
				<Icon icon="material-symbols-light:delete-outline" class="w-7 h-7" />
			</button>
		{/if}
	</TableAutoCard>
</Card>

<Card title="Schedules" class="mt-5">
	<div slot="cornerOption">
		<div class="mb-5 flex space-x-5">
			<button class="bg-primary-400-500-token">
				<p>New Cron</p>
			</button>

			<button class="bg-primary-400-500-token">
				<p>New run now</p>
			</button>
		</div>
	</div>

	<Tables headers={['Enabled', 'Name', 'Description', 'Info', 'Next', 'Edit', 'Delete']}></Tables>
</Card>

<Card title="Scheduled jobs" class="mt-5">
	<div slot="cornerOption">
		<div class="mb-5 flex space-x-5">
			<button class="bg-primary-400-500-token">
				<p>New scheduled job</p>
			</button>
		</div>
	</div>

	<Tables headers={['Enabled', 'Name', 'Description', 'Info', 'Edit', 'Delete']}></Tables>
</Card>
