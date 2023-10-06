<script>
	import { createEventDispatcher, tick } from 'svelte';
	import { addDbo } from '$lib/utils/service';

	export let kind;
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
		console.log('TODO: dropped in ' + kind + ' ' + nodeData.id);
		/*let jsonData = undefined;
		let target = nodeData.id;
		try {
			jsonData = JSON.parse(e.dataTransfer.getData('text'));
			console.log('handleDrop', jsonData);
		} catch (e) {}
		if (target != null && jsonData != undefined) {
			let result = await addDbo(target, jsonData);
			if (result.done) {
				// update tree item
				dispatch('update', {});
			}
		}*/
	}
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<div
	id="dropdivider-{kind}-{nodeData.id}"
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
