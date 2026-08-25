<script>
	import { getLocalTimeZone, now, today, toTime } from '@internationalized/date';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import Button from '$lib/admin/components/Button.svelte';
	import LogViewer from '$lib/admin/components/LogViewer.svelte';
	import LogViewerConfiguration from '$lib/admin/components/LogViewerConfiguration.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import SaveCancelButtons from '$lib/admin/components/SaveCancelButtons.svelte';
	import Configuration from '$lib/admin/Configuration.svelte';
	import Time from '$lib/common/Time.svelte';
	import { getContext, onMount, tick } from 'svelte';
	import { persistedState } from 'svelte-persisted-state';
	import { SvelteURL } from 'svelte/reactivity';

	let logViewer = $state();
	let autoScroll = $state(true);
	let live = $state(true);
	let startDate = $state('');
	let endDate = $state('');
	let liveMinutes = $state(10);
	let studioMode = $derived(page.url.searchParams.get('studioMode') == 'true');
	let panel = $derived(page.url.searchParams.get('panel') == 'config' ? 'config' : 'view');

	const serverFilterState = persistedState('eclipse.logs.serverFilter', '', { syncTabs: false });
	let serverFilter = $state(serverFilterState.current);
	const filtersState = persistedState('eclipse.logs.filters', {}, { syncTabs: false });
	let filters = $state(filtersState.current);
	let logsCategory = $derived(
		panel == 'config' ? Configuration.categories.find(({ name }) => name == 'Logs') : undefined
	);
	let logLevelProperties = $derived(
		logsCategory?.property?.filter(({ name }) => name?.startsWith('LOG4J')) ?? []
	);
	let hasLogLevelChanges = $derived(
		logLevelProperties.some(({ value, originalValue }) => value != originalValue)
	);
	let modalYesNo = getContext('modalYesNo');

	$effect(() => {
		serverFilterState.current = serverFilter;
	});

	$effect(() => {
		filtersState.current = filters;
	});

	const timezone = $derived(Time.serverTimezone ? Time.serverTimezone : getLocalTimeZone());

	function pad(value, length = 2) {
		return String(value).padStart(length, '0');
	}

	function toLogDateTime(date) {
		const time = toTime(date).toString().replace('.', ',');
		return `${date.year}-${pad(date.month)}-${pad(date.day)} ${
			time.includes(',') ? time : time + ',000'
		}`;
	}

	function setLiveRange(minutes = liveMinutes) {
		liveMinutes = minutes;
		const current = now(timezone);
		const start = minutes == 0 ? today(timezone) : current.subtract({ minutes });
		startDate = toLogDateTime(start);
		endDate = toLogDateTime(current);
		live = true;
		autoScroll = true;
	}

	async function refreshLogs() {
		await tick();
		if (logViewer?.list) {
			await logViewer.list(true);
		}
	}

	async function saveLogLevels(event) {
		const toSave = logLevelProperties
			.filter(({ value, originalValue }) => value != originalValue)
			.map(({ name, value }) => ({
				'@_key': name,
				'@_value': value
			}));
		if (toSave.length == 0) {
			return;
		}
		const confirmed =
			!modalYesNo?.open ||
			(await modalYesNo.open({
				event,
				title: `Are you sure you want to save ${toSave.length} propert${
					toSave.length == 1 ? 'y' : 'ies'
				}?`
			}));
		if (confirmed) {
			await Configuration.updateConfigurations(toSave);
		}
	}

	async function setPanel(nextPanel) {
		if (nextPanel == 'view' && hasLogLevelChanges) {
			const confirmed =
				!modalYesNo?.open ||
				(await modalYesNo.open({
					title: 'You have unsaved changes!',
					message: 'Are you sure you want to continue?'
				}));
			if (!confirmed) {
				return;
			}
			await Configuration.refresh();
		}
		const url = new SvelteURL(page.url);
		if (nextPanel == 'config') {
			url.searchParams.set('panel', 'config');
		} else {
			url.searchParams.delete('panel');
		}
		url.hash = '';
		await goto(`${url.pathname}${url.search}`, {
			keepFocus: true,
			noScroll: true,
			replaceState: true
		});
		if (nextPanel == 'view') {
			await refreshLogs();
		}
	}

	onMount(() => {
		const minutesParam = page.url.searchParams.get('minutes');
		const minutes = minutesParam == null ? Number.NaN : Number(minutesParam);
		const filter = page.url.searchParams.get('filter');
		if (Number.isFinite(minutes) && minutes >= 0) {
			liveMinutes = minutes;
		}
		if (filter != null) {
			serverFilter = filter;
		}
		setLiveRange(liveMinutes);
		if (panel == 'view') {
			refreshLogs();
		}
		return () => {
			Configuration.stop();
		};
	});
</script>

<svelte:head>
	<title>Convertigo Engine Log Viewer</title>
</svelte:head>

<div class="h-full min-h-0 w-full overflow-hidden">
	{#if panel == 'config'}
		<div
			class="eclipse-log-levels layout-y-stretch-none h-full min-h-0 w-full overflow-hidden text-xs"
		>
			<div
				class="layout-x-wrap items-center gap-1 border-b border-surface-200-800 bg-surface-100-900 p-0.5"
			>
				<Button
					full={false}
					size={4}
					icon="mdi:arrow-left"
					title="Back to live logs"
					ariaLabel="Back to live logs"
					class="button-ico-primary h-6 w-6 justify-center p-0!"
					onclick={() => setPanel('view')}
				/>
				<div class="min-w-0 grow truncate font-medium">Log Levels</div>
				<SaveCancelButtons
					class="w-fit"
					saveLabel="Save"
					cancelLabel="Cancel"
					onSave={saveLogLevels}
					onCancel={Configuration.refresh}
					changesPending={hasLogLevelChanges}
				/>
			</div>
			<div class="log-levels-grid min-h-0 grow overflow-auto p-1">
				<LogViewerConfiguration />
				{#each logLevelProperties as property (property.name)}
					<div class="log-level-property">
						<PropertyType {...property} bind:value={property.value} />
					</div>
				{/each}
			</div>
		</div>
	{:else}
		<LogViewer
			bind:this={logViewer}
			bind:autoScroll
			{startDate}
			{endDate}
			{live}
			{serverFilter}
			bind:filters
			{studioMode}
			onConfigureLevels={() => setPanel('config')}
		/>
	{/if}
</div>

<style lang="postcss">
	@reference "../../../../../app.css";

	.log-levels-grid {
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(20rem, 1fr));
		gap: --spacing(1);
		align-content: start;
	}

	.log-level-property {
		@apply border-b border-surface-200-800 py-1;
	}

	.eclipse-log-levels :global(.label-common) {
		@apply text-[11px] leading-tight;
	}

	.eclipse-log-levels :global(.input-common) {
		@apply h-7 text-xs;
	}

	.eclipse-log-levels :global(button) {
		min-height: 1.5rem;
	}
</style>
