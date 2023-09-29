<script>
	import { Accordion, AccordionItem } from '@skeletonlabs/skeleton';
	import { popup } from '@skeletonlabs/skeleton';
	import { localStorageStore } from '@skeletonlabs/skeleton';
	import { onMount, onDestroy } from 'svelte';
	import { getServiceUrl } from '$lib/convertigo';
	import { categories } from '$lib/paletteStore';
	import { reusables } from '$lib/paletteStore';

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

	const unsubscribe = categories.subscribe((value) => {
		storeCategories = value;
		update();
	});

	onDestroy(unsubscribe);

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

	function dragStart(event, item) {
		const jsonString = JSON.stringify(item);
		event.dataTransfer.setData('text', jsonString);
		//console.log("Palette dragStart", event.dataTransfer.getData("text"))
	}

	function tooltip(id) {
		return {
			event: 'hover',
			target: id,
			placement: 'bottom'
		};
	}

	function itemClicked(item) {
		selectedItem = item;
	}

	function onInputDrop(event) {
		event.preventDefault();
		let json = JSON.parse(event.dataTransfer.getData('text'));
		//console.log('Palette onInputDrop', json);
		$reusables[json.id] = json;
		$reusables = $reusables;
		textInput.value = '';
		update();
	}
</script>

<div class="palette">
	<div class="header">
		<div>
			<button
				type="button"
				class="btn [&>*]:pointer-events-none"
				on:click={handleLink}
				use:popup={tooltip('tooltip-link')}
			>
				<div class="card p-4" data-popup="tooltip-link">
					<p>Link with the project's tree selection</p>
					<div class="arrow" />
				</div>
				{#if linkOn}
					<IconLinkOn />
				{:else}
					<IconLinkOff />
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
					<IconArrangeOn />
				{:else}
					<IconArrangeOff />
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
					<IconDownOn />
				{:else}
					<IconDownOff />
				{/if}
			</button>
			<button
				type="button"
				class="btn [&>*]:pointer-events-none"
				on:click={handleFavorite}
				use:popup={tooltip('tooltip-favorite')}
			>
				<div class="card p-4" data-popup="tooltip-favorite">
					<p>{isFavorite(selectedItem) ? 'Remove from favorites' : 'Add to favorites'}</p>
					<div class="arrow" />
				</div>
				{#if isFavorite(selectedItem)}
					<IconStarOff />
				{:else}
					<IconStarOn />
				{/if}
			</button>
		</div>
		<div>
			<input
				class="input"
				type="search"
				placeholder="Search..."
				bind:value={search}
				on:input={doSearch}
				on:dragStart={(e) => e.preventdefault()}
			/>
			<input
				class="input"
				type="text"
				placeholder="drop inside..."
				bind:this={textInput}
				on:dragStart={(e) => e.preventdefault()}
				on:drop={onInputDrop}
			/>
		</div>
	</div>

	<div class="content">
		<Accordion>
			{#if localCategories.length > 0}
				<AccordionItem open>
					<svelte:fragment slot="summary">Favorites</svelte:fragment>
					<svelte:fragment slot="content">
						<div class="flex-container">
							{#each favoritesItems as item}
								<!-- svelte-ignore a11y-click-events-have-key-events -->
								<!-- svelte-ignore a11y-no-static-element-interactions -->
								<div
									class="palette-item card card-hover {item === selectedItem
										? 'variant-filled'
										: ''}"
									draggable="true"
									on:dragstart={(event) => dragStart(event, item)}
									on:click={(event) => itemClicked(item)}
								>
									{#if item.icon.includes('/')}
										<img
											src={getServiceUrl() +
												'database_objects.GetIconFromPath?iconPath=' +
												item.icon +
												'&__xsrfToken=' +
												encodeURIComponent(localStorage.getItem('x-xsrf-token') ?? '')}
											alt="ico"
										/>
									{/if}
									<span>
										{item.name}
									</span>
								</div>
							{/each}
						</div>
					</svelte:fragment>
				</AccordionItem>
				<AccordionItem open>
					<svelte:fragment slot="summary">Last used</svelte:fragment>
					<svelte:fragment slot="content">
						<div class="flex-container">
							{#each usedItems as item}
								<!-- svelte-ignore a11y-click-events-have-key-events -->
								<!-- svelte-ignore a11y-no-static-element-interactions -->
								<div
									class="palette-item card card-hover {item === selectedItem
										? 'variant-filled'
										: ''}"
									draggable="true"
									on:dragstart={(event) => dragStart(event, item)}
									on:click={(event) => itemClicked(item)}
								>
									{#if item.icon.includes('/')}
										<img
											src={getServiceUrl() +
												'database_objects.GetIconFromPath?iconPath=' +
												item.icon +
												'&__xsrfToken=' +
												encodeURIComponent(localStorage.getItem('x-xsrf-token') ?? '')}
											alt="ico"
										/>
									{/if}
									<span>
										{item.name}
									</span>
								</div>
							{/each}
						</div>
					</svelte:fragment>
				</AccordionItem>
			{/if}
			{#each localCategories as category}
				{#if category.items.length > 0}
					<AccordionItem open>
						<svelte:fragment slot="summary">{category.name}</svelte:fragment>
						<svelte:fragment slot="content">
							<div class="flex-container">
								{#each category.items as item}
									<!-- svelte-ignore a11y-click-events-have-key-events -->
									<!-- svelte-ignore a11y-no-static-element-interactions -->
									<div
										class="palette-item card card-hover {item === selectedItem
											? 'variant-filled'
											: ''}"
										draggable="true"
										on:dragstart={(event) => dragStart(event, item)}
										on:click={(event) => itemClicked(item)}
									>
										{#if item.icon.includes('/')}
											<img
												src={getServiceUrl() +
													'database_objects.GetIconFromPath?iconPath=' +
													item.icon +
													'&__xsrfToken=' +
													encodeURIComponent(localStorage.getItem('x-xsrf-token') ?? '')}
												alt="ico"
											/>
										{/if}
										<span>
											{item.name}
										</span>
									</div>
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

	.flex-container {
		display: flex;
		flex-flow: row wrap;
	}

	.flex-container > .palette-item {
		border-radius: 5px;
		border: 1px solid #f1f1f1;
		width: 100px;
		margin: 5px;
		text-align: center;
		vertical-align: text-top;
		font-size: 10px;
	}
</style>
