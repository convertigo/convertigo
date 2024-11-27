<script>
	import ApexChartLineAdmin from '$lib/admin/components/ApexChartLineAdmin.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import { call } from '$lib/utils/service';
	import Status from '$lib/common/Status.svelte';
	import Monitor from '$lib/admin/Monitor.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Time from '$lib/common/Time.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import EnvironmentVariables from '$lib/admin/EnvironmentVariables.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';

	const tables = $derived([
		{
			title: 'Status',
			buttons: [
				{
					label: 'Java Properties',
					icon: 'mdi:language-java',
					cls: 'basic-button',
					onclick: (e) => modal(e, 'props')
				},
				{
					label: 'Environment Variables',
					icon: 'mdi:code-block-braces',
					cls: 'basic-button',
					onclick: (e) => modal(e, 'env')
				}
			],
			data: [
				{
					Name: 'Engine State',
					Value: Monitor.engineState ? 'Running' : Monitor.engineState == null ? null : 'Stopped'
				},
				{ Name: 'Convertigo Version', Value: Status.product },
				{
					Name: 'Last Startup',
					Value: Monitor.startTime ? new Date(Monitor.startTime).toLocaleString() : null
				},
				{
					Name: 'Uptime',
					Value: Monitor.startTime
						? new Date(Time.server.getTime() - Monitor.startTime).toLocaleTimeString()
						: null
				},
				{ Name: 'License Type', Value: Status.licenceType },
				{ Name: 'License N°', Value: Status.licenceNumber },
				{ Name: 'License Expiration Date', Value: Status.licenceExpired },
				{ Name: 'Endpoint', Value: Status.endpoint }
			]
		},
		{
			title: 'System Information',
			buttons: [
				{
					label: 'Perform GC',
					icon: 'mdi:broom',
					cls: 'green-button',
					onclick: () => call('engine.PerformGC')
				}
			],
			data: [
				{ Name: 'Host Name', Value: Status.hostName },
				{
					Name: 'CPU',
					Value: Status.osArchitecture
						? `${Status.osArchitecture} architecture ${Status.osAvailableProcessors} processors`
						: null
				},
				{ Name: 'OS', Value: Status.osName ? `${Status.osName} ${Status.osVersion}` : null },
				{ Name: 'Java Vendor', Value: Status.javaVendor },
				{
					Name: 'Java',
					Value: Status.javaVersion
						? `${Status.javaVersion} (classes version: ${Status.javaClassVersion})`
						: null
				},
				{
					Name: 'Used Memory',
					Value: Monitor.memoryUsed.length
						? `${Monitor.memoryUsed[Monitor.memoryUsed.length - 1]} MB`
						: null
				},
				{
					Name: 'Total Memory',
					Value: Monitor.memoryTotal.length
						? `${Monitor.memoryTotal[Monitor.memoryTotal.length - 1]} MB`
						: null
				},
				{
					Name: 'Maximum Memory',
					Value: Monitor.memoryMaximal.length
						? `${Monitor.memoryMaximal[Monitor.memoryMaximal.length - 1]} MB`
						: null
				},
				{ Name: 'Your Browser', Value: Status.browser }
			]
		}
	]);

	const charts = $derived([
		{
			title: 'Memory',
			series: [
				{ name: 'Max', data: Monitor.memoryMaximal },
				{ name: 'Total', data: Monitor.memoryTotal },
				{ name: 'Used', data: Monitor.memoryUsed }
			]
		},
		{ title: 'Threads', series: [{ name: 'Threads', data: Monitor.threads }] },
		{ title: 'Contexts', series: [{ name: 'Contexts', data: Monitor.contexts }] },
		{
			title: 'Requests Duration',
			series: [{ name: 'Requests Duration', data: Monitor.requests }]
		},
		{
			title: 'Sessions',
			series: [
				{ name: 'Max', data: Monitor.sessionMaxCV },
				{ name: 'Current', data: Monitor.sessions },
				{ name: 'Available', data: Monitor.availableSessions }
			]
		}
	]);
	/**
	 * @type {never[]}
	 */
	let categories = $derived(Monitor.labels);

	/**
	 * @param {string} mode
	 */
	async function modal(event, mode) {
		let data = $state();
		if (mode == 'props') {
			data = Array(10).fill({ name: null, value: null });
			call('engine.GetJavaSystemPropertiesJson').then((res) => {
				data = res.properties;
			});
		}
		modalHome.open({
			event,
			mode,
			get data() {
				return mode == 'props' ? data : EnvironmentVariables.variables;
			}
		});
	}

	let modalHome;
</script>

<ModalDynamic bind:this={modalHome} class="w-full">
	{#snippet children({ close, params: { mode, data } })}
		<Card title={mode == 'env' ? 'Environment Variables' : 'Java System Properties'} class="w-full">
			<TableAutoCard
				definition={[
					{ name: 'Name', key: 'name', class: 'break-all min-w-48' },
					{ name: 'Value', key: 'value', class: 'break-all min-w-48' }
				]}
				{data}
				class="max-h-[80vh]"
			></TableAutoCard>

			<div class="w-full layout-x justify-end">
				<button onclick={close} class="cancel-button">Close</button>
			</div>
		</Card>
	{/snippet}
</ModalDynamic>

<div class="layout-y md:layout-x !items-start">
	<div
		class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-1 gap md:auto-rows-min w-full md:min-w-[350px] md:max-w-[400px]"
	>
		{#each tables as { title, buttons, data }}
			<Card {title} class="max-w-[600px] statusTable">
				{#snippet cornerOption()}
					<ResponsiveButtons {buttons} />
				{/snippet}
				<TableAutoCard
					showHeaders={false}
					definition={[
						{ key: 'Name', custom: true },
						{ key: 'Value', custom: true }
					]}
					{data}
				>
					{#snippet children({ row, def })}
						{#if def.key === 'Name'}
							<span class="font-medium">{row.Name}</span>
						{:else}
							{#if row[def.key] == 'Running'}
								<span class="on"></span>
							{:else if row[def.key] == 'Stopped'}
								<span class="off"></span>
							{/if}
							<AutoPlaceholder loading={row[def.key] == null}>{row[def.key] ?? ''}</AutoPlaceholder>
						{/if}
					{/snippet}
				</TableAutoCard>
			</Card>
		{/each}
	</div>
	<div class="layout-grid-[350px] w-full">
		{#each charts as chart}
			<Card class="!items-stretch">
				<ApexChartLineAdmin {...chart} {categories} />
			</Card>
		{/each}
	</div>
</div>

<style lang="postcss">
	:global(.statusTable td:has(> .on)) {
		@apply bg-success-500;
	}
	:global(.statusTable td:has(> .off)) {
		@apply bg-error-500;
	}
</style>
