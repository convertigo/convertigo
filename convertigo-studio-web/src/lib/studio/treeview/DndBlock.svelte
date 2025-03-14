<!-- <svelte:options accessors /> -->

<script>
	import { reusables } from '$lib/studio/palette/paletteStore';
	import { draggedBlock, draggedData } from '$lib/utils/dndStore';
	import { acceptDbo, addDbo, moveDbo } from '$lib/utils/service';
	import { createEventDispatcher, onDestroy, onMount, tick } from 'svelte';
	import { mountedBlocks } from './treeStore';

	const dispatch = createEventDispatcher();

	/** @type {{nodeData: any, item: any, block: any, icon?: import('svelte').Snippet, label?: import('svelte').Snippet, content?: import('svelte').Snippet}} */
	let { nodeData, item = $bindable(), block, icon, label, content } = $props();

	export function dispatchRemove() {
		dispatch('remove', {});
	}

	export function dispatchDelete() {
		dispatch('delete', {});
	}

	let main = $state();
	let canDrop = false;
	let dragOver = false;
	let dropAction = 'none';

	let dragData = undefined;

	onMount(() => {
		let div = main.closest('.tree-item-content');
		if (div) {
			div.classList.add('w-full');
		}
		init();
	});

	onDestroy(() => {
		let index = $mountedBlocks.indexOf(block);
		if (index > -1) {
			//console.log('destroyed block', block.nodeData.id);
			$mountedBlocks.splice(index, 1);
			$mountedBlocks = $mountedBlocks;
		}
	});

	async function init() {
		await tick();
		if (block != undefined) {
			//console.log('mounted block', block.nodeData.id);
			$mountedBlocks.push(block);
			$mountedBlocks = $mountedBlocks;
		}
	}

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
		let dragAction = e.dataTransfer.types.includes('palettedata') ? 'move' : dropAction;
		canDrop = await allowDrop(dragAction);
		if (canDrop && dragData == undefined) {
			dragData = { action: dragAction, id: $draggedData.data.id };
		}
	}

	function handleDragLeave(e) {
		e.preventDefault();
		dragOver = false;
		canDrop = false;
		dragData = undefined;
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
		canDrop = false;
		dragData = undefined;
		let jsonData = undefined;
		let target = nodeData.id;
		try {
			jsonData = JSON.parse(e.dataTransfer.getData('text/plain'));
		} catch (e) {}
		if (target != null && jsonData != undefined) {
			let result = { done: false };
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
					}
				}
				// update target tree item
				dispatch('update', {});
			}
			$draggedBlock = undefined;
		}
	}

	function handleDragEnd(e) {
		$draggedData = undefined;
	}
</script>

<!-- svelte-ignore a11y_no_static_element_interactions -->
<div
	bind:this={main}
	class="flex items-center"
	draggable="true"
	ondragstart={handleDragStart}
	ondragend={handleDragEnd}
	ondragenter={handleDragEnter}
	ondragleave={handleDragLeave}
	ondragover={handleDragOver}
	ondrop={handleDrop}
>
	<div
		class="flex-none"
		ondragenter={(e) => {
			if (!nodeData.expanded) {
				item.open = true;
			}
		}}
	>
		{@render icon?.()}
	</div>
	<div class="ml-2">{@render label?.()}</div>
	<div class="ml-2 grow">{@render content?.()}</div>
</div>
