<script>
	import { page } from '$app/state';
	import { call } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import { SvelteFlow, Background, Controls, MiniMap, useSvelteFlow } from '@xyflow/svelte';
	import { writable } from 'svelte/store';
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
</script>

<div style="height:90vh;">
	<SvelteFlow
		{nodes}
		{edges}
		{nodeTypes}
		fitView
		fitViewOptions = {{duration: 200}}
		defaultEdgeOptions={{ type: 'smoothstep', zIndex: 100, animated: true }}
	>
		<Background />
		<Controls />
		<MiniMap />
	</SvelteFlow>
</div>
