<script>
	import { onDestroy, onMount } from 'svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import Connections from '$lib/common/Connections.svelte';
	import ModalYesNo from '$lib/common/components/ModalYesNo.svelte';
	import Ico from '$lib/utils/Ico.svelte';

	let data = $derived([
		{
			contexts:
				Connections.contextsInUse == null
					? null
					: `${Connections.contextsInUse} / ${Connections.contextsNumber}`,
			threads:
				Connections.threadsInUse == null
					? null
					: `${Connections.threadsInUse} / ${Connections.threadsNumber}`,
			sessions:
				Connections.sessionsInUse == null
					? null
					: `${Connections.sessionsInUse} / ${Connections.sessionsNumber}`,
			timeout: Connections.httpTimeout
		}
	]);

	onDestroy(Connections.stop);

	let disabled = $derived(Connections.sessionsInUse == null);
	let modalDelete;
</script>

<ModalYesNo bind:this={modalDelete} />

<div class="layout-y !items-stretch">
	<Card title="Connections">
		{#snippet cornerOption()}
			<ResponsiveButtons
				buttons={[
					{
						label: 'Delete all Sessions and Connections',
						icon: 'mingcute:delete-line',
						cls: 'delete-button',
						disabled,
						onclick: () => {}
					}
				]}
			/>
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
		<TableAutoCard
			definition={[
				{ name: 'Actions', custom: true },
				{ name: 'ID', key: '@_sessionID' },
				{ name: 'User', key: '@_authenticatedUser' },
				{ name: 'Contexts', key: '@_contexts' },
				{ name: 'Roles', key: '@_adminRoles' },
				{ name: 'FS', custom: true, key: '@_isFullSyncActive' },
				{ name: 'UUID', key: '@_deviceUUID' },
				{ name: 'Access', key: '@_lastSessionAccessDate' },
				{ name: 'Activity', key: '@_sessionInactivityTime' },
				{ name: 'Client IP', key: '@_clientIP' }
			]}
			data={Connections.sessions}
		>
			{#snippet children({ row, def })}
				{#if def.name === 'Actions'}
					<!-- {@debug row} -->
					<ResponsiveButtons
						class="min-w-24 w-full"
						buttons={[
							{
								icon: 'lets-icons:search-light',
								cls: 'basic-button',
								disabled,
								onclick: () => {}
							},
							{
								icon: 'mdi:filter',
								cls: 'yellow-button',
								disabled,
								onclick: () => {
									Connections.selectedSession = row['@_sessionID'];
									Connections.refresh();
								}
							},
							{
								icon: 'mingcute:delete-line',
								cls: 'delete-button',
								disabled,
								onclick: async () => {
									if (
										await modalDelete.open({
											title: 'Delete session',
											message: `${row['@_sessionID']}?`
										})
									) {
										Connections.deleteSession(row['@_sessionID']);
									}
								}
							}
						]}
					/>
				{:else if def.name === 'FS'}
					<Ico
						icon="material-symbols-light:sync-outline"
						class={disabled
							? 'animate-pulse'
							: row['@_isFullSyncActive'] == 'true'
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
			{#if Connections.selectedSession != ''}
				<ResponsiveButtons
					buttons={[
						{
							label: `Remove filter of ${Connections.selectedSession}`,
							icon: 'mdi:broom',
							cls: 'basic-button',
							onclick: () => {
								Connections.selectedSession = '';
							}
						}
					]}
				/>
			{/if}
		{/snippet}
		<TableAutoCard
			definition={[
				{ name: 'Actions', custom: true },
				{ name: 'Context', key: '@_contextName', class: 'max-w-1/4 break-all' },
				{ name: 'Project', key: '@_project' },
				{ name: 'Connector', key: '@_connector' },
				{ name: 'Requested', key: '@_requested' },
				{ name: 'Status', key: '@_status' },
				{ name: 'User', key: '@_user' },
				{ name: 'Client Computer', key: '@_clientComputer' }
			]}
			data={Connections.connections}
		>
			{#snippet children({ row, def })}
				{#if def.name == 'Actions'}
					<ResponsiveButtons
						class="min-w-16 w-full"
						buttons={[
							{
								icon: 'lets-icons:search-light',
								cls: 'basic-button',
								disabled,
								onclick: () => {}
							},
							{
								icon: 'mingcute:delete-line',
								cls: 'delete-button',
								disabled,
								onclick: async () => {
									if (
										await modalDelete.open({
											title: 'Delete context',
											message: `${row['@_contextName']}?`
										})
									) {
										Connections.deleteSession(row['@_sessionID']);
									}
								}
							}
						]}
					/>
				{/if}
			{/snippet}
		</TableAutoCard>
	</Card>
</div>
