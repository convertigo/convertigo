<script>
	import { Switch } from '@skeletonlabs/skeleton-svelte';
	import Ico from '$lib/utils/Ico.svelte';

	let {
		rows = [],
		layoutMode = 'table',
		tableClass = '',
		containerClass = '',
		loading = false,
		loadingTitle = 'Loading...',
		emptyTitle = 'This table is empty',
		showEmptyIcon = false,
		selectedIds = {},
		selectionPrefix = 'select-row',
		rowKeyPrefix = 'rows',
		columnSelectionPrefix = 'columns',
		hideSelectWithoutRev = false,
		disableSelectWithoutRev = false,
		idColumnMode = 'id-or-_id',
		visibleColumns = [],
		displayableColumns = [],
		tableColspanValue = 3,
		working = false,
		onSelectionChange = () => {},
		onColumnSelect = () => {},
		onOpenDocument = () => {},
		onCopyRow = () => {},
		onOpenRow = (row) => onOpenDocument(String(row?.id ?? '')),
		formatCellValue = (value) => String(value ?? ''),
		getCellValue = (row, column) => row?.raw?.[column],
		columnLabel = (column) => String(column ?? ''),
		showJsonOpenButton = true,
		jsonHeaderText = (row) => `id "${String(row?.id ?? '')}"`,
		jsonHeaderTitle = (row) => String(row?.id ?? ''),
		getSelectionKey = (row) => String(row?.id ?? ''),
		showAttachmentCountColumn = false,
		getAttachmentCount = () => 0,
		attachmentCountTitle = 'Attachments',
		getConflictCount = () => 0,
		conflictCountTitle = 'Conflicts',
		isIdColumn = (column) =>
			idColumnMode == 'id-or-_id' ? column == '_id' || column == 'id' : column == '_id',
		isRowSelectable = (row, rowId, rowRev) =>
			Boolean(rowId) && (!hideSelectWithoutRev || Boolean(rowRev))
	} = $props();
</script>

{#snippet rowSelectionSwitch(name, label, checked, disabled, onToggle)}
	<Switch
		{name}
		aria-label={label}
		{checked}
		{disabled}
		onCheckedChange={(event) => onToggle(event.checked)}
	>
		<Switch.Control class="c8o-switch h-5 w-9 transition-surface">
			<Switch.Thumb />
		</Switch.Control>
		<Switch.HiddenInput />
	</Switch>
{/snippet}

{#snippet rowCopyButton(row)}
	<button
		type="button"
		class="button-ico-primary h-7 w-7 justify-center p-0!"
		title="Copy row"
		aria-label="Copy row"
		onclick={() => onCopyRow(row)}
	>
		<Ico icon="mdi:content-copy" size={4.5} />
	</button>
{/snippet}

{#if layoutMode == 'json'}
	<div class={containerClass || (showEmptyIcon ? '' : 'mango-result-body')}>
		{#if loading && rows.length == 0}
			<div class="mango-empty">
				<div class="mango-empty-title">{loadingTitle}</div>
			</div>
		{:else if rows.length == 0}
			<div class="mango-empty">
				{#if showEmptyIcon}
					<Ico icon="mdi:magnify" size={18} class="mango-empty-icon" />
				{/if}
				<div class="mango-empty-title">{emptyTitle}</div>
			</div>
		{:else}
			<div class="mango-json-list">
				{#each rows as row, rowIndex (`${rowKeyPrefix}-${row?.id || row?.key || 'json'}-${rowIndex}`)}
					{@const rowId = row?.id ?? ''}
					{@const rowRev = row?.rev ?? ''}
					{@const rowSelectionKey = getSelectionKey(row)}
					<article class="mango-json-row">
						<div class="mango-json-select">
							{#if isRowSelectable(row, rowId, rowRev)}
								{@render rowSelectionSwitch(
									`${selectionPrefix}-${rowSelectionKey}`,
									`Select ${rowSelectionKey}`,
									Boolean(selectedIds[rowSelectionKey]),
									working || (disableSelectWithoutRev && !rowRev),
									(checked) => onSelectionChange(rowSelectionKey, checked)
								)}
							{/if}
						</div>
						<div class="mango-json-card">
							<header class="mango-json-card-header">
								<span class="mango-json-id" title={jsonHeaderTitle(row)}>{jsonHeaderText(row)}</span
								>
								{#if showJsonOpenButton}
									<button
										type="button"
										class="button-ico-primary h-7 w-7 justify-center p-0!"
										title="Open document"
										aria-label="Open document"
										onclick={() => onOpenRow(row)}
									>
										<Ico icon="mdi:pencil-outline" size={4.5} />
									</button>
								{/if}
							</header>
							<pre class="mango-json-content">{row.jsonText}</pre>
						</div>
					</article>
				{/each}
			</div>
		{/if}
	</div>
{:else}
	<div class="docs-table-wrap">
		<table
			class={`docs-table ${layoutMode == 'metadata' ? 'docs-table-metadata' : ''} ${tableClass}`}
		>
			<thead>
				{#if layoutMode == 'metadata'}
					<tr>
						<th class="col-select"></th>
						<th class="col-copy"></th>
						<th class="metadata-col-id">id</th>
						<th class="metadata-col-key">key</th>
						<th class="metadata-col-value">value</th>
					</tr>
				{:else}
					<tr>
						<th class="col-select"></th>
						<th class="col-copy"></th>
						{#each visibleColumns as column, columnIndex (`${columnSelectionPrefix}-${column}-${columnIndex}`)}
							<th class="mango-column-th">
								<select
									class="mango-column-select select h-9 input-common w-full min-w-0 px-2 text-sm"
									value={column}
									onchange={(event) => onColumnSelect(columnIndex, event.currentTarget.value)}
								>
									{#each displayableColumns as candidate (`${columnSelectionPrefix}-candidate-${candidate}`)}
										<option value={candidate}>{columnLabel(candidate)}</option>
									{/each}
								</select>
							</th>
						{/each}
						{#if showAttachmentCountColumn}
							<th class="attachments-col" title={`${conflictCountTitle} / ${attachmentCountTitle}`}
							></th>
						{/if}
					</tr>
				{/if}
			</thead>
			<tbody>
				{#if loading && rows.length == 0}
					<tr>
						<td colspan={tableColspanValue} class="empty-row">{loadingTitle}</td>
					</tr>
				{:else if rows.length == 0}
					<tr>
						<td colspan={tableColspanValue} class="empty-row">{emptyTitle}</td>
					</tr>
				{:else}
					{#each rows as row, rowIndex (`${rowKeyPrefix}-${row?.id || row?.key || 'row'}-${rowIndex}`)}
						{@const rowId = row?.id ?? ''}
						{@const rowRev = row?.rev ?? ''}
						<tr>
							<td class="col-select">
								{#if rowId && (!hideSelectWithoutRev || rowRev)}
									{@render rowSelectionSwitch(
										`${columnSelectionPrefix}-select-${rowId}`,
										`Select ${rowId}`,
										Boolean(selectedIds[rowId]),
										working || (disableSelectWithoutRev && !rowRev),
										(checked) => onSelectionChange(rowId, checked)
									)}
								{/if}
							</td>
							<td class="col-copy">
								{@render rowCopyButton(row)}
							</td>
							{#if layoutMode == 'metadata'}
								<td class="metadata-col-id">
									{#if rowId}
										<button
											type="button"
											class="metadata-link text-left text-primary-500 transition-surface hover:underline"
											title={rowId}
											onclick={() => onOpenDocument(rowId)}
										>
											<span class="metadata-ellipsis">{rowId}</span>
										</button>
									{:else}
										<span class="metadata-ellipsis"></span>
									{/if}
								</td>
								<td class="metadata-col-key">
									<span class="metadata-ellipsis" title={row.key}>{row.key}</span>
								</td>
								<td class="metadata-col-value">
									<span class="metadata-ellipsis" title={row.valuePreview}>{row.valuePreview}</span>
								</td>
							{:else}
								{#each visibleColumns as column (`${columnSelectionPrefix}-row-${column}-${rowIndex}`)}
									{@const cellValue = formatCellValue(getCellValue(row, column))}
									{@const isIdCell = isIdColumn(column)}
									<td>
										{#if isIdCell && rowId}
											<button
												type="button"
												class="metadata-link text-left text-primary-500 transition-surface hover:underline"
												title={cellValue || rowId}
												onclick={() => onOpenDocument(rowId)}
											>
												<span class="metadata-ellipsis">{cellValue || rowId}</span>
											</button>
										{:else}
											<span class="metadata-ellipsis" title={cellValue}>{cellValue}</span>
										{/if}
									</td>
								{/each}
								{#if showAttachmentCountColumn}
									{@const conflictCount = Number(getConflictCount(row)) || 0}
									{@const attachmentCount = Number(getAttachmentCount(row)) || 0}
									<td class="attachments-col-value">
										{#if conflictCount > 0}
											<span class="conflict-count">
												<Ico icon="mdi:source-branch" size={4} />
												<span>{conflictCount}</span>
											</span>
										{/if}
										{#if attachmentCount > 0}
											<span class="attachment-count">
												<Ico icon="mdi:attachment" size={4} />
												<span>{attachmentCount}</span>
											</span>
										{/if}
									</td>
								{/if}
							{/if}
						</tr>
					{/each}
				{/if}
			</tbody>
		</table>
	</div>
{/if}

<style>
	.docs-table-wrap {
		overflow-x: auto;
	}

	.docs-table {
		width: 100%;
		border-collapse: collapse;
	}

	.docs-table th,
	.docs-table td {
		padding: calc(var(--spacing) * 1.25) calc(var(--spacing) * 1.25);
		vertical-align: top;
	}

	.docs-table thead th {
		border-bottom: 1px solid light-dark(var(--color-surface-500), var(--color-surface-500));
		font-size: 0.98rem;
		font-weight: 600;
		color: var(--convertigo-text-strong);
		text-align: left;
	}

	.docs-table tbody tr {
		border-bottom: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
	}

	.docs-table tbody tr:last-child {
		border-bottom: 0;
	}

	.docs-table-metadata {
		table-layout: fixed;
	}

	.docs-table-metadata .metadata-col-id {
		width: 30%;
	}

	.docs-table-metadata .metadata-col-key {
		width: 30%;
	}

	.docs-table-metadata .metadata-col-value {
		width: 40%;
	}

	.docs-table-metadata td {
		overflow: hidden;
	}

	.metadata-link {
		display: block;
		width: 100%;
		min-width: 0;
	}

	.metadata-ellipsis {
		display: block;
		max-width: 100%;
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.col-copy {
		width: 2.4rem;
	}

	.col-select {
		width: 3rem;
		text-align: center;
	}

	.col-select :global([data-part='root']) {
		display: inline-flex;
		align-items: center;
		justify-content: center;
	}

	.empty-row {
		padding: calc(var(--spacing) * 3) calc(var(--spacing) * 1.5);
		color: var(--convertigo-text-muted);
	}

	.mango-table {
		table-layout: fixed;
	}

	.mango-column-th {
		min-width: 9rem;
	}

	.mango-column-select {
		display: block;
		min-height: 2.25rem;
		line-height: 1.25;
		padding-top: 0.35rem;
		padding-bottom: 0.35rem;
		overflow: visible;
	}

	.attachments-col {
		width: 3.75rem;
		text-align: right;
	}

	.attachments-col-value {
		text-align: right;
	}

	.conflict-count,
	.attachment-count {
		display: inline-flex;
		align-items: center;
		justify-content: flex-end;
		gap: calc(var(--spacing) * 0.3);
		color: var(--convertigo-text-strong);
	}

	.attachments-col-value > span + span {
		margin-left: calc(var(--spacing) * 0.6);
	}

	.mango-result-body {
		min-height: 16rem;
		border: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
		border-radius: var(--radius-base);
		overflow: auto;
		padding: calc(var(--spacing) * 1);
	}

	.mango-json-list {
		display: grid;
		gap: calc(var(--spacing) * 0.75);
		overflow: auto;
	}

	.mango-json-row {
		display: grid;
		grid-template-columns: 2.4rem minmax(0, 1fr);
		gap: calc(var(--spacing) * 0.5);
		align-items: start;
	}

	.mango-json-select {
		display: inline-flex;
		align-items: flex-start;
		justify-content: center;
		padding-top: calc(var(--spacing) * 1);
	}

	.mango-json-card {
		border: 1px solid light-dark(var(--color-surface-400), var(--color-surface-700));
		border-radius: var(--radius-base);
		background: light-dark(var(--color-surface-50), var(--color-surface-800));
		overflow: hidden;
	}

	.mango-json-card-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: calc(var(--spacing) * 0.75);
		padding: calc(var(--spacing) * 0.6) calc(var(--spacing) * 0.9);
		border-bottom: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
		background: light-dark(var(--color-surface-200), var(--color-surface-900));
	}

	.mango-json-id {
		font-family:
			ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New',
			monospace;
		font-size: 0.95rem;
		color: var(--color-primary-500);
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.mango-json-content {
		margin: 0;
		padding: calc(var(--spacing) * 0.9);
		font-size: 0.8rem;
		line-height: 1.45;
		font-family:
			ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New',
			monospace;
		overflow: auto;
		max-height: 14rem;
	}

	.mango-empty {
		display: grid;
		place-items: center;
		gap: calc(var(--spacing) * 0.5);
		min-height: 22rem;
		color: var(--convertigo-text-muted);
	}

	.mango-empty-icon {
		opacity: 0.45;
	}

	.mango-empty-title {
		font-size: 1.6rem;
		font-weight: 500;
	}
</style>
