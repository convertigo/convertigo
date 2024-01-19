<script>
	import { onMount } from 'svelte';
	import { fetchConnectionsList } from '$lib/admin-console/stores/Store';
	import { localStorageStore } from '@skeletonlabs/skeleton';
	import Card from '$lib/admin-console/admin-components/Card.svelte';
	import AutoGrid from '$lib/admin-console/admin-components/AutoGrid.svelte';
	import Icon from '@iconify/svelte';
	import {
		connections,
		contextsInUse,
		contextsNumber,
		httpTimeout,
		sessions,
		sessionsInUse,
		sessionsIsOverFlow,
		sessionsNumber,
		threadsInUse,
		threadsNumber,
		connectionsCheck,
		connectionsStore,
		sessionsStore
	} from '$lib/admin-console/stores/connectionsStore';

	let theme = localStorageStore('studio.theme', 'skeleton');

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
		changeTheme($theme);
		document.body.setAttribute('data-theme', 'dark-theme');

		connectionsCheck();
	});

	function changeTheme(e) {
		$theme = typeof e == 'string' ? e : e.target?.value;
		document.body.setAttribute('data-theme', $theme);
	}
</script>

<div class="flex flex-col h-full p-10 w-full">
	<div class=" p-1">
		<AutoGrid>
			<Card>
				<h1 class="text-[15px]">Connections</h1>
				<div class="bg-surface-900 p-1 border-[0.5px] border-surface-600 mt-5">
					<div class="flex-col w-[60%]">
						<div class="flex bg-surface-900 p-2 border-b-[0.5px] border-surface-500">
							<p class="">Contexts In Use :</p>
							<p class="ml-5 mr-10 font-bold">{$contextsInUse} / {$contextsNumber}</p>
						</div>
						<div class="flex bg-surface-900 p-2 mt-2 border-b-[0.5px] border-surface-500">
							<p class="">Threads currently In Use :</p>
							<p class="ml-5 mr-10 font-bold">{$threadsInUse} / {$threadsNumber}</p>
						</div>
						<div class="flex bg-surface-900 p-2 mt-2 border-b-[0.5px] border-surface-500">
							<p class="">Sessions currently in use:</p>
							<p class="ml-5 mr-10 font-bold">{$sessionsInUse} / {$sessionsNumber}</p>
						</div>
						<div class="flex bg-surface-900 p-2 mt-2">
							<p class="">Max http session inactivity :</p>
							<p class="ml-5 mr-10 font-bold">{$httpTimeout}</p>
						</div>
					</div>
				</div>
			</Card>

			<Card>
				<h1 class="text-[15px] mb-2">Legends</h1>
				<div class="bg-surface-900 p-2 mt-2 border-[0.5px] border-surface-600">
					{#each legendItems as legend}
						<div class={`flex items-center pl-2 p-1 border-b border-surface-800 justify-between`}>
							<p class="mr-5">{legend.title}</p>
							<div class="flex">
								<Icon icon={legend.icon} class="w-6 h-6" />
								<Icon icon={legend.icon2} class="w-6 h-6" />
							</div>
						</div>
					{/each}
				</div>
			</Card>
		</AutoGrid>

		<div class="mt-10">
			<Card>
				<h1 class="text-[15px]">Sessions</h1>

				<table class="mt-5 w-full bg-surface-700">
					<thead>
						<tr class="bg-surface-900">
							<th class="px-4 py-2">ID</th>
							<th class="px-4 py-2">Contexts</th>
							<th class="px-4 py-2">User</th>
							<th class="px-4 py-2">Roles</th>
							<th class="px-4 py-2">UUID</th>
							<th class="px-4 py-2">FS</th>
							<th class="px-4 py-2">
								<Icon icon="material-symbols-light:date-range-rounded" class="w-6 h-6" /></th
							>
							<th class="px-4 py-2"
								><Icon icon="carbon:intent-request-inactive" class="w-6 h-6" /></th
							>
							<th class="px-4 py-2">Client IP</th>
						</tr>
					</thead>
					<tbody>
						{#if $sessionsStore.length >= 0}
							{#each $sessionsStore as session}
								<tr>
									<td class="border px-4 py-2">{session['@_sessionID']}</td>
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
					</tbody>
				</table>
			</Card>
		</div>

		<div class="mt-10">
			<Card>
				<h1 class="text-[15px]">Contexts</h1>

				<table class="mt-5 w-full bg-surface-700">
					<thead>
						<tr class="bg-surface-900">
							<th class="px-4 py-2">Context</th>
							<th class="px-4 py-2">Project</th>
							<th class="px-4 py-2">Connector</th>
							<th class="px-4 py-2">Requested</th>
							<th class="px-4 py-2">Status</th>
							<th class="px-4 py-2">User</th>
							<th class="px-4 py-2">Client Computer</th>
						</tr>
					</thead>
					<tbody>
						{#if $connectionsStore.length >= 0}
							{#each $connectionsStore as connection}
								<tr>
									<td class="border px-4 py-2">{connection['@_contextName']}</td>
									<td>{connection['@_project']}</td>
									<td>{connection['@_connector']}</td>
									<td>{connection['@_requested']}</td>
									<td>{connection['@_status']}</td>
									<td>{connection['@_user']}</td>
									<td>{connection['@_clientComputer']}</td>
								</tr>
							{/each}
						{/if}
					</tbody>
				</table>
			</Card>
		</div>
	</div>
</div>

<style>
	th,
	td {
		border: 1px solid #616161;
		padding: 4px;
		text-align: left;
		font-weight: 300;
		font-size: 13px;
	}

	table {
		border-collapse: collapse;
	}
</style>
