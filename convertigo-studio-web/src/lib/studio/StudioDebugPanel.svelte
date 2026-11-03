<script>
	import Button from '#lib/admin/components/Button.svelte';
	import Editor from '#lib/studio/editor/Editor.svelte';
	import { callRequestable } from '#lib/utils/service.js';
	import { onMount } from 'svelte';
	import {
		debugCommand,
		debugSession,
		frameScopeGroups,
		inspectFrame,
		invalidateInspection,
		variablePreviews,
		watchDebugger
	} from './debugSession.svelte.js';
	import DebugVariables from './DebugVariables.svelte';
	import StudioIconButton from './StudioIconButton.svelte';

	/** @type {{active?: boolean, standalone?: boolean, eclipse?: boolean, onStopped?: () => void, onOpenSource?: (source: any, frame: any) => void}} */
	let { active = false, standalone = false, eclipse = false, onStopped, onOpenSource } = $props();
	let debugState = $derived(debugSession.state);
	let sourceUrl = $state('');
	let detachedNotice = $state('');
	let requestRevision = 0;
	let source = $state(/** @type {any} */ (null));
	let frameIndex = $state(0);
	let variables = $state(/** @type {any} */ (null));
	let expression = $state('');
	let evaluations = $state(
		/** @type {{expression: string, result: string, error: boolean, context: string}[]} */ ([])
	);
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
	let selectionRevision = 0;
	let addingWatch = $state(false);
	let watchExpression = $state('');
	let watches = $state(/** @type {{id: number, expression: string, result: string}[]} */ ([]));
	let nextWatch = 0;
	let scopeGroups = $derived(frameScopeGroups(variables, frame?.kind));
	let stateLabel = $derived(
		debugState?.stopped
			? 'Paused'
			: debugState?.running
				? 'Running'
				: debugState?.attached
					? 'Ready'
					: 'Detached'
	);

	onMount(() => watchDebugger(applyState));
	function applyState(next) {
		const pause = next?.stopped ? `${next.sessionId}:${next.pauseId}` : '';
		if (pause && pause !== lastPause) {
			frameIndex = 0;
			evaluations = [];
			error = '';
			requestResult = '';
			requestRevision++;
			onStopped?.();
			void selectFrame(0);
		}
		lastPause = pause;
		if (!pause) {
			selectionRevision++;
			variables = null;
			watches = watches.map((watch) => ({ ...watch, result: 'Waiting for pause' }));
		}
		if (!next?.attached) {
			source = null;
			sourceUrl = '';
			evaluations = [];
			watches = [];
		}
	}

	async function command(action, parameters = {}) {
		busy = true;
		error = '';
		if (action === 'start') detachedNotice = '';
		if (action === 'stop')
			detachedNotice = 'Debugger detached. The paused request resumes and finishes.';
		try {
			let result = await debugCommand(action, parameters);
			if (
				action === 'start' &&
				eclipse &&
				typeof (/** @type {any} */ (window).java?.receiveFromJS) === 'function' &&
				result?.state?.attached
			)
				result = await debugCommand('studioRequests', { value: 'true' });
			if (result?.error) error = String(result.error.message ?? result.error);
			return result;
		} finally {
			busy = false;
		}
	}
	async function openSource(url) {
		sourceUrl = url;
		const sessionId = debugState?.sessionId;
		const result = await debugCommand('source', { url });
		if (sourceUrl === url && debugState?.attached && debugState.sessionId === sessionId)
			source = result?.source ?? null;
	}
	async function selectFrame(index) {
		const revision = ++selectionRevision;
		frameIndex = index;
		debugSession.frameIndex = index;
		variables = null;
		const snapshot = debugState;
		const selected = snapshot?.frames?.[index];
		if (selected && (standalone || !eclipse)) {
			await openSource(selected.url);
			if (selectionRevision === revision && source) onOpenSource?.(source, selected);
		}
		if (selected && eclipse) {
			/** @type {any} */ (window).java?.receiveFromJS(
				JSON.stringify({
					type: 'debugger.frame',
					sessionId: snapshot.sessionId,
					pauseId: snapshot.pauseId,
					frame: index
				})
			);
		}
		if (
			selectionRevision !== revision ||
			!debugState?.stopped ||
			debugState.pauseId !== snapshot?.pauseId ||
			debugState.sessionId !== snapshot?.sessionId
		)
			return;
		const result = await inspectFrame(index);
		if (
			debugState?.stopped &&
			debugState.pauseId === snapshot?.pauseId &&
			debugState.sessionId === snapshot?.sessionId &&
			frameIndex === index &&
			selectionRevision === revision
		) {
			variables = result;
			await refreshWatches(snapshot, index, revision);
		}
	}
	async function refreshWatches(
		snapshot = debugState,
		index = frameIndex,
		revision = selectionRevision
	) {
		for (const watch of [...watches]) {
			if (
				!snapshot?.stopped ||
				!debugState?.stopped ||
				debugState.sessionId !== snapshot.sessionId ||
				debugState.pauseId !== snapshot.pauseId ||
				selectionRevision !== revision
			)
				return;
			const result = await debugCommand('eval', {
				frame: String(index),
				pauseId: String(snapshot.pauseId),
				expression: watch.expression
			});
			if (
				!debugState?.stopped ||
				debugState.sessionId !== snapshot.sessionId ||
				debugState.pauseId !== snapshot.pauseId ||
				selectionRevision !== revision
			)
				return;
			watches = watches.map((item) =>
				item.id === watch.id
					? {
							...item,
							result: String(result?.result ?? result?.error?.message ?? 'Evaluation failed')
						}
					: item
			);
		}
	}
	async function addWatch(event) {
		event.preventDefault();
		const text = watchExpression.trim();
		if (!text || watches.length >= 20) return;
		watches = [...watches, { id: nextWatch++, expression: text, result: 'Waiting for pause' }];
		watchExpression = '';
		addingWatch = false;
		await refreshWatches();
	}

	async function toggleBreakpoint(line) {
		if (!source) return;
		const url = source.url;
		const result = await command('breakpoint', {
			kind: frame?.kind && frame.kind !== 'rhino' ? 'participant' : 'rhino',
			url,
			line: String(line),
			set: String(!source.breakpoints.includes(line))
		});
		if (result?.accepted && source?.url === url) source.breakpoints = result.breakpoints;
		else if (result?.accepted === false) error = `No executable code at line ${line}.`;
	}
	async function loadProperties(ref, offset) {
		const snapshot = debugState,
			revision = selectionRevision;
		const result = await debugCommand('properties', {
			ref,
			offset: String(offset),
			pauseId: String(snapshot?.pauseId)
		});
		if (
			revision !== selectionRevision ||
			!debugState?.stopped ||
			debugState.pauseId !== snapshot?.pauseId ||
			debugState.sessionId !== snapshot?.sessionId
		)
			return null;
		if (result?.error) throw new Error(result.error.message);
		return result?.properties;
	}
	async function loadValue(ref) {
		const snapshot = debugState,
			revision = selectionRevision;
		const result = await debugCommand('value', { ref, pauseId: String(snapshot?.pauseId) });
		if (
			revision !== selectionRevision ||
			!debugState?.stopped ||
			debugState.pauseId !== snapshot?.pauseId ||
			debugState.sessionId !== snapshot?.sessionId
		)
			return null;
		return result?.value;
	}
	async function evaluate(event) {
		event.preventDefault();
		const text = expression.trim(),
			snapshot = debugState,
			revision = selectionRevision;
		if (!text || !snapshot?.stopped || busy) return;
		busy = true;
		try {
			const result = await debugCommand('eval', {
				frame: String(frameIndex),
				expression: text,
				pauseId: String(snapshot.pauseId)
			});
			if (
				revision !== selectionRevision ||
				!debugState?.stopped ||
				snapshot.pauseId !== debugState.pauseId ||
				snapshot.sessionId !== debugState.sessionId
			)
				return;
			const failed = Boolean(result?.error || result?.evaluationError);
			evaluations = [
				...evaluations,
				{
					expression: text,
					result: String(result?.result ?? result?.error?.message ?? 'Evaluation failed'),
					error: failed,
					context: `${frame?.function || 'Script'}:${frame?.line ?? '?'}`
				}
			].slice(-20);
			expression = '';
			if (!failed) {
				invalidateInspection();
				await selectFrame(frameIndex);
			}
		} finally {
			busy = false;
		}
	}
	async function runRequest(event) {
		event.preventDefault();
		requestRunning = true;
		requestResult = '';
		const revision = ++requestRevision;
		try {
			const response = await callRequestable('json', project, {
				__sequence: sequence,
				__context: '*'
			});
			if (revision === requestRevision)
				requestResult = response.ok ? 'Request finished.' : `Request failed (${response.status}).`;
		} catch {
			if (revision === requestRevision) requestResult = 'Request failed.';
		} finally {
			requestRunning = false;
		}
	}
</script>

<section class="debug-panel" aria-label="JavaScript debugger" data-active={active}>
	<div class="debug-toolbar" role="toolbar" aria-label="Debugger controls">
		<div class="debug-actions" role="group" aria-label="Debugger session">
			<StudioIconButton
				icon={debugState?.attached ? 'mdi:stop' : 'mdi:bug-play-outline'}
				title={debugState?.attached ? 'Stop debugger' : 'Start debugger'}
				active={debugState?.attached}
				disabled={busy || debugState?.enabled === false}
				onclick={() => command(debugState?.attached ? 'stop' : 'start')}
			/>
		</div>
		<div class="debug-actions" role="group" aria-label="Execution">
			<StudioIconButton
				icon="mdi:play"
				title="Continue"
				disabled={!debugState?.stopped || busy}
				onclick={() => command('go')}
			/>
			<StudioIconButton
				icon="mdi:pause"
				title="Pause next"
				disabled={!debugState?.attached || debugState?.stopped || busy}
				onclick={() => command('pause')}
			/>
		</div>
		<div class="debug-actions" role="group" aria-label="Step">
			{#each [['stepOver', 'Step over', 'mdi:debug-step-over'], ['stepInto', 'Step into', 'mdi:debug-step-into'], ['stepOut', 'Step out', 'mdi:debug-step-out']] as [action, label, icon] (action)}
				<StudioIconButton
					{icon}
					title={label}
					disabled={!debugState?.stopped || busy}
					onclick={() => command(action)}
				/>
			{/each}
		</div>
		{#if debugState?.rhinoAvailable !== false}
			<div class="debug-actions debug-modes" role="group" aria-label="Pause conditions">
				{#each [['breakOnExceptions', 'Break on exceptions', 'mdi:alert-octagon-outline'], ['breakOnEnter', 'Break on function entry', 'mdi:arrow-collapse-down'], ['breakOnReturn', 'Break on function return', 'mdi:arrow-collapse-up']] as [action, label, icon] (action)}
					<StudioIconButton
						{icon}
						title={label}
						active={debugState?.[action] ?? false}
						aria-pressed={debugState?.[action] ?? false}
						disabled={!debugState?.attached || busy}
						onclick={() => command(action, { value: String(!debugState?.[action]) })}
					/>
				{/each}
			</div>
		{/if}
	</div>
	<div
		role="status"
		class={[
			'debug-status',
			{ paused: debugState?.stopped, running: debugState?.running && !debugState?.stopped }
		]}
	>
		<span class="status-dot"></span><strong>{stateLabel}</strong>
		{#if frame}<span class="truncate">{frame.function || 'Script'}:{frame.line}</span>{/if}
	</div>
	{#if debugState?.enabled === false}<p class="debug-notice" role="status">
			JavaScript debugging is disabled on this server.
		</p>{/if}
	{#if debugState?.attached}
		<p class="debug-help">
			{debugState.rhinoRequested && debugState.rhinoAvailable
				? 'Rhino source debugging: selected requests use private code and fresh library state.'
				: 'Block debugging: compiled code, prepared runners and library state stay shared.'}
		</p>
		{#each debugState.targets ?? [] as target (target.id)}
			{#if !target.selected}
				<button
					type="button"
					class="debug-help"
					disabled={busy || debugState.running || debugState.rhinoRequested}
					onclick={() => command('attach', { target: target.id })}
					>Attach to running request: {target.label}</button
				>
			{/if}
		{/each}
	{/if}
	{#if debugState?.attached && !debugState.running && !debugState.stopped}
		<p class="debug-help debug-ready">
			{eclipse
				? 'Run a sequence, transaction or test case from its editor.'
				: 'Run a request from the Studio to debug it.'}
		</p>
	{/if}
	{#if standalone && !eclipse && debugState?.attached}
		<details class="debug-run">
			<summary>Run a sequence from this view (optional)</summary>
			<form class="debug-form" onsubmit={runRequest}>
				<input
					class="input-common"
					aria-label="Project"
					placeholder="Project"
					required
					bind:value={project}
				/>
				<input
					class="input-common"
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
		</details>
	{/if}
	{#if error}<p class="debug-notice" role="alert">{error}</p>{/if}
	{#if debugState?.notice}<p class="debug-notice" role="status">{debugState.notice}</p>{/if}
	{#if !debugState?.attached && detachedNotice}<p class="debug-notice" role="status">
			{detachedNotice}
		</p>{/if}
	<div class={['debug-content', { 'with-source': standalone && !eclipse && debugState?.attached }]}>
		{#if standalone && !eclipse && debugState?.attached}
			<div class="debug-source">
				<select
					class="input-common"
					aria-label="Sources"
					value={sourceUrl}
					onchange={(event) => openSource(event.currentTarget.value)}
				>
					<option value="" disabled>Choose a script</option>
					{#each debugState.sources as url (url)}<option value={url}
							>{debugState.sourceLocations?.[url]?.label ?? url}</option
						>{/each}
				</select>
				<div class="min-h-64 min-w-0 grow" aria-label="Source editor">
					{#if source}<Editor
							content={source.source}
							language="javascript"
							readOnly
							breakpoints={source.breakpoints}
							onBreakpointToggle={toggleBreakpoint}
							currentLine={frame?.url === source.url ? (frame?.line ?? 0) : 0}
							{debugValues}
						/>{/if}
				</div>
			</div>
		{/if}
		<div class="debug-inspection">
			<section class="debug-section" aria-label="Watch">
				<div class="debug-section-heading">
					<h3>Watch</h3>
					<div class="layout-x gap-none">
						<StudioIconButton
							icon="mdi:plus"
							title="Add watch"
							size="xs"
							disabled={watches.length >= 20}
							onclick={() => (addingWatch = !addingWatch)}
							aria-expanded={addingWatch}
						/>
						<StudioIconButton
							icon="mdi:refresh"
							title="Refresh watches"
							size="xs"
							disabled={!debugState?.stopped || !watches.length || busy}
							onclick={() => refreshWatches()}
						/>
					</div>
				</div>
				{#if addingWatch}
					<form class="debug-form" onsubmit={addWatch}>
						<input
							aria-label="Watch expression"
							class="input-common"
							placeholder="Expression to watch"
							maxlength="4096"
							bind:value={watchExpression}
							{@attach (input) => {
								input.focus();
							}}
							onkeydown={(event) => {
								if (event.key === 'Escape') {
									addingWatch = false;
									watchExpression = '';
								}
							}}
						/>
						<StudioIconButton
							icon="mdi:check"
							title="Save watch"
							type="submit"
							disabled={!watchExpression.trim()}
						/>
						<StudioIconButton
							icon="mdi:close"
							title="Cancel watch"
							onclick={() => {
								addingWatch = false;
								watchExpression = '';
							}}
						/>
					</form>
				{/if}
				{#each watches as watch (watch.id)}
					<div class="debug-watch">
						<div>
							<code>{watch.expression}</code><span class="watch-separator">: </span>
							<pre>{watch.result}</pre>
						</div>
						<StudioIconButton
							icon="mdi:close"
							title={`Remove watch ${watch.expression}`}
							size="xs"
							onclick={() => (watches = watches.filter((item) => item.id !== watch.id))}
						/>
					</div>
				{:else}<p class="debug-help debug-empty">No watch expressions. Add one with +.</p>{/each}
			</section>
			<section class="debug-section debug-scopes" aria-label="Scope">
				<div class="debug-section-heading"><h3>Scope</h3></div>
				{#if debugState?.stopped}
					{#if variables}
						{#key variables}
							{#each scopeGroups as scope, index (index)}
								<details class="debug-scope" aria-label={scope.name} open={index < 2}>
									<summary>{scope.name}</summary>
									<DebugVariables
										variables={scope.variables ?? []}
										reference={scope.ref}
										nextOffset={scope.nextOffset ?? -1}
										load={loadProperties}
										{loadValue}
									/>
								</details>
							{/each}
						{/key}
					{:else}<p class="debug-help debug-empty">Loading scopes…</p>{/if}
				{:else}<p class="debug-help debug-empty">Variables will appear at the next pause.</p>{/if}
			</section>
			<section class="debug-section debug-stack" aria-label="Call stack">
				<div class="debug-section-heading"><h3>Call Stack</h3></div>
				{#if debugState?.stopped}
					{#each debugState.frames ?? [] as item, index (index)}
						<button
							class={['debug-frame', { selected: index === frameIndex }]}
							aria-pressed={index === frameIndex}
							aria-label={`${item.function || 'Script'}:${item.line}`}
							title={`${item.location?.label ?? item.url}:${item.line}`}
							onclick={() => selectFrame(index)}
						>
							<strong>{item.function || 'Script'}</strong><span class="frame-source"
								>{item.location?.label ?? item.url}</span
							><span class="frame-line">:{item.line}</span>
						</button>
					{/each}
				{:else}<p class="debug-help debug-empty">
						{debugState?.running
							? 'Running — waiting for the next pause…'
							: 'Pause a script to inspect its stack.'}
					</p>{/if}
			</section>
			<section class="debug-section debug-console" aria-label="Console">
				<div class="debug-section-heading">
					<h3>Console</h3>
					<StudioIconButton
						icon="mdi:delete-sweep-outline"
						title="Clear console"
						size="xs"
						disabled={!evaluations.length}
						onclick={() => (evaluations = [])}
					/>
				</div>
				<div
					class="debug-console-output"
					role="log"
					aria-label="Evaluation results"
					aria-live="polite"
				>
					{#each evaluations as evaluation, index (index)}
						<div class={['debug-evaluation', { 'evaluation-error': evaluation.error }]}>
							<div class="evaluation-input">
								<span aria-hidden="true">›</span><code>{evaluation.expression}</code><span
									class="evaluation-context">{evaluation.context}</span
								>
							</div>
							<div class="evaluation-output">
								<span aria-hidden="true">‹</span>
								<pre>{evaluation.result}</pre>
							</div>
						</div>
					{:else}<p class="debug-help debug-empty">
							{debugState?.stopped
								? 'Evaluate JavaScript in the selected frame.'
								: 'Pause a script to evaluate JavaScript.'}
						</p>{/each}
				</div>
				<form class="debug-console-input" onsubmit={evaluate}>
					<span aria-hidden="true">›</span><input
						aria-label="Evaluate an expression"
						placeholder={debugState?.stopped ? 'JavaScript expression' : 'Waiting for a pause…'}
						maxlength="4096"
						autocomplete="off"
						spellcheck="false"
						disabled={!debugState?.stopped || busy}
						bind:value={expression}
					/>
					<StudioIconButton
						icon="mdi:keyboard-return"
						title="Evaluate"
						type="submit"
						size="xs"
						disabled={!debugState?.stopped || busy || !expression.trim()}
					/>
				</form>
			</section>
		</div>
	</div>
</section>

<style>
	.debug-panel {
		display: flex;
		flex-direction: column;
		height: 100%;
		min-height: 0;
		min-width: 0;
		overflow: auto;
		color: var(--color-surface-950-50);
		font-size: 0.78rem;
	}
	.debug-toolbar {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 0.25rem;
		padding: 0.2rem 0.35rem;
		flex: none;
		position: sticky;
		top: 0;
		z-index: 2;
		background: var(--studio-panel-bg, var(--color-surface-50-950));
		border-bottom: 1px solid var(--color-surface-200-800);
	}
	.debug-actions {
		display: flex;
		align-items: center;
		gap: 0.1rem;
		border-right: 1px solid var(--color-surface-300-700);
		padding-right: 0.25rem;
	}
	.debug-modes {
		border: 0;
	}
	.debug-status {
		display: flex;
		align-items: center;
		gap: 0.4rem;
		padding: 0.35rem 0.65rem;
		flex: none;
		background: var(--color-surface-100-900);
		color: var(--color-surface-600-400);
		font-size: 0.75rem;
	}
	.debug-status.paused {
		background: color-mix(in oklab, var(--color-warning-500) 20%, transparent);
		color: var(--color-warning-800-200);
	}
	.debug-status.running {
		color: var(--color-primary-600-400);
	}
	.status-dot {
		flex: none;
		width: 0.4rem;
		height: 0.4rem;
		border-radius: 50%;
		background: currentColor;
	}
	.debug-help {
		color: var(--color-surface-600-400);
		font-size: 0.74rem;
		overflow-wrap: anywhere;
	}
	.debug-ready,
	.debug-notice {
		padding: 0.4rem 0.65rem;
	}
	.debug-notice {
		border-left: 3px solid var(--color-warning-500);
		background: color-mix(in oklab, var(--color-warning-500) 8%, transparent);
		overflow-wrap: anywhere;
	}
	.debug-content,
	.debug-inspection {
		display: flex;
		flex-direction: column;
		min-width: 0;
	}
	.debug-section {
		min-width: 0;
		border-bottom: 1px solid var(--color-surface-200-800);
	}
	.debug-section-heading {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 0.5rem;
		min-height: 1.9rem;
		padding: 0.15rem 0.55rem;
		background: var(--color-surface-100-900);
	}
	h3 {
		font-weight: 600;
		font-size: 0.75rem;
	}
	.debug-empty {
		padding: 0.5rem 0.65rem;
	}
	.debug-scope summary {
		padding: 0.3rem 0.65rem;
		font-weight: 500;
		cursor: pointer;
	}
	.debug-scope :global(.variables) {
		padding: 0 0.55rem 0.25rem 0.9rem;
	}
	.debug-frame {
		display: grid;
		grid-template-columns: minmax(4rem, 0.7fr) minmax(0, 1.8fr) auto;
		align-items: baseline;
		gap: 0.5rem;
		width: 100%;
		padding: 0.35rem 0.65rem;
		border-left: 3px solid transparent;
		text-align: left;
	}
	.debug-frame strong,
	.frame-source {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
	.debug-frame strong {
		font-weight: 500;
	}
	.frame-source {
		color: var(--color-surface-600-400);
		font-size: 0.7rem;
		text-align: right;
	}
	.debug-frame.selected {
		background: color-mix(in oklab, var(--color-primary-500) 13%, transparent);
		border-left-color: var(--color-primary-500);
	}
	.frame-line {
		font-family: monospace;
		color: var(--color-primary-600-400);
	}
	.debug-form {
		display: flex;
		align-items: center;
		flex-wrap: wrap;
		gap: 0.35rem;
		padding: 0.4rem 0.55rem;
		min-width: 0;
	}
	.debug-form input {
		flex: 1 1 8rem;
		min-width: 0;
		width: 100%;
		font-family: monospace;
		font-size: 0.78rem;
	}
	.debug-watch {
		display: flex;
		align-items: start;
		justify-content: space-between;
		gap: 0.4rem;
		padding: 0.3rem 0.55rem;
	}
	.debug-watch > div {
		min-width: 0;
		overflow-wrap: anywhere;
	}
	.debug-watch pre {
		display: inline;
	}
	.watch-separator {
		color: var(--color-surface-500);
	}
	.debug-console-output {
		max-height: 16rem;
		overflow: auto;
	}
	.debug-evaluation {
		padding: 0.35rem 0.55rem;
		border-bottom: 1px solid var(--color-surface-200-800);
	}
	.evaluation-input,
	.evaluation-output {
		display: flex;
		align-items: baseline;
		gap: 0.45rem;
		min-width: 0;
	}
	.evaluation-input > span:first-child,
	.evaluation-output > span:first-child {
		flex: none;
		color: var(--color-primary-600-400);
	}
	.evaluation-input code {
		flex: 1;
	}
	.evaluation-context {
		flex: none;
		font-size: 0.65rem;
		color: var(--color-surface-600-400);
	}
	.evaluation-error .evaluation-output {
		color: var(--color-error-600-400);
	}
	.debug-console-input {
		display: flex;
		align-items: center;
		gap: 0.45rem;
		min-width: 0;
		padding: 0.3rem 0.55rem;
		border-top: 1px solid var(--color-surface-200-800);
		color: var(--color-primary-600-400);
	}
	.debug-console-input input {
		width: 100%;
		min-width: 0;
		flex: 1;
		padding: 0.25rem 0;
		border: 0;
		outline: none;
		background: transparent;
		font: 0.78rem/1.5 monospace;
		color: var(--color-surface-950-50);
	}
	.debug-console-input:focus-within {
		box-shadow: inset 0 -2px var(--color-primary-500);
	}
	.debug-console-input input:disabled {
		color: var(--color-surface-600-400);
	}
	code,
	pre {
		white-space: pre-wrap;
		overflow-wrap: anywhere;
		font-size: 0.76rem;
	}
	.debug-source {
		display: flex;
		flex-direction: column;
		min-width: 0;
		min-height: 24rem;
	}
	.with-source {
		display: grid;
		grid-template-columns: minmax(18rem, 1.4fr) minmax(18rem, 1fr);
		gap: 0.75rem;
	}
	.debug-run {
		padding: 0.4rem 0.65rem;
	}
	button:focus-visible,
	summary:focus-visible {
		outline: 2px solid var(--color-primary-500);
		outline-offset: -2px;
	}
	@media (max-width: 540px) {
		.with-source {
			display: flex;
			flex-direction: column;
		}
	}
</style>
