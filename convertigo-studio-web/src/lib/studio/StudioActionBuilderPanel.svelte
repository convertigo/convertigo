<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { getStudioContextMenu, runStudioContextAction } from '$lib/utils/service';
	import { onDestroy } from 'svelte';
	import { fromAction } from 'svelte/attachments';
	import {
		refreshStudioBuilder,
		runStudioBuilderCommand,
		studioBuilderOperation
	} from './studioBuilder';

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
			lines = [...lines.slice(-1999), result.message || `${operation} complete`];
			for (const step of result.details?.steps ?? []) {
				if (step.stdout || step.output)
					lines = [...lines.slice(-1999), String(step.stdout || step.output)];
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
				lines = [...lines.slice(-1999), error.message];
				onFailedChange?.(true);
				onActivity?.({ phase: '', progress: -1, result: 'failed', serial: ++serial });
			}
		} finally {
			if (!disposed) busy = '';
		}
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
