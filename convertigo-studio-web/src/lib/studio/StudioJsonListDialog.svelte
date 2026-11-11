<script>
	import { call } from '#lib/utils/service.js';
	import { jsonListFields, jsonListValue, parseJsonList } from './nativeJson.js';
	import { portal } from './portal.js';

	/**
	 * The list editors of the native JSON steps of the Eclipse Studio (JsonColumnsEditor and
	 * JsonAggregationsEditor): a list written as a JSON array, null, or a JavaScript expression of the sequence,
	 * the column names proposed coming from the last execution in the Studio.
	 *
	 * @type {{
	 *  stepId: string,
	 *  label: string,
	 *  value: string,
	 *  kind: 'columns' | 'aggregations',
	 *  onApply?: (value: string) => void,
	 *  onClose?: () => void
	 * }}
	 */
	let { stepId, label, value, kind, onApply, onClose } = $props();

	const listId = $props.id();
	let fields = $derived(jsonListFields(kind));
	let mode = $state(/** @type {'list' | 'null' | 'expression'} */ ('list'));
	let rows = $state(/** @type {Record<string, string>[]} */ ([]));
	let selectedIndex = $state(-1);
	let expression = $state('');
	let columnNames = $state(/** @type {string[]} */ ([]));
	let started = false;

	let result = $derived(
		mode === 'expression' ? expression : mode === 'null' ? 'null' : jsonListValue(rows, kind)
	);

	$effect(() => {
		if (started) {
			return;
		}
		started = true;
		const loaded = parseJsonList(value, kind);
		mode = loaded.mode;
		rows = loaded.rows;
		expression = String(value ?? '');
	});

	$effect(() => {
		const id = stepId;
		void call('studio.debug.NativeJson', { action: 'columns', id }).then((response) => {
			if (id === stepId && Array.isArray(response?.columns)) {
				columnNames = response.columns.map(String);
			}
		});
	});

	/**
	 * @param {'list' | 'null' | 'expression'} next
	 */
	function changeMode(next) {
		if (next === 'expression' && mode !== 'expression') {
			// the expression starts from the list or null shown
			expression = result;
		}
		mode = next;
	}

	function addRow() {
		rows = [...rows, kind === 'aggregations' ? { type: 'count' } : {}];
		selectedIndex = rows.length - 1;
	}

	function removeRow() {
		if (selectedIndex < 0) {
			return;
		}
		rows = rows.filter((_, index) => index !== selectedIndex);
		selectedIndex = Math.min(selectedIndex, rows.length - 1);
	}

	/**
	 * @param {number} delta
	 */
	function moveRow(delta) {
		const target = selectedIndex + delta;
		if (selectedIndex < 0 || target < 0 || target >= rows.length) {
			return;
		}
		const next = [...rows];
		[next[selectedIndex], next[target]] = [next[target], next[selectedIndex]];
		rows = next;
		selectedIndex = target;
	}

	function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		onApply?.(result);
	}
</script>

<div class="studio-json-list" role="presentation" use:portal onclick={() => onClose?.()}>
	<div
		class="studio-json-list__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="{listId}-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape') onClose?.();
		}}
	>
		<header class="studio-json-list__header">
			<strong id="{listId}-title">{label}</strong>
			<p>
				{label}: a list written as a JSON array, null, or a JavaScript expression of the sequence.{columnNames.length
					? ' The column names proposed come from the last execution in the Studio.'
					: ''}
			</p>
		</header>
		<form class="studio-json-list__form" onsubmit={submit}>
			<div class="studio-json-list__modes" role="radiogroup" aria-label="Kind of value">
				{#each [['list', 'List'], ['null', 'null'], ['expression', 'JavaScript expression']] as [key, text] (key)}
					<label>
						<input
							type="radio"
							name="{listId}-mode"
							checked={mode === key}
							onchange={() => changeMode(/** @type {'list' | 'null' | 'expression'} */ (key))}
						/>
						{text}
					</label>
				{/each}
			</div>
			<div
				class="studio-json-list__table"
				class:studio-json-list__table--disabled={mode !== 'list'}
			>
				<table>
					<thead>
						<tr>
							{#each fields as field (field.key)}
								<th>{field.label}</th>
							{/each}
						</tr>
					</thead>
					<tbody>
						{#each rows as row, index (index)}
							<tr
								class:studio-json-list__row--selected={index === selectedIndex}
								onfocusin={() => (selectedIndex = index)}
								onclick={() => (selectedIndex = index)}
							>
								{#each fields as field (field.key)}
									<td>
										{#if field.choices}
											<select
												class="input-common"
												aria-label={field.label}
												disabled={mode !== 'list'}
												bind:value={row[field.key]}
											>
												{#each field.choices as choice (choice)}
													<option value={choice}>{choice}</option>
												{/each}
											</select>
										{:else}
											<input
												class="input-common"
												aria-label={field.label}
												disabled={mode !== 'list'}
												list={field.key === 'column' ? `${listId}-columns` : undefined}
												bind:value={row[field.key]}
											/>
										{/if}
									</td>
								{/each}
							</tr>
						{/each}
					</tbody>
				</table>
				<datalist id="{listId}-columns">
					{#each columnNames as name (name)}
						<option value={name}></option>
					{/each}
				</datalist>
			</div>
			<div class="studio-json-list__actions">
				<button type="button" class="button-secondary" disabled={mode !== 'list'} onclick={addRow}
					>Add</button
				>
				<button
					type="button"
					class="button-secondary"
					disabled={mode !== 'list' || selectedIndex < 0}
					onclick={removeRow}>Remove</button
				>
				<button
					type="button"
					class="button-secondary"
					disabled={mode !== 'list' || selectedIndex <= 0}
					onclick={() => moveRow(-1)}>Up</button
				>
				<button
					type="button"
					class="button-secondary"
					disabled={mode !== 'list' || selectedIndex < 0 || selectedIndex >= rows.length - 1}
					onclick={() => moveRow(1)}>Down</button
				>
			</div>
			<label class="studio-json-list__expression">
				<span>Expression</span>
				<textarea
					class="input-common"
					rows="2"
					spellcheck="false"
					readonly={mode !== 'expression'}
					value={mode === 'expression' ? expression : result}
					oninput={(event) => (expression = event.currentTarget.value)}></textarea>
			</label>
			<p class="studio-json-list__preview">Value: {result}</p>
			<footer class="studio-json-list__footer">
				<button type="button" class="button-secondary" onclick={() => onClose?.()}>Cancel</button>
				<button type="submit" class="button-primary">Apply</button>
			</footer>
		</form>
	</div>
</div>

<style>
	.studio-json-list {
		position: fixed;
		inset: 0;
		z-index: 90;
		display: grid;
		place-items: center;
		background: color-mix(in oklab, black 45%, transparent);
		padding: 1rem;
	}

	.studio-json-list__box {
		display: grid;
		width: min(44rem, 100%);
		max-height: min(38rem, 100%);
		grid-template-rows: auto minmax(0, 1fr);
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
		font-size: 0.8rem;
	}

	.studio-json-list__header {
		display: grid;
		gap: 0.25rem;
		border-bottom: 1px solid var(--studio-line);
		padding: 0.75rem 1rem;
	}

	.studio-json-list__header strong {
		color: var(--studio-text-strong);
		font-size: 0.9rem;
		font-weight: 600;
	}

	.studio-json-list__header p,
	.studio-json-list__preview {
		margin: 0;
		color: var(--studio-text-idle);
		font-size: 0.74rem;
		overflow-wrap: anywhere;
	}

	.studio-json-list__form {
		display: grid;
		min-height: 0;
		grid-template-rows: auto minmax(6rem, 1fr) auto auto auto auto;
		gap: 0.55rem;
		overflow: auto;
		padding: 0.75rem 1rem;
	}

	.studio-json-list__modes {
		display: flex;
		flex-wrap: wrap;
		gap: 1rem;
	}

	.studio-json-list__modes label {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
	}

	.studio-json-list__table {
		min-height: 0;
		overflow: auto;
		border: 1px solid var(--studio-line);
		border-radius: 0.4rem;
	}

	.studio-json-list__table--disabled {
		opacity: 0.55;
	}

	.studio-json-list__table table {
		width: 100%;
		border-collapse: collapse;
	}

	.studio-json-list__table th,
	.studio-json-list__table td {
		padding: 0.2rem 0.35rem;
		text-align: start;
	}

	.studio-json-list__table th {
		position: sticky;
		top: 0;
		background: var(--studio-chrome-bg);
		color: var(--studio-text-idle);
		font-weight: 600;
	}

	.studio-json-list__table select {
		min-width: 5.5rem;
	}

	.studio-json-list__table input,
	.studio-json-list__table select {
		width: 100%;
		height: 1.8rem;
		padding-inline: 0.45rem;
		font-size: 0.78rem;
	}

	.studio-json-list__row--selected {
		background: light-dark(
			color-mix(in oklab, var(--color-primary-500) 12%, white),
			color-mix(in oklab, var(--color-primary-500) 18%, #1c1c1d)
		);
	}

	.studio-json-list__actions,
	.studio-json-list__footer {
		display: flex;
		gap: 0.5rem;
	}

	.studio-json-list__footer {
		justify-content: flex-end;
	}

	.studio-json-list__expression {
		display: grid;
		gap: 0.3rem;
	}

	.studio-json-list__expression span {
		font-weight: 600;
	}

	.studio-json-list__expression textarea {
		padding: 0.4rem 0.6rem;
		font-family: var(--font-mono, ui-monospace, monospace);
		font-size: 0.78rem;
	}
</style>
