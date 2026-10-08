<script>
	import Ico from '#lib/utils/Ico.svelte';
	import { call } from '#lib/utils/service.js';

	/**
	 * Imports a COBOL copybook into the input or the output map of a CICS transaction, as the "Import
	 * copybook" action of the Eclipse Studio: the copybook is read from a file or pasted.
	 *
	 * @type {{ id: string, onDone?: (id: string) => void | Promise<void>, onClose?: () => void }}
	 */
	let { id, onDone, onClose } = $props();

	/** @type {'input' | 'output'} */
	let map = $state('input');
	let copybook = $state('');
	let busy = $state(false);

	let name = $derived(id.split(/[.:]/).pop() ?? id);

	/**
	 * @param {Event & { currentTarget: HTMLInputElement }} event
	 */
	async function load(event) {
		const file = event.currentTarget.files?.[0];
		if (file) {
			copybook = await file.text();
		}
	}

	async function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		if (busy || !copybook.trim()) {
			return;
		}
		busy = true;
		try {
			const result = await call('studio.dbo.ImportCopybook', { id, map, copybook });
			if (result?.done) {
				await onDone?.(id);
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
		aria-labelledby="studio-copybook-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape' && !busy) onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-copybook-title">Import a copybook into {name}</strong>
		</header>
		<form class="studio-dialog__form" onsubmit={submit}>
			<fieldset class="studio-dialog__body" disabled={busy}>
				<div class="studio-dialog__choices" role="radiogroup" aria-label="Map">
					<label class="studio-dialog__check">
						<input type="radio" bind:group={map} value="input" /> Input map
					</label>
					<label class="studio-dialog__check">
						<input type="radio" bind:group={map} value="output" /> Output map
					</label>
				</div>
				<label class="studio-dialog__field">
					<span>Copybook file</span>
					<input class="studio-dialog__file input-common" type="file" onchange={load} />
				</label>
				<label class="studio-dialog__field">
					<span>Copybook</span>
					<textarea
						class="input-common"
						bind:value={copybook}
						spellcheck="false"
						placeholder={'       01 CUSTOMER.\n          05 CUST-ID    PIC 9(6).\n          05 CUST-NAME  PIC X(30).'}
					></textarea>
					<small>The fields of the copybook replace the ones of the map.</small>
				</label>
			</fieldset>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" disabled={busy} onclick={() => onClose?.()}
					>Cancel</button
				>
				<button type="submit" class="button-primary" disabled={busy || !copybook.trim()}>
					{#if busy}<Ico icon="mdi:sync" size={4} />{/if}
					Import
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
		justify-content: flex-start;
		border-bottom: 1px solid var(--studio-line);
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

	.studio-dialog__choices {
		display: flex;
		gap: 1rem;
	}

	.studio-dialog__check {
		display: flex;
		align-items: center;
		gap: 0.4rem;
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
</style>
