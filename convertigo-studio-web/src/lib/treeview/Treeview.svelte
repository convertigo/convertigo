<svelte:options accessors />

<script>
	import { onMount, tick, createEventDispatcher } from 'svelte';
	import { TreeView, TreeViewItem } from '@skeletonlabs/skeleton';
	import { localStorageStore, getModalStore } from '@skeletonlabs/skeleton';

	import { call, getUrl, removeDbo, copyDbo, cutDbo, pasteDbo } from '$lib/utils/service';
	import { treeData, selectedId, cutBlocks } from './treeStore';
	import DndBlock from './DndBlock.svelte';
	import DropDivider from './DropDivider.svelte';
	import Toolbar from '../toolbar/Toolbar.svelte';
	import ToolbarItem from '../toolbar/ToolbarItem.svelte';

	// @ts-ignore
	import IconFolder from '~icons/mdi/folder';
	// @ts-ignore
	import IconFile from '~icons/mdi/file';

	// @ts-ignore
	import IconProperties from '~icons/mdi/playlist-edit';
	// @ts-ignore
	import IconHelp from '~icons/mdi/help-box';
	// @ts-ignore
	import IconDelete from '~icons/mdi/delete';

	const modalStore = getModalStore();

	const dispatch = createEventDispatcher();

	export let nodeData = $treeData;

	export let links = {};

	/** @type {TreeView | null} */
	export let root = null;

	/** @type {TreeViewItem} */
	let item;

	/** @type {DndBlock} */
	let block;

	let live = false;

	let propertiesSelected = localStorageStore('studio.propertiesSelected', false);

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
		// case copy dbo
		if ((e.ctrlKey || e.metaKey) && e.code === 'KeyC') {
			let ids = [];
			ids.push(nodeData.id);
			let result = await copyDbo(JSON.stringify(ids));
			if (result.done) {
				let xml = result.xml;
				navigator.clipboard.writeText(xml);
			}
		}
		// case cut dbo
		if ((e.ctrlKey || e.metaKey) && e.code === 'KeyX') {
			let ids = [];
			ids.push(nodeData.id);
			let result = await cutDbo(JSON.stringify(ids));
			if (result.done) {
				let xml = result.xml;
				navigator.clipboard.writeText(xml);

				// store cut block
				$cutBlocks.push(block);
				$cutBlocks = $cutBlocks;
			}
		}
		// case paste dbo
		if ((e.ctrlKey || e.metaKey) && e.code === 'KeyV') {
			let xml = await navigator.clipboard.readText();
			let result = await pasteDbo('' + nodeData.id, xml);
			if (result.done) {
				// update target parent in tree
				update();

				// update source parent in tree
				let ids = result.ids;
				$cutBlocks.forEach((cutBlock) => {
					if (ids.includes(cutBlock.nodeData.id)) {
						cutBlock.dispatchRemove();
					}
				});
			}
			$cutBlocks = [];
		}
		// case delete dbo
		else if (e.key === 'Delete') {
			e.preventDefault();
			let id = nodeData.id;
			let result = await removeDbo(id != null ? id : undefined);
			if (result.done) {
				block.dispatchRemove();
			}
		}
	}

	async function treeDelete(e) {
		let id = e.detail.id;
		let result = await removeDbo(id != null ? id : undefined);
		if (result.done) {
			destroyChild(id);
		}
	}

	function treeRemove(e) {
		destroyChild(e.detail.id);
	}

	function destroyChild(id) {
		let link = links[id];
		if (link != undefined) {
			// destroy child component
			link.$destroy();
			// update tree item
			update();
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
		on:treeRemove={treeRemove}
		regionSymbol="flex-none"
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
						on:treeRemove={treeRemove}
					/>
					{#if child.icon.includes('?')}
						<DropDivider nodeData={child} on:update={update} position="after" />
					{/if}
				{/each}
			{/if}
		</svelte:fragment>
		<div class="flex group">
			<div class="grow">
				<DndBlock
					bind:this={block}
					{block}
					{nodeData}
					{item}
					on:update={update}
					on:remove={(e) => dispatch('treeRemove', { id: nodeData.id })}
					on:delete={(e) => dispatch('treeDelete', { id: nodeData.id })}
				>
					<span slot="icon">
						{#if nodeData.icon.includes('?')}
							<img style="height:1.2rem; width:1.2rem;" src={`${getUrl()}${nodeData.icon}`} alt="ico" />
						{:else if nodeData.icon == 'file'}
							<IconFile height="0.8rem" width="0.8rem"/>
						{:else}
							<IconFolder height="0.8rem" width="0.8rem"/>
						{/if}
					</span>
					<span slot="label">{nodeData.label}</span>
				</DndBlock>
			</div>
			<div class="invisible group-hover:visible">
				{#if nodeData.icon.includes('?')}
					<Toolbar>
						<ToolbarItem
							on:click={(e) => {
								e.preventDefault();
								$propertiesSelected = !$propertiesSelected;
							}}><IconProperties width="16" /></ToolbarItem
						>
						<ToolbarItem
							on:click={(e) => {
								e.preventDefault();
							}}><IconHelp width="16" /></ToolbarItem
						>
						<ToolbarItem
							on:click={(e) => {
								e.preventDefault();
								modalStore.trigger({
									type: 'confirm',
									title: 'Please Confirm',
									body: 'Are you sure you wish to delete this object ?',
									response: (b) => {
										if (b) {
											block.dispatchDelete();
										}
									}
								});
							}}><IconDelete width="16" /></ToolbarItem
						>
					</Toolbar>
				{/if}
			</div>
		</div>
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
			<svelte:self
				nodeData={child}
				{root}
				bind:this={links[child.id]}
				on:treeDelete={treeDelete}
				on:treeRemove={treeRemove}
			/>
		{/each}
	</TreeView>
{:else}
	Not loaded
{/if}