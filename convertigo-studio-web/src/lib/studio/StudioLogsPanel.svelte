<script>
	import { getLocalTimeZone, now, toTime } from '@internationalized/date';
	import LogViewer from '$lib/admin/components/LogViewer.svelte';
	import Time from '$lib/common/Time.svelte';
	import { onMount, tick } from 'svelte';

	let logViewer = $state();
	let autoScroll = $state(true);
	let live = $state(true);
	let startDate = $state('');
	let endDate = $state('');
	let serverFilter = $state('');
	let filters = $state({});

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

	async function refreshLogs() {
		await tick();
		await logViewer?.list?.(true);
	}

	onMount(() => {
		const current = now(timezone);
		startDate = toLogDateTime(current.subtract({ minutes: 10 }));
		endDate = toLogDateTime(current);
		void refreshLogs();
	});
</script>

<div class="studio-logs">
	<LogViewer
		bind:this={logViewer}
		bind:autoScroll
		{startDate}
		{endDate}
		{live}
		{serverFilter}
		bind:filters
		studioMode={true}
	/>
</div>

<style>
	.studio-logs {
		width: 100%;
		height: 100%;
		min-height: 0;
		overflow: hidden;
	}
</style>
