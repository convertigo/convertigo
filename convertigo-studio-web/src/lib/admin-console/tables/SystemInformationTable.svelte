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
	import { monitorCheck, memoryMaximal, memoryTotal, memoryUsed } from '../stores/monitorStore';

	onMount(() => {
		statusCheck();
		monitorCheck();
	});
</script>

<div class="system-information-table p-2">
	<table>
		<tr>
			<th>Host Name</th>
			<td>{$hostName}</td>
		</tr>
		<tr>
			<th>CPU</th>
			<td
				>{$osArchitecture} architecture, {$osAvailableProcessors} processor{$osAvailableProcessors >
				1
					? 's'
					: ''}</td
			>
		</tr>
		<tr>
			<th>OS</th>
			<td>{$osName} {$osVersion}</td>
		</tr>
		<tr>
			<th>Java Vendor</th>
			<td>{$javaVendor}</td>
		</tr>
		<tr>
			<th>Java</th>
			<td>{$javaVersion} (classes {$javaClassVersion})</td>
		</tr>
		<tr>
			<th>Used Memory</th>
			<td>{$memoryUsed[$memoryUsed.length - 1]} MB</td>
		</tr>
		<tr>
			<th>Total memory</th>
			<td>{$memoryTotal[$memoryTotal.length - 1]} MB</td>
		</tr>
		<tr>
			<th>Maximum memory</th>
			<td>{$memoryMaximal[$memoryMaximal.length - 1]} MB</td>
		</tr>
		<tr>
			<th>Your browser</th>
			<td>{$browser}</td>
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
