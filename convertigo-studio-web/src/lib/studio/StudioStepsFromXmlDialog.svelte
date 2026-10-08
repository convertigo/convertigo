<script>
	import Ico from '#lib/utils/Ico.svelte';
	import { call } from '#lib/utils/service.js';

	/**
	 * Creates the steps building an XML structure in a sequence or a step, as the "Create steps structure
	 * from XML" and "from XSD schema" actions of the Eclipse Studio: the structure is pasted or read from a
	 * file, or made from an element of a schema of the project or of a file.
	 *
	 * @type {{
	 *  id: string,
	 *  mode?: 'xml' | 'xsd',
	 *  onDone?: (id: string) => void | Promise<void>,
	 *  onClose?: () => void
	 * }}
	 */
	let { id, mode: initialMode = 'xml', onDone, onClose } = $props();

	/** @type {'xml' | 'xsd'} */
	let mode = $state('xml');
	let xml = $state('');
	/** @type {'project' | 'file'} */
	let schemaSource = $state('project');
	let schemaPath = $state('');
	let schemaText = $state('');
	/** @type {string[]} */
	let elements = $state([]);
	let element = $state('');
	let busy = $state(false);
	let error = $state('');

	$effect.pre(() => {
		mode = initialMode;
	});

	let name = $derived(id.split(/[.:]/).pop() ?? id);
	let schema = $derived(
		schemaSource === 'project' ? { path: schemaPath.trim() } : { xsd: schemaText }
	);
	let hasSchema = $derived(
		schemaSource === 'project' ? Boolean(schemaPath.trim()) : Boolean(schemaText)
	);
	let canCreate = $derived(mode === 'xml' ? Boolean(xml.trim()) : Boolean(element));

	/**
	 * @param {Event & { currentTarget: HTMLInputElement }} event
	 * @param {(text: string) => void} set
	 */
	async function load(event, set) {
		const file = event.currentTarget.files?.[0];
		if (file) {
			set(await file.text());
		}
	}

	function schemaChanged() {
		elements = [];
		element = '';
	}

	async function readElements() {
		busy = true;
		error = '';
		try {
			const result = await call('studio.treeview.StepsFromXsd', { id, ...schema });
			elements = Array.isArray(result?.elements) ? result.elements : [];
			element = elements[0] ?? '';
			if (!elements.length) {
				error = String(result?.error?.message ?? 'The schema has no element.');
			}
		} finally {
			busy = false;
		}
	}

	/**
	 * @param {string} qname
	 */
	function elementLabel(qname) {
		const match = qname.match(/^\{(.*)\}(.+)$/);
		return match ? `${match[2]}${match[1] ? ` (${match[1]})` : ''}` : qname;
	}

	async function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		if (busy || !canCreate) {
			return;
		}
		busy = true;
		error = '';
		try {
			const result =
				mode === 'xml'
					? await call('studio.treeview.StepsFromXml', { id, xml })
					: await call('studio.treeview.StepsFromXsd', { id, ...schema, element });
			if (result?.done) {
				await onDone?.(String(result.id ?? id));
			} else {
				error = String(result?.error?.message ?? 'The steps were not created.');
			}
		} finally {
			busy = false;
		}
	}
</script>

<div class="studio-dialog" role="presentation" onclick={() => !busy && onClose?.()}>
	<div
		class="studio-dialog__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-steps-xml-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape' && !busy) onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-steps-xml-title">Steps structure in {name}</strong>
			<div class="studio-dialog__modes" role="tablist" aria-label="Structure source">
				<button
					type="button"
					role="tab"
					aria-selected={mode === 'xml'}
					class={['studio-dialog__mode', mode === 'xml' && 'studio-dialog__mode--active']}
					onclick={() => (mode = 'xml')}>From XML</button
				>
				<button
					type="button"
					role="tab"
					aria-selected={mode === 'xsd'}
					class={['studio-dialog__mode', mode === 'xsd' && 'studio-dialog__mode--active']}
					onclick={() => (mode = 'xsd')}>From an XSD schema</button
				>
			</div>
		</header>
		<form class="studio-dialog__form" onsubmit={submit}>
			<fieldset class="studio-dialog__body" disabled={busy}>
				{#if mode === 'xml'}
					<label class="studio-dialog__field">
						<span>XML file</span>
						<input
							class="studio-dialog__file input-common"
							type="file"
							accept=".xml"
							onchange={(event) => load(event, (text) => (xml = text))}
						/>
					</label>
					<label class="studio-dialog__field">
						<span>XML structure</span>
						<textarea
							class="input-common"
							bind:value={xml}
							spellcheck="false"
							placeholder={'<person name="">\n\t<address city=""/>\n</person>'}></textarea>
						<small>Each element becomes an element step and each attribute an attribute step.</small
						>
					</label>
				{:else}
					<div class="studio-dialog__choices" role="radiogroup" aria-label="Schema">
						<label class="studio-dialog__check">
							<input
								type="radio"
								bind:group={schemaSource}
								value="project"
								onchange={schemaChanged}
							/> A schema of the project
						</label>
						<label class="studio-dialog__check">
							<input type="radio" bind:group={schemaSource} value="file" onchange={schemaChanged} /> A
							schema file
						</label>
					</div>
					{#if schemaSource === 'project'}
						<label class="studio-dialog__field">
							<span>Path of the schema in the project</span>
							<input
								class="input-common"
								bind:value={schemaPath}
								oninput={schemaChanged}
								placeholder="xsd/MySchema.xsd"
							/>
							<small>Its includes and imports are read beside it.</small>
						</label>
					{:else}
						<label class="studio-dialog__field">
							<span>Schema file</span>
							<input
								class="studio-dialog__file input-common"
								type="file"
								accept=".xsd"
								onchange={(event) =>
									load(event, (text) => {
										schemaText = text;
										schemaChanged();
									})}
							/>
						</label>
					{/if}
					<div class="studio-dialog__row">
						<button
							type="button"
							class="button-secondary"
							disabled={!hasSchema || busy}
							onclick={readElements}>Read the elements</button
						>
						{#if elements.length}
							<select class="select-common" aria-label="Element" bind:value={element}>
								{#each elements as qname (qname)}
									<option value={qname}>{elementLabel(qname)}</option>
								{/each}
							</select>
						{/if}
					</div>
				{/if}
				{#if error}
					<p class="studio-dialog__error" role="alert">{error}</p>
				{/if}
			</fieldset>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" disabled={busy} onclick={() => onClose?.()}
					>Cancel</button
				>
				<button type="submit" class="button-primary" disabled={busy || !canCreate}>
					{#if busy}<Ico icon="mdi:sync" size={4} />{/if}
					Create the steps
				</button>
			</footer>
		</form>
	</div>
</div>

<style>
	.studio-dialog {
		position: fixed;
		inset: 0;
		z-index: 90;
		display: grid;
		place-items: center;
		background: color-mix(in oklab, black 45%, transparent);
		padding: 1rem;
	}

	.studio-dialog__box {
		display: grid;
		width: min(36rem, 100%);
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
	}

	.studio-dialog__form {
		display: contents;
	}

	.studio-dialog__header,
	.studio-dialog__footer {
		display: flex;
		align-items: center;
		justify-content: flex-end;
		gap: 0.6rem;
		padding: 0.75rem 1rem;
	}

	.studio-dialog__header {
		justify-content: space-between;
		border-bottom: 1px solid var(--studio-line);
	}

	.studio-dialog__modes {
		display: flex;
		gap: 0.1rem;
		border: 1px solid var(--studio-line);
		border-radius: 0.4rem;
		padding: 0.12rem;
	}

	.studio-dialog__mode {
		border: 0;
		border-radius: 0.28rem;
		background: transparent;
		color: var(--studio-text-idle);
		padding: 0.25rem 0.6rem;
		font-size: 0.75rem;
		font-weight: 600;
	}

	.studio-dialog__mode--active {
		background: var(--studio-selection-bg);
		color: var(--studio-text-strong);
	}

	.studio-dialog__choices {
		display: flex;
		gap: 1rem;
	}

	.studio-dialog__check {
		display: flex;
		align-items: center;
		gap: 0.4rem;
	}

	.studio-dialog__row {
		display: flex;
		align-items: center;
		gap: 0.6rem;
	}

	.studio-dialog__row select {
		flex: 1;
		min-width: 0;
		height: 2rem;
		padding-block: 0;
		padding-inline: 0.6rem;
		font-size: 0.8rem;
	}

	.studio-dialog__field > input:not(.studio-dialog__file) {
		height: 2rem;
		padding-block: 0;
		padding-inline: 0.6rem;
		font-size: 0.8rem;
	}

	.studio-dialog__header strong {
		color: var(--studio-text-strong);
		font-size: 0.9rem;
		font-weight: 600;
	}

	.studio-dialog__footer {
		border-top: 1px solid var(--studio-line);
	}

	.studio-dialog__footer button {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
	}

	.studio-dialog__body {
		min-width: 0;
		display: grid;
		grid-template-columns: minmax(0, 1fr);
		gap: 0.7rem;
		margin: 0;
		border: 0;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-dialog__field {
		display: grid;
		gap: 0.3rem;
	}

	.studio-dialog__field > span {
		font-weight: 600;
	}

	.studio-dialog__field small {
		color: var(--studio-text-idle);
	}

	.studio-dialog__field textarea {
		min-height: 12rem;
		padding: 0.5rem 0.6rem;
		font-family: var(--font-mono, ui-monospace, monospace);
		font-size: 0.75rem;
		resize: vertical;
	}

	.studio-dialog__file {
		padding: 0.35rem 0.6rem;
	}

	.studio-dialog__file::file-selector-button {
		margin-right: 0.6rem;
		border: 1px solid var(--studio-line);
		border-radius: 0.3rem;
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
		padding: 0.2rem 0.6rem;
		font: inherit;
	}

	.studio-dialog__error {
		margin: 0;
		color: var(--color-error-600-400);
		font-size: 0.75rem;
	}
</style>
