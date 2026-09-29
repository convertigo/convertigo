<script>
	import Projects from '$lib/common/Projects.svelte.js';
	import { createProjectTree } from '$lib/common/ProjectsTree.svelte.js';
	import Ico from '$lib/utils/Ico.svelte';
	import { call, runStudioContextAction, toaster } from '$lib/utils/service';
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
	import {
		loadTreeDiff,
		projectOfNode,
		scheduleTreeDiffRefresh,
		setTreeDiffEnabled,
		setTreeDiffRef,
		treeDiff
	} from './treeDiff.svelte.js';
	import {
		describeOperation,
		gitEvents,
		loadTreeMerge,
		notifyGitChange,
		resolveAllConflicts,
		treeMerge
	} from './treeMerge.svelte.js';
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
	 *  onChooseRenameUpdate?: (request: { id: string, objectType: string, oldName: string, newName: string }) => Promise<string | null>,
	 *  onMergeEnded?: (projectName: string, result: any) => void | Promise<void>
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
		onChooseRenameUpdate,
		onMergeEnded
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
	 * @type {Record<string, { project?: string, branch: string, changes: number, ahead?: number, behind?: number, merging?: boolean }>}
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
		// a merge of a project stopped on conflicts, merged object by object in the tree
		const merging = Object.entries(gitDecorations)
			.filter(([, decoration]) => decoration?.merging)
			.map(([name]) => name);
		untrack(() => {
			for (const name of merging) {
				if (!treeMerge.projects[name]) {
					void loadTreeMerge(name).then(() => {
						const ids = Object.keys(treeMerge.projects[name]?.ancestors ?? {});
						keepExpanded(ids);
					});
				}
			}
			for (const name of Object.keys(treeMerge.projects)) {
				if (!merging.includes(name)) {
					delete treeMerge.projects[name];
				}
			}
		});
	});

	// the Source control view changed a repository: the Git decorations show again
	$effect(() => {
		void gitEvents.serial;
		untrack(() => refreshGitDecorations());
	});

	let mergeListOpen = $state(true);
	let mergeBusy = $state('');

	/**
	 * Ends the resolution of the conflicts of a project: written, then a merge is ready to commit, a
	 * cherry-pick or a revert commits, a rebase continues and may stop at a next commit; or the operation is
	 * abandoned, or a rebase skips the commit it stopped at.
	 * @param {string} projectName
	 * @param {'complete' | 'abort' | 'skip'} action
	 */
	async function endMerge(projectName, action) {
		const merge = treeMerge.projects[projectName];
		const operation = describeOperation(merge?.operation);
		if (
			action === 'abort' &&
			!window.confirm(
				`Abort: ${operation.title}? The project and its files are given back as they were before.`
			)
		) {
			return;
		}
		if (
			action === 'skip' &&
			!window.confirm(
				`Skip the commit ${merge?.operation?.commit?.id ?? ''}? The rebased branch will not have its changes.`
			)
		) {
			return;
		}
		mergeBusy = action;
		try {
			const result =
				action === 'skip'
					? await call('studio.git.SourceControl', { projectName, action: 'skip' })
					: await loadTreeMerge(projectName, { action });
			if (!result || (!result.completed && !result.aborted && !('repository' in result))) {
				return;
			}
			const reloaded = Array.isArray(result.reloadedProjects) ? result.reloadedProjects : [];
			for (const name of new Set([projectName, ...reloaded])) {
				await reloadProjectBranches(name);
			}
			refreshGitDecorations();
			notifyGitChange(
				projectName,
				result.completed && merge?.kind === 'merge' ? String(result.commitMessage ?? '') : undefined
			);
			await onMergeEnded?.(projectName, result);
			if (result.merging) {
				// the rebase goes on and stops at a next commit
				const next = await loadTreeMerge(projectName);
				keepExpanded(Object.keys(treeMerge.projects[projectName]?.ancestors ?? {}));
				const described = describeOperation(next?.operation);
				toaster.warning({
					title: `${described.title}: new conflicts`,
					description: described.detail
				});
				return;
			}
			delete treeMerge.projects[projectName];
			const stopped = result.operation;
			if (action === 'abort') {
				toaster.info({
					title: 'Aborted',
					description: `${projectName} is back as it was before: ${operation.title.toLowerCase()} is abandoned.`
				});
			} else if (result.result === 'EMPTY') {
				toaster.info({
					title: 'Nothing left to commit',
					description: `Once resolved, ${operation.title.toLowerCase()} changes nothing: it is dropped.`
				});
			} else if (stopped?.kind === 'rebase' && result.result === 'NOTHING_TO_COMMIT') {
				toaster.warning({
					title: 'Nothing left to commit',
					description: `The commit ${stopped.commit?.id ?? ''} has no change once resolved: skip it in the Source control view.`
				});
			} else if (stopped?.kind === 'rebase' && result.result === 'EDIT') {
				toaster.info({
					title: 'Rebase stopped to edit a commit',
					description: 'Change the project, commit or amend, then continue the rebase in the Source control view.'
				});
			} else if (stopped && stopped.conflicts > 0) {
				toaster.info({
					title: 'Conflicts left in other files',
					description: `${projectName} is resolved; the operation goes on once the other conflicts are resolved.`
				});
			} else {
				toaster.success({
					title: operation.done,
					description:
						merge?.kind === 'merge'
							? `The merged objects of ${projectName} are written and added: commit them in the Source control view.`
							: `${projectName} is written and loaded again.`
				});
			}
		} finally {
			mergeBusy = '';
		}
	}

	/** the commits the Git mode compares to: the branches of the repository of the project selected */
	let diffRefs = $state(/** @type {string[]} */ ([]));
	let diffRefsProject = '';

	$effect(() => {
		// the Git mode compares the project selected and the projects opened in the tree
		if (!treeDiff.enabled) {
			return;
		}
		const names = new Set([
			projectOfNode(selectedId),
			...[...expandedNodeIds].filter((id) => id && !/[.:/]/.test(String(id)))
		]);
		untrack(() => {
			for (const name of names) {
				if (isTreeProject(name) && !treeDiff.projects[name]) {
					void loadTreeDiff(name);
				}
			}
		});
	});

	$effect(() => {
		// the changes of the projects change their comparison
		void refreshSerial;
		void dataSerial;
		void refreshMutationSerial;
		untrack(scheduleTreeDiffRefresh);
	});

	$effect(() => {
		// the objects changed show, the objects that hold them open
		if (!treeDiff.enabled || !treeDiff.changedOnly) {
			return;
		}
		const ids = Object.values(treeDiff.projects).flatMap((diff) => Object.keys(diff.ancestors));
		untrack(() => keepExpanded(ids));
	});

	/**
	 * @param {string} name
	 * @returns {boolean} whether the tree shows the project, the selection being maybe an item of the palette
	 */
	function isTreeProject(name) {
		return Boolean(name) && rootChildren.some((node) => node?.id === name && !node?.closed);
	}

	$effect(() => {
		const projectName = treeDiff.enabled ? projectOfNode(selectedId) : '';
		if (!isTreeProject(projectName) || projectName === diffRefsProject) {
			return;
		}
		diffRefsProject = projectName;
		untrack(async () => {
			const result = await call('studio.git.SourceControl', {
				projectName,
				action: 'branches'
			}).catch(() => null);
			if (diffRefsProject === projectName) {
				diffRefs = [...(result?.local ?? []), ...(result?.remoteBranches ?? [])].map(String);
			}
		});
	});

	let diffProject = $derived(
		treeDiff.enabled ? treeDiff.projects[projectOfNode(selectedId)] : undefined
	);

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
			tree.style.setProperty(
				'--studio-tree-view-width',
				`${tree.parentElement?.clientWidth || tree.clientWidth}px`
			);
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
	{#each Object.entries(treeMerge.projects) as [projectName, merge] (projectName)}
		{@const operation = describeOperation(merge.operation ?? { kind: merge.kind, ours: merge.ours, theirs: merge.theirs })}
		<div
			class="studio-tree-diff studio-tree-merge"
			role="region"
			aria-label="Conflicts of {projectName}"
		>
			<div class="studio-tree-merge__head">
				<Ico icon={operation.icon} size={3.6} />
				<strong>{operation.title}</strong>
				<span>in {projectName}</span>
				<span
					class={[
						'studio-tree-merge__count',
						merge.unresolved ? 'studio-tree-merge__count--open' : 'studio-tree-merge__count--done'
					]}
					>{merge.unresolved
						? `${merge.unresolved} conflict${merge.unresolved > 1 ? 's' : ''}`
						: 'no conflict left'}</span
				>
				<span class="studio-tree-merge__auto">· {merge.changes.length} merged automatically</span>
				<button
					type="button"
					class="studio-tree-diff__button"
					title={mergeListOpen ? 'Hide the conflicts' : 'Show the conflicts'}
					aria-label={mergeListOpen ? 'Hide the conflicts' : 'Show the conflicts'}
					aria-expanded={mergeListOpen}
					onclick={() => (mergeListOpen = !mergeListOpen)}
				>
					<Ico icon={mergeListOpen ? 'mdi:chevron-up' : 'mdi:chevron-down'} size={3.6} />
				</button>
			</div>
			{#if operation.detail}
				<span class="studio-tree-merge__detail" title={operation.detail}>{operation.detail}</span>
			{/if}
			{#if merge.error}
				<span class="studio-tree-diff__counts--error" title={merge.error}>{merge.error}</span>
			{/if}
			{#if mergeListOpen && merge.conflicts.length}
				<ul class="studio-tree-merge__list">
					{#each merge.conflicts as conflict (conflict.id)}
						<li>
							<button
								type="button"
								class="studio-tree-merge__conflict"
								class:studio-tree-merge__conflict--resolved={Boolean(conflict.resolution)}
								title={conflict.description}
								onclick={() => {
									if (conflict.objectId) {
										selectedId = conflict.objectId;
									}
								}}
							>
								<span class="studio-tree-merge__mark">{conflict.resolution ? '✓' : 'C'}</span>
								<span class="studio-tree-merge__name">{conflict.name}</span>
								<span class="studio-tree-merge__what"
									>{conflict.label ?? conflict.description}{conflict.resolution
										? ` · ${conflict.resolution}`
										: ''}</span
								>
							</button>
						</li>
					{/each}
				</ul>
			{/if}
			<div class="studio-tree-merge__actions">
				<button
					type="button"
					class="studio-tree-merge__action"
					title={merge.ours ? `Keep the version of ${merge.ours}` : 'Keep my version'}
					disabled={!merge.unresolved || Boolean(mergeBusy)}
					onclick={() => void resolveAllConflicts(projectName, 'mine')}>Keep all mine</button
				>
				<button
					type="button"
					class="studio-tree-merge__action"
					title={merge.theirs ? `Take the version of ${merge.theirs}` : 'Take their version'}
					disabled={!merge.unresolved || Boolean(mergeBusy)}
					onclick={() => void resolveAllConflicts(projectName, 'theirs')}>Take all theirs</button
				>
				<span class="studio-tree-merge__spacer"></span>
				{#if merge.operation?.canSkip}
					<button
						type="button"
						class="studio-tree-merge__action"
						title="Skip the commit the rebase stopped at, and go on with the next ones"
						disabled={Boolean(mergeBusy)}
						onclick={() => void endMerge(projectName, 'skip')}>Skip</button
					>
				{/if}
				<button
					type="button"
					class="studio-tree-merge__action"
					disabled={Boolean(mergeBusy)}
					onclick={() => void endMerge(projectName, 'abort')}>Abort</button
				>
				<button
					type="button"
					class="studio-tree-merge__action studio-tree-merge__action--complete"
					disabled={merge.unresolved > 0 || Boolean(mergeBusy)}
					title={merge.unresolved
						? 'Resolve the conflicts first'
						: merge.kind === 'merge'
							? 'Write the merged project, ready to commit'
							: 'Write the project resolved and go on'}
					onclick={() => void endMerge(projectName, 'complete')}
					>{mergeBusy === 'complete' ? 'Completing…' : operation.complete}</button
				>
			</div>
		</div>
	{/each}
	{#if treeDiff.enabled}
		<div class="studio-tree-diff" role="toolbar" aria-label="Changes since a commit">
			<Ico icon="mdi:source-branch" size={3.6} />
			<span>Changes since</span>
			<select
				class="studio-tree-diff__ref"
				aria-label="Commit compared to"
				value={treeDiff.ref}
				onchange={(event) => setTreeDiffRef(event.currentTarget.value)}
			>
				<option value="HEAD">HEAD</option>
				{#if treeDiff.ref !== 'HEAD' && !diffRefs.includes(treeDiff.ref)}
					<option value={treeDiff.ref}>{treeDiff.ref}</option>
				{/if}
				{#each diffRefs.filter((ref) => ref !== 'HEAD') as ref (ref)}
					<option value={ref}>{ref}</option>
				{/each}
			</select>
			{#if diffProject?.loading && !diffProject.changes.length}
				<span class="studio-tree-diff__counts">Comparing…</span>
			{:else if diffProject?.error}
				<span
					class="studio-tree-diff__counts studio-tree-diff__counts--error"
					title={diffProject.error}>{diffProject.error}</span
				>
			{:else if diffProject && !diffProject.repository}
				<span class="studio-tree-diff__counts">Not in a Git repository</span>
			{:else if diffProject}
				<span
					class="studio-tree-diff__counts"
					title={diffProject.commit
						? `${diffProject.commit.shortId} ${diffProject.commit.subject}`
						: 'No commit yet'}
				>
					<span class="studio-tree-diff__count--added">{diffProject.counts.added} added</span> ·
					<span class="studio-tree-diff__count--modified"
						>{diffProject.counts.modified} modified</span
					>
					·
					<span class="studio-tree-diff__count--removed">{diffProject.counts.removed} removed</span>
				</span>
			{/if}
			<label class="studio-tree-diff__only">
				<input type="checkbox" bind:checked={treeDiff.changedOnly} />
				Changed only
			</label>
			<button
				type="button"
				class="studio-tree-diff__button"
				title="Compare again"
				aria-label="Compare again"
				onclick={() => Object.keys(treeDiff.projects).forEach((name) => void loadTreeDiff(name))}
			>
				<Ico icon="mdi:reload" size={3.6} />
			</button>
			<button
				type="button"
				class="studio-tree-diff__button"
				title="Leave the Git mode"
				aria-label="Leave the Git mode"
				onclick={() => setTreeDiffEnabled(false)}
			>
				<Ico icon="mdi:close" size={3.6} />
			</button>
		</div>
	{/if}
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
	/* the Git mode: what the tree is compared to, and its changes */
	.studio-tree-diff {
		position: sticky;
		z-index: 2;
		top: 0;
		left: 0;
		box-sizing: border-box;
		width: var(--studio-tree-view-width, 100%);
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 0.35rem 0.5rem;
		margin: -0.35rem -0.35rem 0.25rem;
		border-bottom: 1px solid var(--studio-line, var(--color-surface-200-800));
		background: var(--studio-panel-bg, var(--color-surface-50-950));
		color: var(--studio-text-idle);
		padding: 0.35rem 0.55rem;
		font-size: 0.72rem;
	}

	/* the merge of a project stopped on conflicts */
	.studio-tree-merge {
		display: grid;
		gap: 0.35rem;
		border-bottom-color: color-mix(in oklab, #d32f2f 40%, var(--studio-line));
		background: color-mix(
			in oklab,
			#d32f2f 6%,
			var(--studio-panel-bg, var(--color-surface-50-950))
		);
	}

	.studio-tree-merge__head {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 0.3rem;
		color: var(--studio-text);
	}

	.studio-tree-merge__head strong {
		color: var(--studio-text-strong);
	}

	.studio-tree-merge__count--open {
		color: light-dark(#c62828, #ef9a9a);
		font-weight: 700;
	}

	.studio-tree-merge__count--done {
		color: light-dark(#2e7d32, #81c784);
		font-weight: 700;
	}

	.studio-tree-merge__detail {
		overflow: hidden;
		padding-left: 1.3rem;
		color: var(--studio-text-idle);
		font-size: 0.72rem;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-tree-merge__auto {
		color: var(--studio-text-idle);
	}

	.studio-tree-merge__head .studio-tree-diff__button {
		margin-left: auto;
	}

	.studio-tree-merge__list {
		display: grid;
		max-height: 9rem;
		overflow: auto;
		margin: 0;
		padding: 0;
		list-style: none;
	}

	.studio-tree-merge__conflict {
		display: flex;
		width: 100%;
		min-width: 0;
		align-items: center;
		gap: 0.4rem;
		border: 0;
		border-radius: 0.3rem;
		background: transparent;
		color: var(--studio-text);
		padding: 0.15rem 0.3rem;
		text-align: left;
	}

	.studio-tree-merge__conflict:hover {
		background: var(--studio-hover-bg);
	}

	.studio-tree-merge__mark {
		display: inline-grid;
		flex: none;
		width: 1rem;
		height: 1rem;
		place-items: center;
		border-radius: 0.2rem;
		background: color-mix(in oklab, #d32f2f 28%, transparent);
		color: light-dark(#b71c1c, #ff8a80);
		font-size: 0.62rem;
		font-weight: 800;
	}

	.studio-tree-merge__conflict--resolved .studio-tree-merge__mark {
		background: color-mix(in oklab, #2e7d32 20%, transparent);
		color: light-dark(#2e7d32, #81c784);
	}

	.studio-tree-merge__name {
		flex: none;
		color: var(--studio-text-strong);
	}

	.studio-tree-merge__what {
		overflow: hidden;
		color: var(--studio-text-idle);
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-tree-merge__actions {
		display: flex;
		flex-wrap: wrap;
		gap: 0.3rem;
	}

	.studio-tree-merge__spacer {
		flex: 1;
	}

	.studio-tree-merge__action {
		height: 1.5rem;
		border: 1px solid var(--studio-line, var(--color-surface-200-800));
		border-radius: 0.3rem;
		background: transparent;
		color: var(--studio-text-strong);
		padding: 0 0.5rem;
		font-size: 0.72rem;
	}

	.studio-tree-merge__action:not(:disabled):hover {
		background: var(--studio-hover-bg);
	}

	.studio-tree-merge__action:disabled {
		opacity: 0.45;
	}

	.studio-tree-merge__action--complete:not(:disabled) {
		border-color: transparent;
		background: color-mix(in oklab, #2e7d32 26%, transparent);
		color: light-dark(#1b5e20, #a5d6a7);
		font-weight: 700;
	}

	.studio-tree-diff__ref {
		height: 1.5rem;
		border: 1px solid var(--studio-line, var(--color-surface-200-800));
		border-radius: 0.3rem;
		background-color: transparent;
		color: var(--studio-text-strong);
		padding: 0 1.4rem 0 0.35rem;
		background-position: right 0.1rem center;
		background-size: 1rem 1rem;
		font-size: 0.72rem;
		field-sizing: content;
	}

	.studio-tree-diff__counts {
		color: var(--studio-text);
	}

	.studio-tree-diff__counts--error {
		overflow: hidden;
		max-width: 14rem;
		color: var(--color-error-600-400);
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-tree-diff__count--added {
		color: light-dark(#2e7d32, #81c784);
	}

	.studio-tree-diff__count--modified {
		color: light-dark(#b26a00, #e2c08d);
	}

	.studio-tree-diff__count--removed {
		color: light-dark(#c62828, #ef9a9a);
	}

	.studio-tree-diff__only {
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
		margin-left: auto;
		cursor: pointer;
	}

	.studio-tree-diff__button {
		display: inline-grid;
		width: 1.5rem;
		height: 1.5rem;
		place-items: center;
		border: 0;
		border-radius: 0.3rem;
		background: transparent;
		color: inherit;
	}

	.studio-tree-diff__button:hover {
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

	.studio-tree {
		display: grid;
		width: max-content;
		min-width: 100%;
		gap: 0.08rem;
		padding: 0.35rem;
	}
</style>
