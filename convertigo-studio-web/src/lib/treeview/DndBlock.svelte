<script>
	import { createEventDispatcher } from 'svelte';
	import { reusables, draggedItem } from '$lib/palette/paletteStore';
	import { addDbo, acceptDbo } from '$lib/utils/service';

	export let nodeData;
	export let item;

	let canDrop = false;

	const dispatch = createEventDispatcher();

	async function allowDrop() {
		if ($draggedItem == undefined) {
			return false;
		}
		try {
			let result = await acceptDbo(nodeData.id, 'inside', $draggedItem);
			//console.log('acceptDbo for ' + nodeData.id, result);
			return result.accept;
		} catch (e) {
			console.log(e);
		}
		return false;
	}

	async function handleDragEnter(e) {
		if (!nodeData.expanded) {
			item.open = true;
		}
		canDrop = await allowDrop();
	}

	function handleDragLeave(e) {
		canDrop = false;
	}

	function handleDragOver(e) {
		if (canDrop) {
			e.preventDefault();
			return true;
		} else {
			return false;
		}
	}

	async function handleDrop(e) {
		e.preventDefault();
		canDrop = false;
		let jsonData = undefined;
		let target = nodeData.id;
		try {
			jsonData = JSON.parse(e.dataTransfer.getData('text'));
		} catch (e) {}
		if (target != null && jsonData != undefined) {
			let result = await addDbo(target, 'inside', jsonData);
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
	class="dndblock"
	draggable="false"
	on:drag={(e) => e.stopPropagation()}
	on:dragstart={(e) => e.stopPropagation()}
	on:dragenter={handleDragEnter}
	on:dragleave={handleDragLeave}
	on:dragover={handleDragOver}
	on:drop={handleDrop}
>
	<div><slot name="icon" /></div>
	<div class="label"><slot name="label" /></div>
	<div><slot /></div>
</div>

<style>
	.dndblock {
		display: flex;
		flex-direction: row;
		/*border: solid red 1px;*/
	}

	.label {
		margin-left: 10px;
	}
</style>
