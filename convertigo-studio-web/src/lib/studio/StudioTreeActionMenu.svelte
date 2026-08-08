<script>
	import { Menu, Portal } from '@skeletonlabs/skeleton-svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { getStudioContextMenu, runStudioContextAction } from '$lib/utils/service';

	/**
	 * @typedef {Object} StudioContextMenuItem
	 * @property {string} id
	 * @property {string=} label
	 * @property {string=} description
	 * @property {string=} group
	 * @property {boolean=} enabled
	 * @property {Record<string, any>=} payload
	 * @property {string=} confirm
	 */
	/**
	 * @type {{
	 *  nodeId: string,
	 *  label?: string,
	 *  canRename?: boolean,
	 *  canDelete?: boolean,
	 *  deleting?: boolean,
	 *  onSelectNode?: () => void,
	 *  onRename?: () => void,
	 *  onDelete?: () => void | Promise<void>,
	 *  onContextAction?: (event: { nodeId: string, action: StudioContextMenuItem, result: any }) => void | Promise<void>
	 * }}
	 */
	let {
		nodeId,
		label = 'object',
		canRename = false,
		canDelete = false,
		deleting = false,
		onSelectNode,
		onRename,
		onDelete,
		onContextAction
	} = $props();

	let open = $state(false);
	let loading = $state(false);
	let loadError = $state('');
	let actionBusy = $state('');
	/** @type {StudioContextMenuItem[]} */
	let contextItems = $state.raw([]);
	let requestSerial = 0;
	let groupedContextItems = $derived(groupByContextMenuGroup(contextItems));
	let triggerIcon = $derived(actionBusy || deleting ? 'mdi:sync' : 'mdi:dots-vertical');

	/**
	 * @param {{ open: boolean }} details
	 */
	function handleOpenChange(details) {
		open = details.open;
		if (open) {
			onSelectNode?.();
			contextItems = [];
			void loadContextMenu();
		}
	}

	async function loadContextMenu() {
		const serial = ++requestSerial;
		loading = true;
		loadError = '';
		try {
			const response = await getStudioContextMenu(nodeId);
			if (serial !== requestSerial) {
				return;
			}
			contextItems = Array.isArray(response?.menu?.items)
				? response.menu.items
				: Array.isArray(response?.items)
					? response.items
					: [];
		} catch (error) {
			if (serial === requestSerial) {
				loadError = String(error instanceof Error ? error.message : error);
				contextItems = [];
			}
		} finally {
			if (serial === requestSerial) {
				loading = false;
			}
		}
	}

	/**
	 * @param {{ value: string }} details
	 */
	async function handleSelect(details) {
		if (details.value === 'object.rename') {
			onRename?.();
			return;
		}
		if (details.value === 'object.delete') {
			await onDelete?.();
			return;
		}
		if (!details.value.startsWith('context.')) {
			return;
		}
		const actionId = details.value.slice('context.'.length);
		const action = contextItems.find((item) => item.id === actionId);
		if (!action?.enabled || actionBusy) {
			return;
		}
		if (action.confirm && typeof window !== 'undefined' && !window.confirm(action.confirm)) {
			return;
		}
		actionBusy = action.id;
		try {
			const result = await runStudioContextAction(nodeId, action);
			await onContextAction?.({ nodeId, action, result });
		} finally {
			actionBusy = '';
		}
	}

	/**
	 * @param {StudioContextMenuItem[]} items
	 * @returns {{ name: string, items: StudioContextMenuItem[] }[]}
	 */
	function groupByContextMenuGroup(items) {
		/** @type {{ name: string, items: StudioContextMenuItem[] }[]} */
		const groups = [];
		for (const item of items) {
			const name = String(item?.group || 'Flow');
			let group = groups.find((candidate) => candidate.name === name);
			if (!group) {
				group = { name, items: [] };
				groups.push(group);
			}
			group.items.push(item);
		}
		return groups;
	}

	/**
	 * @param {string} id
	 * @returns {string}
	 */
	function contextActionIcon(id) {
		if (id.endsWith('.dev.start')) return 'mdi:play';
		if (id.endsWith('.dev.stop')) return 'mdi:close-circle-outline';
		if (id.endsWith('.dev.open') || id.endsWith('.openBuilt')) return 'mdi:open-in-new-variant';
		if (id.endsWith('.generate')) return 'mdi:sync';
		if (id.endsWith('.build')) return 'mdi:wrench';
		if (id.endsWith('.disable')) return 'mdi:close-circle-outline';
		if (id.endsWith('.enable')) return 'mdi:check';
		return 'mdi:play-circle-outline';
	}
</script>

<Menu
	{open}
	onOpenChange={handleOpenChange}
	onSelect={handleSelect}
	positioning={{ placement: 'bottom-end' }}
	aria-label={`Actions for ${label}`}
>
	<Menu.Trigger
		type="button"
		class="studio-tree-action-menu__trigger"
		aria-label={`Actions for ${label}`}
		title={`Actions for ${label}`}
		disabled={Boolean(actionBusy || deleting)}
		onclick={(event) => event.stopPropagation()}
	>
		<Ico icon={triggerIcon} size={4} />
	</Menu.Trigger>
	<Portal>
		<Menu.Positioner class="studio-tree-action-menu__positioner">
			<Menu.Content class="studio-tree-action-menu__content">
				{#if canRename || canDelete}
					<Menu.ItemGroup>
						<Menu.ItemGroupLabel>Object</Menu.ItemGroupLabel>
						{#if canRename}
							<Menu.Item value="object.rename" class="studio-tree-action-menu__item">
								<Ico icon="mdi:pencil-outline" size={4} />
								<Menu.ItemText>Rename</Menu.ItemText>
							</Menu.Item>
						{/if}
						{#if canDelete}
							<Menu.Item
								value="object.delete"
								class="studio-tree-action-menu__item studio-tree-action-menu__item--danger"
								disabled={deleting}
							>
								<Ico icon={deleting ? 'mdi:sync' : 'mdi:delete-outline'} size={4} />
								<Menu.ItemText>Delete</Menu.ItemText>
							</Menu.Item>
						{/if}
					</Menu.ItemGroup>
					{#if loading || loadError || groupedContextItems.length}
						<Menu.Separator />
					{/if}
				{/if}

				{#if loading}
					<Menu.Item value="context.loading" disabled class="studio-tree-action-menu__item">
						<Ico icon="mdi:sync" size={4} />
						<Menu.ItemText>Loading Flow actions…</Menu.ItemText>
					</Menu.Item>
				{:else if loadError}
					<Menu.Item value="context.error" disabled class="studio-tree-action-menu__item">
						<Ico icon="mdi:alert-circle-outline" size={4} />
						<Menu.ItemText>Flow actions unavailable</Menu.ItemText>
					</Menu.Item>
				{:else}
					{#each groupedContextItems as group, groupIndex (group.name)}
						{#if groupIndex > 0}
							<Menu.Separator />
						{/if}
						<Menu.ItemGroup>
							<Menu.ItemGroupLabel>{group.name}</Menu.ItemGroupLabel>
							{#each group.items as item (item.id)}
								<Menu.Item
									value={`context.${item.id}`}
									class="studio-tree-action-menu__item"
									disabled={!item.enabled || actionBusy === item.id}
									title={item.description || item.label || item.id}
								>
									<Ico
										icon={actionBusy === item.id ? 'mdi:sync' : contextActionIcon(item.id)}
										size={4}
									/>
									<span class="studio-tree-action-menu__item-copy">
										<Menu.ItemText>{item.label || item.id}</Menu.ItemText>
										{#if item.description}
											<small>{item.description}</small>
										{/if}
									</span>
								</Menu.Item>
							{/each}
						</Menu.ItemGroup>
					{/each}
				{/if}

				{#if !loading && !loadError && !canRename && !canDelete && !groupedContextItems.length}
					<Menu.Item value="context.empty" disabled class="studio-tree-action-menu__item">
						<Menu.ItemText>No actions available</Menu.ItemText>
					</Menu.Item>
				{/if}
			</Menu.Content>
		</Menu.Positioner>
	</Portal>
</Menu>

<style>
	:global(.studio-tree-action-menu__trigger) {
		display: inline-grid;
		width: 2rem;
		height: 2rem;
		place-items: center;
		border: 1px solid color-mix(in oklab, var(--color-primary-500) 32%, transparent);
		border-radius: 0.35rem;
		background: color-mix(in oklab, var(--color-surface-50-950) 90%, transparent);
		color: var(--color-surface-700-300);
		padding: 0;
	}

	:global(.studio-tree-action-menu__trigger:hover:not(:disabled)),
	:global(.studio-tree-action-menu__trigger:focus-visible:not(:disabled)),
	:global(.studio-tree-action-menu__trigger[data-state='open']) {
		border-color: color-mix(in oklab, var(--color-primary-500) 56%, transparent);
		background: color-mix(in oklab, var(--color-primary-500) 14%, var(--color-surface-50-950));
		color: var(--color-primary-700-300);
	}

	:global(.studio-tree-action-menu__positioner) {
		z-index: 180;
	}

	:global(.studio-tree-action-menu__content) {
		width: min(20rem, calc(100vw - 1rem));
		max-height: min(32rem, calc(100vh - 1rem));
		overflow: auto;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.5rem;
		background: var(--color-surface-50-950);
		box-shadow: 0 1rem 2.5rem color-mix(in oklab, black 24%, transparent);
		padding: 0.35rem;
	}

	:global(.studio-tree-action-menu__content [data-part='item-group-label']) {
		color: var(--color-surface-500-500);
		padding: 0.35rem 0.55rem 0.2rem;
		font-size: 0.66rem;
		font-weight: 760;
		letter-spacing: 0.05em;
		text-transform: uppercase;
	}

	:global(.studio-tree-action-menu__item) {
		display: grid;
		grid-template-columns: 1.1rem minmax(0, 1fr);
		align-items: center;
		gap: 0.45rem;
		border-radius: 0.35rem;
		color: var(--color-surface-800-200);
		padding: 0.42rem 0.55rem;
		font-size: 0.76rem;
		cursor: pointer;
	}

	:global(.studio-tree-action-menu__item[data-highlighted]) {
		background: color-mix(in oklab, var(--color-primary-500) 14%, transparent);
		color: var(--color-primary-700-300);
		outline: none;
	}

	:global(.studio-tree-action-menu__item[data-disabled]) {
		cursor: not-allowed;
		opacity: 0.48;
	}

	:global(.studio-tree-action-menu__item--danger:not([data-disabled])) {
		color: var(--color-error-600-400);
	}

	:global(.studio-tree-action-menu__item-copy) {
		display: grid;
		min-width: 0;
		gap: 0.08rem;
	}

	:global(.studio-tree-action-menu__item-copy small) {
		color: var(--color-surface-500-500);
		font-size: 0.66rem;
		font-weight: 480;
		line-height: 1.25;
	}
</style>
