<script>
	import Projects from '$lib/common/Projects.svelte.js';
	import { createProjectTree } from '$lib/common/ProjectsTree.svelte.js';
	import { onMount } from 'svelte';
	import { SvelteSet } from 'svelte/reactivity';
	import { areEquivalentDboObjectIds, equivalentDboObjectIds, mutationDboContextIds } from './dnd';
	import StudioEmptyState from './StudioEmptyState.svelte';
	import StudioTreeNode from './StudioTreeNode.svelte';

	/**
	 * @type {{
	 *  selectedId?: string,
	 *  renameTargetId?: string,
	 *  autoSelectFirst?: boolean,
	 *  refreshSerial?: number,
	 *  refreshMutation?: import('./dnd').DboDropResult | null,
	 *  refreshMutationSerial?: number,
	 *  onMutation?: (mutation: import('./dnd').DboDropResult) => void | Promise<void>,
	 *  onMutationBusyChange?: (busy: boolean, handled?: boolean) => void,
	 *  onContextAction?: (event: { nodeId: string, action: any, result: any }) => void | Promise<void>,
	 *  onSourceDrop?: (targetId: string, payload: import('./sourcePickerDnd').SourcePickerDragPayload) => void | Promise<void>
	 * }}
	 */
	let {
		selectedId = $bindable(''),
		renameTargetId = $bindable(''),
		autoSelectFirst = true,
		refreshSerial = 0,
		refreshMutation = null,
		refreshMutationSerial = 0,
		onMutation,
		onMutationBusyChange,
		onContextAction,
		onSourceDrop
	} = $props();

	const { checkChildren, checkNodes } = createProjectTree({
		equivalentIds: equivalentDboObjectIds
	});
	/** @type {Record<string, any>} */
	const rootNodeCache = {};
	let expandedNodeIds = $state.raw(new SvelteSet());
	let dataSerial = $state(0);
	let lastRefreshMutationSerial = 0;
	let rootChildren = $derived.by(() =>
		(Projects.projects ?? [])
			.filter((project) => project?.name)
			.map((project) => {
				if (!rootNodeCache[project.name]) {
					rootNodeCache[project.name] = {
						id: project.name,
						name: project.name,
						label: project.name,
						icon: 'folder',
						children: true
					};
				}
				return rootNodeCache[project.name];
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
		void refreshMutationContext(serial, mutation);
	});

	/**
	 * @param {any} node
	 * @param {boolean=} force
	 */
	async function loadChildren(node, force = false) {
		await checkChildren(node, force);
	}

	/**
	 * @param {import('./dnd').DboDropResult} mutation
	 */
	async function handleMutation(mutation) {
		const parentIds = mutationDboContextIds(mutation);
		keepExpanded(parentIds);
		await refreshAffectedParents(parentIds);
		dataSerial += 1;
		await onMutation?.(mutation);
	}

	/**
	 * @param {number} serial
	 * @param {import('./dnd').DboDropResult} mutation
	 */
	async function refreshMutationContext(serial, mutation) {
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
</script>

<div class="studio-tree" role="tree" aria-label="Projects">
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
				{expandedNodeIds}
				onSetExpanded={setNodeExpanded}
				onKeepExpanded={keepExpanded}
				onLoadChildren={loadChildren}
				onMutation={handleMutation}
				{onMutationBusyChange}
				{onContextAction}
				{onSourceDrop}
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
