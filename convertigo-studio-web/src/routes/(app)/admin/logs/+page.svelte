<script>
	import { run } from 'svelte/legacy';

	import Card from '$lib/admin/components/Card.svelte';
	import { Tab, TabGroup, RangeSlider, popup, SlideToggle } from '@skeletonlabs/skeleton';
	import Ico from '$lib/utils/Ico.svelte';
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';
	import { refreshConfigurations, configurations } from '$lib/admin/stores/configurationStore';
	import {
		logsList,
		formatDate,
		formatTime,
		startDate,
		endDate,
		realtime
	} from '$lib/admin/stores/logsStore';
	import { onMount } from 'svelte';
	import ResponsiveContainer from '$lib/admin/components/ResponsiveContainer.svelte';
	import TimePicker from '$lib/admin/components/TimePicker.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import { checkArray } from '$lib/utils/service';
	import { slide } from 'svelte/transition';
	import { DatePicker } from '@svelte-plugins/datepicker';
	import LogViewer from '$lib/admin/components/LogViewer.svelte';

	onMount(() => {
		// refreshConfigurations();
		// logsList();
	});

	let tabSet = $state(0);
	let rangeVal = $state(15);
	let max = 25;

	let logsCategory = $state(null);

	const tzOffset = new Date().getTimezoneOffset() * 60000;

	// Subscribe to config and extract Logs category .. maybe easier than reuse Prprty comp
	// from configration page
	$effect(() => {
		const config = $configurations;
		if (config?.admin?.category) {
			logsCategory = config.admin.category.find((cat) => cat['@_name'] === 'Logs');
		}
	});

	const tabs = [
		{ name: 'Viewer', icon: 'grommet-icons:add' },
		{ name: 'Real Time', icon: 'grommet-icons:add' },
		{ name: 'Purge', icon: 'grommet-icons:add' },
		{ name: 'Log Levels', icon: 'grommet-icons:add' }
	];

	/** @type {Array<number|null>}*/
	let datesEdited = $state([
		new Date().setDate(new Date().getDate() - 1),
		new Date().setHours(0, 0, 0, 0) + 86400000
	]);
	let dates = $state([...datesEdited]);
	let times = $state(datesEdited.map((d) => formatTime(d)));
	let isOpen = $state(false);

	$startDate = formatDate(dates[0]) + ' ' + times[0];
	$endDate = formatDate(dates[1]) + ' ' + times[1];

	function onDayClick(e) {
		if (e.startDate) {
			dates[0] = e.startDate - tzOffset;
		}
		if (e.endDate) {
			dates[1] = e.endDate - tzOffset;
		}
	}

	async function refreshLogs() {
		$startDate = formatDate(dates[0]) + ' ' + times[0];
		$endDate = formatDate(dates[1]) + ' ' + times[1];
		$realtime = tabs[tabSet].name == 'Real Time';
		await logsList(true);
	}

	const presets = [
		[
			{ name: 'now', fn: () => new Date().getTime() },
			{
				name: '10 min ago',
				fn: () => new Date(dates[0] ?? 0).setMinutes(new Date(dates[0] ?? 0).getMinutes() - 10)
			},
			{
				name: '1 hour ago',
				fn: () => new Date(dates[0] ?? 0).setHours(new Date(dates[0] ?? 0).getHours() - 1)
			},
			{ name: 'today', fn: () => new Date().setHours(0, 0, 0, 0) },
			{ name: 'yesterday', fn: () => new Date().setHours(0, 0, 0, 0) - 86400000 },
			{
				name: 'start of the month',
				fn: () => new Date(new Date().setHours(0, 0, 0, 0)).setDate(1)
			},
			{
				name: 'start of the year',
				fn: () => new Date(new Date().setHours(0, 0, 0, 0)).setMonth(0, 1)
			},
			{ name: 'epoch', fn: () => 0 }
		],
		[
			{ name: 'now', fn: () => new Date().getTime() },
			{ name: '10 min after', fn: () => new Date().setMinutes(new Date().getMinutes() + 10) },
			{ name: '1 hour after', fn: () => new Date().setHours(new Date().getHours() + 1) },
			{ name: 'tomorrow', fn: () => new Date().setHours(0, 0, 0, 0) + 86400000 }
		]
	];

	function tabChanged() {
		if (tabs[tabSet].name == 'Real Time') {
			$realtime = true;
			refreshLogs();
		} else if (tabs[tabSet].name == 'Viewer' && $realtime) {
			$realtime = false;
			refreshLogs();
		}
	}
</script>

<Card title="Logs">
	{#snippet cornerOption()}
		{#if tabs[tabSet].name == 'Purge'}
			<ButtonsContainer class="flex">
				<button type="button" class="basic-button">
					<span><Ico icon="material-symbols-light:save-as-outline" class="w-6 h-6" /></span>
					<span>Purge</span>
				</button>
			</ButtonsContainer>
		{:else if tabs[tabSet].name == 'Purge'}
			<ButtonsContainer class="flex">
				<button type="button" class="basic-button">
					<span><Ico icon="material-symbols-light:save-as-outline" class="w-6 h-6" /></span>
					<span>Save changes</span>
				</button>
				<button type="button" class="yellow-button" onclick={refreshConfigurations}>
					<span><Ico icon="material-symbols-light:cancel-outline" class="w-6 h-6" /></span>
					<span class="">Cancel changes</span>
				</button>
			</ButtonsContainer>
		{/if}		
	{/snippet}
	<TabGroup>
		{#each tabs as { name, icon }, value}
			<Tab
				bind:group={tabSet}
				{name}
				{value}
				active="dark:bg-surface-500 bg-surface-50"
				on:change={tabChanged}
			>
				<svelte:fragment slot="lead">
					<div class="flex items-center gap-2">
						<p>{name}</p>
						<Ico {icon} />
					</div>
				</svelte:fragment>
			</Tab>
		{/each}
		<svelte:fragment slot="panel">
			{#if tabs[tabSet].name == 'Viewer'}
				<div class="flex flex-col gap-2">
					<div class="flex flex-col gap-2" transition:slide={{ axis: 'y' }}>
						{#each presets as preset, i}
							<div class="card p-4 variant-filled-surface z-50" data-popup="preset-{i}">
								<div class="flex flex-col gap-2 overflow-y-auto">
									{#each preset as { name, fn }}
										<button
											class="btn variant-ghost-primary"
											onclick={() => {
												dates[i] = fn();
												times[i] = formatTime(dates[i]);
											}}
										>
											{name}
										</button>
									{/each}
									<div class="arrow variant-filled-surface"></div>
								</div>
							</div>
						{/each}
						<DatePicker
							bind:isOpen
							alwaysShow={false}
							isRange={true}
							isMultipane={true}
							bind:startDate={datesEdited[0]}
							bind:endDate={datesEdited[1]}
							showYearControls={true}
							startOfWeek={1}
							{onDayClick}
						>
							<div class="flex flex-row flex-wrap gap-4">
								{#each ['From', 'To'] as way, i}
									<div class="flex flex-col items-center gap-4">
										<div class="flex flex-row flex-wrap items-center gap-2">
											<button
												type="button"
												class="btn btn-sm p-2 variant-filled-surface"
												use:popup={{ event: 'click', target: `preset-${i}`, placement: 'bottom' }}
												>{way}&nbsp;<Ico icon="mdi:clock-star-four-points-outline" /></button
											>
											<input
												type="text"
												class="input max-w-fit h-full"
												value={formatDate(dates[i])}
												onfocus={() => {
													datesEdited[i] = null;
													isOpen = true;
												}}
												size="11"
											/>
											<TimePicker bind:inputValue={times[i]} />
										</div>
									</div>
								{/each}
								<button class="btn btn-sm variant-filled-surface p-2" onclick={refreshLogs}
									>Search&nbsp;<Ico icon="mdi:receipt-text-send-outline" /></button
								>
							</div>
						</DatePicker>
					</div>
				</div>
			{:else if tabs[tabSet].name == 'Purge'}
				<div class="logsCard">
					<RangeSlider
						accent="accent-tertiary-500 dark:accent-tertiary-500"
						name="range-slider"
						bind:value={rangeVal}
						max={25}
						step={1}
					>
						<div class="flex justify-between items-center">
							<div class="font-bold">Delete logs files older than 24/05/2024, 13:03:07</div>
							<div class="text-xs">{rangeVal} / {max}</div>
						</div>
					</RangeSlider>
				</div>
			{:else if tabs[tabSet].name == 'Log Levels'}
				<Card>
					{#if checkArray(logsCategory?.property)}
						<ResponsiveContainer
							scrollable={false}
							maxHeight="h-auto"
							smCols="sm:grid-cols-1"
							mdCols="md:grid-cols-1"
							lgCols="lg:grid-cols-4"
						>
							{#each logsCategory.property as property}
								{#if property['@_description'] && property['@_description'].startsWith('Log4J')}
									<PropertyType {property} />
								{/if}
							{/each}
						</ResponsiveContainer>
					{:else}
						<p>No logs category found or properties are not available.</p>
					{/if}
				</Card>
			{/if}
		</svelte:fragment>
	</TabGroup>
</Card>

{#if tabs[tabSet].name == 'Viewer' || tabs[tabSet].name == 'Real Time'}
	<Card class="mt-2">
		<LogViewer autoScroll={tabs[tabSet].name == 'Real Time'} />
	</Card>
{/if}

<style lang="postcss">
	.logsCard {
		@apply bg-surface-50 dark:bg-surface-700 p-5 rounded-token;
	}
</style>
