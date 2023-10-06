<script>
	import { createEventDispatcher } from 'svelte';
	import { addDbo } from '$lib/utils/service';

	export let position;
	export let nodeData;

	const dispatch = createEventDispatcher();

	let visible = false;

	function handleDragOver(e) {
		e.preventDefault();
		return true;
	}

	function handleDragEnter(e) {
		visible = true;
	}

	function handleDragLeave(e) {
		visible = false;
	}

	async function handleDrop(e) {
		e.preventDefault();
		visible = false;
		let target = nodeData.id;
		let jsonData = undefined;
		try {
			jsonData = JSON.parse(e.dataTransfer.getData('text'));
		} catch (e) {}
		if (target != null && jsonData != undefined) {
			let result = await addDbo(target, position, jsonData);
			if (result.done) {
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
