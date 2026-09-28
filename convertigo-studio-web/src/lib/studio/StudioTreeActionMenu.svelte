<script>
	import { Menu, Portal } from '@skeletonlabs/skeleton-svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { getStudioContextMenu, runStudioContextAction } from '$lib/utils/service';
	import { untrack } from 'svelte';

	/**
	 * @typedef {Object} StudioContextMenuItem
	 * @property {string} id
	 * @property {string=} label
	 * @property {string=} description
	 * @property {string=} group
	 * @property {boolean=} enabled
	 * @property {Record<string, any>=} payload
	 * @property {string=} confirm
	 * @property {string=} icon
	 * @property {string=} clientAction
	 */
	/**
	 * @type {{
	 *  nodeId: string,
	 *  label?: string,
	 *  canRename?: boolean,
	 *  canDelete?: boolean,
	 *  canShowInFrontend?: boolean,
	 *  canRevealInPalette?: boolean,
	 *  canRevealDefinition?: boolean,
	 *  canCopy?: boolean,
	 *  canPaste?: boolean,
	 *  isProject?: boolean,
	 *  closed?: boolean,
	 *  contextRequest?: { x: number, y: number, serial: number } | null,
	 *  fileKind?: '' | 'root' | 'folder' | 'file',
	 *  enabledState?: boolean,
	 *  deleting?: boolean,
	 *  onSelectNode?: () => void,
	 *  onRename?: () => void,
	 *  onEditComment?: () => void,
	 *  onDelete?: () => void | Promise<void>,
	 *  onShowInFrontend?: () => void | Promise<void>,
	 *  onRevealInPalette?: () => void | Promise<void>,
	 *  onRevealDefinition?: () => void | Promise<void>,
	 *  onOpenSource?: () => void | Promise<void>,
	 *  onContextAction?: (event: { nodeId: string, action: StudioContextMenuItem, result: any }) => void | Promise<void>,
	 *  onTreeAction?: (action: string) => void | Promise<void>
	 * }}
	 */
	let {
		nodeId,
		label = 'object',
		canRename = false,
		canDelete = false,
		canShowInFrontend = false,
		canRevealInPalette = false,
		canRevealDefinition = false,
		canCopy = false,
		canPaste = false,
		isProject = false,
		closed = false,
		contextRequest = null,
		fileKind = '',
		enabledState = undefined,
		deleting = false,
		onSelectNode,
		onRename,
		onEditComment,
		onDelete,
		onShowInFrontend,
		onRevealInPalette,
		onRevealDefinition,
		onOpenSource,
		onContextAction,
		onTreeAction
	} = $props();

	const mod =
		typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform) ? '⌘' : 'Ctrl+';
	/** The actions of the tree the page runs, with the shortcuts of the tree */
	const treeActionShortcuts = {
		'edit.cut': `${mod}X`,
		'edit.copy': `${mod}C`,
		'edit.paste': `${mod}V`,
		'object.rename': 'F2',
		'object.delete': 'Del',
		'file.rename': 'F2',
		'file.delete': 'Del',
		'project.save': `${mod}S`
	};
	/**
	 * @param {string} value
	 * @returns {string | undefined} the shortcut of a tree action, as aria-keyshortcuts names it
	 */
	function ariaShortcut(value) {
		const shortcut = treeActionShortcuts[/** @type {keyof typeof treeActionShortcuts} */ (value)];
		if (!shortcut) {
			return undefined;
		}
		if (shortcut === 'Del') {
			return 'Delete';
		}
		const key = shortcut.slice(mod.length);
		return shortcut.startsWith(mod) ? `Control+${key} Meta+${key}` : shortcut;
	}
	let editable = $derived(canCopy || canRename || canDelete || canPaste);

	let open = $state(false);
	let loading = $state(false);
	let loadError = $state('');
	let actionBusy = $state('');
	/** @type {boolean | null} */
	let devRunningOverride = $state(null);
	/** @type {StudioContextMenuItem[]} */
	let contextItems = $state.raw([]);
	let requestSerial = 0;
	let groupedContextItems = $derived(groupByContextMenuGroup(contextItems));
	let triggerIcon = $derived(actionBusy || deleting ? 'mdi:sync' : 'mdi:dots-vertical');

	/**
	 * The point of the right click the menu opens at, as a context menu, instead of below its button
	 * @type {{ x: number, y: number } | null}
	 */
	let contextPoint = $state(null);
	let handledContextSerial = 0;
	$effect(() => {
		const request = contextRequest;
		if (!request || request.serial === handledContextSerial) {
			return;
		}
		handledContextSerial = request.serial;
		// the tree already selected the object, or kept the objects selected together
		untrack(() => {
			contextPoint = { x: request.x, y: request.y };
			open = true;
			contextItems = [];
			void loadContextMenu();
		});
	});
	let positioning = $derived.by(() => {
		const point = contextPoint;
		return point
			? {
					placement: /** @type {const} */ ('bottom-start'),
					getAnchorRect: () => ({ ...point, width: 0, height: 0 })
				}
			: { placement: /** @type {const} */ ('bottom-end') };
	});

	/**
	 * @param {{ open: boolean }} details
	 */
	function handleOpenChange(details) {
		open = details.open;
		if (!open) {
			contextPoint = null;
		}
		if (open) {
			onSelectNode?.();
			contextItems = [];
			void loadContextMenu();
		}
	}

	async function loadContextMenu() {
		if (fileKind || closed) {
			// a file of the project has only the actions on files, a closed project opens or is deleted
			contextItems = [];
			return;
		}
		const serial = ++requestSerial;
		loading = true;
		loadError = '';
		try {
			const response = await getStudioContextMenu(nodeId);
			if (serial !== requestSerial) {
				return;
			}
			const items = Array.isArray(response?.menu?.items)
				? response.menu.items
				: Array.isArray(response?.items)
					? response.items
					: [];
			contextItems = reconcileDevActionState(reconcileClientActionState(items));
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
		if (details.value === 'object.comment') {
			onEditComment?.();
			return;
		}
		if (details.value === 'object.delete') {
			await onDelete?.();
			return;
		}
		if (/^(edit|project|state|file)\./.test(details.value)) {
			await onTreeAction?.(details.value);
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
			if (action.clientAction) {
				await runClientAction(action.clientAction);
				return;
			}
			const result = await runStudioContextAction(nodeId, action);
			// a failed request keeps the state of the development server
			const succeeded = result?.ok !== false && !result?.isError;
			if (succeeded && action.id.endsWith('.dev.start')) {
				devRunningOverride = true;
				contextItems = reconcileDevActionState(contextItems);
			} else if (succeeded && action.id.endsWith('.dev.stop')) {
				devRunningOverride = false;
				contextItems = reconcileDevActionState(contextItems);
			}
			await onContextAction?.({ nodeId, action, result });
		} finally {
			actionBusy = '';
		}
	}

	/**
	 * @param {string} clientAction
	 */
	async function runClientAction(clientAction) {
		if (clientAction === 'frontend.reveal') {
			await onShowInFrontend?.();
		} else if (clientAction === 'palette.reveal') {
			await onRevealInPalette?.();
		} else if (clientAction === 'definition.reveal') {
			await onRevealDefinition?.();
		} else if (clientAction === 'source.open') {
			await onOpenSource?.();
		} else if (/^(dialog|code|picker|execution)\.|^frontend\.execute/.test(clientAction)) {
			// a dialog, an editor or a picker of the page, as the variables of a transaction
			await onTreeAction?.(clientAction);
		}
	}

	/**
	 * Surface capabilities can disable a shared descriptor without changing its
	 * identity, label or grouping across Eclipse and Studio Web.
	 * @param {StudioContextMenuItem[]} items
	 */
	function reconcileClientActionState(items) {
		return items.map((item) => {
			if (item.clientAction === 'frontend.reveal') {
				return { ...item, enabled: item.enabled !== false && canShowInFrontend };
			}
			if (item.clientAction === 'palette.reveal') {
				return { ...item, enabled: item.enabled !== false && canRevealInPalette };
			}
			if (item.clientAction === 'definition.reveal') {
				return { ...item, enabled: item.enabled !== false && canRevealDefinition };
			}
			return item;
		});
	}

	/**
	 * Keep the touch menu coherent immediately after a successful local action.
	 * Isolated Flow runtimes may need one request to observe the persisted state;
	 * the override disappears as soon as the backend reports the same state.
	 * @param {StudioContextMenuItem[]} items
	 */
	function reconcileDevActionState(items) {
		if (devRunningOverride === null) {
			return items;
		}
		const start = items.find((item) => item.id.endsWith('.dev.start'));
		const stop = items.find((item) => item.id.endsWith('.dev.stop'));
		if (start && stop && Boolean(stop.enabled) === devRunningOverride) {
			devRunningOverride = null;
			return items;
		}
		const running = devRunningOverride === true;
		return items.map((item) => {
			if (item.id.endsWith('.dev.start')) {
				return { ...item, enabled: !running };
			}
			if (item.id.endsWith('.dev.stop') || item.id.endsWith('.dev.open')) {
				return { ...item, enabled: running };
			}
			return item;
		});
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
	 * @param {StudioContextMenuItem} item
	 * @returns {string}
	 */
	function contextActionIcon(item) {
		const id = item.id;
		const icon = item.icon || '';
		if (/^[a-z][a-z0-9-]*:[a-z0-9_.-]+$/i.test(icon)) return icon;
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

{#snippet treeItem(
	/** @type {string} */ value,
	/** @type {string} */ icon,
	/** @type {string} */ text,
	/** @type {boolean} */ disabled = false,
	/** @type {boolean} */ danger = false
)}
	<Menu.Item
		{value}
		class={[
			'studio-tree-action-menu__item studio-tree-action-menu__item--shortcut',
			danger && 'studio-tree-action-menu__item--danger'
		]}
		{disabled}
		aria-keyshortcuts={ariaShortcut(value)}
	>
		<Ico {icon} size={4} />
		<Menu.ItemText>{text}</Menu.ItemText>
		<kbd class="studio-tree-action-menu__shortcut" aria-hidden="true"
			>{treeActionShortcuts[/** @type {keyof typeof treeActionShortcuts} */ (value)] ?? ''}</kbd
		>
	</Menu.Item>
{/snippet}

<Menu
	{open}
	onOpenChange={handleOpenChange}
	onSelect={handleSelect}
	{positioning}
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
		<Menu.Positioner class="studio-tree-action-menu__positioner" style="z-index: 180;">
			<Menu.Content class="studio-tree-action-menu__content">
				{#if editable}
					<Menu.ItemGroup>
						<Menu.ItemGroupLabel>Edit</Menu.ItemGroupLabel>
						{#if canCopy}
							{@render treeItem('edit.cut', 'mdi:content-cut', 'Cut')}
							{@render treeItem('edit.copy', 'mdi:content-copy', 'Copy')}
						{/if}
						{@render treeItem('edit.paste', 'mdi:content-paste', 'Paste', !canPaste)}
						{#if canRename}
							{@render treeItem('object.rename', 'mdi:pencil-outline', 'Rename')}
						{/if}
						{#if onEditComment}
							{@render treeItem('object.comment', 'mdi:comment-text-outline', 'Edit comment')}
						{/if}
						{#if enabledState !== undefined}
							{@render treeItem(
								enabledState ? 'state.disable' : 'state.enable',
								enabledState ? 'mdi:close-circle-outline' : 'mdi:check',
								enabledState ? 'Disable' : 'Enable'
							)}
						{/if}
						{#if canDelete}
							{@render treeItem(
								'object.delete',
								deleting ? 'mdi:sync' : 'mdi:delete-outline',
								'Delete',
								deleting,
								true
							)}
						{/if}
					</Menu.ItemGroup>
				{/if}
				{#if fileKind}
					<Menu.ItemGroup>
						<Menu.ItemGroupLabel>Files</Menu.ItemGroupLabel>
						{#if fileKind !== 'file'}
							{@render treeItem('file.newFile', 'mdi:file-outline', 'New file…')}
							{@render treeItem('file.newFolder', 'mdi:folder-plus-outline', 'New folder…')}
							{@render treeItem('file.upload', 'mdi:upload', 'Upload files…')}
						{/if}
						{#if fileKind !== 'root'}
							{@render treeItem('file.rename', 'mdi:pencil-outline', 'Rename…')}
							{@render treeItem('file.delete', 'mdi:delete-outline', 'Delete', false, true)}
						{/if}
					</Menu.ItemGroup>
				{/if}
				{#if isProject && closed}
					<Menu.ItemGroup>
						<Menu.ItemGroupLabel>Project</Menu.ItemGroupLabel>
						{@render treeItem('project.open', 'mdi:folder-open-outline', 'Open')}
						{@render treeItem(
							'project.delete',
							'mdi:delete-outline',
							'Delete project',
							false,
							true
						)}
					</Menu.ItemGroup>
				{:else if isProject}
					{#if editable}
						<Menu.Separator />
					{/if}
					<Menu.ItemGroup>
						<Menu.ItemGroupLabel>Project</Menu.ItemGroupLabel>
						{@render treeItem('project.save', 'mdi:content-save-outline', 'Save')}
						{@render treeItem('project.reload', 'mdi:reload', 'Reload from disk')}
						{@render treeItem('project.close', 'mdi:folder-lock-outline', 'Close')}
						{@render treeItem('project.export', 'mdi:export', 'Export as .car')}
						{@render treeItem('project.deploy', 'mdi:server-network', 'Deploy to a server…')}
						{@render treeItem('project.importWs', 'mdi:web', 'Import a web service…')}
						{@render treeItem('project.dashboard', 'mdi:open-in-new-variant', 'Open in dashboard')}
						{@render treeItem('project.swagger', 'mdi:api', 'Open the Swagger console')}
						{@render treeItem(
							'project.builtApp',
							'mdi:cellphone-link',
							'Open the built application'
						)}
						{@render treeItem(
							'project.readme',
							'mdi:language-markdown-outline',
							'Generate the readme.md'
						)}
						{@render treeItem('project.symbols', 'mdi:code-braces', 'Declare its global symbols')}
						{@render treeItem('project.addFile', 'mdi:file-code-outline', 'Add files…')}
						{@render treeItem('project.statistics', 'mdi:chart-box-outline', 'Statistics')}
						{@render treeItem(
							'project.checkReferences',
							'mdi:source-pull',
							'Check the referenced projects'
						)}
						{@render treeItem('project.remoteUrl', 'mdi:link-variant', 'Copy the remote URL')}
						{@render treeItem(
							'project.convertNgx',
							'mdi:swap-horizontal',
							'Convert the mobile application to NGX…'
						)}
						{@render treeItem(
							'project.delete',
							'mdi:delete-outline',
							'Delete project',
							false,
							true
						)}
					</Menu.ItemGroup>
					<Menu.Separator />
					<Menu.ItemGroup>
						<Menu.ItemGroupLabel>Continuous integration</Menu.ItemGroupLabel>
						{@render treeItem('project.ci:gitlab', 'mdi:source-branch', 'Update GitLab and Gradle')}
						{@render treeItem(
							'project.ci:circleci',
							'mdi:source-branch',
							'Update CircleCI and Gradle'
						)}
						{@render treeItem(
							'project.ci:github-actions',
							'mdi:source-branch',
							'Update GitHub Actions and Gradle'
						)}
						{@render treeItem('project.ci:gradle', 'mdi:source-branch', 'Update Gradle only')}
						{@render treeItem('project.ci:httpignore', 'mdi:file-hidden', 'Update .httpignore')}
					</Menu.ItemGroup>
				{/if}
				{#if (editable || isProject) && (loading || loadError || groupedContextItems.length)}
					<Menu.Separator />
				{/if}

				{#if loading}
					<Menu.Item value="context.loading" disabled class="studio-tree-action-menu__item">
						<Ico icon="mdi:sync" size={4} />
						<Menu.ItemText>Loading actions…</Menu.ItemText>
					</Menu.Item>
				{:else if loadError}
					<Menu.Item value="context.error" disabled class="studio-tree-action-menu__item">
						<Ico icon="mdi:alert-circle-outline" size={4} />
						<Menu.ItemText>Actions unavailable</Menu.ItemText>
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
										icon={actionBusy === item.id ? 'mdi:sync' : contextActionIcon(item)}
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

				{#if !loading && !loadError && !editable && !isProject && !fileKind && !groupedContextItems.length}
					<Menu.Item value="context.empty" disabled class="studio-tree-action-menu__item">
						<Menu.ItemText>No actions available</Menu.ItemText>
					</Menu.Item>
				{/if}
			</Menu.Content>
		</Menu.Positioner>
	</Portal>
</Menu>

<style>
	/* a ghost button in the row, larger for a finger */
	:global(.studio-tree-action-menu__trigger) {
		display: inline-grid;
		width: 1.3rem;
		height: 1.3rem;
		place-items: center;
		border: 1px solid transparent;
		border-radius: 0.3rem;
		background: transparent;
		color: var(--studio-text-idle, var(--color-surface-700-300));
		padding: 0;
	}

	@media (pointer: coarse) {
		:global(.studio-tree-action-menu__trigger) {
			width: 2rem;
			height: 2rem;
		}
	}

	:global(.studio-tree-action-menu__trigger:hover:not(:disabled)),
	:global(.studio-tree-action-menu__trigger:focus-visible:not(:disabled)),
	:global(.studio-tree-action-menu__trigger[data-state='open']) {
		background: color-mix(in oklab, var(--studio-text-strong, currentColor) 12%, transparent);
		color: var(--studio-text-strong, var(--color-primary-700-300));
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

	:global(.studio-tree-action-menu__item--shortcut) {
		grid-template-columns: 1.1rem minmax(0, 1fr) auto;
	}

	:global(.studio-tree-action-menu__shortcut) {
		color: var(--color-surface-500-500);
		font-family: inherit;
		font-size: 0.68rem;
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
