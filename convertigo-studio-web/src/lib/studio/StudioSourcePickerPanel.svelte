<script>
	import { draggedData } from '$lib/utils/dndStore';
	import Ico from '$lib/utils/Ico.svelte';
	import { call } from '$lib/utils/service';
	import { SvelteSet } from 'svelte/reactivity';
	import {
		applySourcePickerDrop,
		setSourcePickerDragData,
		SOURCE_PICKER_DND_TYPE,
		sourceDefinitionFromPayload
	} from './sourcePickerDnd';
	import StudioEmptyState from './StudioEmptyState.svelte';
	import StudioIconButton from './StudioIconButton.svelte';

	/**
	 * @typedef {{
	 *  id: string,
	 *  propertyName?: string,
	 *  displayName?: string,
	 *  value?: any,
	 *  serial?: number
	 * }} PickerTarget
	 */
	/**
	 * @typedef {{
	 *  type?: string,
	 *  label?: string,
	 *  name?: string,
	 *  value?: string,
	 *  xpath?: string,
	 *  displayXpath?: string,
	 *  children?: SourceNode[]
	 * }} SourceNode
	 */
	/**
	 * @typedef {{
	 *  ownerId?: string,
	 *  sourceId?: string,
	 *  sourceName?: string,
	 *  sourcePriority?: string,
	 *  schemaSourceId?: string,
	 *  schemaSourceName?: string,
	 *  anchor?: string,
	 *  xpath?: string,
	 *  displayXpath?: string,
	 *  available?: boolean,
	 *  message?: string,
	 *  tree?: SourceNode,
	 *  result?: SourceNode,
	 *  jsonTree?: SourceNode,
	 *  jsonResult?: SourceNode
	 * }} SourceModel
	 */

	/**
	 * @type {{
	 *  selectedId?: string,
	 *  active?: boolean,
	 *  pickerTarget?: PickerTarget | null,
	 *  onSelectObject?: (id: string) => void,
	 *  onApply?: (id: string, sourceDefinition?: string[]) => void | Promise<void>
	 * }}
	 */
	let {
		selectedId = '',
		active = true,
		pickerTarget = null,
		onSelectObject = () => {},
		onApply = () => {}
	} = $props();

	let linked = $state(true);
	let loading = $state(false);
	let evaluating = $state(false);
	let applying = $state(false);
	let error = $state('');
	let loadSerial = 0;
	let model = $state(/** @type {SourceModel | null} */ (null));
	let xpathSuffixInput = $state('');
	let relativeXPath = $state('.');
	let selectedTreeXPath = $state('');
	let showJsonPreview = $state(false);
	let collapsedNodes = $state.raw(new SvelteSet());

	let targetSource = $derived(parseSourceDefinition(pickerTarget?.value));
	let canApply = $derived(
		Boolean(pickerTarget?.id && pickerTarget?.propertyName && model?.available)
	);
	let anchor = $derived(model?.anchor ?? '');
	let activeSourceId = $derived(sourceRequest().id);
	let domTree = $derived(showJsonPreview ? model?.jsonTree : model?.tree);
	let resultTree = $derived(showJsonPreview ? model?.jsonResult : model?.result);
	let domCount = $derived(countTreeNodes(domTree));
	let resultCount = $derived(countTreeNodes(resultTree));

	$effect(() => {
		if (!active || !linked) {
			return;
		}
		const request = sourceRequest();
		if (!request.id) {
			model = null;
			return;
		}
		const serial = ++loadSerial;
		void loadSource(request, serial);
	});

	/**
	 * @returns {{ id: string, sourcePriority: string, xpath: string }}
	 */
	function sourceRequest() {
		const targetId = pickerTarget?.id ?? '';
		const currentSelection = selectedId || targetId;
		const useExistingSource = Boolean(
			targetId && currentSelection === targetId && targetSource.sourcePriority
		);
		return {
			id: useExistingSource ? targetId : currentSelection,
			sourcePriority: useExistingSource ? targetSource.sourcePriority : '',
			xpath: useExistingSource ? targetSource.xpath : '.'
		};
	}

	/**
	 * @param {{ id: string, sourcePriority: string, xpath: string }} request
	 * @param {number} serial
	 */
	async function loadSource(request, serial) {
		loading = true;
		error = '';
		try {
			const response = await call('studio.sourcepicker.Get', request);
			if (serial !== loadSerial) {
				return;
			}
			model = normalizeModel(response);
			xpathSuffixInput = toXPathSuffix(model.displayXpath || model.anchor || '', model.anchor);
			relativeXPath = model.xpath || '.';
			selectedTreeXPath = '';
			collapsedNodes = new SvelteSet();
		} catch (err) {
			if (serial === loadSerial) {
				error = String(err instanceof Error ? err.message : err);
			}
		} finally {
			if (serial === loadSerial) {
				loading = false;
			}
		}
	}

	/**
	 * @param {any} response
	 * @returns {SourceModel}
	 */
	function normalizeModel(response) {
		return {
			...response,
			available: Boolean(response?.available),
			tree: response?.tree ?? null,
			result: response?.result ?? null,
			jsonTree: response?.jsonTree ?? null,
			jsonResult: response?.jsonResult ?? null
		};
	}

	/**
	 * @param {any} value
	 * @returns {{ sourcePriority: string, xpath: string }}
	 */
	function parseSourceDefinition(value) {
		const parts = normalizeSourceParts(value);
		return {
			sourcePriority: String(parts[0] ?? ''),
			xpath: String(parts[1] ?? '.')
		};
	}

	/**
	 * @param {any} value
	 * @returns {any[]}
	 */
	function normalizeSourceParts(value) {
		if (Array.isArray(value)) {
			return value;
		}
		if (typeof value !== 'string') {
			return [];
		}
		const trimmed = value.trim();
		if (!trimmed) {
			return [];
		}
		if (trimmed.startsWith('[')) {
			try {
				const parsed = JSON.parse(trimmed);
				return Array.isArray(parsed) ? parsed : [];
			} catch {
				return [trimmed];
			}
		}
		return trimmed.includes(',') ? trimmed.split(',').map((part) => part.trim()) : [trimmed];
	}

	/**
	 * @param {KeyboardEvent} event
	 */
	function onXPathKeydown(event) {
		if (event.key === 'Enter') {
			event.preventDefault();
			void evaluateXPath();
		}
	}

	async function evaluateXPath() {
		if (!model?.ownerId) {
			return;
		}
		evaluating = true;
		error = '';
		try {
			const response = await call('studio.sourcepicker.Evaluate', {
				id: model.ownerId,
				sourcePriority: model.sourcePriority,
				xpath: fullXPathInput() || '.'
			});
			model = normalizeModel({ ...model, ...response });
			xpathSuffixInput = toXPathSuffix(model.displayXpath || fullXPathInput(), model.anchor);
			relativeXPath = model.xpath || '.';
		} catch (err) {
			error = String(err instanceof Error ? err.message : err);
		} finally {
			evaluating = false;
		}
	}

	/**
	 * @param {SourceNode} node
	 */
	function selectNode(node) {
		if (!node?.displayXpath || node.type === 'attributes') {
			return;
		}
		selectedTreeXPath = node.displayXpath;
		xpathSuffixInput = toXPathSuffix(node.displayXpath, anchor);
		void evaluateXPath();
	}

	/**
	 * @param {string | undefined} id
	 */
	function selectObject(id) {
		if (id) {
			onSelectObject(id);
		}
	}

	async function applySource() {
		if (!canApply || !pickerTarget?.propertyName || !model?.sourcePriority) {
			return;
		}
		applying = true;
		error = '';
		try {
			const payload = makeSourcePayload(
				model.displayXpath || fullXPathInput(),
				relativeXPath || '.'
			);
			const response = await applySourcePickerDrop(
				pickerTarget.id,
				payload,
				pickerTarget.propertyName
			);
			if (response?.done) {
				await onApply?.(
					response.id || pickerTarget.id,
					response.sourceDefinition ?? sourceDefinitionFromPayload(payload)
				);
			} else {
				error = response?.message || 'Unable to apply the selected source';
			}
		} catch (err) {
			error = String(err instanceof Error ? err.message : err);
		} finally {
			applying = false;
		}
	}

	/**
	 * @param {SourceNode | null | undefined} node
	 * @returns {number}
	 */
	function countTreeNodes(node) {
		if (!node) {
			return 0;
		}
		return 1 + (node.children ?? []).reduce((total, child) => total + countTreeNodes(child), 0);
	}

	/**
	 * @param {SourceNode} node
	 * @returns {{ className: string, label: string }}
	 */
	function nodeMarker(node) {
		if (node.type === 'attribute' || node.type === 'attributes') {
			return { className: 'studio-source-picker__marker--attribute', label: '' };
		}
		if (node.type === 'text') {
			return { className: 'studio-source-picker__marker--text', label: '' };
		}
		return { className: 'studio-source-picker__marker--element', label: '' };
	}

	/**
	 * @param {SourceNode} node
	 * @returns {string}
	 */
	function nodeClass(node) {
		return [
			'studio-source-picker__node',
			node.type && `studio-source-picker__node--${node.type}`,
			selectedTreeXPath &&
				selectedTreeXPath === node.displayXpath &&
				'studio-source-picker__node--selected'
		]
			.filter(Boolean)
			.join(' ');
	}

	/**
	 * @param {string} displayXpath
	 * @param {string} currentAnchor
	 * @returns {string}
	 */
	function toXPathSuffix(displayXpath, currentAnchor = anchor) {
		if (!currentAnchor || !displayXpath) {
			return displayXpath || '';
		}
		if (displayXpath === currentAnchor) {
			return '';
		}
		if (displayXpath.startsWith(currentAnchor)) {
			return displayXpath.slice(currentAnchor.length);
		}
		return displayXpath;
	}

	/**
	 * @returns {string}
	 */
	function fullXPathInput() {
		if (!anchor) {
			return xpathSuffixInput;
		}
		if (!xpathSuffixInput) {
			return anchor;
		}
		if (xpathSuffixInput.startsWith('.')) {
			return `${anchor}${xpathSuffixInput.slice(1)}`;
		}
		return xpathSuffixInput.startsWith('/') || xpathSuffixInput.startsWith('[')
			? `${anchor}${xpathSuffixInput}`
			: xpathSuffixInput;
	}

	/**
	 * @param {string=} displayXpath
	 * @param {string=} xpath
	 * @returns {import('./sourcePickerDnd').SourcePickerDragPayload}
	 */
	function makeSourcePayload(
		displayXpath = fullXPathInput(),
		xpath = relativeXPathFor(displayXpath)
	) {
		return {
			type: SOURCE_PICKER_DND_TYPE,
			data: {
				ownerId: model?.ownerId ?? '',
				sourceId: model?.sourceId ?? '',
				sourceName: model?.sourceName ?? '',
				sourcePriority: model?.sourcePriority ?? '',
				xpath: xpath || '.',
				displayXpath: displayXpath || ''
			}
		};
	}

	/**
	 * @param {string} displayXpath
	 * @returns {string}
	 */
	function relativeXPathFor(displayXpath) {
		if (!displayXpath) {
			return relativeXPath || '.';
		}
		if (!anchor) {
			return displayXpath;
		}
		if (displayXpath === anchor) {
			return '.';
		}
		if (displayXpath.startsWith(anchor)) {
			const suffix = displayXpath.slice(anchor.length);
			return suffix ? `.${suffix}` : '.';
		}
		return displayXpath;
	}

	/**
	 * @param {DragEvent} event
	 * @param {SourceNode=} node
	 */
	function handleSourceDragStart(event, node) {
		if (!model?.sourcePriority) {
			return;
		}
		const displayXpath = node?.displayXpath || fullXPathInput();
		const payload = makeSourcePayload(displayXpath, relativeXPathFor(displayXpath));
		setSourcePickerDragData(event, payload);
		$draggedData = payload;
	}

	function handleSourceDragEnd() {
		$draggedData = undefined;
	}

	/**
	 * @param {string} namespace
	 * @param {SourceNode} node
	 * @param {string} path
	 * @returns {string}
	 */
	function nodeKey(namespace, node, path) {
		return `${namespace}:${node.displayXpath || node.xpath || node.name || node.label || path}`;
	}

	/**
	 * @param {string} namespace
	 * @param {SourceNode} node
	 * @param {string} path
	 */
	function toggleNode(namespace, node, path) {
		const key = nodeKey(namespace, node, path);
		const next = new SvelteSet(collapsedNodes);
		if (next.has(key)) {
			next.delete(key);
		} else {
			next.add(key);
		}
		collapsedNodes = next;
	}

	/**
	 * @param {SourceNode} node
	 * @param {boolean} jsonMode
	 * @returns {boolean}
	 */
	function isDefaultCollapsed(node, jsonMode) {
		return !jsonMode && node.type === 'attributes';
	}

	/**
	 * @param {string} namespace
	 * @param {SourceNode} node
	 * @param {string} path
	 * @param {boolean} jsonMode
	 * @returns {boolean}
	 */
	function isCollapsed(namespace, node, path, jsonMode) {
		const toggled = collapsedNodes.has(nodeKey(namespace, node, path));
		return isDefaultCollapsed(node, jsonMode) ? !toggled : toggled;
	}

	/**
	 * @param {SourceNode} node
	 * @returns {{ className: string, label: string }}
	 */
	function jsonNodeMarker(node) {
		if (node.type === 'value') {
			return { className: 'studio-source-picker__marker--text', label: '' };
		}
		if (node.type === 'array') {
			return { className: 'studio-source-picker__marker--array', label: '' };
		}
		return { className: 'studio-source-picker__marker--element', label: '' };
	}
</script>

{#snippet treeNode(node, namespace, path, jsonMode)}
	{@const marker = jsonMode ? jsonNodeMarker(node) : nodeMarker(node)}
	{@const hasChildren = Boolean(node.children?.length)}
	{@const collapsed = isCollapsed(namespace, node, path, jsonMode)}
	{@const selectable = !jsonMode && Boolean(node.displayXpath) && node.type !== 'attributes'}
	<li>
		<div class={nodeClass(node)}>
			<button
				type="button"
				class="studio-source-picker__toggle"
				class:studio-source-picker__toggle--open={!collapsed}
				disabled={!hasChildren}
				aria-label={collapsed ? 'Expand' : 'Collapse'}
				onclick={() => toggleNode(namespace, node, path)}
			>
				<Ico icon="mdi:chevron-right" size={3} />
			</button>
			<button
				type="button"
				class="studio-source-picker__content"
				disabled={!selectable}
				draggable={selectable}
				title={node.displayXpath || node.label}
				onclick={() => selectNode(node)}
				ondragstart={(event) => handleSourceDragStart(event, node)}
				ondragend={handleSourceDragEnd}
			>
				<span class={`studio-source-picker__marker ${marker.className}`}>{marker.label}</span>
				<span class="studio-source-picker__node-label studio-ellipsis">{node.label}</span>
				{#if node.value}
					<span class="studio-source-picker__node-value studio-ellipsis">{node.value}</span>
				{/if}
			</button>
		</div>
		{#if hasChildren && !collapsed}
			<ul>
				{#each node.children as child, index (`${child.displayXpath || child.label}-${index}`)}
					{@render treeNode(child, namespace, `${path}/${index}`, jsonMode)}
				{/each}
			</ul>
		{/if}
	</li>
{/snippet}

<div class="studio-source-picker layout-y-stretch">
	<div class="studio-source-picker__toolbar layout-x-low studio-panel-toolbar">
		<StudioIconButton
			icon="mdi:link-variant"
			active={linked}
			aria-pressed={linked}
			title="Link with the projects tree selection"
			onclick={() => (linked = !linked)}
		/>
		<StudioIconButton
			icon="mdi:target"
			disabled={!model?.sourceId}
			title="Select displayed source"
			onclick={() => selectObject(model?.sourceId)}
		/>
		<StudioIconButton
			icon="mdi:code-tags"
			active={showJsonPreview}
			aria-pressed={showJsonPreview}
			title="Toggle JSON tree"
			onclick={() => (showJsonPreview = !showJsonPreview)}
		/>
		<button
			type="button"
			class="studio-source-picker__apply button-primary"
			disabled={!canApply || applying}
			onclick={applySource}
		>
			<Ico icon="mdi:check" size={4} />
			Apply
		</button>
	</div>

	<div class="studio-source-picker__body layout-y-stretch-low">
		{#if !activeSourceId}
			<StudioEmptyState message="No object selected" />
		{:else if loading}
			<StudioEmptyState message="Loading source" loading />
		{:else if error}
			<StudioEmptyState message={error} icon="mdi:alert-circle-outline" />
		{:else if !model?.available}
			<StudioEmptyState
				message={model?.message || 'No source DOM available'}
				icon="mdi:file-tree-outline"
			/>
		{:else}
			<section class="studio-source-picker__tree">
				<header class="layout-x-between-low">
					<span class="studio-label">{showJsonPreview ? 'DOM as JSON' : 'DOM'}</span>
					<span class="studio-pill">{domCount}</span>
				</header>
				<div class="studio-source-picker__tree-scroll">
					<ul>
						{#if domTree}
							{@render treeNode(
								domTree,
								showJsonPreview ? 'dom-json' : 'dom',
								'0',
								showJsonPreview
							)}
						{/if}
					</ul>
				</div>
			</section>

			<section class="studio-source-picker__xpath layout-y-stretch-low">
				<div class="studio-source-picker__xpath-row layout-x-none">
					<button
						type="button"
						class="studio-source-picker__xpath-drag"
						draggable={Boolean(model?.sourcePriority)}
						title="Drag XPath source"
						aria-label="Drag XPath source"
						ondragstart={(event) => handleSourceDragStart(event)}
						ondragend={handleSourceDragEnd}
					>
						xPath
					</button>
					<span class="studio-source-picker__xpath-anchor studio-ellipsis" title={anchor}>
						{anchor}
					</span>
					<input
						class="input"
						value={xpathSuffixInput}
						aria-label="XPath suffix"
						oninput={(event) => (xpathSuffixInput = event.currentTarget.value)}
						onkeydown={onXPathKeydown}
					/>
					<StudioIconButton
						icon="mdi:play"
						size="xs"
						title="Evaluate XPath"
						disabled={evaluating}
						onclick={evaluateXPath}
					/>
				</div>
			</section>

			<section class="studio-source-picker__result">
				<header class="layout-x-between-low">
					<span class="studio-label">{showJsonPreview ? 'Result as JSON' : 'Result'}</span>
					<span class="studio-pill">{resultCount}</span>
				</header>
				<div class="studio-source-picker__tree-scroll studio-source-picker__tree-scroll--result">
					{#if resultTree}
						<ul>
							{@render treeNode(
								resultTree,
								showJsonPreview ? 'result-json' : 'result',
								'0',
								showJsonPreview
							)}
						</ul>
					{:else}
						<StudioEmptyState message="No result" small />
					{/if}
				</div>
			</section>
		{/if}
	</div>
</div>

<style>
	.studio-source-picker {
		height: 100%;
		min-height: 0;
	}

	.studio-source-picker__toolbar {
		min-width: 0;
		align-items: center;
	}

	.studio-source-picker__apply {
		margin-inline-start: auto;
		min-height: 1.75rem;
		padding-block: 0 !important;
	}

	.studio-source-picker__body {
		min-height: 0;
		flex: 1;
		overflow: auto;
		padding: 0.35rem;
	}

	.studio-source-picker__tree,
	.studio-source-picker__xpath,
	.studio-source-picker__result {
		min-width: 0;
		border-block-end: 1px solid var(--color-surface-200-800);
		padding: 0.38rem;
	}

	.studio-source-picker__tree {
		min-height: 0;
	}

	.studio-source-picker__tree-scroll {
		min-height: 13rem;
		max-height: 26rem;
		overflow: auto;
		padding-block-start: 0.18rem;
	}

	.studio-source-picker__tree-scroll--result {
		min-height: 8rem;
		max-height: 11rem;
	}

	.studio-source-picker__tree-scroll ul {
		margin: 0;
		padding: 0;
		list-style: none;
	}

	.studio-source-picker__tree-scroll ul ul {
		margin-inline-start: 0.68rem;
		padding-inline-start: 0;
	}

	.studio-source-picker__node {
		display: grid;
		width: max-content;
		min-width: 100%;
		grid-template-columns: 0.72rem max-content;
		align-items: center;
		gap: 0.08rem;
		border: 1px solid transparent;
		border-radius: 0.35rem;
		background: transparent;
		color: var(--color-surface-900-100);
		padding: 0.05rem 0.24rem 0.05rem 0;
		text-align: start;
		font-size: 0.74rem;
		font-weight: 650;
	}

	.studio-source-picker__node:hover,
	.studio-source-picker__node--selected {
		border-color: color-mix(in oklab, var(--color-primary-500) 42%, transparent);
		background: color-mix(in oklab, var(--color-primary-500) 12%, transparent);
		color: var(--color-primary-600-400);
	}

	.studio-source-picker__toggle {
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

	.studio-source-picker__toggle:hover:not(:disabled) {
		background: color-mix(in oklab, var(--color-surface-300-700) 48%, transparent);
	}

	.studio-source-picker__toggle--open {
		transform: rotate(90deg);
	}

	.studio-source-picker__toggle:disabled {
		opacity: 0;
	}

	.studio-source-picker__content {
		display: grid;
		min-width: max-content;
		grid-template-columns: 0.72rem max-content auto;
		align-items: center;
		gap: 0.12rem;
		border: 0;
		background: transparent;
		color: inherit;
		padding: 0;
		text-align: left;
	}

	.studio-source-picker__content:disabled {
		cursor: default;
		color: inherit;
	}

	.studio-source-picker__marker {
		display: inline-flex;
		width: 0.72rem;
		height: 0.9rem;
		align-items: center;
		justify-content: center;
		line-height: 1;
	}

	.studio-source-picker__marker--element::before {
		width: 0.45rem;
		height: 0.45rem;
		border-radius: 999px;
		background: var(--color-success-500);
		content: '';
	}

	.studio-source-picker__marker--attribute::before {
		width: 0.42rem;
		height: 0.42rem;
		border-radius: 0.12rem;
		background: var(--color-error-500);
		content: '';
	}

	.studio-source-picker__marker--array::before {
		width: 0.48rem;
		height: 0.48rem;
		border: 1px solid var(--color-primary-500);
		border-radius: 0.12rem;
		background: color-mix(in oklab, var(--color-primary-500) 12%, transparent);
		content: '';
	}

	.studio-source-picker__marker--text {
		color: var(--color-primary-600-400);
		font-family: var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
	}

	.studio-source-picker__node--attribute {
		color: var(--color-warning-700-300);
	}

	.studio-source-picker__node--text {
		color: var(--color-primary-700-300);
	}

	.studio-source-picker__node-value {
		color: var(--color-surface-500-500);
		font-family: var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
		font-size: 0.66rem;
		font-weight: 500;
	}

	.studio-source-picker__xpath-row {
		overflow: hidden;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.35rem;
		background: var(--color-surface-50-950);
	}

	.studio-source-picker__xpath-anchor {
		max-width: 52%;
		align-self: stretch;
		border-inline-end: 1px solid color-mix(in oklab, var(--color-warning-500) 42%, transparent);
		background: color-mix(in oklab, var(--color-warning-400) 18%, transparent);
		color: var(--color-surface-950-50);
		padding: 0.36rem 0.42rem;
		font-family: var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
		font-size: 0.72rem;
	}

	.studio-source-picker__xpath-drag {
		display: inline-flex;
		align-self: stretch;
		align-items: center;
		border: 0;
		border-inline-end: 1px solid var(--color-surface-200-800);
		background: transparent;
		color: var(--color-primary-600-400);
		cursor: grab;
		padding-inline: 0.38rem;
		font-size: 0.68rem;
		font-weight: 750;
		text-transform: uppercase;
	}

	.studio-source-picker__xpath-drag:active {
		cursor: grabbing;
	}

	.studio-source-picker__xpath-row :global(.input) {
		min-width: 0;
		flex: 1;
		border: 0;
		border-radius: 0;
		background: transparent;
		font-family: var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
		font-size: 0.72rem;
	}
</style>
