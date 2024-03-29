<script>
	import { onMount } from 'svelte';
	import Card from '$lib/admin/components/Card.svelte';
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
	import Ico from '$lib/utils/Ico.svelte';
	import { getModalStore } from '@skeletonlabs/skeleton';

	const modalStore = getModalStore();

	$: data = [
		{
			contexts: `${$contextsInUse} / ${$contextsNumber}`,
			threads: `${$threadsInUse} / ${$threadsNumber}`,
			sessions: `${$sessionsInUse} / ${$sessionsNumber}`,
			timeout: $httpTimeout
		}
	];

	onMount(() => {
		connectionsCheck();
	});
</script>

<Card title="Connections">
	<div slot="cornerOption">
		<button class="bg-primary-400-500-token max-w-80">Delete all Sessions and Connections</button>
	</div>
	<TableAutoCard
		definition={[
			{ name: 'Contexts In Use', key: 'contexts' },
			{ name: 'Threads In Use', key: 'threads' },
			{ name: 'Sessions In Use', key: 'sessions' },
			{ name: 'Max http session inactivity', key: 'timeout' }
		]}
		{data}
	/>
</Card>

<Card title="Sessions" class="mt-5">
	<div slot="cornerOption">
		<button
			class="bg-primary-400-500-token max-w-80"
			on:click={() => modalStore.trigger({ type: 'component', component: 'modalSessionLegend' })}
			>Show Legends</button
		>
	</div>
	<TableAutoCard
		definition={[
			{ name: 'ID', key: '@_sessionID' },
			{ name: 'Contexts', key: '@_contexts' },
			{ name: 'User', key: '@__authenticatedUser' },
			{ name: 'Roles', key: '@_adminRoles' },
			{ name: 'UUID', key: '@_deviceUUID' },
			{ name: 'FS', key: '@_isFullSyncActive' },
			{ icon: 'material-symbols-light:date-range-rounded', key: '@_lastSessionAccessDate' },
			{ icon: 'carbon:intent-request-inactive', key: '@_sessionInactivityTime' },
			{ name: 'Client IP', key: '@_clientIP' },
			{ name: 'Delete', custom: true }
		]}
		data={$sessionsStore}
		let:row
		let:def
	>
		{#if def.name === 'Delete'}
			<button class="bg-error-400-500-token">
				<Ico icon="material-symbols-light:delete-outline" class="h-7 w-7 " />
			</button>
		{/if}
	</TableAutoCard>
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
			{ name: 'Client Computer', key: '@_clientComputer' },
			{ name: 'Delete', custom: true }
		]}
		data={$connectionsStore}
		let:row
		let:def
	>
		{#if def.name === 'Delete'}
			<button class="bg-error-400-500-token">
				<Ico icon="material-symbols-light:delete-outline" class="h-7 w-7 " />
			</button>
		{/if}
	</TableAutoCard>
</Card>

<style lang="postcss">
	.legendDiv {
		@apply flex justify-between p-2 mt-2 border-b-[0.5px] dark:border-surface-500 border-surface-100;
	}
</style>
