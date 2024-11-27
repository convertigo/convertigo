<script>
	import { onDestroy, onMount } from 'svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import Connections from '$lib/admin/Connections.svelte';
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
						label: 'Delete all Sessions and Contexts',
						icon: 'mingcute:delete-line',
						cls: 'delete-button',
						disabled,
						onclick: async (event) => {
							if (
								await modalDelete.open({
									event,
									title: 'Do you confirm to delete',
									message: `${Connections.sessionsInUse} sessions and ${Connections.contextsInUse} contexts?`
								})
							) {
								Connections.deleteAll();
							}
						}
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
			data={Connections.sessions}
		>
			{#snippet children({ row: { sessionID, isCurrentSession, isFullSyncActive }, def })}
				{#if def.name === 'Actions'}
					<ResponsiveButtons
						class="min-w-24 w-full"
						size="4"
						buttons={[
							{
								icon: 'lets-icons:search-light',
								cls: 'basic-button',
								disabled,
								onclick: () => {
									alert('TODO: filter in log viewer');
								}
							},
							{
								icon: 'mdi:filter',
								cls: 'yellow-button',
								disabled,
								onclick: () => {
									Connections.selectedSession = sessionID;
								}
							},
							{
								icon: 'mingcute:delete-line',
								cls: 'delete-button',
								disabled,
								onclick: async (event) => {
									if (
										await modalDelete.open({
											event,
											title: 'Delete session',
											message: `${sessionID}?`
										})
									) {
										Connections.deleteSession(sessionID);
									}
								}
							}
						]}
					/>
					{#if isCurrentSession == 'true'}
						<span class="current"></span>
					{/if}
				{:else if def.name === 'FS'}
					<Ico
						icon="material-symbols-light:sync-outline"
						class={disabled
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
				{ name: 'Context', key: 'contextName', class: 'break-all min-w-40' },
				{ name: 'Project', key: 'project', class: 'break-all min-w-40' },
				{ name: 'Connector', key: 'connector', class: 'break-all min-w-40' },
				{ name: 'Requested', key: 'requested', class: 'break-all min-w-40' },
				{ name: 'Status', key: 'status' },
				{ name: 'User', key: 'user' },
				{ name: 'Client Computer', key: 'clientComputer' }
			]}
			data={Connections.connections}
		>
			{#snippet children({ row: { contextName }, def })}
				{#if def.name == 'Actions'}
					<ResponsiveButtons
						class="min-w-16 w-full"
						size="4"
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
								onclick: async (event) => {
									if (
										await modalDelete.open({
											event,
											title: 'Delete context',
											message: `${contextName}?`
										})
									) {
										Connections.deleteContext(contextName);
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

<style lang="postcss">
	:global(.session-table tr:has(.current)) {
		@apply bg-success-300;
	}
	:global(.dark .session-table tr:has(.current)) {
		@apply bg-success-900;
	}
</style>
