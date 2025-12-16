<script>
	import TreeView from '$lib/common/components/TreeView.svelte';
	import { createProjectTree } from '$lib/common/ProjectsTree.svelte.js';
	import AutoSvg from '$lib/utils/AutoSvg.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { getUrl } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import { selectedId } from './treeStore';

	let { rootNode, loadRoot, checkChildren } = $derived(createProjectTree());

	onMount(() => {
		loadRoot();
	});

	function onSelectionChange(e) {
		$selectedId = e?.selectedValue?.[0] ?? '';
	}

	async function onExpandedChange(details) {
		const nodes = details?.expandedNodes ?? [];
		await Promise.all(nodes.map((node) => checkChildren(node)));
	}

	function onNodeIndicator(e, api, nodeState) {
		e.stopPropagation();
		api[nodeState.expanded ? 'collapse' : 'expand'](nodeState.value);
	}
</script>

<TreeView
	{rootNode}
	{onSelectionChange}
	{onExpandedChange}
	expandOnClick={false}
	base="w-full"
	classes="break-words select-none"
	textClass="text-sm font-medium"
	indicatorClass="order-first transition-transform duration-200 data-[state=open]:rotate-90"
	childrenClass="border-l border-surface-200-800 pl-2"
	controlClass="layout-x-low rounded-base py-1 transition-soft hover:bg-surface-200-800"
>
	{#snippet nodeIcon({ node })}
		{#if typeof node.icon === 'string' && node.icon.includes('?')}
			<AutoSvg class="h-4 w-4" fill="currentColor" src="{getUrl()}{node.icon}" alt="ico" />
		{:else if node.icon == 'file'}
			<Ico icon="mdi:file-document-box-outline" size={4} />
		{:else if node.icon == 'folder'}
			<Ico icon="mdi:folder-outline" size={4} />
		{:else}
			<Ico icon="convertigo:logo" size={4} />
		{/if}
	{/snippet}

	{#snippet nodeText({ node })}
		<span>{node.label ?? node.name ?? 'Unnamed'}</span>
	{/snippet}

	{#snippet nodeIndicator({ api, nodeState })}
		<button
			type="button"
			class="button-ico-tertiary grid size-6 place-items-center p-none!"
			aria-label={nodeState.expanded ? 'Collapse' : 'Expand'}
			onclick={(e) => onNodeIndicator(e, api, nodeState)}
		>
			<Ico icon="mdi:chevron-right" size={4} />
		</button>
	{/snippet}
</TreeView>
