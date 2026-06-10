<script>
	import InputGroup from '$lib/common/components/InputGroup.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { loadStepPaletteGroups } from './palette';

	/** @type {{ parentId?: string }} */
	let { parentId = '' } = $props();

	/** @type {import('./types').PaletteGroup[]} */
	let groups = $state([]);
	let loading = $state(true);
	let error = $state('');
	let query = $state('');

	$effect(() => {
		const id = parentId;
		if (!id) {
			groups = [];
			loading = false;
			error = '';
			return;
		}
		let cancelled = false;
		loading = true;
		error = '';
		loadStepPaletteGroups(id)
			.then((nextGroups) => {
				if (!cancelled) {
					groups = nextGroups;
				}
			})
			.catch((err) => {
				if (!cancelled) {
					error = String(err instanceof Error ? err.message : err);
				}
			})
			.finally(() => {
				if (!cancelled) {
					loading = false;
				}
			});
		return () => {
			cancelled = true;
		};
	});

	let filteredGroups = $derived.by(() => {
		const needle = query.trim().toLowerCase();
		if (!needle) {
			return groups;
		}
		return groups
			.map((group) => ({
				...group,
				items: group.items.filter((item) =>
					`${group.name} ${item.label} ${item.type}`.toLowerCase().includes(needle)
				)
			}))
			.filter((group) => group.items.length > 0);
	});

	/**
	 * @param {import('./types').PaletteItem} item
	 * @returns {string}
	 */
	function portsLabel(item) {
		const sideInputs = item.inputs ?? 0;
		const sideOutputs = item.outputs ?? 0;
		const bottomPorts = (item.bottomInputs ?? 0) + (item.bottomOutputs ?? 0);
		return bottomPorts
			? `${sideInputs}/${sideOutputs} + ${bottomPorts}`
			: `${sideInputs}/${sideOutputs}`;
	}
</script>

<section class="flow-palette">
	<div class="flow-palette__header">
		<span>Palette</span>
		<span class="flow-palette__count">{filteredGroups.length}</span>
	</div>
	<InputGroup
		id="flow-palette-search"
		type="search"
		placeholder="Search component..."
		class="w-full"
		icon="mdi:magnify"
		bind:value={query}
	/>
	<div class="flow-palette__content">
		{#if loading}
			<AutoPlaceholder loading={true} />
			<AutoPlaceholder class="h-4 w-32" loading={true} />
		{:else if error}
			<div class="flow-palette__error">{error}</div>
		{:else}
			{#each filteredGroups as group, groupIndex (group.name)}
				<details class="flow-palette__group" open={groupIndex < 2 || Boolean(query)}>
					<summary class="flow-palette__group-header">
						<span>{group.name}</span>
						<span>{group.items.length}</span>
					</summary>
					<div class="flow-palette__items">
						{#each group.items as item (item.type)}
							<button type="button" class="flow-palette__item" title={item.type}>
								<span class="flow-palette__swatch" style={`background: ${item.color}`}></span>
								<span class="flow-palette__item-main">
									<span class="flow-palette__item-name">{item.label}</span>
									<span class="flow-palette__item-type">{item.type}</span>
								</span>
								<span class="flow-palette__ports" title="inputs/outputs">
									<Ico icon="mdi:source-branch" size={3} />
									{portsLabel(item)}
								</span>
							</button>
						{/each}
					</div>
				</details>
			{/each}
		{/if}
	</div>
</section>

<style>
	.flow-palette {
		display: flex;
		min-height: 0;
		flex: 1 1 auto;
		flex-direction: column;
		gap: 0.65rem;
		border-top: 1px solid var(--color-surface-200-800);
		overflow: hidden;
		padding-top: 0.75rem;
	}

	.flow-palette__header,
	.flow-palette__group-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 0.75rem;
	}

	.flow-palette__header {
		color: var(--color-surface-700-300);
		font-size: 0.8rem;
		font-weight: 700;
		text-transform: uppercase;
	}

	.flow-palette__count,
	.flow-palette__group-header span:last-child,
	.flow-palette__ports {
		border: 1px solid var(--color-surface-200-800);
		border-radius: 999px;
		background: var(--color-surface-50-950);
		color: var(--color-surface-600-400);
		font-size: 0.68rem;
		font-weight: 650;
		line-height: 1;
		padding: 0.25rem 0.45rem;
	}

	.flow-palette__content {
		display: flex;
		min-height: 0;
		flex: 1 1 auto;
		flex-direction: column;
		gap: 0.45rem;
		overflow-x: hidden;
		overflow-y: auto;
		padding-right: 0.15rem;
		scrollbar-gutter: stable;
	}

	.flow-palette__group {
		flex: 0 0 auto;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: color-mix(in oklab, var(--color-surface-50-950) 70%, transparent);
		overflow: hidden;
	}

	.flow-palette__group-header {
		cursor: pointer;
		list-style: none;
		padding: 0.55rem 0.65rem;
		color: var(--color-surface-900-100);
		font-size: 0.78rem;
		font-weight: 700;
	}

	.flow-palette__group-header::-webkit-details-marker {
		display: none;
	}

	.flow-palette__items {
		display: flex;
		flex-direction: column;
		gap: 0.25rem;
		border-top: 1px solid var(--color-surface-200-800);
		padding: 0.35rem;
	}

	.flow-palette__item {
		display: grid;
		grid-template-columns: auto minmax(0, 1fr) auto;
		align-items: center;
		gap: 0.5rem;
		width: 100%;
		border: 1px solid transparent;
		border-radius: 0.4rem;
		background: transparent;
		color: var(--color-surface-900-100);
		padding: 0.45rem;
		text-align: left;
	}

	.flow-palette__item:hover {
		border-color: color-mix(in oklab, var(--color-primary-500) 32%, transparent);
		background: color-mix(in oklab, var(--color-primary-500) 8%, transparent);
	}

	.flow-palette__swatch {
		width: 0.75rem;
		height: 0.75rem;
		border-radius: 999px;
		box-shadow: 0 0 0 2px var(--color-surface-50-950);
	}

	.flow-palette__item-main {
		min-width: 0;
	}

	.flow-palette__item-name,
	.flow-palette__item-type {
		display: block;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.flow-palette__item-name {
		font-size: 0.78rem;
		font-weight: 650;
	}

	.flow-palette__item-type {
		margin-top: 0.08rem;
		color: var(--color-surface-600-400);
		font-size: 0.67rem;
	}

	.flow-palette__ports {
		display: inline-flex;
		align-items: center;
		gap: 0.2rem;
	}

	.flow-palette__error {
		color: var(--color-error-600-400);
		font-size: 0.78rem;
	}
</style>
