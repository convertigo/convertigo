<script>
	import { onMount } from 'svelte';
	import { fetchEngineStatus } from '../stores/Store';

	let statusData = null;

	async function updateStatus() {
		const response = await fetchEngineStatus();
		statusData = response.admin;
	}

	onMount(() => {
		updateStatus();
		const interval = setInterval(updateStatus, 3000);

		return () => {
			clearInterval(interval);
		};
	});
</script>

{#if statusData}
	<div class="status-table p-2">
		<table>
			<tr>
				<th>Convertigo Version</th>
				<td>{statusData.version['@_product']}</td>
			</tr>
			<tr>
				<th>Engine State</th>
				<td>{statusData.engineState}</td>
			</tr>
			<tr>
				<th>Last Startup</th>
				<td>{statusData.startStopDate['@_localeFormatted']}</td>
			</tr>
			<tr>
				<th>Uptime</th>
				<td>
					{statusData.runningElapse['@_days']} days,
					{statusData.runningElapse['@_hours']} hours,
					{statusData.runningElapse['@_minutes']} minutes,
					{statusData.runningElapse['@_seconds']} seconds
				</td>
			</tr>
			<tr>
				<th>Current Time</th>
				<td>{statusData.time['@_localeFormatted']}</td>
			</tr>
			<tr>
				<th>Mode</th>
				<td>{statusData.mode}</td>
			</tr>
			<!-- Ajoutez d'autres lignes ici selon la structure de vos données -->
		</table>
	</div>
{:else}
	<p>Loading engine status...</p>
{/if}

<style>
	.system-information-table {
		width: 100%;
		margin: auto;
	}

	table {
		width: 100%;
		border-collapse: collapse;
	}
	th,
	td {
		padding: 4px;
		text-align: left;
		font-weight: 300;
		font-size: 13px;
	}
	th {
		@apply bg-surface-800 border-r-2 border-surface-900;
	}
	tr {
		@apply border-b-2 border-surface-900;
	}
</style>
