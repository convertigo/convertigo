<script>
	import { onMount } from 'svelte';
	import ApexChartLineAdmin from '$lib/admin/charts/ApexChartLineAdmin.svelte';
	import StatusTable from '$lib/admin/tables/StatusTable.svelte';
	import SystemInformationTable from '$lib/admin/tables/SystemInformationTable.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import AutoGrid from '$lib/admin/components/AutoGrid.svelte';
	import { monitorCheck, isLoading, monitorData } from '$lib/admin/stores/monitorStore';
	import { call } from '$lib/utils/service';
	import { getModalStore, getToastStore } from '@skeletonlabs/skeleton';

	const modalStore = getModalStore();
	const toastStore = getToastStore();

	onMount(() => {
		monitorCheck();
	});

	const charts = [
		{
			title: 'Memory',
			series: [
				{ name: 'Memory Maximal', data: $monitorData.memoryMaximal },
				{ name: 'Memory Total', data: $monitorData.memoryTotal },
				{ name: 'Memory Used', data: $monitorData.memoryUsed }
			]
		},
		{ title: 'Threads', series: [{ name: 'Threads', data: $monitorData.threads }] },
		{ title: 'Contexts', series: [{ name: 'Contexts', data: $monitorData.contexts }] },
		{
			title: 'Requests Duration',
			series: [{ name: 'Requests Duration', data: $monitorData.requests }]
		},
		{
			title: 'Sessions',
			series: [
				{ name: 'Max Sessions', data: $monitorData.sessionMaxCV },
				{ name: 'Current Sessions', data: $monitorData.sessions },
				{ name: 'Available Sessions', data: $monitorData.availableSessions }
			]
		}
	];
	/**
	 * @type {never[]}
	 */
	$: categories = $monitorData.labels;

	async function performGC() {
		try {
			const res = await call('engine.PerformGC');
			toastStore.trigger({
				message: 'GC performed successfully',
				timeout: 8000,
				background: 'bg-success-400-500-token'
			});
			console.log(res);
		} catch (err) {
			toastStore.trigger({
				message: 'An error occurred while performing GC',
				timeout: 8000,
				background: 'bg-error-400-500-token'
			});
		}
	}

	/**
	 * @param {string} mode
	 */
	function modal(mode) {
		modalStore.trigger({
			type: 'component',
			component: 'modalHome',
			meta: { mode }
		});
	}
</script>

<AutoGrid>
	<Card title="Status">
		<div slot="cornerOption">
			<div class="flex flex-wrap gap-2 pl-5">
				<div class="flex-1">
					<button class="bg-primary-400-500-token w-full" on:click={() => modal('props')}
						>Java System Properties</button
					>
				</div>
				<div class="flex-1">
					<button class="bg-primary-400-500-token w-full" on:click={() => modal('env')}
						>Environment Variables</button
					>
				</div>
			</div>
		</div>
		<StatusTable
			class="mt-5"
			time={$monitorData.time}
			startTime={$monitorData.startTime}
			engineState={$monitorData.engineState}
		/>
	</Card>

	<Card title="System Information">
		<div slot="cornerOption">
			<button on:click={performGC} class="w-full bg-secondary-400-500-token">Perform GC</button>
		</div>
		<SystemInformationTable
			class="mt-5"
			memoryMaximal={$monitorData.memoryMaximal}
			memoryTotal={$monitorData.memoryTotal}
			memoryUsed={$monitorData.memoryUsed}
		/>
	</Card>

	{#each charts as chart}
		<Card class="max-h-[300px]">
			<ApexChartLineAdmin {...chart} {isLoading} {categories} />
		</Card>
	{/each}
</AutoGrid>
