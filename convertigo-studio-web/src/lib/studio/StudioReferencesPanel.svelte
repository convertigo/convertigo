<script>
	import AutoSvg from '$lib/utils/AutoSvg.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { call, getUrl } from '$lib/utils/service';
	import StudioEmptyState from './StudioEmptyState.svelte';

	/**
	 * The references of the selected object, as the References view of the Eclipse Studio: what it requires
	 * and what uses it, grouped by project.
	 *
	 * @typedef {{ id: string, name: string, type: string, icon: string }} ReferenceNode
	 * @typedef {{ project: ReferenceNode, entries: { target: ReferenceNode, source: ReferenceNode }[] }} ReferenceGroup
	 */

	/** @type {{
	 *  selectedId?: string,
	 *  active?: boolean,
	 *  onSelect?: (id: string) => void
	 * }}
	 */
	let { selectedId = '', active = false, onSelect } = $props();

	/** @type {{ object: ReferenceNode, requires: ReferenceGroup[], usedBy: ReferenceGroup[] } | null} */
	let references = $state.raw(null);
	let loading = $state(false);
	let error = $state('');
	let loadedId = '';
	let serial = 0;

	$effect(() => {
		const id = selectedId;
		if (!active || !id || id === loadedId) {
			return;
		}
		void load(id);
	});

	/**
	 * @param {string} id
	 */
	async function load(id) {
		const current = ++serial;
		loadedId = id;
		if (id.includes('/')) {
			// a file of a project is no object: nothing refers to it by name
			references = null;
			loading = false;
			error = 'A file of a project has no references.';
			return;
		}
		loading = true;
		error = '';
		try {
			const response = await call('studio.treeview.References', { id });
			if (current !== serial) {
				return;
			}
			if (response?.object) {
				references = response;
			} else {
				references = null;
				error = String(response?.error?.message ?? 'No references for this object.');
			}
		} finally {
			if (current === serial) {
				loading = false;
			}
		}
	}
</script>

{#snippet node(/** @type {ReferenceNode} */ item, /** @type {string} */ className = '')}
	<AutoSvg class="h-4 w-4" fill="currentColor" src="{getUrl()}{item.icon}" alt="" />
	<span class={className}>{item.name}</span>
{/snippet}

{#snippet section(
	/** @type {string} */ title,
	/** @type {ReferenceGroup[]} */ groups,
	/** @type {'target' | 'source'} */ main,
	/** @type {string} */ empty
)}
	<h3 class="studio-references__section">{title}</h3>
	{#if !groups.length}
		<p class="studio-references__empty">{empty}</p>
	{/if}
	{#each groups as group (group.project.id)}
		<button
			type="button"
			class="studio-references__row studio-references__row--project"
			onclick={() => onSelect?.(group.project.id)}
		>
			{@render node(group.project)}
		</button>
		{#each group.entries as entry (entry.target.id + entry.source.id)}
			{@const other = main === 'target' ? entry.source : entry.target}
			<button
				type="button"
				class="studio-references__row"
				title={`${entry.source.id} → ${entry.target.id}`}
				onclick={() => onSelect?.(entry[main].id)}
			>
				{@render node(entry[main])}
				<small>
					<Ico icon={main === 'target' ? 'mdi:arrow-left' : 'mdi:arrow-right'} size={3} />
					{other.name}
				</small>
			</button>
		{/each}
	{/each}
{/snippet}

<div class="studio-references">
	{#if !selectedId}
		<StudioEmptyState message="No object selected" small />
	{:else if loading && !references}
		<StudioEmptyState message="Looking for the references" loading small />
	{:else if error}
		<p class="studio-references__empty">{error}</p>
	{:else if references}
		<div class="studio-references__columns" class:studio-references__columns--loading={loading}>
			<div>
				{@render section(
					`${references.object.name} requires`,
					references.requires,
					'target',
					'It requires no other object.'
				)}
			</div>
			<div>
				{@render section(
					`${references.object.name} is used by`,
					references.usedBy,
					'source',
					'Nothing depends on this object.'
				)}
			</div>
		</div>
	{/if}
</div>

<style>
	.studio-references {
		height: 100%;
		min-height: 0;
		overflow: auto;
		padding: 0.4rem 0;
	}

	.studio-references__columns {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(18rem, 1fr));
		gap: 0.5rem 1.5rem;
	}

	.studio-references__columns--loading {
		opacity: 0.6;
	}

	.studio-references__section {
		margin: 0;
		color: var(--studio-text);
		padding: 0.2rem 0.85rem;
		font-size: 0.7rem;
		font-weight: 700;
		letter-spacing: 0.02em;
		text-transform: uppercase;
	}

	.studio-references__empty {
		margin: 0;
		color: var(--studio-text-idle);
		padding: 0.2rem 0.85rem;
		font-size: 0.75rem;
	}

	.studio-references__row {
		display: flex;
		width: 100%;
		min-width: 0;
		align-items: center;
		gap: 0.35rem;
		border: 0;
		background: transparent;
		color: var(--studio-text);
		padding: 0.15rem 0.85rem 0.15rem 1.9rem;
		font-size: 0.78rem;
		text-align: left;
	}

	.studio-references__row--project {
		padding-left: 0.85rem;
		font-weight: 600;
	}

	.studio-references__row:hover {
		background: var(--studio-hover-bg);
	}

	.studio-references__row small {
		display: inline-flex;
		min-width: 0;
		align-items: center;
		gap: 0.2rem;
		overflow: hidden;
		color: var(--studio-text-idle);
		font-size: 0.7rem;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
</style>
