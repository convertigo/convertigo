<script>
	import { getLocalTimeZone, now, toTime } from '@internationalized/date';
	import LogViewer from '$lib/admin/components/LogViewer.svelte';
	import Time from '$lib/common/Time.svelte';
	import { onMount, tick } from 'svelte';
	import StudioLogLevelsDialog from './StudioLogLevelsDialog.svelte';

	let logViewer = $state();
	let autoScroll = $state(true);
	let live = $state(true);
	let startDate = $state('');
	let endDate = $state('');
	let serverFilter = $state('');
	let filters = $state({});
	/** the levels of the logs, as the Configure Log level of the log view of the Eclipse Studio */
	let levelsOpen = $state(false);
	/** @type {{ toolbarLead?: import('svelte').Snippet, toolbarTrail?: import('svelte').Snippet }} */
	let { toolbarLead, toolbarTrail } = $props();

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
		onConfigureLevels={() => (levelsOpen = true)}
		{toolbarLead}
		{toolbarTrail}
	/>
</div>

{#if levelsOpen}
	<StudioLogLevelsDialog onClose={() => (levelsOpen = false)} />
{/if}

<style>
	.studio-logs {
		width: 100%;
		height: 100%;
		min-height: 0;
		overflow: hidden;
	}

	/* The rows are on the background of the panel, as the output of Cursor: only the warnings and the
	   errors keep a light tint of their level */
	.studio-logs :global(.log-row.log-row) {
		--log-tint-weak: 0%;
		--log-tint-strong: 3%;
	}

	.studio-logs :global(.log-row.WARN),
	.studio-logs :global(.log-row.ERROR),
	.studio-logs :global(.log-row.FATAL) {
		--log-tint-weak: 10%;
		--log-tint-strong: 14%;
	}

	/* Quiet tool bar buttons, as the actions of a panel of Cursor */
	.studio-logs :global(.log-toolbar-button),
	.studio-logs :global(.log-toolbar-button-active) {
		border-color: transparent;
		background: transparent;
		box-shadow: none;
		color: var(--studio-text-idle);
	}

	.studio-logs :global(.log-toolbar-button:hover),
	.studio-logs :global(.log-toolbar-button-active:hover) {
		border-color: transparent;
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

	.studio-logs :global(.log-toolbar-button-active) {
		color: var(--color-primary-500);
	}

	.studio-logs :global(.log-toolbar-row) {
		border-color: transparent;
		border-radius: 0;
		background: transparent;
	}
</style>
