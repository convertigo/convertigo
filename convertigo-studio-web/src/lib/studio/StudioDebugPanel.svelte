<script>
	import Button from '#lib/admin/components/Button.svelte';
	import CheckState from '#lib/admin/components/CheckState.svelte';
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
	let selectionRevision = 0;
	let eclipseRequests = $state(true);
	let inspectionTab = $state('Locals');
	let watchExpression = $state('');
	let watches = $state(/** @type {{id: number, expression: string, result: string}[]} */ ([]));
	let nextWatch = 0;
	let allVariables = $derived(
		(variables?.scopes ?? []).map((scope) => ({
			name: scope.name,
			value: '[Scope]',
			type: 'scope',
			ref: scope.ref,
			expandable: true
		}))
	);
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
			if (action === 'start' && eclipse && eclipseRequests && result?.state?.attached)
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
		await refreshWatches();
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
		if (!text) return;
		const result = await command('eval', {
			frame: String(frameIndex),
			expression: text,
			pauseId: String(snapshot?.pauseId)
		});
		if (
			revision !== selectionRevision ||
			!debugState?.stopped ||
			snapshot?.pauseId !== debugState.pauseId ||
			snapshot?.sessionId !== debugState.sessionId
		)
			return;
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
	<div class="debug-toolbar">
		<div class="debug-actions" role="group" aria-label="Debugger session">
			<Button
				label={debugState?.attached ? 'Stop debugger' : 'Start debugger'}
				icon={debugState?.attached ? 'mdi:stop' : 'mdi:bug-play-outline'}
				cls="button-secondary h-fit!"
				full={false}
				disabled={busy || debugState?.enabled === false}
				onclick={() => command(debugState?.attached ? 'stop' : 'start')}
			/>
		</div>
		{#if debugState?.attached}
			<div class="debug-actions" role="group" aria-label="Execution">
				<StudioIconButton
					icon="mdi:play"
					title="Continue"
					size="md"
					disabled={!debugState.stopped || busy}
					onclick={() => command('go')}
				/>
				<StudioIconButton
					icon="mdi:pause"
					title="Pause next"
					size="md"
					disabled={debugState.stopped || busy}
					onclick={() => command('pause')}
				/>
			</div>
			<div class="debug-actions" role="group" aria-label="Step">
				{#each [['stepInto', 'Step into', 'mdi:debug-step-into'], ['stepOver', 'Step over', 'mdi:debug-step-over'], ['stepOut', 'Step out', 'mdi:debug-step-out']] as [action, label, icon] (action)}
					<StudioIconButton
						{icon}
						title={label}
						size="md"
						disabled={!debugState.stopped || busy}
						onclick={() => command(action)}
					/>
				{/each}
			</div>
		{/if}
		<span
			role="status"
			class="debug-badge"
			class:paused={debugState?.stopped}
			class:running={debugState?.running && !debugState?.stopped}
			><span class="status-dot"></span>{stateLabel}</span
		>
	</div>
	{#if debugState?.enabled === false}<p role="status">
			JavaScript debugging is disabled on this server.
		</p>{/if}
	{#if eclipse}
		<div class="debug-options">
			<CheckState
				value={String(debugState?.attached ? debugState.studioRequests : eclipseRequests)}
				disabled={busy}
				ariaLabel="Debug Eclipse launches"
				onCheckedChange={(event) => {
					eclipseRequests = event.checked;
					if (debugState?.attached)
						void command('studioRequests', { value: String(eclipseRequests) });
				}}>Debug Eclipse launches</CheckState
			>
		</div>
	{/if}
	{#if debugState?.attached}
		<div class="debug-options" role="group" aria-label="Pause conditions">
			{#each [['breakOnExceptions', 'Break on exceptions'], ['breakOnEnter', 'Break on function entry'], ['breakOnReturn', 'Break on function return']] as [action, label] (action)}
				<CheckState
					value={String(debugState[action] ?? false)}
					disabled={busy}
					ariaLabel={label}
					onCheckedChange={(event) => command(action, { value: String(event.checked) })}
					>{label}</CheckState
				>
			{/each}
		</div>
		<p class="debug-help">
			{eclipse
				? debugState.studioRequests
					? 'Ready — Eclipse launches join this session.'
					: 'Ready — Eclipse launches are not selected.'
				: 'Ready — Web Studio requests join this session.'}
			{eclipse
				? 'Launch from the sequence, connector or test case editor. A second request is refused while this session is running.'
				: ''}
		</p>
	{/if}
	{#if standalone && !eclipse && debugState?.attached}
		<details>
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
	{#if !debugState?.attached && detachedNotice}<p role="status">{detachedNotice}</p>{/if}
	{#if debugState?.attached}
		<div class="debug-content" class:with-source={standalone && !eclipse}>
			{#if standalone && !eclipse}
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
				<div class="debug-stack" aria-label="Call stack">
					<h3>Call stack</h3>
					{#if debugState.stopped}
						{#each debugState.frames ?? [] as item, index (index)}
							<button
								class="debug-frame"
								class:selected={index === frameIndex}
								aria-pressed={index === frameIndex}
								aria-label={`${item.function || 'Script'}:${item.line}`}
								title={`${item.location?.label ?? item.url}:${item.line}`}
								onclick={() => selectFrame(index)}
							>
								<strong
									>{item.function || 'Script'}<span class="frame-line">:{item.line}</span></strong
								><span>{item.location?.label ?? item.url}</span>
							</button>
						{/each}
					{:else}<p class="debug-help">
							{debugState.running
								? 'Running — waiting for the next pause…'
								: 'Pause a script to inspect its stack and scopes.'}
						</p>{/if}
				</div>
				<div class="debug-properties">
					<div role="tablist" aria-label="Variable scopes" class="debug-tabs">
						{#each ['this', 'Locals', 'All scopes'] as tab (tab)}
							<button
								role="tab"
								aria-selected={inspectionTab === tab}
								class:selected={inspectionTab === tab}
								onclick={() => (inspectionTab = tab)}>{tab}</button
							>
						{/each}
					</div>
					<div role="tabpanel" aria-label={inspectionTab}>
						{#if debugState.stopped}
							{#key variables}
								{#key inspectionTab}
									<DebugVariables
										variables={inspectionTab === 'this'
											? (variables?.this ?? [])
											: inspectionTab === 'Locals'
												? (variables?.scope ?? [])
												: allVariables}
										reference={inspectionTab === 'this' ? variables?.thisRef : variables?.scopeRef}
										nextOffset={inspectionTab === 'this'
											? (variables?.thisNextOffset ?? -1)
											: inspectionTab === 'Locals'
												? (variables?.scopeNextOffset ?? -1)
												: -1}
										load={loadProperties}
										{loadValue}
									/>
								{/key}
							{/key}
						{:else}<p class="debug-help">Variables will appear at the next pause.</p>{/if}
					</div>
				</div>
				<div class="debug-expressions">
					<h3>Expressions</h3>
					<form class="debug-form" onsubmit={addWatch}>
						<input
							aria-label="Watch expression"
							class="input-common"
							placeholder="Watch expression"
							maxlength="4096"
							bind:value={watchExpression}
						/>
						<Button
							label="Add watch"
							type="submit"
							cls="button-secondary h-fit!"
							full={false}
							disabled={watches.length >= 20}
						/>
					</form>
					{#each watches as watch (watch.id)}
						<div class="debug-watch">
							<div>
								<code>{watch.expression}</code>
								<pre>{watch.result}</pre>
							</div>
							<StudioIconButton
								icon="mdi:close"
								title={`Remove watch ${watch.expression}`}
								onclick={() => (watches = watches.filter((item) => item.id !== watch.id))}
							/>
						</div>
					{/each}
					<p class="debug-help">
						Watched expressions execute on each pause and frame change and may have side effects.
					</p>
					{#if debugState.stopped}
						<form class="debug-form" onsubmit={evaluate}>
							<input
								aria-label="Evaluate an expression"
								class="input-common"
								placeholder="Evaluate an expression"
								maxlength="4096"
								bind:value={expression}
							/>
							<Button
								label="Evaluate"
								type="submit"
								cls="button-secondary h-fit!"
								full={false}
								disabled={busy}
							/>
						</form>
						{#each evaluations as evaluation, index (index)}<div class="debug-evaluation">
								<code>{evaluation.expression}</code>
								<pre>{evaluation.result}</pre>
							</div>{/each}
					{/if}
				</div>
			</div>
		</div>
	{/if}
</section>

<style>
	.debug-panel {
		display: flex;
		flex-direction: column;
		height: 100%;
		min-height: 0;
		min-width: 0;
		gap: 0.6rem;
		padding: 0.25rem;
		font-size: 0.82rem;
		overflow: auto;
		color: var(--color-surface-950-50);
	}
	.debug-toolbar {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 0.5rem;
		flex: none;
	}
	.debug-actions {
		display: flex;
		align-items: center;
		gap: 0.2rem;
		border-right: 1px solid var(--color-surface-300-700);
		padding-right: 0.5rem;
	}
	.debug-badge {
		display: inline-flex;
		align-items: center;
		gap: 0.4rem;
		border-radius: 999px;
		background: var(--color-surface-200-800);
		padding: 0.25rem 0.65rem;
		font-weight: 600;
	}
	.debug-badge.paused {
		background: color-mix(in oklab, var(--color-warning-500) 20%, transparent);
		color: var(--color-warning-800-200);
	}
	.debug-badge.running {
		background: color-mix(in oklab, var(--color-primary-500) 16%, transparent);
		color: var(--color-primary-700-300);
	}
	.status-dot {
		width: 0.45rem;
		height: 0.45rem;
		border-radius: 50%;
		background: currentColor;
	}
	.debug-options {
		display: flex;
		flex-wrap: wrap;
		gap: 0.6rem 1.2rem;
	}
	.debug-help {
		color: var(--color-surface-600-400);
		font-size: 0.75rem;
		overflow-wrap: anywhere;
	}
	.debug-notice {
		border-left: 3px solid var(--color-warning-500);
		background: color-mix(in oklab, var(--color-warning-500) 8%, transparent);
		padding: 0.5rem;
		overflow-wrap: anywhere;
	}
	.debug-content {
		min-height: 0;
		min-width: 0;
		flex: 1;
	}
	.debug-inspection {
		display: grid;
		grid-template-columns: minmax(10rem, 0.8fr) minmax(12rem, 1.4fr);
		gap: 0.75rem;
		align-items: start;
	}
	.debug-stack,
	.debug-properties,
	.debug-expressions {
		min-width: 0;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.35rem;
		overflow: hidden;
	}
	h3 {
		margin: 0;
		padding: 0.4rem 0.5rem;
		font-size: 0.75rem;
		font-weight: 600;
		background: var(--color-surface-100-900);
	}
	.debug-stack > p,
	.debug-properties p {
		padding: 0.5rem;
	}
	.debug-frame {
		display: flex;
		flex-direction: column;
		text-align: left;
		gap: 0.1rem;
		width: 100%;
		padding: 0.45rem 0.5rem;
		border-left: 3px solid transparent;
		overflow-wrap: anywhere;
	}
	.debug-frame > span {
		font-size: 0.72rem;
		color: var(--color-surface-600-400);
	}
	.debug-frame.selected {
		background: color-mix(in oklab, var(--color-primary-500) 13%, transparent);
		border-left-color: var(--color-primary-500);
	}
	.frame-line {
		margin-left: 0.2rem;
		color: var(--color-primary-600-400);
	}
	.debug-tabs {
		display: flex;
		background: var(--color-surface-100-900);
		border-bottom: 1px solid var(--color-surface-200-800);
	}
	.debug-tabs button {
		padding: 0.4rem 0.65rem;
		border-bottom: 2px solid transparent;
		color: var(--color-surface-600-400);
	}
	.debug-tabs button.selected {
		border-bottom-color: var(--color-primary-500);
		color: var(--color-primary-600-400);
		font-weight: 600;
		background: color-mix(in oklab, var(--color-primary-500) 10%, transparent);
	}
	.debug-expressions {
		grid-column: 1 / -1;
	}
	.debug-expressions > p {
		padding: 0.4rem 0.5rem;
	}
	.debug-form {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 0.4rem;
		padding: 0.5rem;
		min-width: 0;
	}
	.debug-form input {
		flex: 1 1 10rem;
		min-width: 0;
		width: 100%;
	}
	.debug-watch {
		display: flex;
		justify-content: space-between;
		align-items: start;
		gap: 0.4rem;
		padding: 0.4rem 0.5rem;
		border-top: 1px solid var(--color-surface-200-800);
	}
	.debug-watch > div {
		min-width: 0;
	}
	.debug-evaluation {
		padding: 0.4rem 0.5rem;
	}
	code,
	pre {
		white-space: pre-wrap;
		overflow-wrap: anywhere;
		font-size: 0.78rem;
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
	.with-source .debug-inspection {
		display: flex;
		flex-direction: column;
	}
	.with-source .debug-inspection > div {
		width: 100%;
	}
	button:focus-visible {
		outline: 2px solid var(--color-primary-500);
		outline-offset: -2px;
	}
	@media (max-width: 540px) {
		.debug-inspection,
		.with-source {
			display: flex;
			flex-direction: column;
		}
		.debug-inspection > div {
			width: 100%;
		}
	}
</style>
