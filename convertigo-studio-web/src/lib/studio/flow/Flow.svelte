<script>
	import { page } from '$app/stores';
	import { call } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import { SvelteFlow, Background, Controls, MiniMap, Position } from '@xyflow/svelte';
	import { writable } from 'svelte/store';
	import '@xyflow/svelte/dist/style.css';

	let data = $state({});

	onMount(() => {
		call('studio.treeview.Get', { id: `${$page.params.project}.sq:${$page.params.sequence}` }).then(
			(res) => {
				data = res;
				console.log(res);
			}
		);
	});

	const bgColor = writable('#1A192B');

	const initialNodes = [
		{
			id: '1',
			type: 'input',
			data: { label: 'An input node' },
			position: { x: 0, y: 50 },
			sourcePosition: Position.Right
		},
		{
			id: '2',
			type: '',
			data: { color: bgColor },
			style: 'border: 1px solid #777; padding: 10px;',
			position: { x: 300, y: 50 }
		},
		{
			id: '3',
			type: 'output',
			data: { label: 'Output A' },
			position: { x: 650, y: 25 },
			targetPosition: Position.Left
		},
		{
			id: '4',
			type: 'output',
			data: { label: 'Output B' },
			position: { x: 650, y: 100 },
			targetPosition: Position.Left
		}
	];

	const initialEdges = [
		{
			id: 'e1-2',
			source: '1',
			target: '2',
			animated: true,
			style: 'stroke: #fff;'
		},
		{
			id: 'e2a-3',
			source: '2',
			target: '3',
			//sourceHandle: 'a',
			animated: true,
			style: 'stroke: #fff;'
		},
		{
			id: 'e2b-4',
			source: '2',
			target: '4',
			//sourceHandle: 'b',
			animated: true,
			style: 'stroke: #fff;'
		}
	];

	const nodes = writable(initialNodes);
	const edges = writable(initialEdges);
</script>

<div style="height:90vh;">
	<SvelteFlow {nodes} {edges} style="background: {$bgColor}" fitView>
		<Background />
		<Controls />
		<MiniMap />
	</SvelteFlow>
</div>
