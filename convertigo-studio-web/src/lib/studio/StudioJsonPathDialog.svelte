<script>
	import { call } from '#lib/utils/service.js';
	import { jsonPathPreview, parseJsonPath, parseSample, toJsonPath } from './nativeJson.js';
	import { portal } from './portal.js';
	import StudioJsonTree from './StudioJsonTree.svelte';

	/**
	 * The JSON path editor of Read JSON value in the Eclipse Studio: the JSON text the step read at its last
	 * execution in the Studio as a tree, a value picked there writes its JSON path.
	 *
	 * @type {{
	 *  stepId: string,
	 *  label: string,
	 *  value: string,
	 *  onApply?: (value: string) => void,
	 *  onClose?: () => void
	 * }}
	 */
	let { stepId, label, value, onApply, onClose } = $props();

	const dialogId = $props.id();
	let path = $state('');
	let stepName = $state('');
	let input = $state(/** @type {string | null} */ (null));
	let loading = $state(true);
	let started = false;

	let parsed = $derived(input ? parseSample(input) : null);
	let selected = $derived.by(() => {
		const segments = parseJsonPath(path);
		return segments && !segments.includes(null)
			? /** @type {(string | number)[]} */ (segments)
			: null;
	});
	let preview = $derived(
		parsed?.ok
			? jsonPathPreview(parsed.value, path)
			: parseJsonPath(path)
				? ''
				: `Invalid JSON path: ${path}`
	);

	$effect(() => {
		if (!started) {
			started = true;
			path = String(value ?? '').trim() || '$';
		}
	});

	$effect(() => {
		const id = stepId;
		loading = true;
		void call('studio.debug.NativeJson', { action: 'sample', id }).then((response) => {
			if (id !== stepId) {
				return;
			}
			stepName = String(response?.stepName ?? '');
			input = typeof response?.input === 'string' ? response.input : null;
			loading = false;
		});
	});

	function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		onApply?.(path);
	}
</script>

<div class="studio-json-path" role="presentation" use:portal onclick={() => onClose?.()}>
	<div
		class="studio-json-path__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="{dialogId}-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape') onClose?.();
		}}
	>
		<header class="studio-json-path__header">
			<strong id="{dialogId}-title">{label}</strong>
			<p>
				The JSON path of the value read: <code>$</code> for the whole value, <code>.key</code>,
				<code>['key']</code>, <code>[index]</code>, <code>.*</code> and <code>[*]</code> for all the
				items. Pick a value in the JSON text '{stepName}' read at its last execution in the Studio.
			</p>
		</header>
		<form class="studio-json-path__form" onsubmit={submit}>
			<div class="studio-json-path__tree">
				{#if loading}
					<p class="studio-json-path__info">Loading…</p>
				{:else if parsed?.ok}
					<StudioJsonTree
						value={parsed.value}
						rootLabel="$"
						{selected}
						label="JSON text read"
						onSelect={(segments) => (path = toJsonPath('', segments))}
						onActivate={(segments) => onApply?.(toJsonPath('', segments))}
					/>
				{:else if parsed}
					<pre>{parsed.text}</pre>
				{:else}
					<p class="studio-json-path__info">
						'{stepName}' read no JSON text in an execution of the Studio yet: execute the sequence
						to pick the path in the text it reads.
					</p>
				{/if}
			</div>
			<label class="studio-json-path__field">
				<span>JSON path</span>
				<input class="input-common" bind:value={path} spellcheck="false" />
			</label>
			<p class="studio-json-path__info" role="status">{preview}</p>
			<footer class="studio-json-path__footer">
				<button type="button" class="button-secondary" onclick={() => onClose?.()}>Cancel</button>
				<button type="submit" class="button-primary" disabled={!parseJsonPath(path)}>Apply</button>
			</footer>
		</form>
	</div>
</div>

<style>
	.studio-json-path {
		position: fixed;
		inset: 0;
		z-index: 90;
		display: grid;
		place-items: center;
		background: color-mix(in oklab, black 45%, transparent);
		padding: 1rem;
	}

	.studio-json-path__box {
		display: grid;
		width: min(40rem, 100%);
		height: min(34rem, 100%);
		grid-template-rows: auto minmax(0, 1fr);
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
		font-size: 0.8rem;
	}

	.studio-json-path__header {
		display: grid;
		gap: 0.25rem;
		border-bottom: 1px solid var(--studio-line);
		padding: 0.75rem 1rem;
	}

	.studio-json-path__header strong {
		color: var(--studio-text-strong);
		font-size: 0.9rem;
		font-weight: 600;
	}

	.studio-json-path__header p,
	.studio-json-path__info {
		margin: 0;
		color: var(--studio-text-idle);
		font-size: 0.74rem;
	}

	.studio-json-path__form {
		display: grid;
		min-height: 0;
		grid-template-rows: minmax(0, 1fr) auto auto auto;
		gap: 0.5rem;
		padding: 0.75rem 1rem;
	}

	.studio-json-path__tree {
		min-height: 0;
		overflow: auto;
		border: 1px solid var(--studio-line);
		border-radius: 0.4rem;
		padding: 0.35rem;
	}

	.studio-json-path__tree pre {
		margin: 0;
		white-space: pre-wrap;
	}

	.studio-json-path__field {
		display: grid;
		gap: 0.3rem;
	}

	.studio-json-path__field span {
		font-weight: 600;
	}

	.studio-json-path__field input {
		height: 2rem;
		padding-inline: 0.6rem;
		font-family: var(--font-mono, ui-monospace, monospace);
		font-size: 0.78rem;
	}

	.studio-json-path__footer {
		display: flex;
		justify-content: flex-end;
		gap: 0.6rem;
	}
</style>
