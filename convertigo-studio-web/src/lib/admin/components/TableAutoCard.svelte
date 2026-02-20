<script>
	import Icon from '@iconify/svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import { fromAction } from 'svelte/attachments';

	/** @type {{definition: any, data: any, showHeaders?: boolean, showNothing?: boolean, title?: string, comment?: string, class?: string, thClass?: string, trClass?: string, fnRowId?: function, animationProps?: any, children?: import('svelte').Snippet<[any]>, rowChildren?: import('svelte').Snippet<[any]>, thead?: import('svelte').Snippet<[any]>, cardBreakpoint?: number}} */
	let {
		definition,
		data,
		showHeaders = true,
		showNothing = true,
		title = '',
		comment = '',
		class: cls = '',
		thClass = 'text-left text-strong text-[14px] font-semibold',
		trClass = 'row-hover',
		fnRowId = (row, i) => row.name ?? i,
		cardBreakpoint = 0,
		children,
		rowChildren,
		thead
	} = $props();

	const overflowThreshold = 8;
	let isCardView = $state(false);
	let tableMinWidth = $state(0);
	let sortKey = $state('');
	/** @type {'asc' | 'desc' | ''} */
	let sortDirection = $state(/** @type {'asc' | 'desc' | ''} */ (''));
	const attachContainer = $derived(fromAction(observeContainer));
	const sortableDefinition = $derived(definition.filter((def) => isDefSortable(def)));
	const activeSortKey = $derived(
		sortableDefinition.some((def) => def.key === sortKey) ? sortKey : ''
	);
	const sortedData = $derived.by(() => {
		if (!Array.isArray(data) || data.length === 0 || sortDirection === '' || activeSortKey === '') {
			return data;
		}
		const rows = [...data];
		rows.sort((a, b) => {
			const comparison = compareValues(a?.[activeSortKey], b?.[activeSortKey]);
			return sortDirection === 'asc' ? comparison : -comparison;
		});
		return rows;
	});

	/** @param {HTMLDivElement} node */
	function observeContainer(node) {
		let rafId = 0;
		const update = () => {
			if (!node.isConnected) return;
			const table = node.querySelector('table');
			if (!table) return;
			const containerWidth = Math.floor(node.clientWidth);
			if (containerWidth === 0) return;
			if (!isCardView) {
				const measured = Math.ceil(table.scrollWidth);
				if (measured > 0) {
					tableMinWidth = measured;
				}
			} else if (tableMinWidth === 0) {
				tableMinWidth = Math.ceil(table.scrollWidth);
			}
			if (cardBreakpoint > 0 && containerWidth >= cardBreakpoint) {
				isCardView = false;
			} else if (!isCardView && containerWidth + overflowThreshold < tableMinWidth) {
				isCardView = true;
			} else if (isCardView && containerWidth >= tableMinWidth + overflowThreshold) {
				isCardView = false;
			}
		};
		const schedule = () => {
			if (rafId) return;
			rafId = requestAnimationFrame(() => {
				rafId = 0;
				update();
			});
		};

		update();

		const resizeObserver = new ResizeObserver(schedule);
		resizeObserver.observe(node);
		const intersectionObserver = new IntersectionObserver((entries) => {
			for (const entry of entries) {
				if (entry.isIntersecting) {
					schedule();
				}
			}
		});
		intersectionObserver.observe(node);

		const mutationObserver = new MutationObserver(schedule);
		mutationObserver.observe(node, { childList: true, subtree: true, characterData: true });

		return {
			destroy() {
				if (rafId) {
					cancelAnimationFrame(rafId);
				}
				resizeObserver.disconnect();
				intersectionObserver.disconnect();
				mutationObserver.disconnect();
			}
		};
	}

	const normalizeValue = (value) => {
		if (value == null || value === '') {
			return { kind: 'empty', value: '' };
		}
		if (typeof value === 'number') {
			return { kind: 'number', value };
		}
		if (typeof value === 'boolean') {
			return { kind: 'number', value: value ? 1 : 0 };
		}
		if (value instanceof Date) {
			return { kind: 'number', value: value.getTime() };
		}
		if (typeof value === 'string') {
			const trimmed = value.trim();
			const num = Number(trimmed);
			if (trimmed !== '' && Number.isFinite(num)) {
				return { kind: 'number', value: num };
			}
			return { kind: 'string', value: trimmed.toLocaleLowerCase() };
		}
		return { kind: 'string', value: String(value).toLocaleLowerCase() };
	};

	const compareValues = (left, right) => {
		const a = normalizeValue(left);
		const b = normalizeValue(right);
		if (a.kind === 'empty' && b.kind === 'empty') return 0;
		if (a.kind === 'empty') return 1;
		if (b.kind === 'empty') return -1;
		if (a.kind === 'number' && b.kind === 'number') {
			return Number(a.value) - Number(b.value);
		}
		return String(a.value).localeCompare(String(b.value), undefined, {
			numeric: true,
			sensitivity: 'base'
		});
	};

	const toggleSort = (def) => {
		if (!isDefSortable(def)) return;
		if (sortKey !== def.key) {
			sortKey = def.key;
			sortDirection = 'asc';
			return;
		}
		if (sortDirection === 'asc') {
			sortDirection = 'desc';
			return;
		}
		if (sortDirection === 'desc') {
			sortKey = '';
			sortDirection = '';
			return;
		}
		sortDirection = 'asc';
	};

	const getSortIndicator = (def) => {
		if (activeSortKey !== def?.key || sortDirection === '') {
			return '';
		}
		return sortDirection === 'asc' ? '↓' : '↑';
	};

	function isDefSortable(def) {
		if (def?.sortable === true) return true;
		if (def?.sortable === false) return false;
		if (!def?.key || typeof def.key !== 'string' || def.key.length === 0) return false;
		if (def?.custom) return false;
		const label = String(def?.name ?? def.key)
			.trim()
			.toLowerCase();
		if (label === 'action' || label === 'actions') return false;
		return true;
	}

	/** @param {string} key @param {'asc' | 'desc' | ''} [direction='asc'] */
	const setSort = (key, direction = 'asc') => {
		if (!key) {
			sortKey = '';
			sortDirection = '';
			return;
		}
		sortKey = key;
		sortDirection = direction;
	};

	const cycleSortDirection = () => {
		if (!activeSortKey) {
			const first = sortableDefinition[0];
			if (first?.key) {
				setSort(first.key, 'asc');
			}
			return;
		}
		sortDirection = sortDirection === 'asc' ? 'desc' : 'asc';
	};

	const onCardSortKeyChange = (event) => {
		const key = event?.currentTarget?.value ?? '';
		if (!key) {
			setSort('', '');
			return;
		}
		setSort(key, 'asc');
	};

	const cardSortDirectionLabel = $derived(
		getSortIndicator({ key: activeSortKey }) === '↑' ? '↑' : '↓'
	);
</script>

<div class="table-container {cls}" class:autocard={isCardView} {@attach attachContainer}>
	{#if title.length > 0}
		<h1 class="text-[16px] font-normal">{title}</h1>
	{/if}
	{#if comment.length > 0}
		<h1 class="p-3 font-medium">{comment}</h1>
	{/if}
	{#if isCardView && sortableDefinition.length > 0}
		<div class="table-card-sort-controls">
			<select
				class="table-card-sort-select select select-common"
				value={activeSortKey}
				onchange={onCardSortKeyChange}
				aria-label="Sort cards by"
			>
				<option value="">Sort by ...</option>
				{#each sortableDefinition as def (def.key)}
					<option value={def.key}>{def.name ?? def.key}</option>
				{/each}
			</select>
			{#if activeSortKey}
				<button
					type="button"
					class="table-card-sort-direction button-secondary"
					onclick={cycleSortDirection}
					title={cardSortDirectionLabel === '↓'
						? 'Sort direction: ascending'
						: 'Sort direction: descending'}
					aria-label="Toggle sort direction"
				>
					{cardSortDirectionLabel}
				</button>
			{/if}
		</div>
	{/if}

	<table class="w-full">
		{#if showHeaders}
			{#if thead}
				{@render thead({ definition })}
			{:else}
				<thead>
					<tr class={thClass}>
						{#each definition as def (def.key ?? def.name ?? def)}
							<th class={def.th}>
								{#if isDefSortable(def)}
									<button
										type="button"
										class="table-sort-button"
										onclick={() => toggleSort(def)}
										aria-label={`Sort by ${def.name ?? def.key}`}
										title={`Sort by ${def.name ?? def.key}`}
									>
										{#if def.icon}
											<Icon icon={def.icon} class="h-7 w-7" />
										{:else}
											{def.name ?? ''}
										{/if}
										{#if getSortIndicator(def) !== ''}
											<span class="table-sort-indicator">{getSortIndicator(def)}</span>
										{/if}
									</button>
								{:else if def.icon}
									<Icon icon={def.icon} class="h-7 w-7" />
								{:else}
									{def.name ?? ''}
								{/if}
							</th>
						{/each}
					</tr>
				</thead>
			{/if}
		{/if}
		{#if sortedData && sortedData.length > 0}
			<tbody>
				{#each sortedData as row, rowIdx (fnRowId(row, rowIdx))}
					<tr class={trClass} data-custom={row.name}>
						{#snippet rowRender()}
							{#each definition as def (def.key ?? def.name ?? def)}
								<td
									class={def.class
										? typeof def.class == 'function'
											? def.class(row, def)
											: def.class
										: ''}
									data-label={showHeaders ? (def.name ?? '') : ''}
								>
									{#if def.custom}
										{#if children}
											{@render children({ row, def, rowIdx })}
										{:else}
											{row[def.key] ?? ''}
										{/if}
									{:else}
										<AutoPlaceholder loading={row[def.key] == null}
											>{row[def.key] ?? ''}</AutoPlaceholder
										>
									{/if}
								</td>
							{/each}
						{/snippet}
						{#if rowChildren}
							{@render rowChildren({ row, rowIdx, definition, rowRender })}
						{:else}
							{@render rowRender()}
						{/if}
					</tr>
				{/each}
			</tbody>
		{:else if showNothing}
			<tbody>
				<tr class="table-empty-row row-hover">
					<td colspan={definition.length}>
						<div class="layout-x">
							<p class="font-medium">This table is empty</p>
						</div>
					</td>
				</tr>
			</tbody>
		{/if}
	</table>
</div>

<style lang="postcss">
	@reference "../../../app.css";

	table {
		width: 100%;
		border-collapse: collapse;
	}
	th,
	td {
		@apply p-2! align-middle!;
	}
	.table-container:not(.autocard) :global(thead th) {
		border-bottom: 1px solid var(--table-separator-color);
	}
	.table-container:not(.autocard) :global(tbody tr > td) {
		border-bottom: 1px solid var(--table-separator-color);
	}
	.table-container :global(.table-empty-row > td) {
		border-bottom: 1px solid var(--table-separator-color);
		background-color: transparent;
	}
	.table-container {
		overflow-x: auto;
		-webkit-overflow-scrolling: touch;
		width: 100%;
		container-type: inline-size;
		--table-separator-color: light-dark(var(--color-surface-400), var(--color-surface-600));
	}

	.table-sort-button {
		align-items: center;
		background: transparent;
		border: 0;
		color: inherit;
		cursor: pointer;
		display: inline-flex;
		font: inherit;
		gap: 0.35rem;
		padding: 0;
		text-align: left;
		width: 100%;
	}

	.table-sort-indicator {
		font-size: 12px;
		line-height: 1;
	}

	.table-card-sort-controls {
		@apply mb-2 grid grid-cols-[1fr_auto] items-center gap-2;
	}

	.table-card-sort-select {
		@apply h-9 w-full min-w-0 px-2 text-sm;
	}

	.table-card-sort-direction {
		@apply h-9 min-w-9 justify-center px-2 text-sm leading-none;
	}

	.autocard {
		th,
		td {
			@apply text-wrap;
		}

		table {
			background-color: unset;
		}

		tr {
			display: block;
			@apply layout-grid-low-48;
		}

		tbody tr:not(:last-child) {
			border-bottom: 1px solid var(--table-separator-color);
		}

		thead {
			display: none;
		}

		td {
			@apply layout-y-start-low overflow-x-auto;
		}

		td[data-label]:not([data-label='']):before {
			content: attr(data-label);
			display: inline-block;
			color: var(--convertigo-text-strong);
			font-size: 14px;
			font-weight: 600;
			line-height: 1.5;
			width: 100%;
			z-index: 1;
		}
	}
</style>
