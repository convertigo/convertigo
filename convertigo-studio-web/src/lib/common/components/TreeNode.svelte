<script>
	import { slide } from 'svelte/transition';
	import TreeNode from './TreeNode.svelte';

	let {
		animationConfig,
		node,
		indexPath,
		api,
		controlClass,
		textClass,
		indicatorClass,
		childrenClass,
		nodeIcon,
		nodeText,
		nodeIndicator,
		nodeClass
	} = $props();

	let nodeState = $derived(api.getNodeState({ node, indexPath }));

	const toggleBranch = (event) => {
		event?.preventDefault?.();
		event?.stopPropagation?.();
		if (!nodeState.isBranch) return;
		api[nodeState.expanded ? 'collapse' : 'expand'](nodeState.value);
	};
</script>

{#snippet nodeCommon({ node, indexPath })}
	{#if !!nodeIcon}
		{@render nodeIcon({
			api,
			node,
			nodeState,
			indexPath
		})}
	{/if}
	<span {...api.getBranchTextProps({ node, indexPath })} class={textClass}>
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
{/snippet}

{#if nodeState.isBranch}
	<div {...api.getBranchProps({ node, indexPath })} class={nodeClass}>
		<div
			{...api.getBranchControlProps({ node, indexPath })}
			class={controlClass}
			ondblclick={toggleBranch}
		>
			{@render nodeCommon({ node, indexPath })}
			<span {...api.getBranchIndicatorProps({ node, indexPath })} class={indicatorClass}>
				{#if !!nodeIndicator}
					{@render nodeIndicator({
						api,
						node,
						nodeState,
						indexPath
					})}
				{/if}</span
			>
		</div>
		{#if nodeState.expanded}
			<div
				{...api.getBranchContentProps({ node, indexPath })}
				class={childrenClass}
				transition:slide={animationConfig}
			>
				<div {...api.getBranchIndentGuideProps({ node, indexPath })}></div>
				{#each node.children as child, index}
					<TreeNode
						{api}
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
	</div>
{:else}
	<div
		{...api.getItemProps({ node, indexPath })}
		class="{controlClass} {nodeClass}"
		ondblclick={() => api.setSelectedValue([nodeState.value])}
	>
		{@render nodeCommon({ node, indexPath })}
	</div>
{/if}
