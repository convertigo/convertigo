<script>
	import {
		createTreeViewCollection,
		TreeView as SkeletonTreeView,
		useTreeView
	} from '@skeletonlabs/skeleton-svelte';
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
		controlClass = 'layout-x-low cursor-pointer',
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
		createTreeViewCollection({
			nodeToValue: (node) => node.id,
			nodeToString: (node) => node.name,
			rootNode
		})
	);

	function onLoadChildrenComplete({ collection: c }) {
		collection = c;
	}

	const id = $props.id();
	const treeView = useTreeView(() => ({
		id,
		collection,
		onLoadChildrenComplete,
		...zagProps
	}));
	const api = $derived(treeView());

	export function setSelectedValue(value) {
		return api.setSelectedValue(value);
	}

	export function getExpandedValue() {
		return api.expandedValue;
	}

	export function setExpandedValue(value) {
		return api.setExpandedValue(value);
	}

	const rootClass = ['convertigo-treeview', base, classes].filter(Boolean).join(' ').trim();
</script>

<SkeletonTreeView.Provider value={treeView} class={rootClass} data-testid="tree-view">
	{#if !!label}
		<SkeletonTreeView.Label class={labelBase}>{@render label()}</SkeletonTreeView.Label>
	{/if}
	<SkeletonTreeView.Tree class={treeBase}>
		{#each collection.rootNode.children as node, index}
			<TreeNode
				{node}
				{treeView}
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
	</SkeletonTreeView.Tree>
</SkeletonTreeView.Provider>

<style lang="postcss">
	@reference '../../../app.css';

	:global(.convertigo-treeview) {
		--convertigo-tree-indent: 0.75;
	}

	:global(.convertigo-treeview [data-part='branch-control']) {
		padding-inline-start: calc(var(--depth) * var(--spacing) * var(--convertigo-tree-indent));
	}

	:global(.convertigo-treeview [data-part='branch-indent-guide']) {
		margin-inline-start: calc(var(--depth) * var(--spacing) * var(--convertigo-tree-indent));
	}

	:global(.convertigo-treeview [data-part='item']) {
		padding-inline-start: calc(var(--depth) * var(--spacing) * var(--convertigo-tree-indent));
	}
</style>
