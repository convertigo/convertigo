<script>
	import AccordionGroup from '$lib/common/components/AccordionGroup.svelte';
	import AccordionSection from '$lib/common/components/AccordionSection.svelte';
	import InputGroup from '$lib/common/components/InputGroup.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import AutoSvg from '$lib/utils/AutoSvg.svelte';
	import { draggedData } from '$lib/utils/dndStore';
	import Ico from '$lib/utils/Ico.svelte';
	import { getUrl } from '$lib/utils/service';
	import { loadPaletteContext } from './paletteContext';

	/**
	 * @typedef {Object} PaletteItem
	 * @property {string=} id
	 * @property {string=} name
	 * @property {string=} classname
	 * @property {string=} description
	 * @property {string=} shortDescriptionHtml
	 * @property {string=} longDescriptionText
	 * @property {string=} longDescriptionHtml
	 * @property {string=} shortDescriptionText
	 * @property {string=} propertiesDescriptionHtml
	 * @property {string=} icon
	 * @property {boolean=} builtin
	 * @property {boolean=} additional
	 */
	/**
	 * @typedef {Object} PaletteCategory
	 * @property {string=} name
	 * @property {PaletteItem[]=} items
	 */

	/**
	 * @type {{
	 * 	selectedId?: string,
	 * 	active?: boolean,
	 * 	selectedPaletteItem?: PaletteItem | null,
	 * 	onPaletteItemSelect?: (item: PaletteItem) => void
	 * }}
	 */
	let {
		selectedId = '',
		active = true,
		selectedPaletteItem = null,
		onPaletteItemSelect
	} = $props();

	let query = $state('');
	/** @type {{ id: string, fallbackFrom: string, categories: PaletteCategory[] }} */
	let paletteContext = $state(emptyPaletteContext());
	let paletteLoading = $state(false);
	let paletteError = $state('');
	let paletteRequestId = '';
	let paletteLoadSerial = 0;
	let openedCategories = $state(/** @type {string[]} */ ([]));
	let paletteCategoriesTouched = $state(false);
	let filteredCategories = $derived(filterCategories(paletteContext.categories));
	let selectedPaletteItemKey = $derived(itemKey(selectedPaletteItem));
	let paletteOpenValues = $derived.by(() => {
		const keys = filteredCategories.map((category, index) => categoryKey(category, index));
		if (query.trim()) {
			return keys;
		}
		if (paletteCategoriesTouched) {
			const available = new Set(keys);
			return openedCategories.filter((value) => available.has(value));
		}
		return keys.slice(0, 3);
	});

	$effect(() => {
		const nextId = active ? selectedId : '';
		if (!active || nextId === paletteRequestId) {
			return;
		}
		paletteRequestId = nextId;
		const serial = ++paletteLoadSerial;
		if (!nextId) {
			paletteContext = emptyPaletteContext();
			paletteError = '';
			paletteLoading = false;
			return;
		}
		void loadPalette(nextId, serial);
	});

	function emptyPaletteContext() {
		return { id: '', fallbackFrom: '', categories: [] };
	}

	/**
	 * @param {string} nextId
	 * @param {number} serial
	 */
	async function loadPalette(nextId, serial) {
		paletteLoading = true;
		paletteError = '';
		try {
			const context = await loadPaletteContext(nextId);
			if (serial === paletteLoadSerial) {
				if (!samePaletteContext(paletteContext, context)) {
					paletteContext = context;
					openedCategories = [];
					paletteCategoriesTouched = false;
				}
			}
		} catch (err) {
			if (serial === paletteLoadSerial) {
				paletteContext = emptyPaletteContext();
				openedCategories = [];
				paletteCategoriesTouched = false;
				paletteError = String(err instanceof Error ? err.message : err);
			}
		} finally {
			if (serial === paletteLoadSerial) {
				paletteLoading = false;
			}
		}
	}

	/**
	 * @param {PaletteCategory} category
	 * @param {number} index
	 * @returns {string}
	 */
	function categoryKey(category, index) {
		return `${category.name ?? 'Category'}:${index}`;
	}

	/**
	 * @param {PaletteCategory[]} categories
	 * @returns {PaletteCategory[]}
	 */
	function filterCategories(categories = []) {
		const needle = query.trim().toLowerCase();
		return categories
			.map((category) => ({
				...category,
				items: (category.items ?? []).filter((item) => {
					if (!needle) {
						return true;
					}
					return `${category.name ?? ''} ${item.name ?? ''} ${item.classname ?? ''} ${item.shortDescriptionText ?? ''}`
						.toLowerCase()
						.includes(needle);
				})
			}))
			.filter((category) => category.items.length > 0);
	}

	/**
	 * @param {{ id: string, fallbackFrom: string, categories: PaletteCategory[] }} current
	 * @param {{ id: string, fallbackFrom: string, categories: PaletteCategory[] }} next
	 * @returns {boolean}
	 */
	function samePaletteContext(current, next) {
		return paletteFingerprint(current.categories) === paletteFingerprint(next.categories);
	}

	/**
	 * @param {PaletteCategory[]} categories
	 * @returns {string}
	 */
	function paletteFingerprint(categories = []) {
		return categories
			.map((category) =>
				[category.name ?? '', ...(category.items ?? []).map((item) => itemKey(item))].join('\u001f')
			)
			.join('\u001e');
	}

	function iconSource(icon) {
		if (!icon) {
			return '';
		}
		return `${getUrl()}studio.dbo.GetIcon?iconPath=${encodeURIComponent(icon)}`;
	}

	/**
	 * @param {PaletteItem | null | undefined} item
	 * @returns {string}
	 */
	function itemKey(item) {
		return item?.id || item?.classname || item?.name || '';
	}

	/**
	 * @param {PaletteItem} item
	 * @returns {string}
	 */
	function itemDisplayName(item) {
		return item.name || item.classname || 'Component';
	}

	/**
	 * @param {PaletteItem} item
	 * @returns {string}
	 */
	function itemTitle(item) {
		return [itemDisplayName(item), item.classname, item.shortDescriptionText]
			.filter(Boolean)
			.join('\n');
	}

	function onDragStart(event, item) {
		const paletteData = { type: 'paletteData', data: item, options: {} };
		event.dataTransfer?.setData('text/plain', JSON.stringify(paletteData));
		event.dataTransfer?.setData('palettedata', JSON.stringify(paletteData));
		if (event.dataTransfer) {
			event.dataTransfer.effectAllowed = 'copy';
		}
		$draggedData = paletteData;
	}

	/**
	 * @param {PaletteItem} item
	 */
	function selectPaletteItem(item) {
		onPaletteItemSelect?.(item);
	}
</script>

<div class="studio-palette">
	<div class="studio-palette__search">
		<InputGroup
			id="studio-palette-search"
			type="search"
			placeholder="Search component..."
			class="w-full"
			icon="mdi:magnify"
			bind:value={query}
		/>
	</div>

	<div class="studio-palette__content">
		{#if paletteLoading && paletteContext.categories.length === 0}
			<AutoPlaceholder loading={true} />
			<AutoPlaceholder loading={true} class="h-4 w-32" />
		{:else if paletteError}
			<div class="studio-palette__empty">
				{paletteError}
			</div>
		{:else if !selectedId}
			<div class="studio-palette__empty">No object selected</div>
		{:else if filteredCategories.length === 0}
			<div class="studio-palette__empty">No component available</div>
		{:else}
			<AccordionGroup
				class="studio-palette__categories"
				value={paletteOpenValues}
				onValueChange={({ value }) => {
					openedCategories = value;
					paletteCategoriesTouched = !query.trim();
				}}
				multiple
			>
				{#each filteredCategories as category, index (categoryKey(category, index))}
					<AccordionSection
						value={categoryKey(category, index)}
						class="studio-palette__category"
						triggerClass="studio-palette__category-title"
						panelClass="studio-palette__category-panel"
						title={category.name}
						count={category.items?.length ?? 0}
						countVariant="number"
						titleClass="studio-palette__category-name"
						indicatorSize={4}
					>
						{#snippet panel()}
							<div class="studio-palette__items">
								{#each category.items ?? [] as item (itemKey(item))}
									{@const selected = itemKey(item) === selectedPaletteItemKey}
									<button
										type="button"
										class="studio-palette__item"
										class:studio-palette__item--selected={selected}
										aria-label={itemDisplayName(item)}
										aria-pressed={selected}
										title={itemTitle(item)}
										draggable="true"
										onclick={() => selectPaletteItem(item)}
										ondragstart={(event) => onDragStart(event, item)}
										ondragend={() => ($draggedData = undefined)}
									>
										<span class="studio-palette__icon">
											{#if iconSource(item.icon)}
												<AutoSvg
													src={iconSource(item.icon)}
													alt=""
													class="h-5 w-5 object-contain"
												/>
											{:else}
												<Ico icon="mdi:cube-outline" size={4} />
											{/if}
										</span>
										<span class="studio-palette__item-main">
											<span class="studio-palette__item-name">{itemDisplayName(item)}</span>
										</span>
									</button>
								{/each}
							</div>
						{/snippet}
					</AccordionSection>
				{/each}
			</AccordionGroup>
		{/if}
	</div>
</div>

<style>
	.studio-palette {
		display: flex;
		height: 100%;
		min-height: 0;
		flex-direction: column;
	}

	.studio-palette__search {
		border-bottom: 1px solid var(--color-surface-200-800);
		background: color-mix(in oklab, var(--color-surface-50-950) 94%, transparent);
		padding: 0.5rem;
	}

	.studio-palette__content {
		min-height: 0;
		flex: 1;
		overflow: auto;
		padding: 0;
	}

	:global(.studio-palette__categories) {
		width: 100%;
	}

	:global(.studio-palette__category) {
		border-bottom: 1px solid var(--color-surface-200-800);
		background: var(--studio-panel-bg, var(--color-surface-50-950));
	}

	:global(.studio-palette__category:first-child) {
		border-top: 0;
	}

	:global(.studio-palette__category-title) {
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

	:global(.studio-palette__category-title:hover:not(:disabled)) {
		background: color-mix(in oklab, var(--color-primary-500) 9%, transparent);
		color: var(--color-surface-950-50);
	}

	:global(.studio-palette__category-title [data-state]) {
		color: var(--color-surface-700-300);
	}

	:global(.studio-palette__category-title:hover:not(:disabled) [data-state]) {
		color: var(--color-surface-950-50);
	}

	:global(.studio-palette__category-title svg) {
		width: 1rem;
		height: 1rem;
	}

	:global(.studio-palette__category-name) {
		overflow: hidden;
		color: currentcolor;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	:global(.studio-palette__category-panel) {
		background: color-mix(in oklab, var(--color-surface-50-950) 78%, transparent);
		padding: 0;
	}

	.studio-palette__items {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(8.2rem, 1fr));
		gap: 0.18rem;
		padding: 0.35rem;
	}

	.studio-palette__item {
		display: grid;
		grid-template-columns: 1.35rem minmax(0, 1fr);
		align-items: center;
		gap: 0.35rem;
		width: 100%;
		min-height: 2.25rem;
		border: 1px solid transparent;
		border-radius: 0.35rem;
		background: transparent;
		color: var(--color-surface-900-100);
		padding: 0.24rem 0.34rem;
		text-align: left;
	}

	.studio-palette__item:hover,
	.studio-palette__item--selected {
		border-color: color-mix(in oklab, var(--color-primary-500) 35%, transparent);
		background: color-mix(in oklab, var(--color-primary-500) 9%, transparent);
	}

	.studio-palette__item--selected {
		color: var(--color-primary-700-300);
	}

	.studio-palette__icon {
		display: grid;
		width: 1.35rem;
		height: 1.35rem;
		place-items: center;
		color: var(--color-primary-600-400);
	}

	.studio-palette__item-main {
		min-width: 0;
	}

	.studio-palette__item-name {
		display: block;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-palette__item-name {
		font-size: 0.72rem;
		font-weight: 650;
	}

	.studio-palette__empty {
		display: grid;
		min-height: 7rem;
		place-items: center;
		color: var(--color-surface-600-400);
		font-size: 0.82rem;
		text-align: center;
	}
</style>
