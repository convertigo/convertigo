<script lang="ts">
	import { useEdges, useNodes } from '@xyflow/svelte';

	export let onClick: () => void;
	export let id: string;
	export let top: number | undefined;
	export let left: number | undefined;
	export let right: number | undefined;
	export let bottom: number | undefined;

	const nodes = useNodes();
	const edges = useEdges();

	function duplicateNode() {
		const node = nodes.current.find((node) => node.id === id);
		if (node) {
			nodes.update((nds) => {
				nds.push({
					...node,
					// You should use a better id than this in production
					id: `${id}-copy${Math.random()}`,
					position: {
						x: node.position.x,
						y: node.position.y + 50
					}
				});
				return nds;
			});
		}
		// $nodes = $nodes;
	}

	function deleteNode() {
		nodes.set(nodes.current.filter((node) => node.id !== id));
		edges.set(edges.current.filter((edge) => edge.source !== id && edge.target !== id));
	}
</script>

<div
	style="top: {top}px; left: {left}px; right: {right}px; bottom: {bottom}px;"
	class="context-menu"
	onclick={onClick}
>
	<p style="margin: 0.5em;">
		<small>node: {id}</small>
	</p>
	<button onclick={duplicateNode}>duplicate</button>
	<button onclick={deleteNode}>delete</button>
</div>

<style>
	.context-menu {
		background: white;
		border-style: solid;
		box-shadow: 10px 19px 20px rgba(0, 0, 0, 10%);
		position: absolute;
		z-index: 10;
	}

	.context-menu button {
		border: none;
		display: block;
		padding: 0.5em;
		text-align: left;
		width: 100%;
	}

	.context-menu button:hover {
		background: white;
	}
</style>
