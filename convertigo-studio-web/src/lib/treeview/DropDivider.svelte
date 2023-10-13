<script>
	import { createEventDispatcher, tick } from 'svelte';
	import { reusables, draggedItem } from '$lib/palette/paletteStore';
	import { addDbo, acceptDbo } from '$lib/utils/service';

	export let position;
	export let nodeData;

	const dispatch = createEventDispatcher();

	let visible = false;
	let canDrop = false;
	let dragOver = false;
	let action = 'none';

	async function allowDrop() {
		if ($draggedItem == undefined) {
			return false;
		}
		try {
			let result = await acceptDbo(nodeData.id, position, $draggedItem);
			return result.accept;
		} catch (e) {
			console.log(e);
		}
		return false;
	}

	function handleDragOver(e) {
		if (canDrop) {
			e.preventDefault();
			if (e.dataTransfer.effectAllowed === 'copy') {
				e.dataTransfer.dropEffect = 'copy';
			} else {
				e.dataTransfer.dropEffect = true === e.ctrlKey ? 'copy' : 'move';
			}
			action = e.dataTransfer.dropEffect;
			return true;
		} else {
			action = 'none';
			return false;
		}
	}

	async function handleDragEnter(e) {
		dragOver = true;
		canDrop = await allowDrop();
		visible = dragOver && canDrop ? true : false;
	}

	function handleDragLeave(e) {
		dragOver = false;
		canDrop = false;
		visible = false;
	}

	async function handleDrop(e) {
		e.preventDefault();
		visible = false;
		canDrop = false;
		let target = nodeData.id;
		let jsonData = undefined;
		try {
			jsonData = JSON.parse(e.dataTransfer.getData('text/plain'));
		} catch (e) {}
		if (target != null && jsonData != undefined) {
			let result = { done: false };
			switch (action) {
				case 'copy':
					result = await addDbo(target, position, jsonData);
					break;
				case 'move':
					//result = await moveDbo(target, position, jsonData);
					console.log("handleDrop: moveDbo not yet implemented")
					break;
				default:
					break;
			}
			if (result.done) {
				// update palette reusables
				if (jsonData.type === 'paletteData') {
					$reusables[jsonData.data.id] = jsonData.data;
					$reusables = $reusables;
				}
				// update tree item
				dispatch('update', {});
			}
		}
	}
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<div
	id="dropdivider-{position}-{nodeData.id}"
	class="drop-divider {visible ? 'dropin' : ''}"
	on:dragenter={handleDragEnter}
	on:dragleave={handleDragLeave}
	on:dragover={handleDragOver}
	on:drop={handleDrop}
>
	<span />
</div>

<style>
	.drop-divider {
		min-height: 5px;
	}

	.dropin {
		border: dashed 1px;
		height: 30px;
	}
</style>
