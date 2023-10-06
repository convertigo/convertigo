<svelte:options accessors />

<script>
	import { onMount, createEventDispatcher } from 'svelte';
	import { TreeView, TreeViewItem } from '@skeletonlabs/skeleton';
	import DndBlock from './DndBlock.svelte';
	import DropDivider from './DropDivider.svelte';
	import { call, getUrl } from '../utils/service';
	import { treeData, selectedId } from './treeStore';
	import { removeDbo } from '$lib/utils/service';
	import { tick } from 'svelte';

	// @ts-ignore
	import IconFolder from '~icons/mdi/folder';
	// @ts-ignore
	import IconFile from '~icons/mdi/file';

	const dispatch = createEventDispatcher();

	export let nodeData = $treeData;

	export let links = {};

	/** @type {TreeView | null} */
	export let root = null;

	/** @type {TreeViewItem} */
	let item;

	let live = false;

	let self;
	onMount(() => {
		live = true;
		// @ts-ignore
		self = this;
		checkChildren();
	});

	function update() {
		nodeData.children = true;
		checkChildren(true);
	}

	export function checkChildren(force = false) {
		if (!force && !nodeData.expanded) {
			return;
		}
		(async () => {
			if (nodeData.children == true) {
				nodeData.children = (
					await call('studio.treeview.Get', nodeData.id == null ? {} : { id: nodeData.id })
				).children;
			}
			await tick();
			/** @type string[] */
			let ids = [];
			for (let child of Object.values(links)) {
				try {
					if (child.nodeData.children == true) {
						ids.push(child.nodeData.id);
					}
				} catch (e) {}
			}
			if (ids.length > 0) {
				call('studio.treeview.Get', { ids: JSON.stringify(ids) }).then((res) => {
					for (let i of ids) {
						try {
							links[i].nodeData.children = res[i];
						} catch (e) {}
					}
					nodeData = nodeData;
				});
			}
		})();
	}

	/**
	 * @param {MouseEvent} e
	 */
	async function nodeClicked(e) {
		// @ts-ignore
		if (e.target?.tagName == 'SPAN') {
			e.preventDefault();
		}
		$selectedId = nodeData.id ?? '';
	}

	/**
	 * @param {KeyboardEvent} e
	 */
	async function handleKeyDown(e) {
		if (e.key === 'Delete') {
			e.preventDefault();
			// dispatch for parent
			dispatch('treeDelete', { id: nodeData.id });
		}
	}

	async function treeDelete(e) {
		let id = e.detail.id;
		let link = links[id];
		if (link != undefined) {
			let result = await removeDbo(id != null ? id : undefined);
			if (result.done) {
				// destroy child component
				link.$destroy();
				// update tree item
				update();
			}
		}
	}
</script>

{#if nodeData.id != null}
	<TreeViewItem
		bind:this={item}
		on:toggle={(e) => {
			nodeData.expanded = e.detail.open;
			checkChildren();
		}}
		on:click={nodeClicked}
		hideChildren={!Array.isArray(nodeData.children) || nodeData.children.length == 0}
		open={nodeData.expanded ?? false}
		on:keydown={handleKeyDown}
		on:treeDelete={treeDelete}
	>
		<svelte:fragment slot="children">
			{#if Array.isArray(nodeData.children) && nodeData.children.length > 0}
				{#each nodeData.children as child}
					{#if child.icon.includes('?') && child.id === nodeData.children[0].id}
						<DropDivider nodeData={child} on:update={update} position="first" />
					{/if}
					<svelte:self
						nodeData={child}
						{root}
						bind:this={links[child.id]}
						on:treeDelete={treeDelete}
					/>
					{#if child.icon.includes('?')}
						<DropDivider nodeData={child} on:update={update} position="after" />
					{/if}
				{/each}
			{/if}
		</svelte:fragment>
		<DndBlock {nodeData} {item} on:update={update}>
			<span slot="icon">
				{#if nodeData.icon.includes('?')}
					<img src={`${getUrl()}${nodeData.icon}`} alt="ico" />
				{:else if nodeData.icon == 'file'}
					<IconFile />
				{:else}
					<IconFolder />
				{/if}
			</span>
			<span slot="label">{nodeData.label}</span>
		</DndBlock>
	</TreeViewItem>
{:else if Array.isArray(nodeData.children)}
	<TreeView
		padding="py-1 px-1"
		bind:this={root}
		open={nodeData.expanded ?? false}
		caretClosed="-rotate-90"
		caretOpen="rotate-0"
	>
		{#each nodeData.children as child}
			<svelte:self nodeData={child} {root} bind:this={links[child.id]} on:treeDelete={treeDelete} />
		{/each}
	</TreeView>
{:else}
	Not loaded
{/if}
