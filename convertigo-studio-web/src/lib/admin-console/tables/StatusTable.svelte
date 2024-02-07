<script>
	import { onMount } from 'svelte';
	import {
		statusCheck,
		product,
		licenceType,
		licenceNumber,
		licenceExpired
	} from '../stores/statusStore';
	export let time;
	export let startTime;
	export let engineState;

	onMount(() => {
		statusCheck();
	});
</script>

<div class="status-table p-2">
	<table>
		<tr>
			<th>Engine State</th>
			{#if engineState}
				<td style="background-color:lightgreen; color: black">Running</td>
			{:else if engineState == false}
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
			<td>{new Date(startTime).toLocaleString()}</td>
		</tr>
		<tr>
			<th>Uptime</th>
			<td>{new Date(time - startTime).toLocaleTimeString()}</td>
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

<style lang="postcss">
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
