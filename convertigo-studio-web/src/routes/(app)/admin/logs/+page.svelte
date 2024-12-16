<script>
	import Card from '$lib/admin/components/Card.svelte';
	import { Popover, Slider, Tabs } from '@skeletonlabs/skeleton-svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import Logs from '$lib/admin/Logs.svelte';
	import { onMount, untrack } from 'svelte';
	import TimePicker from '$lib/admin/components/TimePicker.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import { slide } from 'svelte/transition';
	import { DatePicker } from '@svelte-plugins/datepicker';
	import LogViewer from '$lib/admin/components/LogViewer.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import Configuration from '$lib/admin/Configuration.svelte';
	import MaxRectangle from '$lib/admin/components/MaxRectangle.svelte';
	import Button from '$lib/admin/components/Button.svelte';

	onMount(() => {
		Logs.list();
	});

	let tabSet = $state('0');
	let rangeVal = $state([15]);
	let max = 25;

	let logsCategory = $derived(Configuration.categories.find(({ name }) => name == 'Logs'));

	const tzOffset = new Date().getTimezoneOffset() * 60000;

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
	let times = $state(datesEdited.map((d) => Logs.formatTime(d)));
	let isOpen = $state(false);

	Logs.startDate = Logs.formatDate(dates[0]) + ' ' + times[0];
	Logs.endDate = Logs.formatDate(dates[1]) + ' ' + times[1];

	function onDayClick(e) {
		if (e.startDate) {
			dates[0] = e.startDate - tzOffset;
		}
		if (e.endDate) {
			dates[1] = e.endDate - tzOffset;
		}
	}

	async function refreshLogs() {
		Logs.startDate = Logs.formatDate(dates[0]) + ' ' + times[0];
		Logs.endDate = Logs.formatDate(dates[1]) + ' ' + times[1];
		Logs.realtime = tabs[tabSet].name == 'Real Time';
		await Logs.list(true);
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

	$effect(() => {
		if (['Viewer', 'Real Time'].includes(tabs[tabSet].name)) {
			untrack(refreshLogs);
		}
	});
</script>

<MaxRectangle>
	<Card title="Logs" class="!gap-low h-full">
		{#snippet cornerOption()}
			{#if tabs[tabSet].name == 'Purge'}
				<ResponsiveButtons
					buttons={[
						{
							label: 'Purge',
							icon: 'material-symbols-light:save-as-outline',
							cls: 'basic-button',
							onclick: () => {}
						}
					]}
				/>
			{:else if tabs[tabSet].name == 'Log Levels'}
				<ResponsiveButtons
					buttons={[
						{
							label: 'Save changes',
							icon: 'material-symbols-light:save-as-outline',
							cls: 'basic-button',
							onclick: () => {}
						},
						{
							label: 'Cancel changes',
							icon: 'material-symbols-light:cancel-outline',
							cls: 'yellow-button',
							onclick: Configuration.refresh
						}
					]}
				/>
			{/if}
		{/snippet}
		<Tabs
			bind:value={tabSet}
			listClasses="flex-wrap"
			classes="h-full layout-y-stretch-none"
			contentClasses="grow"
		>
			{#snippet list()}
				{#each tabs as { name, icon }, i}
					<Tabs.Control
						value={'' + i}
						stateLabelActive="dark:bg-surface-500 bg-surface-50"
						padding=""
					>
						<!-- onchange={tabChanged} -->
						{#snippet lead()}{name}{/snippet}
						<Ico {icon} />
					</Tabs.Control>
				{/each}
			{/snippet}
			{#snippet content()}
				{#if ['Viewer', 'Real Time'].includes(tabs[tabSet].name)}
					<div class="h-full layout-y-start-low" transition:slide={{ axis: 'y' }}>
						{#if tabs[tabSet].name == 'Viewer'}
							<div class="w-full" transition:slide={{ axis: 'y' }}>
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
									<div class="layout-x-stretch flex-wrap">
										{#each ['From', 'To'] as way, i}
											<div class="layout-x-baseline-low flex-wrap">
												<Popover triggerBase="basic-button" arrow arrowBackground="">
													{#snippet trigger()}{way}<Ico
															icon="mdi:clock-star-four-points-outline"
														/>{/snippet}
													{#snippet content()}
														<Card bg="bg-surface-50-950" class="!p-low">
															<div class="layout-y-stretch-low">
																{#each presets[i] as { name, fn }}
																	<Button
																		label={name}
																		class="basic-button"
																		onclick={() => {
																			dates[i] = fn();
																			times[i] = Logs.formatTime(dates[i]);
																		}}
																	/>
																{/each}
															</div>
														</Card>
													{/snippet}
												</Popover>
												<input
													type="text"
													class="input-common input-text max-w-fit w-[12ch]"
													value={Logs.formatDate(dates[i])}
													onfocus={() => {
														datesEdited[i] = null;
														isOpen = true;
													}}
													size="11"
												/>
												<TimePicker bind:inputValue={times[i]} />
											</div>
										{/each}
										<Button
											label="Search"
											icon="mdi:receipt-text-send-outline"
											class="basic-button !w-fit !h-auto grow"
											onclick={refreshLogs}
										/>
									</div>
								</DatePicker>
							</div>
						{/if}
						<div class="grow">
							<LogViewer autoScroll={tabs[tabSet].name == 'Real Time'} />
						</div>
					</div>
				{:else if tabs[tabSet].name == 'Purge'}
					<div class="layout-y-stretch" transition:slide={{ axis: 'y' }}>
						<div class="bg-surface-50 dark:bg-surface-700 p-5 rounded">
							<Slider name="range-slider" bind:value={rangeVal} max={25} step={1} />
						</div>
						<div class="flex justify-between items-center">
							<div class="font-bold">Delete logs files older than 24/05/2024, 13:03:07</div>
							<div class="text-xs">{rangeVal} / {max}</div>
						</div>
					</div>
				{:else if tabs[tabSet].name == 'Log Levels'}
					<div class="layout-grid-[300px]" transition:slide={{ axis: 'y' }}>
						{#each logsCategory?.property as property}
							{#if property.description?.startsWith('Log4J')}
								<PropertyType {property} />
							{/if}
						{/each}
					</div>
				{/if}
			{/snippet}
		</Tabs>
	</Card>
</MaxRectangle>
