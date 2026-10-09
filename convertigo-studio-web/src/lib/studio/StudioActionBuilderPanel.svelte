<script>
	import Ico from '#lib/utils/Ico.svelte';
	import { getStudioContextMenu, runStudioContextAction } from '#lib/utils/service.js';
	import { onDestroy } from 'svelte';
	import { fromAction } from 'svelte/attachments';
	import {
		refreshStudioBuilder,
		runStudioBuilderCommand,
		studioBuilderOperation
	} from './studioBuilder';
	import StudioShareQr from './StudioShareQr.svelte';

	/** @type {Record<string, any>} */
	let {
		projectName,
		builders,
		active = false,
		onLoad,
		onBuilt,
		onServerStop,
		onFailedChange,
		onActivity,
		serveRequest = 0,
		onServeRequestTaken,
		buildRequest = 0,
		onBuildRequestTaken
	} = $props();
	let builder = $state.raw();
	let selectedBuilder = $state('');
	let currentBuilder = $derived(
		builder === undefined
			? (builders.find((candidate) => candidate.id === selectedBuilder) ?? builders[0])
			: builder
	);
	let busy = $state('');
	let failed = $state(false);
	let lines = $state([]);
	let serial = 0;
	// the position reached in the log of the frontbuilder; false when the engine keeps no log (an older provider)
	let logNext = $state(0);
	let logsSupported = $state(true);
	let logReading = false;
	let disposed = false;
	onDestroy(() => {
		disposed = true;
	});

	async function execute(command, reveal = true) {
		if (busy || !currentBuilder) return;
		const selected = currentBuilder;
		busy = command;
		failed = false;
		onFailedChange?.(false);
		onActivity?.({ phase: command, progress: -1, result: '', serial });
		try {
			const current = await refreshStudioBuilder(selected, getStudioContextMenu);
			if (disposed) return;
			builder = current;
			const operation = studioBuilderOperation(current, command);
			if (!operation) {
				onActivity?.({ phase: '', progress: -1, result: '', serial });
				return;
			}
			const result = await runStudioBuilderCommand(current, operation, runStudioContextAction);
			if (disposed) return;
			await readLogs(selected.target);
			lines = [...lines.slice(-1999), result.message || `${operation} complete`];
			// the output of the steps is in the log; an older provider only gives it in the result
			if (!logsSupported) {
				for (const step of result.details?.steps ?? []) {
					if (step.stdout || step.output)
						lines = [...lines.slice(-1999), String(step.stdout || step.output)];
				}
			}
			builder = await refreshStudioBuilder(selected, getStudioContextMenu);
			if (disposed) return;
			// onLoad is the development preview callback; production uses onBuilt.
			if (reveal && command !== 'build' && result.openUrl) onLoad?.(result.openUrl);
			if (command === 'stop') onServerStop?.();
			if (command === 'build') onBuilt?.();
			onActivity?.({ phase: '', progress: -1, result: 'success', serial: ++serial });
		} catch (error) {
			if (!disposed) {
				failed = true;
				lines = [...lines.slice(-1999), error instanceof Error ? error.message : String(error)];
				onFailedChange?.(true);
				onActivity?.({ phase: '', progress: -1, result: 'failed', serial: ++serial });
			}
		} finally {
			if (!disposed) busy = '';
		}
	}

	/**
	 * Appends the lines of the frontbuilder log not shown yet: the actions of the project (generation, installs,
	 * builds) and the development server log there. Read out of the authoring lock, it follows a running build.
	 * @param {string} target
	 */
	async function readLogs(target) {
		if (!logsSupported || logReading || !target) return;
		logReading = true;
		try {
			const result = await runStudioContextAction(
				target,
				{ id: 'frontbuilder.svelte.logs', authoring: false, payload: { since: logNext } },
				{ silentError: () => true, silentStatuses: [401, 403, 404, 500, 502, 503] }
			);
			if (disposed) return;
			if (!result || result.ok === false || !Array.isArray(result.lines)) {
				logsSupported = false;
				return;
			}
			const added = [
				...(result.skipped > 0 ? [`… ${result.skipped} older lines are no longer kept`] : []),
				...result.lines
			];
			if (added.length) lines = [...lines, ...added].slice(-2000);
			logNext = Number(result.next) || logNext;
		} catch {
			// the next reading tries again
		} finally {
			logReading = false;
		}
	}

	// the log is read on activation, then every second while an action runs or the development server serves
	function followLogs(_node, value) {
		/** @type {ReturnType<typeof setInterval> | undefined} */
		let timer;
		function follow(current) {
			clearInterval(timer);
			if (!current.active || !current.target) return;
			void readLogs(current.target);
			timer = setInterval(() => {
				if (busy || currentBuilder?.state?.serving) void readLogs(current.target);
			}, 1000);
		}
		follow(value);
		return {
			update: follow,
			destroy: () => clearInterval(timer)
		};
	}

	function shareUrl() {
		const state = currentBuilder?.state;
		return String((state?.serving ? state?.url : state?.productionUrl) ?? '');
	}

	// External imperative requests belong to the mounted panel's lifecycle,
	// not to an effect which synchronizes reactive state.
	function takeServeRequest(_node, request) {
		function take(value) {
			if (!value) return;
			onServeRequestTaken?.();
			void execute(typeof value === 'object' && value.attach ? 'attach' : 'serve');
		}
		take(request);
		return { update: take };
	}
	function takeBuildRequest(_node, request) {
		function take(value) {
			if (!value) return;
			onBuildRequestTaken?.();
			void execute('build');
		}
		take(request);
		return { update: take };
	}
	function refreshOnActivate(_node, selection) {
		let generation = 0;
		function refresh(value) {
			const at = ++generation;
			if (!value.active) return;
			const candidate = builders.find((item) => item.id === value.selectedBuilder) ?? builders[0];
			void refreshStudioBuilder(candidate, getStudioContextMenu)
				.then((result) => {
					if (at === generation && !disposed && !busy) builder = result;
				})
				.catch((error) => {
					if (at === generation && !disposed) lines = [...lines.slice(-1999), error.message];
				});
		}
		refresh(selection);
		return {
			update: refresh,
			destroy: () => {
				generation++;
			}
		};
	}
</script>

<div
	class="builder"
	{@attach fromAction(takeServeRequest, () => serveRequest)}
	{@attach fromAction(takeBuildRequest, () => buildRequest)}
	{@attach fromAction(refreshOnActivate, () => ({ active, selectedBuilder }))}
	{@attach fromAction(followLogs, () => ({ active, target: currentBuilder?.target ?? '' }))}
>
	<div class="layout-x-wrap-low p-low">
		{#if builders.length > 1}
			<select
				class="input-common"
				aria-label="Frontend builder"
				disabled={Boolean(busy)}
				value={currentBuilder?.id ?? ''}
				onchange={(event) => {
					selectedBuilder = event.currentTarget.value;
					builder = undefined;
				}}
			>
				{#each builders as candidate (candidate.id)}
					<option value={candidate.id}>{candidate.label}</option>
				{/each}
			</select>
		{/if}
		<button
			class="button-primary"
			disabled={Boolean(busy) || !currentBuilder?.available}
			onclick={() => void execute('serve')}><Ico icon="mdi:play" size={4} /> Build and serve</button
		>
		<button
			class="button-secondary"
			disabled={Boolean(busy) || currentBuilder?.commands?.build?.enabled !== true}
			onclick={() => void execute('build')}>Production build</button
		>
		<button
			class="button-secondary"
			disabled={Boolean(busy) || currentBuilder?.commands?.stop?.enabled !== true}
			onclick={() => void execute('stop')}><Ico icon="mdi:stop" size={4} /> Stop</button
		>
		<button
			class="button-secondary"
			disabled={Boolean(busy) || currentBuilder?.commands?.open?.enabled !== true}
			onclick={() => void execute('open')}>Open</button
		>
		<StudioShareQr
			resolveUrl={shareUrl}
			available={Boolean(shareUrl())}
			buttonClass="button-secondary"
			title={currentBuilder?.state?.serving
				? 'QR code of the development server, to open it on a phone: it follows the edits (hot reload)'
				: 'QR code of the built application, to open it on a phone'}
		/>
		<span role="status"
			>{projectName} — {busy
				? `${busy}…`
				: failed
					? 'Build failed'
					: currentBuilder?.state?.serving
						? 'Serving in development mode'
						: 'Ready'}</span
		>
	</div>
	<div class="output p-low" aria-label="Build output">
		{#each lines as line, index (index)}<pre>{line}</pre>{:else}<p>
				Build and serve to follow the result here.
			</p>{/each}
	</div>
</div>

<style>
	.builder {
		display: grid;
		height: 100%;
		min-height: 0;
		grid-template-rows: auto minmax(0, 1fr);
	}
	.output {
		overflow: auto;
		font-size: 0.75rem;
	}
	pre {
		white-space: pre-wrap;
		overflow-wrap: anywhere;
		margin: 0;
	}
</style>
