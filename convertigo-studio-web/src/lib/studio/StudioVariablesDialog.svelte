<script>
	import Ico from '#lib/utils/Ico.svelte';
	import { call, toaster } from '#lib/utils/service.js';

	/**
	 * @typedef {{ name: string, label: string, description: string, group: string, custom?: boolean, checked: boolean }} VariableOption
	 */

	/**
	 * @typedef {{ variable: string, label: string, name: string, value: string }} CustomVariable
	 */

	/**
	 * Chooses the variables of a transaction among the ones it can use, as the variables dialogs of the
	 * Eclipse Studio: the dynamic variables of an HTTP transaction, which also adds custom headers, POST or
	 * GET variables, and the parameters of a CouchDB transaction.
	 *
	 * @type {{ id: string, onDone?: (id: string) => void | Promise<void>, onClose?: () => void }}
	 */
	let { id, onDone, onClose } = $props();

	/** @type {VariableOption[]} */
	let options = $state([]);
	let removable = $state(false);
	/** @type {string[]} */
	let initial = $state([]);
	/** @type {CustomVariable[]} */
	let customs = $state([]);
	let loading = $state(true);
	let busy = $state(false);
	let error = $state('');

	let name = $derived(id.split(/[.:]/).pop() ?? id);
	let groups = $derived(
		[...new Set(options.map((option) => option.group))].map((group) => ({
			group,
			options: options.filter((option) => option.group === group)
		}))
	);

	$effect(() => {
		load(id);
	});

	/**
	 * @param {string} target
	 */
	async function load(target) {
		loading = true;
		error = '';
		try {
			const result = await call('studio.treeview.Variables', { id: target });
			options = Array.isArray(result?.options) ? result.options : [];
			removable = Boolean(result?.removable);
			initial = options.filter((option) => option.checked).map((option) => option.name);
			if (!options.length) {
				error = String(result?.error?.message ?? 'This transaction has no variable to add.');
			}
		} finally {
			loading = false;
		}
	}

	/**
	 * @param {VariableOption} option
	 */
	function addCustom(option) {
		customs = [...customs, { variable: option.name, label: option.label, name: '', value: '' }];
	}

	async function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		if (busy) {
			return;
		}
		busy = true;
		error = '';
		try {
			const result = await call('studio.treeview.Variables', {
				id,
				apply: JSON.stringify({
					names: options.filter((option) => option.checked).map((option) => option.name),
					customs: customs
						.filter((custom) => custom.name.trim())
						.map(({ variable, name, value }) => ({ variable, name: name.trim(), value }))
				})
			});
			if (result?.done) {
				if (!result.changed) {
					toaster.info({ description: 'The variables of the transaction are unchanged.' });
				}
				await onDone?.(id);
			} else {
				error = String(result?.error?.message ?? 'The variables were not changed.');
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
		aria-labelledby="studio-variables-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape' && !busy) onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-variables-title">Variables of {name}</strong>
		</header>
		<form class="studio-dialog__form" onsubmit={submit}>
			<fieldset class="studio-dialog__body" disabled={busy || loading}>
				{#if loading}
					<p class="studio-dialog__hint"><Ico icon="mdi:sync" size={4} /> Loading the variables…</p>
				{/if}
				{#each groups as { group, options: groupOptions } (group)}
					<section class="studio-variables__group">
						<h3>{group}</h3>
						{#each groupOptions as option (option.name)}
							{#if option.custom}
								<div class="studio-variables__option">
									<button
										type="button"
										class="studio-variables__add"
										title="Add a {option.label}"
										onclick={() => addCustom(option)}><Ico icon="mdi:plus" size={4} /></button
									>
									<span>
										<strong>{option.label}</strong>
										<small>{option.description}</small>
									</span>
								</div>
							{:else}
								<label class="studio-variables__option">
									<input
										type="checkbox"
										bind:checked={option.checked}
										disabled={!removable && initial.includes(option.name)}
									/>
									<span>
										<strong>{option.label}</strong>
										<small>{option.description}</small>
									</span>
								</label>
							{/if}
						{/each}
					</section>
				{/each}
				{#if customs.length}
					<section class="studio-variables__group">
						<h3>Variables to add</h3>
						{#each customs as custom, index (index)}
							<div class="studio-variables__custom">
								<span>{custom.label}</span>
								<input
									class="input-common"
									aria-label="Name of the {custom.label}"
									placeholder="name"
									bind:value={custom.name}
								/>
								<input
									class="input-common"
									aria-label="Value of the {custom.label}"
									placeholder="default value"
									bind:value={custom.value}
								/>
								<button
									type="button"
									class="studio-variables__add"
									title="Remove"
									aria-label="Remove"
									onclick={() => (customs = customs.filter((_, other) => other !== index))}
									><Ico icon="mdi:close" size={4} /></button
								>
							</div>
						{/each}
					</section>
				{/if}
				{#if error}
					<p class="studio-dialog__error" role="alert">{error}</p>
				{/if}
			</fieldset>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" disabled={busy} onclick={() => onClose?.()}
					>Cancel</button
				>
				<button type="submit" class="button-primary" disabled={busy || loading || !options.length}>
					{#if busy}<Ico icon="mdi:sync" size={4} />{/if}
					Apply
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
		min-width: 0;
		display: grid;
		min-height: 0;
		align-content: start;
		gap: 0.9rem;
		overflow-y: auto;
		margin: 0;
		border: 0;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-dialog__hint {
		display: flex;
		align-items: center;
		gap: 0.4rem;
		margin: 0;
		color: var(--studio-text-idle);
	}

	.studio-dialog__error {
		margin: 0;
		color: var(--color-error-600-400);
		font-size: 0.75rem;
	}

	.studio-variables__group {
		display: grid;
		gap: 0.35rem;
	}

	.studio-variables__group h3 {
		margin: 0;
		color: var(--studio-text-idle);
		font-size: 0.7rem;
		font-weight: 600;
		letter-spacing: 0.04em;
		text-transform: uppercase;
	}

	.studio-variables__option {
		display: grid;
		grid-template-columns: auto minmax(0, 1fr);
		align-items: start;
		gap: 0.55rem;
		border-radius: 0.3rem;
		padding: 0.3rem 0.35rem;
	}

	.studio-variables__option:hover {
		background: var(--studio-hover-bg);
	}

	.studio-variables__option input {
		margin-top: 0.15rem;
	}

	.studio-variables__option span {
		display: grid;
		gap: 0.1rem;
	}

	.studio-variables__option strong {
		color: var(--studio-text-strong);
		font-weight: 600;
	}

	.studio-variables__option small {
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-variables__add {
		display: inline-grid;
		width: 1.4rem;
		height: 1.4rem;
		place-items: center;
		border: 0;
		border-radius: 0.25rem;
		background: transparent;
		color: var(--studio-text-idle);
		padding: 0;
	}

	.studio-variables__add:hover {
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

	.studio-variables__custom {
		display: grid;
		grid-template-columns: 7rem minmax(0, 1fr) minmax(0, 1fr) auto;
		align-items: center;
		gap: 0.4rem;
	}

	.studio-variables__custom span {
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-variables__custom input {
		height: 1.8rem;
		padding-block: 0;
		padding-inline: 0.5rem;
		font-size: 0.78rem;
	}
</style>
