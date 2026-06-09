<script>
	import InputGroup from '$lib/common/components/InputGroup.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import AutoSvg from '$lib/utils/AutoSvg.svelte';
	import { draggedData } from '$lib/utils/dndStore';
	import Ico from '$lib/utils/Ico.svelte';
	import { getUrl } from '$lib/utils/service';
	import { objectNameFromId } from './dnd';
	import { loadPaletteContext } from './paletteContext';

	/**
	 * @typedef {Object} PaletteItem
	 * @property {string=} id
	 * @property {string=} name
	 * @property {string=} classname
	 * @property {string=} description
	 * @property {string=} shortDescriptionText
	 * @property {string=} icon
	 * @property {boolean=} builtin
	 * @property {boolean=} additional
	 */
	/**
	 * @typedef {Object} PaletteCategory
	 * @property {string=} name
	 * @property {PaletteItem[]=} items
	 */

	/** @type {{ selectedId?: string, active?: boolean }} */
	let { selectedId = '', active = true } = $props();

	let query = $state('');
	/** @type {{ id: string, fallbackFrom: string, categories: PaletteCategory[] }} */
	let paletteContext = $state(emptyPaletteContext());
	let paletteLoading = $state(false);
	let paletteError = $state('');
	let paletteRequestId = '';
	let paletteLoadSerial = 0;
	let filteredCategories = $derived(filterCategories(paletteContext.categories));

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
				paletteContext = context;
			}
		} catch (err) {
			if (serial === paletteLoadSerial) {
				paletteContext = emptyPaletteContext();
				paletteError = String(err instanceof Error ? err.message : err);
			}
		} finally {
			if (serial === paletteLoadSerial) {
				paletteLoading = false;
			}
		}
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

	function iconSource(icon) {
		if (!icon) {
			return '';
		}
		return `${getUrl()}studio.dbo.GetIcon?iconPath=${encodeURIComponent(icon)}`;
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
		{#if paletteLoading}
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
			{#if paletteContext.fallbackFrom}
				<div class="studio-palette__fallback">
					<Ico icon="mdi:subdirectory-arrow-left" size={3.2} />
					<span>Palette from {objectNameFromId(paletteContext.id)}</span>
				</div>
			{/if}
			{#each filteredCategories as category, index (`${category.name ?? ''}:${index}`)}
				<details class="studio-palette__category" open={index < 3 || Boolean(query)}>
					<summary class="studio-palette__category-title">
						<span>{category.name}</span>
						<span>{category.items?.length ?? 0}</span>
					</summary>
					<div class="studio-palette__items">
						{#each category.items ?? [] as item (item.id ?? item.classname ?? item.name)}
							<button
								type="button"
								class="studio-palette__item"
								title={item.classname ?? item.name}
								draggable="true"
								ondragstart={(event) => onDragStart(event, item)}
								ondragend={() => ($draggedData = undefined)}
							>
								<span class="studio-palette__icon">
									{#if iconSource(item.icon)}
										<AutoSvg src={iconSource(item.icon)} alt="" class="h-5 w-5 object-contain" />
									{:else}
										<Ico icon="mdi:cube-outline" size={4} />
									{/if}
								</span>
								<span class="studio-palette__item-main">
									<span class="studio-palette__item-name">{item.name}</span>
									<span class="studio-palette__item-class">{item.classname}</span>
								</span>
							</button>
						{/each}
					</div>
				</details>
			{/each}
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
		padding: 0.55rem;
	}

	.studio-palette__content {
		min-height: 0;
		flex: 1;
		overflow: auto;
		padding: 0.55rem;
	}

	.studio-palette__category {
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.4rem;
		background: color-mix(in oklab, var(--color-surface-100-900) 55%, transparent);
		overflow: hidden;
	}

	.studio-palette__fallback {
		display: flex;
		align-items: center;
		gap: 0.35rem;
		margin-bottom: 0.45rem;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.35rem;
		background: color-mix(in oklab, var(--color-primary-500) 9%, transparent);
		color: var(--color-surface-700-300);
		padding: 0.34rem 0.45rem;
		font-size: 0.68rem;
		font-weight: 650;
	}

	.studio-palette__fallback span {
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-palette__category + .studio-palette__category {
		margin-top: 0.45rem;
	}

	.studio-palette__category-title {
		display: flex;
		cursor: pointer;
		align-items: center;
		justify-content: space-between;
		gap: 0.5rem;
		padding: 0.45rem 0.55rem;
		font-size: 0.75rem;
		font-weight: 700;
	}

	.studio-palette__category-title span:last-child {
		border: 1px solid var(--color-surface-200-800);
		border-radius: 999px;
		padding: 0.14rem 0.42rem;
		color: var(--color-surface-600-400);
		font-size: 0.68rem;
		line-height: 1;
	}

	.studio-palette__items {
		display: grid;
		gap: 0.3rem;
		border-top: 1px solid var(--color-surface-200-800);
		padding: 0.35rem;
	}

	.studio-palette__item {
		display: grid;
		grid-template-columns: auto minmax(0, 1fr);
		align-items: center;
		gap: 0.45rem;
		width: 100%;
		border: 1px solid transparent;
		border-radius: 0.35rem;
		background: transparent;
		color: var(--color-surface-900-100);
		padding: 0.38rem;
		text-align: left;
	}

	.studio-palette__item:hover {
		border-color: color-mix(in oklab, var(--color-primary-500) 35%, transparent);
		background: color-mix(in oklab, var(--color-primary-500) 9%, transparent);
	}

	.studio-palette__icon {
		display: grid;
		width: 1.55rem;
		height: 1.55rem;
		place-items: center;
		color: var(--color-primary-600-400);
	}

	.studio-palette__item-main {
		min-width: 0;
	}

	.studio-palette__item-name,
	.studio-palette__item-class {
		display: block;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-palette__item-name {
		font-size: 0.76rem;
		font-weight: 650;
	}

	.studio-palette__item-class {
		margin-top: 0.08rem;
		color: var(--color-surface-600-400);
		font-size: 0.65rem;
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
