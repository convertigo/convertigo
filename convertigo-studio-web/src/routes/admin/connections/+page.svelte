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
	import Icon from '@iconify/svelte';
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';

	const modalStore = getModalStore();
	const toastStore = getToastStore();

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

	function refreshConnections() {
		try {
			connectionsCheck();
			toastStore.trigger({
				message: 'Refreshed successfully',
				background: 'bg-success-400-500-token',
				timeout: 8000
			});
		} catch (err) {
			console.error(err);
			toastStore.trigger({
				message: 'An error occurred',
				background: 'bg-success-400-500-token',
				timeout: 8000
			});
		}
	}
</script>

<Card title="Connections">
	<div slot="cornerOption">
		<ButtonsContainer>
			<button class="bg-error-400-500-token">
				Delete all Sessions and Connections
				<Ico icon="material-symbols-light:delete-outline" class="h-6 w-6" />
			</button>
		</ButtonsContainer>
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
		<ButtonsContainer>
			<button class="bg-success-400-500-token max-w-80" on:click={() => refreshConnections()}>
				Refresh Sessions
				<Icon icon="simple-line-icons:reload" rotate={1} class="w-4 h-4" />
			</button>
			<button
				class="bg-primary-400-500-token max-w-80"
				on:click={() => modalStore.trigger({ type: 'component', component: 'modalSessionLegend' })}
			>
				Show Legends
				<Icon icon="mdi:show" class="h-4 w-4" /></button
			>
		</ButtonsContainer>
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
				<Ico icon="material-symbols-light:delete-outline" class="h-6 w-6 " />
			</button>
		{/if}
	</TableAutoCard>
</Card>

<Card title="Contexts" class="mt-5">
	<div slot="cornerOption">
		<ButtonsContainer>
			<button class="bg-success-400-500-token max-w-80" on:click={() => refreshConnections()}
				>Refresh Contexts
				<Icon icon="simple-line-icons:reload" rotate={1} class="w-4 h-4" />
			</button>
		</ButtonsContainer>
	</div>
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
				<Ico icon="material-symbols-light:delete-outline" class="h-6 w-6 " />
			</button>
		{/if}
	</TableAutoCard>
</Card>

<style lang="postcss">
	.legendDiv {
		@apply flex justify-between p-2 mt-2 border-b-[0.5px] dark:border-surface-500 border-surface-100;
	}
</style>
