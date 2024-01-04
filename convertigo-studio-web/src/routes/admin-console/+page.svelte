<script lang="ts">
	import { initializeStores, localStorageStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import MonitorChart from '$lib/admin-console/charts/MonitorChart.svelte';
	import ThreadsChart from '$lib/admin-console/charts/ThreadsChart.svelte';
	import ContextChart from '$lib/admin-console/charts/ContextChart.svelte';
	import RequestDurationChart from '$lib/admin-console/charts/RequestDurationChart.svelte';
	import StatusTable from '$lib/admin-console/tables/StatusTable.svelte';
	import SystemInformationTable from '$lib/admin-console/tables/SystemInformationTable.svelte';
	import { fetchEngineMonitorData } from '$lib/admin-console/stores/Store';

	initializeStores();

	let theme = localStorageStore('studio.theme', 'skeleton');

	onMount(() => {
		changeTheme($theme);
		document.body.setAttribute('data-theme', 'dark-theme');

		fetchEngineMonitorData();

		const interval = setInterval(fetchEngineMonitorData, 2000);

		return () => {
			clearInterval(interval);
		};
	});

	function changeTheme(e) {
		$theme = typeof e == 'string' ? e : e.target?.value;
		document.body.setAttribute('data-theme', $theme);
	}
</script>

<div class="h-full flex flex-col p-10">
	<div class="flex flex-col grid grid-cols-2 gap-10">
		<div class="h-auto">
			<verticalCards class="grid grid-cols-1 flex flex-col gap-5">
				<cardStatus class="col-span-1 h-auto bg-surface-600">
					<cardBar class="w-full p-2 bg-surface-600 flex flex-col">
						<h1 class="font-light">Status</h1>
					</cardBar>
					<StatusTable />
				</cardStatus>

				<cardSystem class="col-span-1 h-auto mt-5 bg-surface-600">
					<cardBar class="w-full p-2 bg-surface-600 flex flex-col">
						<h1 class="font-light">System Information</h1>
					</cardBar>
					<SystemInformationTable />
				</cardSystem>
			</verticalCards>
		</div>

		<cardMonitor class="h-auto bg-surface-500 flex flex-col">
			<cardBar class="w-full p-2 bg-surface-600 flex flex-col">
				<h1 class="font-light">Monitor</h1>
			</cardBar>

			<content class="mb-10">
				<cardMemory class="flex flex-col h-60 bg-surface-500 p-5">
					<h1 class="font-extralight text-[13.5px]">Memory</h1>
					<MonitorChart />
				</cardMemory>

				<cardThreads class="flex flex-col bg-surface-500 h-60 p-5 font-extralight text-[13.5px]">
					<h1>Threads</h1>
					<ThreadsChart />
				</cardThreads>

				<cardContexts class="flex flex-col h-60 bg-surface-500 p-5">
					<h1 class="font-extralight text-[13.5px]">Contexts</h1>
					<ContextChart />
				</cardContexts>

				<cardRequestDuration class="flex flex-col h-60 bg-surface-500 p-5">
					<h1 class="font-extralight text-[13.5px]">Request duration</h1>
					<RequestDurationChart />
				</cardRequestDuration>
			</content>
		</cardMonitor>
	</div>
</div>
