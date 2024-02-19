<script>
	import { onMount } from 'svelte';
	import ApexChartLineAdmin from '$lib/admin/charts/ApexChartLineAdmin.svelte';
	import StatusTable from '$lib/admin/tables/StatusTable.svelte';
	import SystemInformationTable from '$lib/admin/tables/SystemInformationTable.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import AutoGrid from '$lib/admin/components/AutoGrid.svelte';
	import { monitorCheck, isLoading, monitorData } from '$lib/admin/stores/monitorStore';
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
	/**
	 * @type {never[]}
	 */
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
