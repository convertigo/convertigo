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
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';

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

<AutoGrid>
	<Card title="Status">
		{#snippet cornerOption()}
			<ButtonsContainer>
				<button class="basic-button" onclick={() => modal('props')}>Java System Properties</button>
				<button class="basic-button" onclick={() => modal('env')}>Environment Variables</button>
			</ButtonsContainer>
		{/snippet}
		<StatusTable
			class="mt-5"
			time={$monitorData.time}
			startTime={$monitorData.startTime}
			engineState={$monitorData.engineState}
		/>
	</Card>

	<Card title="System Information">
		{#snippet cornerOption()}
			<button onclick={performGC} class="green-button">Perform GC</button>
		{/snippet}
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
