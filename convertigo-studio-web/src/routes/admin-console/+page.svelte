<script>
	import { onMount } from 'svelte';
	import ApexChartLineAdmin from '$lib/admin-console/charts/ApexChartLineAdmin.svelte';
	import StatusTable from '$lib/admin-console/tables/StatusTable.svelte';
	import SystemInformationTable from '$lib/admin-console/tables/SystemInformationTable.svelte';
	import Card from '$lib/admin-console/admin-components/Card.svelte';
	import AutoGrid from '$lib/admin-console/admin-components/AutoGrid.svelte';
	import { monitorCheck, isLoading, monitorData } from '$lib/admin-console/stores/monitorStore';
	onMount(() => {
		monitorCheck();
	});

	const charts = [
		{
			title: 'Memory',
			series: [
				{ name: 'Memory maximal', data: $monitorData.memoryMaximal },
				{ name: 'Memory total', data: $monitorData.memoryTotal },
				{ name: 'Memory used', data: $monitorData.memoryUsed }
			]
		},
		{ title: 'Threads', series: [{ name: 'Threads', data: $monitorData.threads }] },
		{ title: 'Contexts', series: [{ name: 'Contexts', data: $monitorData.contexts }] },
		{
			title: 'Requests duration',
			series: [{ name: 'Requests duration', data: $monitorData.requests }]
		},
		{
			title: 'Sessions',
			series: [
				{ name: 'Max sessions', data: $monitorData.sessionMaxCV },
				{ name: 'Current sessions', data: $monitorData.sessions },
				{ name: 'Available sessions', data: $monitorData.availableSessions }
			]
		}
	];
	$: categories = $monitorData.labels;
</script>

<AutoGrid>
	<Card>
		<div class="card-header">Status</div>
		<StatusTable
			time={$monitorData.time}
			startTime={$monitorData.startTime}
			engineState={$monitorData.engineState}
		/>
	</Card>

	<Card>
		<div class="card-header">System Information</div>
		<SystemInformationTable
			memoryMaximal={$monitorData.memoryMaximal}
			memoryTotal={$monitorData.memoryTotal}
			memoryUsed={$monitorData.memoryUsed}
		/>
	</Card>
</AutoGrid>

<div class="monitor-section">
	<div class="header-section">Monitor</div>

	<AutoGrid>
		{#each charts as chart}
			<Card customStyle="height: 300px;">
				<ApexChartLineAdmin {...chart} {isLoading} {categories} />
			</Card>
		{/each}
	</AutoGrid>
</div>

<style lang="postcss">
	.card-header {
		@apply w-full p-2 bg-surface-800 flex flex-col font-light;
	}
	.header-section {
		@apply w-full p-2 bg-surface-800 flex flex-col font-light mt-10 mb-10;
	}
	.monitor-section {
		@apply flex flex-col h-auto;
	}
</style>
