<script context="module">
	//data shared accross each instance of this component
	let selectedItem;
</script>

<script>
	import { createEventDispatcher } from 'svelte';
	import { onMount, onDestroy } from 'svelte';
	import { getUrl } from '$lib/utils/service';

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
<div
	class="palette-item card card-hover {item === selected ? 'variant-filled' : ''}"
	draggable="true"
	on:dragstart={(event) => dragStart(event, item)}
	on:click={(event) => onClick()}
>
	{#if item.icon.includes('/')}
		<img src={`${getUrl()}studio.dbo.GetIcon?iconPath=${item.icon}`} alt="ico" />
	{/if}
	<span>
		{item.name}
	</span>
</div>

<style>
	.palette-item {
		border-radius: 5px;
		border: 1px solid #f1f1f1;
		width: 100px;
		margin: 5px;
		text-align: center;
		vertical-align: text-top;
		font-size: 10px;
	}
</style>
