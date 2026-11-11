<script>
	import Ico from '#lib/utils/Ico.svelte';
	import { childrenOf, nodeLabel } from './nativeJson.js';

	/**
	 * A JSON value as a tree, opened on demand, as the JSON trees of the Eclipse Studio: key: {n}, [i]: [n],
	 * key: "text", at most 500 children by node. A click picks a value by its path.
	 *
	 * @type {{
	 *  value: any,
	 *  rootLabel?: string,
	 *  selected?: (string | number | null)[] | null,
	 *  onSelect?: (path: (string | number)[], value: any) => void,
	 *  onActivate?: (path: (string | number)[], value: any) => void,
	 *  onDragStart?: (event: DragEvent, path: (string | number)[], value: any) => void,
	 *  label?: string
	 * }}
	 */
	let {
		value,
		rootLabel = '$',
		selected = null,
		onSelect,
		onActivate,
		onDragStart,
		label = 'JSON'
	} = $props();

	/** @type {Set<string>} */
	let expanded = $state(new Set(['[]']));
	let selectedKey = $derived(selected ? JSON.stringify(selected) : '');

	$effect(() => {
		// the ancestors of the selection open, as a selected node of the Eclipse tree shows
		const path = selected;
		if (!path) {
			return;
		}
		const next = new Set(expanded);
		let changed = false;
		for (let i = 0; i < path.length; i++) {
			const key = JSON.stringify(path.slice(0, i));
			if (!next.has(key)) {
				next.add(key);
				changed = true;
			}
		}
		if (changed) {
			expanded = next;
		}
	});

	/**
	 * @param {(string | number)[]} path
	 */
	function toggle(path) {
		const key = JSON.stringify(path);
		const next = new Set(expanded);
		if (next.has(key)) {
			next.delete(key);
		} else {
			next.add(key);
		}
		expanded = next;
	}

	/**
	 * @param {any} node
	 */
	function isContainer(node) {
		return node !== null && typeof node === 'object';
	}
</script>

{#snippet branch(
	/** @type {any} */ node,
	/** @type {(string | number)[]} */ path,
	/** @type {string} */ name,
	/** @type {number} */ depth
)}
	{@const key = JSON.stringify(path)}
	{@const open = expanded.has(key)}
	{@const container = isContainer(node)}
	<li
		role="treeitem"
		aria-expanded={container ? open : undefined}
		aria-selected={key === selectedKey}
	>
		<div
			class="studio-json-tree__row"
			class:studio-json-tree__row--selected={key === selectedKey}
			style:padding-inline-start={`${depth * 0.85 + 0.2}rem`}
		>
			{#if container}
				<button
					type="button"
					class="studio-json-tree__toggle"
					aria-label={open ? 'Collapse' : 'Expand'}
					onclick={() => toggle(path)}
				>
					<Ico icon={open ? 'mdi:chevron-down' : 'mdi:chevron-right'} size={4} />
				</button>
			{:else}
				<span class="studio-json-tree__toggle" aria-hidden="true"></span>
			{/if}
			<button
				type="button"
				class="studio-json-tree__label"
				draggable={onDragStart ? 'true' : undefined}
				title={nodeLabel(name, node)}
				onclick={() => onSelect?.(path, node)}
				ondblclick={() => (container ? toggle(path) : onActivate?.(path, node))}
				ondragstart={(event) => onDragStart?.(event, path, node)}
			>
				{nodeLabel(name, node)}
			</button>
		</div>
		{#if container && open}
			{@const children = childrenOf(node)}
			<ul role="group">
				{#each children.entries as child (child.segment)}
					{@render branch(
						child.value,
						[...path, child.segment],
						typeof child.segment === 'number' ? `[${child.segment}]` : child.segment,
						depth + 1
					)}
				{/each}
				{#if children.more > 0}
					<li
						class="studio-json-tree__more"
						style:padding-inline-start={`${(depth + 1) * 0.85 + 1.4}rem`}
					>
						… {children.more} more
					</li>
				{/if}
			</ul>
		{/if}
	</li>
{/snippet}

<ul class="studio-json-tree" role="tree" aria-label={label}>
	{@render branch(value, [], rootLabel, 0)}
</ul>

<style>
	.studio-json-tree,
	.studio-json-tree :global(ul) {
		margin: 0;
		padding: 0;
		list-style: none;
	}

	.studio-json-tree {
		min-width: max-content;
		font-family: var(--font-mono, ui-monospace, monospace);
		font-size: 0.74rem;
		line-height: 1.35;
	}

	.studio-json-tree__row {
		display: flex;
		align-items: center;
		gap: 0.15rem;
		min-height: 1.45rem;
		border-radius: 0.3rem;
	}

	.studio-json-tree__row:hover {
		background: var(
			--studio-hover-bg,
			color-mix(in oklab, var(--color-surface-500) 12%, transparent)
		);
	}

	.studio-json-tree__row--selected,
	.studio-json-tree__row--selected:hover {
		background: light-dark(
			color-mix(in oklab, var(--color-primary-500) 16%, white),
			color-mix(in oklab, var(--color-primary-500) 22%, #1c1c1d)
		);
		color: var(--studio-text-strong, var(--color-primary-700-300));
	}

	.studio-json-tree__toggle {
		display: inline-grid;
		flex: none;
		width: 1.1rem;
		height: 1.1rem;
		place-items: center;
		border: 0;
		background: none;
		color: var(--studio-text-idle, var(--color-surface-600-400));
		cursor: pointer;
	}

	.studio-json-tree__label {
		border: 0;
		background: none;
		padding: 0 0.25rem;
		color: inherit;
		font: inherit;
		text-align: start;
		white-space: nowrap;
		cursor: pointer;
	}

	.studio-json-tree__more {
		color: var(--studio-text-idle, var(--color-surface-600-400));
		font-style: italic;
	}
</style>
