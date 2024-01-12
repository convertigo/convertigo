<script lang="ts">
	import { initializeStores, localStorageStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import MonitorChart from '$lib/admin-console/charts/MonitorChart.svelte';
	import SessionsChart from '$lib/admin-console/charts/SessionsChart.svelte';
	import ThreadsChart from '$lib/admin-console/charts/ThreadsChart.svelte';
	import ContextChart from '$lib/admin-console/charts/ContextChart.svelte';
	import RequestDurationChart from '$lib/admin-console/charts/RequestDurationChart.svelte';
	import StatusTable from '$lib/admin-console/tables/StatusTable.svelte';
	import SystemInformationTable from '$lib/admin-console/tables/SystemInformationTable.svelte';
	import Card from '$lib/admin-console/admin-components/Card.svelte';
	import AutoGrid from '$lib/admin-console/admin-components/AutoGrid.svelte';

	initializeStores();

	let theme = localStorageStore('studio.theme', 'skeleton');

	onMount(() => {
		changeTheme($theme);
		document.body.setAttribute('data-theme', 'dark-theme');
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
			<StatusTable />
		</Card>

		<Card>
			<div class="card-header">System Information</div>
			<SystemInformationTable />
		</Card>
	</AutoGrid>

	<div class="monitor-section">
		<div class="header-section">Monitor</div>

		<AutoGrid>
			<Card customStyle="height: 300px;">
				<h1>Memory</h1>
				<MonitorChart />
			</Card>

			<Card customStyle="height: 300px;">
				<h1>Threads</h1>
				<ThreadsChart />
			</Card>

			<Card customStyle="height: 300px;">
				<h1>Contexts</h1>
				<ContextChart />
			</Card>

			<Card customStyle="height: 300px;">
				<h1>Requests duration</h1>
				<RequestDurationChart />
			</Card>

			<Card customStyle="height: 300px;">
				<h1>Sessions</h1>
				<SessionsChart />
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
