<script>
	import { onMount } from 'svelte';
	import ApexChartLineAdmin from '$lib/admin/charts/ApexChartLineAdmin.svelte';
	import StatusTable from '$lib/admin/tables/StatusTable.svelte';
	import SystemInformationTable from '$lib/admin/tables/SystemInformationTable.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import AutoGrid from '$lib/admin/components/AutoGrid.svelte';
	import { monitorCheck, isLoading, monitorData } from '$lib/admin/stores/monitorStore';
	import { call } from '$lib/utils/service';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import ModalHome from '$lib/admin/modals/ModalHome.svelte';

	const homeModalStore = getModalStore();

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
		const res = await call('engine.PerformGC');
	}

	async function javaSystemPropModal() {
		homeModalStore.trigger({
			type: 'component',
			component: { ref: ModalHome },
			meta: { mode: 'Java System Prop' }
		});
	}

	async function environmentVariablesModal() {
		homeModalStore.trigger({
			type: 'component',
			component: { ref: ModalHome },
			meta: { mode: 'Environment Variables' }
		});
	}
</script>

<AutoGrid>
	<Card title="Status">
		<div slot="cornerOption">
			<div class="flex gap-5">
				<button class="w-full bg-primary-400-500-token" on:click={javaSystemPropModal}
					>Java System Properties</button
				>
				<button class="w-full bg-primary-400-500-token" on:click={environmentVariablesModal}
					>Environment Variables</button
				>
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
</AutoGrid>

<div class="mt-5">
	<AutoGrid>
		{#each charts as chart}
			<Card>
				<ApexChartLineAdmin {...chart} {isLoading} {categories} />
			</Card>
		{/each}
	</AutoGrid>
</div>
