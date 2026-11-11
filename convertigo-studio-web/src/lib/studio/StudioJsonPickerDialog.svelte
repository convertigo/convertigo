<script>
	import AutoSvg from '#lib/utils/AutoSvg.svelte';
	import { call, getUrl, toaster } from '#lib/utils/service.js';
	import {
		parseJavaScript,
		parseSample,
		sampleSummary,
		toJavaScript,
		toJsonPath
	} from './nativeJson.js';
	import { portal } from './portal.js';
	import StudioJsonTree from './StudioJsonTree.svelte';

	/**
	 * @typedef {{ id: string, name: string, icon?: string, variable: string, stream?: boolean, found?: boolean, count?: number, truncated?: boolean, note?: string }} JsonStep
	 */

	/**
	 * The JSON picker of the Eclipse Studio: the steps of the sequence before a step that give JSON, the JSON of
	 * their last execution in the Studio as a tree, and the JavaScript expression of the value picked. Opened from
	 * the menu of a JSON step it only shows (the expression can be copied); opened from a property it gives the
	 * expression to the property.
	 *
	 *
	 * @type {{
	 *  stepId: string,
	 *  includeStep?: boolean,
	 *  expression?: string,
	 *  purpose?: string,
	 *  onApply?: (expression: string) => void,
	 *  onClose?: () => void
	 * }}
	 */
	let { stepId, includeStep = false, expression = '', purpose = '', onApply, onClose } = $props();

	let steps = $state(/** @type {JsonStep[]} */ ([]));
	let sequenceName = $state('');
	let stepName = $state('');
	let loading = $state(true);
	let selectedStepId = $state('');
	let sample = $state(/** @type {any} */ (null));
	let path = $state(/** @type {(string | number)[] | null} */ (null));
	// the expression the dialog starts from, typed or picked
	let text = $state('');
	let initialized = false;

	let selectedStep = $derived(steps.find((step) => step.id === selectedStepId) ?? null);
	let parsed = $derived(sample?.found && sample.json ? parseSample(sample.json) : null);
	let jsonPathText = $derived.by(() => {
		if (!text.trim()) {
			return '';
		}
		for (const step of [...steps].reverse()) {
			const segments = parseJavaScript(step.variable, text);
			if (segments && !segments.includes(null)) {
				return toJsonPath(step.variable, /** @type {(string | number)[]} */ (segments));
			}
		}
		return '(a JavaScript expression)';
	});
	let info = $derived.by(() => {
		if (loading) {
			return '';
		}
		if (!steps.length) {
			return `No step before '${stepName}' gives JSON: the expression can use any variable of the sequence.`;
		}
		if (!selectedStep || !sample) {
			return '';
		}
		if (selectedStep.stream || (sample.note === 'stream' && !sample.json)) {
			return `'${selectedStep.name}' writes its rows in stream into the response: they are in the JSON output of the sequence.`;
		}
		if (!sample.found) {
			return `'${selectedStep.name}' has no sample yet: execute the sequence in the Studio to see its JSON. Its value is in the variable '${selectedStep.variable}'.`;
		}
		return sample.truncated ? '(sample cut at 4194304 characters)' : '';
	});

	$effect(() => {
		const id = stepId;
		void load(id);
	});

	/**
	 * @param {string} id
	 */
	async function load(id) {
		loading = true;
		const result = await call('studio.debug.NativeJson', {
			action: 'steps',
			id,
			includeStep: String(includeStep)
		});
		if (id !== stepId) {
			return;
		}
		steps = Array.isArray(result?.steps) ? result.steps : [];
		sequenceName = String(result?.sequenceName ?? '');
		stepName = String(result?.stepName ?? '');
		loading = false;
		if (!initialized) {
			initialized = true;
			text = expression ?? '';
			// the step and the value of the expression, the last matching step first, as the Eclipse picker
			for (const step of [...steps].reverse()) {
				const segments = parseJavaScript(step.variable, text);
				if (segments && !segments.includes(null)) {
					await selectStep(step, /** @type {(string | number)[]} */ (segments));
					return;
				}
			}
			const last = includeStep ? steps.find((step) => step.id === stepId) : steps.at(-1);
			if (last) {
				await selectStep(last, null);
			}
		}
	}

	/**
	 * @param {JsonStep} step
	 * @param {(string | number)[] | null} nextPath
	 */
	async function selectStep(step, nextPath) {
		selectedStepId = step.id;
		path = nextPath;
		sample = null;
		const result = await call('studio.debug.NativeJson', { action: 'sample', id: step.id });
		if (selectedStepId === step.id) {
			sample = result ?? { found: false };
		}
	}

	/**
	 * @param {(string | number)[]} nextPath
	 */
	function pick(nextPath) {
		if (!selectedStep) {
			return;
		}
		path = nextPath;
		text = toJavaScript(selectedStep.variable, nextPath);
	}

	async function copy() {
		try {
			await navigator.clipboard.writeText(text);
			toaster.info({ description: 'Expression copied.' });
		} catch {
			toaster.error({ description: 'The expression cannot be copied.' });
		}
	}

	function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		if (onApply) {
			onApply(text);
		} else {
			void copy();
		}
	}
</script>

<div class="studio-json-picker" role="presentation" use:portal onclick={() => onClose?.()}>
	<div
		class="studio-json-picker__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-json-picker-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape') onClose?.();
		}}
	>
		<header class="studio-json-picker__header">
			<strong id="studio-json-picker-title"
				>JSON picker{stepName && !onApply ? ` - ${stepName}` : ''}</strong
			>
			<p>
				Pick a value in the JSON of a step of '{sequenceName}'{purpose ? ` for ${purpose}` : ''}.
				The samples are the JSON of the last execution of the sequence in the Studio.
			</p>
		</header>
		<form class="studio-json-picker__form" onsubmit={submit}>
			<div class="studio-json-picker__body">
				<div class="studio-json-picker__steps">
					<table>
						<thead>
							<tr>
								<th>Step</th>
								<th>Variable</th>
								<th>Last sample</th>
							</tr>
						</thead>
						<tbody>
							{#each steps as step (step.id)}
								<tr
									class:studio-json-picker__row--selected={step.id === selectedStepId}
									onclick={() => selectStep(step, null)}
								>
									<td>
										<span class="studio-json-picker__step">
											{#if step.icon}
												<AutoSvg
													class="h-4 w-4"
													fill="currentColor"
													src="{getUrl()}{step.icon}"
													alt=""
												/>
											{/if}
											<button type="button" onclick={() => selectStep(step, null)}
												>{step.name}</button
											>
										</span>
									</td>
									<td>{step.variable}</td>
									<td>{sampleSummary(step)}</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
				<div class="studio-json-picker__tree">
					{#if parsed?.ok && selectedStep}
						<StudioJsonTree
							value={parsed.value}
							rootLabel={selectedStep.variable}
							selected={path}
							label={`JSON of ${selectedStep.name}`}
							onSelect={pick}
							onActivate={(nextPath) => {
								pick(nextPath);
								if (onApply) onApply(text);
							}}
						/>
					{:else if parsed && !parsed.ok}
						<pre>{parsed.text}</pre>
					{:else if loading}
						<p class="studio-json-picker__info">Loading…</p>
					{/if}
				</div>
			</div>
			{#if info}
				<p class="studio-json-picker__info" role="status">{info}</p>
			{/if}
			<label class="studio-json-picker__expression">
				<span>Expression</span>
				<input
					class="input-common"
					bind:value={text}
					readonly={!onApply}
					spellcheck="false"
					placeholder="A JavaScript expression of the sequence"
				/>
			</label>
			<p class="studio-json-picker__path">
				{#if jsonPathText}JSON path: <code>{jsonPathText}</code>{/if}
			</p>
			<footer class="studio-json-picker__footer">
				{#if onApply}
					<button type="button" class="button-secondary" onclick={() => onClose?.()}>Cancel</button>
					<button type="submit" class="button-primary">Apply</button>
				{:else}
					<button type="submit" class="button-secondary" disabled={!text}>Copy expression</button>
					<button type="button" class="button-primary" onclick={() => onClose?.()}>Close</button>
				{/if}
			</footer>
		</form>
	</div>
</div>

<style>
	.studio-json-picker {
		position: fixed;
		inset: 0;
		z-index: 90;
		display: grid;
		place-items: center;
		background: color-mix(in oklab, black 45%, transparent);
		padding: 1rem;
	}

	.studio-json-picker__box {
		display: grid;
		width: min(62rem, 100%);
		height: min(36rem, 100%);
		grid-template-rows: auto minmax(0, 1fr);
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
		font-size: 0.8rem;
	}

	.studio-json-picker__header {
		display: grid;
		gap: 0.25rem;
		border-bottom: 1px solid var(--studio-line);
		padding: 0.75rem 1rem;
	}

	.studio-json-picker__header strong {
		color: var(--studio-text-strong);
		font-size: 0.9rem;
		font-weight: 600;
	}

	.studio-json-picker__header p,
	.studio-json-picker__info,
	.studio-json-picker__path {
		margin: 0;
		color: var(--studio-text-idle);
		font-size: 0.74rem;
	}

	.studio-json-picker__form {
		display: grid;
		min-height: 0;
		grid-template-rows: minmax(0, 1fr) auto auto auto auto;
		gap: 0.5rem;
		padding: 0.75rem 1rem;
	}

	.studio-json-picker__body {
		display: grid;
		min-height: 0;
		grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
		gap: 0.6rem;
	}

	@media (max-width: 640px) {
		.studio-json-picker__body {
			grid-template-columns: minmax(0, 1fr);
		}
	}

	.studio-json-picker__steps,
	.studio-json-picker__tree {
		min-height: 0;
		overflow: auto;
		border: 1px solid var(--studio-line);
		border-radius: 0.4rem;
	}

	.studio-json-picker__tree {
		padding: 0.35rem;
	}

	.studio-json-picker__tree pre {
		margin: 0;
		white-space: pre-wrap;
	}

	.studio-json-picker__steps table {
		width: 100%;
		border-collapse: collapse;
	}

	.studio-json-picker__steps th,
	.studio-json-picker__steps td {
		padding: 0.25rem 0.45rem;
		text-align: start;
		white-space: nowrap;
	}

	.studio-json-picker__steps th {
		position: sticky;
		top: 0;
		background: var(--studio-chrome-bg);
		color: var(--studio-text-idle);
		font-weight: 600;
	}

	.studio-json-picker__steps tbody tr {
		cursor: pointer;
	}

	.studio-json-picker__steps tbody tr:hover {
		background: var(--studio-hover-bg);
	}

	.studio-json-picker__steps tbody tr.studio-json-picker__row--selected {
		background: light-dark(
			color-mix(in oklab, var(--color-primary-500) 16%, white),
			color-mix(in oklab, var(--color-primary-500) 22%, #1c1c1d)
		);
	}

	.studio-json-picker__step {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
	}

	.studio-json-picker__step button {
		border: 0;
		background: none;
		padding: 0;
		color: inherit;
		font: inherit;
		cursor: pointer;
	}

	.studio-json-picker__expression {
		display: grid;
		gap: 0.3rem;
	}

	.studio-json-picker__expression span {
		font-weight: 600;
	}

	.studio-json-picker__expression input {
		height: 2rem;
		padding-inline: 0.6rem;
		font-family: var(--font-mono, ui-monospace, monospace);
		font-size: 0.78rem;
	}

	.studio-json-picker__footer {
		display: flex;
		justify-content: flex-end;
		gap: 0.6rem;
	}
</style>
