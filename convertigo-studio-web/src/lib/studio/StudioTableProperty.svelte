<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { propertyTableEditor } from './propertyTables';

	/**
	 * Edits a table property as the table editors of the Eclipse Studio: its rows of cells, each column a
	 * text or a choice, with rows to add, move and remove.
	 *
	 * @type {{
	 *  value: any[][],
	 *  editorClass?: string,
	 *  name?: string,
	 *  onChange?: (rows: string[][]) => void
	 * }}
	 */
	let { value = [], editorClass = '', name = '', onChange } = $props();

	let rows = $derived(
		(Array.isArray(value) ? value : []).map((row) =>
			(Array.isArray(row) ? row : [row]).map((cell) => String(cell ?? ''))
		)
	);
	let editor = $derived(propertyTableEditor(editorClass, rows, name));

	/**
	 * @param {string[][]} next
	 */
	function change(next) {
		onChange?.(next);
	}

	/**
	 * @param {number} rowIndex
	 * @param {number} columnIndex
	 * @param {string} cell
	 */
	function setCell(rowIndex, columnIndex, cell) {
		change(
			rows.map((row, index) =>
				index === rowIndex
					? row.map((current, column) => (column === columnIndex ? cell : current))
					: row
			)
		);
	}

	/**
	 * @param {number} rowIndex
	 * @param {number} offset
	 */
	function moveRow(rowIndex, offset) {
		const target = rowIndex + offset;
		if (target < 0 || target >= rows.length) {
			return;
		}
		const next = [...rows];
		[next[rowIndex], next[target]] = [next[target], next[rowIndex]];
		change(next);
	}
</script>

<div class="studio-table-property">
	<table>
		<thead>
			<tr>
				{#each editor.columns as column (column)}
					<th>{column}</th>
				{/each}
				<th class="studio-table-property__actions" aria-label="Actions"></th>
			</tr>
		</thead>
		<tbody>
			{#each rows as row, rowIndex (rowIndex)}
				<tr>
					{#each editor.columns as column, columnIndex (column)}
						<td>
							{#if editor.choices?.[columnIndex]}
								<select
									class="select-common"
									aria-label={column}
									value={row[columnIndex] ?? ''}
									onchange={(event) => setCell(rowIndex, columnIndex, event.currentTarget.value)}
								>
									{#each editor.choices[columnIndex] as choice (choice)}
										<option value={choice}>{choice}</option>
									{/each}
								</select>
							{:else}
								<input
									class="input-common"
									aria-label={column}
									value={row[columnIndex] ?? ''}
									onchange={(event) => setCell(rowIndex, columnIndex, event.currentTarget.value)}
								/>
							{/if}
						</td>
					{/each}
					<td class="studio-table-property__actions">
						<button
							type="button"
							title="Move up"
							aria-label="Move up"
							disabled={rowIndex === 0}
							onclick={() => moveRow(rowIndex, -1)}><Ico icon="mdi:arrow-up" size={3} /></button
						>
						<button
							type="button"
							title="Move down"
							aria-label="Move down"
							disabled={rowIndex === rows.length - 1}
							onclick={() => moveRow(rowIndex, 1)}><Ico icon="mdi:arrow-down" size={3} /></button
						>
						<button
							type="button"
							title="Remove the row"
							aria-label="Remove the row"
							onclick={() => change(rows.filter((_, index) => index !== rowIndex))}
							><Ico icon="mdi:close" size={3} /></button
						>
					</td>
				</tr>
			{:else}
				<tr>
					<td class="studio-table-property__empty" colspan={editor.columns.length + 1}>No row</td>
				</tr>
			{/each}
		</tbody>
	</table>
	<button
		type="button"
		class="studio-table-property__add"
		onclick={() => change([...rows, [...editor.template]])}
	>
		<Ico icon="mdi:plus" size={4} /> Add a row
	</button>
</div>

<style>
	.studio-table-property {
		display: grid;
		gap: 0.3rem;
		min-width: 0;
		overflow-x: auto;
	}

	table {
		width: 100%;
		border-collapse: collapse;
		font-size: 0.75rem;
	}

	th {
		color: var(--studio-text-idle);
		padding: 0.15rem 0.25rem;
		font-weight: 600;
		text-align: left;
		white-space: nowrap;
	}

	td {
		padding: 0.1rem 0.15rem;
	}

	td :is(input, select) {
		width: 100%;
		min-width: 5rem;
		height: 1.6rem;
		padding: 0 0.35rem;
		font-size: 0.75rem;
	}

	.studio-table-property__actions {
		width: 1%;
		white-space: nowrap;
	}

	.studio-table-property__actions button {
		display: inline-grid;
		width: 1.35rem;
		height: 1.35rem;
		place-items: center;
		border: 0;
		border-radius: 0.25rem;
		background: transparent;
		color: var(--studio-text-idle);
		padding: 0;
	}

	.studio-table-property__actions button:hover:not(:disabled) {
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

	.studio-table-property__actions button:disabled {
		opacity: 0.35;
	}

	.studio-table-property__empty {
		color: var(--studio-text-idle);
		padding: 0.3rem 0.25rem;
	}

	.studio-table-property__add {
		display: inline-flex;
		width: fit-content;
		align-items: center;
		gap: 0.25rem;
		border: 0;
		border-radius: 0.25rem;
		background: transparent;
		color: var(--color-primary-500);
		padding: 0.15rem 0.35rem;
		font-size: 0.75rem;
	}

	.studio-table-property__add:hover {
		background: var(--studio-hover-bg);
	}
</style>
