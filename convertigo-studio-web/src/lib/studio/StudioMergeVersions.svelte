<script>
	/**
	 * @typedef {{ name: string, label: string, old: string, new: string }} VersionProperty
	 * @typedef {{ name: string, type?: string, status: 'same' | 'modified' | 'left' | 'right',
	 *  objectId?: string, properties?: VersionProperty[], children?: VersionNode[] }} VersionNode
	 */

	/**
	 * The two versions of an object in conflict, side by side: its properties that differ, and its children,
	 * those of a single side, those that differ and those that are the same.
	 *
	 * @type {{
	 *  versions: { left: string, right: string, root: VersionNode },
	 *  labels: Record<string, string>,
	 *  onSelect?: (id: string) => void
	 * }}
	 */
	let { versions, labels, onSelect } = $props();

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

{#snippet row(/** @type {VersionNode} */ node, /** @type {number} */ depth)}
	<div
		class={['studio-versions__row', `studio-versions__row--${node.status}`]}
		style:--studio-versions-depth={depth}
	>
		{#each ['left', 'right'] as side (side)}
			{@const present = node.status !== (side === 'left' ? 'right' : 'left')}
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
		{/each}
	</div>
	{#if node.status !== 'same'}
		{#each node.properties ?? [] as property (property.name)}
			<div
				class="studio-versions__row studio-versions__row--property"
				style:--studio-versions-depth={depth + 1}
			>
				<span class="studio-versions__value" title={property.old}
					><small>{property.label}</small>{property.old || '—'}</span
				>
				<span class="studio-versions__value" title={property.new}
					><small>{property.label}</small>{property.new || '—'}</span
				>
			</div>
		{/each}
		{#each node.children ?? [] as child, index (index)}
			{@render row(child, depth + 1)}
		{/each}
	{/if}
{/snippet}

<div class="studio-versions" aria-label="The two versions">
	<div class="studio-versions__head">
		<span>{labels[versions.left] ?? versions.left}</span>
		<span>{labels[versions.right] ?? versions.right}</span>
	</div>
	{@render row(versions.root, 0)}
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

	.studio-versions__note {
		margin: 0;
		padding: 0.3rem 0.4rem;
		color: var(--studio-text-idle);
		font-size: 0.7rem;
	}
</style>
