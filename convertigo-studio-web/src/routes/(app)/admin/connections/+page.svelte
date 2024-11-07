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
	import { getModalStore, getToastStore } from '@skeletonlabs/skeleton';
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';

	const modalStore = getModalStore();
	const toastStore = getToastStore();

	let data = $derived([
		{
			contexts: `${$contextsInUse} / ${$contextsNumber}`,
			threads: `${$threadsInUse} / ${$threadsNumber}`,
			sessions: `${$sessionsInUse} / ${$sessionsNumber}`,
			timeout: $httpTimeout
		}
	]);

	onMount(() => {
		connectionsCheck();
	});

	function refreshConnections() {
		try {
			connectionsCheck();
			toastStore.trigger({
				message: 'Refreshed successfully',
				background: 'bg-success-400-500',
				timeout: 8000
			});
		} catch (err) {
			console.error(err);
			toastStore.trigger({
				message: 'An error occurred',
				background: 'bg-success-400-500',
				timeout: 8000
			});
		}
	}
</script>

<Card title="Connections">
	{#snippet cornerOption()}
		<ButtonsContainer>
			<button class="delete-button">
				<Ico icon="mingcute:delete-line" />
				<p>Delete all Sessions and Connections</p>
			</button>
		</ButtonsContainer>
	{/snippet}
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

<Card title="Sessions">
	{#snippet cornerOption()}
		<ButtonsContainer>
			<button class="basic-button" onclick={refreshConnections}>
				<Ico icon="simple-line-icons:reload" />
				<p>Refresh Sessions</p>
			</button>
			<button
				class="basic-button"
				onclick={() => modalStore.trigger({ type: 'component', component: 'modalSessionLegend' })}
			>
				<Ico icon="mdi:eye" />
				<p>Show Legends</p>
			</button>
		</ButtonsContainer>
	{/snippet}
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
	>
		{#snippet children(row, def)}
			{#if def.name === 'Delete'}
				<button class="delete-button">
					<Ico icon="mingcute:delete-line" />
				</button>
			{/if}
		{/snippet}
	</TableAutoCard>
</Card>

<Card title="Contexts">
	{#snippet cornerOption()}
		<ButtonsContainer>
			<button class="basic-button" onclick={refreshConnections}
				><Ico icon="simple-line-icons:reload" />
				<p>Refresh Sessions</p>
			</button>
		</ButtonsContainer>
	{/snippet}
	<TableAutoCard
		definition={[
			{ name: 'Context', key: '@_contextName', class: 'max-w-1/4 break-all' },
			{ name: 'Project', key: '@_project' },
			{ name: 'Connector', key: '@_connector' },
			{ name: 'Requested', key: '@_requested' },
			{ name: 'Status', key: '@_status' },
			{ name: 'User', key: '@_user' },
			{ name: 'Client Computer', key: '@_clientComputer' },
			{ name: 'Delete', custom: true }
		]}
		data={$connectionsStore}
	>
		{#snippet children(row, def)}
			{#if def.name === 'Delete'}
				<button class="delete-button">
					<Ico icon="mingcute:delete-line" />
				</button>
			{/if}
		{/snippet}
	</TableAutoCard>
</Card>

<style lang="postcss">
	.legendDiv {
		@apply flex justify-between p-2 mt-2 border-b-[0.5px] dark:border-surface-500 border-surface-100;
	}
</style>
