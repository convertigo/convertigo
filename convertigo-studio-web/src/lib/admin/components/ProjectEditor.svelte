<script>
	import TreeView from '$lib/common/components/TreeView.svelte';
	import AutoSvg from '$lib/utils/AutoSvg.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { call, getUrl } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import TableAutoCard from './TableAutoCard.svelte';

	let { project } = $props();

	/** @type {any} */
	let rootNode = $state({
		id: 'ROOT',
		name: '',
		children: [{ id: project, name: project, children: true }]
	});

	let properties = $state({});

	async function checkChildren(node = rootNode) {
		let toUpdate = {};
		if (node.children && !Array.isArray(node.children)) {
			toUpdate[node.id] = node;
		} else if (Array.isArray(node.children)) {
			for (let child of node.children) {
				if (child.children && !Array.isArray(child.children)) {
					toUpdate[child.id] = child;
				}
			}
		}
		const ids = Object.keys(toUpdate);
		if (ids.length > 0) {
			const updates = await call('studio.treeview.Get', {
				ids: JSON.stringify(ids)
			});
			for (let id in updates) {
				if (toUpdate[id]) {
					toUpdate[id].children = updates[id];
				}
			}
		}
	}

	function onExpandedChange({ expandedNodes }) {
		for (let node of expandedNodes) {
			checkChildren(node);
		}
	}

	async function onSelectionChange({ selectedValue }) {
		const res = await call('studio.properties.Get', {
			id: selectedValue[0]
		});
		const props = [];
		for (let key in res.properties) {
			props.push({
				Name: res.properties[key].displayName ?? key,
				Value: res.properties[key].value
			});
		}
		properties = props;
	}

	onMount(() => {
		checkChildren().then(() => {
			checkChildren(rootNode.children[0]);
			onSelectionChange({ selectedValue: [project] });
		});
	});

	function onNodeIndicator(e, api, nodeState) {
		e.stopPropagation();
		api[nodeState.expanded ? 'collapse' : 'expand'](nodeState.value);
	}
</script>

<div class="m-low layout-x items-start">
	<TreeView
		{rootNode}
		{onExpandedChange}
		{onSelectionChange}
		expandOnClick={false}
		defaultExpandedValue={[project]}
		defaultSelectedValue={[project]}
		base="preset-outlined-primary-500 py-low px-[20px] rounded-base"
		classes="text-primary-700-300"
		textClass="text-surface-800-200"
		indicatorClass="order-first text-primary-600-400 -ml-[14px]"
	>
		{#snippet nodeIcon({ api, node, nodeState, indexPath })}
			{#if node.icon?.includes('?')}
				<AutoSvg class="h-6 w-6 p-1" fill="currentColor" src="{getUrl()}{node.icon}" alt="ico" />
			{:else if node.icon == 'file'}
				<Ico icon="material-symbols:unknown-document-outline" class="h-6 w-6" />
			{:else if node.icon == 'folder'}
				<Ico icon="material-symbols:folder-outline" class="h-6 w-6" />
			{:else}
				<Ico icon="convertigo:logo" class="h-6 w-6" />
			{/if}
		{/snippet}
		{#snippet nodeText({ node })}
			<span>{node.name}</span>
		{/snippet}
		{#snippet nodeIndicator({ api, nodeState })}
			<button
				onclick={(e) => onNodeIndicator(e, api, nodeState)}
				class:opened={nodeState.expanded}
				class:closed={!nodeState.expanded}>❯</button
			>
		{/snippet}
	</TreeView>
	<TableAutoCard
		thClass="preset-filled-primary-100-900"
		trClass="even:preset-filled-primary-200-800 odd:preset-filled-primary-300-700 hover:preset-filled-primary-400-600"
		definition={[
			{ key: 'Name', name: 'Name' },
			{ key: 'Value', name: 'Value' }
		]}
		data={properties}
	></TableAutoCard>
</div>

<style lang="postcss">
	@reference "../../../app.css";

	:global([data-selected] > [data-part='branch-text']) {
		@apply rounded-base preset-filled-primary-100-900 px-low;
	}

	.closed {
		rotate: 0deg;
		transition: rotate 0.2s ease-in-out;
	}

	.opened {
		rotate: 90deg;
		transition: rotate 0.2s ease-in-out;
	}
</style>
