<script>
	/**
	 * @typedef {{ name: string, label: string, old: string, new: string }} VersionProperty
	 * @typedef {{ name: string, type?: string, status: 'same' | 'modified' | 'left' | 'right',
	 *  key?: string, rightKey?: string, objectId?: string, properties?: VersionProperty[],
	 *  children?: VersionNode[] }} VersionNode
	 * @typedef {{ props?: Record<string, 'mine' | 'theirs'>, children?: Record<string, boolean> }} Combination
	 */

	/**
	 * The two versions of an object in conflict, side by side: its properties that differ, and its children,
	 * those of a single side, those that differ and those that are the same. Combined, each property that
	 * differs is taken from the side clicked, mine by default, and each child of a single side kept or not.
	 *
	 * @type {{
	 *  versions: { left: string, right: string, root: VersionNode },
	 *  labels: Record<string, string>,
	 *  onSelect?: (id: string) => void,
	 *  combination?: Combination | null,
	 *  onCombine?: (combination: Combination) => void
	 * }}
	 */
	let { versions, labels, onSelect, combination = null, onCombine } = $props();

	const CHOICES = /** @type {('mine' | 'theirs')[]} */ (['mine', 'theirs']);

	/**
	 * @param {string} id the object and its property, key#property
	 * @param {'mine' | 'theirs'} side
	 */
	function choose(id, side) {
		onCombine?.({
			props: { ...(combination?.props ?? {}), [id]: side },
			children: { ...(combination?.children ?? {}) }
		});
	}

	/**
	 * @param {string} id the child of a single side, L:key or R:key
	 * @param {boolean} kept
	 */
	function keep(id, kept) {
		onCombine?.({
			props: { ...(combination?.props ?? {}) },
			children: { ...(combination?.children ?? {}), [id]: kept }
		});
	}

	/**
	 * @param {VersionNode} node
	 * @returns {number} its children and properties that differ, all levels
	 */
	function differences(node) {
		return (
			(node.properties?.length ?? 0) +
			(node.children ?? []).reduce(
				(count, child) => count + (child.status === 'same' ? 0 : 1 + differences(child)),
				0
			)
		);
	}
</script>

{#snippet row(
	/** @type {VersionNode} */ node,
	/** @type {number} */ depth,
	/** @type {boolean} */ inOnly
)}
	{@const only = node.status === 'left' || node.status === 'right'}
	{@const keepId = node.status === 'left' ? `L:${node.key}` : `R:${node.rightKey}`}
	{@const choosable = Boolean(combination) && only && !inOnly}
	{@const kept = combination?.children?.[keepId] !== false}
	<div
		class={[
			'studio-versions__row',
			`studio-versions__row--${node.status}`,
			choosable && !kept && 'studio-versions__row--dropped'
		]}
		style:--studio-versions-depth={depth}
	>
		{#each ['left', 'right'] as side (side)}
			{@const present = node.status !== (side === 'left' ? 'right' : 'left')}
			{#if choosable && present}
				<label
					class="studio-versions__cell studio-versions__cell--keep"
					title="Keep {node.name} in the object combined"
				>
					<input
						type="checkbox"
						checked={kept}
						aria-label="Keep {node.name}"
						onchange={(event) => keep(keepId, event.currentTarget.checked)}
					/>
					<span class="studio-versions__name">{node.name}</span>
					{#if node.type}<small>{node.type}</small>{/if}
				</label>
			{:else}
				<button
					type="button"
					class="studio-versions__cell"
					disabled={!present || !node.objectId || !onSelect}
					title={present
						? `${node.name}${node.type ? ` (${node.type})` : ''}${node.status === 'modified' ? ', changed' : node.status === 'same' ? ', the same' : `, ${labels[versions[side]] ?? versions[side]} only`}`
						: `Not in ${labels[versions[side]] ?? versions[side]}`}
					onclick={() => node.objectId && onSelect?.(node.objectId)}
				>
					{#if present}
						<span class="studio-versions__mark"
							>{node.status === 'modified' ? '~' : node.status === 'same' ? '=' : '+'}</span
						>
						<span class="studio-versions__name">{node.name}</span>
						{#if node.type}<small>{node.type}</small>{/if}
					{:else}
						<span class="studio-versions__missing">—</span>
					{/if}
				</button>
			{/if}
		{/each}
	</div>
	{#if node.status !== 'same'}
		{#each node.properties ?? [] as property (property.name)}
			{@const id = `${node.key}#${property.name}`}
			{@const chosen = combination?.props?.[id] ?? 'mine'}
			<div
				class="studio-versions__row studio-versions__row--property"
				style:--studio-versions-depth={depth + 1}
			>
				{#if combination && node.key && !inOnly}
					{#each CHOICES as side (side)}
						{@const value = side === 'mine' ? property.old : property.new}
						<button
							type="button"
							class={[
								'studio-versions__value',
								'studio-versions__value--choice',
								chosen === side && 'studio-versions__value--chosen'
							]}
							aria-pressed={chosen === side}
							title="Take the {property.label} of this side"
							onclick={() => choose(id, side)}><small>{property.label}</small>{value || '—'}</button
						>
					{/each}
				{:else}
					<span class="studio-versions__value" title={property.old}
						><small>{property.label}</small>{property.old || '—'}</span
					>
					<span class="studio-versions__value" title={property.new}
						><small>{property.label}</small>{property.new || '—'}</span
					>
				{/if}
			</div>
		{/each}
		{#each node.children ?? [] as child, index (index)}
			{@render row(child, depth + 1, inOnly || only)}
		{/each}
	{/if}
{/snippet}

<div class="studio-versions" aria-label="The two versions">
	<div class="studio-versions__head">
		<span>{labels[versions.left] ?? versions.left}</span>
		<span>{labels[versions.right] ?? versions.right}</span>
	</div>
	{@render row(versions.root, 0, false)}
	{#if versions.root.status === 'modified' && !differences(versions.root)}
		<p class="studio-versions__note">The same objects, in another order.</p>
	{/if}
</div>

<style>
	.studio-versions {
		display: grid;
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.3rem;
		font-size: 0.74rem;
	}

	.studio-versions__head,
	.studio-versions__row {
		display: grid;
		grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
	}

	.studio-versions__head {
		border-bottom: 1px solid var(--studio-line);
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
		font-size: 0.7rem;
		font-weight: 600;
	}

	.studio-versions__head span,
	.studio-versions__cell,
	.studio-versions__value {
		min-width: 0;
		padding: 0.15rem 0.4rem 0.15rem calc(0.4rem + var(--studio-versions-depth, 0) * 0.75rem);
	}

	.studio-versions__head span + span,
	.studio-versions__cell + .studio-versions__cell,
	.studio-versions__value + .studio-versions__value {
		border-left: 1px solid var(--studio-line);
	}

	.studio-versions__cell {
		display: flex;
		align-items: baseline;
		gap: 0.3rem;
		border: 0;
		background: transparent;
		color: var(--studio-text);
		text-align: left;
	}

	.studio-versions__cell:not(:disabled):hover {
		background: var(--studio-hover-bg);
	}

	.studio-versions__cell:disabled {
		opacity: 1;
		cursor: default;
	}

	.studio-versions__cell small,
	.studio-versions__value small {
		overflow: hidden;
		color: var(--studio-text-idle);
		font-size: 0.66rem;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-versions__name {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-versions__mark {
		width: 0.7rem;
		flex: none;
		font-family: var(--font-mono, ui-monospace, monospace);
		font-weight: 700;
	}

	.studio-versions__row--same {
		color: var(--studio-text-idle);
	}

	.studio-versions__row--same .studio-versions__cell {
		color: var(--studio-text-idle);
	}

	.studio-versions__row--modified .studio-versions__mark {
		color: var(--color-warning-600-400);
	}

	.studio-versions__row--left .studio-versions__cell:first-child,
	.studio-versions__row--right .studio-versions__cell:last-child {
		background: color-mix(in oklab, var(--color-success-500) 12%, transparent);
	}

	.studio-versions__row--left .studio-versions__mark,
	.studio-versions__row--right .studio-versions__mark {
		color: var(--color-success-600-400);
	}

	.studio-versions__missing {
		color: var(--studio-text-idle);
	}

	.studio-versions__value {
		display: grid;
		max-height: 7.5rem;
		gap: 0.05rem;
		overflow-x: hidden;
		overflow-y: auto;
		overflow-wrap: anywhere;
		background: color-mix(in oklab, var(--color-warning-500) 8%, transparent);
		white-space: pre-wrap;
	}

	.studio-versions__cell--keep {
		cursor: pointer;
	}

	.studio-versions__cell--keep input {
		flex: none;
		margin: 0;
	}

	.studio-versions__row--dropped .studio-versions__name {
		opacity: 0.55;
		text-decoration: line-through;
	}

	.studio-versions__value--choice {
		border: 0;
		border-left: 2px solid transparent;
		color: var(--studio-text);
		text-align: left;
		cursor: pointer;
	}

	.studio-versions__value--choice:not(.studio-versions__value--chosen) {
		background: transparent;
		color: var(--studio-text-idle);
	}

	.studio-versions__value--chosen {
		border-left-color: var(--color-primary-500);
		background: color-mix(in oklab, var(--color-primary-500) 16%, transparent);
	}

	.studio-versions__note {
		margin: 0;
		padding: 0.3rem 0.4rem;
		color: var(--studio-text-idle);
		font-size: 0.7rem;
	}
</style>
