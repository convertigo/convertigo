<script>
	import { localStorageStore, ProgressRadial } from '@skeletonlabs/skeleton';
	import { authenticated } from '$lib/utils/loadingStore';
	import { linear } from 'svelte/easing';

	/** @type {{name: any, children?: import('svelte').Snippet}} */
	let { name, children } = $props();
	let dragDiv = $state();
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

<!-- svelte-ignore a11y_no_static_element_interactions -->
<div
	class="preset-soft-primary widthTransition overflow-hidden border-r border-[0.5] border-surface-300 dark:border-surface-800"
	style:width="{$width}px"
	style:min-width="100px"
	ondrag={widthDrag}
	ondragstart={noDragImage}
	transition:withTransition={{ duration: 50 }}
>
	<div class="flex h-full flex-row items-stretch bg-surface-500 dark:bg-surface-800">
		<div
			class="flex grow snap-y snap-mandatory scroll-px-4 flex-col items-stretch overflow-y-auto scroll-smooth bg-surface-900"
		>
			{#if $authenticated}
				{@render children?.()}
			{:else}
				<ProgressRadial ... stroke={100} meter="stroke-primary-500" track="stroke-primary-500/30" />
			{/if}
		</div>
		<span bind:this={dragDiv} class="draggable divider-vertical h-full" draggable="true"></span>
	</div>
</div>

<style>
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
