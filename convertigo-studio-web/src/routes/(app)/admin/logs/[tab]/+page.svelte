<script>
	import { getLocalTimeZone, now, toCalendarDate, today, toTime } from '@internationalized/date';
	import { Popover, Slider, Tabs } from '@skeletonlabs/skeleton-svelte';
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
	import { formatTime } from '$lib/admin/Logs.svelte';
	import LogsPurge from '$lib/admin/LogsPurge.svelte';
	import DateRangePicker from '$lib/common/components/DateRangePicker.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { getContext, onMount, untrack } from 'svelte';
	import { slide } from 'svelte/transition';

	let logViewer = $state();
	onMount(() => {
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
	let presetOpened = $state(false);

	const tzOffset = new Date().getTimezoneOffset() * 60000;

	const tabs = {
		view: { name: 'Viewer', icon: 'material-symbols:search-rounded', viewer: true },
		realtime: { name: 'Real Time', icon: 'grommet-icons:add', viewer: true },
		purge: { name: 'Purge', icon: 'material-symbols-light:delete-outline' },
		config: { name: 'Log Levels', icon: 'material-symbols:settings-outline-rounded' }
	};

	let tabSet = $derived(Object.keys(tabs).includes(page.params.tab) ? page.params.tab : 'view');
	let dates = $state([
		toCalendarDate(now(getLocalTimeZone()).subtract({ minutes: 10 })),
		toCalendarDate(today(getLocalTimeZone()))
	]);
	let times = $state([
		toTime(now(getLocalTimeZone()).subtract({ minutes: 10 }))
			.toString()
			.replace('.', ','),
		toTime(now(getLocalTimeZone())).toString().replace('.', ',')
	]);

	// let isOpen = $state(false);

	let startDate = $derived(dates[0].toString() + ' ' + times[0]);
	let endDate = $derived(dates[1].toString() + ' ' + times[1]);
	let realtime = $derived(tabSet == 'realtime');

	// function onDayClick(e) {
	// 	if (e.startDate) {
	// 		dates[0] = e.startDate - tzOffset;
	// 	}
	// 	if (e.endDate) {
	// 		dates[1] = e.endDate - tzOffset;
	// 	}
	// }

	async function refreshLogs() {
		// Logs.startDate = formatDate(dates[0]) + ' ' + times[0];
		// Logs.endDate = formatDate(dates[1]) + ' ' + times[1];
		// Logs.realtime = tabSet == 'realtime';
		// Logs.filter = serverFilter;
		await logViewer.list(true);
	}

	function setDatesTimes(start, end) {
		dates = [toCalendarDate(start), toCalendarDate(end)];
		times = [toTime(start).toString().replace('.', ','), toTime(end).toString().replace('.', ',')];
		presetOpened = false;
	}

	const presets = [
		{
			label: 'now ⇒ +24h',
			onclick: () => {
				const nowDate = now(getLocalTimeZone());
				setDatesTimes(nowDate, nowDate.add({ days: 1 }));
			}
		},
		{
			label: '1 min ago ⇒ now',
			onclick: () => {
				const nowDate = now(getLocalTimeZone());
				setDatesTimes(nowDate.copy().subtract({ minutes: 1 }), nowDate);
			}
		},
		{
			label: '10 min ago ⇒ now',
			onclick: () => {
				const nowDate = now(getLocalTimeZone());
				setDatesTimes(nowDate.subtract({ minutes: 10 }), nowDate);
			}
		},
		{
			label: '1 hour ago ⇒ now',
			onclick: () => {
				const nowDate = now(getLocalTimeZone());
				setDatesTimes(nowDate.subtract({ hours: 1 }), nowDate);
			}
		},
		{
			label: 'today',
			onclick: () => {
				const todayDate = today(getLocalTimeZone());
				setDatesTimes(todayDate, todayDate.add({ days: 1 }));
			}
		},
		{
			label: 'yesterday',
			onclick: () => {
				const yesterdayDate = today(getLocalTimeZone()).subtract({ days: 1 });
				setDatesTimes(yesterdayDate, yesterdayDate.add({ days: 1 }));
			}
		},
		{
			label: 'last 7 days',
			onclick: () => {
				const nowDate = now(getLocalTimeZone());
				setDatesTimes(nowDate.subtract({ days: 7 }), nowDate);
			}
		},
		{
			label: 'last 30 days',
			onclick: () => {
				const nowDate = now(getLocalTimeZone());
				setDatesTimes(nowDate.subtract({ days: 30 }), nowDate);
			}
		}
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
					.map(({ mode, value, not, sensitive }) =>
						mode == 'equals'
							? `${name.toLowerCase()}${sensitive ? '' : '.toLowerCase()'} ${not ? '!' : '='}= '${sensitive ? value : value.toLowerCase()}'`
							: `${not ? '!' : ''}${name.toLowerCase()}${sensitive ? '' : '.toLowerCase()'}.${mode}('${sensitive ? value : value.toLowerCase()}')`
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
	<Card title="Logs" class="h-full gap-low! overflow-hidden pt-low!">
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
								{#snippet lead()}<Ico {icon} />{/snippet}{name}
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
								<div class="layout-x-end-low flex-wrap">
									<div class="layout-x-baseline-low flex-wrap">
										<Popover
											triggerBase="button-primary"
											arrow
											arrowBackground="preset-glass-primary"
											open={presetOpened}
											onOpenChange={(e) => (presetOpened = e.open)}
										>
											{#snippet trigger()}Preset<Ico
													icon="mdi:clock-star-four-points-outline"
												/>{/snippet}
											{#snippet content()}
												<Card bg="preset-glass-primary" class="p-low!">
													<div class="layout-y-stretch-low">
														{#each presets as { label, onclick }}
															<Button
																{label}
																class="button-primary bg-primary-50-950 odd:bg-primary-50-950/75"
																{onclick}
															/>
														{/each}
													</div>
												</Card>
											{/snippet}
										</Popover>
										<TimePicker bind:inputValue={times[0]} />
										<DateRangePicker bind:start={dates[0]} bind:end={dates[1]} />
										<TimePicker bind:inputValue={times[1]} />
									</div>
									<Button
										size={4}
										label="Server filter"
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
											label="Copy client filters"
										/>
									{/if}
									<Button
										label="Server search"
										size={4}
										icon="mdi:receipt-text-send-outline"
										class="button-success w-fit!"
										onclick={refreshLogs}
									/>
									{#if showFilters}
										<PropertyType
											placeholder="Server filter…"
											bind:value={serverFilter}
											onkeyup={(e) => {
												if (e?.key == 'Enter') refreshLogs();
											}}
											class="preset-filled-secondary-50-950 motif-secondary"
										/>
									{/if}
								</div>
							</div>
						{/if}
						<div class="-mx -mb h-full">
							<LogViewer
								bind:this={logViewer}
								autoScroll={realtime}
								{startDate}
								{endDate}
								{realtime}
								{serverFilter}
								bind:filters
							/>
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
