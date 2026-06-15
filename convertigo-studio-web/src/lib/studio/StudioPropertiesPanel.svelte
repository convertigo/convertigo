<script>
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import SaveCancelButtons from '$lib/admin/components/SaveCancelButtons.svelte';
	import AccordionGroup from '$lib/common/components/AccordionGroup.svelte';
	import AccordionSection from '$lib/common/components/AccordionSection.svelte';
	import { createDatabaseObjectProperties } from '$lib/common/DatabaseObjectProperties.svelte.js';
	import LightSvelte from '$lib/common/Light.svelte';
	import Editor from '$lib/studio/editor/Editor.svelte';
	import {
		asEditorValue,
		getPropertyLanguage,
		isMonacoProperty
	} from '$lib/studio/propertyEditors';
	import { untrack } from 'svelte';

	/**
	 * @type {{
	 *  selectedId?: string,
	 *  active?: boolean,
	 *  onSave?: (id: string) => void | Promise<void>,
	 *  onOpenPropertyEditor?: (target: { id: string, propertyName?: string, displayName?: string, value?: any }) => void
	 * }}
	 */
	let { selectedId = '', active = true, onSave, onOpenPropertyEditor } = $props();

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
			return 'array';
		}
		if (cls?.endsWith('Boolean')) {
			return 'boolean';
		}
		if (cls?.endsWith('Integer') || cls?.endsWith('Long')) {
			return 'number';
		}
		return 'text';
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

	function applyMonaco() {
		if (monacoRow) {
			monacoRow.value = monacoValue;
		}
		monacoRow = undefined;
	}
</script>

<div class="studio-properties">
	<div class="studio-properties__actions">
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
			<div class="studio-properties__empty">No object selected</div>
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
					<AccordionSection
						value={category}
						class="studio-properties__section"
						triggerClass="studio-properties__section-trigger"
						panelClass="studio-properties__section-panel"
						disabled={total == 0}
						title={category}
						count={total}
						countVariant="number"
						titleClass="studio-properties__section-title"
						indicatorSize={4}
					>
						{#snippet panel()}
							{#if total === 0}
								<div class="studio-properties__empty studio-properties__empty--small">Empty</div>
							{:else}
								<div class="studio-properties__fields">
									{#each rows as row (row.name ?? row.displayName)}
										{@const { class: cls, value, originalValue, values } = row}
										{@const label = row.displayName ?? row.name ?? ''}
										{@const changed = category != 'Information' && value != originalValue}
										<div
											class="studio-properties__field"
											class:studio-properties__field--changed={changed}
										>
											<div class="studio-properties__field-header">
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
												{:else if cls?.startsWith('java.lang.')}
													{@const type = getType(row)}
													<PropertyType
														{type}
														bind:value={() => value, (nextValue) => (row.value = nextValue)}
														item={values}
														{originalValue}
														actionsHorizontal={true}
														buttons={isMonacoProperty(row, selectedId)
															? [
																	{
																		icon: 'mdi:open-in-new-variant',
																		title: 'Open text editor',
																		onclick: () => openMonaco(row)
																	}
																]
															: []}
													/>
												{:else}
													<span class="studio-properties__static">{row.value}</span>
												{/if}
											</div>
										</div>
									{/each}
								</div>
							{/if}
						{/snippet}
					</AccordionSection>
				{/each}
			</AccordionGroup>
			{#if loading}
				<span class="studio-properties__loading" aria-label="Loading properties"></span>
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
		display: flex;
		height: 100%;
		min-height: 0;
		flex-direction: column;
	}

	.studio-properties__actions {
		border-bottom: 1px solid var(--color-surface-200-800);
		background: color-mix(in oklab, var(--color-surface-50-950) 94%, transparent);
		padding: 0.45rem 0.5rem;
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
		width: 1.35rem;
		height: 1.35rem;
		margin-left: auto;
		border: 2px solid color-mix(in oklab, var(--color-primary-500) 20%, transparent);
		border-top-color: var(--color-primary-500);
		border-radius: 999px;
		background: color-mix(in oklab, var(--color-surface-50-950) 92%, transparent);
		box-shadow: 0 0.5rem 1.4rem color-mix(in oklab, black 18%, transparent);
		animation: studio-properties-loading 0.75s linear infinite;
		pointer-events: none;
	}

	.studio-properties__empty {
		display: grid;
		min-height: 8rem;
		place-items: center;
		color: var(--color-surface-600-400);
		font-size: 0.82rem;
	}

	.studio-properties__empty--small {
		min-height: 3rem;
		border-top: 1px dashed var(--color-surface-200-800);
	}

	:global(.studio-properties__sections) {
		width: 100%;
	}

	:global(.studio-properties__section) {
		border-bottom: 1px solid var(--color-surface-200-800);
		background: var(--studio-panel-bg, var(--color-surface-50-950));
	}

	:global(.studio-properties__section:first-child) {
		border-top: 0;
	}

	:global(.studio-properties__section-trigger) {
		min-height: 2.45rem;
		border-bottom: 1px solid var(--color-surface-200-800);
		background: var(
			--studio-panel-header-bg,
			color-mix(in oklab, var(--color-surface-100-900) 88%, transparent)
		);
		color: var(--color-surface-800-200);
		padding: 0.45rem 0.65rem;
		font-size: 0.78rem;
		font-weight: 700;
		text-transform: uppercase;
	}

	:global(.studio-properties__section-trigger:hover:not(:disabled)) {
		background: color-mix(in oklab, var(--color-primary-500) 9%, transparent);
		color: var(--color-surface-950-50);
	}

	:global(.studio-properties__section-trigger [data-state]) {
		color: var(--color-surface-700-300);
	}

	:global(.studio-properties__section-trigger:hover:not(:disabled) [data-state]) {
		color: var(--color-surface-950-50);
	}

	:global(.studio-properties__section-trigger svg) {
		width: 1rem;
		height: 1rem;
	}

	:global(.studio-properties__section-title) {
		overflow: hidden;
		color: currentcolor;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	:global(.studio-properties__section-panel) {
		background: color-mix(in oklab, var(--color-surface-50-950) 78%, transparent);
		padding: 0;
	}

	.studio-properties__fields {
		display: grid;
	}

	.studio-properties__field {
		display: grid;
		gap: 0.32rem;
		border-bottom: 1px solid color-mix(in oklab, var(--color-surface-200-800) 62%, transparent);
		padding: 0.58rem 0.65rem 0.65rem;
	}

	.studio-properties__field:last-child {
		border-bottom: 0;
	}

	.studio-properties__field--changed {
		background: color-mix(in oklab, var(--color-primary-500) 5%, transparent);
	}

	.studio-properties__field-header {
		display: flex;
		min-width: 0;
		align-items: center;
		justify-content: space-between;
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

	.studio-properties__field-control :global(.layout-y-low.sm\:layout-x-low) {
		width: 100%;
		flex-direction: column;
		align-items: stretch;
		gap: 0.32rem;
	}

	.studio-properties__field-control :global(.sm\:grow) {
		width: 100%;
		min-width: 0;
	}

	.studio-properties__field-control :global(.layout-x-low.h-fit) {
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

	@keyframes studio-properties-loading {
		to {
			transform: rotate(360deg);
		}
	}
</style>
