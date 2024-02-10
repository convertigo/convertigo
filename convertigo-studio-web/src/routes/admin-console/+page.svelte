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
	<Card title="Status">
		<StatusTable
			time={$monitorData.time}
			startTime={$monitorData.startTime}
			engineState={$monitorData.engineState}
		/>
	</Card>

	<Card title="System Information">
		<SystemInformationTable
			memoryMaximal={$monitorData.memoryMaximal}
			memoryTotal={$monitorData.memoryTotal}
			memoryUsed={$monitorData.memoryUsed}
		/>
	</Card>
</AutoGrid>

<div class="mt-5">
	<AutoGrid>
		{#each charts as chart}
			<Card customStyle="mb-10">
				<ApexChartLineAdmin {...chart} {isLoading} {categories} />
			</Card>
		{/each}
	</AutoGrid>
</div>

<style lang="postcss">
	.table-container {
		overflow-x: auto;
		margin: 0 auto;
		max-width: 100%;
	}

	table {
		width: 100%;
		border-collapse: collapse;
		@apply rounded-xl;
	}

	@media screen and (max-width: 600px) {
		.table-container {
			-webkit-overflow-scrolling: touch;
		}
	}
</style>
