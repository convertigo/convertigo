<script>
	import { createEventDispatcher } from 'svelte';
	import { reusables, draggedItem } from '$lib/palette/paletteStore';
	import { addDbo, acceptDbo } from '$lib/utils/service';

	export let nodeData;
	export let item;

	let canDrop = false;
	let action = 'none';

	const dispatch = createEventDispatcher();

	async function allowDrop() {
		if ($draggedItem == undefined) {
			return false;
		}
		try {
			let result = await acceptDbo(nodeData.id, 'inside', $draggedItem);
			return result.accept;
		} catch (e) {
			console.log(e);
		}
		return false;
	}

	async function handleDragEnter(e) {
		canDrop = await allowDrop();
	}

	function handleDragLeave(e) {
		canDrop = false;
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

	async function handleDrop(e) {
		e.preventDefault();
		canDrop = false;
		let jsonData = undefined;
		let target = nodeData.id;
		try {
			jsonData = JSON.parse(e.dataTransfer.getData('text/plain'));
		} catch (e) {}
		if (target != null && jsonData != undefined) {
			let result = { done: false };
			switch (action) {
				case 'copy':
					result = await addDbo(target, 'inside', jsonData);
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

	function handleDragStart(event) {
		const treeData = { type: 'treeData', data: { id: nodeData.id }, options: {} };
		event.dataTransfer.setData('text/plain', JSON.stringify(treeData));
		$draggedItem = treeData;
	}
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<div
	class="dndblock"
	draggable="true"
	on:dragstart={handleDragStart}
	on:dragend={(event) => ($draggedItem = undefined)}
	on:dragenter={handleDragEnter}
	on:dragleave={handleDragLeave}
	on:dragover={handleDragOver}
	on:drop={handleDrop}
>
	<div
		on:dragenter={(e) => {
			if (!nodeData.expanded) {
				item.open = true;
			}
		}}
	>
		<slot name="icon" />
	</div>
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
