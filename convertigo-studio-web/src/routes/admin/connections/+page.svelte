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

	onMount(() => {
		connectionsCheck();
	});
</script>

<AutoGrid>
	<Card title="Connections">
		<div class="mt-5">
			<div class="flex-col">
				<div class="legendDiv">
					<p class="">Contexts In Use :</p>
					<p class="valueConnectionsText">{$contextsInUse} / {$contextsNumber}</p>
				</div>
				<div class="legendDiv">
					<p class="">Threads currently In Use :</p>
					<p class="valueConnectionsText">{$threadsInUse} / {$threadsNumber}</p>
				</div>
				<div class="legendDiv">
					<p class="">Sessions currently in use:</p>
					<p class="valueConnectionsText">{$sessionsInUse} / {$sessionsNumber}</p>
				</div>
				<div class="flex p-2 mt-2">
					<p class="">Max http session inactivity :</p>
					<p class="valueConnectionsText">{$httpTimeout}</p>
				</div>
			</div>
		</div>
	</Card>

	<Card title="Legends">
		<div class="p-2 mt-2">
			{#each legendItems as legend}
				<div
					class={`flex items-center pl-2 p-1 border-b dark:border-surface-500 border-surface-100 justify-between`}
				>
					<p class="mr-5">{legend.title}</p>
					<div class="flex">
						<Icon icon={legend.icon} class="w-6 h-6" />
						{#if legend.icon2}
							<Icon icon={legend.icon2} class="w-6 h-6" />
						{/if}
					</div>
				</div>
			{/each}
		</div>
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
		@apply flex p-2 mt-2 border-b-[0.5px] dark:border-surface-500 border-surface-100;
	}

	.valueConnectionsText {
		@apply ml-5 mr-10 font-bold;
	}
</style>
