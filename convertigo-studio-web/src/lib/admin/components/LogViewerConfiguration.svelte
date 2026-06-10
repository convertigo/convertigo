<script>
	import {
		DEFAULT_MAX_LOADED_LOG_LINES,
		MAX_MAX_LOADED_LOG_LINES,
		maxLoadedLogLinesState,
		MIN_MAX_LOADED_LOG_LINES,
		normalizeMaxLoadedLogLines
	} from '$lib/admin/LogViewerSettings.svelte.js';
	import Button from './Button.svelte';
	import PropertyType from './PropertyType.svelte';

	let { class: cls = '' } = $props();
	let maxLoadedLines = $derived(normalizeMaxLoadedLogLines(maxLoadedLogLinesState.current));

	function normalize() {
		maxLoadedLogLinesState.current = normalizeMaxLoadedLogLines(maxLoadedLogLinesState.current);
	}

	function reset() {
		maxLoadedLogLinesState.current = DEFAULT_MAX_LOADED_LOG_LINES;
	}
</script>

<div class="layout-y-stretch-none gap-1 border-b border-surface-200-800 py-1 {cls}">
	<div class="layout-x-low items-center justify-between">
		<div class="min-w-0 truncate text-sm font-medium">Viewer Configuration</div>
		<Button
			full={false}
			size={4}
			icon="mdi:backup-restore"
			title="Restore default viewer configuration"
			ariaLabel="Restore default viewer configuration"
			disabled={maxLoadedLines == DEFAULT_MAX_LOADED_LOG_LINES}
			onclick={reset}
			class="inline-flex h-7 w-7 items-center justify-center p-0!"
		/>
	</div>
	<PropertyType
		type="number"
		name="logs-max-loaded-lines"
		label="Maximum loaded lines"
		tooltip={`Keep between ${MIN_MAX_LOADED_LOG_LINES.toLocaleString()} and ${MAX_MAX_LOADED_LOG_LINES.toLocaleString()} log lines in the browser buffer.`}
		min={MIN_MAX_LOADED_LOG_LINES}
		max={MAX_MAX_LOADED_LOG_LINES}
		step={1000}
		bind:value={maxLoadedLogLinesState.current}
		onchange={normalize}
		onblur={normalize}
	/>
</div>
