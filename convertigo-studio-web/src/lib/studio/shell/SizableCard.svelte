<script>
	import { linear } from 'svelte/easing';
	import { localStorageStore } from '@skeletonlabs/skeleton';
	import { authenticated } from '$lib/utils/loadingStore';
	import { ProgressRadial } from '@skeletonlabs/skeleton';
	export let name;
	let dragDiv;
	let width = localStorageStore(`studio.${name}Width`, 400);

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
	class="variant-soft-primary overflow-hidden widthTransition border-[0.5] border-r dark:border-surface-800 border-surface-300"
	style:width="{$width}px"
	style:min-width="100px"
	on:drag={widthDrag}
	on:dragstart={noDragImage}
	transition:withTransition={{ duration: 50 }}
>
	<div class="flex flex-row items-stretch h-full dark:bg-surface-800 bg-surface-500">
		<div
			class="flex-col flex bg-surface-900 items-stretch grow scroll-smooth overflow-y-auto snap-y scroll-px-4 snap-mandatory"
		>
			{#if $authenticated}
				<slot />
			{:else}
				<ProgressRadial ... stroke={100} meter="stroke-primary-500" track="stroke-primary-500/30" />
			{/if}
		</div>
		<span bind:this={dragDiv} class="draggable divider-vertical h-full" draggable="true" />
	</div>
</div>

<style lang="postcss">
	.draggable {
		width: 4px;
		cursor: grab;
		background-color: #a0a0a0;
	}

	.draggable:hover {
		background-color: #2396ff; /* Changez en la couleur bleue souhaitée */
	}

	.widthTransition {
		transition-property: min-width;
		transition-duration: 250ms;
	}
</style>
