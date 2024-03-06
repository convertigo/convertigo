<script>
	import { onMount } from 'svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import AutoGrid from '$lib/admin/components/AutoGrid.svelte';
	import Icon from '@iconify/svelte';
	import {
		contextsInUse,
		contextsNumber,
		httpTimeout,
		sessionsInUse,
		sessionsNumber,
		threadsInUse,
		threadsNumber,
		connectionsCheck,
		connectionsStore,
		sessionsStore
	} from '$lib/admin/stores/connectionsStore';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';

	let legendItems = [
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

	$: statsData = [
		{ category: 'Contexts In Use', inUse: $contextsInUse, total: $contextsNumber },
		{ category: 'Threads currently In Use', inUse: $threadsInUse, total: $threadsNumber },
		{ category: 'Sessions currently in use', inUse: $sessionsInUse, total: $sessionsNumber },
		{ category: 'Max http session inactivity', timeout: $httpTimeout }
	];

	onMount(() => {
		connectionsCheck();
	});
</script>

<AutoGrid>
	<Card title="Connections">
		<TableAutoCard
			definition={[
				{ name: 'Category', custom: true },
				{ name: 'In Use / Total', custom: true }
			]}
			data={statsData}
			let:row
			let:def
		>
			{#if def.name === 'Category'}
				{row.category}
			{:else if def.name === 'In Use / Total'}
				{#if row.total !== undefined}
					{row.inUse} / {row.total}
				{:else}
					{row.timeout}
				{/if}
			{/if}
		</TableAutoCard>

			<button class="bg-error-400-500-token max-w-80 mt-10">Delete all Sessions and Connections</button>

	</Card>

	<Card title="Legends">
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
</AutoGrid>

<Card title="Sessions" class="mt-5">
	<TableAutoCard
		definition={[
			{ name: 'ID', key: '@_sessionID' },
			{ name: 'Contexts', key: '@_contexts' },
			{ name: 'User', key: '@__authenticatedUser' },
			{ name: 'Roles', key: '@_adminRoles' },
			{ name: 'UUID', key: '@_deviceUUID' },
			{ name: 'FS', key: '@_isFullSyncActive' },
			{ name: '', key: '@_lastSessionAccessDate' },
			{ name: '', key: '@_sessionInactivityTime' },
			{ name: '', key: '@_clientIP' }
		]}
		data={$sessionsStore}
		let:row
		let:def
	></TableAutoCard>
</Card>

<Card title="Contexts" class="mt-5">
	<TableAutoCard
		definition={[
			{ name: 'Context', key: '@_contextName' },
			{ name: 'Project', key: '@_project' },
			{ name: 'Connector', key: '@_connector' },
			{ name: 'Requested', key: '@_requested' },
			{ name: 'Status', key: '@_status' },
			{ name: 'User', key: '@_user' },
			{ name: 'Client Computer', key: '@_clientComputer' }
		]}
		data={$connectionsStore}
		let:row
		let:def
	></TableAutoCard>
</Card>

<style lang="postcss">
	.legendDiv {
		@apply flex justify-between p-2 mt-2 border-b-[0.5px] dark:border-surface-500 border-surface-100;
	}
</style>
