<script>
	import Card from '$lib/admin/components/Card.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import Connections from '$lib/admin/Connections.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { getContext, onMount } from 'svelte';

	let {
		contextsInUse,
		contextsNumber,
		threadsInUse,
		threadsNumber,
		sessionsInUse,
		sessionsNumber,
		httpTimeout,
		deleteAll,
		sessions,
		selectedSession,
		deleteSession,
		connections,
		deleteContext,
		stop,
		init
	} = $derived(Connections);

	let data = $derived([
		{
			contexts: contextsInUse == null ? null : `${contextsInUse} / ${contextsNumber}`,
			threads: threadsInUse == null ? null : `${threadsInUse} / ${threadsNumber}`,
			sessions: sessionsInUse == null ? null : `${sessionsInUse} / ${sessionsNumber}`,
			timeout: httpTimeout
		}
	]);

	onMount(() => {
		httpTimeout;
		return stop;
	});

	let modalYesNo = getContext('modalYesNo');
</script>

<div class="layout-y-stretch">
	<Card title="Connections">
		{#snippet cornerOption()}
			<ResponsiveButtons
				buttons={[
					{
						label: 'Delete all Sessions and Contexts',
						icon: 'mdi:delete-outline',
						cls: 'button-error',
						onclick: async (event) => {
							if (
								await modalYesNo.open({
									event,
									title: 'Do you confirm to delete',
									message: `${sessionsInUse} sessions and ${contextsInUse} contexts?`
								})
							) {
								deleteAll();
							}
						}
					}
				]}
				disabled={!init}
			/>
		{/snippet}
		<TableAutoCard
			class="text-left"
			definition={[
				{ name: 'Contexts In Use', key: 'contexts', class: 'max-w-10' },
				{ name: 'Threads In Use', key: 'threads', class: 'max-w-10' },
				{ name: 'Sessions In Use', key: 'sessions', class: 'max-w-10' },
				{ name: 'Max http session inactivity', key: 'timeout', class: 'max-w-10' }
			]}
			{data}
		/>
	</Card>

	<Card title="Sessions">
		<TableAutoCard
			definition={[
				{ name: 'Actions', custom: true },
				{ name: 'ID', key: 'sessionID', class: 'break-all' },
				{ name: 'User', key: 'authenticatedUser' },
				{ name: 'Contexts', key: 'contexts' },
				{ name: 'Roles', key: 'adminRoles' },
				{ name: 'FS', custom: true, key: 'isFullSyncActive' },
				{ name: 'UUID', key: 'deviceUUID' },
				{ name: 'Access', key: 'lastSessionAccessDate' },
				{ name: 'Activity', key: 'sessionInactivityTime' },
				{ name: 'Client IP', key: 'clientIP' }
			]}
			class="session-table"
			data={sessions}
		>
			{#snippet children({ row: { sessionID, isCurrentSession, isFullSyncActive }, def })}
				{#if def.name == 'Actions'}
					<ResponsiveButtons
						class="w-full min-w-24"
						size="6"
						buttons={[
							{
								icon: 'mdi:magnify',
								cls: 'button-ico-primary',
								onclick: () => {
									alert('TODO: filter in log viewer');
								}
							},
							{
								icon: 'mdi:filter',
								cls: 'button-ico-tertiary',
								onclick: () => {
									Connections.selectedSession = sessionID;
								}
							},
							{
								icon: 'mdi:delete-outline',
								cls: 'button-ico-error',
								onclick: async (event) => {
									if (
										await modalYesNo.open({
											event,
											title: 'Delete session',
											message: `${sessionID}?`
										})
									) {
										deleteSession(sessionID);
									}
								}
							}
						]}
						disabled={!init}
					/>
					{#if isCurrentSession == 'true'}
						<span class="current"></span>
					{/if}
				{:else if def.name == 'FS'}
					<Ico
						icon="mdi:sync"
						class={!init
							? 'animate-pulse'
							: isFullSyncActive == 'true'
								? 'text-green-500'
								: 'text-red-500'}
						size="btn"
					/>
				{/if}
			{/snippet}
		</TableAutoCard>
	</Card>

	<Card title="Contexts">
		{#snippet cornerOption()}
			{#if selectedSession != ''}
				<ResponsiveButtons
					buttons={[
						{
							label: `Remove filter of ${selectedSession}`,
							icon: 'mdi:broom',
							cls: 'button-primary',
							onclick: () => {
								Connections.selectedSession = '';
							}
						}
					]}
					disabled={!init}
				/>
			{/if}
		{/snippet}
		<TableAutoCard
			definition={[
				{ name: 'Actions', custom: true },
				{ name: 'Context', key: 'contextName', class: 'break-all min-w-40' },
				{ name: 'Project', key: 'project', class: 'break-all min-w-40' },
				{ name: 'Connector', key: 'connector', class: 'break-all min-w-40' },
				{ name: 'Requested', key: 'requested', class: 'break-all min-w-40' },
				{ name: 'Status', key: 'status' },
				{ name: 'User', key: 'user' },
				{ name: 'Client Computer', key: 'clientComputer' }
			]}
			data={connections}
		>
			{#snippet children({ row: { contextName }, def })}
				{#if def.name == 'Actions'}
					<ResponsiveButtons
						class="w-full min-w-16"
						size="6"
						buttons={[
							{
								icon: 'mdi:magnify',
								cls: 'button-ico-primary',
								onclick: () => {}
							},
							{
								icon: 'mdi:delete-outline',
								cls: 'button-ico-error',
								onclick: async (event) => {
									if (
										await modalYesNo.open({
											event,
											title: 'Delete context',
											message: `${contextName}?`
										})
									) {
										deleteContext(contextName);
									}
								}
							}
						]}
						disabled={!init}
					/>
				{/if}
			{/snippet}
		</TableAutoCard>
	</Card>
</div>

<style lang="postcss">
	@reference "../../../../app.css";

	:global(.session-table tr:has(.current)) {
		@apply preset-filled-success-100-900;
	}
</style>
