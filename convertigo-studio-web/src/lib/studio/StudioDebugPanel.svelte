<script>
	import Button from '#lib/admin/components/Button.svelte';
	import Editor from '#lib/studio/editor/Editor.svelte';
	import { callRequestable } from '#lib/utils/service.js';
	import { onMount } from 'svelte';
	import {
		debugCommand,
		debugSession,
		inspectFrame,
		invalidateInspection,
		variablePreviews,
		watchDebugger
	} from './debugSession.svelte.js';
	import DebugVariables from './DebugVariables.svelte';

	/** @type {{active?: boolean, standalone?: boolean, onStopped?: () => void}} */
	let { active = false, standalone = false, onStopped } = $props();
	let debugState = $derived(debugSession.state);
	let sourceUrl = $state('');
	let source = $state(/** @type {any} */ (null));
	let frameIndex = $state(0);
	let variables = $state(/** @type {any} */ (null));
	let expression = $state('');
	let evaluations = $state(/** @type {{expression: string, result: string}[]} */ ([]));
	let busy = $state(false);
	let error = $state('');
	let project = $state('');
	let sequence = $state('');
	let requestRunning = $state(false);
	let requestResult = $state('');
	let frame = $derived(debugState?.stopped ? debugState.frames?.[frameIndex] : null);
	let debugValues = $derived(
		debugState?.stopped && frame?.url === sourceUrl ? variablePreviews(variables) : null
	);
	let lastPause = '';
	onMount(() => watchDebugger(applyState));
	function applyState(next) {
		const pause = next?.stopped ? `${next.sessionId}:${next.pauseId}` : '';
		if (pause && pause !== lastPause) {
			frameIndex = 0;
			onStopped?.();
			void selectFrame(0);
		}
		lastPause = pause;
		if (!pause) variables = null;
		if (!next?.attached) {
			source = null;
			sourceUrl = '';
			evaluations = [];
		}
	}

	async function command(action, parameters = {}) {
		busy = true;
		error = '';
		try {
			const result = await debugCommand(action, parameters);
			if (result?.error) error = String(result.error.message ?? result.error);
			return result;
		} finally {
			busy = false;
		}
	}
	async function openSource(url) {
		sourceUrl = url;
		const result = await debugCommand('source', { url });
		if (sourceUrl === url) source = result?.source ?? null;
	}
	async function selectFrame(index) {
		frameIndex = index;
		variables = null;
		const snapshot = debugState;
		const selected = snapshot?.frames?.[index];
		if (selected) await openSource(selected.url);
		const result = await inspectFrame(index);
		if (
			debugState?.stopped &&
			debugState.pauseId === snapshot?.pauseId &&
			debugState.sessionId === snapshot?.sessionId &&
			frameIndex === index
		)
			variables = result;
	}
	async function toggleBreakpoint(line) {
		if (!source) return;
		const url = source.url;
		const result = await command('breakpoint', {
			url,
			line: String(line),
			set: String(!source.breakpoints.includes(line))
		});
		if (result?.accepted && source?.url === url) source.breakpoints = result.breakpoints;
		else if (result?.accepted === false) error = `No executable code at line ${line}.`;
	}
	async function evaluate(event) {
		event.preventDefault();
		const text = expression.trim();
		if (!text) return;
		const result = await command('eval', { frame: String(frameIndex), expression: text });
		if (result && 'result' in result) {
			evaluations = [...evaluations, { expression: text, result: String(result.result) }].slice(
				-20
			);
			expression = '';
			invalidateInspection();
			await selectFrame(frameIndex);
		}
	}
	async function runRequest(event) {
		event.preventDefault();
		requestRunning = true;
		requestResult = '';
		try {
			const response = await callRequestable('json', project, {
				__sequence: sequence,
				__context: '*'
			});
			requestResult = response.ok ? 'Request finished.' : `Request failed (${response.status}).`;
		} catch {
			requestResult = 'Request failed.';
		} finally {
			requestRunning = false;
		}
	}
</script>

<section
	class="layout-y-stretch h-full min-h-0 gap-low text-sm"
	aria-label="JavaScript debugger"
	data-active={active}
>
	<div class="layout-x-wrap-low shrink-0">
		<Button
			label={debugState?.attached ? 'Stop debugger' : 'Start debugger'}
			cls="button-secondary h-fit!"
			full={false}
			disabled={busy || debugState?.enabled === false}
			onclick={() => command(debugState?.attached ? 'stop' : 'start')}
		/>
		{#if debugState?.attached}
			{#each [['go', 'Continue'], ['stepInto', 'Step into'], ['stepOver', 'Step over'], ['stepOut', 'Step out']] as [action, label] (action)}
				<Button
					{label}
					cls="button-secondary h-fit!"
					full={false}
					disabled={!debugState.stopped || busy}
					onclick={() => command(action)}
				/>
			{/each}
			<Button
				label="Pause next"
				cls="button-secondary h-fit!"
				full={false}
				disabled={debugState.stopped || busy}
				onclick={() => command('pause')}
			/>
			<label class="layout-x-low"
				><input
					type="checkbox"
					checked={debugState.breakOnExceptions}
					onchange={(event) =>
						command('breakOnExceptions', { value: String(event.currentTarget.checked) })}
				/>Break on exceptions</label
			>
		{/if}
		<span role="status"
			>{debugState?.enabled === false
				? 'JavaScript debugging is disabled on this server.'
				: debugState?.stopped
					? 'Paused'
					: debugState?.running
						? 'Running'
						: debugState?.attached
							? 'Ready — Studio requests join this session.'
							: 'Debugger detached'}</span
		>
	</div>
	{#if standalone && debugState?.attached}
		<form class="layout-x-wrap-low shrink-0" onsubmit={runRequest}>
			<input
				class="input-common w-52!"
				aria-label="Project"
				placeholder="Project"
				required
				bind:value={project}
			/>
			<input
				class="input-common w-52!"
				aria-label="Sequence"
				placeholder="Sequence"
				required
				bind:value={sequence}
			/>
			<Button
				label="Run sequence"
				type="submit"
				cls="button-primary h-fit!"
				full={false}
				disabled={requestRunning || debugState.running}
			/>
			<span role="status">{requestResult}</span>
		</form>
	{/if}
	{#if error}<p role="alert">{error}</p>{/if}
	{#if debugState?.notice}<p role="status">{debugState.notice}</p>{/if}
	{#if debugState?.attached}
		<div class="layout-x-stretch min-h-0 grow gap-low">
			<nav class="layout-y-start-low w-48 shrink-0 overflow-auto" aria-label="Sources">
				{#each debugState.sources as url (url)}
					<Button
						label={url}
						cls={url === sourceUrl ? 'button-primary h-fit!' : 'button-secondary h-fit!'}
						onclick={() => openSource(url)}
					/>
				{:else}<p>
						Run a sequence or transaction to load its scripts. Use Pause next to inspect the first
						line.
					</p>{/each}
			</nav>
			<div class="min-h-64 min-w-0 grow" aria-label="Source editor">
				{#if source}
					<Editor
						content={source.source}
						language="javascript"
						readOnly
						breakpoints={source.breakpoints}
						onBreakpointToggle={toggleBreakpoint}
						currentLine={frame?.url === source.url ? (frame?.line ?? 0) : 0}
						{debugValues}
					/>
				{/if}
			</div>
			<aside class="layout-y-stretch w-72 shrink-0 gap-low overflow-auto" aria-label="Inspection">
				{#if debugState.stopped}
					<strong>Call stack</strong>
					{#each debugState.frames ?? [] as item, index (index)}
						<Button
							label={`${item.function || item.url}:${item.line}`}
							cls={index === frameIndex ? 'button-primary h-fit!' : 'button-secondary h-fit!'}
							onclick={() => selectFrame(index)}
						/>
					{/each}
					{#each variables?.scopes ?? [] as scope (scope.name)}
						<details open>
							<summary>{scope.name}</summary>
							<DebugVariables variables={scope.variables} />
						</details>
					{/each}
					<details>
						<summary>this</summary><DebugVariables variables={variables?.this ?? []} />
					</details>
					<form class="layout-y-stretch gap-low" onsubmit={evaluate}>
						<label for="debug-expression">Evaluate an expression</label>
						<input
							id="debug-expression"
							class="input-common"
							maxlength="4096"
							bind:value={expression}
						/>
						<Button label="Evaluate" type="submit" cls="button-secondary h-fit!" disabled={busy} />
					</form>
					{#each evaluations as evaluation, index (index)}<div>
							<code>{evaluation.expression}</code>
							<pre class="whitespace-pre-wrap">{evaluation.result}</pre>
						</div>{/each}
				{:else}<p>Pause a script to inspect its stack and scopes.</p>{/if}
			</aside>
		</div>
	{/if}
</section>
