<script>
	import { onMount } from 'svelte';
	import {
		statusCheck,
		locale,
		timezone,
		product,
		beans,
		licenceType,
		licenceNumber,
		licenceEnd,
		licenceExpired,
		javaVersion,
		javaClassVersion,
		javaVendor,
		hostName,
		hostAddresses,
		osName,
		osVersion,
		osArchitecture,
		osAvailableProcessors,
		browser,
		cloud
	} from '../stores/statusStore';
	import { monitorCheck, time, startTime, engineState } from '../stores/monitorStore';

	onMount(() => {
		statusCheck();
		monitorCheck();
	});
</script>

<div class="status-table p-2">
	<table>
		<!-- <tr>
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
		</tr> -->
		<tr>
			<th>Engine State</th>
			{#if $engineState}
				<td style="background-color:lightgreen; color: black">Running</td>
			{:else if $engineState == false}
				<td style="background-color:lightcoral; color: black">Stopped</td>
			{:else}
				<td></td>
			{/if}
		</tr>
		<tr>
			<th>Convertigo Version</th>
			<td>{$product}</td>
		</tr>
		<tr>
			<th>Last Startup</th>
			<td>{new Date($startTime).toLocaleString()}</td>
		</tr>
		<tr>
			<th>Uptime</th>
			<td>{new Date($time - $startTime).toLocaleTimeString()}</td>
		</tr>
		<tr>
			<th>Licence Type</th>
			<td>{$licenceType}</td>
		</tr>
		<tr>
			<th>Licence n°</th>
			<td>{$licenceNumber}</td>
		</tr>
		<tr>
			<th>License expiration date</th>
			<td>{$licenceExpired}</td>
		</tr>
	</table>
</div>

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
