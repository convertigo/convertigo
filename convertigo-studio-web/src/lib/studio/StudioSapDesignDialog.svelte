<script>
	import Ico from '#lib/utils/Ico.svelte';
	import { call } from '#lib/utils/service.js';

	/**
	 * Imports the BAPIs of the SAP repository of an SAP JCo connector as its transactions, as the design of
	 * the SAP connector editor of the Eclipse Studio: the functions matching a pattern, a transaction each,
	 * replacing the transaction of a BAPI imported again.
	 *
	 * @type {{ id: string, onDone?: (id: string) => void | Promise<void>, onClose?: () => void }}
	 */
	let { id, onDone, onClose } = $props();

	let pattern = $state('BAPI_*');
	/** @type {{ name: string, description: string, group: string }[]} */
	let functions = $state([]);
	let searched = $state(false);
	/** @type {string[]} */
	let chosen = $state([]);
	let busy = $state(false);

	let name = $derived(id.split(/[.:]/).pop() ?? id);

	async function search() {
		busy = true;
		try {
			const result = await call('studio.dbo.SapDesign', { id, action: 'search', pattern });
			functions = Array.isArray(result?.functions) ? result.functions : [];
			chosen = [];
			// an error is told by the call, the list tells what a search finds
			searched = Array.isArray(result?.functions);
		} finally {
			busy = false;
		}
	}

	async function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		if (busy || !chosen.length) {
			return;
		}
		busy = true;
		try {
			const result = await call('studio.dbo.SapDesign', {
				id,
				action: 'import',
				functions: JSON.stringify(functions.filter((item) => chosen.includes(item.name)))
			});
			if (Array.isArray(result?.transactions)) {
				await onDone?.(String(result.transactions[0] ?? id));
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
		aria-labelledby="studio-sap-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape' && !busy) onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-sap-title">Import BAPIs in {name}</strong>
		</header>
		<form class="studio-dialog__form" onsubmit={submit}>
			<fieldset class="studio-dialog__body" disabled={busy}>
				<div class="studio-sap__search">
					<label class="studio-dialog__field">
						<span>Search BAPI pattern</span>
						<input
							class="input-common"
							bind:value={pattern}
							title="A pattern such as BAPI_USER*"
							onkeydown={(event) => {
								if (event.key === 'Enter') {
									event.preventDefault();
									void search();
								}
							}}
						/>
					</label>
					<button type="button" class="button-secondary" onclick={search}>
						<Ico icon="mdi:magnify" size={4} /> Search
					</button>
				</div>
				<div class="studio-sap__list" role="group" aria-label="BAPIs">
					{#each functions as item (item.name)}
						<label class="studio-sap__item">
							<input type="checkbox" value={item.name} bind:group={chosen} />
							<strong class="studio-ellipsis">{item.name}</strong>
							<small>{item.group}</small>
							<span class="studio-ellipsis" title={item.description}>{item.description}</span>
						</label>
					{:else}
						<p class="studio-sap__empty">
							{searched ? 'No BAPI matches the pattern.' : 'Search the SAP repository.'}
						</p>
					{/each}
				</div>
			</fieldset>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" disabled={busy} onclick={() => onClose?.()}
					>Close</button
				>
				<button type="submit" class="button-primary" disabled={busy || !chosen.length}>
					{#if busy}<Ico icon="mdi:sync" size={4} />{/if}
					Import {chosen.length || ''} as transactions
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
		width: min(44rem, 100%);
		max-height: min(44rem, 100%);
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

	.studio-dialog__footer button,
	.studio-sap__search button {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
	}

	.studio-dialog__body {
		display: grid;
		min-height: 0;
		grid-template-rows: auto auto minmax(8rem, 1fr) auto auto auto;
		gap: 0.6rem;
		overflow: hidden;
		margin: 0;
		border: 0;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-sap__search {
		display: flex;
		align-items: flex-end;
		gap: 0.5rem;
	}

	.studio-dialog__field {
		display: grid;
		flex: 1;
		gap: 0.3rem;
	}

	.studio-dialog__field > span,
	.studio-dialog__body input:not([type='checkbox']) {
		height: 2rem;
		padding-block: 0;
		padding-inline: 0.6rem;
		font-size: 0.8rem;
	}

	.studio-sap__list {
		min-height: 0;
		overflow-y: auto;
		border: 1px solid var(--studio-line);
		border-radius: 0.35rem;
		padding: 0.25rem;
	}

	.studio-sap__item {
		display: grid;
		grid-template-columns: auto minmax(6rem, 14rem) 6rem minmax(0, 1fr);
		align-items: center;
		gap: 0.5rem;
		border-radius: 0.25rem;
		padding: 0.2rem 0.4rem;
	}

	.studio-sap__item:hover {
		background: var(--studio-hover-bg);
	}

	.studio-sap__item small,
	.studio-sap__item span,
	.studio-sap__empty {
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-sap__empty {
		margin: 0.5rem;
	}
</style>
