<script>
	import { onMount } from 'svelte';
	import { fetchConnectionsList } from '../../../adminconsol-lib/stores/Store';
	import { localStorageStore } from '@skeletonlabs/skeleton';

	let connections = {}; // Initialize as an object
	let theme = localStorageStore('studio.theme', 'skeleton');

	onMount(() => {
		changeTheme($theme);
		document.body.setAttribute('data-theme', 'dark-theme');

		updateConnections();
		const interval = setInterval(updateConnections, 3000); // Pass the correct function reference

		return () => {
			clearInterval(interval);
		};
	});

	function changeTheme(e) {
		$theme = typeof e == 'string' ? e : e.target?.value;
		document.body.setAttribute('data-theme', $theme);
	}

	async function updateConnections() {
		try {
			const response = await fetchConnectionsList();
			connections = response.admin || {}; // Safely assign if response.admin exists
		} catch (error) {
			console.error('Error fetching connections list:', error);
		}
	}
</script>

<div class="flex flex-col h-full p-10 w-full">
	{#if Object.keys(connections).length}
		<!-- Now we check if connections object has keys (properties), meaning it's not empty -->
		<div class=" p-1">
			<h1 class="text-[15px]">Connections</h1>

			<table class="text-start flex flex-col mt-10">
				<tr>
					<th class="w-60">Contexts In Use : </th>
					<td class="w-20">{connections.contextsInUse} / {connections.contextsNumber}</td>
				</tr>
				<tr>
					<th class="w-60">Threads currently In Use : </th>
					<td class="w-20">{connections.threadsInUse} / {connections.threadsNumber}</td>
				</tr>
				<tr>
					<th class="w-60">Threads currently In Use : </th>
					<td class="w-20">{connections.sessionsInUse} / {connections.sessionsNumber}</td>
				</tr>
				<tr>
					<th class="w-60">Max http session inactivity : </th>
					<td class="w-20">{connections.httpTimeout}</td>
				</tr>
				<!-- Add more rows here to display other data from the connections object -->
			</table>

			<h1 class="text-[15px] mt-10">Legend</h1>

			<table class="mt-5 w-full">
				<thead>
					<tr class="bg-gray-700">
						<th class="px-4 py-2">ID</th>
						<th class="px-4 py-2">Contexts</th>
						<th class="px-4 py-2">User</th>
						<th class="px-4 py-2">Roles</th>
						<th class="px-4 py-2">UUID</th>
						<th class="px-4 py-2">FS</th>
						<th class="px-4 py-2">Client IP</th>
						<!-- Add more <th> elements as needed -->
					</tr>
				</thead>
				<tbody>
					<tr>
						<td class="border px-4 py-2">{connections.id}</td>
						<td class="border px-4 py-2"
							>{connections.contextsInUse} / {connections.contextsNumber}</td
						>
						<td class="border px-4 py-2">{connections.user}</td>
						<td class="border px-4 py-2">{connections.roles}</td>
						<td class="border px-4 py-2">{connections.uuid}</td>
						<td class="border px-4 py-2">{connections.fs}</td>
						<td class="border px-4 py-2">{connections.clientIp}</td>
						<!-- Add more <td> elements as needed -->
					</tr>
				</tbody>
			</table>

			<h1 class="text-[15px] mt-10">Sessions</h1>

			<table class="mt-5 w-full">
				<thead>
					<tr class="bg-gray-700">
						<th class="px-4 py-2">Context</th>
						<th class="px-4 py-2">Project</th>
						<th class="px-4 py-2">Connector</th>
						<th class="px-4 py-2">Requested</th>
						<th class="px-4 py-2">Status</th>
						<th class="px-4 py-2">User</th>
						<th class="px-4 py-2">Client Computer</th>
						<!-- Add more <th> elements as needed -->
					</tr>
				</thead>
				<tbody>
					<tr>
						<td class="border px-4 py-2">{connections.id}</td>
						<td class="border px-4 py-2"
							>{connections.contextsInUse} / {connections.contextsNumber}</td
						>
						<td class="border px-4 py-2">{connections.user}</td>
						<td class="border px-4 py-2">{connections.roles}</td>
						<td class="border px-4 py-2">{connections.uuid}</td>
						<td class="border px-4 py-2">{connections.fs}</td>
						<td class="border px-4 py-2">{connections.clientIp}</td>
						<!-- Add more <td> elements as needed -->
					</tr>
				</tbody>
			</table>
		</div>
	{:else}
		<div>No connection data available.</div>
	{/if}
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
