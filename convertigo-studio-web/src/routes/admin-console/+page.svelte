<script lang="ts">
	import { initializeStores, localStorageStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import MonitorChart from '$lib/admin-console/charts/MonitorChart.svelte';
	import ThreadsChart from '$lib/admin-console/charts/ThreadsChart.svelte';
	import ContextChart from '$lib/admin-console/charts/ContextChart.svelte';
	import RequestDurationChart from '$lib/admin-console/charts/RequestDurationChart.svelte';
	import StatusTable from '$lib/admin-console/tables/StatusTable.svelte';
	import SystemInformationTable from '$lib/admin-console/tables/SystemInformationTable.svelte';

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
	<div class="charts-grid">
		<div class="chart-card">
			<div class="card-header">Status</div>
			<StatusTable />
		</div>

		<div class="chart-card">
			<div class="card-header">System Information</div>
			<SystemInformationTable />
		</div>

		<!-- Répétez ce pattern pour les autres cartes -->
	</div>

	<div class="monitor-section">
		<div class="header-section">Monitor</div>

		<div class="monitor-content">
			<div class="card">
				<h1>Memory</h1>
				<MonitorChart />
			</div>

			<div class="card">
				<h1>Threads</h1>
				<ThreadsChart />
			</div>

			<div class="card">
				<h1>Threads</h1>
				<ContextChart />
			</div>

			<div class="card">
				<h1>Threads</h1>
				<RequestDurationChart />
			</div>
		</div>
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
