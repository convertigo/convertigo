<script>
	import { Background, Controls, getViewportForBounds, MiniMap, SvelteFlow } from '@xyflow/svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import InputGroup from '$lib/common/components/InputGroup.svelte';
	import {
		areEquivalentDboObjectIds,
		canDropDbo,
		canUseDboDropFallback,
		equivalentDboObjectIds,
		getDboDragPayload,
		getDboDropAction,
		isDescendantObjectId,
		isNoopSiblingMove,
		mutationDboContextIds,
		objectNameFromId,
		parentObjectId,
		performDboDrop,
		renameObjectId
	} from '$lib/studio/dnd';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import { draggedData } from '$lib/utils/dndStore';
	import Ico from '$lib/utils/Ico.svelte';
	import { removeDbo, renameDbo } from '$lib/utils/service';
	import { untrack } from 'svelte';
	import { SvelteMap, SvelteSet } from 'svelte/reactivity';
	import { findFlowLaneDropTarget as resolveFlowLaneDropTarget } from './flowDropTargets';
	import { flowEdgeDistanceToPoint } from './flowGeometry';
	import FlowPalette from './FlowPalette.svelte';
	import FlowStepNode from './FlowStepNode.svelte';
	import { loadSequenceFlow } from './sequenceLoader';
	import { toXyFlow } from './xyflow';
	import '@xyflow/svelte/dist/style.css';

	/**
	 * @typedef {Object} SequenceLike
	 * @property {string} name
	 * @property {string=} comment
	 * @property {string=} accessibility
	 * @property {boolean=} autostart
	 */

	/**
	 * @type {{
	 *  projectName: string,
	 *  sequences?: SequenceLike[],
	 *  selectedSequenceName?: string,
	 *  showSequences?: boolean,
	 *  showPalette?: boolean,
	 *  showInspector?: boolean,
	 *  autoSelectFirst?: boolean,
	 *  selectedObjectId?: string,
	 *  refreshSerial?: number,
	 *  refreshMutation?: import('$lib/studio/dnd').DboDropResult | null,
	 *  refreshMutationSerial?: number,
	 *  onSelectNode?: (node: { id: string, data: import('./types').FlowStepNodeData }) => void,
	 *  onMutation?: (mutation: import('$lib/studio/dnd').DboDropResult) => void | Promise<void>
	 * }}
	 */
	let {
		projectName,
		sequences = [],
		selectedSequenceName = $bindable(''),
		showSequences = true,
		showPalette = true,
		showInspector = true,
		autoSelectFirst = true,
		selectedObjectId = '',
		refreshSerial = 0,
		refreshMutation = null,
		refreshMutationSerial = 0,
		onSelectNode,
		onMutation
	} = $props();

	const nodeTypes = { 'flow-step': FlowStepNode };
	const flowNodeSize = { width: 150, height: 72 };
	const flowDropBeforeRatio = 0.34;
	const flowDropAfterRatio = 0.66;

	let sequenceQuery = $state('');
	/** @type {{ id: string, data: import('./types').FlowStepNodeData } | null} */
	let selectedNode = $state(null);
	/** @type {import('./types').Flow | null} */
	let flow = $state(null);
	/** @type {import('@xyflow/svelte').Node[]} */
	let nodes = $state.raw([]);
	/** @type {import('@xyflow/svelte').Edge[]} */
	let edges = $state.raw([]);
	/** @type {Set<string>} */
	let collapsedSubstepParents = $state.raw(new SvelteSet());
	/** @type {HTMLDivElement | null} */
	let canvasElement = $state(null);
	let flowViewport = $state({ x: 32, y: 88, zoom: 0.82 });
	let loading = $state(false);
	let error = $state('');
	let loadSerial = 0;
	let fitSerial = 0;
	let loadedFlowKey = '';
	let lastFocusedObjectId = '';
	let lastRefreshSerial = $state(0);
	let lastRefreshMutationSerial = 0;
	let flowDropTargetNodeId = $state('');
	/** @type {'inside' | 'before' | 'after'} */
	let flowDropPosition = $state('inside');
	let flowDropBranch = $state('');
	let flowDropHostLabel = $state('');
	let flowDropAllowed = $state(false);
	let flowDropDenied = $state(false);
	let flowDropCheckKey = '';
	let flowRenameObjectId = $state('');
	/** @type {Set<string>} */
	let pendingExpandedSubstepParents = $state.raw(new SvelteSet());
	/** @type {Set<string>} */
	let pendingViewportFocusObjectIds = $state.raw(new SvelteSet());

	let filteredSequences = $derived.by(() => {
		const query = sequenceQuery.trim().toLowerCase();
		return (sequences ?? []).filter((sequence) =>
			query ? sequence.name?.toLowerCase().includes(query) : true
		);
	});

	let selectedSequence = $derived(
		(sequences ?? []).find((sequence) => sequence.name === selectedSequenceName) ?? null
	);
	let selectedSequenceId = $derived(
		projectName && selectedSequenceName ? `${projectName}.sq:${selectedSequenceName}` : ''
	);

	$effect(() => {
		if (!sequences?.length) {
			selectedSequenceName = '';
			return;
		}
		const hasSelectedSequence = sequences.some(
			(sequence) => sequence.name === selectedSequenceName
		);
		if (autoSelectFirst && (!selectedSequenceName || !hasSelectedSequence)) {
			selectedSequenceName = sequences[0].name;
		} else if (!autoSelectFirst && selectedSequenceName && !hasSelectedSequence) {
			selectedSequenceName = '';
		}
	});

	$effect(() => {
		const sequenceName = selectedSequenceName;
		const project = projectName;
		if (!project || !sequenceName) {
			flow = null;
			nodes = [];
			edges = [];
			loadedFlowKey = '';
			collapsedSubstepParents = new SvelteSet();
			return;
		}
		startLoadFlow(project, sequenceName);
	});

	$effect(() => {
		const objectId = selectedObjectId;
		if (!flow || !objectId) {
			applySelectedObjectId(objectId);
			return;
		}
		const targetNode = findFlowNodeByObjectId(objectId);
		if (!targetNode) {
			applySelectedObjectId(objectId);
			return;
		}
		if (objectId !== lastFocusedObjectId) {
			lastFocusedObjectId = objectId;
			const expandedAncestors = expandCollapsedAncestorsForNode(targetNode.id);
			if (expandedAncestors) {
				syncXyFlow({ focusNodeIds: new SvelteSet([targetNode.id]) });
				return;
			}
			applySelectedObjectId(objectId);
			scheduleViewportFit(new SvelteSet([targetNode.id]));
			return;
		}
		applySelectedObjectId(objectId);
	});

	$effect(() => {
		const serial = refreshSerial;
		const mutationSerial = refreshMutationSerial;
		const mutation = refreshMutation;
		if (mutationSerial !== lastRefreshMutationSerial) {
			lastRefreshMutationSerial = mutationSerial;
			if (mutation?.done) {
				rememberExpandedParentsForMutation(mutation);
				queueViewportFocusObject(mutation.selectedId || mutation.id || mutation.payload?.data?.id);
			}
		}
		if (serial === lastRefreshSerial) {
			return;
		}
		lastRefreshSerial = serial;
		untrack(refresh);
	});

	/**
	 * @param {string} project
	 * @param {string} sequenceName
	 */
	function startLoadFlow(project, sequenceName) {
		untrack(() => {
			void loadFlow(project, sequenceName);
		});
	}

	/**
	 * @param {string} project
	 * @param {string} sequenceName
	 */
	async function loadFlow(project, sequenceName) {
		const serial = ++loadSerial;
		const flowKey = `${project}.sq:${sequenceName}`;
		const previousFlow = flow;
		const previousCollapsedSubstepParents = new SvelteSet(collapsedSubstepParents);
		const shouldPreserveCollapseState = loadedFlowKey === flowKey;
		loading = true;
		error = '';
		selectedNode = null;
		try {
			const nextFlow = await loadSequenceFlow(project, sequenceName);
			if (serial !== loadSerial) {
				return;
			}
			flow = nextFlow;
			const nextCollapsedSubstepParents = shouldPreserveCollapseState
				? reconcileCollapsedSubsteps(nextFlow, previousFlow, previousCollapsedSubstepParents)
				: initialCollapsedSubsteps(nextFlow);
			pendingExpandedSubstepParents.forEach((nodeId) =>
				deleteEquivalentObjectId(nextCollapsedSubstepParents, nodeId)
			);
			pendingExpandedSubstepParents = new SvelteSet();
			expandSelectedObjectParents(nextFlow, nextCollapsedSubstepParents);
			collapsedSubstepParents = nextCollapsedSubstepParents;
			loadedFlowKey = flowKey;
			const focusNodeIds = takePendingViewportFocusNodeIds(nextFlow);
			syncXyFlow(focusNodeIds.size ? { focusNodeIds } : { fit: 'all' });
		} catch (err) {
			if (serial !== loadSerial) {
				return;
			}
			flow = null;
			nodes = [];
			edges = [];
			loadedFlowKey = '';
			collapsedSubstepParents = new SvelteSet();
			error = String(err instanceof Error ? err.message : err);
		} finally {
			if (serial === loadSerial) {
				loading = false;
			}
		}
	}

	/**
	 * @param {{ node: { id: string, data: Record<string, unknown> } }} event
	 */
	function handleNodeClick({ node }) {
		const nextNode = /** @type {{ id: string, data: import('./types').FlowStepNodeData }} */ (node);
		if (nextNode.data.isFlowTerminal) {
			return;
		}
		selectedNode = nextNode;
		onSelectNode?.(nextNode);
	}

	function handlePaneClick() {
		if (!selectedObjectId) {
			selectedNode = null;
		}
	}

	function refresh() {
		if (projectName && selectedSequenceName) {
			queueViewportFocusObject(selectedObjectId);
			startLoadFlow(projectName, selectedSequenceName);
		}
	}

	/**
	 * @param {DragEvent} event
	 */
	async function handleFlowDragOver(event) {
		const payload = getDboDragPayload(event, $draggedData);
		const target = resolveFlowDropTarget(event, payload);
		if (!payload || !target?.target) {
			resetFlowDrop();
			return;
		}
		if (isNoopFlowMoveTarget(payload, target)) {
			resetFlowDrop();
			return;
		}
		event.preventDefault();
		event.stopPropagation();
		const action = getDboDropAction(event, payload);
		const nextKey = [
			action,
			target.target,
			target.position,
			target.indicator,
			target.targetNodeId,
			target.fallbackTarget ?? '',
			target.fallbackPosition ?? '',
			target.fallbackIndicator ?? '',
			target.fallbackTargetNodeId ?? '',
			target.dropBranch ?? '',
			target.dropHostLabel ?? '',
			target.fallbackDropBranch ?? '',
			target.fallbackDropHostLabel ?? '',
			payload?.data?.id ?? ''
		].join(':');
		if (nextKey === flowDropCheckKey) {
			return;
		}
		flowDropCheckKey = nextKey;
		let allowed = await canDropDbo({
			payload,
			target: target.target,
			position: target.position,
			dropAction: action
		});
		let indicator = target.indicator;
		let targetNodeId = target.targetNodeId;
		let dropBranch = target.dropBranch ?? '';
		let dropHostLabel = target.dropHostLabel ?? '';
		if (!allowed && shouldUseFlowFallback(target, payload, action)) {
			allowed = await canDropDbo({
				payload,
				target: target.fallbackTarget,
				position: target.fallbackPosition ?? 'after',
				dropAction: action
			});
			if (allowed && target.position === 'inside') {
				indicator =
					target.fallbackIndicator ??
					flowIndicatorFromDropPosition(target.fallbackPosition ?? 'after');
				targetNodeId = target.fallbackTargetNodeId ?? target.targetNodeId;
				dropBranch = target.fallbackDropBranch ?? '';
				dropHostLabel = target.fallbackDropHostLabel ?? '';
			}
		}
		if (flowDropCheckKey !== nextKey) {
			return;
		}
		flowDropAllowed = allowed;
		flowDropDenied = !allowed && Boolean(target.targetNodeId);
		flowDropTargetNodeId = allowed ? targetNodeId : flowDropDenied ? target.targetNodeId : '';
		flowDropPosition = indicator;
		flowDropBranch = allowed || flowDropDenied ? dropBranch : '';
		flowDropHostLabel = allowed || flowDropDenied ? dropHostLabel : '';
		syncXyFlow();
	}

	/**
	 * @param {DragEvent} event
	 */
	async function handleFlowDrop(event) {
		const payload = getDboDragPayload(event, $draggedData);
		const target = resolveFlowDropTarget(event, payload);
		const action = getDboDropAction(event, payload);
		event.preventDefault();
		event.stopPropagation();
		resetFlowDrop();
		if (!payload || !target?.target || action === 'none' || isNoopFlowMoveTarget(payload, target)) {
			$draggedData = undefined;
			return;
		}
		const result = await performDboDrop({
			payload,
			target: target.target,
			position: target.position,
			dropAction: action,
			fallbackTarget: shouldUseFlowFallback(target, payload, action)
				? target.fallbackTarget
				: undefined,
			fallbackPosition: shouldUseFlowFallback(target, payload, action)
				? target.fallbackPosition
				: undefined
		});
		if (result?.done) {
			rememberExpandedParentsForMutation(result);
			queueViewportFocusObject(result.selectedId || result.id || result.payload?.data?.id || '');
			if (result.payload?.type === 'paletteData' && result.selectedId) {
				flowRenameObjectId = result.selectedId;
			}
			await onMutation?.({ ...result, source: 'flow' });
			if (!onMutation) {
				refresh();
			}
		}
		$draggedData = undefined;
	}

	/**
	 * @param {DragEvent} event
	 */
	function handleFlowDragLeave(event) {
		if (
			event.currentTarget instanceof HTMLElement &&
			event.relatedTarget instanceof Node &&
			event.currentTarget.contains(event.relatedTarget)
		) {
			return;
		}
		resetFlowDrop();
	}

	function resetFlowDrop() {
		const changed = Boolean(
			flowDropTargetNodeId ||
			flowDropAllowed ||
			flowDropDenied ||
			flowDropCheckKey ||
			flowDropBranch ||
			flowDropHostLabel
		);
		flowDropTargetNodeId = '';
		flowDropPosition = 'inside';
		flowDropBranch = '';
		flowDropHostLabel = '';
		flowDropAllowed = false;
		flowDropDenied = false;
		flowDropCheckKey = '';
		if (changed) {
			syncXyFlow();
		}
	}

	/**
	 * @param {{
	 *  target: string,
	 *  position: import('$lib/studio/dnd').DropPosition,
	 *  fallbackTarget?: string,
	 *  fallbackPosition?: import('$lib/studio/dnd').DropPosition,
	 *  fallbackIndicator?: 'inside' | 'before' | 'after',
	 *  fallbackTargetNodeId?: string
	 * }} target
	 * @param {import('$lib/studio/dnd').DboDragPayload} payload
	 * @param {import('$lib/studio/dnd').DropAction} action
	 * @returns {boolean}
	 */
	function shouldUseFlowFallback(target, payload, action) {
		return canUseDboDropFallback({
			payload,
			target: target.target,
			position: target.position,
			dropAction: action,
			fallbackTarget: target.fallbackTarget,
			fallbackPosition: target.fallbackPosition
		});
	}

	/**
	 * @param {DragEvent} event
	 * @param {import('$lib/studio/dnd').DboDragPayload | undefined} payload
	 * @returns {{
	 *  target: string,
	 *  position: import('$lib/studio/dnd').DropPosition,
	 *  indicator: 'inside' | 'before' | 'after',
	 *  targetNodeId: string,
	 *  fallbackTarget?: string,
	 *  fallbackPosition?: import('$lib/studio/dnd').DropPosition,
	 *  fallbackIndicator?: 'inside' | 'before' | 'after',
	 *  fallbackTargetNodeId?: string,
	 *  fallbackDropBranch?: string,
	 *  fallbackDropHostLabel?: string,
	 *  dropBranch?: string,
	 *  dropHostLabel?: string
	 * } | null}
	 */
	function resolveFlowDropTarget(event, payload = undefined) {
		if (!selectedSequenceId) {
			return null;
		}
		const hostNode = findFlowNodeAtEvent(event);
		if (hostNode) {
			const target = flowObjectId(hostNode);
			if (!target) {
				return null;
			}
			if (isDraggedFlowObject(payload, target)) {
				return null;
			}
			const hostFlowNode = flow?.nodes.find((node) => node.id === hostNode.id);
			const hostParentId = hostFlowNode ? flowParentId(hostFlowNode) : '';
			const hostBranch = hostFlowNode ? flowParentBranch(hostFlowNode) : '';
			const hostParentLabel = dropHostLabel(hostParentId);
			const placement = getFlowNodeDropPlacement(event, hostNode);
			if (placement === 'before') {
				return {
					target,
					position: 'before',
					indicator: 'before',
					targetNodeId: hostNode.id,
					fallbackTarget: hostParentId || undefined,
					fallbackPosition: hostParentId ? 'inside' : undefined,
					dropBranch: hostBranch,
					dropHostLabel: hostParentLabel
				};
			}
			if (placement === 'after') {
				return {
					target,
					position: 'after',
					indicator: 'after',
					targetNodeId: hostNode.id,
					fallbackTarget: hostParentId || undefined,
					fallbackPosition: hostParentId ? 'inside' : undefined,
					dropBranch: hostBranch,
					dropHostLabel: hostParentLabel
				};
			}
			const point = flowPointFromEvent(event);
			const laneFallback = point ? findFlowLaneDropTarget(point, payload) : null;
			const fallbackPosition = laneFallback?.position ?? 'after';
			return {
				target,
				position: 'inside',
				indicator: 'inside',
				targetNodeId: hostNode.id,
				fallbackTarget: laneFallback?.target ?? target,
				fallbackPosition,
				fallbackIndicator:
					laneFallback?.indicator ?? flowIndicatorFromDropPosition(fallbackPosition),
				fallbackTargetNodeId: laneFallback?.targetNodeId ?? hostNode.id,
				fallbackDropBranch: laneFallback?.dropBranch ?? hostBranch,
				fallbackDropHostLabel: laneFallback?.dropHostLabel ?? hostParentLabel,
				dropHostLabel: dropHostLabel(target)
			};
		}
		const point = flowPointFromEvent(event);
		const laneTarget = point ? findFlowLaneDropTarget(point, payload) : null;
		if (laneTarget) {
			return laneTarget;
		}
		const edge = point ? findFlowEdgeNearPoint(point) : null;
		if (edge) {
			const edgeTarget = resolveFlowEdgeDropTarget(edge, payload);
			if (edgeTarget) {
				return edgeTarget;
			}
		}
		const nearestNode = point ? findNearestFlowNode(point, payload) : null;
		if (nearestNode && flowObjectId(nearestNode)) {
			return {
				target: flowObjectId(nearestNode),
				position: 'after',
				indicator: 'after',
				targetNodeId: nearestNode.id
			};
		}
		return {
			target: selectedSequenceId,
			position: 'inside',
			indicator: 'inside',
			targetNodeId: ''
		};
	}

	/**
	 * @param {import('$lib/studio/dnd').DropPosition} position
	 * @returns {'inside' | 'before' | 'after'}
	 */
	function flowIndicatorFromDropPosition(position) {
		if (position === 'first' || position === 'before') {
			return 'before';
		}
		if (position === 'after') {
			return 'after';
		}
		return 'inside';
	}

	/**
	 * @param {DragEvent} event
	 * @param {import('@xyflow/svelte').Node} node
	 * @returns {'inside' | 'before' | 'after'}
	 */
	function getFlowNodeDropPlacement(event, node) {
		const targetElement = event.target instanceof HTMLElement ? event.target : null;
		const nodeElement = targetElement?.closest('.svelte-flow__node');
		if (nodeElement instanceof HTMLElement) {
			const rect = nodeElement.getBoundingClientRect();
			const x = event.clientX - rect.left;
			if (x < rect.width * flowDropBeforeRatio) {
				return 'before';
			}
			if (x > rect.width * flowDropAfterRatio) {
				return 'after';
			}
			return 'inside';
		}
		const point = flowPointFromEvent(event);
		if (!point) {
			return 'inside';
		}
		const x = point.x - node.position.x;
		if (x < flowNodeSize.width * flowDropBeforeRatio) {
			return 'before';
		}
		if (x > flowNodeSize.width * flowDropAfterRatio) {
			return 'after';
		}
		return 'inside';
	}

	/**
	 * @param {{ x: number, y: number }} point
	 * @param {import('$lib/studio/dnd').DboDragPayload | undefined} payload
	 * @returns {{
	 *  target: string,
	 *  position: 'before' | 'after',
	 *  indicator: 'before' | 'after',
	 *  targetNodeId: string,
	 *  fallbackTarget?: string,
	 *  fallbackPosition?: 'inside',
	 *  dropBranch?: string,
	 *  dropHostLabel?: string
	 * } | null}
	 */
	function findFlowLaneDropTarget(point, payload = undefined) {
		const sourceId = payload?.type === 'treeData' ? (payload.data?.id ?? '') : '';
		return resolveFlowLaneDropTarget({
			point,
			flow,
			xyNodes: nodes,
			sourceId,
			selectedSequenceId,
			nodeSize: flowNodeSize
		});
	}

	/**
	 * @param {import('@xyflow/svelte').Edge} edge
	 * @param {import('$lib/studio/dnd').DboDragPayload | undefined} payload
	 * @returns {{
	 *  target: string,
	 *  position: import('$lib/studio/dnd').DropPosition,
	 *  indicator: 'before' | 'after',
	 *  targetNodeId: string,
	 *  fallbackTarget?: string,
	 *  fallbackPosition?: 'inside',
	 *  dropBranch?: string,
	 *  dropHostLabel?: string
	 * } | null}
	 */
	function resolveFlowEdgeDropTarget(edge, payload = undefined) {
		const sourceNode = nodes.find((node) => node.id === edge.source);
		const targetNode = nodes.find((node) => node.id === edge.target);
		if (!sourceNode || !targetNode) {
			return null;
		}
		const sourceObjectId = flowObjectId(sourceNode);
		const targetObjectId = flowObjectId(targetNode);
		if (!sourceObjectId || !targetObjectId || !flow) {
			return null;
		}
		if (
			isDraggedFlowObject(payload, sourceObjectId) ||
			isDraggedFlowObject(payload, targetObjectId)
		) {
			return null;
		}
		const sourceFlowNode = flow.nodes.find((node) => node.id === sourceNode.id);
		const targetFlowNode = flow.nodes.find((node) => node.id === targetNode.id);
		if (targetFlowNode && flowParentId(targetFlowNode) === sourceObjectId) {
			return {
				target: targetObjectId,
				position: 'before',
				indicator: 'before',
				targetNodeId: targetNode.id,
				fallbackTarget: sourceObjectId,
				fallbackPosition: 'inside',
				dropBranch: flowParentBranch(targetFlowNode),
				dropHostLabel: dropHostLabel(sourceObjectId)
			};
		}
		if (sourceFlowNode && targetFlowNode && flowParentId(sourceFlowNode) === targetObjectId) {
			return {
				target: sourceObjectId,
				position: 'after',
				indicator: 'after',
				targetNodeId: sourceNode.id,
				fallbackTarget: targetObjectId,
				fallbackPosition: 'inside',
				dropBranch: flowParentBranch(sourceFlowNode),
				dropHostLabel: dropHostLabel(targetObjectId)
			};
		}
		const fallbackParentId = targetFlowNode
			? flowParentId(targetFlowNode)
			: sourceFlowNode
				? flowParentId(sourceFlowNode)
				: '';
		return {
			target: sourceObjectId,
			position: 'after',
			indicator: 'after',
			targetNodeId: sourceNode.id,
			fallbackTarget: fallbackParentId || undefined,
			fallbackPosition: fallbackParentId ? 'inside' : undefined,
			dropBranch: sourceFlowNode ? flowParentBranch(sourceFlowNode) : '',
			dropHostLabel: dropHostLabel(fallbackParentId)
		};
	}

	/**
	 * @param {string | undefined} parentId
	 * @returns {string}
	 */
	function dropHostLabel(parentId) {
		if (!parentId || parentId === selectedSequenceId) {
			return '';
		}
		return objectNameFromId(parentId);
	}

	/**
	 * @param {import('./types').FlowNode} node
	 * @returns {string}
	 */
	function flowParentId(node) {
		return typeof node.data?.parentId === 'string' && node.data.parentId
			? node.data.parentId
			: selectedSequenceId;
	}

	/**
	 * @param {import('./types').FlowNode} node
	 * @returns {string}
	 */
	function flowParentBranch(node) {
		return typeof node.data?.parentBranch === 'string' ? node.data.parentBranch : '';
	}

	/**
	 * @param {import('./types').FlowNode} node
	 * @returns {number}
	 */
	function flowOrderIndex(node) {
		return typeof node.data?.orderIndex === 'number' ? node.data.orderIndex : 0;
	}

	/**
	 * @param {import('./types').FlowNode} node
	 * @returns {string}
	 */
	function flowNodeObjectId(node) {
		return typeof node.data?.originalId === 'string' && node.data.originalId
			? node.data.originalId
			: node.id;
	}

	/**
	 * @param {DragEvent} event
	 * @returns {import('@xyflow/svelte').Node | null}
	 */
	function findFlowNodeAtEvent(event) {
		const targetElement = event.target instanceof HTMLElement ? event.target : null;
		const nodeElement = targetElement?.closest('.svelte-flow__node');
		const nodeId = nodeElement?.getAttribute('data-id') ?? '';
		const directNode = nodeId ? nodes.find((node) => node.id === nodeId) : null;
		if (directNode && flowObjectId(directNode)) {
			return directNode;
		}
		const point = flowPointFromEvent(event);
		if (!point) {
			return null;
		}
		return (
			nodes.find(
				(node) =>
					flowObjectId(node) &&
					point.x >= node.position.x &&
					point.x <= node.position.x + flowNodeSize.width &&
					point.y >= node.position.y &&
					point.y <= node.position.y + flowNodeSize.height
			) ?? null
		);
	}

	/**
	 * @param {DragEvent} event
	 * @returns {{ x: number, y: number } | null}
	 */
	function flowPointFromEvent(event) {
		if (!canvasElement) {
			return null;
		}
		const rect = canvasElement.getBoundingClientRect();
		const zoom = flowViewport.zoom || 1;
		return {
			x: (event.clientX - rect.left - flowViewport.x) / zoom,
			y: (event.clientY - rect.top - flowViewport.y) / zoom
		};
	}

	/**
	 * @param {{ x: number, y: number }} point
	 * @returns {import('@xyflow/svelte').Edge | null}
	 */
	function findFlowEdgeNearPoint(point) {
		const zoom = flowViewport.zoom || 1;
		const threshold = 24 / zoom;
		let best = /** @type {{ edge: import('@xyflow/svelte').Edge, distance: number } | null} */ (
			null
		);
		for (const edge of edges) {
			const source = nodes.find((node) => node.id === edge.source);
			const target = nodes.find((node) => node.id === edge.target);
			if (!source || !target || !flowObjectId(source) || !flowObjectId(target)) {
				continue;
			}
			const distance = flowEdgeDistanceToPoint(point, source, target, edge, flowNodeSize);
			if (distance <= threshold && (!best || distance < best.distance)) {
				best = { edge, distance };
			}
		}
		return best?.edge ?? null;
	}

	/**
	 * @param {{ x: number, y: number }} point
	 * @param {import('$lib/studio/dnd').DboDragPayload | undefined} payload
	 * @returns {import('@xyflow/svelte').Node | null}
	 */
	function findNearestFlowNode(point, payload = undefined) {
		let best = /** @type {{ node: import('@xyflow/svelte').Node, distance: number } | null} */ (
			null
		);
		for (const node of nodes) {
			const objectId = flowObjectId(node);
			if (!objectId || isDraggedFlowObject(payload, objectId)) {
				continue;
			}
			const center = nodeCenter(node);
			const distance = Math.hypot(point.x - center.x, point.y - center.y);
			if (!best || distance < best.distance) {
				best = { node, distance };
			}
		}
		return best?.node ?? null;
	}

	/**
	 * @param {import('@xyflow/svelte').Node} node
	 * @returns {{ x: number, y: number }}
	 */
	function nodeCenter(node) {
		return {
			x: node.position.x + flowNodeSize.width / 2,
			y: node.position.y + flowNodeSize.height / 2
		};
	}

	/**
	 * @param {import('@xyflow/svelte').Node} node
	 * @returns {string}
	 */
	function flowObjectId(node) {
		return typeof node.data?.originalId === 'string' && node.data.originalId
			? node.data.originalId
			: '';
	}

	/**
	 * @param {import('$lib/studio/dnd').DboDragPayload | undefined} payload
	 * @param {string} objectId
	 * @returns {boolean}
	 */
	function isDraggedFlowObject(payload, objectId) {
		return Boolean(
			payload?.type === 'treeData' &&
			payload.data?.id &&
			(areEquivalentDboObjectIds(payload.data.id, objectId) ||
				isDescendantObjectId(objectId, payload.data.id))
		);
	}

	/**
	 * @param {import('$lib/studio/dnd').DboDragPayload | undefined} payload
	 * @param {{ target: string, position: import('$lib/studio/dnd').DropPosition } | null} target
	 * @returns {boolean}
	 */
	function isNoopFlowMoveTarget(payload, target) {
		const sourceId = payload?.type === 'treeData' ? (payload.data?.id ?? '') : '';
		if (!sourceId || !target) {
			return false;
		}
		return isNoopSiblingMove({
			payload,
			target: target.target,
			position: target.position,
			sourceSiblingIds: visibleFlowSiblingObjectIds(sourceId)
		});
	}

	/**
	 * @param {string} sourceId
	 * @returns {string[]}
	 */
	function visibleFlowSiblingObjectIds(sourceId) {
		if (!flow || !sourceId) {
			return [];
		}
		const sourceNode = flow.nodes.find((node) =>
			areEquivalentDboObjectIds(flowNodeObjectId(node), sourceId)
		);
		if (!sourceNode) {
			return [];
		}
		const visibleNodeIds = new Set(nodes.map((node) => node.id));
		const parentId = flowParentId(sourceNode);
		const parentBranch = flowParentBranch(sourceNode);
		return flow.nodes
			.filter(
				(node) =>
					(visibleNodeIds.has(node.id) || node.id === sourceNode.id) &&
					flowParentId(node) === parentId &&
					flowParentBranch(node) === parentBranch
			)
			.sort((left, right) => flowOrderIndex(left) - flowOrderIndex(right))
			.map(flowNodeObjectId)
			.filter(Boolean);
	}

	/**
	 * @param {{ fit?: 'all', focusNodeIds?: Set<string> }} [options]
	 */
	function syncXyFlow(options = {}) {
		if (!flow) {
			nodes = [];
			edges = [];
			return;
		}
		const descendantCountByNodeId = collectAllSubstepDescendantCounts(flow);
		const hiddenNodeIds = collectHiddenSubstepNodeIds(flow, collapsedSubstepParents);
		const next = toXyFlow(flow, {
			hiddenNodeIds,
			collapsedNodeIds: collapsedSubstepParents,
			descendantCountByNodeId,
			onToggleSubsteps: toggleSubsteps,
			onRenameObject: renameFlowObject,
			onRequestRenameObject: requestFlowRename,
			onDeleteObject: deleteFlowObject,
			selectedObjectId,
			dropTargetNodeId: flowDropAllowed || flowDropDenied ? flowDropTargetNodeId : '',
			dropDenied: flowDropDenied,
			dropPosition: flowDropPosition,
			dropBranch: flowDropBranch,
			dropHostLabel: flowDropHostLabel,
			renameObjectId: flowRenameObjectId
		});
		nodes = next.nodes;
		edges = next.edges;
		if (selectedNode && hiddenNodeIds.has(selectedNode.id)) {
			selectedNode = null;
		}
		if (options.fit === 'all') {
			scheduleViewportFit();
		} else if (options.focusNodeIds?.size) {
			scheduleViewportFit(options.focusNodeIds);
		}
	}

	/**
	 * @param {string} nodeId
	 * @param {boolean} expandAll
	 */
	function toggleSubsteps(nodeId, expandAll) {
		if (!flow) {
			return;
		}
		const nextCollapsed = new SvelteSet(collapsedSubstepParents);
		const descendants = collectSubstepDescendants(flow, nodeId);
		const children = collectSubstepChildren(flow, nodeId);
		const wasCollapsed = nextCollapsed.has(nodeId);
		if (nextCollapsed.has(nodeId)) {
			nextCollapsed.delete(nodeId);
			if (expandAll) {
				descendants.forEach((id) => nextCollapsed.delete(id));
			}
		} else {
			nextCollapsed.add(nodeId);
			const descendantCountByNodeId = collectAllSubstepDescendantCounts(flow);
			descendants.forEach((id) => {
				if ((descendantCountByNodeId.get(id) ?? 0) > 0) {
					nextCollapsed.add(id);
				}
			});
		}
		collapsedSubstepParents = nextCollapsed;
		syncXyFlow({
			focusNodeIds: wasCollapsed ? new SvelteSet([nodeId, ...children]) : new SvelteSet([nodeId])
		});
	}

	/**
	 * @param {string} objectId
	 */
	function applySelectedObjectId(objectId) {
		if (!nodes.length) {
			if (!objectId) {
				selectedNode = null;
			}
			return;
		}
		let changed = false;
		const nextNodes = nodes.map((node) => {
			const isSelected = isXyNodeSelected(node, objectId);
			if (node.selected === isSelected && node.data?.isSelected === isSelected) {
				return node;
			}
			changed = true;
			return {
				...node,
				selected: isSelected,
				data: {
					...node.data,
					isSelected
				}
			};
		});
		if (changed) {
			nodes = nextNodes;
		}
		const matchedNode = nextNodes.find((node) => isXyNodeSelected(node, objectId));
		const nextSelectedNode = matchedNode
			? /** @type {{ id: string, data: import('./types').FlowStepNodeData }} */ (
					/** @type {unknown} */ (matchedNode)
				)
			: null;
		const nextSelectedNodeId = nextSelectedNode?.id;
		if (selectedNode?.id !== nextSelectedNodeId) {
			selectedNode = nextSelectedNode;
		}
	}

	/**
	 * @param {string} objectId
	 * @returns {import('./types').FlowNode | undefined}
	 */
	function findFlowNodeByObjectId(objectId) {
		if (!flow || !objectId) {
			return undefined;
		}
		const equivalentIds = new Set(equivalentDboObjectIds(objectId));
		return flow.nodes.find(
			(node) =>
				equivalentIds.has(node.id) ||
				(typeof node.data?.originalId === 'string' && equivalentIds.has(node.data.originalId))
		);
	}

	/**
	 * @param {string} nodeId
	 * @returns {boolean}
	 */
	function expandCollapsedAncestorsForNode(nodeId) {
		if (!flow || !nodeId || !collapsedSubstepParents.size) {
			return false;
		}
		let changed = false;
		const nextCollapsed = new SvelteSet(collapsedSubstepParents);
		for (const parentId of collapsedSubstepParents) {
			if (collectSubstepDescendants(flow, parentId).has(nodeId)) {
				nextCollapsed.delete(parentId);
				changed = true;
			}
		}
		if (changed) {
			collapsedSubstepParents = nextCollapsed;
		}
		return changed;
	}

	/**
	 * @param {import('@xyflow/svelte').Node} node
	 * @param {string} objectId
	 * @returns {boolean}
	 */
	function isXyNodeSelected(node, objectId) {
		if (!objectId) {
			return false;
		}
		const equivalentIds = new Set(equivalentDboObjectIds(objectId));
		return (
			equivalentIds.has(node.id) ||
			(typeof node.data?.originalId === 'string' && equivalentIds.has(node.data.originalId))
		);
	}

	/**
	 * @param {string} objectId
	 * @param {string} nextName
	 */
	async function renameFlowObject(objectId, nextName) {
		if (!objectId || !nextName) {
			flowRenameObjectId = '';
			syncXyFlow();
			return;
		}
		if (nextName === objectNameFromId(objectId)) {
			flowRenameObjectId = '';
			syncXyFlow();
			return;
		}
		const result = await renameDbo(objectId, nextName, 'UPDATE_NONE');
		if (!result?.done) {
			return;
		}
		const nextId = renameObjectId(objectId, nextName);
		flowRenameObjectId = '';
		await onMutation?.({
			done: true,
			id: nextId,
			selectedId: nextId,
			target: objectId,
			position: 'inside',
			source: 'flow',
			payload: {
				type: 'renameData',
				data: { id: objectId }
			}
		});
		if (!onMutation) {
			refresh();
		}
	}

	/**
	 * @param {string} objectId
	 */
	function requestFlowRename(objectId) {
		if (!objectId) {
			return;
		}
		flowRenameObjectId = objectId;
		syncXyFlow();
	}

	/**
	 * @param {string} objectId
	 */
	async function deleteFlowObject(objectId) {
		if (!objectId) {
			return;
		}
		const name = objectNameFromId(objectId);
		if (
			typeof window !== 'undefined' &&
			!window.confirm(`Delete "${name}"?\n\nThis action cannot be undone.`)
		) {
			return;
		}
		const result = await removeDbo(objectId);
		if (!result?.done) {
			return;
		}
		flowRenameObjectId = '';
		const nextSelection = parentObjectId(objectId) || selectedSequenceId;
		await onMutation?.({
			done: true,
			id: nextSelection,
			selectedId: nextSelection,
			target: objectId,
			position: 'inside',
			source: 'flow',
			payload: {
				type: 'deleteData',
				data: { id: objectId }
			}
		});
		if (!onMutation) {
			refresh();
		}
	}

	/**
	 * @param {import('./types').Flow} nextFlow
	 * @returns {Set<string>}
	 */
	function initialCollapsedSubsteps(nextFlow) {
		const descendantCountByNodeId = collectAllSubstepDescendantCounts(nextFlow);
		return new SvelteSet(
			Array.from(descendantCountByNodeId.entries())
				.filter(([, count]) => count > 0)
				.map(([nodeId]) => nodeId)
		);
	}

	/**
	 * @param {import('./types').Flow} nextFlow
	 * @param {import('./types').Flow | null} previousFlow
	 * @param {Set<string>} previousCollapsed
	 * @returns {Set<string>}
	 */
	function reconcileCollapsedSubsteps(nextFlow, previousFlow, previousCollapsed) {
		if (!previousFlow) {
			return initialCollapsedSubsteps(nextFlow);
		}
		const previousCollapsibleIds = collectAllSubstepDescendantCounts(previousFlow);
		const previousCollapsibleNodeIds = new Set(previousCollapsibleIds.keys());
		const nextCollapsibleIds = collectAllSubstepDescendantCounts(nextFlow);
		const nextCollapsed = new SvelteSet();
		nextCollapsibleIds.forEach((count, nodeId) => {
			if (!count) {
				return;
			}
			if (
				hasEquivalentObjectId(previousCollapsibleNodeIds, nodeId) &&
				hasEquivalentObjectId(previousCollapsed, nodeId)
			) {
				nextCollapsed.add(nodeId);
			}
		});
		return nextCollapsed;
	}

	/**
	 * @param {import('./types').Flow} nextFlow
	 * @param {Set<string>} nextCollapsed
	 */
	function expandSelectedObjectParents(nextFlow, nextCollapsed) {
		const objectId = selectedObjectId;
		if (!objectId || !nextCollapsed.size) {
			return;
		}
		const selectedFlowNode = findFlowNodeByObjectIdInFlow(nextFlow, objectId);
		if (!selectedFlowNode) {
			return;
		}
		for (const parentId of Array.from(nextCollapsed)) {
			if (collectSubstepDescendants(nextFlow, parentId).has(selectedFlowNode.id)) {
				nextCollapsed.delete(parentId);
			}
		}
	}

	/**
	 * @param {import('$lib/studio/dnd').DboDropResult} mutation
	 */
	function rememberExpandedParentsForMutation(mutation) {
		const nextExpanded = new SvelteSet(pendingExpandedSubstepParents);
		for (const id of mutationDboContextIds(mutation)) {
			nextExpanded.add(id);
		}
		pendingExpandedSubstepParents = nextExpanded;
	}

	/**
	 * @param {string | undefined} objectId
	 */
	function queueViewportFocusObject(objectId) {
		if (!objectId) {
			return;
		}
		const nextFocusObjectIds = new SvelteSet(pendingViewportFocusObjectIds);
		nextFocusObjectIds.add(objectId);
		pendingViewportFocusObjectIds = nextFocusObjectIds;
	}

	/**
	 * @param {import('./types').Flow} nextFlow
	 * @returns {Set<string>}
	 */
	function takePendingViewportFocusNodeIds(nextFlow) {
		const focusObjectIds = new SvelteSet(pendingViewportFocusObjectIds);
		pendingViewportFocusObjectIds = new SvelteSet();
		const focusNodeIds = new SvelteSet();
		for (const objectId of focusObjectIds) {
			const node = findFlowNodeByObjectIdInFlow(nextFlow, objectId);
			if (node?.id) {
				focusNodeIds.add(node.id);
			}
		}
		return focusNodeIds;
	}

	/**
	 * @param {import('./types').Flow} nextFlow
	 * @param {string} objectId
	 * @returns {import('./types').FlowNode | undefined}
	 */
	function findFlowNodeByObjectIdInFlow(nextFlow, objectId) {
		if (!objectId) {
			return undefined;
		}
		const equivalentIds = new Set(equivalentDboObjectIds(objectId));
		return nextFlow.nodes.find(
			(node) =>
				equivalentIds.has(node.id) ||
				(typeof node.data?.originalId === 'string' && equivalentIds.has(node.data.originalId))
		);
	}

	/**
	 * @param {import('./types').Flow} nextFlow
	 * @returns {Map<string, number>}
	 */
	function collectAllSubstepDescendantCounts(nextFlow) {
		const counts = new SvelteMap();
		for (const node of nextFlow.nodes) {
			const descendants = collectSubstepDescendants(nextFlow, node.id);
			if (descendants.size) {
				counts.set(node.id, descendants.size);
			}
		}
		return counts;
	}

	/**
	 * @param {import('./types').Flow} nextFlow
	 * @param {Set<string>} collapsedNodeIds
	 * @returns {Set<string>}
	 */
	function collectHiddenSubstepNodeIds(nextFlow, collapsedNodeIds) {
		const hidden = new SvelteSet();
		for (const nodeId of collapsedNodeIds) {
			collectSubstepDescendants(nextFlow, nodeId).forEach((id) => hidden.add(id));
		}
		return hidden;
	}

	/**
	 * @param {import('./types').Flow} nextFlow
	 * @param {string} parentId
	 * @returns {Set<string>}
	 */
	function collectSubstepDescendants(nextFlow, parentId) {
		const parentNode = findFlowNodeByObjectIdInFlow(nextFlow, parentId);
		if (!parentNode) {
			return new SvelteSet();
		}
		const effectiveParentId = parentNode.id;
		const outgoing = new SvelteMap();
		for (const link of nextFlow.links) {
			const links = outgoing.get(link.from.nodeId) ?? [];
			links.push(link);
			outgoing.set(link.from.nodeId, links);
		}
		const queue = (outgoing.get(effectiveParentId) ?? [])
			.filter((link) => isBottomOutput(parentNode, link.from.portIndex))
			.map((link) => link.to.nodeId);
		const descendants = new SvelteSet();
		const visited = new SvelteSet();
		while (queue.length) {
			const nodeId = queue.shift();
			if (!nodeId || nodeId === effectiveParentId || visited.has(nodeId)) {
				continue;
			}
			visited.add(nodeId);
			descendants.add(nodeId);
			for (const link of outgoing.get(nodeId) ?? []) {
				if (link.to.nodeId !== effectiveParentId && !descendants.has(link.to.nodeId)) {
					queue.push(link.to.nodeId);
				}
			}
		}
		return descendants;
	}

	/**
	 * @param {import('./types').Flow} nextFlow
	 * @param {string} parentId
	 * @returns {Set<string>}
	 */
	function collectSubstepChildren(nextFlow, parentId) {
		const parentNode = findFlowNodeByObjectIdInFlow(nextFlow, parentId);
		if (!parentNode) {
			return new SvelteSet();
		}
		const effectiveParentId = parentNode.id;
		const children = new SvelteSet();
		for (const link of nextFlow.links) {
			if (
				link.from.nodeId === effectiveParentId &&
				isBottomOutput(parentNode, link.from.portIndex)
			) {
				children.add(link.to.nodeId);
			}
		}
		return children;
	}

	/**
	 * @param {Set<string>} ids
	 * @param {string} objectId
	 * @returns {boolean}
	 */
	function hasEquivalentObjectId(ids, objectId) {
		for (const id of ids) {
			if (areEquivalentDboObjectIds(id, objectId)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param {Set<string>} ids
	 * @param {string} objectId
	 */
	function deleteEquivalentObjectId(ids, objectId) {
		for (const id of Array.from(ids)) {
			if (areEquivalentDboObjectIds(id, objectId)) {
				ids.delete(id);
			}
		}
	}

	/**
	 * @param {import('./types').FlowNode} node
	 * @param {number} portIndex
	 * @returns {boolean}
	 */
	function isBottomOutput(node, portIndex) {
		const outputs = node.outputs ?? 0;
		const bottomOutputs = node.bottomOutputs ?? 0;
		if (!bottomOutputs || !outputs) {
			return false;
		}
		return portIndex >= outputs - bottomOutputs;
	}

	/**
	 * @param {Set<string>=} nodeIds
	 */
	function scheduleViewportFit(nodeIds) {
		const serial = ++fitSerial;
		if (typeof requestAnimationFrame === 'undefined') {
			return;
		}
		requestAnimationFrame(() => {
			if (serial !== fitSerial) {
				return;
			}
			fitViewportToNodes(nodeIds);
		});
	}

	/**
	 * @param {Set<string>=} nodeIds
	 */
	function fitViewportToNodes(nodeIds) {
		if (!canvasElement || !nodes.length) {
			return;
		}
		const width = canvasElement.clientWidth;
		const height = canvasElement.clientHeight;
		if (!width || !height) {
			return;
		}
		const targetNodes = nodeIds?.size ? nodes.filter((node) => nodeIds.has(node.id)) : nodes;
		const nodesToFit = targetNodes.length ? targetNodes : nodes;
		const xs = nodesToFit.map((node) => node.position.x);
		const ys = nodesToFit.map((node) => node.position.y);
		const minX = Math.min(...xs);
		const maxX = Math.max(...xs);
		const minY = Math.min(...ys);
		const maxY = Math.max(...ys);
		flowViewport = getViewportForBounds(
			{
				x: minX,
				y: minY,
				width: maxX - minX + flowNodeSize.width,
				height: maxY - minY + flowNodeSize.height
			},
			width,
			height,
			0.15,
			nodeIds?.size ? 0.82 : 0.92,
			nodeIds?.size ? 0.24 : 0.18
		);
	}

	/**
	 * @param {string | undefined} accessibility
	 * @returns {string}
	 */
	function accessibilityIcon(accessibility) {
		const normalized = (accessibility ?? 'Public').toLowerCase();
		if (normalized === 'private') {
			return 'mdi:lock';
		}
		if (normalized === 'hidden') {
			return 'mdi:eye-off';
		}
		return 'mdi:lock-open-variant';
	}
</script>

<div
	class="flow-dashboard"
	class:flow-dashboard--no-left={!showSequences && !showPalette}
	class:flow-dashboard--no-inspector={!showInspector}
>
	{#if showSequences || showPalette}
		<aside class="flow-dashboard__sequences">
			{#if showSequences}
				<section class="flow-dashboard__sequence-panel">
					<div class="flow-dashboard__panel-header">
						<span>Sequences</span>
						<span class="flow-dashboard__count">{filteredSequences.length}</span>
					</div>
					<InputGroup
						id="flow-sequence-search"
						type="search"
						placeholder="Search sequence..."
						class="w-full"
						icon="mdi:magnify"
						bind:value={sequenceQuery}
					/>
					<div class="flow-dashboard__sequence-list">
						{#each filteredSequences as sequence (sequence.name)}
							<button
								type="button"
								class:flow-dashboard__sequence--selected={sequence.name === selectedSequenceName}
								class="flow-dashboard__sequence"
								onclick={() => (selectedSequenceName = sequence.name)}
							>
								<span class="flow-dashboard__sequence-icon">
									<Ico icon={accessibilityIcon(sequence.accessibility)} size={4} />
								</span>
								<span class="flow-dashboard__sequence-main">
									<span class="flow-dashboard__sequence-name">{sequence.name}</span>
									{#if sequence.comment}
										<span class="flow-dashboard__sequence-comment">{sequence.comment}</span>
									{/if}
								</span>
								{#if sequence.autostart}
									<span class="flow-dashboard__sequence-flag">
										<Ico icon="mdi:lightning-bolt" size={3} />
									</span>
								{/if}
							</button>
						{/each}
					</div>
				</section>
			{/if}
			{#if showPalette}
				<FlowPalette parentId={selectedSequenceId} />
			{/if}
		</aside>
	{/if}

	<section class="flow-dashboard__viewer">
		<div class="flow-dashboard__toolbar">
			<div class="flow-dashboard__title">
				<span>{selectedSequence?.name ?? 'Flow'}</span>
				{#if flow}
					<span class="flow-dashboard__metrics">
						{flow.nodes.length} steps · {flow.links.length} links
					</span>
				{/if}
			</div>
			<Button
				full={false}
				icon="mdi:refresh"
				class="button-ico-secondary h-9! w-9! justify-center p-0!"
				title="Refresh flow"
				ariaLabel="Refresh flow"
				disabled={loading || !selectedSequenceName}
				onclick={refresh}
			/>
		</div>
		<div
			class="flow-dashboard__canvas"
			class:flow-dashboard__canvas--drop={flowDropAllowed}
			class:flow-dashboard__canvas--drop-denied={flowDropDenied}
			data-testid="dashboard-flow-canvas"
			role="presentation"
			bind:this={canvasElement}
			ondragenter={handleFlowDragOver}
			ondragover={handleFlowDragOver}
			ondragleave={handleFlowDragLeave}
			ondrop={handleFlowDrop}
		>
			{#if loading}
				<div class="flow-dashboard__center">
					<AutoPlaceholder class="w-72" loading={true} />
					<AutoPlaceholder class="h-4 w-48" loading={true} />
				</div>
			{:else if error}
				<div class="flow-dashboard__center flow-dashboard__error">{error}</div>
			{:else if nodes.length}
				<SvelteFlow
					bind:nodes
					bind:edges
					bind:viewport={flowViewport}
					{nodeTypes}
					initialViewport={flowViewport}
					minZoom={0.15}
					maxZoom={1.6}
					nodesDraggable={false}
					nodesConnectable={false}
					elementsSelectable={true}
					defaultEdgeOptions={{ type: 'smoothstep', zIndex: 0 }}
					fitViewOptions={{ padding: 0.18, maxZoom: 0.92 }}
					onnodeclick={handleNodeClick}
					onpaneclick={handlePaneClick}
				>
					<Background />
					<Controls />
					<MiniMap pannable zoomable />
				</SvelteFlow>
			{:else}
				<div class="flow-dashboard__center">No sequence selected</div>
			{/if}
		</div>
	</section>

	{#if showInspector}
		<aside class="flow-dashboard__inspector">
			<div class="flow-dashboard__panel-header">
				<span>Step</span>
			</div>
			{#if selectedNode}
				<div class="flow-dashboard__inspector-stack">
					<div>
						<div class="flow-dashboard__inspector-label">Name</div>
						<div class="flow-dashboard__inspector-value">{selectedNode.data.name}</div>
					</div>
					<div>
						<div class="flow-dashboard__inspector-label">Type</div>
						<div class="flow-dashboard__inspector-value">
							{selectedNode.data.type.split('.').pop()}
						</div>
					</div>
					{#if selectedNode.data.classname}
						<div>
							<div class="flow-dashboard__inspector-label">Class</div>
							<div class="flow-dashboard__inspector-value">{selectedNode.data.classname}</div>
						</div>
					{/if}
					{#if selectedNode.data.originalId}
						<div>
							<div class="flow-dashboard__inspector-label">QName</div>
							<div class="flow-dashboard__inspector-value flow-dashboard__mono">
								{selectedNode.data.originalId}
							</div>
						</div>
					{/if}
					<div class="flow-dashboard__ports">
						<span>{selectedNode.data.inputs} inputs</span>
						<span>{selectedNode.data.outputs} outputs</span>
					</div>
				</div>
			{:else}
				<div class="flow-dashboard__empty">Select a step</div>
			{/if}
		</aside>
	{/if}
</div>

<style>
	.flow-dashboard {
		--flow-canvas-bg: light-dark(var(--color-surface-50), var(--color-surface-950));
		--flow-toolbar-bg: light-dark(var(--color-surface-100), var(--color-surface-900));
		--flow-toolbar-text: light-dark(var(--color-surface-950), var(--color-surface-50));
		--flow-toolbar-border: light-dark(var(--color-surface-200), var(--color-surface-800));
		--flow-edge-stroke: light-dark(var(--color-primary-600), var(--color-primary-400));
		--flow-edge-label-bg: light-dark(var(--color-surface-100), var(--color-surface-900));
		--flow-edge-label-text: light-dark(var(--color-primary-800), var(--color-primary-300));
		--flow-node-bg-start: light-dark(var(--color-surface-100), var(--color-surface-900));
		--flow-node-bg-end: light-dark(var(--color-surface-50), var(--color-surface-950));
		--flow-node-border-base: light-dark(var(--color-surface-300), var(--color-surface-700));
		--flow-node-text: light-dark(var(--color-surface-950), var(--color-surface-50));
		--flow-node-muted: light-dark(var(--color-surface-600), var(--color-surface-400));
		--flow-node-shadow: light-dark(rgb(15 23 42 / 0.16), rgb(0 0 0 / 0.48));
		--flow-node-inset: light-dark(rgb(255 255 255 / 0.9), rgb(255 255 255 / 0.05));
		--flow-node-handle-bg: light-dark(var(--color-surface-100), var(--color-surface-900));
		--flow-node-handle-border: light-dark(var(--color-surface-200), var(--color-surface-800));
		--flow-node-handle-text: light-dark(var(--color-surface-700), var(--color-surface-300));
		--flow-node-input-bg: light-dark(var(--color-surface-100), var(--color-surface-900));
		--flow-node-input-text: light-dark(var(--color-surface-950), var(--color-surface-50));
		--flow-node-bottom-input: light-dark(var(--color-surface-950), var(--color-surface-50));
		--flow-node-bottom-output: light-dark(var(--color-surface-100), var(--color-surface-950));
		--flow-node-toggle-bg: light-dark(var(--color-surface-100), var(--color-surface-900));
		--flow-node-toggle-text: light-dark(var(--color-surface-950), var(--color-surface-50));
		--flow-node-toggle-border: light-dark(var(--color-surface-300), var(--color-surface-700));
		--flow-node-toggle-hover-bg: light-dark(var(--color-surface-200), var(--color-surface-800));
		--flow-node-toggle-hover-border: var(--color-surface-500);
		--flow-node-toggle-collapsed-bg: light-dark(var(--color-error-100), var(--color-error-900));
		--flow-node-toggle-collapsed-border: light-dark(var(--color-error-300), var(--color-error-800));
		--flow-node-selected-border: light-dark(var(--color-primary-600), var(--color-primary-300));
		--flow-node-selected-ring: light-dark(rgb(24 207 255 / 0.55), rgb(125 211 252 / 0.72));
		--flow-node-selected-glow: light-dark(rgb(24 207 255 / 0.24), rgb(56 189 248 / 0.48));
		--flow-node-selected-halo-border: light-dark(rgb(24 207 255 / 0.55), rgb(125 211 252 / 0.72));
		--flow-node-selected-halo-bg: light-dark(rgb(24 207 255 / 0.12), rgb(14 165 233 / 0.24));
		--flow-node-selected-halo-shadow: light-dark(rgb(24 207 255 / 0.22), rgb(14 165 233 / 0.48));
		--flow-drop-allowed: light-dark(var(--color-success-700), #22c55e);
		--flow-drop-allowed-soft: light-dark(rgb(124 143 132 / 0.16), rgb(34 197 94 / 0.18));
		--flow-drop-allowed-ring: light-dark(rgb(124 143 132 / 0.5), rgb(34 197 94 / 0.72));
		--flow-drop-denied: light-dark(var(--color-warning-700), #f97316);
		--flow-drop-denied-soft: light-dark(rgb(242 140 106 / 0.16), rgb(249 115 22 / 0.18));
		--flow-drop-denied-ring: light-dark(rgb(242 140 106 / 0.54), rgb(249 115 22 / 0.72));
		--flow-drop-zone-label-text: light-dark(var(--color-success-800), #dcfce7);
		--flow-drop-denied-zone-label-text: light-dark(var(--color-warning-800), #ffedd5);
		--flow-drop-badge-bg: light-dark(var(--color-success-50), #052e16);
		--flow-drop-badge-text: light-dark(var(--color-success-800), #bbf7d0);
		--flow-drop-denied-badge-bg: light-dark(var(--color-warning-50), #431407);
		--flow-drop-denied-badge-text: light-dark(var(--color-warning-800), #fed7aa);
		--flow-node-port-label-bg: light-dark(rgb(255 255 255 / 0.95), rgb(15 23 42 / 0.92));
		--flow-node-port-label-border: light-dark(rgb(112 117 120 / 0.38), rgb(148 163 184 / 0.5));
		--flow-node-port-label-text: light-dark(var(--color-surface-950), var(--color-surface-100));
		--flow-controls-bg: light-dark(var(--color-surface-100), var(--color-surface-900));
		--flow-controls-text: light-dark(var(--color-surface-900), var(--color-surface-100));
		--flow-controls-border: light-dark(rgb(112 117 120 / 0.28), rgb(148 163 184 / 0.22));
		--flow-controls-shadow: light-dark(rgb(15 23 42 / 0.12), rgb(0 0 0 / 0.42));
		--flow-minimap-bg: light-dark(rgb(255 255 255 / 0.88), rgb(29 32 36 / 0.88));
		--flow-minimap-mask-bg: light-dark(rgb(231 240 254 / 0.68), rgb(11 12 15 / 0.54));
		--flow-minimap-node-bg: light-dark(var(--color-surface-600), var(--color-surface-200));
		--flow-minimap-node-border: light-dark(rgb(112 117 120 / 0.46), rgb(251 253 255 / 0.42));
		--flow-handle-border: light-dark(var(--color-surface-100), var(--color-surface-950));
		--xy-background-color: var(--flow-canvas-bg);
		--xy-background-pattern-color: light-dark(rgb(0 47 83 / 0.18), rgb(125 211 252 / 0.24));
		--xy-controls-button-background-color: var(--flow-controls-bg);
		--xy-controls-button-background-color-hover: var(--flow-node-toggle-hover-bg);
		--xy-controls-button-color: var(--flow-controls-text);
		--xy-controls-button-color-hover: var(--flow-toolbar-text);
		--xy-controls-button-border-color: var(--flow-controls-border);
		--xy-controls-box-shadow: 0 16px 32px -24px var(--flow-controls-shadow);
		--xy-minimap-background-color: var(--flow-minimap-bg);
		--xy-minimap-mask-background-color: var(--flow-minimap-mask-bg);
		--xy-minimap-node-background-color: var(--flow-minimap-node-bg);
		--xy-minimap-node-stroke-color: var(--flow-minimap-node-border);
		display: grid;
		grid-template-columns: minmax(13rem, 17rem) minmax(0, 1fr) minmax(13rem, 18rem);
		min-height: min(72vh, 760px);
		height: calc(100vh - 12rem);
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.5rem;
		background: var(--color-surface-50-950);
		overflow: hidden;
	}

	.flow-dashboard--no-left {
		grid-template-columns: minmax(0, 1fr) minmax(13rem, 18rem);
	}

	.flow-dashboard--no-inspector {
		grid-template-columns: minmax(13rem, 17rem) minmax(0, 1fr);
	}

	.flow-dashboard--no-left.flow-dashboard--no-inspector {
		grid-template-columns: minmax(0, 1fr);
	}

	.flow-dashboard__sequences,
	.flow-dashboard__inspector {
		display: flex;
		min-height: 0;
		flex-direction: column;
		gap: 0.65rem;
		padding: 0.75rem;
		background: color-mix(in oklab, var(--color-surface-100-900) 82%, transparent);
	}

	.flow-dashboard__sequences {
		border-right: 1px solid var(--color-surface-200-800);
		overflow: hidden;
	}

	.flow-dashboard__sequence-panel {
		display: flex;
		min-height: 0;
		flex: 0 0 clamp(12rem, 38%, 18rem);
		flex-direction: column;
		gap: 0.65rem;
	}

	.flow-dashboard__inspector {
		border-left: 1px solid var(--color-surface-200-800);
	}

	.flow-dashboard__panel-header,
	.flow-dashboard__toolbar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 0.75rem;
	}

	.flow-dashboard__panel-header {
		font-size: 0.8rem;
		font-weight: 700;
		text-transform: uppercase;
		color: var(--color-surface-700-300);
	}

	.flow-dashboard__count,
	.flow-dashboard__metrics,
	.flow-dashboard__sequence-flag,
	.flow-dashboard__ports span {
		border: 1px solid var(--color-surface-200-800);
		border-radius: 999px;
		background: var(--color-surface-50-950);
		color: var(--color-surface-600-400);
		font-size: 0.68rem;
		font-weight: 650;
		line-height: 1;
		padding: 0.25rem 0.45rem;
	}

	.flow-dashboard__sequence-list {
		display: flex;
		min-height: 0;
		flex: 1;
		flex-direction: column;
		gap: 0.35rem;
		overflow: auto;
		padding-right: 0.15rem;
	}

	.flow-dashboard__sequence {
		display: grid;
		flex: 0 0 auto;
		grid-template-columns: auto minmax(0, 1fr) auto;
		align-items: center;
		gap: 0.55rem;
		width: 100%;
		border: 1px solid transparent;
		border-radius: 0.45rem;
		background: transparent;
		color: var(--color-surface-900-100);
		padding: 0.55rem;
		text-align: left;
		transition:
			background 0.16s ease,
			border-color 0.16s ease,
			color 0.16s ease;
	}

	.flow-dashboard__sequence:hover,
	.flow-dashboard__sequence--selected {
		border-color: color-mix(in oklab, var(--color-primary-500) 38%, transparent);
		background: color-mix(in oklab, var(--color-primary-500) 10%, transparent);
	}

	.flow-dashboard__sequence-icon {
		display: grid;
		place-items: center;
		width: 1.85rem;
		height: 1.85rem;
		border-radius: 0.4rem;
		background: var(--color-surface-50-950);
		color: var(--color-surface-700-300);
	}

	.flow-dashboard__sequence-main {
		min-width: 0;
	}

	.flow-dashboard__sequence-name,
	.flow-dashboard__sequence-comment {
		display: block;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.flow-dashboard__sequence-name {
		font-size: 0.82rem;
		font-weight: 650;
	}

	.flow-dashboard__sequence-comment {
		margin-top: 0.12rem;
		color: var(--color-surface-600-400);
		font-size: 0.72rem;
	}

	.flow-dashboard__viewer {
		display: grid;
		min-width: 0;
		min-height: 0;
		grid-template-rows: auto minmax(0, 1fr);
		background: var(--flow-canvas-bg);
	}

	.flow-dashboard__toolbar {
		border-bottom: 1px solid var(--flow-toolbar-border);
		background: var(--flow-toolbar-bg);
		color: var(--flow-toolbar-text);
		padding: 0.7rem 0.8rem;
	}

	.flow-dashboard__title {
		display: flex;
		min-width: 0;
		align-items: center;
		gap: 0.5rem;
		font-weight: 700;
	}

	.flow-dashboard__canvas {
		position: relative;
		min-width: 0;
		min-height: 0;
		background: var(--flow-canvas-bg);
	}

	.flow-dashboard__canvas--drop {
		box-shadow: inset 0 0 0 2px var(--flow-drop-allowed-ring);
	}

	.flow-dashboard__canvas--drop-denied {
		box-shadow: inset 0 0 0 2px var(--flow-drop-denied-ring);
	}

	.flow-dashboard__center {
		display: grid;
		height: 100%;
		place-content: center;
		gap: 0.75rem;
		color: var(--color-surface-600-400);
		text-align: center;
	}

	.flow-dashboard__error {
		padding: 1.5rem;
		color: var(--color-error-600-400);
	}

	.flow-dashboard__inspector-stack {
		display: flex;
		flex-direction: column;
		gap: 0.8rem;
		overflow: auto;
		font-size: 0.82rem;
	}

	.flow-dashboard__inspector-label {
		margin-bottom: 0.2rem;
		color: var(--color-surface-600-400);
		font-size: 0.68rem;
		font-weight: 700;
		text-transform: uppercase;
	}

	.flow-dashboard__inspector-value {
		overflow-wrap: anywhere;
		color: var(--color-surface-950-50);
	}

	.flow-dashboard__mono {
		font-family:
			ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
		font-size: 0.72rem;
	}

	.flow-dashboard__ports {
		display: flex;
		flex-wrap: wrap;
		gap: 0.4rem;
	}

	.flow-dashboard__empty {
		color: var(--color-surface-600-400);
		font-size: 0.82rem;
	}

	:global(.flow-dashboard .svelte-flow__node.selected .flow-step-node) {
		border-color: var(--flow-node-selected-border);
		box-shadow:
			0 0 0 3px var(--flow-node-selected-ring),
			0 0 34px var(--flow-node-selected-glow),
			0 18px 36px -22px var(--flow-node-shadow);
	}

	:global(.flow-dashboard .svelte-flow__node.selected) {
		z-index: 1000 !important;
	}

	:global(.flow-dashboard .svelte-flow__edges) {
		z-index: 0;
	}

	:global(.flow-dashboard .svelte-flow__nodes) {
		z-index: 1;
	}

	:global(.flow-dashboard .svelte-flow__edge-path) {
		stroke: var(--flow-edge-stroke);
		stroke-width: 2;
	}

	:global(.flow-dashboard .svelte-flow__edge-textbg) {
		fill: var(--flow-edge-label-bg);
	}

	:global(.flow-dashboard .svelte-flow__edge-text) {
		fill: var(--flow-edge-label-text);
		font-size: 0.62rem;
		font-weight: 700;
	}

	:global(.flow-dashboard .svelte-flow__background) {
		background-color: var(--flow-canvas-bg);
	}

	:global(.flow-dashboard .svelte-flow__controls) {
		overflow: hidden;
		border: 1px solid var(--flow-controls-border);
		border-radius: 0.45rem;
		box-shadow: 0 16px 32px -24px var(--flow-controls-shadow);
	}

	:global(.flow-dashboard .svelte-flow__controls-button) {
		border-bottom-color: var(--flow-controls-border);
		background: var(--flow-controls-bg);
		color: var(--flow-controls-text);
	}

	:global(.flow-dashboard .svelte-flow__minimap) {
		border: 1px solid var(--flow-controls-border);
		border-radius: 0.45rem;
		background: var(--flow-minimap-bg);
	}

	@media (max-width: 980px) {
		.flow-dashboard {
			grid-template-columns: minmax(11rem, 15rem) minmax(0, 1fr);
		}

		.flow-dashboard__inspector {
			display: none;
		}
	}

	@media (max-width: 720px) {
		.flow-dashboard {
			grid-template-columns: 1fr;
			height: auto;
			min-height: 78vh;
		}

		.flow-dashboard__sequences {
			max-height: 32rem;
			border-right: 0;
			border-bottom: 1px solid var(--color-surface-200-800);
		}

		.flow-dashboard__sequence-panel {
			flex: 0 0 12rem;
		}

		.flow-dashboard__canvas {
			min-height: 34rem;
		}
	}
</style>
