<script>
	import AccordionGroup from '$lib/common/components/AccordionGroup.svelte';
	import InputGroup from '$lib/common/components/InputGroup.svelte';
	import AutoSvg from '$lib/utils/AutoSvg.svelte';
	import { draggedData } from '$lib/utils/dndStore';
	import Ico from '$lib/utils/Ico.svelte';
	import { getUrl } from '$lib/utils/service';
	import { loadPaletteContext } from './paletteContext';
	import StudioEmptyState from './StudioEmptyState.svelte';
	import StudioSection from './StudioSection.svelte';

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

<div class="studio-palette layout-y-stretch">
	<div class="studio-palette__search studio-panel-toolbar">
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
			<StudioEmptyState message="Loading palette" loading />
		{:else if paletteError}
			<StudioEmptyState message={paletteError} />
		{:else if !selectedId}
			<StudioEmptyState message="No object selected" />
		{:else if filteredCategories.length === 0}
			<StudioEmptyState message="No component available" />
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
					<StudioSection
						value={categoryKey(category, index)}
						title={category.name}
						count={category.items?.length ?? 0}
						countVariant="number"
					>
						{#snippet panel()}
							<div class="studio-palette__items layout-grid-none-[8.2rem]">
								{#each category.items ?? [] as item (itemKey(item))}
									{@const selected = itemKey(item) === selectedPaletteItemKey}
									<button
										type="button"
										class="studio-palette__item layout-x-start-low"
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
											<span class="studio-palette__item-name studio-ellipsis"
												>{itemDisplayName(item)}</span
											>
										</span>
									</button>
								{/each}
							</div>
						{/snippet}
					</StudioSection>
				{/each}
			</AccordionGroup>
		{/if}
	</div>
</div>

<style>
	.studio-palette {
		height: 100%;
		min-height: 0;
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

	.studio-palette__items {
		gap: 0.18rem;
		padding: 0.35rem;
	}

	.studio-palette__item {
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
		font-size: 0.72rem;
		font-weight: 650;
	}
</style>
