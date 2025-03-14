<script>
	import { Background, Controls, MiniMap, SvelteFlow, useSvelteFlow } from '@xyflow/svelte';
	import { page } from '$app/state';
	import { call } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import { writable } from 'svelte/store';
	import ContextMenu from './ContextMenu.svelte';
	import { createNodesAndEdges, getLayoutedElements } from './FlowUtils.js';
	import StepNode from './StepNode.svelte';
	import '@xyflow/svelte/dist/style.css';

	const { fitView } = useSvelteFlow();

	const nodes = writable([]);
	const edges = writable([]);

	const nodeTypes = {
		'step-node': StepNode
	};

	onMount(() => {
		try {
			call('studio.treeview.Get', {
				id: `${page.params.project}.sq:${page.params.sequence}`,
				flow: true
			}).then((res) => {
				console.log(res);
				const ne = createNodesAndEdges(res);
				console.log(ne);

				$nodes = ne.nodes;
				$edges = ne.edges;

				onLayout('DOWN');
			});
		} catch (error) {
			console.log('Error', error);
		}
	});

	function onLayout(direction) {
		const opts = { 'elk.direction': direction };
		getLayoutedElements($nodes, $edges, opts)
			.then((layouted) => {
				$nodes = layouted.nodes;
				$edges = layouted.edges;
			})
			.then(() => {
				fitView();
				//window.requestAnimationFrame(() => fitView());
			});
	}

	//menu: { id: string; top?: number; left?: number; right?: number; bottom?: number } | null
	let menu;
	let width;
	let height;

	function handleContextMenu({ detail: { event, node } }) {
		// Prevent native context menu from showing
		event.preventDefault();

		// Calculate position of the context menu. We want to make sure it
		// doesn't get positioned off-screen.
		menu = {
			id: node.id,
			top: event.clientY < height - 200 ? event.clientY : undefined,
			left: event.clientX < width - 200 ? event.clientX : undefined,
			right: event.clientX >= width - 200 ? width - event.clientX : undefined,
			bottom: event.clientY >= height - 200 ? height - event.clientY : undefined
		};
	}

	// Close the context menu if it's open whenever the window is clicked.
	function handlePaneClick() {
		menu = null;
	}
</script>

<div style="height:90vh;" bind:clientWidth={width} bind:clientHeight={height}>
	<SvelteFlow
		{nodes}
		{edges}
		{nodeTypes}
		fitView
		fitViewOptions={{ duration: 200 }}
		defaultEdgeOptions={{ type: 'smoothstep', zIndex: 100, animated: true }}
		on:nodecontextmenu={handleContextMenu}
		on:paneclick={handlePaneClick}
	>
		{#if menu}
			<ContextMenu
				onClick={handlePaneClick}
				id={menu.id}
				top={menu.top}
				left={menu.left}
				right={menu.right}
				bottom={menu.bottom}
			/>
		{/if}
		<Background />
		<Controls />
		<MiniMap />
	</SvelteFlow>
</div>
