<script>
	import { getLocalTimeZone, now, toCalendarDate, today, toTime } from '@internationalized/date';
	import { Popover, Slider } from '@skeletonlabs/skeleton-svelte';
	import { afterNavigate, beforeNavigate, goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import LogViewer from '$lib/admin/components/LogViewer.svelte';
	import MaxRectangle from '$lib/admin/components/MaxRectangle.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import SaveCancelButtons from '$lib/admin/components/SaveCancelButtons.svelte';
	import TimePicker from '$lib/admin/components/TimePicker.svelte';
	import Configuration from '$lib/admin/Configuration.svelte';
	import Instances from '$lib/admin/Instances.svelte';
	import LogsPurge from '$lib/admin/LogsPurge.svelte';
	import DateRangePicker from '$lib/common/components/DateRangePicker.svelte';
	import InputGroup from '$lib/common/components/InputGroup.svelte';
	import Time from '$lib/common/Time.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { getContext, onMount } from 'svelte';
	import { persistedState } from 'svelte-persisted-state';
	import { slide } from 'svelte/transition';
	import Last from '../Last.svelte';

	let logViewer = $state();
	let currentInstance = $state('');
	onMount(() => {
		currentInstance = Instances.current;

		let timezoneInitialized = false;
		const syncTimezonePreset = () => {
			if (timezoneInitialized || !Time.serverTimezone) return false;
			timezoneInitialized = true;
			presets[1].onclick();
			return true;
		};

		syncTimezonePreset();
		const timezoneInterval = setInterval(() => {
			if (syncTimezonePreset()) {
				clearInterval(timezoneInterval);
			}
		}, 200);

		const instanceInterval = setInterval(() => {
			if (Instances.current != currentInstance) {
				currentInstance = Instances.current;
				refreshLogs();
			}
		}, 500);

		return () => {
			clearInterval(instanceInterval);
			clearInterval(timezoneInterval);
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
				return;
			}
		}
	});

	let logsCategory = $derived(Configuration.categories.find(({ name }) => name == 'Logs'));
	const serverFilterState = persistedState('admin.logs.serverFilter', '', { syncTabs: false });
	let serverFilter = $state(serverFilterState.current);
	const serverFilterVisibleState = persistedState('admin.logs.serverFilterVisible', false, {
		syncTabs: false
	});
	let serverFilterVisible = $derived.by(
		() => serverFilterVisibleState.current || (serverFilter?.length ?? 0) > 0
	);

	$effect(() => {
		serverFilterState.current = serverFilter;
	});

	const filtersState = persistedState('admin.logs.filters', {}, { syncTabs: false });
	let filters = $derived(filtersState.current);
	let presetOpened = $state(false);

	const tzOffset = new Date().getTimezoneOffset() * 60000;

	const tabs = {
		view: { name: 'Viewer', icon: 'mdi:file-document-box-outline', viewer: true },
		purge: { name: 'Purge', icon: 'mdi:delete-outline' },
		config: { name: 'Log Levels', icon: 'mdi:cog-outline' }
	};

	const tabKeys = Object.keys(tabs);
	const tabSet = $derived.by(() => {
		const current = page.params.tab ?? 'view';
		return tabKeys.includes(current) ? current : 'view';
	});
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

	let startDate = $derived(dates[0].toString() + ' ' + times[0]);
	let endDate = $derived(dates[1] ? dates[1].toString() + ' ' + times[1] : '');
	let autoScroll = $state(false);
	let live = $state(false);

	async function refreshLogs() {
		if (logViewer?.list) {
			await logViewer.list(true);
		}
	}

	function setDatesTimes(start, end) {
		const range = [start, end];
		dates = range.map(toCalendarDate);
		times = range.map((date) => {
			let time = toTime(date).toString().replace('.', ',');
			return time.includes(',') ? time : time + ',000';
		});
		presetOpened = false;
	}

	let timezone = $derived(Time.serverTimezone ? Time.serverTimezone : getLocalTimeZone());
	afterNavigate(() => {
		Last.tab = tabSet;
		if (tabSet != 'purge') {
			LogsPurge.stop();
		}
	});

	const presets = [
		{
			label: 'now ⇒ live',
			onclick: () => {
				const nowDate = now(timezone);
				setDatesTimes(nowDate, nowDate.add({ days: 1 }));
				autoScroll = true;
				live = true;
				refreshLogs();
			}
		},
		{
			label: '1 min ago ⇒ live',
			onclick: () => {
				const nowDate = now(timezone);
				setDatesTimes(nowDate.copy().subtract({ minutes: 1 }), nowDate);
				live = true;
				refreshLogs();
			}
		},
		{
			label: '10 min ago ⇒ live',
			onclick: () => {
				const nowDate = now(timezone);
				setDatesTimes(nowDate.subtract({ minutes: 10 }), nowDate);
				live = true;
				refreshLogs();
			}
		},
		{
			label: '1 hour ago ⇒ live',
			onclick: () => {
				const nowDate = now(timezone);
				setDatesTimes(nowDate.subtract({ hours: 1 }), nowDate);
				live = true;
				refreshLogs();
			}
		},
		{
			label: 'today ⇒ live',
			onclick: () => {
				const todayDate = today(timezone);
				setDatesTimes(todayDate, todayDate.add({ days: 1 }));
				live = true;
				refreshLogs();
			}
		},
		{
			label: 'yesterday',
			onclick: () => {
				const yesterdayDate = today(timezone).subtract({ days: 1 });
				setDatesTimes(yesterdayDate, yesterdayDate.add({ days: 1 }));
				live = false;
				refreshLogs();
			}
		},
		{
			label: 'last 7 days',
			onclick: () => {
				const nowDate = now(timezone);
				setDatesTimes(nowDate.subtract({ days: 7 }), nowDate);
				live = true;
				refreshLogs();
			}
		},
		{
			label: 'last 30 days',
			onclick: () => {
				const nowDate = now(timezone);
				setDatesTimes(nowDate.subtract({ days: 30 }), nowDate);
				live = true;
				refreshLogs();
			}
		}
	];

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
</script>

<MaxRectangle delay={200} enabled={tabs[tabSet].viewer ?? false}>
	<Card title="Logs {timezone}" class="h-full gap-low! overflow-hidden pt-low!">
		{#snippet cornerOption()}
			<div class="layout-x-wrap w-full items-center gap">
				<PropertyType
					type="segment"
					name="logs-tabs"
					item={Object.entries(tabs).map(([value, { name, icon }]) => ({
						text: name,
						value,
						icon
					}))}
					bind:value={() => tabSet, (v) => goto(resolve(`/admin/logs/${v}`))}
					fit={true}
				/>

				<div class="grow">
					{#if tabSet == 'purge'}
						<ResponsiveButtons
							buttons={[
								{
									label: 'Purge',
									icon: 'mdi:content-save-edit-outline',
									cls: 'button-primary',
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
						<SaveCancelButtons
							onSave={saveChanges}
							onCancel={Configuration.refresh}
							changesPending={hasChanges}
						/>
					{/if}
				</div>
			</div>
		{/snippet}
		<div class="h-full">
			{#if tabSet == 'view'}
				<div class="layout-y-stretch-low h-full" transition:slide={{ axis: 'y' }}>
					<div transition:slide={{ axis: 'y' }}>
						<div class="relative z-10 layout-x-end-low flex-wrap items-center">
							<div class="layout-x-end-low flex-wrap items-center">
								<Popover open={presetOpened} onOpenChange={(e) => (presetOpened = e.open)}>
									<Popover.Trigger class="button-secondary layout-x-low h-9">
										Preset<Ico icon="mdi:clock-star-four-points-outline" />
									</Popover.Trigger>
									<Popover.Positioner class="z-60">
										<Popover.Content class="border-none bg-transparent p-0 shadow-none">
											<Card bg="preset-glass-surface" class="border-none! p-low! shadow-follow">
												<div class="layout-y-stretch-low">
													{#each presets as preset (preset.label)}
														<Button
															label={preset.label}
															class="button-primary"
															onclick={preset.onclick}
														/>
													{/each}
												</div>
											</Card>
											<Popover.Arrow class="fill-primary-200-800" />
										</Popover.Content>
									</Popover.Positioner>
								</Popover>
								<TimePicker bind:inputValue={times[0]} />
								<DateRangePicker bind:start={dates[0]} bind:end={dates[1]} bind:live />
								{#if !live}
									<span transition:slide={{ axis: 'x' }}>
										<TimePicker bind:inputValue={times[1]} />
									</span>
								{/if}
							</div>
							<Button
								size={4}
								label="Server filter"
								icon="mdi:filter-cog{serverFilterVisible ? '' : '-outline'}"
								onmousedown={() =>
									(serverFilterVisibleState.current = !serverFilterVisibleState.current)}
								class="button-secondary h-9! w-fit!"
							/>
							{#if serverFilterVisible}
								<Button
									size={4}
									icon="mdi:cloud-sync-outline"
									onmousedown={copyFilters}
									class="button-secondary h-9! w-fit!"
									label="Copy client filters"
									disabled={Object.keys(filters).length == 0}
								/>
							{/if}
							<Button
								label="Server search"
								size={4}
								icon="mdi:receipt-text-send-outline"
								class="button-primary h-9! w-fit!"
								onclick={refreshLogs}
							/>
							{#if serverFilterVisible}
								<InputGroup
									type="search"
									placeholder="Server filter…"
									icon="mdi:filter"
									class="h-9 bg-surface-200-800"
									inputClass="h-9 text-[13px] placeholder:text-[13px]"
									bind:value={serverFilter}
									onkeyup={(e) => {
										if (e?.key == 'Enter') refreshLogs();
									}}
								></InputGroup>
							{/if}
						</div>
					</div>
					<div class="-mx -mb h-full">
						<LogViewer
							bind:this={logViewer}
							{autoScroll}
							{startDate}
							{endDate}
							{live}
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
							>
								<Slider.Control class="relative w-full">
									<Slider.Track class="h-1 w-full rounded-full bg-surface-200-800">
										<Slider.Range class="h-full rounded-full bg-primary-500" />
									</Slider.Track>
									<Slider.Thumb index={0} class="h-5 w-5 rounded-full bg-primary-500" />
								</Slider.Control>
							</Slider>
						</AutoPlaceholder>
					</div>
					<AutoPlaceholder {loading}>
						<div class="layout-x justify-between">
							{#if idx >= 0}
								<div class="font-medium">Delete logs files older than {date}</div>
							{:else}
								<div class="font-medium">No selected date</div>
							{/if}
							<div class="text-xs">{idx + 1} / {dates.length}</div>
						</div>
					</AutoPlaceholder>
				</div>
			{:else}
				<div class="layout-grid-[300px]" transition:slide={{ axis: 'y' }}>
					{#each logsCategory?.property ?? [] as property (property.name)}
						{#if property.name?.startsWith('LOG4J')}
							<PropertyType {...property} bind:value={property.value} />
						{/if}
					{/each}
				</div>
			{/if}
		</div>
	</Card>
</MaxRectangle>
