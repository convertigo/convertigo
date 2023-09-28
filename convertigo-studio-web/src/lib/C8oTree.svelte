<svelte:options accessors />

<script>
	import { onMount, createEventDispatcher } from 'svelte';
	import { TreeView, TreeViewItem } from '@skeletonlabs/skeleton';
	import { callService, getServiceUrl } from './convertigo';
	import { treeData } from './treeStore';

	// @ts-ignore
	import IconFolder from '~icons/mdi/folder';
	// @ts-ignore
	import IconFile from '~icons/mdi/file';

	const dispatch = createEventDispatcher();

	export let nodeData = $treeData;

	/** @type {any} */
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

	export function checkChildren(force = false) {
		if (!force && !nodeData.expanded) {
			return;
		}
		if (nodeData.children == true) {
			callService('tree.Get', nodeData.id == null ? {} : { id: nodeData.id }).then((res) => {
				nodeData.children = res.children;
			});
		}
		/** @type string[] */
		let ids = [];
		for (let child of Object.values(links)) {
			if (child.nodeData.children == true) {
				ids.push(child.nodeData.id);
			}
		}
		if (ids.length > 0) {
			callService('tree.Get', { ids: JSON.stringify(ids) }).then((res) => {
				for (let i of ids) {
					links[i].nodeData.children = res[i];
				};
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
		dispatch('treeClick', {id: nodeData.id});
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
	>
		<svelte:fragment slot="lead">
			{#if nodeData.icon.includes('?')}
				<img
					src={getServiceUrl() +
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
					<svelte:self nodeData={child} {root} bind:this={links[child.id]} on:treeClick />
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
				console.log(e);
				if (!nodeData.expanded) {
					item.open = true;
				}
			}}
			>{nodeData.label}
		</span>
	</TreeViewItem>
{:else if Array.isArray(nodeData.children)}
	<TreeView padding="py-1 px-1" bind:this={root} open={nodeData.expanded ?? false}>
		{#each nodeData.children as child}
			<svelte:self nodeData={child} {root} bind:this={links[child.id]} on:treeClick />
		{/each}
	</TreeView>
{:else}
	Not loaded
{/if}
