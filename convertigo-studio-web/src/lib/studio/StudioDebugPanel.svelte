<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { call } from '$lib/utils/service';
	import { tick, untrack } from 'svelte';
	import { debugSession, followDebugger, refreshDebugger } from './debugSession.svelte.js';
	import StudioEmptyState from './StudioEmptyState.svelte';

	/**
	 * @typedef {import('./debugSession.svelte.js').DebugState} DebugState
	 */

	/**
	 * @typedef {{ url: string, source: string, breakpoints: number[], breakable: number[] }} DebugSource
	 */

	/**
	 * @typedef {{ name: string, value: string }} DebugVariable
	 */

	/**
	 * The JavaScript debugger of the Studio, as the Rhino debugger of the Eclipse Studio: the scripts of
	 * the objects run since it started, their breakpoints, and the frames, variables and expressions of a
	 * stopped script.
	 *
	 * @type {{ active?: boolean, onStopped?: () => void }}
	 */
	let { active = false, onStopped } = $props();

	let debugState = $derived(debugSession.state);
	let sourceUrl = $state('');
	let source = $state(/** @type {DebugSource | null} */ (null));
	let frameIndex = $state(0);
	let variables = $state(
		/** @type {{ scope: DebugVariable[], this: DebugVariable[] } | null} */ (null)
	);
	let expression = $state('');
	/** @type {{ expression: string, result: string }[]} */
	let evaluations = $state([]);
	let busy = $state('');
	let error = $state('');
	/** @type {HTMLElement | undefined} */
	let sourceElement = $state();

	let stoppedFrame = $derived(debugState?.stopped ? debugState.frames?.[frameIndex] : undefined);
	let lines = $derived(source ? source.source.split('\n') : []);
	let breakpoints = $derived(new Set(source?.breakpoints ?? []));
	let breakable = $derived(new Set(source?.breakable ?? []));
	let lastSerial = -1;
	let wasStopped = false;
	let stoppedFrames = '';

	$effect(() => {
		if (active) {
			untrack(() => void refreshDebugger());
		}
	});

	$effect(() => {
		// the state the debugger service returned, here or from the code editor
		const next = debugSession.state;
		if (next) {
			untrack(() => void apply(next));
		}
	});

	/**
	 * @param {string} action
	 * @param {Record<string, string>} [parameters]
	 */
	async function run(action, parameters = {}) {
		busy = action;
		error = '';
		try {
			const result = await call('studio.debug.Debugger', { action, ...parameters });
			if (result?.state) {
				followDebugger(result.state);
			} else if (result?.error) {
				error = String(result.error.message ?? result.error);
			}
			return result;
		} finally {
			busy = '';
		}
	}

	/**
	 * @param {DebugState} next
	 */
	async function apply(next) {
		const changed = next.serial !== lastSerial;
		lastSerial = next.serial;
		if (!changed) {
			return;
		}
		// a state also changes when a script loads: a stop is new when its frames are
		const frames = JSON.stringify(next.frames ?? []);
		const stoppedAgain = next.stopped && wasStopped && frames !== stoppedFrames;
		stoppedFrames = next.stopped ? frames : '';
		if (next.stopped && (!wasStopped || stoppedAgain)) {
			// a script stopped, or stopped again after a step: its source and variables show, as the
			// Eclipse Studio shows them
			if (!wasStopped) {
				onStopped?.();
			}
			frameIndex = 0;
			const frame = next.frames?.[0];
			if (frame && (!wasStopped || frame.url !== sourceUrl)) {
				await openSource(frame.url);
			}
			await loadVariables();
		} else if (!next.stopped) {
			variables = null;
		}
		wasStopped = next.stopped;
		if (!sourceUrl && next.sources.length) {
			await openSource(next.sources[next.sources.length - 1]);
		}
	}

	/**
	 * @param {string} url
	 */
	async function openSource(url) {
		sourceUrl = url;
		const result = await call('studio.debug.Debugger', { action: 'source', url });
		if (sourceUrl === url) {
			source = result?.source ?? null;
			await tick();
			sourceElement
				?.querySelector('.studio-debug__line--current')
				?.scrollIntoView({ block: 'center' });
		}
	}

	async function loadVariables() {
		const result = await call('studio.debug.Debugger', {
			action: 'variables',
			frame: String(frameIndex)
		});
		variables = result?.variables ?? null;
	}

	/**
	 * @param {number} index
	 */
	async function selectFrame(index) {
		frameIndex = index;
		const frame = debugState?.frames?.[index];
		if (frame) {
			await openSource(frame.url);
		}
		await loadVariables();
	}

	/**
	 * @param {number} line
	 */
	async function toggleBreakpoint(line) {
		if (!source || !breakable.has(line)) {
			return;
		}
		const set = !breakpoints.has(line);
		const result = await run('breakpoint', {
			url: source.url,
			line: String(line),
			set: String(set)
		});
		if (result?.accepted) {
			source.breakpoints = set
				? [...source.breakpoints, line]
				: source.breakpoints.filter((other) => other !== line);
		}
	}

	async function evaluate(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		const text = expression.trim();
		if (!text) {
			return;
		}
		const result = await run('eval', { expression: text });
		if (result && 'result' in result) {
			evaluations = [...evaluations, { expression: text, result: String(result.result) }].slice(
				-20
			);
			expression = '';
			await loadVariables();
		}
	}

	/**
	 * @param {string} url
	 * @returns {string} the object and the part of an object a script comes from
	 */
	function sourceLabel(url) {
		// the url is the short name of the object, then the name of the script, as Sequence_JS-expression
		const script = url.slice(url.indexOf('-') + 1);
		const index = script.lastIndexOf('-');
		return index > 0 ? `${script.slice(0, index)} › ${script.slice(index + 1)}` : script;
	}
</script>

<div class="studio-debug">
	<div class="studio-debug__toolbar">
		{#if debugState?.attached}
			<button
				type="button"
				class="studio-debug__tool"
				title="Stop the debugger"
				disabled={Boolean(busy)}
				onclick={() => run('stop')}
			>
				<Ico icon="mdi:stop" size={4} /> Stop
			</button>
			<span class="studio-debug__separator"></span>
			<button
				type="button"
				class="studio-debug__icon"
				title="Continue"
				aria-label="Continue"
				disabled={!debugState.stopped}
				onclick={() => run('go')}><Ico icon="mdi:play" size={4} /></button
			>
			<button
				type="button"
				class="studio-debug__icon"
				title="Step over"
				aria-label="Step over"
				disabled={!debugState.stopped}
				onclick={() => run('stepOver')}><Ico icon="mdi:debug-step-over" size={4} /></button
			>
			<button
				type="button"
				class="studio-debug__icon"
				title="Step into"
				aria-label="Step into"
				disabled={!debugState.stopped}
				onclick={() => run('stepInto')}><Ico icon="mdi:debug-step-into" size={4} /></button
			>
			<button
				type="button"
				class="studio-debug__icon"
				title="Step out"
				aria-label="Step out"
				disabled={!debugState.stopped}
				onclick={() => run('stepOut')}><Ico icon="mdi:debug-step-out" size={4} /></button
			>
			<button
				type="button"
				class="studio-debug__icon"
				title="Break at the next script"
				aria-label="Break"
				disabled={debugState.stopped}
				onclick={() => run('pause')}><Ico icon="mdi:pause" size={4} /></button
			>
			<label class="studio-debug__check">
				<input
					type="checkbox"
					checked={debugState.breakOnExceptions}
					onchange={(event) =>
						run('breakOnExceptions', { value: String(event.currentTarget.checked) })}
				/> Break on exceptions
			</label>
			<span class="studio-debug__status">
				{#if debugState.stopped}
					<Ico icon="mdi:pause-circle-outline" size={4} /> Stopped{debugState.alert
						? `: ${debugState.alert}`
						: ''}
				{:else}
					Running the scripts interpreted
				{/if}
			</span>
		{:else}
			<button
				type="button"
				class="studio-debug__start button-primary"
				disabled={Boolean(busy)}
				onclick={() => run('start')}
			>
				<Ico icon="mdi:bug-outline" size={4} /> Start the debugger
			</button>
			<span class="studio-debug__status">
				The scripts of the objects run interpreted while the debugger runs, then stop on its
				breakpoints.
			</span>
		{/if}
	</div>
	{#if error}
		<p class="studio-debug__error">{error}</p>
	{/if}
	{#if debugState?.attached}
		<div class="studio-debug__body">
			<div class="studio-debug__sources" aria-label="Scripts">
				{#each [...debugState.sources].reverse() as url (url)}
					<button
						type="button"
						class={['studio-debug__source', url === sourceUrl && 'studio-debug__source--active']}
						title={url}
						onclick={() => openSource(url)}>{sourceLabel(url)}</button
					>
				{:else}
					<p class="studio-debug__hint">Run a sequence or a transaction: its scripts show here.</p>
				{/each}
			</div>
			<div class="studio-debug__code" bind:this={sourceElement}>
				{#if source}
					{#each lines as text, index (index)}
						{@const line = index + 1}
						<div
							class={[
								'studio-debug__line',
								stoppedFrame?.url === source.url &&
									stoppedFrame.line === line &&
									'studio-debug__line--current'
							]}
						>
							<button
								type="button"
								class={[
									'studio-debug__gutter',
									breakpoints.has(line) && 'studio-debug__gutter--breakpoint',
									breakable.has(line) && 'studio-debug__gutter--breakable'
								]}
								title={breakable.has(line)
									? breakpoints.has(line)
										? 'Remove the breakpoint'
										: 'Add a breakpoint'
									: ''}
								disabled={!breakable.has(line)}
								onclick={() => toggleBreakpoint(line)}>{line}</button
							>
							<code>{text || ' '}</code>
						</div>
					{/each}
				{:else}
					<StudioEmptyState message="Choose a script to set its breakpoints" small />
				{/if}
			</div>
			<div class="studio-debug__inspect">
				{#if debugState.stopped}
					<div class="studio-debug__heading">Frames</div>
					{#each debugState.frames ?? [] as frame, index (index)}
						<button
							type="button"
							class={['studio-debug__frame', index === frameIndex && 'studio-debug__frame--active']}
							onclick={() => selectFrame(index)}
						>
							<span>{frame.function || sourceLabel(frame.url)}</span>
							<small>:{frame.line}</small>
						</button>
					{/each}
					<div class="studio-debug__heading">Variables</div>
					{#each variables?.scope ?? [] as variable (variable.name)}
						<div class="studio-debug__variable" title={variable.value}>
							<span>{variable.name}</span>
							<code>{variable.value}</code>
						</div>
					{/each}
					<form class="studio-debug__eval" onsubmit={evaluate}>
						{#each evaluations as evaluation, index (index)}
							<div class="studio-debug__variable">
								<span>{evaluation.expression}</span>
								<code>{evaluation.result}</code>
							</div>
						{/each}
						<input
							class="input-common"
							placeholder="Evaluate an expression"
							aria-label="Expression"
							bind:value={expression}
						/>
					</form>
				{:else}
					<p class="studio-debug__hint">The frames and variables of a stopped script show here.</p>
				{/if}
			</div>
		</div>
	{/if}
</div>

<style>
	.studio-debug {
		display: grid;
		height: 100%;
		min-height: 0;
		grid-template-rows: auto auto minmax(0, 1fr);
		font-size: 0.75rem;
	}

	.studio-debug__toolbar {
		display: flex;
		min-width: 0;
		align-items: center;
		gap: 0.2rem;
		border-bottom: 1px solid var(--studio-line);
		padding: 0.25rem 0.6rem;
	}

	.studio-debug__tool,
	.studio-debug__icon {
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
		border: 0;
		border-radius: 0.25rem;
		background: transparent;
		color: var(--studio-text);
		padding: 0.2rem 0.4rem;
	}

	.studio-debug__icon {
		width: 1.6rem;
		height: 1.6rem;
		justify-content: center;
		padding: 0;
	}

	.studio-debug__tool:hover:not(:disabled),
	.studio-debug__icon:hover:not(:disabled) {
		background: var(--studio-hover-bg);
	}

	.studio-debug__icon:disabled {
		opacity: 0.35;
	}

	.studio-debug__start {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
	}

	.studio-debug__separator {
		width: 1px;
		height: 1rem;
		margin: 0 0.3rem;
		background: var(--studio-line);
	}

	.studio-debug__check {
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
		margin-left: 0.5rem;
		white-space: nowrap;
	}

	.studio-debug__status {
		display: inline-flex;
		min-width: 0;
		align-items: center;
		gap: 0.3rem;
		margin-left: auto;
		overflow: hidden;
		color: var(--studio-text-idle);
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-debug__error {
		margin: 0;
		color: var(--color-error-600-400);
		padding: 0.25rem 0.6rem;
	}

	.studio-debug__body {
		display: grid;
		min-height: 0;
		grid-template-columns: minmax(8rem, 14rem) minmax(0, 1fr) minmax(12rem, 18rem);
	}

	.studio-debug__sources,
	.studio-debug__code,
	.studio-debug__inspect {
		min-height: 0;
		overflow: auto;
	}

	.studio-debug__sources {
		border-right: 1px solid var(--studio-line);
		padding: 0.25rem 0;
	}

	.studio-debug__source,
	.studio-debug__frame {
		display: flex;
		width: 100%;
		align-items: baseline;
		gap: 0.25rem;
		overflow: hidden;
		border: 0;
		background: transparent;
		color: var(--studio-text);
		padding: 0.15rem 0.6rem;
		text-align: left;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-debug__source:hover,
	.studio-debug__frame:hover {
		background: var(--studio-hover-bg);
	}

	.studio-debug__source--active,
	.studio-debug__frame--active {
		background: var(--studio-selection-bg);
		color: var(--studio-text-strong);
	}

	.studio-debug__frame small {
		color: var(--studio-text-idle);
	}

	.studio-debug__code {
		font-family: var(--font-mono, ui-monospace, monospace);
		font-size: 0.72rem;
		line-height: 1.5;
	}

	.studio-debug__line {
		display: flex;
		min-width: max-content;
	}

	.studio-debug__line--current {
		background: color-mix(in oklab, var(--color-warning-500) 22%, transparent);
	}

	.studio-debug__gutter {
		position: relative;
		width: 3.2rem;
		flex: none;
		border: 0;
		background: transparent;
		color: var(--studio-text-idle);
		padding: 0 0.6rem 0 1rem;
		font: inherit;
		text-align: right;
	}

	.studio-debug__gutter--breakable {
		cursor: pointer;
	}

	.studio-debug__gutter--breakable:hover::before,
	.studio-debug__gutter--breakpoint::before {
		position: absolute;
		top: 50%;
		left: 0.3rem;
		width: 0.55rem;
		height: 0.55rem;
		border-radius: 999px;
		background: var(--color-error-500);
		content: '';
		transform: translateY(-50%);
	}

	.studio-debug__gutter--breakable:hover:not(.studio-debug__gutter--breakpoint)::before {
		opacity: 0.35;
	}

	.studio-debug__line code {
		white-space: pre;
	}

	.studio-debug__inspect {
		border-left: 1px solid var(--studio-line);
		padding-bottom: 0.4rem;
	}

	.studio-debug__heading {
		color: var(--studio-text-idle);
		padding: 0.35rem 0.6rem 0.15rem;
		font-size: 0.68rem;
		font-weight: 600;
		letter-spacing: 0.04em;
		text-transform: uppercase;
	}

	.studio-debug__variable {
		display: flex;
		gap: 0.5rem;
		overflow: hidden;
		padding: 0.1rem 0.6rem;
		white-space: nowrap;
	}

	.studio-debug__variable span {
		flex: none;
		color: var(--studio-text-strong);
	}

	.studio-debug__variable code {
		min-width: 0;
		overflow: hidden;
		color: var(--studio-text-idle);
		text-overflow: ellipsis;
	}

	.studio-debug__eval {
		display: grid;
		gap: 0.2rem;
		padding-top: 0.35rem;
	}

	.studio-debug__eval input {
		height: 1.7rem;
		margin: 0 0.6rem;
		padding-block: 0;
		padding-inline: 0.5rem;
		font-size: 0.72rem;
	}

	.studio-debug__hint {
		margin: 0;
		color: var(--studio-text-idle);
		padding: 0.4rem 0.6rem;
	}
</style>
