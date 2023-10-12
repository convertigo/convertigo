<script context="module">
	//data shared accross each instance of this component
	let selectedItem;
</script>

<script>
	import { createEventDispatcher } from 'svelte';
	import { onMount, onDestroy } from 'svelte';
	import { getUrl } from '$lib/utils/service';
	import { draggedItem } from './paletteStore';

	const dispatch = createEventDispatcher();

	export let item;

	let selected;
	let interval;

	//read the data on interval
	onMount(() => {
		interval = setInterval(() => (selected = selectedItem), 100);
	});

	//destroy the timer
	onDestroy(() => clearInterval(interval));

	function dragStart(event, item) {
		const paletteData = { type: 'paletteData', data: item, options: {} };
		event.dataTransfer.setData('text', JSON.stringify(paletteData));
		$draggedItem = paletteData;
	}

	function onClick() {
		selectedItem = item;
		selected = selectedItem;
		dispatch('itemClicked', {
			item: item
		});
	}
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<span
	class="chip text-left m-1 w-36 justify-start hover:shadow-lg hover:variant-filled-primary {item ===
	selected
		? 'variant-filled-primary'
		: 'variant-soft-primary'}"
	draggable="true"
	on:dragstart={(event) => dragStart(event, item)}
	on:dragend={(event) => $draggedItem = undefined}
	on:click={(event) => onClick()}
>
	{#if item.icon.includes('/')}
		<span>
			<img src={`${getUrl()}studio.dbo.GetIcon?iconPath=${item.icon}`} alt="ico" />
		</span>
	{/if}
	<span class="whitespace-normal">
		{item.name}
	</span>
</span>
