<script>
	import { linear } from 'svelte/easing';
	import { localStorageStore } from '@skeletonlabs/skeleton';
	import { authenticated } from '$lib/utils/loadingStore';
	import { ProgressRadial } from '@skeletonlabs/skeleton';
	export let name;
	let dragDiv;
	let width = localStorageStore(`studio.${name}Width`, 300);

	let img;
	function noDragImage(e) {
		if (e.target == dragDiv) {
			e.target.parentElement.parentElement.classList.remove('widthTransition');
			if (!img) {
				img = document.createElement('img');
				img.src = 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7';
			}
			e.dataTransfer.setDragImage(img, 0, 0);
		}
	}

	function widthDrag(e) {
		if (e.target == dragDiv && e.layerX > 0) {
			$width = e.x - (e.target?.parentElement?.offsetLeft ?? 0);
		}
	}

	/**
	 * @param {HTMLDivElement} node
	 */
	function withTransition(node, { duration }) {
		return {
			duration,
			css: (/** @type {any} */ t) => {
				let l = Math.round(linear(t) * node.clientWidth);
				return `
					width: ${l}px;
					min-width: ${l}px;
					opacity: ${t};
				`;
			}
		};
	}
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<div
	class="card m-1 variant-soft-primary overflow-hidden widthTransition"
	style:width="{$width}px"
	style:min-width="100px"
	on:drag={widthDrag}
	on:dragstart={noDragImage}
	transition:withTransition={{ duration: 250 }}
>
	<div class="flex flex-row items-stretch h-full">
		<div
			class="flex-col flex items-stretch grow scroll-smooth overflow-y-auto snap-y scroll-px-4 snap-mandatory"
		>
			{#if $authenticated}
				<slot />
			{:else}
				<ProgressRadial ... stroke={100} meter="stroke-primary-500" track="stroke-primary-500/30" />
			{/if}
		</div>
		<span bind:this={dragDiv} class="draggable divider-vertical h-full border-2" draggable="true" />
	</div>
</div>

<style>
	.draggable {
		cursor: grab;
	}

	.card:active {
		cursor: grabbing;
	}

	.widthTransition {
		transition-property: min-width;
		transition-duration: 250ms;
	}
</style>
