<script>
	import { onMount } from 'svelte';
	import { fetchSystemInformation } from '../stores/Store';

	let systemInfo = null;

	async function updateSystemInfo() {
		const response = await fetchSystemInformation();
		systemInfo = response.admin; // Prenez les données de l'objet 'admin'
	}

	onMount(() => {
		updateSystemInfo();
		const interval = setInterval(updateSystemInfo, 30000); // Mise à jour toutes les 30 secondes

		return () => {
			clearInterval(interval); // Nettoyage de l'intervalle lors de la destruction du composant
		};
	});
</script>

{#if systemInfo}
	<div class="system-information-table p-1">
		<table>
			<tr>
				<th>Host Name</th>
				<td>{systemInfo.host['@_name']}</td>
			</tr>
			<tr>
				<th>CPU</th>
				<td
					>{systemInfo.os['@_architecture']} architecture, {systemInfo.os['@_availableProcessors']} processor(s)</td
				>
			</tr>
			<tr>
				<th>OS</th>
				<td>{systemInfo.os['@_name']} {systemInfo.os['@_version']}</td>
			</tr>
			<tr>
				<th>JAVA</th>
				<td
					>{systemInfo.java['@_classVersion']}
					{systemInfo.java['@_vendor']}
					{systemInfo.java['@_version']}</td
				>
			</tr>
			<tr>
				<th>Available memory</th>
				<td>{(systemInfo.memory['@_available'] / 1048576).toFixed(2)} MB</td>
			</tr>
			<tr>
				<th>Maximum memory</th>
				<td>{(systemInfo.memory['@_maximal'] / 1048576).toFixed(2)} MB</td>
			</tr>
			<tr>
				<th>Total memory</th>
				<td>{(systemInfo.memory['@_total'] / 1048576).toFixed(2)} MB</td>
			</tr>
			<tr>
				<th>Your browser</th>
				<td>{systemInfo.browser}</td>
			</tr>
		</table>
	</div>
{:else}
	<p>Loading system information...</p>
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
		border: 1px solid #616161;
		padding: 4px;
		text-align: left;
		font-weight: 300;
		font-size: 13px;
	}
	th {
		@apply bg-surface-800;
	}
</style>
