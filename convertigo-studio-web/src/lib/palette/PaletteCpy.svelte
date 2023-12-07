<script>
	import {
		Accordion,
		AccordionItem,
		popup,
		localStorageStore,
		RadioGroup,
		RadioItem
	} from '@skeletonlabs/skeleton';
	import { onMount, onDestroy } from 'svelte';
	import { categories } from '$lib/palette/paletteStore';
	import { reusables } from '$lib/palette/paletteStore';
	import PaletteItem from './PaletteItemCpy.svelte';

	// @ts-ignore
	import IconLinkOn from '~icons/mdi/arrow-left-right-bold';
	// @ts-ignore
	import IconLinkOff from '~icons/mdi/arrow-left-right-bold-outline';
	// @ts-ignore
	import IconArrangeOn from '~icons/mdi/arrange-bring-forward';
	// @ts-ignore
	import IconArrangeOff from '~icons/mdi/arrange-send-backward';
	// @ts-ignore
	import IconDownOn from '~icons/mdi/chevron-down-box';
	// @ts-ignore
	import IconDownOff from '~icons/mdi/chevron-down-box-outline';
	// @ts-ignore
	import IconStarOn from '~icons/mdi/star';
	// @ts-ignore
	import IconStarOff from '~icons/mdi/star-outline';
	import Icon from '@iconify/svelte';

	let favorites = localStorageStore('palette.favorites', {});
	let storeCategories = [];
	let localCategories = [];
	let favoritesItems = [];
	let usedItems = [];
	let selectedItem;

	let search = '';
	let textInput;

	let linkOn = true;
	let builtinOn = true;
	let additionalOn = true;

	onMount(() => {});

	const unsubscribeFavorites = favorites.subscribe(() => update());

	const unsubscribeCategories = categories.subscribe((value) => {
		storeCategories = value;
		update();
	});

	const unsubscribeReusables = reusables.subscribe((value) => {
		update();
	});

	onDestroy(() => {
		unsubscribeFavorites;
		unsubscribeCategories;
		unsubscribeReusables;
	});

	function update() {
		selectedItem = undefined;
		favoritesItems = [];
		usedItems = [];

		// link to selection in tree: get categories from store
		if (linkOn) {
			localCategories = storeCategories;
		}

		// localCategories filtered on button state
		let filtered = localCategories.map(({ type, name, items }) => {
			return {
				type,
				name,
				items: items.filter((item) => {
					let key = item.name.toLowerCase() + ' ' + item.description.toLowerCase().split('|')[0];
					let found = search.trim() !== '' ? key.indexOf(search.toLowerCase()) != -1 : true;
					let ret =
						found &&
						((builtinOn && item.builtin === builtinOn) ||
							(additionalOn && item.additional === additionalOn));

					// favoritesItems & usedItems update
					if (ret) {
						let favItem = $favorites[item.id];
						if (favItem != undefined && favItem.id == item.id) {
							favoritesItems.push(favItem);
						}
						let useItem = $reusables[item.id];
						if (useItem != undefined && useItem.id == item.id) {
							usedItems.push(useItem);
						}
					}
					return ret;
				})
			};
		});
		localCategories = filtered;

		//console.log('localCategories', localCategories);
		//console.log('favoritesItems', favoritesItems);
		//console.log('usedItems', usedItems);
	}

	function handleLink(e) {
		linkOn = !linkOn;
		update();
	}

	function handleBuiltin(e) {
		builtinOn = !builtinOn;
		update();
	}

	function handleAdditional(e) {
		additionalOn = !additionalOn;
		update();
	}

	function handleFavorite(e) {
		if (selectedItem != undefined) {
			if (isFavorite(selectedItem)) {
				delete $favorites[selectedItem.id];
				$favorites = $favorites;
			} else {
				$favorites[selectedItem.id] = { ...selectedItem };
			}
			update();
		}
	}

	function isFavorite(item) {
		return item != undefined ? $favorites[item.id] != undefined : false;
	}

	function doSearch() {
		update();
	}

	function tooltip(id) {
		return {
			event: 'hover',
			target: id,
			placement: 'bottom'
		};
	}

	function itemClicked(event) {
		selectedItem = event.detail.item;
	}

	let value = 0;
</script>

<div class="palette bg-surface-800">
	<div class="header">
		<div class="flex flex-row gap-5">
			<button
				type="button"
				class="btn [&>*]:pointer-events-none"
				on:click={handleLink}
				use:popup={tooltip('tooltip-link')}
			>
				<div class="card p-4" data-popup="tooltip-link">
					<p>Link with the project's tree selection 2</p>
					<div class="arrow" />
				</div>
				{#if linkOn}
					<IconLinkOn class="xl" />
				{:else}
					<IconLinkOff class="xl" />
				{/if}
			</button>

			<button
				type="button"
				class="btn [&>*]:pointer-events-none"
				on:click={handleBuiltin}
				use:popup={tooltip('tooltip-builtin')}
			>
				<div class="card p-4" data-popup="tooltip-builtin">
					<p>Built-in objects visibility</p>
					<div class="arrow" />
				</div>
				{#if builtinOn}
					<IconArrangeOn class="xl" />
				{:else}
					<IconArrangeOff class="xl" />
				{/if}
			</button>

			<button
				type="button"
				class="btn [&>*]:pointer-events-none"
				on:click={handleAdditional}
				use:popup={tooltip('tooltip-additional')}
			>
				<div class="card p-4" data-popup="tooltip-additional">
					<p>Shared objects visibility</p>
					<div class="arrow" />
				</div>
				{#if additionalOn}
					<IconDownOn class="xl" />
				{:else}
					<IconDownOff class="xl" />
				{/if}
			</button>

			<button
				type="button"
				class="btn [&>*]:pointer-events-none"
				on:click={handleFavorite}
				use:popup={tooltip('tooltip-favorite')}
			>
				<div class="card p-4" data-popup="tooltip-favorite">
					<p class="text-white">
						{isFavorite(selectedItem) ? 'Remove from favorites' : 'Add to favorites'}
					</p>
					<div class="arrow" />
				</div>
				{#if isFavorite(selectedItem)}
					<IconStarOff class="xl" />
				{:else}
					<Icon icon="fluent-mdl2:add-favorite" />
				{/if}
			</button>
		</div>

		<div class="mt-10 w-[70%]">
			<input
				id="inputSearch"
				class="input searchbar"
				type="search"
				placeholder="Search..."
				bind:value={search}
				on:input={doSearch}
			/>
		</div>
	</div>

	<div class="content">
		<Accordion>
			{#if localCategories.length > 0}
				<AccordionItem open>
					<svelte:fragment slot="summary">
						<span class="font-extralight">Favorites</span>
					</svelte:fragment>
					<svelte:fragment slot="content">
						<div class="items-container bg-surface-800">
							{#each favoritesItems as item}
								<PaletteItem {item} on:itemClicked={itemClicked} />
							{/each}
						</div>
					</svelte:fragment>
				</AccordionItem>
				<AccordionItem open>
					<svelte:fragment slot="summary">
						<span class="font-extralight"> Last used </span>
					</svelte:fragment>
					<svelte:fragment slot="content">
						<div class="items-container bg-surface-800">
							{#each usedItems as item}
								<PaletteItem {item} on:itemClicked={itemClicked} />
							{/each}
						</div>
					</svelte:fragment>
				</AccordionItem>
			{/if}
			{#each localCategories as category}
				{#if category.items.length > 0}
					<AccordionItem open>
						<svelte:fragment slot="summary">
							<span class="font-extralight">
								{category.name}
							</span>
						</svelte:fragment>
						<svelte:fragment slot="content">
							<div class="items-container bg-surface-800 p-4">
								{#each category.items as item}
									<PaletteItem {item} on:itemClicked={itemClicked} />
								{/each}
							</div>
						</svelte:fragment>
					</AccordionItem>
				{/if}
			{/each}
		</Accordion>
	</div>
</div>

<style>
	.palette {
		display: flex;
		flex-direction: column;
	}

	.header {
		display: flex;
		flex-flow: row wrap;
		margin: 10px;
	}

	.content {
		margin-top: 5px;
	}

	.items-container {
		display: flex;
		flex-flow: row wrap;
	}

	.searchbar {
		padding: 4px;
	}
</style>
