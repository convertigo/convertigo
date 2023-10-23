<script>
	import { createEventDispatcher } from 'svelte';
	import { reusables } from '$lib/palette/paletteStore';
	import { draggedData, draggedBlock } from '$lib/utils/dndStore';
	import { addDbo, moveDbo, acceptDbo } from '$lib/utils/service';

	export let position;
	export let nodeData;

	const dispatch = createEventDispatcher();

	let visible = false;
	let canDrop = false;
	let dragOver = false;
	let dropAction = 'none';

	async function allowDrop(dragAction) {
		if ($draggedData == undefined) {
			return false;
		}
		try {
			let result = await acceptDbo(dragAction, nodeData.id, position, $draggedData);
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

	async function handleDragEnter(e) {
		e.preventDefault();
		dragOver = true;
		dropAction = setDropEffect(e);
		canDrop = await allowDrop(e.dataTransfer.types.includes('palettedata') ? 'move' : dropAction);
		visible = dragOver && canDrop ? true : false;
	}

	function handleDragLeave(e) {
		e.preventDefault();
		dragOver = false;
		canDrop = false;
		visible = false;
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
		visible = false;
		canDrop = false;
		let target = nodeData.id;
		let jsonData = undefined;
		try {
			jsonData = JSON.parse(e.dataTransfer.getData('text/plain'));
		} catch (e) {}
		if (target != null && jsonData != undefined) {
			let result = { done: false };
			console.log('handleDrop divider', dropAction);
			switch (dropAction) {
				case 'copy':
					result = await addDbo(target, position, jsonData);
					break;
				case 'move':
					result = await moveDbo(target, position, jsonData);
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
