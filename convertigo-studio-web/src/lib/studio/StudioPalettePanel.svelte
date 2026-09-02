<script>
	import Button from '$lib/admin/components/Button.svelte';
	import AccordionGroup from '$lib/common/components/AccordionGroup.svelte';
	import InputGroup from '$lib/common/components/InputGroup.svelte';
	import AutoSvg from '$lib/utils/AutoSvg.svelte';
	import { draggedData } from '$lib/utils/dndStore';
	import Ico from '$lib/utils/Ico.svelte';
	import { getUrl } from '$lib/utils/service';
	import { onDestroy, tick } from 'svelte';
	import { loadPaletteContext, paletteContextLabel } from './paletteContext';
	import StudioEmptyState from './StudioEmptyState.svelte';
	import StudioSection from './StudioSection.svelte';

	const PALETTE_RETRY_DELAY_MS = 160;
	const PALETTE_TIMEOUT_MS = 20_000;

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
	 * 	revealRequest?: { key?: string, contextId?: string, serial?: number },
	 * 	onPaletteItemSelect?: (item: PaletteItem) => void
	 * }}
	 */
	let {
		selectedId = '',
		active = true,
		selectedPaletteItem = null,
		revealRequest = { key: '', contextId: '', serial: 0 },
		onPaletteItemSelect
	} = $props();

	let query = $state('');
	/** @type {{ id: string, fallbackFrom: string, categories: PaletteCategory[] }} */
	let paletteContext = $state(emptyPaletteContext());
	let paletteLoading = $state(false);
	let paletteError = $state('');
	let paletteRequestId = $state('');
	let paletteLoadSerial = 0;
	/** @type {AbortController | null} */
	let paletteRequestController = null;
	let openedCategories = $state(/** @type {string[]} */ ([]));
	let paletteCategoriesTouched = $state(false);
	let handledRevealSerial = 0;
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
		const nextId = active ? revealRequest?.contextId || selectedId : '';
		if (!active) {
			cancelPaletteRequest();
			paletteRequestId = '';
			paletteLoading = false;
			return;
		}
		if (nextId === paletteRequestId) {
			return;
		}
		cancelPaletteRequest();
		paletteRequestId = nextId;
		const serial = ++paletteLoadSerial;
		if (!nextId) {
			paletteContext = emptyPaletteContext();
			paletteError = '';
			paletteLoading = false;
			return;
		}
		paletteRequestController = new AbortController();
		void loadPalette(nextId, serial, 0, paletteRequestController);
	});

	onDestroy(cancelPaletteRequest);

	$effect(() => {
		const serial = Number(revealRequest?.serial ?? 0);
		const key = String(revealRequest?.key ?? '');
		if (!serial || serial === handledRevealSerial || !key || !active) {
			return;
		}
		const categoryIndex = paletteContext.categories.findIndex((category) =>
			(category.items ?? []).some((item) => itemKey(item) === key)
		);
		if (categoryIndex < 0) {
			return;
		}
		handledRevealSerial = serial;
		query = '';
		const category = paletteContext.categories[categoryIndex];
		openedCategories = [categoryKey(category, categoryIndex)];
		paletteCategoriesTouched = true;
		void tick().then(() => {
			const escaped = typeof CSS !== 'undefined' && CSS.escape ? CSS.escape(key) : key;
			document
				.querySelector(`[data-palette-item-key="${escaped}"]`)
				?.scrollIntoView({ block: 'center', inline: 'nearest' });
		});
	});

	function emptyPaletteContext() {
		return { id: '', fallbackFrom: '', categories: [] };
	}

	/**
	 * @param {string} nextId
	 * @param {number} serial
	 * @param {number} attempt
	 * @param {AbortController} controller
	 */
	async function loadPalette(nextId, serial, attempt = 0, controller) {
		paletteLoading = true;
		paletteError = '';
		try {
			const context = await loadPaletteContext(nextId, undefined, {
				signal: controller.signal,
				timeoutMs: PALETTE_TIMEOUT_MS
			});
			if (serial === paletteLoadSerial) {
				if (!samePaletteContext(paletteContext, context)) {
					paletteContext = context;
					openedCategories = [];
					paletteCategoriesTouched = false;
				}
			}
		} catch (err) {
			if (err instanceof Error && err.name === 'AbortError') {
				return;
			}
			if (
				attempt === 0 &&
				serial === paletteLoadSerial &&
				active &&
				(revealRequest?.contextId || selectedId) === nextId
			) {
				await new Promise((resolve) => setTimeout(resolve, PALETTE_RETRY_DELAY_MS));
				if (
					serial === paletteLoadSerial &&
					active &&
					(revealRequest?.contextId || selectedId) === nextId
				) {
					await loadPalette(nextId, serial, attempt + 1, controller);
					return;
				}
			}
			if (serial === paletteLoadSerial) {
				paletteContext = emptyPaletteContext();
				openedCategories = [];
				paletteCategoriesTouched = false;
				paletteError = String(err instanceof Error ? err.message : err);
			}
		} finally {
			if (serial === paletteLoadSerial) {
				paletteLoading = false;
				if (paletteRequestController === controller) {
					paletteRequestController = null;
				}
			}
		}
	}

	function cancelPaletteRequest() {
		paletteRequestController?.abort();
		paletteRequestController = null;
		paletteLoadSerial += 1;
	}

	function retryPalette() {
		const nextId = active ? revealRequest?.contextId || selectedId : '';
		if (!nextId || paletteLoading) {
			return;
		}
		cancelPaletteRequest();
		paletteRequestId = nextId;
		const serial = ++paletteLoadSerial;
		paletteRequestController = new AbortController();
		void loadPalette(nextId, serial, 0, paletteRequestController);
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

	<div class="studio-palette__content" aria-busy={paletteLoading}>
		{#if paletteLoading && paletteContext.categories.length > 0}
			<div class="studio-palette__pending layout-x-start-low" role="status">
				<span class="studio-spinner" aria-hidden="true"></span>
				<span>Updating palette for {paletteContextLabel(paletteRequestId)}</span>
			</div>
		{/if}
		{#if paletteLoading && paletteContext.categories.length === 0}
			<StudioEmptyState
				message={`Loading palette for ${paletteContextLabel(paletteRequestId)}`}
				loading
			/>
		{:else if paletteError}
			<StudioEmptyState>
				<div class="studio-palette__error layout-y-center-low" role="alert">
					<span>{paletteError}</span>
					<Button
						label="Retry"
						icon="mdi:refresh"
						class="button-secondary"
						full={false}
						onclick={retryPalette}
					/>
				</div>
			</StudioEmptyState>
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
										data-palette-item-key={itemKey(item)}
										aria-pressed={selected}
										title={itemTitle(item)}
										disabled={paletteLoading}
										draggable={paletteLoading ? 'false' : 'true'}
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

	.studio-palette__error {
		max-width: 20rem;
	}

	.studio-palette__pending {
		position: sticky;
		top: 0;
		z-index: 1;
		min-height: 2rem;
		border-bottom: 1px solid color-mix(in oklab, var(--color-primary-500) 22%, transparent);
		background: color-mix(in oklab, var(--color-surface-50-950) 94%, transparent);
		color: var(--color-surface-600-400);
		padding: 0.35rem 0.7rem;
		font-size: 0.72rem;
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

	.studio-palette__item:disabled {
		cursor: wait;
		opacity: 0.55;
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
