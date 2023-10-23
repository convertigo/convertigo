<svelte:options accessors />

<script>
	import { createEventDispatcher } from 'svelte';
	import { reusables } from '$lib/palette/paletteStore';
	import { draggedData, draggedBlock } from '$lib/utils/dndStore';
	import { addDbo, moveDbo, acceptDbo } from '$lib/utils/service';

	const dispatch = createEventDispatcher();

	export let nodeData;
	export let item;
	export let block;
	
	export function dispatchRemove() {
		dispatch('remove', {});
	}
	
	let canDrop = false;
	let dragOver = false;
	let dropAction = 'none';

	async function allowDrop(dragAction) {
		if ($draggedData == undefined || nodeData.id === $draggedData.data.id) {
			return false;
		}
		try {
			let result = await acceptDbo(dragAction, nodeData.id, 'inside', $draggedData);
			return result.accept;
		} catch (e) {
			console.log(e);
		}
		return false;
	}

	function setDropEffect(e) {
		if (e.dataTransfer.effectAllowed === 'copy') {
			e.dataTransfer.dropEffect = 'copy';
		} else {
			e.dataTransfer.dropEffect = true === e.ctrlKey ? 'copy' : 'move';
		}
		return e.dataTransfer.dropEffect;
	}

	function handleDragStart(event) {
		const treeData = { type: 'treeData', data: { id: nodeData.id }, options: {} };
		event.dataTransfer.setData('text/plain', JSON.stringify(treeData));
		event.dataTransfer.setData('treedata', JSON.stringify(treeData));
		$draggedData = treeData;
		$draggedBlock = block;
	}

	async function handleDragEnter(e) {
		e.preventDefault();
		dragOver = true;
		dropAction = setDropEffect(e);
		canDrop = await allowDrop(e.dataTransfer.types.includes('palettedata') ? 'move' : dropAction);
	}

	function handleDragLeave(e) {
		e.preventDefault();
		dragOver = false;
		canDrop = false;
	}

	async function handleDragOver(e) {
		if (canDrop) {
			e.preventDefault();
			dropAction = setDropEffect(e);
			setTimeout(() => {
				(async () => {
					let allow = await allowDrop(
						e.dataTransfer.types.includes('palettedata') ? 'move' : dropAction
					);
					if (dragOver) {
						canDrop = allow;
					}
				})();
			}, 0);
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
			jsonData = JSON.parse(e.dataTransfer.getData('text/plain'));
		} catch (e) {}
		if (target != null && jsonData != undefined) {
			let result = { done: false };
			console.log('handleDrop dndblock', dropAction);
			switch (dropAction) {
				case 'copy':
					result = await addDbo(target, 'inside', jsonData);
					break;
				case 'move':
					result = await moveDbo(target, 'inside', jsonData);
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
				if (jsonData.type === 'treeData' && dropAction === 'move') {
					// update source tree item
					if ($draggedBlock) {
						$draggedBlock.dispatchRemove();
						$draggedBlock = undefined;
					}
				}
				// update target tree item
				dispatch('update', {});
			}
		}
	}

	function handleDragEnd(e) {
		$draggedData = undefined;
	}
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<div
	class="dndblock"
	draggable="true"
	on:dragstart={handleDragStart}
	on:dragend={handleDragEnd}
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
