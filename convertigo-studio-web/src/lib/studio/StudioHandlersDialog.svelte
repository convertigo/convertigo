<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { call } from '$lib/utils/service';

	/**
	 * @typedef {{ name: string, label: string, enabled: boolean, exists: boolean }} EventHandler
	 * @typedef {{ name: string, entry: boolean, exit: boolean, children?: ScreenClassNode[] }} ScreenClassNode
	 * @typedef {{ name: string, depth: number, entry: boolean, exit: boolean }} ScreenClassRow
	 */

	/**
	 * Adds functions to the JavaScript handlers of a transaction, as the "New handler" dialog of the Eclipse
	 * Studio: the start of the transaction, the XML generation, and the entry and exit of screen classes.
	 *
	 * @type {{ id: string, onDone?: (id: string, handlers: string) => void | Promise<void>, onClose?: () => void }}
	 */
	let { id, onDone, onClose } = $props();

	/** @type {EventHandler[]} */
	let handlers = $state([]);
	/** @type {ScreenClassRow[]} */
	let screenClasses = $state([]);
	let chosenHandlers = $state(/** @type {string[]} */ ([]));
	let chosenScreenClasses = $state(/** @type {string[]} */ ([]));
	let entry = $state(true);
	let exit = $state(false);
	let loading = $state(true);
	let busy = $state(false);
	let error = $state('');

	let transactionName = $derived(id.split(/[.:]/).pop() ?? id);
	let canCreate = $derived(
		chosenHandlers.length > 0 || (chosenScreenClasses.length > 0 && (entry || exit))
	);

	$effect(() => {
		load(id);
	});

	/**
	 * @param {ScreenClassNode} node
	 * @param {number} depth
	 * @returns {ScreenClassRow[]}
	 */
	function flatten(node, depth) {
		return [
			{ name: node.name, depth, entry: node.entry, exit: node.exit },
			...(node.children ?? []).flatMap((child) => flatten(child, depth + 1))
		];
	}

	/**
	 * @param {string} target
	 */
	async function load(target) {
		loading = true;
		error = '';
		try {
			const result = await call('studio.treeview.Handlers', { id: target });
			handlers = Array.isArray(result?.handlers) ? result.handlers : [];
			screenClasses = result?.screenClasses ? flatten(result.screenClasses, 0) : [];
			if (!Array.isArray(result?.handlers)) {
				error = String(result?.error?.message ?? 'This object has no handlers.');
			}
		} finally {
			loading = false;
		}
	}

	async function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		if (busy || !canCreate) {
			return;
		}
		busy = true;
		error = '';
		try {
			const result = await call('studio.treeview.Handlers', {
				id,
				apply: JSON.stringify({
					handlers: chosenHandlers,
					screenClasses: chosenScreenClasses,
					entry,
					exit
				})
			});
			if (result?.done) {
				await onDone?.(String(result.id ?? id), String(result.handlers ?? ''));
			} else {
				error = String(result?.error?.message ?? 'The chosen handlers already exist.');
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
		aria-labelledby="studio-handlers-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape' && !busy) onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-handlers-title">New handler of {transactionName}</strong>
		</header>
		<form class="studio-dialog__form" onsubmit={submit}>
			<fieldset class="studio-dialog__body" disabled={busy || loading}>
				{#if loading}
					<p class="studio-dialog__hint"><Ico icon="mdi:sync" size={4} /> Reading its handlers…</p>
				{:else}
					<section class="studio-dialog__field" aria-label="Transaction events">
						<span>Transaction events</span>
						{#each handlers as handler (handler.name)}
							<label
								class="studio-dialog__check"
								class:studio-dialog__check--off={!handler.enabled}
							>
								<input
									type="checkbox"
									value={handler.name}
									bind:group={chosenHandlers}
									disabled={!handler.enabled || handler.exists}
								/>
								{handler.label}
								{#if handler.exists}<small>exists</small>{/if}
							</label>
						{/each}
					</section>
					{#if screenClasses.length}
						<section class="studio-dialog__field" aria-label="Screen classes">
							<span>Screen classes</span>
							<div class="studio-handlers__screens">
								{#each screenClasses as screenClass (screenClass.name)}
									<label
										class="studio-dialog__check"
										style:padding-inline-start="{screenClass.depth * 1.1}rem"
									>
										<input
											type="checkbox"
											value={screenClass.name}
											bind:group={chosenScreenClasses}
										/>
										{screenClass.name}
										{#if screenClass.entry || screenClass.exit}
											<small
												>{[screenClass.entry && 'entry', screenClass.exit && 'exit']
													.filter(Boolean)
													.join(' and ')} exists</small
											>
										{/if}
									</label>
								{/each}
							</div>
							<div class="studio-handlers__kinds">
								<label class="studio-dialog__check">
									<input type="checkbox" bind:checked={entry} /> Entry handler
								</label>
								<label class="studio-dialog__check">
									<input type="checkbox" bind:checked={exit} /> Exit handler
								</label>
							</div>
						</section>
					{/if}
					<p class="studio-dialog__hint">The existing functions stay as they are.</p>
				{/if}
				{#if error}
					<p class="studio-dialog__error" role="alert">{error}</p>
				{/if}
			</fieldset>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" disabled={busy} onclick={() => onClose?.()}
					>Cancel</button
				>
				<button type="submit" class="button-primary" disabled={busy || loading || !canCreate}>
					{#if busy}<Ico icon="mdi:sync" size={4} />{/if}
					Create
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
		width: min(32rem, 100%);
		max-height: min(40rem, 100%);
		grid-template-rows: auto minmax(0, 1fr);
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
	}

	.studio-dialog__form {
		display: grid;
		min-height: 0;
		grid-template-rows: minmax(0, 1fr) auto;
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
		display: grid;
		min-width: 0;
		min-height: 0;
		grid-template-columns: minmax(0, 1fr);
		align-content: start;
		gap: 0.9rem;
		overflow-y: auto;
		margin: 0;
		border: 0;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-dialog__field {
		display: grid;
		gap: 0.35rem;
	}

	.studio-dialog__field > span {
		font-weight: 600;
	}

	.studio-dialog__check {
		display: flex;
		align-items: center;
		gap: 0.45rem;
	}

	.studio-dialog__check--off {
		color: var(--studio-text-idle);
	}

	.studio-dialog__check small,
	.studio-dialog__hint {
		margin: 0;
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-handlers__screens {
		display: grid;
		max-height: 12rem;
		gap: 0.3rem;
		overflow-y: auto;
		border: 1px solid var(--studio-line);
		border-radius: 0.3rem;
		padding: 0.45rem 0.6rem;
	}

	.studio-handlers__kinds {
		display: flex;
		gap: 1.2rem;
	}

	.studio-dialog__hint {
		display: flex;
		align-items: center;
		gap: 0.4rem;
	}

	.studio-dialog__error {
		margin: 0;
		color: var(--color-error-600-400);
		font-size: 0.75rem;
	}
</style>
