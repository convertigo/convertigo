<script>
	import { browser } from '$app/environment';
	import Ico from '$lib/utils/Ico.svelte';
	import { onDestroy, onMount } from 'svelte';
	import PaletteItem from './PaletteItem.svelte';
	import { categories, reusables } from './paletteStore';

	const FAVORITES_KEY = 'palette.favorites';

	let search = $state('');
	let linkOn = $state(true);
	let builtinOn = $state(true);
	let additionalOn = $state(true);
	let selectedItem = $state();

	let localCategories = $state([]);
	let favoritesById = $state(/** @type {Record<string, any>} */ ({}));
	let favoritesLoaded = $state(false);
	const serializedFavorites = $derived(JSON.stringify(favoritesById));

	onMount(() => {
		if (!browser) return;
		try {
			const raw = localStorage.getItem(FAVORITES_KEY);
			if (raw) {
				favoritesById = JSON.parse(raw) ?? {};
			}
		} catch (e) {}
		favoritesLoaded = true;
	});

	$effect(() => {
		if (!browser || !favoritesLoaded) return;
		try {
			localStorage[FAVORITES_KEY] = serializedFavorites;
		} catch (e) {}
	});

	const unsubscribeCategories = categories.subscribe((value) => {
		if (linkOn) {
			localCategories = value ?? [];
		}
	});

	onDestroy(() => unsubscribeCategories());

	const computed = $derived.by(() => {
		const query = search.trim().toLowerCase();
		const currentFavorites = favoritesById ?? {};
		const used = $reusables ?? {};

		/** @type {any[]} */
		const favoritesItems = [];
		/** @type {any[]} */
		const usedItems = [];

		const filteredCategories = (localCategories ?? []).map(({ type, name, items }) => {
			const filteredItems = (items ?? []).filter((item) => {
				const key = `${item.name ?? ''} ${(item.description ?? '').split('|')[0]}`.toLowerCase();
				const found = query ? key.includes(query) : true;
				const visibleType =
					(found && builtinOn && item.builtin) || (found && additionalOn && item.additional);

				if (visibleType) {
					const favItem = currentFavorites[item.id];
					if (favItem) favoritesItems.push(favItem);
					const usedItem = used[item.id];
					if (usedItem) usedItems.push(usedItem);
				}

				return visibleType;
			});

			return { type, name, items: filteredItems };
		});

		return {
			categories: filteredCategories.filter((c) => c.items?.length),
			favoritesItems,
			usedItems
		};
	});

	function toggleFavorite() {
		if (!selectedItem?.id) return;
		const next = { ...(favoritesById ?? {}) };
		if (next[selectedItem.id]) {
			delete next[selectedItem.id];
		} else {
			next[selectedItem.id] = { ...selectedItem };
		}
		favoritesById = next;
	}

	function isFavorite(item) {
		return item?.id ? favoritesById?.[item.id] != null : false;
	}

	function itemClicked(event) {
		selectedItem = event.detail.item;
	}
</script>

<div class="palette">
	<div class="palette__header">
		<div class="palette__actions">
			<button
				type="button"
				class="button-ico-secondary"
				aria-pressed={linkOn}
				title="Link with the tree selection"
				onclick={() => {
					linkOn = !linkOn;
					if (linkOn) localCategories = $categories ?? [];
				}}
			>
				<Ico icon="mdi:smartphone-link" />
			</button>
			<button
				type="button"
				class="button-ico-secondary"
				aria-pressed={builtinOn}
				title="Built-in objects visibility"
				onclick={() => (builtinOn = !builtinOn)}
			>
				<Ico icon="mdi:layers-outline" />
			</button>
			<button
				type="button"
				class="button-ico-secondary"
				aria-pressed={additionalOn}
				title="Shared objects visibility"
				onclick={() => (additionalOn = !additionalOn)}
			>
				<Ico icon="mdi:download-off-outline" />
			</button>
			<button
				type="button"
				class="button-ico-secondary"
				disabled={!selectedItem}
				title={isFavorite(selectedItem) ? 'Remove from favorites' : 'Add to favorites'}
				onclick={toggleFavorite}
			>
				<Ico
					icon={isFavorite(selectedItem) ? 'mdi:star-three-points-outline' : 'mdi:star-outline'}
				/>
			</button>
		</div>

		<input
			class="palette__search input"
			type="search"
			placeholder="Search…"
			autocomplete="off"
			aria-label="Search palette"
			bind:value={search}
		/>
	</div>

	<div class="palette__content">
		<details class="section" open>
			<summary class="section__summary">Favorites</summary>
			<div class="items">
				{#each computed.favoritesItems as item (item.id)}
					<PaletteItem {item} on:itemClicked={itemClicked} />
				{/each}
			</div>
		</details>

		<details class="section" open>
			<summary class="section__summary">Last used</summary>
			<div class="items">
				{#each computed.usedItems as item (item.id)}
					<PaletteItem {item} on:itemClicked={itemClicked} />
				{/each}
			</div>
		</details>

		{#each computed.categories as cat (`${cat.type ?? ''}:${cat.name ?? ''}`)}
			<details class="section" open>
				<summary class="section__summary">{cat.name}</summary>
				<div class="items">
					{#each cat.items as item (item.id)}
						<PaletteItem {item} on:itemClicked={itemClicked} />
					{/each}
				</div>
			</details>
		{/each}
	</div>
</div>

<style>
	.palette {
		height: 100%;
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}
	.palette__header {
		padding: 10px;
		border-bottom: 1px solid color-mix(in oklab, var(--color-surface-900, #0f172a) 10%, transparent);
		display: grid;
		gap: 8px;
	}
	.palette__actions {
		display: flex;
		gap: 6px;
		align-items: center;
		justify-content: center;
		flex-wrap: wrap;
	}
	.palette__search {
		width: 100%;
	}
	.palette__content {
		flex: 1;
		min-height: 0;
		overflow: auto;
		padding: 8px;
	}
	.section {
		border: 1px solid color-mix(in oklab, var(--color-surface-900, #0f172a) 10%, transparent);
		border-radius: 10px;
		background: color-mix(in oklab, var(--color-surface-50, #f8fafc) 92%, transparent);
		overflow: hidden;
	}
	:global(.dark) .section {
		border-color: color-mix(in oklab, var(--color-surface-50, #f8fafc) 14%, transparent);
		background: color-mix(in oklab, var(--color-surface-900, #0f172a) 65%, transparent);
	}
	.section + .section {
		margin-top: 8px;
	}
	.section__summary {
		cursor: pointer;
		list-style: none;
		padding: 8px 10px;
		font-size: 12px;
		font-weight: 600;
		color: var(--color-surface-800, #1e293b);
		background: color-mix(in oklab, var(--color-surface-50, #f8fafc) 80%, transparent);
		border-bottom: 1px solid color-mix(in oklab, var(--color-surface-900, #0f172a) 8%, transparent);
	}
	:global(.dark) .section__summary {
		color: var(--color-surface-100, #f1f5f9);
		background: color-mix(in oklab, var(--color-surface-900, #0f172a) 75%, transparent);
		border-color: color-mix(in oklab, var(--color-surface-50, #f8fafc) 14%, transparent);
	}
	.items {
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(92px, 1fr));
		gap: 6px;
		padding: 8px;
	}
</style>
