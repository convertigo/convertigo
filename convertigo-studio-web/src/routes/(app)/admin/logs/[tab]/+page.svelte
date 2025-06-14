<script>
	import { Popover, Slider, Tabs } from '@skeletonlabs/skeleton-svelte';
	import { DatePicker } from '@svelte-plugins/datepicker';
	import { beforeNavigate, goto } from '$app/navigation';
	import { page } from '$app/state';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import LogViewer from '$lib/admin/components/LogViewer.svelte';
	import MaxRectangle from '$lib/admin/components/MaxRectangle.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import TimePicker from '$lib/admin/components/TimePicker.svelte';
	import Configuration from '$lib/admin/Configuration.svelte';
	import Logs from '$lib/admin/Logs.svelte';
	import LogsPurge from '$lib/admin/LogsPurge.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { getContext, onMount, untrack } from 'svelte';
	import { persisted } from 'svelte-persisted-store';
	import { fromStore } from 'svelte/store';
	import { slide } from 'svelte/transition';

	onMount(() => {
		Logs.list();
		return () => {
			Configuration.stop();
			LogsPurge.stop();
		};
	});

	let skip = false;
	beforeNavigate(async (nav) => {
		if (skip) {
			skip = false;
			return;
		}
		if (hasChanges) {
			nav.cancel();
			if (
				await modalYesNo.open({
					title: 'You have unsaved changes!',
					message: 'Are you sure you want to continue?'
				})
			) {
				Configuration.refresh();
				skip = true;
				await goto(nav.to?.url ?? '');
			} else {
				tabSet = 'view';
				tabSet = 'config';
				return;
			}
		}
	});

	let logsCategory = $derived(Configuration.categories.find(({ name }) => name == 'Logs'));
	let serverFilter = $state('');
	let filters = $state({}); //fromStore(persisted('adminLogsFilters', {}, { syncTabs: false })).current;

	const tzOffset = new Date().getTimezoneOffset() * 60000;

	const tabs = {
		view: { name: 'Viewer', icon: 'grommet-icons:add', viewer: true },
		realtime: { name: 'Real Time', icon: 'grommet-icons:add', viewer: true },
		purge: { name: 'Purge', icon: 'grommet-icons:add' },
		config: { name: 'Log Levels', icon: 'grommet-icons:add' }
	};

	let tabSet = $derived(Object.keys(tabs).includes(page.params.tab) ? page.params.tab : 'view');

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
		Logs.realtime = tabSet == 'realtime';
		Logs.filter = serverFilter;
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
		if (tabs[tabSet].viewer) {
			untrack(refreshLogs);
		}
	});

	$effect(() => {
		if (tabSet != 'purge') {
			LogsPurge.stop();
		}
	});

	async function saveChanges(event) {
		const toSave = logsCategory.property
			?.filter(({ value, originalValue }) => value != originalValue)
			.map(({ name, value }) => ({
				'@_key': name,
				'@_value': value
			}));
		const confirmed = await modalYesNo.open({
			event,
			title: `Are you sure you want to save ${toSave.length} propert${toSave.length == 1 ? 'y' : 'ies'}?`
		});
		if (confirmed) {
			Configuration.updateConfigurations(toSave);
		}
	}

	function copyFilters() {
		let filterString = Object.entries(filters)
			.filter(([_, array]) => array.length > 0)
			.map(([name, array]) =>
				array
					.map(({ mode, value, not }) =>
						mode == 'equals'
							? `${name.toLowerCase()} ${not ? '!' : '='}= '${value}'`
							: `${not ? '!' : ''}${name.toLowerCase()}.${mode}('${value}')`
					)
					.join(' or ')
			)
			.join(' and ');
		serverFilter = filterString;
	}

	let hasChanges = $derived(
		tabSet == 'config' &&
			logsCategory?.property?.some(({ value, originalValue }) => value != originalValue)
	);

	let modalYesNo = getContext('modalYesNo');

	let showFilters = $state(false);
</script>

<MaxRectangle delay={200} enabled={tabs[tabSet].viewer ?? false}>
	<Card title="Logs" class="h-full gap-low! pt-low!">
		{#snippet cornerOption()}
			<div class="layout-x-low w-full">
				<Tabs
					value={tabSet}
					onValueChange={(e) => goto(`../${e.value}/`)}
					listClasses="mb-0! flex-wrap"
					classes="w-fit!"
				>
					{#snippet list()}
						{#each Object.entries(tabs) as [value, { name, icon }]}
							<Tabs.Control {value} stateLabelActive="preset-filled-primary-100-900" padding="">
								{#snippet lead()}{name}{/snippet}
								<Ico {icon} />
							</Tabs.Control>
						{/each}
					{/snippet}
				</Tabs>
				<div class="grow">
					{#if tabSet == 'purge'}
						<ResponsiveButtons
							buttons={[
								{
									label: 'Purge',
									icon: 'material-symbols-light:save-as-outline',
									cls: 'button-error',
									disabled: LogsPurge.value[0] == -1,
									onclick: async (event) => {
										if (
											await modalYesNo.open({
												event,
												title: 'Delete logs files older than',
												message: `${LogsPurge.date} ?`
											})
										) {
											LogsPurge.purge();
										}
									}
								}
							]}
						/>
					{:else if tabSet == 'config'}
						<ResponsiveButtons
							buttons={[
								{
									label: 'Save changes',
									icon: 'material-symbols-light:save-as-outline',
									cls: 'button-success',
									disabled: !hasChanges,
									onclick: saveChanges
								},
								{
									label: 'Cancel changes',
									icon: 'material-symbols-light:cancel-outline',
									cls: 'button-error',
									disabled: !hasChanges,
									onclick: Configuration.refresh
								}
							]}
						/>
					{/if}
				</div>
			</div>
		{/snippet}
		<Tabs
			value={tabSet}
			listClasses="hidden"
			classes="h-full layout-y-stretch-none"
			contentClasses="grow"
		>
			{#snippet content()}
				{#if tabs[tabSet].viewer}
					<div class="layout-y-stretch-low h-full" transition:slide={{ axis: 'y' }}>
						{#if tabSet == 'view'}
							<div transition:slide={{ axis: 'y' }}>
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
									<div class="layout-x-end-low flex-wrap">
										{#each ['From', 'To'] as way, i}
											<div class="layout-x-baseline-low flex-wrap">
												<Popover triggerBase="button-primary" arrow arrowBackground="">
													{#snippet trigger()}{way}<Ico
															icon="mdi:clock-star-four-points-outline"
														/>{/snippet}
													{#snippet content()}
														<Card bg="bg-surface-50-950" class="p-low!">
															<div class="layout-y-stretch-low">
																{#each presets[i] as { name, fn }}
																	<Button
																		label={name}
																		class="button-primary"
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
													class="input-text button input-common w-[12ch] max-w-fit preset-filled-primary-50-950 px-low"
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
											size={4}
											icon="mdi:filter-cog{showFilters ? '' : '-outline'}"
											onmousedown={() => (showFilters = !showFilters)}
											class="button-secondary h-7! w-fit!"
										/>
										{#if showFilters}
											<Button
												size={4}
												icon="mingcute:delete-line"
												onmousedown={() => (serverFilter = '')}
												class="button-error h-7! w-fit!"
												label="Clear"
											/>
											<Button
												size={4}
												icon="material-symbols:sync-arrow-up-rounded"
												onmousedown={copyFilters}
												class="button-tertiary h-7! w-fit!"
												label="Copy filters"
											/>
											<PropertyType
												placeholder="Server filter…"
												bind:value={serverFilter}
												onkeyup={(e) => {
													if (e?.key == 'Enter') refreshLogs();
												}}
												class="preset-filled-secondary-50-950 motif-secondary"
											/>
										{/if}
										<Button
											label="Search"
											size={4}
											icon="mdi:receipt-text-send-outline"
											class="button-success w-fit! grow"
											onclick={refreshLogs}
										/>
									</div>
								</DatePicker>
							</div>
						{/if}
						<div class="-mx -mb h-full">
							<LogViewer autoScroll={tabSet == 'realtime'} bind:filters />
						</div>
					</div>
				{:else if tabSet == 'purge'}
					{@const {
						loading,
						dates,
						date,
						value: [idx]
					} = LogsPurge}
					<div class="layout-y-stretch" transition:slide={{ axis: 'y' }}>
						<div class="mt">Logs are split into multiple files, each step is a file.</div>
						<div class="rounded-sm preset-filled-surface-300-700 p-5">
							<AutoPlaceholder {loading}>
								<Slider
									name="range-slider"
									value={LogsPurge.value}
									onValueChange={(e) => (LogsPurge.value = e.value)}
									min={-1}
									max={dates.length - 1}
									step={1}
								/>
							</AutoPlaceholder>
						</div>
						<AutoPlaceholder {loading}>
							<div class="layout-x justify-between">
								{#if idx >= 0}
									<div class="font-bold">Delete logs files older than {date}</div>
								{:else}
									<div class="font-bold">No selected date</div>
								{/if}
								<div class="text-xs">{idx + 1} / {dates.length}</div>
							</div>
						</AutoPlaceholder>
					</div>
				{:else if tabSet == 'config'}
					<div class="layout-grid-[300px]" transition:slide={{ axis: 'y' }}>
						{#each logsCategory?.property as property}
							{#if property.description?.startsWith('Log4J')}
								<PropertyType {...property} bind:value={property.value} />
							{/if}
						{/each}
					</div>
				{/if}
			{/snippet}
		</Tabs>
	</Card>
</MaxRectangle>
