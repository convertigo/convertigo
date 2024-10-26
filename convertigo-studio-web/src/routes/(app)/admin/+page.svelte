<script>
	import { onMount } from 'svelte';
	import ApexChartLineAdmin from '$lib/admin/charts/ApexChartLineAdmin.svelte';
	import StatusTable from '$lib/admin/tables/StatusTable.svelte';
	import SystemInformationTable from '$lib/admin/tables/SystemInformationTable.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import { monitorCheck, isLoading, monitorData } from '$lib/admin/stores/monitorStore';
	import { call } from '$lib/utils/service';
	import { getModalStore, getToastStore } from '@skeletonlabs/skeleton';

	const modalStore = getModalStore();
	const toastStore = getToastStore();

	onMount(() => {
		monitorCheck();
	});

	const charts = $derived([
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
	]);
	/**
	 * @type {never[]}
	 */
	let categories = $derived($monitorData.labels);

	async function performGC() {
		try {
			const res = await call('engine.PerformGC');
			toastStore.trigger({
				message: 'GC performed successfully',
				timeout: 8000,
				background: 'bg-success-400-500-token'
			});
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

<div class="layout-y md:layout-x !items-start">
	<div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-1 gap md:auto-rows-min">
		<Card title="Status" class="max-w-[600px]">
			{#snippet cornerOption()}
				<div class="layout-grid-low-[100px]">
					<button class="basic-button text-wrap w-full" onclick={() => modal('props')}
						>Java System Properties</button
					>
					<button class="basic-button text-wrap w-full" onclick={() => modal('env')}
						>Environment Variables</button
					>
				</div>
			{/snippet}
			<StatusTable
				class="mt-5"
				time={$monitorData.time}
				startTime={$monitorData.startTime}
				engineState={$monitorData.engineState}
			/>
		</Card>

		<Card title="System Information" class="max-w-[600px]">
			{#snippet cornerOption()}
				<div class="layout-grid-wrap-low">
					<button onclick={performGC} class="green-button text-wrap w-full">Perform GC</button>
				</div>
			{/snippet}
			<SystemInformationTable
				class="mt-5"
				memoryMaximal={$monitorData.memoryMaximal}
				memoryTotal={$monitorData.memoryTotal}
				memoryUsed={$monitorData.memoryUsed}
			/>
		</Card>
	</div>
	<div class="layout-grid-[350px] w-full">
		{#each charts as chart}
			<Card class="w-full">
				<ApexChartLineAdmin {...chart} {isLoading} {categories} />
			</Card>
		{/each}
	</div>
</div>
