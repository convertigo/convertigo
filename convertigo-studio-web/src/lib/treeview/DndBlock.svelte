<script>
	import { createEventDispatcher } from 'svelte';
	import { reusables } from '$lib/palette/paletteStore';
	import { addDbo } from '$lib/utils/service';

	export let nodeData;
	export let item;

	const dispatch = createEventDispatcher();

	function handleDragOver(e) {
		e.preventDefault();
		return false;
	}

	async function handleDrop(e) {
		e.preventDefault();
		let jsonData = undefined;
		let target = nodeData.id;
		try {
			jsonData = JSON.parse(e.dataTransfer.getData('text'));
		} catch (e) {}
		if (target != null && jsonData != undefined) {
			let result = await addDbo(target, jsonData);
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
<span
	draggable="true"
	on:drag={(e) => {
		e.stopPropagation();
	}}
	on:dragstart={(e) => e.stopPropagation()}
	on:dragenter={(e) => {
		//console.log(e);
		if (!nodeData.expanded) {
			item.open = true;
		}
	}}
	on:dragover={handleDragOver}
	on:drop={handleDrop}
>
	<slot name="content" />
</span>
