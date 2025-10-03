<script>
	import { normalizeProps, useMachine } from '@zag-js/svelte';
	import * as tree from '@zag-js/tree-view';
	import TreeNode from './TreeNode.svelte';

	/** @type {any} */
	const {
		animationConfig = {
			duration: 200
		},
		base = 'w-full',
		classes = '',
		labelBase = 'text-lg font-semibold mb-2',
		treeBase = '',
		rootNode = {
			id: 'root',
			name: 'Root',
			children: []
		},
		controlClass = 'flex items-center gap-2 cursor-pointer',
		textClass = 'text-sm',
		indicatorClass = 'ml-auto text-muted',
		childrenClass = 'pl-4',
		nodeClass = '',
		label,
		nodeIcon,
		nodeText,
		nodeIndicator,
		...zagProps
	} = $props();

	let collection = $state(
		tree.collection({
			nodeToValue: (node) => node.id,
			nodeToString: (node) => node.name,
			rootNode
		})
	);

	function onLoadChildrenComplete({ collection: c }) {
		console.log('Load children complete', c);
		collection = c;
	}

	const id = $props.id();
	const service = useMachine(tree.machine, () => ({
		id,
		collection,
		onLoadChildrenComplete,
		...zagProps
	}));
	const api = $derived(tree.connect(service, normalizeProps));
	export function setSelectedValue(value) {
		return api.setSelectedValue(value);
	}

	export function getExpandedValue() {
		return api.expandedValue;
	}

	export function setExpandedValue(value) {
		return api.setExpandedValue(value);
	}
</script>

<div {...api.getRootProps()} class="{base} {classes}" data-testid="tree-view">
	{#if !!label}
		<h3 {...api.getLabelProps()} class={labelBase}>{@render label()}</h3>
	{/if}
	<div {...api.getTreeProps()} class={treeBase}>
		{#each collection.rootNode.children as node, index}
			<TreeNode
				{node}
				{api}
				indexPath={[index]}
				{nodeIcon}
				{nodeText}
				{nodeIndicator}
				{controlClass}
				{textClass}
				{indicatorClass}
				{childrenClass}
				{animationConfig}
				{nodeClass}
			/>
		{/each}
	</div>
</div>
