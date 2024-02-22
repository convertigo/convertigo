<script>
	import Card from '$lib/admin/components/Card.svelte';
	import Tables from '$lib/admin/components/Tables.svelte';
	import ModalScheduler from '$lib/admin/modals/ModalScheduler.svelte';
	import { call } from '$lib/utils/service';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';

	const schedulerModalStore = getModalStore();

	onMount(() => {
		schedulerList();
	});

	async function schedulerList() {
		const res = await call('scheduler.List');
		console.log('scheduler list res:', res);
	}

	async function createScheduleElement() {
		const res = await call('scheduler.CreateScheduledElements');
		console.log('scheduler create:', res);
	}

	function openNewTransactionModal() {
		schedulerModalStore.trigger({
			type: 'component',
			component: { ref: ModalScheduler },
			meta: { mode: 'newTransaction' }
		});
	}
</script>

<Card title="Jobs" class="">
	<div class="mb-5 flex space-x-5">
		<button class="btn bg-buttons" on:click={openNewTransactionModal}>
			<p>New transaction</p>
		</button>

		<button class="btn bg-buttons">
			<p>New sequence</p>
		</button>

		<button class="btn bg-buttons">
			<p>New job group</p>
		</button>
	</div>
	<Tables headers={['Enabled', 'Name', 'Description', 'Info', 'Edit', 'Delete']}></Tables>
</Card>

<Card title="Schedules" class="mt-5">
	<div class="mb-5 flex space-x-5">
		<button class="btn bg-buttons">
			<p>New Cron</p>
		</button>

		<button class="btn bg-buttons">
			<p>New run now</p>
		</button>
	</div>
	<Tables headers={['Enabled', 'Name', 'Description', 'Info', 'Next', 'Edit', 'Delete']}></Tables>
</Card>

<Card title="Scheduled jobs" class="mt-5">
	<div class="mb-5 flex space-x-5">
		<button class="btn bg-buttons">
			<p>New scheduled job</p>
		</button>
	</div>
	<Tables headers={['Enabled', 'Name', 'Description', 'Info', 'Edit', 'Delete']}></Tables>
</Card>
