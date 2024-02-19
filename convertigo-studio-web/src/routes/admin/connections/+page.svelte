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
	import Tables from '$lib/admin/components/Tables.svelte';

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
				<div class="flex p-2 border-b-[0.5px] dark:border-surface-500 border-surface-100">
					<p class="">Contexts In Use :</p>
					<p class="ml-5 mr-10 font-bold">{$contextsInUse} / {$contextsNumber}</p>
				</div>
				<div class="flex p-2 mt-2 border-b-[0.5px] dark:border-surface-500 border-surface-100">
					<p class="">Threads currently In Use :</p>
					<p class="ml-5 mr-10 font-bold">{$threadsInUse} / {$threadsNumber}</p>
				</div>
				<div class="flex p-2 mt-2 border-b-[0.5px] dark:border-surface-500 border-surface-100">
					<p class="">Sessions currently in use:</p>
					<p class="ml-5 mr-10 font-bold">{$sessionsInUse} / {$sessionsNumber}</p>
				</div>
				<div class="flex p-2 mt-2">
					<p class="">Max http session inactivity :</p>
					<p class="ml-5 mr-10 font-bold">{$httpTimeout}</p>
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

<div class="mt-10">
	<Card title="Sessions">
		<Tables headers={['ID', 'Contexts', 'User', 'Roles', 'UUID', 'FS']}>
			{#if $sessionsStore.length >= 0}
				{#each $sessionsStore as session}
					<tr>
						<td>{session['@_sessionID']}</td>
						<td>{session['@_contexts']}</td>
						<td>{session['@__authenticatedUser']}</td>
						<td>{session['@_adminRoles']}</td>
						<td>{session['@_deviceUUID']}</td>
						<td>{session['@_isFullSyncActive']}</td>
						<td>{session['@_lastSessionAccessDate']}</td>
						<td>{session['@_sessionInactivityTime']}</td>
						<td>{session['@_clientIP']}</td>
					</tr>
				{/each}
			{/if}
		</Tables>
	</Card>
</div>

<div class="mt-10">
	<Card title="Contexts">
		<Tables
			headers={[
				'Context',
				'Project',
				'Connector',
				'Requested',
				'Status',
				'User',
				'Client Computer'
			]}
		>
			{#if $connectionsStore.length >= 0}
				{#each $connectionsStore as connection}
					<tr>
						<td>{connection['@_contextName']}</td>
						<td>{connection['@_project']}</td>
						<td>{connection['@_connector']}</td>
						<td>{connection['@_requested']}</td>
						<td>{connection['@_status']}</td>
						<td>{connection['@_user']}</td>
						<td>{connection['@_clientComputer']}</td>
					</tr>
				{/each}
			{/if}
		</Tables>
	</Card>
</div>
