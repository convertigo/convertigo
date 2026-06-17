<script>
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import SaveCancelButtons from '$lib/admin/components/SaveCancelButtons.svelte';
	import AccordionGroup from '$lib/common/components/AccordionGroup.svelte';
	import { createDatabaseObjectProperties } from '$lib/common/DatabaseObjectProperties.svelte.js';
	import LightSvelte from '$lib/common/Light.svelte';
	import Editor from '$lib/studio/editor/Editor.svelte';
	import {
		SMART_TYPE_MODES,
		asEditorValue,
		canOpenCodeProperty,
		getPropertyLanguage,
		isSmartTypeProperty
	} from '$lib/studio/propertyEditors';
	import { untrack } from 'svelte';
	import StudioEmptyState from './StudioEmptyState.svelte';
	import StudioIconButton from './StudioIconButton.svelte';
	import StudioSection from './StudioSection.svelte';

	/**
	 * @type {{
	 *  selectedId?: string,
	 *  active?: boolean,
	 *  onSave?: (id: string) => void | Promise<void>,
	 *  onOpenPropertyEditor?: (target: { id: string, propertyName?: string, displayName?: string, value?: any }) => void,
	 *  onOpenPropertyPicker?: (target: { id: string, propertyName?: string, displayName?: string, value?: any }) => void
	 * }}
	 */
	let {
		selectedId = '',
		active = true,
		onSave,
		onOpenPropertyEditor,
		onOpenPropertyPicker
	} = $props();

	let openedCategories = $state(/** @type {string[]} */ ([]));
	let clickedCategories = $state(/** @type {string[]} */ ([]));
	let monacoRow = $state();
	let monacoValue = $state('');
	let requestedSelectionId = '';
	let {
		id,
		properties,
		categories,
		onSelectionChange,
		hasChanges,
		loading,
		getChanges,
		save,
		cancel
	} = $derived(createDatabaseObjectProperties());
	let monacoLanguage = $derived(getPropertyLanguage(monacoRow, selectedId));
	let monacoTitle = $derived(monacoRow?.displayName ?? monacoRow?.name ?? 'Editor');
	let monacoTheme = $derived(LightSvelte.light ? '' : 'vs-dark');

	$effect(() => {
		const nextId = selectedId;
		if (!active) {
			return;
		}
		if (!nextId) {
			requestedSelectionId = '';
			return;
		}
		if (nextId === id || nextId === requestedSelectionId) {
			return;
		}
		requestedSelectionId = nextId;
		void onSelectionChange({ selectedValue: [nextId] }).finally(() => {
			if (requestedSelectionId === nextId) {
				requestedSelectionId = '';
			}
		});
	});

	function getDefaultOpenedCategories() {
		const open = categories.filter(
			({ category, properties: rows }) => clickedCategories.includes(category) && rows.length > 0
		);
		if (open.length > 0) {
			return open.map(({ category }) => category);
		}
		const first = categories.find(({ properties: rows }) => rows.length > 0);
		return first ? [first.category] : [];
	}

	function getType(row) {
		if (isSmartTypeProperty(row)) {
			return 'smarttype';
		}
		let { class: cls, value, values } = row;
		if (row.symbols && value != row.originalValue) {
			return 'text';
		}
		untrack(() => {
			row.symbols = false;
		});
		if (values && values.includes(value)) {
			return values.length < 4 ? 'segment' : 'combo';
		}
		if (row.isMultiline) {
			return 'textarea';
		}
		if (cls?.endsWith('Boolean')) {
			return 'boolean';
		}
		if (
			cls?.endsWith('Integer') ||
			cls?.endsWith('Long') ||
			cls?.endsWith('Double') ||
			cls?.endsWith('Float') ||
			cls?.endsWith('Short') ||
			cls?.endsWith('Byte')
		) {
			return 'number';
		}
		return 'text';
	}

	/**
	 * @param {any} row
	 * @returns {boolean}
	 */
	function isInlineEditable(row) {
		return String(row?.class ?? '').startsWith('java.lang.');
	}

	/**
	 * @param {any} row
	 * @returns {string}
	 */
	function smartMode(row) {
		return ['plain', 'script', 'source'].includes(row?.mode) ? row.mode : 'plain';
	}

	/**
	 * @param {any} row
	 * @param {string} mode
	 */
	function setSmartMode(row, mode) {
		row.mode = mode;
		if (mode === 'source' && row.value == null) {
			row.value = [];
		} else if (mode !== 'source' && Array.isArray(row.value)) {
			row.value = row.value.join('\n');
		}
	}

	/**
	 * @param {any} row
	 * @returns {string}
	 */
	function previewValue(row) {
		const value = asEditorValue(row?.value);
		return value === '' ? 'empty' : value;
	}

	/**
	 * @param {any} row
	 * @returns {{ icon: string, title: string, onclick: () => void }[]}
	 */
	function smartTypeButtons(row) {
		if (smartMode(row) === 'source') {
			return [
				{
					icon: 'mdi:hub',
					title: 'Open source picker',
					onclick: () => openPicker(row)
				}
			];
		}
		if (smartMode(row) === 'script') {
			return [
				{
					icon: 'mdi:open-in-new-variant',
					title: 'Open code editor',
					onclick: () => openMonaco(row)
				}
			];
		}
		return [];
	}

	/**
	 * @param {any} row
	 * @returns {boolean}
	 */
	function hasSmartTypeSideActions(row) {
		return smartMode(row) === 'source' || smartMode(row) === 'script';
	}

	/**
	 * @param {any} row
	 * @returns {number}
	 */
	function textareaRows(row) {
		const value = asEditorValue(row?.value);
		const lines = value ? value.split(/\r\n|\r|\n/).length : 1;
		return Math.min(Math.max(lines, 1), 3);
	}

	/**
	 * @param {any} row
	 * @param {string} category
	 * @param {string} type
	 * @returns {boolean}
	 */
	function isWideField(row, category, type) {
		if (category === 'Information') {
			return false;
		}
		if (type === 'textarea') {
			return textareaRows(row) > 1;
		}
		if (isSmartTypeProperty(row)) {
			return true;
		}
		if (!isInlineEditable(row)) {
			const value = previewValue(row);
			return value.includes('\n') || value.length > 96;
		}
		return false;
	}

	/**
	 * @param {any} row
	 * @param {string} category
	 * @returns {boolean}
	 */
	function isChanged(row, category) {
		return (
			category !== 'Information' &&
			(row.value != row.originalValue || ('mode' in row && row.mode != row.originalMode))
		);
	}

	async function saveChanges() {
		if (!getChanges().length) {
			return;
		}
		if (await save()) {
			await onSave?.(selectedId);
		}
	}

	function openMonaco(row) {
		if (onOpenPropertyEditor) {
			onOpenPropertyEditor({
				id: selectedId,
				propertyName: row?.name,
				displayName: row?.displayName,
				value: row?.value
			});
			return;
		}
		monacoRow = row;
		monacoValue = asEditorValue(row?.value);
	}

	function openPicker(row) {
		onOpenPropertyPicker?.({
			id: selectedId,
			propertyName: row?.name,
			displayName: row?.displayName,
			value: row?.value
		});
	}

	function applyMonaco() {
		if (monacoRow) {
			monacoRow.value = monacoValue;
		}
		monacoRow = undefined;
	}
</script>

<div class="studio-properties layout-y-stretch">
	<div class="studio-properties__actions studio-panel-toolbar">
		<SaveCancelButtons
			class="w-full"
			saveLabel="Save"
			cancelLabel="Cancel"
			onSave={saveChanges}
			onCancel={cancel}
			changesPending={hasChanges}
			disabled={!selectedId || properties.length == 0}
		/>
	</div>

	<div class="studio-properties__body" class:studio-properties__body--loading={loading}>
		{#if !selectedId}
			<StudioEmptyState message="No object selected" />
		{:else}
			<AccordionGroup
				class="studio-properties__sections"
				value={openedCategories.length ? openedCategories : getDefaultOpenedCategories()}
				onValueChange={({ value }) => {
					openedCategories = value;
					clickedCategories = value;
				}}
				multiple
			>
				{#each categories as { category, properties: rows } (category)}
					{@const total = rows.length}
					<StudioSection
						value={category}
						disabled={total == 0}
						title={category}
						count={total}
						countVariant="number"
					>
						{#snippet panel()}
							{#if total === 0}
								<StudioEmptyState message="Empty" small />
							{:else}
								<div class="studio-properties__fields layout-y-start-none">
									{#each rows as row (row.name ?? row.displayName)}
										{@const { value, originalValue, values } = row}
										{@const label = row.displayName ?? row.name ?? ''}
										{@const type = getType(row)}
										{@const changed = isChanged(row, category)}
										{@const inlineEditable = isInlineEditable(row)}
										{@const smartType = isSmartTypeProperty(row)}
										{@const wideField = isWideField(row, category, type)}
										<div
											class="studio-properties__field"
											class:studio-properties__field--wide={wideField}
											class:studio-properties__field--textarea={type === 'textarea' || smartType}
											class:studio-properties__field--changed={changed}
										>
											<div class="studio-properties__field-header layout-x-between-none">
												<span class="studio-properties__field-label" title={label}>
													{label}
												</span>
												{#if changed}
													<span class="studio-properties__field-dirty" aria-label="Modified"></span>
												{/if}
											</div>
											<div class="studio-properties__field-control">
												{#if category == 'Information'}
													<span class="studio-properties__static">{value}</span>
												{:else if smartType}
													<PropertyType
														type="smarttype"
														bind:value={() => value, (nextValue) => (row.value = nextValue)}
														bind:mode={() => smartMode(row), (mode) => setSmartMode(row, mode)}
														item={SMART_TYPE_MODES}
														{originalValue}
														originalMode={row.originalMode}
														rows={smartMode(row) === 'script' ? textareaRows(row) : undefined}
														actionsHorizontal={!hasSmartTypeSideActions(row)}
														buttons={smartTypeButtons(row)}
													/>
												{:else if inlineEditable}
													<PropertyType
														{type}
														bind:value={() => value, (nextValue) => (row.value = nextValue)}
														item={values}
														{originalValue}
														rows={type === 'textarea' ? textareaRows(row) : undefined}
														adaptiveTextarea={type === 'textarea'}
														segmentCompact={type === 'segment'}
														actionsHorizontal={type !== 'textarea'}
														buttons={canOpenCodeProperty(row, selectedId)
															? [
																	{
																		icon: 'mdi:open-in-new-variant',
																		title: 'Open code editor',
																		onclick: () => openMonaco(row)
																	}
																]
															: []}
													/>
												{:else}
													<div class="studio-properties__fallback layout-x-low">
														<code
															class="studio-properties__fallback-value"
															class:studio-properties__fallback-value--compact={!wideField}
															>{previewValue(row)}</code
														>
														<div class="studio-properties__fallback-actions layout-x-low">
															{#if canOpenCodeProperty(row, selectedId)}
																<StudioIconButton
																	icon="mdi:code-tags"
																	size="xs"
																	title="Open code editor"
																	ariaLabel="Open code editor"
																	onclick={() => openMonaco(row)}
																/>
															{/if}
															<StudioIconButton
																icon="mdi:hub"
																size="xs"
																title="Open picker"
																ariaLabel="Open picker"
																onclick={() => openPicker(row)}
															/>
														</div>
													</div>
												{/if}
											</div>
										</div>
									{/each}
								</div>
							{/if}
						{/snippet}
					</StudioSection>
				{/each}
			</AccordionGroup>
			{#if loading}
				<span class="studio-properties__loading studio-spinner" aria-label="Loading properties"
				></span>
			{/if}
		{/if}
	</div>

	{#if monacoRow}
		<div class="studio-properties__editor" role="dialog" aria-modal="true">
			<header class="studio-properties__editor-header">
				<div>
					<strong>{monacoTitle}</strong>
					<span>{selectedId}</span>
				</div>
				<div class="studio-properties__editor-actions">
					<button type="button" class="button-secondary" onclick={() => (monacoRow = undefined)}>
						Cancel
					</button>
					<button type="button" class="button-primary" onclick={applyMonaco}>Apply</button>
				</div>
			</header>
			<div class="studio-properties__editor-body">
				<Editor
					bind:content={monacoValue}
					language={monacoLanguage}
					theme={monacoTheme}
					readOnly={false}
				/>
			</div>
		</div>
	{/if}
</div>

<style>
	.studio-properties {
		height: 100%;
		min-height: 0;
	}

	.studio-properties__body {
		position: relative;
		min-height: 0;
		flex: 1;
		overflow: auto;
		padding: 0;
	}

	.studio-properties__body--loading {
		opacity: 0.82;
	}

	.studio-properties__loading {
		position: sticky;
		z-index: 2;
		right: 0.5rem;
		bottom: 0.5rem;
		display: block;
		margin-left: auto;
		background: color-mix(in oklab, var(--color-surface-50-950) 92%, transparent);
		box-shadow: 0 0.5rem 1.4rem color-mix(in oklab, black 18%, transparent);
		pointer-events: none;
	}

	:global(.studio-properties__sections) {
		width: 100%;
	}

	.studio-properties__fields {
		width: 100%;
	}

	.studio-properties__field {
		display: grid;
		width: 100%;
		grid-template-columns: minmax(5.8rem, 34%) minmax(0, 1fr);
		align-items: center;
		column-gap: 0.55rem;
		row-gap: 0.32rem;
		border-bottom: 1px solid color-mix(in oklab, var(--color-surface-200-800) 62%, transparent);
		padding: 0.46rem 0.65rem;
	}

	.studio-properties__field:last-child {
		border-bottom: 0;
	}

	.studio-properties__field--wide {
		grid-template-columns: minmax(0, 1fr);
		align-items: stretch;
		padding-block: 0.58rem 0.65rem;
	}

	.studio-properties__field--changed {
		background: color-mix(in oklab, var(--color-primary-500) 5%, transparent);
	}

	.studio-properties__field-header {
		min-width: 0;
		gap: 0.4rem;
	}

	.studio-properties__field-label {
		min-width: 0;
		overflow: hidden;
		color: var(--color-surface-600-400);
		font-size: 0.67rem;
		font-weight: 750;
		letter-spacing: 0;
		line-height: 1.2;
		text-overflow: ellipsis;
		text-transform: uppercase;
		white-space: nowrap;
	}

	.studio-properties__field-dirty {
		width: 0.45rem;
		height: 0.45rem;
		flex: 0 0 auto;
		border-radius: 999px;
		background: var(--color-primary-500);
	}

	.studio-properties__field-control {
		min-width: 0;
	}

	.studio-properties__field-control :global(.layout-y-low.sm\:layout-x-low > div:first-child) {
		width: 100%;
		min-width: 0;
	}

	.studio-properties__field:not(.studio-properties__field--wide)
		.studio-properties__field-control
		:global(.layout-y-low.sm\:layout-x-low) {
		width: 100%;
		flex-direction: row;
		align-items: center;
		gap: 0.32rem;
	}

	.studio-properties__field--wide
		.studio-properties__field-control
		:global(.layout-y-low.sm\:layout-x-low) {
		width: 100%;
		flex-direction: column;
		align-items: stretch;
		gap: 0.32rem;
	}

	.studio-properties__field--wide.studio-properties__field--textarea
		.studio-properties__field-control
		:global(.layout-y-low.sm\:layout-x-low) {
		flex-direction: row;
		align-items: flex-start;
	}

	.studio-properties__field-control :global(.sm\:grow) {
		width: 100%;
		min-width: 0;
	}

	.studio-properties__field:not(.studio-properties__field--wide)
		.studio-properties__field-control
		:global(.layout-x-low.h-fit) {
		flex: 0 0 auto;
		align-self: center;
	}

	.studio-properties__field--wide .studio-properties__field-control :global(.layout-x-low.h-fit) {
		align-self: flex-end;
	}

	.studio-properties__field-control :global(.input-common),
	.studio-properties__field-control :global(input),
	.studio-properties__field-control :global(textarea),
	.studio-properties__field-control :global(select) {
		width: 100%;
		min-width: 0;
	}

	.studio-properties__field-control :global(textarea) {
		resize: vertical;
	}

	.studio-properties__static {
		display: block;
		max-width: 100%;
		overflow-wrap: anywhere;
		font-size: 0.76rem;
	}

	.studio-properties__fallback {
		min-width: 0;
		align-items: center;
	}

	.studio-properties__fallback-value {
		display: block;
		flex: 1 1 auto;
		max-height: 7.5rem;
		min-height: 2rem;
		overflow: auto;
		margin: 0;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.35rem;
		background: color-mix(in oklab, var(--color-surface-100-900) 72%, transparent);
		color: var(--color-surface-800-200);
		padding: 0.45rem 0.55rem;
		font-family: var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
		font-size: 0.7rem;
		line-height: 1.35;
		overflow-wrap: anywhere;
		white-space: pre-wrap;
	}

	.studio-properties__fallback-value--compact {
		min-height: 1.75rem;
		max-height: 1.75rem;
		overflow: hidden;
		padding-block: 0.34rem;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-properties__fallback-actions {
		flex: 0 0 auto;
		justify-content: flex-end;
	}

	.studio-properties__editor {
		position: fixed;
		inset: 3rem;
		z-index: 95;
		display: grid;
		grid-template-rows: auto minmax(0, 1fr);
		overflow: hidden;
		border: 1px solid var(--color-surface-300-700);
		border-radius: 0.45rem;
		background: #1e1e1e;
		box-shadow: 0 1.5rem 4rem rgb(0 0 0 / 0.35);
	}

	.studio-properties__editor::before {
		position: fixed;
		inset: -3rem;
		z-index: -1;
		background: rgb(0 0 0 / 0.45);
		content: '';
	}

	.studio-properties__editor-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 1rem;
		border-bottom: 1px solid var(--color-surface-700);
		background: color-mix(in oklab, #1e1e1e 90%, var(--color-primary-500));
		padding: 0.55rem 0.7rem;
		color: white;
	}

	.studio-properties__editor-header div:first-child {
		display: grid;
		min-width: 0;
		gap: 0.1rem;
	}

	.studio-properties__editor-header strong,
	.studio-properties__editor-header span {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-properties__editor-header span {
		color: #b8c2d6;
		font-size: 0.7rem;
	}

	.studio-properties__editor-actions {
		display: flex;
		flex: 0 0 auto;
		gap: 0.45rem;
	}

	.studio-properties__editor-body {
		min-height: 0;
	}

	@media (max-width: 760px) {
		.studio-properties__editor {
			inset: 0.5rem;
		}
	}
</style>
