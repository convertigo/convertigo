<script lang="ts">
	import { initializeStores, localStorageStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import ApexChartLineAdmin from '$lib/admin-console/charts/ApexChartLineAdmin.svelte';
	import StatusTable from '$lib/admin-console/tables/StatusTable.svelte';
	import SystemInformationTable from '$lib/admin-console/tables/SystemInformationTable.svelte';
	import Card from '$lib/admin-console/admin-components/Card.svelte';
	import AutoGrid from '$lib/admin-console/admin-components/AutoGrid.svelte';
	import { monitorCheck, isLoading, monitorData } from '$lib/admin-console/stores/monitorStore';
	initializeStores();
	let theme = localStorageStore('studio.theme', 'skeleton');
	onMount(() => {
		changeTheme($theme);
		document.body.setAttribute('data-theme', 'dark-theme');
		monitorCheck();
	});
	function changeTheme(e) {
		$theme = typeof e == 'string' ? e : e.target?.value;
		document.body.setAttribute('data-theme', $theme);
	}
</script>

<div class="page-container">
	<div class="charts-grid"></div>

	<AutoGrid>
		<Card>
			<div class="card-header">Status</div>
			<StatusTable time={$monitorData.time} startTime={$monitorData.startTime} engineState={$monitorData.engineState} />
		</Card>

		<Card>
			<div class="card-header">System Information</div>
			<SystemInformationTable memoryMaximal={$monitorData.memoryMaximal} memoryTotal={$monitorData.memoryTotal} memoryUsed={$monitorData.memoryUsed}/>
		</Card>
	</AutoGrid>

	<div class="monitor-section">
		<div class="header-section">Monitor</div>

		<AutoGrid>
			<Card customStyle="height: 300px;">
				<ApexChartLineAdmin 
					_title="Memory"
					_isLoading={isLoading}
					_series={[
						{name: 'Memory maximal', data: $monitorData.memoryMaximal},
						{name: 'Memory total', data: $monitorData.memoryTotal},
						{name: 'Memory used', data: $monitorData.memoryUsed}
					]}
					_labels={$monitorData.labels}
				/>
			</Card>

			<Card customStyle="height: 300px;">
				<ApexChartLineAdmin 
					_title="Threads"
					_isLoading={isLoading}
					_series={[
						{name: 'Threads', data: $monitorData.threads},
					]}
					_labels={$monitorData.labels}
				/>
			</Card>

			<Card customStyle="height: 300px;">
				<ApexChartLineAdmin 
					_title="Contexts"
					_isLoading={isLoading}
					_series={[
						{name: 'Contexts', data: $monitorData.contexts},
					]}
					_labels={$monitorData.labels}
				/>
			</Card>

			<Card customStyle="height: 300px;">
				<ApexChartLineAdmin 
					_title="Requests duration"
					_isLoading={isLoading}
					_series={[
						{name: 'Requests duration', data: $monitorData.requests},
					]}
					_labels={$monitorData.labels}
				/>
			</Card>

			<Card customStyle="height: 300px;">
				<ApexChartLineAdmin 
					_title="Sessions"
					_isLoading={isLoading}
					_series={[
						{name: 'Max sessions', data: $monitorData.sessionMaxCV},
						{name: 'Current sessions', data: $monitorData.sessions},
						{name: 'Available sessions', data: $monitorData.availableSessions}
					]}
					_labels={$monitorData.labels}
				/>
			</Card>
		</AutoGrid>
	</div>
</div>

<style>
	.page-container {
		@apply h-full flex flex-col p-10;
	}
	.charts-grid {
		@apply flex flex-col grid grid-cols-2 gap-10;
	}
	.chart-card {
		@apply h-auto bg-surface-800 border-[0.5px] border-surface-600;
	}
	.card-header {
		@apply w-full p-2 bg-surface-800 flex flex-col font-light;
	}
	.header-section {
		@apply w-full p-2 bg-surface-800 flex flex-col font-light mt-10 mb-10;
	}
	.monitor-section {
		@apply flex flex-col h-auto;
	}
	.monitor-content {
		@apply flex flex-col grid grid-cols-2 gap-10;
	}
	.card {
		@apply flex flex-col bg-surface-800 h-60 p-5 font-extralight text-[13.5px] border-[0.5px] border-surface-600 rounded-none;
	}
</style>
