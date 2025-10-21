<script>
	import { TreeView as SkeletonTreeView } from '@skeletonlabs/skeleton-svelte';
	import { slide } from 'svelte/transition';
	import TreeNode from './TreeNode.svelte';

	let {
		animationConfig,
		node,
		indexPath,
		treeView,
		controlClass,
		textClass,
		indicatorClass,
		childrenClass,
		nodeIcon,
		nodeText,
		nodeIndicator,
		nodeClass
	} = $props();

	const api = $derived(treeView());
	const nodeProps = $derived({ node, indexPath });
	const nodeState = $derived(api.getNodeState(nodeProps));

	const isBranch = $derived(nodeState.isBranch);

	function toggleBranch(event) {
		event?.preventDefault?.();
		event?.stopPropagation?.();
		if (!nodeState.isBranch) return;
		api[nodeState.expanded ? 'collapse' : 'expand'](nodeState.value);
	}

	function selectLeaf() {
		api.setSelectedValue([nodeState.value]);
	}

	const itemClass = $derived([controlClass, nodeClass].filter(Boolean).join(' ').trim());
</script>

<SkeletonTreeView.NodeProvider value={nodeProps}>
	{#if isBranch}
		<SkeletonTreeView.Branch class={nodeClass}>
			<SkeletonTreeView.BranchControl class={controlClass} ondblclick={toggleBranch}>
				{#if !!nodeIcon}
					{@render nodeIcon({
						api,
						node,
						nodeState,
						indexPath
					})}
				{/if}
				<SkeletonTreeView.BranchText class={textClass}>
					{#if !!nodeText}
						{@render nodeText({
							api,
							node,
							nodeState,
							indexPath
						})}
					{:else}
						{node.name}
					{/if}
				</SkeletonTreeView.BranchText>
				<SkeletonTreeView.BranchIndicator class={indicatorClass}>
					{#if !!nodeIndicator}
						{@render nodeIndicator({
							api,
							node,
							nodeState,
							indexPath
						})}
					{/if}
				</SkeletonTreeView.BranchIndicator>
			</SkeletonTreeView.BranchControl>
			<SkeletonTreeView.BranchContent class={childrenClass}>
				{#if Array.isArray(node.children) && node.children.length}
					<div transition:slide={animationConfig}>
						{#each node.children as child, index}
							<TreeNode
								{treeView}
								{animationConfig}
								node={child}
								indexPath={[...indexPath, index]}
								{nodeIcon}
								{nodeText}
								{nodeIndicator}
								{controlClass}
								{textClass}
								{indicatorClass}
								{childrenClass}
								{nodeClass}
							/>
						{/each}
					</div>
				{/if}
			</SkeletonTreeView.BranchContent>
		</SkeletonTreeView.Branch>
	{:else}
		<SkeletonTreeView.Item class={itemClass} ondblclick={selectLeaf}>
			{#if !!nodeIcon}
				{@render nodeIcon({
					api,
					node,
					nodeState,
					indexPath
				})}
			{/if}
			<span class={textClass}>
				{#if !!nodeText}
					{@render nodeText({
						api,
						node,
						nodeState,
						indexPath
					})}
				{:else}
					{node.name}
				{/if}
			</span>
		</SkeletonTreeView.Item>
	{/if}
</SkeletonTreeView.NodeProvider>
