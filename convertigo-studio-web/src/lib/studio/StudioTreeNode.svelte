<script>
	import { dropLabel, isIfStep } from '$lib/studio/flow/flowStepLabels';
	import AutoSvg from '$lib/utils/AutoSvg.svelte';
	import { draggedData } from '$lib/utils/dndStore';
	import Ico from '$lib/utils/Ico.svelte';
	import { getUrl, removeDbo, renameDbo } from '$lib/utils/service';
	import { tick } from 'svelte';
	import {
		canDropDbo,
		canUseDboDropFallback,
		equivalentDboObjectIds,
		getDboDragPayload,
		getDboDropAction,
		isNoopSiblingMove,
		mutationDboContextIds,
		objectNameFromId,
		parentObjectId,
		performDboDrop,
		renameObjectId,
		treeRowDropPosition
	} from './dnd';
	import { getSourcePickerDragPayload } from './sourcePickerDnd';
	import StudioTreeActionMenu from './StudioTreeActionMenu.svelte';
	import { applyProjectedTreeMutation, removeProjectedTreeNode } from './studioTreeMutation';
	import StudioTreeNode from './StudioTreeNode.svelte';

	const folderTypeIds = new Set(['sq', 'cn', 'tr', 'st', 'vr', 'tc', 'ref', 'url', 'app', 'mob']);

	/**
	 * @type {{
	 *  node: any,
	 *  selectedId?: string,
	 *  depth?: number,
	 *  parentNode?: any,
	 *  ancestorDisabled?: boolean,
	 *  renameTargetId?: string,
	 *  dataSerial?: number,
	 *  refreshSerial?: number,
	 *  expandedNodeIds?: Set<string>,
	 *  onSetExpanded?: (id: string, expanded: boolean) => void,
	 *  onKeepExpanded?: (ids: string[]) => void,
	 *  onLoadChildren?: (node: any, force?: boolean) => Promise<void>,
	 *  onMutation?: (mutation: import('./dnd').DboDropResult, context?: { targetParentNode?: any, projectTargetParent?: () => void, clearTargetProjection?: () => void, projectPendingParent?: () => void }) => void | Promise<void>,
	 *  projectParentChildren?: () => void,
	 *  clearParentProjection?: () => void,
	 *  onMutationBusyChange?: (busy: boolean, handled?: boolean) => void,
	 *  onContextAction?: (event: { nodeId: string, action: any, result: any }) => void | Promise<void>,
	 *  canShowInFrontend?: (nodeId: string) => boolean,
	 *  onShowInFrontend?: (nodeId: string) => void | Promise<void>,
	 *  canRevealInPalette?: (nodeId: string) => boolean,
	 *  onRevealInPalette?: (nodeId: string) => void | Promise<void>,
	 *  canRevealBlockDefinition?: (nodeId: string) => boolean,
	 *  onRevealBlockDefinition?: (nodeId: string) => void | Promise<void>,
	 *  onSourceDrop?: (targetId: string, payload: import('./sourcePickerDnd').SourcePickerDragPayload) => void | Promise<void>
	 * }}
	 */
	let {
		node,
		selectedId = $bindable(''),
		depth = 0,
		parentNode = null,
		ancestorDisabled = false,
		renameTargetId = $bindable(''),
		dataSerial = 0,
		refreshSerial = 0,
		expandedNodeIds,
		onSetExpanded,
		onKeepExpanded,
		onLoadChildren = async () => {},
		onMutation,
		projectParentChildren,
		clearParentProjection,
		onMutationBusyChange,
		onContextAction,
		canShowInFrontend,
		onShowInFrontend,
		canRevealInPalette,
		onRevealInPalette,
		canRevealBlockDefinition,
		onRevealBlockDefinition,
		onSourceDrop
	} = $props();

	let localExpanded = $state(false);
	let loading = $state(false);
	let dropOver = $state(false);
	let dropAllowed = $state(false);
	let sourceDropOver = $state(false);
	let dropIndicator = $state('inside');
	/** @type {import('./dnd').DropAction} */
	let dropAction = $state('none');
	let dropCheckKey = '';
	let revision = $state(0);
	/** @type {any[] | null} */
	let projectedChildren = $state.raw(null);
	let autoExpandTarget = '';
	let lastRefreshSerial;
	let renameValue = $state('');
	let renameFocusId = '';
	let renamingBusy = $state(false);
	let deletingBusy = $state(false);
	/** @type {HTMLDivElement | undefined} */
	let rowElement = $state();
	/** @type {HTMLInputElement | undefined} */
	let renameInput = $state();

	let isBranch = $derived.by(() => {
		dataSerial;
		revision;
		return hasExpandableChildren(node);
	});
	let expanded = $derived(
		Boolean(node?.id && expandedNodeIds ? hasExpandedNodeId(node.id) : localExpanded)
	);
	let children = $derived.by(() => {
		dataSerial;
		revision;
		return projectedChildren ?? (Array.isArray(node?.children) ? node.children : []);
	});
	let selected = $derived(Boolean(node?.id && isEquivalentNodeId(node.id, selectedId)));
	let label = $derived.by(() => {
		dataSerial;
		revision;
		return node?.label ?? node?.name ?? 'Unnamed';
	});
	let disabled = $derived.by(() => {
		dataSerial;
		revision;
		return node?.enabled === false;
	});
	let iconify = $derived.by(() => {
		dataSerial;
		revision;
		return node?.iconify;
	});
	let icon = $derived.by(() => {
		dataSerial;
		revision;
		return node?.icon;
	});
	let unreachable = $derived(!disabled && ancestorDisabled);
	let availabilityTitle = $derived(
		disabled
			? `${label} — Disabled`
			: unreachable
				? `${label} — Unreachable because an ancestor is disabled`
				: undefined
	);
	let paddingLeft = $derived(`${depth * 0.34 + 0.14}rem`);
	let draggableNode = $derived(isDraggableNode(node?.id ?? ''));
	let renaming = $derived(Boolean(node?.id && isEquivalentNodeId(node.id, renameTargetId)));
	let showSelectedActions = $derived(
		Boolean(selected && !renaming && (draggableNode || isFlowContextNode(node)))
	);

	$effect(() => {
		const target = selectedId;
		const nodeId = node?.id ?? '';
		if (
			!target ||
			!nodeId ||
			isEquivalentNodeId(nodeId, target) ||
			!isBranch ||
			autoExpandTarget === target ||
			!isAncestorSelectionNode(nodeId, target)
		) {
			return;
		}
		autoExpandTarget = target;
		void expandForSelection();
	});

	$effect(() => {
		const serial = refreshSerial;
		if (lastRefreshSerial === undefined) {
			lastRefreshSerial = serial;
			return;
		}
		if (serial === lastRefreshSerial) {
			return;
		}
		lastRefreshSerial = serial;
		const nodeId = node?.id ?? '';
		const target = selectedId;
		const shouldRefresh =
			expanded ||
			(Boolean(target && nodeId) &&
				(isEquivalentNodeId(nodeId, target) || isAncestorSelectionNode(nodeId, target)));
		if (!shouldRefresh || !isBranch || !nodeId) {
			return;
		}
		void refreshExpandedNode(serial);
	});

	$effect(() => {
		if (selected && rowElement) {
			rowElement.scrollIntoView({ block: 'center', inline: 'nearest' });
		}
	});

	$effect(() => {
		const id = node?.id ?? '';
		if (!renaming) {
			renameFocusId = '';
			return;
		}
		if (!renaming || !id || renameFocusId === id) {
			return;
		}
		renameFocusId = id;
		renameValue = objectNameFromId(id);
		void tick().then(() => {
			const focusInput = () => {
				renameInput?.focus();
				renameInput?.select();
			};
			if (typeof requestAnimationFrame === 'function') {
				requestAnimationFrame(focusInput);
			} else {
				focusInput();
			}
		});
	});

	async function toggleExpanded(event) {
		event.stopPropagation();
		if (!isBranch) {
			return;
		}
		const nextExpanded = !expanded;
		setExpanded(nextExpanded);
		if (nextExpanded && !Array.isArray(node.children)) {
			loading = true;
			try {
				await onLoadChildren(node);
				revision += 1;
			} finally {
				loading = false;
			}
		}
	}

	async function expandForSelection() {
		if (!isBranch) {
			return;
		}
		setExpanded(true);
		if (!Array.isArray(node.children)) {
			loading = true;
			try {
				await onLoadChildren(node);
				revision += 1;
			} finally {
				loading = false;
			}
		}
	}

	/**
	 * @param {boolean} nextExpanded
	 */
	function setExpanded(nextExpanded) {
		const nodeId = node?.id ?? '';
		if (onSetExpanded && nodeId) {
			onSetExpanded(nodeId, nextExpanded);
			return;
		}
		if (expandedNodeIds && nodeId) {
			if (nextExpanded) {
				for (const id of equivalentDboObjectIds(nodeId)) {
					expandedNodeIds.add(id);
				}
			} else {
				for (const id of Array.from(expandedNodeIds)) {
					if (isEquivalentNodeId(id, nodeId)) {
						expandedNodeIds.delete(id);
					}
				}
			}
			return;
		}
		localExpanded = nextExpanded;
	}

	/**
	 * @param {string} nodeId
	 * @returns {boolean}
	 */
	function hasExpandedNodeId(nodeId) {
		if (!expandedNodeIds) {
			return false;
		}
		if (expandedNodeIds.has(nodeId)) {
			return true;
		}
		return equivalentDboObjectIds(nodeId).some((id) => expandedNodeIds?.has(id));
	}

	/**
	 * @param {any} candidate
	 * @returns {boolean}
	 */
	function hasExpandableChildren(candidate) {
		const children = candidate?.children;
		return children === 0 || children === true || (Array.isArray(children) && children.length > 0);
	}

	/**
	 * @param {number} serial
	 */
	async function refreshExpandedNode(serial) {
		loading = true;
		try {
			await onLoadChildren(node, true);
			if (serial === lastRefreshSerial) {
				revision += 1;
			}
		} finally {
			if (serial === lastRefreshSerial) {
				loading = false;
			}
		}
	}

	function selectNode() {
		selectedId = node?.id ?? '';
	}

	function requestRename() {
		if (!node?.id) {
			return;
		}
		selectedId = node.id;
		renameTargetId = node.id;
	}

	async function deleteSelectedNode() {
		if (deletingBusy || !node?.id) {
			return;
		}
		const previousId = node.id;
		const name = objectNameFromId(previousId);
		if (
			typeof window !== 'undefined' &&
			!window.confirm(`Delete "${name}"?\n\nThis action cannot be undone.`)
		) {
			return;
		}
		deletingBusy = true;
		try {
			const result = await removeDbo(previousId);
			if (!result?.done) {
				return;
			}
			renameTargetId = '';
			const nextSelection = parentObjectId(previousId) || parentNode?.id || '';
			if (parentNode) {
				await onLoadChildren(parentNode, true);
			}
			revision += 1;
			selectedId = nextSelection;
			await onMutation?.({
				done: true,
				id: nextSelection,
				selectedId: nextSelection,
				target: previousId,
				position: 'inside',
				source: 'tree',
				payload: {
					type: 'deleteData',
					data: { id: previousId }
				}
			});
		} finally {
			deletingBusy = false;
		}
	}

	/**
	 * @param {Event} event
	 */
	async function commitRename(event) {
		event.preventDefault();
		event.stopPropagation();
		if (renamingBusy || !renaming || !node?.id) {
			return;
		}
		const currentName = objectNameFromId(node.id);
		const nextName = renameValue.trim();
		if (!nextName || nextName === currentName) {
			renameTargetId = '';
			return;
		}
		renamingBusy = true;
		try {
			const result = await renameDbo(node.id, nextName, 'UPDATE_NONE');
			if (!result?.done) {
				await tick();
				renameInput?.focus();
				return;
			}
			const previousId = node.id;
			const nextId = renameObjectId(previousId, nextName);
			renameTargetId = '';
			if (parentNode) {
				await onLoadChildren(parentNode, true);
			}
			revision += 1;
			selectedId = nextId;
			await onMutation?.({
				done: true,
				id: nextId,
				selectedId: nextId,
				target: previousId,
				position: 'inside',
				source: 'tree',
				payload: {
					type: 'renameData',
					data: { id: previousId }
				}
			});
		} finally {
			renamingBusy = false;
		}
	}

	/**
	 * @param {KeyboardEvent} event
	 */
	function handleRenameKeydown(event) {
		if (event.key === 'Escape') {
			event.preventDefault();
			event.stopPropagation();
			renameTargetId = '';
		}
	}

	/**
	 * @param {string} nodeId
	 * @param {string} target
	 * @returns {boolean}
	 */
	function isAncestorNode(nodeId, target) {
		const folderPrefix = folderTargetPrefix(nodeId);
		return (
			target.startsWith(`${nodeId}.`) ||
			target.startsWith(`${nodeId}:`) ||
			target.startsWith(`${nodeId}/`) ||
			Boolean(folderPrefix && target.startsWith(folderPrefix))
		);
	}

	/**
	 * @param {string} nodeId
	 * @param {string} target
	 * @returns {boolean}
	 */
	function isAncestorSelectionNode(nodeId, target) {
		return equivalentDboObjectIds(target).some((id) => isAncestorNode(nodeId, id));
	}

	/**
	 * @param {string | undefined} nodeId
	 * @param {string | undefined} target
	 * @returns {boolean}
	 */
	function isEquivalentNodeId(nodeId, target) {
		if (!nodeId || !target) {
			return false;
		}
		return equivalentDboObjectIds(target).includes(nodeId);
	}

	/**
	 * Tree folders use ids like `Project:sq` or `Project.sq:Seq:st`, while database
	 * objects below them use qnames like `Project.sq:Seq.st:Step`.
	 * @param {string} nodeId
	 * @returns {string}
	 */
	function folderTargetPrefix(nodeId) {
		const match = nodeId.match(/^(.*):([^:.]+)$/);
		if (!match?.[1] || !match?.[2]) {
			return '';
		}
		return `${match[1]}.${match[2]}:`;
	}

	/**
	 * @param {DragEvent} event
	 */
	async function checkDrop(event) {
		const sourcePayload = getSourcePickerDragPayload(event, $draggedData);
		if (sourcePayload && node?.id) {
			event.preventDefault();
			event.stopPropagation();
			dropOver = true;
			sourceDropOver = true;
			dropAllowed = true;
			dropAction = 'copy';
			dropIndicator = 'inside';
			if (event.dataTransfer) {
				event.dataTransfer.dropEffect = 'copy';
			}
			return;
		}
		const payload = getDboDragPayload(event, $draggedData);
		const target = getTreeDropTarget(event);
		if (
			!payload ||
			!target?.target ||
			isOwnNodePayload(payload) ||
			isNoopTreeMoveTarget(payload, target)
		) {
			resetDrop();
			dropAllowed = false;
			return;
		}
		event.preventDefault();
		event.stopPropagation();
		dropOver = true;
		dropAction = getDboDropAction(event, payload);
		const nextKey = `${dropAction}:${target.target}:${target.position}:${payload?.data?.id ?? ''}`;
		if (nextKey === dropCheckKey) {
			return;
		}
		dropCheckKey = nextKey;
		let allowed = await canDropDbo({
			payload,
			target: target.target,
			position: target.position,
			dropAction
		});
		let nextIndicator = target.indicator;
		const fallback = getTreeDropFallback(target);
		const canUseFallback = canUseDboDropFallback({
			payload,
			target: target.target,
			position: target.position,
			dropAction,
			fallbackTarget: fallback.fallbackTarget,
			fallbackPosition: fallback.fallbackPosition
		});
		if (!allowed && canUseFallback && target.position === 'inside') {
			allowed = await canDropDbo({
				payload,
				target: fallback.fallbackTarget,
				position: fallback.fallbackPosition ?? 'after',
				dropAction
			});
			if (allowed) {
				nextIndicator = 'after';
			}
		} else if (!allowed && canUseFallback && target.position !== 'inside') {
			allowed = await canDropDbo({
				payload,
				target: fallback.fallbackTarget,
				position: fallback.fallbackPosition ?? 'inside',
				dropAction
			});
		}
		if (dropCheckKey !== nextKey) {
			return;
		}
		dropIndicator = nextIndicator;
		dropAllowed = allowed;
	}

	function resetDrop() {
		dropOver = false;
		dropAllowed = false;
		sourceDropOver = false;
		dropIndicator = 'inside';
		dropCheckKey = '';
	}

	function projectChildren() {
		projectedChildren = Array.isArray(node?.children) ? [...node.children] : [];
	}

	function clearChildrenProjection() {
		projectedChildren = null;
		revision += 1;
	}

	/**
	 * @param {DragEvent} event
	 */
	async function handleDrop(event) {
		event.preventDefault();
		event.stopPropagation();
		const sourcePayload = getSourcePickerDragPayload(event, $draggedData);
		if (sourcePayload && node?.id) {
			resetDrop();
			selectedId = node.id;
			await onSourceDrop?.(node.id, sourcePayload);
			$draggedData = undefined;
			return;
		}
		const payload = getDboDragPayload(event, $draggedData);
		const target = getTreeDropTarget(event);
		const action = /** @type {import('./dnd').DropAction} */ (
			dropAction === 'none' ? getDboDropAction(event, payload) : dropAction
		);
		resetDrop();
		if (
			!payload ||
			!target?.target ||
			action === 'none' ||
			isOwnNodePayload(payload) ||
			isNoopTreeMoveTarget(payload, target)
		) {
			return;
		}
		let handled = false;
		const fallback = getUsableTreeDropFallback(payload, target, action);
		const pending = beginOptimisticPaletteDrop(payload, target, fallback);
		onMutationBusyChange?.(true);
		try {
			if (pending) {
				await tick();
			}
			const result = await performDboDrop({
				payload,
				target: target.target,
				position: target.position,
				dropAction: action,
				...fallback
			});
			if (result?.done) {
				if (pending) {
					result.pendingId = pending.id;
				}
				keepMutationParentsExpanded(result);
				if (result.position === 'inside') {
					setExpanded(true);
				}
				if (result.selectedId) {
					selectedId = result.selectedId;
				}
				if (onMutation) {
					const projection = projectionContext(result.parentId, result.position);
					await onMutation(
						{ ...result, source: 'tree' },
						{
							targetParentNode: projection.parent,
							projectTargetParent: projection.project,
							clearTargetProjection: projection.clear,
							projectPendingParent:
								pending && pending.parent !== projection.parent ? pending.project : undefined
						}
					);
				} else {
					rollbackOptimisticPaletteDrop(pending);
					const refreshNode = result.position === 'inside' ? node : parentNode;
					if (refreshNode) {
						await onLoadChildren(refreshNode, true);
						revision += 1;
					}
				}
				handled = true;
			}
		} finally {
			if (!handled) {
				rollbackOptimisticPaletteDrop(pending);
			}
			onMutationBusyChange?.(false, handled);
			$draggedData = undefined;
		}
	}

	/**
	 * Project source-backed frontend palette drops before the engine finishes
	 * mutation/generation. The engine response remains authoritative and
	 * replaces this temporary id; refused calls roll it back.
	 * @param {import('./dnd').DboDragPayload} payload
	 * @param {{ target: string, position: import('./dnd').DropPosition }} target
	 * @param {{ fallbackTarget?: string, fallbackPosition?: import('./dnd').DropPosition }} fallback
	 */
	function beginOptimisticPaletteDrop(payload, target, fallback) {
		if (payload?.type !== 'paletteData' || payload.data?.type !== 'FrontendBlock') {
			return null;
		}
		let effectiveTarget = target.target;
		let effectivePosition = target.position;
		let context = projectionContext('', effectivePosition);
		if (
			(!context.parent || !Array.isArray(context.parent.children) || !context.project) &&
			fallback?.fallbackTarget &&
			fallback.fallbackPosition
		) {
			effectiveTarget = fallback.fallbackTarget;
			effectivePosition = fallback.fallbackPosition;
			context = projectionContext('', effectivePosition);
		}
		if (!context.parent || !Array.isArray(context.parent.children) || !context.project) {
			return null;
		}
		const id = `${context.parent.id}.__pending_${Date.now()}_${Math.random().toString(36).slice(2)}`;
		const applied = applyProjectedTreeMutation(
			[context.parent],
			{
				done: true,
				optimistic: true,
				selectedId: id,
				parentId: context.parent.id,
				target: effectiveTarget,
				position: effectivePosition,
				payload
			},
			isEquivalentNodeId
		);
		if (!applied) {
			return null;
		}
		context.project();
		return { id, parent: context.parent, project: context.project };
	}

	/** @param {{ id: string, parent: any, project: () => void } | null} pending */
	function rollbackOptimisticPaletteDrop(pending) {
		if (!pending || !removeProjectedTreeNode([pending.parent], pending.id, isEquivalentNodeId)) {
			return;
		}
		pending.project();
	}

	/**
	 * Resolve the visual parent from the authoritative parent id first. This
	 * matters when the engine retargets an inside drop to a sibling insertion.
	 * @param {string | undefined} parentId
	 * @param {import('./dnd').DropPosition | undefined} position
	 */
	function projectionContext(parentId, position) {
		if (parentId && isEquivalentNodeId(node?.id, parentId)) {
			return { parent: node, project: projectChildren, clear: clearChildrenProjection };
		}
		if (parentId && isEquivalentNodeId(parentNode?.id, parentId)) {
			return {
				parent: parentNode,
				project: projectParentChildren,
				clear: clearParentProjection
			};
		}
		return position === 'inside'
			? { parent: node, project: projectChildren, clear: clearChildrenProjection }
			: { parent: parentNode, project: projectParentChildren, clear: clearParentProjection };
	}

	/**
	 * @param {import('./dnd').DboDropResult} mutation
	 */
	function keepMutationParentsExpanded(mutation) {
		const ids = mutationDboContextIds(mutation);
		if (onKeepExpanded) {
			onKeepExpanded(ids);
			return;
		}
		for (const id of ids) {
			expandedNodeIds?.add(id);
		}
	}

	/**
	 * @param {DragEvent} event
	 */
	function handleDragStart(event) {
		if (!draggableNode || !node?.id) {
			return;
		}
		const treeData = {
			type: 'treeData',
			data: { id: node.id, classname: node.classname ?? '' },
			options: {}
		};
		event.dataTransfer?.setData('text/plain', JSON.stringify(treeData));
		event.dataTransfer?.setData('treedata', JSON.stringify(treeData));
		if (event.dataTransfer) {
			event.dataTransfer.effectAllowed = 'copyMove';
		}
		$draggedData = treeData;
	}

	function handleDragEnd() {
		$draggedData = undefined;
		resetDrop();
	}

	/**
	 * @returns {string}
	 */
	function currentDropLabel() {
		if (sourceDropOver) {
			return `Use source for ${label}`;
		}
		const action = dropAction === 'copy' ? 'Copy' : dropAction === 'move' ? 'Move' : 'Drop';
		if (!isIfStep(node) || dropIndicator === 'before') {
			return `${action} ${dropIndicator} ${label}`;
		}
		const placement = dropLabel({
			position: /** @type {'inside' | 'before' | 'after'} */ (dropIndicator),
			step: node
		});
		return actionDropLabel(action, placement);
	}

	/**
	 * @param {string} action
	 * @param {string} placement
	 * @returns {string}
	 */
	function actionDropLabel(action, placement) {
		if (placement.startsWith('Drop ')) {
			return `${action} ${placement.slice(5)}`;
		}
		if (placement.startsWith('Inside ')) {
			return `${action} inside ${placement.slice(7)}`;
		}
		if (placement.startsWith('Before ')) {
			return `${action} before ${placement.slice(7)}`;
		}
		if (placement.startsWith('After ')) {
			return `${action} after ${placement.slice(6)}`;
		}
		return `${action} ${placement.toLowerCase()}`;
	}

	/**
	 * @param {DragEvent} event
	 * @returns {{ target: string, position: import('./dnd').DropPosition, indicator: 'inside' | 'before' | 'after' } | null}
	 */
	function getTreeDropTarget(event) {
		if (!node?.id) {
			return null;
		}
		let position = /** @type {import('./dnd').DropPosition} */ ('inside');
		let target = node.id;
		let indicator = /** @type {'inside' | 'before' | 'after'} */ ('inside');
		if (rowElement && parentNode?.id) {
			const rect = rowElement.getBoundingClientRect();
			const y = event.clientY - rect.top;
			const rowPosition = treeRowDropPosition(y, rect.height, node?.children !== false);
			if (rowPosition === 'before') {
				position = 'before';
				indicator = 'before';
			} else if (rowPosition === 'after') {
				position = 'after';
				target = node.id;
				indicator = 'after';
			}
		}
		return { target, position, indicator };
	}

	/**
	 * @param {{ position: import('./dnd').DropPosition } | null} target
	 * @returns {{ fallbackTarget?: string, fallbackPosition?: import('./dnd').DropPosition }}
	 */
	function getTreeDropFallback(target) {
		if (!target || !parentNode?.id) {
			return {};
		}
		if (target.position === 'inside') {
			return { fallbackTarget: node.id, fallbackPosition: 'after' };
		}
		return { fallbackTarget: parentNode.id, fallbackPosition: 'inside' };
	}

	/**
	 * @param {import('./dnd').DboDragPayload} payload
	 * @param {{ target: string, position: import('./dnd').DropPosition }} target
	 * @param {import('./dnd').DropAction} action
	 * @returns {{ fallbackTarget?: string, fallbackPosition?: import('./dnd').DropPosition }}
	 */
	function getUsableTreeDropFallback(payload, target, action) {
		const fallback = getTreeDropFallback(target);
		return canUseDboDropFallback({
			payload,
			target: target.target,
			position: target.position,
			dropAction: action,
			fallbackTarget: fallback.fallbackTarget,
			fallbackPosition: fallback.fallbackPosition
		})
			? fallback
			: {};
	}

	/**
	 * @param {import('./dnd').DboDragPayload | undefined} payload
	 * @returns {boolean}
	 */
	function isOwnNodePayload(payload) {
		return Boolean(
			payload?.type === 'treeData' &&
			payload.data?.id &&
			node?.id &&
			isEquivalentNodeId(node.id, payload.data.id)
		);
	}

	/**
	 * @param {import('./dnd').DboDragPayload | undefined} payload
	 * @param {{ target: string, position: import('./dnd').DropPosition } | null} target
	 * @returns {boolean}
	 */
	function isNoopTreeMoveTarget(payload, target) {
		if (!target || !Array.isArray(parentNode?.children)) {
			return false;
		}
		return isNoopSiblingMove({
			payload,
			target: target.target,
			position: target.position,
			sourceSiblingIds: parentNode.children.map((child) => child?.id).filter(Boolean)
		});
	}

	/**
	 * @param {string} id
	 * @returns {boolean}
	 */
	function isDraggableNode(id) {
		if (!id || !id.includes('.')) {
			return false;
		}
		const folderMatch = id.match(/:([a-z]{2,4})$/);
		return !folderMatch || !folderTypeIds.has(folderMatch[1]);
	}

	/**
	 * @param {any} candidate
	 * @returns {boolean}
	 */
	function isFlowContextNode(candidate) {
		const id = String(candidate?.id ?? '');
		const classname = String(candidate?.classname ?? '');
		return (
			classname === 'Flow' ||
			classname === 'FlowEngine' ||
			classname === 'FlowVirtualObject' ||
			id.includes('.frontends') ||
			id.endsWith('.flow') ||
			id.includes('.flow.')
		);
	}
</script>

<div class="studio-tree-node" role="treeitem" aria-selected={selected} data-enabled={node?.enabled}>
	<div
		bind:this={rowElement}
		role="presentation"
		draggable={draggableNode}
		ondragstart={handleDragStart}
		ondragend={handleDragEnd}
		ondragenter={checkDrop}
		ondragover={checkDrop}
		ondragleave={resetDrop}
		ondrop={handleDrop}
		class:studio-tree-node__row--selected={selected}
		class:studio-tree-node__row--drop={dropOver && dropAllowed}
		class:studio-tree-node__row--drop-denied={dropOver && !dropAllowed}
		class:studio-tree-node__row--drop-before={dropOver && dropAllowed && dropIndicator === 'before'}
		class:studio-tree-node__row--drop-after={dropOver && dropAllowed && dropIndicator === 'after'}
		class:studio-tree-node__row--drop-inside={dropOver && dropAllowed && dropIndicator === 'inside'}
		class:studio-tree-node__row--pending={Boolean(node?.pending)}
		class:studio-tree-node__row--disabled={disabled}
		class:studio-tree-node__row--unreachable={unreachable}
		class="studio-tree-node__row"
		style:padding-left={paddingLeft}
		title={availabilityTitle}
	>
		<span class="studio-tree-node__toggle">
			{#if isBranch}
				<button
					type="button"
					data-node-id={node?.id}
					class:studio-tree-node__toggle-button--open={expanded}
					class="studio-tree-node__toggle-button"
					aria-label={expanded ? 'Collapse' : 'Expand'}
					onclick={(event) => toggleExpanded(event)}
					disabled={loading}
				>
					<Ico icon={loading ? 'mdi:sync' : 'mdi:chevron-right'} size={4} />
				</button>
			{/if}
		</span>
		{#if renaming}
			<form
				data-node-id={node?.id}
				class="studio-tree-node__content studio-tree-node__content--rename"
				onsubmit={commitRename}
			>
				<span class="studio-tree-node__icon">
					{#if typeof icon === 'string' && icon.includes('?')}
						<AutoSvg class="h-4 w-4" fill="currentColor" src="{getUrl()}{icon}" alt="" />
					{:else if iconify}
						<Ico icon={iconify} size={4} />
					{:else if icon == 'file'}
						<Ico icon="mdi:file-document-box-outline" size={4} />
					{:else if icon == 'folder'}
						<Ico icon="mdi:folder-outline" size={4} />
					{:else}
						<Ico icon="convertigo:logo" size={4} />
					{/if}
				</span>
				<input
					bind:this={renameInput}
					bind:value={renameValue}
					class="studio-tree-node__rename"
					aria-label="Rename object"
					disabled={renamingBusy}
					onblur={commitRename}
					onkeydown={handleRenameKeydown}
				/>
			</form>
		{:else}
			<button
				type="button"
				data-node-id={node?.id}
				class="studio-tree-node__content"
				onclick={selectNode}
				ondblclick={(event) => toggleExpanded(event)}
			>
				<span class="studio-tree-node__icon">
					{#if typeof icon === 'string' && icon.includes('?')}
						<AutoSvg class="h-4 w-4" fill="currentColor" src="{getUrl()}{icon}" alt="" />
					{:else if iconify}
						<Ico icon={iconify} size={4} />
					{:else if icon == 'file'}
						<Ico icon="mdi:file-document-box-outline" size={4} />
					{:else if icon == 'folder'}
						<Ico icon="mdi:folder-outline" size={4} />
					{:else}
						<Ico icon="convertigo:logo" size={4} />
					{/if}
				</span>
				<span class="studio-tree-node__label">{label}</span>
			</button>
		{/if}
		{#if showSelectedActions}
			<div class="studio-tree-node__actions">
				<StudioTreeActionMenu
					nodeId={node.id}
					{label}
					canRename={draggableNode}
					canDelete={draggableNode}
					deleting={deletingBusy}
					canShowInFrontend={canShowInFrontend?.(node.id) ?? false}
					canRevealInPalette={canRevealInPalette?.(node.id) ?? false}
					canRevealDefinition={canRevealBlockDefinition?.(node.id) ?? false}
					onSelectNode={selectNode}
					onRename={requestRename}
					onDelete={deleteSelectedNode}
					onShowInFrontend={() => onShowInFrontend?.(node.id)}
					onRevealInPalette={() => onRevealInPalette?.(node.id)}
					onRevealDefinition={() => onRevealBlockDefinition?.(node.id)}
					{onContextAction}
				/>
			</div>
		{/if}
		{#if dropOver}
			<span
				class="studio-tree-node__drop-label"
				class:studio-tree-node__drop-label--denied={!dropAllowed}
				title={dropAllowed ? currentDropLabel() : 'Not allowed'}
			>
				{dropAllowed ? currentDropLabel() : 'Not allowed'}
			</span>
		{/if}
	</div>

	{#if expanded && children.length}
		<div class="studio-tree-node__children" role="group">
			{#each children as child, index (child.id ?? child.name)}
				<StudioTreeNode
					node={child}
					bind:selectedId
					bind:renameTargetId
					depth={depth + 1}
					parentNode={node}
					ancestorDisabled={ancestorDisabled || disabled}
					{dataSerial}
					{onLoadChildren}
					{refreshSerial}
					{expandedNodeIds}
					{onSetExpanded}
					{onKeepExpanded}
					{onMutation}
					projectParentChildren={projectChildren}
					clearParentProjection={clearChildrenProjection}
					{onMutationBusyChange}
					{onContextAction}
					{canShowInFrontend}
					{onShowInFrontend}
					{canRevealInPalette}
					{onRevealInPalette}
					{canRevealBlockDefinition}
					{onRevealBlockDefinition}
					{onSourceDrop}
				/>
			{/each}
		</div>
	{/if}
</div>

<style>
	.studio-tree-node {
		width: max-content;
		min-width: 100%;
	}

	.studio-tree-node__row {
		position: relative;
		display: grid;
		width: max-content;
		min-width: 100%;
		grid-template-columns: 0.72rem max-content auto;
		align-items: center;
		gap: 0.08rem;
		border: 1px solid transparent;
		border-radius: 0.35rem;
		background: transparent;
		color: var(--color-surface-900-100);
		padding-top: 0.16rem;
		padding-right: 0.24rem;
		padding-bottom: 0.16rem;
		transition:
			background 0.14s ease,
			border-color 0.14s ease,
			color 0.14s ease;
	}

	.studio-tree-node__row:hover {
		background: color-mix(in oklab, var(--color-primary-500) 9%, transparent);
	}

	.studio-tree-node__row--pending {
		opacity: 0.68;
		animation: studio-tree-pending 0.9s ease-in-out infinite alternate;
	}

	@keyframes studio-tree-pending {
		to {
			opacity: 1;
		}
	}

	.studio-tree-node__row--selected {
		border-color: color-mix(in oklab, var(--color-primary-500) 38%, transparent);
		background: color-mix(in oklab, var(--color-primary-500) 14%, transparent);
		color: var(--color-primary-700-300);
	}

	.studio-tree-node__row--disabled {
		color: var(--color-error-500, #dc2626);
	}

	.studio-tree-node__row--unreachable {
		color: var(--color-warning-500, #ff8c00);
	}

	.studio-tree-node__row--drop {
		border-color: color-mix(in oklab, var(--color-primary-500) 48%, transparent);
		padding-right: 6.6rem;
	}

	.studio-tree-node__row--drop-denied {
		border-color: color-mix(in oklab, var(--color-warning-500) 65%, transparent);
		background: color-mix(in oklab, var(--color-warning-500) 10%, transparent);
		padding-right: 6.6rem;
	}

	.studio-tree-node__row--drop-inside {
		background: color-mix(in oklab, var(--color-primary-500) 20%, transparent);
		box-shadow: inset 0 0 0 1px var(--color-primary-500);
	}

	.studio-tree-node__row--drop-before::before,
	.studio-tree-node__row--drop-after::after {
		position: absolute;
		right: 0.25rem;
		left: var(--tree-drop-line-left, 0.25rem);
		z-index: 2;
		height: 2px;
		border-radius: 999px;
		background: var(--color-primary-500);
		box-shadow: 0 0 0 3px color-mix(in oklab, var(--color-primary-500) 20%, transparent);
		content: '';
	}

	.studio-tree-node__row--drop-before::before {
		top: -2px;
	}

	.studio-tree-node__row--drop-after::after {
		bottom: -2px;
	}

	.studio-tree-node__toggle,
	.studio-tree-node__icon {
		display: grid;
		min-width: 0;
		place-items: center;
	}

	.studio-tree-node__content {
		display: grid;
		min-width: max-content;
		grid-template-columns: 0.82rem max-content;
		align-items: center;
		gap: 0.12rem;
		border: 0;
		background: transparent;
		color: inherit;
		padding: 0;
		text-align: left;
	}

	.studio-tree-node__content--rename {
		min-width: 9rem;
	}

	.studio-tree-node__toggle-button {
		display: grid;
		width: 0.72rem;
		height: 0.9rem;
		place-items: center;
		border: 0;
		border-radius: 0.25rem;
		background: transparent;
		color: inherit;
		padding: 0;
		transition:
			background 0.14s ease,
			transform 0.14s ease;
	}

	.studio-tree-node__toggle-button:hover {
		background: color-mix(in oklab, var(--color-surface-300-700) 48%, transparent);
	}

	.studio-tree-node__toggle-button--open {
		transform: rotate(90deg);
	}

	.studio-tree-node__label {
		min-width: max-content;
		white-space: nowrap;
		font-size: 0.74rem;
		font-weight: 650;
	}

	.studio-tree-node__actions {
		display: inline-flex;
		align-items: center;
		gap: 0.12rem;
		margin-left: 0.26rem;
	}

	.studio-tree-node__drop-label {
		position: absolute;
		top: 50%;
		right: 0.32rem;
		z-index: 3;
		max-width: 6rem;
		transform: translateY(-50%);
		border: 1px solid color-mix(in oklab, var(--color-primary-500) 52%, transparent);
		border-radius: 999px;
		background: color-mix(in oklab, var(--color-primary-500) 18%, var(--color-surface-50-950));
		color: var(--color-primary-700-300);
		padding: 0.04rem 0.34rem;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		font-size: 0.62rem;
		font-weight: 750;
		letter-spacing: 0;
		pointer-events: none;
	}

	.studio-tree-node__drop-label--denied {
		border-color: color-mix(in oklab, var(--color-warning-500) 60%, transparent);
		background: color-mix(in oklab, var(--color-warning-500) 16%, var(--color-surface-50-950));
		color: var(--color-warning-700-300);
	}

	.studio-tree-node__rename {
		width: min(11rem, 46vw);
		min-width: 0;
		border: 1px solid color-mix(in oklab, var(--color-primary-500) 55%, transparent);
		border-radius: 0.28rem;
		background: var(--color-surface-50-950);
		color: var(--color-surface-950-50);
		padding: 0.08rem 0.32rem;
		font-size: 0.76rem;
		font-weight: 650;
		outline: none;
	}

	.studio-tree-node__rename:focus {
		box-shadow: 0 0 0 2px color-mix(in oklab, var(--color-primary-500) 22%, transparent);
	}

	.studio-tree-node__children {
		margin-left: 0;
		border-left: 0;
	}
</style>
