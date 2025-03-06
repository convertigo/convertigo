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
	import { onDestroy } from 'svelte';
	import Button from '$lib/admin/components/Button.svelte';

	let {
		product,
		licenceType,
		licenceNumber,
		licenceExpired,
		endpoint,
		hostName,
		osArchitecture,
		osAvailableProcessors,
		osName,
		osVersion,
		javaVendor,
		javaVersion,
		javaClassVersion,
		browser,
		init
	} = $derived(Status);

	let {
		engineState,
		startTime,
		memoryUsed,
		memoryTotal,
		memoryMaximal,
		threads,
		contexts,
		requests,
		sessionMaxCV,
		sessions,
		availableSessions,
		labels
	} = $derived(Monitor);

	onDestroy(Status.stop);

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
					Value: engineState ? 'Running' : engineState == null ? null : 'Stopped'
				},
				{ Name: 'Convertigo Version', Value: product },
				{
					Name: 'Last Startup',
					Value: startTime ? new Date(startTime).toLocaleString() : null
				},
				{
					Name: 'Uptime',
					Value: startTime ? new Date(Time.server.getTime() - startTime).toLocaleTimeString() : null
				},
				{ Name: 'License Type', Value: licenceType },
				{ Name: 'License N°', Value: licenceNumber },
				{ Name: 'License Expiration Date', Value: licenceExpired },
				{ Name: 'Endpoint', Value: endpoint }
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
				{ Name: 'Host Name', Value: hostName },
				{
					Name: 'CPU',
					Value: osArchitecture
						? `${osArchitecture} architecture ${osAvailableProcessors} processors`
						: null
				},
				{ Name: 'OS', Value: osName ? `${osName} ${osVersion}` : null },
				{ Name: 'Java Vendor', Value: javaVendor },
				{
					Name: 'Java',
					Value: javaVersion ? `${javaVersion} (classes version: ${javaClassVersion})` : null
				},
				{
					Name: 'Used Memory',
					Value: memoryUsed.length ? `${memoryUsed[memoryUsed.length - 1]} MB` : null
				},
				{
					Name: 'Total Memory',
					Value: memoryTotal.length ? `${memoryTotal[memoryTotal.length - 1]} MB` : null
				},
				{
					Name: 'Maximum Memory',
					Value: memoryMaximal.length ? `${memoryMaximal[memoryMaximal.length - 1]} MB` : null
				},
				{ Name: 'Your Browser', Value: browser }
			]
		}
	]);

	const charts = $derived([
		{
			title: 'Memory',
			series: [
				{ name: 'Max', data: memoryMaximal },
				{ name: 'Total', data: memoryTotal },
				{ name: 'Used', data: memoryUsed }
			]
		},
		{ title: 'Threads', series: [{ name: 'Threads', data: threads }] },
		{ title: 'Contexts', series: [{ name: 'Contexts', data: contexts }] },
		{
			title: 'Requests Duration',
			series: [{ name: 'Requests Duration', data: requests }]
		},
		{
			title: 'Sessions',
			series: [
				{ name: 'Max', data: sessionMaxCV },
				{ name: 'Current', data: sessions },
				{ name: 'Available', data: availableSessions }
			]
		}
	]);

	let categories = $derived(labels);

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
				<Button label="Close" onclick={close} class="w-fit! cancel-button" />
			</div>
		</Card>
	{/snippet}
</ModalDynamic>

<div class="layout-y-start md:layout-x-start">
	<div
		class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-1 gap md:auto-rows-min w-full md:min-w-[350px] md:max-w-[400px]"
	>
		{#each tables as { title, buttons, data }}
			<Card {title} class="statusTable">
				{#snippet cornerOption()}
					<ResponsiveButtons {buttons} disabled={!init} />
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

	<div class="w-full grid gap grid-cols-1 sm:grid-cols-2 md:grid-cols-1 lg:grid-cols-2">
		{#each charts as chart}
			<Card class="p-none!">
				<ApexChartLineAdmin {...chart} {categories} />
			</Card>
		{/each}
	</div>
</div>

<style>
	@reference "../../../app.css";
	
	:global(.statusTable td:has(> .on)) {
		@apply bg-success-500 font-normal;
	}
	:global(.statusTable td:has(> .off)) {
		@apply bg-error-500;
	}
</style>
