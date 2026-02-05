<script>
	import { SegmentedControl } from '@skeletonlabs/skeleton-svelte';
	import ActionBar from '$lib/admin/components/ActionBar.svelte';
	import ApexChartLineAdmin from '$lib/admin/components/ApexChartLineAdmin.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import EnvironmentVariables from '$lib/admin/EnvironmentVariables.svelte';
	import Monitor from '$lib/admin/Monitor.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import Status from '$lib/common/Status.svelte';
	import Time from '$lib/common/Time.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { call } from '$lib/utils/service';
	import { formatDuration } from '$lib/utils/time';
	import { onDestroy } from 'svelte';
	import { persistedState } from 'svelte-persisted-state';

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

	const varsMode = persistedState('admin.status.variablesMode', 'env', { syncTabs: false });
	const varsTabs = [
		{ value: 'props', label: 'Java Props', icon: 'mdi:language-java' },
		{ value: 'env', label: 'Env Vars', icon: 'mdi:code-block-braces' }
	];
	let javaProps = $state(Array(10).fill({ name: null, value: null }));
	let javaPropsLoading = $state(false);
	let javaPropsLoaded = $state(false);

	async function loadJavaProps(force = false) {
		if (javaPropsLoading || (javaPropsLoaded && !force)) {
			return;
		}
		javaPropsLoading = true;
		javaProps = Array(10).fill({ name: null, value: null });
		const res = await call('engine.GetJavaSystemPropertiesJson');
		javaProps = res?.properties ?? [];
		javaPropsLoading = false;
		javaPropsLoaded = true;
	}

	function onVarsTabChange(value) {
		varsMode.current = value;
		if (value == 'props') {
			loadJavaProps();
		}
	}

	function openVariables(event) {
		modalHome.open({ event });
		if (varsMode.current == 'props') {
			loadJavaProps();
		}
	}

	function openRestart(event) {
		modalRestart.open({ event });
	}

	const tables = $derived([
		{
			title: 'Status',
			buttons: [
				{
					label: 'Variables',
					title: 'Java Props / Env Vars',
					icon: 'mdi:code-braces',
					cls: 'button-secondary text-[11px] px-2! whitespace-nowrap',
					onclick: openVariables
				},
				{
					label: 'Restart',
					title: 'Restart Engine',
					icon: 'mdi:restart-alert',
					cls: 'button-secondary text-[11px] px-2! whitespace-nowrap',
					onclick: openRestart
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
					Value: startTime ? formatDuration(Time.server.getTime() - startTime) : null
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
					cls: 'button-secondary',
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
	const varsData = $derived(
		varsMode.current == 'props' ? javaProps : EnvironmentVariables.variables
	);
	let modalHome;
	let modalRestart;
</script>

<ModalDynamic bind:this={modalHome} class="w-full overflow-hidden">
	{#snippet children({ close })}
		<Card title="Variables" class="w-full preset-filled-surface-100-900">
			{#snippet cornerOption()}
				<SegmentedControl
					value={varsMode.current}
					onValueChange={(event) => onVarsTabChange(event.value ?? 'env')}
				>
					<SegmentedControl.Control
						class={[
							'relative',
							'gap-0.5',
							'rounded-base',
							'border',
							'border-surface-100-900',
							'bg-surface-200-800',
							'p-0.5',
							'shadow-none',
							'overflow-hidden'
						]}
					>
						<SegmentedControl.Indicator class="rounded-[0.3rem] bg-primary-600 shadow-none" />
						{#each varsTabs as tab (tab.value)}
							<SegmentedControl.Item value={tab.value} class="relative flex-1">
								<SegmentedControl.ItemText
									class={[
										varsMode.current == tab.value ? 'text-white' : 'text-muted',
										'inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium'
									]}
								>
									<Ico icon={tab.icon} size={4} />
									{tab.label}
								</SegmentedControl.ItemText>
								<SegmentedControl.ItemHiddenInput />
							</SegmentedControl.Item>
						{/each}
					</SegmentedControl.Control>
				</SegmentedControl>
			{/snippet}
			<TableAutoCard
				definition={[
					{ name: 'Name', key: 'name', class: 'break-all min-w-48' },
					{ name: 'Value', key: 'value', class: 'break-all min-w-48' }
				]}
				data={varsData}
				class="max-h-[75vh] overflow-auto"
			></TableAutoCard>

			<ActionBar>
				<Button label="Close" onclick={close} class="button-primary w-fit!" />
			</ActionBar>
		</Card>
	{/snippet}
</ModalDynamic>

<ModalDynamic bind:this={modalRestart}>
	{#snippet children({ close })}
		<Card title="Restart Engine" class="max-w-xl">
			<div class="layout-y-stretch gap-3 text-sm">
				<p>
					<strong>Soft restart</strong> stops and restarts the Convertigo engine without killing the JVM.
				</p>
				<p class="preset-tonal-warning">
					<strong>Hard restart</strong> stops the engine and exits the JVM.<br />
					Restart will depend on your orchestrator or process supervisor (or completely shutdown).
				</p>
			</div>
			<ActionBar>
				<Button
					label="Soft Restart"
					icon="mdi:restart-alert"
					class="button-primary w-fit!"
					onclick={async () => {
						await call('engine.Restart');
						close();
					}}
				/>
				<Button
					label="Hard Restart"
					icon="mdi:restart-alert"
					class="button-warning w-fit!"
					onclick={async () => {
						await call('engine.Restart', { hard: 'true' });
						close();
					}}
				/>
				<Button
					label="Cancel"
					icon="mdi:close-circle-outline"
					class="button-secondary w-fit!"
					onclick={close}
				/>
			</ActionBar>
		</Card>
	{/snippet}
</ModalDynamic>

<div class="layout-y-start md:layout-x-start">
	<div
		class="grid w-full grid-cols-1 gap sm:grid-cols-2 md:max-w-[400px] md:min-w-[350px] md:auto-rows-min md:grid-cols-1"
	>
		{#each tables as { title, buttons, data } (title)}
			<Card {title} class="statusTable">
				{#snippet cornerOption()}
					<ActionBar full={false} wrap={false} class="ml-auto" disabled={!init}>
						{#each buttons as button (button.label)}
							<Button {...button} full={false} />
						{/each}
					</ActionBar>
				{/snippet}
				<TableAutoCard
					showHeaders={false}
					definition={[
						{
							key: 'Name',
							custom: true,
							class: 'min-w-32 text-sm font-normal text-muted text-right'
						},
						{
							key: 'Value',
							custom: true,
							class: 'text-sm text-right! break text-strong font-normal'
						}
					]}
					{data}
				>
					{#snippet children({ row, def })}
						{#if def.key === 'Name'}
							<span>{row.Name}</span>
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

	<div class="grid w-full grid-cols-1 gap sm:grid-cols-2 md:grid-cols-1 lg:grid-cols-2">
		{#each charts as chart (chart.title)}
			<Card class="p-none!">
				<ApexChartLineAdmin {...chart} {categories} />
			</Card>
		{/each}
	</div>
</div>

<style lang="postcss">
	@reference "../../../app.css";

	:global(.statusTable td:has(> .on)) {
		background-color: var(--convertigo-running);
		color: var(--convertigo-text);
	}
	:global(.statusTable td:has(> .off)) {
		@apply preset-filled-error-200-800;
	}
	:global(.statusTable td.break) {
		@apply wrap-break-word;
		word-break: break-word;
	}
</style>
