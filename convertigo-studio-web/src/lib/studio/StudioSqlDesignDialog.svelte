<script>
	import Ico from '#lib/utils/Ico.svelte';
	import { call } from '#lib/utils/service.js';

	/**
	 * Imports the tables, procedures and functions of the database of an SQL connector as transactions,
	 * as the Design tab of the SQL connector editor of the Eclipse Studio: a table gives its list, insert,
	 * select, update and delete transactions, each one possibly wrapped in a sequence.
	 *
	 * @type {{ id: string, onDone?: (id: string) => void | Promise<void>, onClose?: () => void }}
	 */
	let { id, onDone, onClose } = $props();

	const TYPES = ['TABLE', 'PROCEDURE', 'FUNCTION'];
	const CRUDS = ['LIST', 'INSERT', 'SELECT', 'UPDATE', 'DELETE'];

	let pattern = $state('%');
	let types = $state([...TYPES]);
	/** @type {{ name: string, specificName: string, type: string, remarks: string }[]} */
	let items = $state([]);
	let searched = $state(false);
	/** @type {string[]} */
	let chosen = $state([]);
	let cruds = $state([...CRUDS]);
	let override = $state(false);
	let wrap = $state(false);
	let accessibility = $state('Hidden');
	let authenticated = $state(true);
	let busy = $state(false);

	let name = $derived(id.split(/[.:]/).pop() ?? id);

	/**
	 * @param {{ name: string, type: string }} item
	 */
	function key(item) {
		return `${item.type}\u0000${item.name}`;
	}

	async function search() {
		busy = true;
		try {
			const result = await call('studio.dbo.SqlDesign', {
				id,
				action: 'search',
				pattern,
				types: types.join(',')
			});
			items = Array.isArray(result?.items) ? result.items : [];
			chosen = [];
			searched = true;
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
			const result = await call('studio.dbo.SqlDesign', {
				id,
				action: 'import',
				items: JSON.stringify(items.filter((item) => chosen.includes(key(item)))),
				cruds: cruds.join(','),
				override: String(override),
				accessibility: wrap ? accessibility : '',
				authenticated: String(authenticated)
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
		aria-labelledby="studio-sql-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape' && !busy) onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-sql-title">Import from the database of {name}</strong>
		</header>
		<form class="studio-dialog__form" onsubmit={submit}>
			<fieldset class="studio-dialog__body" disabled={busy}>
				<div class="studio-sql__search">
					<label class="studio-dialog__field">
						<span>Search pattern</span>
						<input
							class="input-common"
							bind:value={pattern}
							title="A pattern such as SUB%"
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
				<div class="studio-sql__options" role="group" aria-label="Search in">
					<span>Search in</span>
					{#each TYPES as type (type)}
						<label><input type="checkbox" value={type} bind:group={types} /> {type}</label>
					{/each}
				</div>
				<div class="studio-sql__list" role="group" aria-label="Database objects">
					{#each items as item (key(item))}
						<label class="studio-sql__item">
							<input type="checkbox" value={key(item)} bind:group={chosen} />
							<strong class="studio-ellipsis">{item.name}</strong>
							<small>{item.type}</small>
							<span class="studio-ellipsis" title={item.remarks}>{item.remarks}</span>
						</label>
					{:else}
						<p class="studio-sql__empty">
							{searched ? 'Nothing matches the pattern.' : 'Search the database.'}
						</p>
					{/each}
				</div>
				<div class="studio-sql__options" role="group" aria-label="For a table">
					<span>For a table</span>
					{#each CRUDS as crud (crud)}
						<label><input type="checkbox" value={crud} bind:group={cruds} /> {crud}</label>
					{/each}
				</div>
				<div class="studio-sql__options">
					<label
						><input type="checkbox" bind:checked={override} /> Replace the existing transactions</label
					>
				</div>
				<div class="studio-sql__options">
					<label><input type="checkbox" bind:checked={wrap} /> Generate sequences</label>
					{#if wrap}
						<select class="input-common" aria-label="Accessibility" bind:value={accessibility}>
							<option value="Public">Public</option>
							<option value="Hidden">Hidden</option>
							<option value="Private">Private</option>
						</select>
						<label
							><input type="checkbox" bind:checked={authenticated} /> Authenticated session mandatory</label
						>
					{/if}
				</div>
			</fieldset>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" disabled={busy} onclick={() => onClose?.()}
					>Close</button
				>
				<button type="submit" class="button-primary" disabled={busy || !chosen.length}>
					{#if busy}<Ico icon="mdi:sync" size={4} />{/if}
					Import {chosen.length || ''} in the project
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
	.studio-sql__search button {
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

	.studio-sql__search {
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
	.studio-sql__options > span {
		font-weight: 600;
	}

	.studio-dialog__body input:not([type='checkbox']),
	.studio-dialog__body select {
		height: 2rem;
		padding-block: 0;
		padding-inline: 0.6rem;
		font-size: 0.8rem;
	}

	.studio-dialog__body select {
		width: auto;
	}

	.studio-sql__options {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 0.3rem 0.9rem;
	}

	.studio-sql__options label {
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
	}

	.studio-sql__list {
		min-height: 0;
		overflow-y: auto;
		border: 1px solid var(--studio-line);
		border-radius: 0.35rem;
		padding: 0.25rem;
	}

	.studio-sql__item {
		display: grid;
		grid-template-columns: auto minmax(6rem, 14rem) 6rem minmax(0, 1fr);
		align-items: center;
		gap: 0.5rem;
		border-radius: 0.25rem;
		padding: 0.2rem 0.4rem;
	}

	.studio-sql__item:hover {
		background: var(--studio-hover-bg);
	}

	.studio-sql__item small,
	.studio-sql__item span,
	.studio-sql__empty {
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-sql__empty {
		margin: 0.5rem;
	}
</style>
