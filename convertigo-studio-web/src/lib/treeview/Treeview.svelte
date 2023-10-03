<svelte:options accessors />

<script>
	import { onMount, createEventDispatcher } from 'svelte';
	import { TreeView, TreeViewItem } from '@skeletonlabs/skeleton';
	import { call, getUrl } from '../utils/service';
	import { treeData, selectedId } from './treeStore';
	import { reusables } from '$lib/palette/paletteStore';
	import { addDbo, removeDbo } from '$lib/utils/service';

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
		if (nodeData.children == true) {
			call('studio.treeview.Get', nodeData.id == null ? {} : { id: nodeData.id }).then((res) => {
				nodeData.children = res.children;
			});
		}
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
	}

	/**
	 * @param {MouseEvent} e
	 */
	async function nodeClicked(e) {
		// @ts-ignore
		if (e.target?.tagName == 'SPAN') {
			e.preventDefault();
		}
		dispatch('treeClick', { id: nodeData.id });
		$selectedId = nodeData.id ?? '';
	}

	function handleDragOver(e) {
		e.preventDefault();
		return false;
	}

	async function handleDrop(e) {
		e.preventDefault();
		let jsonData = undefined;
		let target = nodeData.id;
		try {
			jsonData = JSON.parse(e.dataTransfer.getData('text'));
		} catch (e) {}
		if (target != null && jsonData != undefined) {
			let result = await addDbo(target, jsonData);
			if (result.done) {
				// update palette reusables
				if (jsonData.type === 'paletteData') {
					$reusables[jsonData.data.id] = jsonData.data;
					$reusables = $reusables;
				}
				// update tree item
				update();
			}
		}
	}

	/**
	 * @param {KeyboardEvent} e
	 */
	async function handleKeyDown(e) {
		if (e.key === 'Delete') {
			e.preventDefault();
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
		<svelte:fragment slot="lead">
			{#if nodeData.icon.includes('?')}
				<img
					src={getUrl() +
						nodeData.icon +
						'&__xsrfToken=' +
						encodeURIComponent(localStorage.getItem('x-xsrf-token') ?? '')}
					alt="ico"
				/>
			{:else if nodeData.icon == 'file'}
				<IconFile />
			{:else}
				<IconFolder />
			{/if}
		</svelte:fragment>
		<svelte:fragment slot="children">
			{#if Array.isArray(nodeData.children) && nodeData.children.length > 0}
				{#each nodeData.children as child}
					<svelte:self
						nodeData={child}
						{root}
						bind:this={links[child.id]}
						on:treeClick
						on:treeDelete={treeDelete}
					/>
				{/each}
			{/if}
		</svelte:fragment>
		<!-- svelte-ignore a11y-no-static-element-interactions -->
		<span
			draggable="true"
			on:drag={(e) => {
				e.stopPropagation();
			}}
			on:dragstart={(e) => e.stopPropagation()}
			on:dragenter={(e) => {
				//console.log(e);
				if (!nodeData.expanded) {
					item.open = true;
				}
			}}
			on:dragover={handleDragOver}
			on:drop={handleDrop}
			>{nodeData.label}
		</span>
	</TreeViewItem>
{:else if Array.isArray(nodeData.children)}
	<TreeView padding="py-1 px-1" bind:this={root} open={nodeData.expanded ?? false}>
		{#each nodeData.children as child}
			<svelte:self
				nodeData={child}
				{root}
				bind:this={links[child.id]}
				on:treeClick
				on:treeDelete={treeDelete}
			/>
		{/each}
	</TreeView>
{:else}
	Not loaded
{/if}
