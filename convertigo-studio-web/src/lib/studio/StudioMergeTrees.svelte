<script>
	import AutoSvg from '$lib/utils/AutoSvg.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { getUrl } from '$lib/utils/service';

	/**
	 * @typedef {{ name: string, label: string, value: string, differs?: boolean }} TreeProperty
	 * @typedef {{ key: string, name: string, type?: string, icon?: string, match?: string,
	 *  objectId?: string, status: 'same' | 'changed' | 'only', inner?: boolean,
	 *  properties?: TreeProperty[], children?: TreeNode[] }} TreeNode
	 */

	/**
	 * The two versions of an object in conflict as two trees side by side: each object opens on its
	 * children, on both sides at once, and shows its properties beside those of its match.
	 *
	 * @type {{
	 *  trees: { left?: TreeNode, right?: TreeNode },
	 *  leftLabel: string,
	 *  rightLabel: string,
	 *  onSelect?: (id: string) => void
	 * }}
	 */
	let { trees, leftLabel, rightLabel, onSelect } = $props();

	const SIDES = /** @type {('left' | 'right')[]} */ (['left', 'right']);

	/** the objects opened, by side and key, else opened when a change is below them */
	let opened = $state(/** @type {Record<string, boolean>} */ ({}));
	/** the object whose properties show */
	let selected = $state(/** @type {{ side: 'left' | 'right', key: string } | null} */ (null));
	let allProperties = $state(false);

	/** @type {Record<'left' | 'right', Map<string, TreeNode>>} */
	let index = $derived.by(() => {
		/** @type {Record<'left' | 'right', Map<string, TreeNode>>} */
		const maps = { left: new Map(), right: new Map() };
		/**
		 * @param {'left' | 'right'} side
		 * @param {TreeNode | undefined} node
		 */
		const walk = (side, node) => {
			if (node) {
				maps[side].set(node.key, node);
				node.children?.forEach((child) => walk(side, child));
			}
		};
		walk('left', trees.left);
		walk('right', trees.right);
		return maps;
	});

	/**
	 * @param {'left' | 'right'} side
	 * @param {TreeNode} node
	 * @param {number} depth
	 */
	function isOpen(side, node, depth) {
		return opened[`${side}:${node.key}`] ?? (depth === 0 || Boolean(node.inner));
	}

	/**
	 * Opens or closes an object, and its match on the other side.
	 * @param {'left' | 'right'} side
	 * @param {TreeNode} node
	 * @param {number} depth
	 */
	function toggle(side, node, depth) {
		const open = !isOpen(side, node, depth);
		opened[`${side}:${node.key}`] = open;
		if (node.match) {
			opened[`${side === 'left' ? 'right' : 'left'}:${node.match}`] = open;
		}
	}

	let pair = $derived.by(() => {
		if (!selected) {
			return null;
		}
		const node = index[selected.side].get(selected.key);
		const other = node?.match
			? index[selected.side === 'left' ? 'right' : 'left'].get(node.match)
			: undefined;
		return selected.side === 'left' ? { left: node, right: other } : { left: other, right: node };
	});

	/** the properties of the object selected and of its match, those that differ first */
	let rows = $derived.by(() => {
		if (!pair) {
			return [];
		}
		/** @type {Map<string, { name: string, label: string, left?: string, right?: string, differs: boolean }>} */
		const byName = new Map();
		for (const [side, node] of /** @type {const} */ ([
			['left', pair.left],
			['right', pair.right]
		])) {
			for (const property of node?.properties ?? []) {
				const row = byName.get(property.name) ?? {
					name: property.name,
					label: property.label,
					differs: false
				};
				row[side] = property.value;
				row.differs ||= Boolean(property.differs);
				byName.set(property.name, row);
			}
		}
		const all = [...byName.values()];
		if (!pair.left || !pair.right) {
			return all.filter((row) => allProperties || (row.left ?? row.right));
		}
		return all
			.filter((row) => allProperties || row.differs)
			.sort((a, b) => Number(b.differs) - Number(a.differs));
	});
</script>

{#snippet branch(
	/** @type {'left' | 'right'} */ side,
	/** @type {TreeNode} */ node,
	/** @type {number} */ depth
)}
	{@const open = isOpen(side, node, depth)}
	{@const current =
		selected &&
		((selected.side === side && selected.key === node.key) ||
			(selected.side !== side && index[selected.side].get(selected.key)?.match === node.key))}
	<div
		class={[
			'studio-trees__row',
			`studio-trees__row--${node.status}`,
			current && 'studio-trees__row--selected'
		]}
		style:--studio-trees-depth={depth}
	>
		{#if node.children?.length}
			<button
				type="button"
				class="studio-trees__toggle"
				aria-label="{open ? 'Close' : 'Open'} {node.name}"
				aria-expanded={open}
				onclick={() => toggle(side, node, depth)}
			>
				<Ico icon={open ? 'mdi:chevron-down' : 'mdi:chevron-right'} size={3.5} />
			</button>
		{:else}
			<span class="studio-trees__toggle"></span>
		{/if}
		<button
			type="button"
			class="studio-trees__object"
			title={`${node.name}${node.type ? ` (${node.type})` : ''}${node.status === 'only' ? ` — only in ${side === 'left' ? leftLabel : rightLabel}` : node.status === 'changed' ? ' — changed' : ''}`}
			onclick={() => (selected = { side, key: node.key })}
			ondblclick={() => node.objectId && onSelect?.(node.objectId)}
		>
			{#if node.icon}
				<span class="studio-trees__icon"
					><AutoSvg class="h-4 w-4" fill="currentColor" src="{getUrl()}{node.icon}" alt="" /></span
				>
			{/if}
			<span class="studio-trees__name">{node.name}</span>
			{#if node.status === 'only'}
				<span class="studio-trees__mark studio-trees__mark--only">+</span>
			{:else if node.status === 'changed'}
				<span class="studio-trees__mark studio-trees__mark--changed">~</span>
			{:else if node.inner}
				<span class="studio-trees__mark studio-trees__mark--inner">•</span>
			{/if}
		</button>
	</div>
	{#if open}
		{#each node.children ?? [] as child (child.key)}
			{@render branch(side, child, depth + 1)}
		{/each}
	{/if}
{/snippet}

<div class="studio-trees">
	<div class="studio-trees__columns">
		{#each SIDES as side (side)}
			<div
				class="studio-trees__column"
				role="tree"
				aria-label={side === 'left' ? leftLabel : rightLabel}
			>
				<div class="studio-trees__head">{side === 'left' ? leftLabel : rightLabel}</div>
				{#if trees[side]}
					{@render branch(side, /** @type {TreeNode} */ (trees[side]), 0)}
				{:else}
					<p class="studio-trees__none">Not in this version</p>
				{/if}
			</div>
		{/each}
	</div>
	{#if pair}
		<div class="studio-trees__properties">
			<div class="studio-trees__properties-head">
				<strong>{(pair.left ?? pair.right)?.name}</strong>
				<small>{(pair.left ?? pair.right)?.type}</small>
				<label>
					<input type="checkbox" bind:checked={allProperties} />
					All the properties
				</label>
				{#if pair.left?.objectId && onSelect}
					<button
						type="button"
						class="studio-trees__select"
						onclick={() => pair?.left?.objectId && onSelect?.(pair.left.objectId)}
						>Select in the tree</button
					>
				{/if}
			</div>
			{#each rows as row (row.name)}
				<div class={['studio-trees__property', row.differs && 'studio-trees__property--differs']}>
					<span class="studio-trees__label">{row.label}</span>
					<span title={row.left}>{pair.left ? (row.left ?? '') || '—' : ''}</span>
					<span title={row.right}>{pair.right ? (row.right ?? '') || '—' : ''}</span>
				</div>
			{:else}
				<p class="studio-trees__none">
					{pair.left && pair.right ? 'The same properties on both sides.' : 'No property set.'}
				</p>
			{/each}
		</div>
	{:else}
		<p class="studio-trees__hint">
			Click an object to compare its properties; double-click selects it in the tree.
		</p>
	{/if}
</div>

<style>
	.studio-trees {
		display: grid;
		gap: 0.4rem;
		font-size: 0.74rem;
	}

	.studio-trees__columns {
		display: grid;
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.3rem;
		grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
	}

	.studio-trees__column {
		min-width: 0;
		max-height: var(--studio-trees-height, 22rem);
		overflow: auto;
		padding-bottom: 0.25rem;
	}

	.studio-trees__column + .studio-trees__column {
		border-left: 1px solid var(--studio-line);
	}

	.studio-trees__head {
		position: sticky;
		z-index: 1;
		top: 0;
		border-bottom: 1px solid var(--studio-line);
		background: var(--studio-chrome-bg);
		color: var(--studio-text-strong);
		padding: 0.2rem 0.4rem;
		font-size: 0.7rem;
		font-weight: 600;
	}

	.studio-trees__row {
		display: flex;
		min-width: 0;
		align-items: center;
		padding-left: calc(0.2rem + var(--studio-trees-depth, 0) * 0.8rem);
	}

	.studio-trees__row--selected {
		background: var(--studio-selection-bg);
	}

	.studio-trees__row--only .studio-trees__object {
		background: color-mix(in oklab, var(--color-success-500) 12%, transparent);
	}

	.studio-trees__row--same .studio-trees__name {
		color: var(--studio-text-idle);
	}

	.studio-trees__toggle {
		display: inline-grid;
		width: 1rem;
		height: 1.25rem;
		flex: none;
		place-items: center;
		border: 0;
		background: transparent;
		color: var(--studio-text-idle);
		padding: 0;
	}

	.studio-trees__object {
		display: flex;
		min-width: 0;
		flex: 1;
		align-items: center;
		gap: 0.3rem;
		border: 0;
		border-radius: 0.2rem;
		background: transparent;
		color: var(--studio-text);
		padding: 0.1rem 0.3rem;
		text-align: left;
	}

	.studio-trees__object:hover {
		background: var(--studio-hover-bg);
	}

	.studio-trees__icon {
		display: inline-grid;
		flex: none;
		place-items: center;
	}

	.studio-trees__name {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-trees__mark {
		flex: none;
		font-family: var(--font-mono, ui-monospace, monospace);
		font-weight: 700;
	}

	.studio-trees__mark--only {
		color: var(--color-success-600-400);
	}

	.studio-trees__mark--changed,
	.studio-trees__mark--inner {
		color: var(--color-warning-600-400);
	}

	.studio-trees__properties {
		display: grid;
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.3rem;
	}

	.studio-trees__properties-head {
		display: flex;
		flex-wrap: wrap;
		align-items: baseline;
		gap: 0.4rem;
		border-bottom: 1px solid var(--studio-line);
		background: var(--studio-hover-bg);
		padding: 0.25rem 0.4rem;
	}

	.studio-trees__properties-head small {
		color: var(--studio-text-idle);
	}

	.studio-trees__properties-head label {
		display: inline-flex;
		align-items: center;
		gap: 0.25rem;
		margin-left: auto;
		color: var(--studio-text-idle);
		font-size: 0.7rem;
	}

	.studio-trees__select {
		border: 1px solid var(--studio-line);
		border-radius: 0.25rem;
		background: transparent;
		color: var(--studio-text);
		padding: 0 0.35rem;
		font-size: 0.7rem;
	}

	.studio-trees__property {
		display: grid;
		gap: 0.4rem;
		padding: 0.15rem 0.4rem;
		grid-template-columns: minmax(5rem, 0.7fr) minmax(0, 1fr) minmax(0, 1fr);
	}

	.studio-trees__property span {
		min-width: 0;
		max-height: 6rem;
		overflow: auto;
		overflow-wrap: anywhere;
		white-space: pre-wrap;
	}

	.studio-trees__property--differs {
		background: color-mix(in oklab, var(--color-warning-500) 10%, transparent);
	}

	.studio-trees__label {
		color: var(--studio-text-idle);
	}

	.studio-trees__none,
	.studio-trees__hint {
		margin: 0;
		padding: 0.3rem 0.4rem;
		color: var(--studio-text-idle);
		font-size: 0.7rem;
	}
</style>
