<svelte:options accessors />

<script>
	import { onMount } from 'svelte';
	import { TreeView, TreeViewItem } from '@skeletonlabs/skeleton';
	import { callService, getServiceUrl } from './convertigo';
	import { properties } from '$lib/propertiesStore';
	// @ts-ignore
	import IconFolder from '~icons/mdi/folder';
	// @ts-ignore
	import IconFile from '~icons/mdi/file';

	/** @type string | null */
	export let id = null;

	/** @type string */
	export let label = '';

	/** @type boolean | [] */
	export let children = true;

	export let icon = 'folder';

	export let links = {};

	export let root;

	let item;

	let live = false;
	let self;
	onMount(() => {
		live = true;
		self = this;
		expanded |= id == null;
		checkChildren();
	});

	export let expanded = false;

	export function checkChildren(force = false) {
		if (!force && !expanded) {
			return;
		}
		if (children == true) {
			callService('tree.Get', id == null ? {} : { id }).then((res) => {
				children = res.children;
			});
		}
		let ids = [];
		for (let child of Object.values(links)) {
			if (child.children == true) {
				ids.push(child.id);
			}
			//child.checkChildren(true);
		}
		if (ids.length > 0) {
			callService('tree.Get', { ids: JSON.stringify(ids) }).then((res) => {
				ids.forEach((i) => (links[i].children = res[i]));
			});
		}
	}

	async function nodeClicked(e) {
		if (e.target.tagName == 'SPAN') {
			e.preventDefault();
		}
		var data = await callService('tree.PropertyGet', { id });
		properties.set(data.properties);
	}
</script>

{#if id != null}
	<TreeViewItem
		bind:this={item}
		on:toggle={(e) => {
			expanded = e.detail.open;
			checkChildren();
		}}
		on:click={nodeClicked}
		hideChildren={!Array.isArray(children) || children.length == 0}
	>
		<svelte:fragment slot="lead">
			{#if icon.includes('?')}
				<img
					src={getServiceUrl() +
						icon +
						'&__xsrfToken=' +
						encodeURIComponent(localStorage.getItem('x-xsrf-token') ?? '')}
					alt="ico"
				/>
			{:else if icon == 'file'}
				<IconFile />
			{:else}
				<IconFolder />
			{/if}
		</svelte:fragment>
		<svelte:fragment slot="children">
			{#if Array.isArray(children) && children.length > 0}
				{#each children as child}
					<svelte:self {...child} {root} bind:this={links[child.id]} />
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
				if (!expanded) {
					item.open = true;
				}
			}}
			>{label}
		</span>
	</TreeViewItem>
{:else if Array.isArray(children)}
	<TreeView padding="py-1 px-1" bind:this={root}>
		{#each children as child}
			<svelte:self {...child} {root} bind:this={links[child.id]} />
		{/each}
	</TreeView>
{:else}
	Not loaded
{/if}
