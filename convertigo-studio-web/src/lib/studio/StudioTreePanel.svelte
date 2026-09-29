<script>
	import Projects from '$lib/common/Projects.svelte.js';
	import { createProjectTree } from '$lib/common/ProjectsTree.svelte.js';
	import { call, runStudioContextAction } from '$lib/utils/service';
	import { onMount, tick, untrack } from 'svelte';
	import { SvelteSet } from 'svelte/reactivity';
	import { closedProjects, refreshClosedProjects } from './closedProjects.svelte.js';
	import {
		areEquivalentDboObjectIds,
		equivalentDboObjectIds,
		mutationDboContextIds,
		mutationDboRefreshIds
	} from './dnd';
	import { isFolderId } from './folderTypes.js';
	import StudioEmptyState from './StudioEmptyState.svelte';
	import { applyProjectedTreeMutation, remapExpandedTreeIds } from './studioTreeMutation';
	import StudioTreeNode from './StudioTreeNode.svelte';
	import { treeSelectionOf } from './treeSelection.svelte.js';

	/** the part of the width of the view the column of the comments goes to at most */
	const COMMENT_COLUMN_MAX = 0.6;
	const COMMENT_GAP = 10;

	/**
	 * @type {{
	 *  selectedId?: string,
	 *  hideLibs?: boolean,
	 *  renameTargetId?: string,
	 *  autoSelectFirst?: boolean,
	 *  refreshSerial?: number,
	 *  refreshMutation?: import('./dnd').DboDropResult | null,
	 *  refreshMutationSerial?: number,
	 *  reloadProject?: { projectName: string, serial: number } | null,
	 *  onMutation?: (mutation: import('./dnd').DboDropResult) => void | Promise<void>,
	 *  onMutationBusyChange?: (busy: boolean, handled?: boolean) => void,
	 *  onContextAction?: (event: { nodeId: string, action: any, result: any }) => void | Promise<void>,
	 *  canShowInFrontend?: (nodeId: string) => boolean,
	 *  onShowInFrontend?: (nodeId: string) => void | Promise<void>,
	 *  canRevealInPalette?: (nodeId: string) => boolean,
	 *  onRevealInPalette?: (nodeId: string) => void | Promise<void>,
	 *  canRevealBlockDefinition?: (nodeId: string) => boolean,
	 *  onRevealBlockDefinition?: (nodeId: string) => void | Promise<void>,
	 *  onOpenSource?: (nodeId: string) => void | Promise<void>,
	 *  onSourceDrop?: (targetId: string, payload: import('./sourcePickerDnd').SourcePickerDragPayload) => void | Promise<void>,
	 *  onTreeAction?: (action: string, nodeId: string, options?: { text?: string }) => void | Promise<void>,
	 *  canPasteInto?: (nodeId: string) => boolean,
	 *  onChooseRenameUpdate?: (request: { id: string, objectType: string, oldName: string, newName: string }) => Promise<string | null>
	 * }}
	 */
	let {
		selectedId = $bindable(''),
		hideLibs = false,
		renameTargetId = $bindable(''),
		autoSelectFirst = true,
		refreshSerial = 0,
		refreshMutation = null,
		refreshMutationSerial = 0,
		reloadProject = null,
		onMutation,
		onMutationBusyChange,
		onContextAction,
		canShowInFrontend,
		onShowInFrontend,
		canRevealInPalette,
		onRevealInPalette,
		canRevealBlockDefinition,
		onRevealBlockDefinition,
		onOpenSource,
		onSourceDrop,
		onTreeAction,
		canPasteInto,
		onChooseRenameUpdate
	} = $props();

	const { checkChildren, checkNodes } = createProjectTree({
		equivalentIds: equivalentDboObjectIds
	});
	/** @type {Record<string, any>} */
	const rootNodeCache = {};
	let expandedNodeIds = $state.raw(new SvelteSet());
	let dataSerial = $state(0);
	let lastRefreshMutationSerial = 0;
	/**
	 * The branch and the changed files of each project in a Git repository, as the decorations of the
	 * Eclipse Studio
	 * @type {Record<string, { branch: string, changes: number, ahead?: number, behind?: number }>}
	 */
	let gitDecorations = $state({});
	/** @type {ReturnType<typeof setTimeout> | undefined} */
	let gitDecorationsTimer;

	function refreshGitDecorations() {
		clearTimeout(gitDecorationsTimer);
		gitDecorationsTimer = setTimeout(async () => {
			const result = await call('studio.git.SourceControl', { action: 'decorations' });
			if (Array.isArray(result?.projects)) {
				gitDecorations = Object.fromEntries(
					result.projects.map((/** @type {any} */ decoration) => [decoration.project, decoration])
				);
			}
		}, 800);
	}

	$effect(() => {
		// the saves, reloads and changes of the tree can change the files of the projects
		void refreshSerial;
		void dataSerial;
		void Projects.projects?.length;
		untrack(refreshGitDecorations);
	});

	onMount(() => {
		window.addEventListener('focus', refreshGitDecorations);
		return () => {
			window.removeEventListener('focus', refreshGitDecorations);
			clearTimeout(gitDecorationsTimer);
		};
	});
	$effect(() => {
		// the closed projects, which the list of the projects of the engine leaves out
		void refreshSerial;
		void Projects.projects?.length;
		untrack(() => void refreshClosedProjects());
	});
	let rootChildren = $derived.by(() =>
		[
			...(Projects.projects ?? [])
				.map((project) => project?.name)
				.filter((name) => name && !closedProjects.names.includes(name)),
			...closedProjects.names
		]
			.filter((name) => !(hideLibs && name.startsWith('lib_')))
			// the order of the engine, which ignores the case
			.sort((left, right) => {
				const [a, b] = [left.toLowerCase(), right.toLowerCase()];
				return a < b ? -1 : a > b ? 1 : 0;
			})
			.map((name) => {
				const closed = closedProjects.names.includes(name);
				// a project opened or closed again shows as a new node, without the children it had
				if (!rootNodeCache[name] || Boolean(rootNodeCache[name].closed) !== closed) {
					rootNodeCache[name] = {
						id: name,
						name,
						label: name,
						icon: 'folder',
						children: !closed,
						...(closed ? { closed: true } : {})
					};
				}
				return rootNodeCache[name];
			})
	);
	let loading = $derived(Projects.loading && rootChildren.length === 0);

	onMount(() => {
		let cancelled = false;
		async function selectFirstWhenReady() {
			while (!cancelled && autoSelectFirst && !selectedId) {
				const firstProject = rootChildren.find((node) => node?.id && node.id !== 'ROOT');
				if (firstProject?.id) {
					selectedId = firstProject.id;
					return;
				}
				await new Promise((resolve) => setTimeout(resolve, 120));
			}
		}
		void selectFirstWhenReady();
		return () => {
			cancelled = true;
		};
	});

	$effect(() => {
		const serial = refreshMutationSerial;
		const mutation = refreshMutation;
		if (serial === lastRefreshMutationSerial) {
			return;
		}
		lastRefreshMutationSerial = serial;
		if (!mutation?.done || mutation.source === 'tree') {
			return;
		}
		untrack(() => {
			void refreshMutationContext(serial, mutation);
		});
	});

	/**
	 * The shortcuts of the tree of Eclipse, on its selected object: F2 renames, Del deletes, Ctrl or ⌘
	 * with C, X and V copies, cuts and pastes, with S saves the project.
	 * @param {KeyboardEvent} event
	 */
	function handleTreeKeydown(event) {
		const target = /** @type {HTMLElement | null} */ (event.target);
		if (
			!onTreeAction ||
			!selectedId ||
			renameTargetId ||
			target?.closest('input, textarea, select, [contenteditable]')
		) {
			return;
		}
		const mod = event.metaKey || event.ctrlKey;
		const key = event.key.toLowerCase();
		if (!mod && !event.altKey && event.key.startsWith('Arrow')) {
			event.preventDefault();
			navigate(event.key, /** @type {HTMLElement} */ (event.currentTarget));
			return;
		}
		if (!mod && (event.key === '+' || event.key === '-')) {
			// the priority, as + and - in the tree of the Eclipse Studio
			event.preventDefault();
			void moveSelected(event.key === '+' ? 'object.moveUp' : 'object.moveDown');
			return;
		}
		/** @type {string} */
		let action = '';
		if (!mod && !event.altKey && event.key === 'Enter') {
			// Enter opens the object, as a double-click, or opens and closes a folder
			const open = findNodeById(selectedId)?.open;
			event.preventDefault();
			if (open) {
				void onTreeAction(open, selectedId);
			} else {
				const toggle = /** @type {HTMLElement} */ (event.currentTarget).querySelector(
					`button.studio-tree-node__toggle-button[data-node-id="${CSS.escape(selectedId)}"]`
				);
				/** @type {HTMLButtonElement | null} */ (toggle)?.click();
			}
			return;
		}
		if (event.key === 'F5') {
			// F5 runs the selected requestable or test case and else refreshes the tree, Ctrl+F5 runs the default
			// transaction, as in the Eclipse Studio, instead of reloading the page
			action = mod
				? 'execution.default'
				: /\.tc:[^.:]+$/.test(selectedId)
					? 'execution.testcase'
					: /\.(sq|tr):[^.:]+$/.test(selectedId)
						? 'execution.run'
						: 'tree.refresh';
		} else if (mod && key === 'g' && /\.tr:[^.:]+$/.test(selectedId)) {
			action = 'dialog.handlers';
		} else if (event.key === 'F2') {
			action = 'object.rename';
		} else if (event.key === 'Delete' || (event.key === 'Backspace' && event.metaKey)) {
			action = 'object.delete';
		} else if (mod && !event.altKey && !event.shiftKey && key === 'v') {
			// the browser gives the text of the system clipboard to the paste event that follows, without
			// asking to read the clipboard; the paste runs without it when no such event comes
			const nodeId = selectedId;
			clearTimeout(pendingPaste);
			pendingPaste = setTimeout(() => {
				pendingPaste = undefined;
				void onTreeAction?.('edit.paste', nodeId);
			}, 50);
			return;
		} else if (mod && !event.altKey && !event.shiftKey) {
			action = { c: 'edit.copy', x: 'edit.cut', s: 'project.save' }[key] ?? '';
		}
		if (action) {
			event.preventDefault();
			void onTreeAction(action, selectedId);
		}
	}

	/** @type {ReturnType<typeof setTimeout> | undefined} */
	let pendingPaste;

	/**
	 * Pastes on Ctrl or ⌘ V the objects of the system clipboard, copied by this Studio or another one.
	 * @param {ClipboardEvent} event
	 */
	function handleTreePaste(event) {
		if (pendingPaste === undefined) {
			return;
		}
		clearTimeout(pendingPaste);
		pendingPaste = undefined;
		event.preventDefault();
		void onTreeAction?.('edit.paste', selectedId, {
			text: event.clipboardData?.getData('text/plain') ?? ''
		});
	}

	/**
	 * Moves the selection in the tree with the arrows: up and down to the previous and next rows, right to
	 * expand or go into, left to collapse or go to the parent.
	 * @param {string} key
	 * @param {HTMLElement} container
	 */
	function navigate(key, container) {
		const rows = /** @type {HTMLElement[]} */ ([
			...container.querySelectorAll('button.studio-tree-node__content')
		]);
		const index = rows.findIndex((row) => row.dataset.nodeId === selectedId);
		const current = rows[index];
		const toggle = /** @type {HTMLButtonElement | null} */ (
			current
				? container.querySelector(
						`button.studio-tree-node__toggle-button[data-node-id="${CSS.escape(selectedId)}"]`
					)
				: null
		);
		/** @type {HTMLElement | undefined} */
		let target;
		if (key === 'ArrowDown') {
			target = rows[index + 1] ?? rows[0];
		} else if (key === 'ArrowUp') {
			target = index > 0 ? rows[index - 1] : rows[0];
		} else if (key === 'ArrowRight') {
			if (toggle?.getAttribute('aria-label') === 'Expand') {
				toggle.click();
				return;
			}
			target = rows[index + 1];
		} else if (key === 'ArrowLeft') {
			if (toggle?.getAttribute('aria-label') === 'Collapse') {
				toggle.click();
				return;
			}
			const parent = current
				?.closest('[role="treeitem"]')
				?.parentElement?.closest('[role="treeitem"]');
			target = /** @type {HTMLElement | undefined} */ (
				parent?.querySelector('button.studio-tree-node__content') ?? undefined
			);
		}
		if (target && target !== current) {
			target.click();
			target.focus();
		}
	}

	/**
	 * @param {string} actionId the move of the selected object, up or down
	 */
	async function moveSelected(actionId) {
		// a project, a file or a folder has no priority
		const movable = treeSelectionOf(selectedId).filter(
			(id) => /[.:]/.test(id) && !id.includes('/') && !isFolderId(id)
		);
		// the objects selected together move in the order of the tree: the first ones first when they go up
		const buttons = [...document.querySelectorAll('button.studio-tree-node__content')].map(
			(button) => /** @type {HTMLElement} */ (button).dataset.nodeId
		);
		movable.sort((a, b) => buttons.indexOf(a) - buttons.indexOf(b));
		if (actionId === 'object.moveDown') {
			movable.reverse();
		}
		for (const nodeId of movable) {
			const action = { id: actionId };
			const result = await runStudioContextAction(nodeId, action);
			await onContextAction?.({ nodeId, action, result });
			if (result?.ok === false || result?.isError) {
				return;
			}
		}
	}

	/**
	 * @param {any} node
	 * @param {boolean=} force
	 */
	async function loadChildren(node, force = false) {
		await checkChildren(node, force);
	}

	/**
	 * @param {import('./dnd').DboDropResult} mutation
	 * @param {{ targetParentNode?: any, projectTargetParent?: () => void, clearTargetProjection?: () => void, projectPendingParent?: () => void }=} context
	 */
	async function handleMutation(mutation, context) {
		remapExpandedTreeIds(expandedNodeIds, mutation, equivalentDboObjectIds);
		const contextIds = mutationDboContextIds(mutation);
		keepExpanded(contextIds);
		const targetRoots = context?.targetParentNode ? [context.targetParentNode] : rootChildren;
		let updatedLocally = applyProjectedTreeMutation(
			rootChildren,
			mutation,
			areEquivalentDboObjectIds
		);
		if (!updatedLocally && targetRoots !== rootChildren) {
			updatedLocally = applyProjectedTreeMutation(targetRoots, mutation, areEquivalentDboObjectIds);
		}
		if (updatedLocally) {
			context?.projectTargetParent?.();
			context?.projectPendingParent?.();
			// Flush the confirmed local projection before beginning the slower
			// authoritative tree request. This keeps the DnD feedback independent
			// from virtual-tree reconstruction latency.
			await tick();
		}
		// Register the local mutation before the delayed server event can arrive.
		// The page callback remembers its exact source path synchronously, while
		// its project/flow refresh may continue alongside tree reconciliation.
		const mutationOutcome = Promise.resolve(onMutation?.(mutation)).then(
			() => ({ error: null }),
			(error) => ({ error })
		);
		await refreshAffectedParents(mutationDboRefreshIds(mutation));
		context?.clearTargetProjection?.();
		// The project tree uses raw nested objects. Signal both the optimistic
		// projection and the authoritative replacement performed by checkNodes;
		// otherwise the second assignment can remain invisible until another UI
		// state change happens to reevaluate the branch.
		dataSerial += 1;
		const outcome = await mutationOutcome;
		if (outcome.error) {
			throw outcome.error;
		}
	}

	$effect(() => {
		// an undo or a redo replaced the objects of a project
		const projectName = reloadProject?.projectName;
		if (reloadProject?.serial && projectName) {
			untrack(() => void reloadProjectBranches(projectName));
		}
	});

	/**
	 * Reads again the open branches of a project, whose objects an undo or a redo replaced: a branch
	 * emptied by the change undone has its children again.
	 * @param {string} projectName
	 */
	async function reloadProjectBranches(projectName) {
		const ids = [...expandedNodeIds].filter((id) => String(id).split(/[.:/]/)[0] === projectName);
		await refreshAffectedParents([projectName, ...ids]);
		dataSerial += 1;
		// an object the change undone had created is gone: its nearest parent is selected
		let id = String(selectedId ?? '');
		if (id.split(/[.:/]/)[0] === projectName && !findNodeById(id)) {
			while (id.includes('.') || id.includes(':') || id.includes('/')) {
				id = id.replace(/[.:/]+[^.:/]*$/, '');
				if (findNodeById(id)) {
					break;
				}
			}
			selectedId = id || projectName;
		}
	}

	/**
	 * @param {number} serial
	 * @param {import('./dnd').DboDropResult} mutation
	 */
	async function refreshMutationContext(serial, mutation) {
		remapExpandedTreeIds(expandedNodeIds, mutation, equivalentDboObjectIds);
		const parentIds = mutationDboContextIds(mutation);
		if (!parentIds.length) {
			return;
		}
		keepExpanded(parentIds);
		await refreshAffectedParents(parentIds);
		if (serial === lastRefreshMutationSerial) {
			dataSerial += 1;
		}
	}

	/**
	 * @param {string[]} ids
	 */
	function keepExpanded(ids) {
		if (!ids.length) {
			return;
		}
		let changed = false;
		const nextExpanded = new SvelteSet(expandedNodeIds);
		for (const id of ids) {
			for (const equivalentId of equivalentDboObjectIds(id)) {
				if (equivalentId && !nextExpanded.has(equivalentId)) {
					nextExpanded.add(equivalentId);
					changed = true;
				}
			}
		}
		if (changed) {
			expandedNodeIds = nextExpanded;
		}
	}

	/**
	 * @param {string} id
	 * @param {boolean} nextExpanded
	 */
	function setNodeExpanded(id, nextExpanded) {
		if (!id) {
			return;
		}
		const nextExpandedNodeIds = new SvelteSet(expandedNodeIds);
		if (nextExpanded) {
			for (const equivalentId of equivalentDboObjectIds(id)) {
				nextExpandedNodeIds.add(equivalentId);
			}
		} else {
			for (const expandedId of Array.from(nextExpandedNodeIds)) {
				if (areEquivalentDboObjectIds(expandedId, id)) {
					nextExpandedNodeIds.delete(expandedId);
				}
			}
		}
		expandedNodeIds = nextExpandedNodeIds;
	}

	/**
	 * @param {string[]} ids
	 */
	async function refreshAffectedParents(ids) {
		const visited = new SvelteSet();
		const nodes = [];
		for (const id of ids.filter(Boolean).sort(compareTreeContainerDepth)) {
			if (visited.has(id)) {
				continue;
			}
			visited.add(id);
			const node = findNodeById(id);
			if (node?.id) {
				nodes.push(node);
			}
		}
		await checkNodes(nodes, true);
	}

	/**
	 * @param {string} left
	 * @param {string} right
	 * @returns {number}
	 */
	function compareTreeContainerDepth(left, right) {
		return treeContainerDepth(left) - treeContainerDepth(right);
	}

	/**
	 * @param {string} id
	 * @returns {number}
	 */
	function treeContainerDepth(id) {
		return String(id).split(/[.:/]/).length;
	}

	/**
	 * @param {string} id
	 * @param {any[]=} nodes
	 * @returns {any}
	 */
	function findNodeById(id, nodes = rootChildren) {
		for (const node of nodes) {
			if (node?.id && areEquivalentDboObjectIds(node.id, id)) {
				return node;
			}
			if (Array.isArray(node?.children)) {
				const found = findNodeById(id, node.children);
				if (found) {
					return found;
				}
			}
		}
		return undefined;
	}

	/**
	 * Aligns the comments of the rows shown on a column, after the longest name, which a name too long
	 * for the width of the view goes beyond.
	 * @param {HTMLElement} tree
	 */
	function alignComments(tree) {
		let frame = 0;
		const align = () => {
			frame = 0;
			const comments = /** @type {NodeListOf<HTMLElement>} */ (
				tree.querySelectorAll('.studio-tree-node__comment')
			);
			if (!comments.length) {
				return;
			}
			const origin = tree.getBoundingClientRect().left;
			const ends = [...comments].map(
				(comment) =>
					(comment.previousElementSibling?.getBoundingClientRect().right ?? origin) - origin
			);
			const width = tree.parentElement?.clientWidth || tree.clientWidth;
			const column = Math.min(Math.max(...ends), width * COMMENT_COLUMN_MAX);
			comments.forEach((comment, index) => {
				comment.style.marginLeft = `${Math.max(0, column - ends[index]) + COMMENT_GAP}px`;
			});
		};
		const schedule = () => {
			if (!frame) {
				frame = requestAnimationFrame(align);
			}
		};
		const mutations = new MutationObserver(schedule);
		mutations.observe(tree, { childList: true, subtree: true, characterData: true });
		const resize = new ResizeObserver(schedule);
		if (tree.parentElement) {
			resize.observe(tree.parentElement);
		}
		schedule();
		return () => {
			cancelAnimationFrame(frame);
			mutations.disconnect();
			resize.disconnect();
		};
	}
</script>

<div
	class="studio-tree"
	role="tree"
	aria-label="Projects"
	tabindex="-1"
	onkeydown={handleTreeKeydown}
	onpaste={handleTreePaste}
	{@attach alignComments}
>
	{#if loading}
		<StudioEmptyState message="Loading" loading small />
	{:else if rootChildren.length === 0}
		<StudioEmptyState message="No project available" small />
	{:else}
		{#each rootChildren as node (node.id ?? node.name)}
			<StudioTreeNode
				{node}
				bind:selectedId
				bind:renameTargetId
				depth={0}
				{dataSerial}
				{refreshSerial}
				gitDecoration={gitDecorations[node.id]}
				{expandedNodeIds}
				onSetExpanded={setNodeExpanded}
				onKeepExpanded={keepExpanded}
				onLoadChildren={loadChildren}
				onMutation={handleMutation}
				{onMutationBusyChange}
				{onContextAction}
				{canShowInFrontend}
				{onShowInFrontend}
				{canRevealInPalette}
				{onRevealInPalette}
				{canRevealBlockDefinition}
				{onRevealBlockDefinition}
				{onOpenSource}
				{onSourceDrop}
				{onTreeAction}
				{canPasteInto}
				{onChooseRenameUpdate}
			/>
		{/each}
	{/if}
</div>

<style>
	.studio-tree {
		display: grid;
		width: max-content;
		min-width: 100%;
		gap: 0.08rem;
		padding: 0.35rem;
	}
</style>
