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
	import Treeview from './Treeview.svelte';

	// @ts-ignore
	import IconFile from '~icons/mdi/file';
	import Icon from '@iconify/svelte';
	import AutoSvg from '$lib/utils/AutoSvg.svelte';

	const modalStore = getModalStore();

	const dispatch = createEventDispatcher();

	/** @type {{nodeData?: any, links?: any, root?: TreeView | null}} */
	let { nodeData = $bindable($treeData), links = $bindable({}), root = $bindable(null) } = $props();

	/** @type {TreeViewItem} */
	let item = $state();

	/** @type {DndBlock} */
	let block = $state();

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

<div class="h-full bg-surface-50 dark:bg-surface-800">
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
		>
			<svelte:fragment slot="children">
				{#if Array.isArray(nodeData.children) && nodeData.children.length > 0}
					{#each nodeData.children as child}
						{#if child.icon.includes('?') && child.id === nodeData.children[0].id}
							<DropDivider nodeData={child} on:update={update} position="first" />
						{/if}
						<Treeview
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
			<div class="group flex">
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
								<AutoSvg
									class="h-6 w-6"
									fill="currentColor"
									src="{getUrl()}{nodeData.icon}"
									alt="ico"
									height="35px"
									width="35px"
								/>
							{:else if nodeData.icon == 'file'}
								<IconFile class="h-6 w-6" />
							{:else}
								<Icon icon="material-symbols-light:folder-outline" class="h-6 w-6" />
							{/if}
						</span>
						<span
							slot="label"
							class="text-[11.5px] text-surface-900 dark:font-light dark:text-gray-100"
							>{nodeData.label}</span
						>
					</DndBlock>
				</div>
				<div class="invisible group-hover:visible">
					{#if nodeData.icon.includes('?')}
						<Toolbar>
							<ToolbarItem
								onclick={(e) => {
									e.preventDefault();
									$propertiesSelected = !$propertiesSelected;
								}}
								><Icon
									icon="material-symbols-light:event-list-outline-sharp"
									class="h-6 w-6"
								/></ToolbarItem
							>
							<ToolbarItem
								onclick={(e) => {
									e.preventDefault();
								}}
								><Icon
									icon="material-symbols-light:help-center-outline-sharp"
									class="h-6 w-6"
								/></ToolbarItem
							>
							<ToolbarItem
								onclick={(e) => {
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
								}}><Icon icon="ph:trash-thin" class="h-6 w-6" /></ToolbarItem
							>
						</Toolbar>
					{/if}
				</div>
			</div>
		</TreeViewItem>
	{:else if Array.isArray(nodeData.children)}
		<TreeView
			padding="px-1"
			bind:this={root}
			open={nodeData.expanded ?? false}
			caretClosed="-rotate-90"
			caretOpen="rotate-0"
		>
			{#each nodeData.children as child}
				<TreeView
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
</div>

<style>
	:global(html.dark) .color {
		filter: invert(100);
	}
	.color {
		filter: invert(0);
	}

	.white-svg {
		/*filter: invert(100%);*/
		fill: white;
	}
</style>
