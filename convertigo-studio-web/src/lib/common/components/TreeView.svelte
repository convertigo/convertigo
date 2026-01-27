<script>
	import {
		createTreeViewCollection,
		TreeView as SkeletonTreeView,
		useTreeView
	} from '@skeletonlabs/skeleton-svelte';
	import TreeNode from './TreeNode.svelte';

	/** @type {{ id: string; name: string; children: any[] }} */
	const defaultRoot = {
		id: 'root',
		name: 'Root',
		children: []
	};

	/** @type {any} */
	let {
		animationConfig = {
			duration: 200
		},
		base = 'w-full',
		classes = '',
		labelBase = 'text-lg font-semibold mb-2',
		treeBase = '',
		rootNode = defaultRoot,
		controlClass = 'layout-x-low cursor-pointer',
		textClass = 'text-sm',
		indicatorClass = 'ml-auto text-muted',
		childrenClass = 'pl-4',
		nodeClass = '',
		label,
		nodeIcon,
		nodeText,
		nodeIndicator,
		apiRef = $bindable(),
		...zagProps
	} = $props();

	const rootKey = $derived.by(() => rootNode?.id ?? defaultRoot.id);
	let loadedKey = $state('');
	let loadedCollection = $state.raw(null);

	const collection = $derived.by(() => {
		if (loadedCollection && loadedKey === rootKey) return loadedCollection;
		return createTreeViewCollection({
			nodeToValue: (node) => node.id,
			nodeToString: (node) => node.name,
			rootNode
		});
	});

	function onLoadChildrenComplete({ collection: c }) {
		loadedKey = rootKey;
		loadedCollection = c;
	}

	const id = $props.id();
	const treeView = useTreeView(() => ({
		id,
		collection,
		onLoadChildrenComplete,
		...zagProps
	}));
	const api = $derived(treeView());
	$effect(() => {
		apiRef = api;
	});

	export function setSelectedValue(value) {
		return api.setSelectedValue(value);
	}

	export function getExpandedValue() {
		return api.expandedValue;
	}

	export function setExpandedValue(value) {
		return api.setExpandedValue(value);
	}

	const rootClass = $derived(
		['convertigo-treeview', base, classes].filter(Boolean).join(' ').trim()
	);
</script>

<SkeletonTreeView.Provider value={treeView} class={rootClass} data-testid="tree-view">
	{#if !!label}
		<SkeletonTreeView.Label class={labelBase}>{@render label()}</SkeletonTreeView.Label>
	{/if}
	<SkeletonTreeView.Tree class={treeBase}>
		{#if collection}
			{#each collection.rootNode.children as node, index (node.id ?? index)}
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
		{/if}
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

	:global(.convertigo-treeview [data-part='branch-control']),
	:global(.convertigo-treeview [data-part='item']) {
		@apply w-full;
	}

	:global(.convertigo-treeview [data-part='branch-control'][data-selected]),
	:global(.convertigo-treeview [data-part='item'][data-selected]) {
		@apply border border-l-2 border-primary-200/70 border-l-primary-500 bg-primary-50/80 text-strong shadow-sm/10 shadow-surface-900-100;
		@apply dark:border-primary-700/60 dark:border-l-primary-400 dark:bg-primary-900/30 dark:text-primary-50;
	}

	:global(
		.convertigo-treeview [data-part='branch-control'][data-selected] [data-part='branch-text']
	),
	:global(.convertigo-treeview [data-part='item'][data-selected] [data-part='item-text']) {
		@apply font-semibold;
	}
</style>
