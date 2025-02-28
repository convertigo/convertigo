<script>
	import { onMount } from 'svelte';
	import { call, getUrl } from '$lib/utils/service';
	import AutoSvg from '$lib/utils/AutoSvg.svelte';
	import { Position, Handle, useSvelteFlow, useNodes, useEdges } from '@xyflow/svelte';
	import {
		createEdge,
		createNodesAndEdges,
		getLayoutedElements,
		isOutsideContainer
	} from './FlowUtils';

	const { fitView } = useSvelteFlow();

	let { id, data, targetPosition, sourcePosition, ...rest } = $props();

	const nodes = useNodes();
	const edges = useEdges();

	let done = $state(false);
	let expanded = $state(false);

	onMount(() => {
		const children = $nodes.filter(
			(node) => id === node?.parentId || id === node?.data?.parentDboId
		);
		done = data.hasChildren && children.length > 0;

		const visibleChildren = children.filter((node) => node?.hidden != true);
		expanded = visibleChildren.length > 0;
	});

	function updateEdges(newNodes = []) {
		if (isOutsideContainer(data.classname)) {
			const currentNode = $nodes.filter((node) => id === node.id)[0];
			const browsers = $nodes.filter(
				(node) => currentNode.data.parentDboId === node.data.parentDboId
			);
			//console.log("browsers", $state.snapshot(browsers))
			const index = browsers.indexOf(currentNode);
			if (index > 0 && index < browsers.length - 1) {
				const nextNode = browsers[index + 1];
				$edges.forEach((edge) => {
					if (currentNode.id === edge.source && nextNode.id === edge.target) {
						edge.hidden = !expanded;
					}
				});
				newNodes.forEach((node) => {
					$edges.push(createEdge(node.id, nextNode.id));
				});
			}
		}
	}

	function showhideChildren(nodeId) {
		const children = $nodes.filter(
			(node) => nodeId === node?.parentId || nodeId === node?.data?.parentDboId
		);
		if (children.length > 0) {
			children.forEach((child) => {
				child.hidden = expanded;
				$edges.forEach((edge) => {
					if (child.id === edge.source || child.id === edge.target) {
						edge.hidden = expanded;
					}
				});
				if (child.data?.expanded != false) {
					showhideChildren(child.id);
				}
			});
		}
	}

	const onclick = (event) => {
		const currentNode = $nodes.filter((node) => id === node.id)[0];
		if (done) {
			currentNode.style = 'width: 150px; height: 40px';

			updateEdges();

			showhideChildren(id);

			expanded = !expanded;
			currentNode.data.expanded = expanded;

			$nodes = $nodes;
			$edges = $edges;
			//console.log("openclose nodes after", JSON.parse(JSON.stringify($nodes)));

			onLayout('DOWN');
		} else {
			if (data.hasChildren) {
				call('studio.treeview.Get', {
					id: `${id}`,
					flow: true
				}).then((res) => {
					console.log(res);
					const parentDbo = { ...res, classname: data.classname };
					console.log(parentDbo);
					const ne = createNodesAndEdges(parentDbo, currentNode);
					console.log(ne);
					$nodes.push(...ne.nodes);
					$edges.push(...ne.edges);
					updateEdges(ne.nodes);

					$nodes = $nodes;
					$edges = $edges;
					done = true;
					expanded = true;

					onLayout('DOWN');
				});
			}
		}
	};

	function onLayout(direction) {
		const opts = { 'elk.direction': direction };
		getLayoutedElements($nodes, $edges, opts)
			.then((layouted) => {
				$nodes = layouted.nodes;
				$edges = layouted.edges;
			})
			.then(() => {
				fitView();
			});
	}

	const DEFAULT_HANDLE_STYLE = 'width: 6px; height: 6px; bottom: -5px;';
	const isXControl = isOutsideContainer(data?.classname);
</script>

<Handle id="in" type="target" position={targetPosition ?? Position.Top} />
{#if data.isSourceContainer}
	<Handle
		id="input"
		type="target"
		position={Position.Left}
		style="{DEFAULT_HANDLE_STYLE}; top: 10px; background: green;"
	/>
{/if}
{#if data.isXml}
	<Handle
		id="output"
		type="source"
		position={Position.Left}
		style="{DEFAULT_HANDLE_STYLE}; top: 20px; background: blue;"
	/>
{/if}
<Handle id="out" type="source" position={sourcePosition ?? Position.Bottom} />

<div class="flex flex-col w-full h-full">
	<div class="w-full">
		<button
			type="button"
			class="w-full h-full px-2 py-3 inline-flex text-nowrap justify-center gap-x-1.5 rounded-md bg-white text-xs font-semibold text-gray-900 shadow-xs ring-1 ring-inset ring-gray-300 hover:bg-gray-50"
			id="menu-button"
		>
			<AutoSvg class="w-4 h-4" fill="currentColor" src="{getUrl()}{data.icon}" alt="ico" />
			<span>{data.label.substring(0, 15)}</span>
			{#if data.hasChildren}
				{#if expanded}
					<svg
						class="h-4 w-4 text-gray-400"
						fill="none"
						viewBox="0 0 24 24"
						stroke="currentColor"
						aria-hidden="true"
						data-slot="icon"
						{onclick}
					>
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M5 15l7-7 7 7"
						/>
					</svg>
				{:else}
					<svg
						class="h-4 w-4 text-gray-400"
						fill="none"
						viewBox="0 0 24 24"
						stroke="currentColor"
						aria-hidden="true"
						data-slot="icon"
						{onclick}
					>
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M19 9l-7 7-7-7"
						/>
					</svg>
				{/if}
			{/if}
		</button>
	</div>
	{#if data.hasChildren && expanded && !isXControl}
		<div class="grow w-full bg-white shadow-xs rounded-md ring-1 ring-inset ring-gray-300"></div>
	{/if}
</div>
