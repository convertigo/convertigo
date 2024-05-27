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
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';

	const modalStore = getModalStore();

	const jobTypes = {
		TransactionConvertigoJob: { name: 'Job Transaction', imageUrl: 'carbon:data-regular' },
		SequenceConvertigoJob: { name: 'Job Sequence', imageUrl: 'material-symbols:api-rounded' },
		JobGroupJob: { name: 'Jobs Group', imageUrl: 'uim:layer-group' },
		ScheduleCron: { name: 'Cron', imageUrl: 'eos-icons:cronjob' },
		ScheduleRunNow: { name: 'Run Now', imageUrl: 'codicon:run-all' },
		ScheduledJob: { name: 'Scheduled Job', imageUrl: 'mdi:invoice-scheduled-outline' }
	};

	onMount(async () => {
		await schedulerList();
	});

	function openModals(mode, row) {
		modalStore.trigger({
			type: 'component',
			component: 'modalScheduler',
			meta: { mode, row },
			title: `${row ? 'Edit' : 'New'} ${jobTypes[mode].name}`
		});
	}
	function makeSingular(title) {
		return title.endsWith('s') ? title.slice(0, -1) : title;
	}
	//Service do not include any response for that
	async function deleteScheduledElement(row, title) {
		const singularTitle = makeSingular(title);
		modalStore.trigger({
			type: 'component',
			title: 'Please Confirm',
			body: `Are you sure you want to delete this ${singularTitle} ?`,
			component: 'modalWarning',
			meta: { mode: 'Confirm' },
			response: (confirmed) => {
				if (confirmed) {
					call('scheduler.CreateScheduledElements', {
						del: 'true',
						exname: row['@_name'],
						type: 'schedulerNew' + row['@_type']
					});
					schedulerList();
				}
			}
		});
	}
	const cards = [
		{
			title: 'Jobs',
			range: [0, 3]
		},
		{
			title: 'Schedules',
			range: [3, 5],
			next: true
		},
		{
			title: 'Scheduled jobs',
			range: [5, 6]
		}
	];
</script>

<div class="flex flex-col gap-y-5">
	{#each cards as { title, range, next }, i}
		<Card {title}>
			<div slot="cornerOption">
				<ButtonsContainer marginB="mb-0">
					{#each Object.entries(jobTypes).slice(...range) as [type, { name, imageUrl }]}
						<button class="bg-primary-400-500-token" on:click={() => openModals(type)}>
							<p>New {name}</p>
							<Icon icon={imageUrl} />
						</button>
					{/each}
				</ButtonsContainer>
			</div>

			<TableAutoCard
				definition={[
					{
						name: 'Enabled',
						custom: true
					},
					{ name: 'Name', key: '@_name' },
					{ name: 'Description', key: '@_description' },
					{ name: 'Info', key: '@_info', class: 'max-w-40 break-all' },
					{ name: 'Next', custom: true },
					{ name: 'Edit', custom: true },
					{ name: 'Delete', custom: true }
				].filter((elt) => next || elt.name != 'Next')}
				data={[$jobsStore, $schedulesStore, $scheduledStore][i]}
				let:def
				let:row
			>
				{#if def.name === 'Edit'}
					<button
						class="btn p-1 px-2 shadow-md bg-tertiary-400-500-token"
						on:click={() => openModals(row['@_type'], row)}
					>
						<Icon icon="bitcoin-icons:edit-outline" class="w-7 h-7" />
					</button>
				{:else if def.name === 'Enabled'}
					<div class="bg-success-400-500-token rounded-token text-token py-2 px-2">
						{row['@_enabled']}
					</div>
				{:else if def.name === 'Delete'}
					<button
						class="btn p-1 px-2 shadow-md bg-error-400-500-token"
						on:click={() => deleteScheduledElement(row, title)}
					>
						<Icon icon="material-symbols-light:delete-outline" class="w-7 h-7" />
					</button>
				{:else if def.name === 'Next'}
					<AutoPlaceholder loading={!row.next}
						><button
							on:click={() =>
								modalStore.trigger({
									title: 'Next triggers',
									body: `<div class="overflow-y-auto max-h-48">${row.next.join('</br>')}</div>`,
									type: 'alert',
									modalClasses: 'text-center overflow-y-scroll'
								})}>{row.next[0]}</button
						></AutoPlaceholder
					>
				{/if}
			</TableAutoCard>
		</Card>
	{/each}
</div>
