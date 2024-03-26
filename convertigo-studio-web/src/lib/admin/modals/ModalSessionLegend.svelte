<script>
	import { getModalStore } from '@skeletonlabs/skeleton';
	import TableAutoCard from '../components/TableAutoCard.svelte';
	import Icon from '@iconify/svelte';
	import Card from '../components/Card.svelte';

	const modalStore = getModalStore();

	const legendItems = [
		{
			title: 'Connection state of the connector used by the context associated to the connection',
			icon: 'fluent-emoji-flat:green-circle',
			icon2: 'emojione:red-circle'
		},
		{
			title: 'Creation date of the context',
			icon: 'material-symbols-light:date-range-outline-sharp'
		},
		{
			title: 'Last access date to the context',
			icon: 'material-symbols-light:date-range-rounded'
		},
		{
			title: 'Inactivity duration',
			icon: 'carbon:intent-request-inactive'
		},
		{
			title: 'Click to filter logs',
			icon: 'octicon:filter-24'
		},
		{
			title: 'FullSync replication activity',
			icon: 'fluent:cube-sync-20-regular'
		},
		{
			title: 'Number of Convertigo administration roles affected to this session',
			icon: 'arcticons:google-admin'
		}
	];
</script>

<Card title="Session Legends">
	<TableAutoCard
		definition={[
			{ name: 'Name', custom: true },
			{ name: 'Icon', custom: true }
		]}
		data={legendItems}
		let:row
		let:def
	>
		{#if def.name === 'Name'}
			{row.title}
		{/if}

		{#if def.name === 'Icon'}
			<div class="flex">
				{#if row.icon !== undefined}
					<Icon icon={row.icon} class="w-6 h-6" />
				{/if}
				{#if row.icon2}
					<Icon icon={row.icon2} class="w-6 h-6" />
				{/if}
			</div>
		{/if}
	</TableAutoCard>
</Card>
