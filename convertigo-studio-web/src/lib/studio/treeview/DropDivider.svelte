<script>
	import { reusables } from '$lib/studio/palette/paletteStore';
	import { draggedBlock, draggedData } from '$lib/utils/dndStore';
	import { acceptDbo, addDbo, moveDbo } from '$lib/utils/service';
	import { createEventDispatcher } from 'svelte';

	/** @type {{position: any, nodeData: any}} */
	let { position, nodeData } = $props();

	const dispatch = createEventDispatcher();

	let visible = $state(false);
	let canDrop = false;
	let dragOver = false;
	let dropAction = 'none';

	let dragData = undefined;

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
		let dragAction = e.dataTransfer.types.includes('palettedata') ? 'move' : dropAction;
		canDrop = await allowDrop(dragAction);
		if (canDrop && dragData == undefined) {
			dragData = { action: dragAction, id: $draggedData.data.id };
		}
		visible = dragOver && canDrop ? true : false;
	}

	function handleDragLeave(e) {
		e.preventDefault();
		dragOver = false;
		canDrop = false;
		dragData = undefined;
		visible = false;
	}

	async function handleDragOver(e) {
		if (canDrop) {
			e.preventDefault();
			dropAction = setDropEffect(e);
			let dragAction = e.dataTransfer.types.includes('palettedata') ? 'move' : dropAction;
			if (dragData != undefined) {
				if (dragData.action == dragAction && dragData.id == $draggedData.data.id) {
					return true;
				} else {
					dragData = { action: dragAction, id: $draggedData.data.id };
				}
			}

			setTimeout(() => {
				(async () => {
					let allow = await allowDrop(dragAction);
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
		dragData = undefined;
		let target = nodeData.id;
		let jsonData = undefined;
		try {
			jsonData = JSON.parse(e.dataTransfer.getData('text/plain'));
		} catch (e) {}
		if (target != null && jsonData != undefined) {
			let result = { done: false };
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

<!-- svelte-ignore a11y_no_static_element_interactions -->
<div
	id="dropdivider-{position}-{nodeData.id}"
	class="drop-divider {visible ? 'dropin' : ''}"
	ondragenter={handleDragEnter}
	ondragleave={handleDragLeave}
	ondragover={handleDragOver}
	ondrop={handleDrop}
>
	<span></span>
</div>

<style lang="postcss">
	.drop-divider {
		min-height: 5px;
	}

	.dropin {
		border: dashed 1px;
		height: 30px;
	}
</style>
