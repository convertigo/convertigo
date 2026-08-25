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
	import StudioFlowPickerFrame from './StudioFlowPickerFrame.svelte';
	import StudioIconButton from './StudioIconButton.svelte';

	/**
	 * @typedef {{
	 *  id: string,
	 *  propertyName?: string,
	 *  displayName?: string,
	 *  kind?: string,
	 *  editorClass?: string,
	 *  mode?: string,
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
	 * @typedef {{
	 *  type?: string,
	 *  label?: string,
	 *  name?: string,
	 *  value?: string,
	 *  path?: string,
	 *  qname?: string,
	 *  source?: string,
	 *  sourceData?: any,
	 *  selected?: boolean,
	 *  children?: NgxNode[]
	 * }} NgxNode
	 */
	/**
	 * @typedef {{
	 *  ownerId?: string,
	 *  propertyName?: string,
	 *  projectName?: string,
	 *  filter?: string,
	 *  path?: string,
	 *  prefix?: string,
	 *  suffix?: string,
	 *  custom?: string,
	 *  input?: string,
	 *  computedValue?: string,
	 *  useCustom?: boolean,
	 *  available?: boolean,
	 *  message?: string,
	 *  filters?: { value: string, label: string, supported?: boolean }[],
	 *  sources?: NgxNode | null,
	 *  modelTree?: NgxNode | null,
	 *  sourceData?: any,
	 *  sourceValue?: any
	 * }} NgxSourceModel
	 */

	/**
	 * @type {{
	 *  selectedId?: string,
	 *  active?: boolean,
	 *  pickerTarget?: PickerTarget | null,
	 *  embedded?: boolean,
	 *  onSelectObject?: (id: string) => void,
	 *  onApply?: (id: string, sourceDefinition?: any) => void | Promise<void>
	 * }}
	 */
	let {
		selectedId = '',
		active = true,
		pickerTarget = null,
		embedded = false,
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
	let ngxLoading = $state(false);
	let ngxRefreshing = $state(false);
	let ngxApplying = $state(false);
	let ngxError = $state('');
	let ngxLoadSerial = 0;
	let ngxModel = $state(/** @type {NgxSourceModel | null} */ (null));
	let ngxFilter = $state('Sequence');
	let ngxSourceData = $state(/** @type {any} */ (null));
	let ngxPath = $state('');
	let ngxPrefix = $state('');
	let ngxSuffix = $state('');
	let ngxCustom = $state('');
	let ngxUseCustom = $state(false);
	let ngxCollapsedNodes = $state.raw(new SvelteSet());

	let targetSource = $derived(parseSourceDefinition(pickerTarget?.value));
	let flowPicker = $derived(isFlowPickerTarget(pickerTarget));
	let ngxPicker = $derived(isNgxPickerTarget(pickerTarget));
	let ngxDirectFilter = $derived(ngxFilter === 'Icon' || ngxFilter === 'Asset');
	let canApply = $derived(
		Boolean(!ngxPicker && pickerTarget?.id && pickerTarget?.propertyName && model?.available)
	);
	let canApplyNgx = $derived(
		Boolean(
			ngxPicker &&
			ngxModel?.available &&
			pickerTarget?.id &&
			pickerTarget?.propertyName &&
			hasNgxSourceData(ngxSourceData)
		)
	);
	let anchor = $derived(model?.anchor ?? '');
	let activeSourceId = $derived(sourceRequest().id);
	let domTree = $derived(showJsonPreview ? model?.jsonTree : model?.tree);
	let resultTree = $derived(showJsonPreview ? model?.jsonResult : model?.result);
	let domCount = $derived(countTreeNodes(domTree));
	let resultCount = $derived(countTreeNodes(resultTree));
	let ngxSourceCount = $derived(countNgxNodes(ngxModel?.sources));
	let ngxModelCount = $derived(countNgxNodes(ngxModel?.modelTree));

	$effect(() => {
		if (!active || flowPicker || ngxPicker || !linked) {
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

	$effect(() => {
		const target = pickerTarget;
		if (!active || !isNgxPickerTarget(target) || !target?.id || !target?.propertyName) {
			return;
		}
		const serial = ++ngxLoadSerial;
		void loadNgxSource(
			{
				id: target.id,
				propertyName: target.propertyName
			},
			serial
		);
	});

	/**
	 * @param {PickerTarget | null} target
	 * @returns {boolean}
	 */
	function isNgxPickerTarget(target) {
		return Boolean(
			target?.propertyName &&
			(target?.editorClass === 'NgxSmartSourcePropertyDescriptor' ||
				(target?.kind === 'ion' && target?.mode === 'source'))
		);
	}

	/**
	 * @param {PickerTarget | null} target
	 * @returns {boolean}
	 */
	function isFlowPickerTarget(target) {
		return Boolean(target?.propertyName && String(target?.editorClass ?? '').startsWith('flow-'));
	}

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
	 * @param {Record<string, any>} request
	 * @param {number} serial
	 * @param {{ preserveSources?: boolean }=} options
	 */
	async function loadNgxSource(request, serial, options = {}) {
		const preserveSources = options.preserveSources === true;
		if (preserveSources) {
			ngxRefreshing = true;
		} else {
			ngxLoading = true;
		}
		ngxError = '';
		try {
			const response = await call('studio.ngxpicker.Get', request);
			if (serial !== ngxLoadSerial) {
				return;
			}
			applyNgxModel(response, options);
		} catch (err) {
			if (serial === ngxLoadSerial) {
				ngxError = String(err instanceof Error ? err.message : err);
			}
		} finally {
			if (serial === ngxLoadSerial) {
				if (preserveSources) {
					ngxRefreshing = false;
				} else {
					ngxLoading = false;
				}
			}
		}
	}

	/**
	 * @param {NgxSourceModel} response
	 * @param {{ preserveSources?: boolean }=} options
	 */
	function applyNgxModel(response, options = {}) {
		const preserveSources = options.preserveSources === true;
		const nextModel = {
			...response,
			available: Boolean(response?.available),
			filters: response?.filters ?? [],
			sources: preserveSources
				? (ngxModel?.sources ?? response?.sources ?? null)
				: (response?.sources ?? null),
			modelTree: response?.modelTree ?? null
		};
		ngxModel = nextModel;
		ngxFilter = nextModel.filter || 'Sequence';
		ngxSourceData = nextModel.sourceData ?? null;
		ngxPath = nextModel.path ?? '';
		ngxPrefix = nextModel.prefix ?? '';
		ngxSuffix = nextModel.suffix ?? '';
		ngxCustom = nextModel.custom ?? '';
		ngxUseCustom = Boolean(nextModel.useCustom);
		if (!preserveSources) {
			ngxCollapsedNodes = new SvelteSet();
		}
	}

	/**
	 * @param {Partial<{
	 *  filter: string,
	 *  sourceData: any,
	 *  path: string,
	 *  prefix: string,
	 *  suffix: string,
	 *  custom: string,
	 *  useCustom: boolean
	 * }>} overrides
	 * @param {{ preserveSources?: boolean }=} options
	 */
	function reloadNgxPreview(overrides = {}, options = { preserveSources: true }) {
		if (!pickerTarget?.id || !pickerTarget?.propertyName) {
			return;
		}
		const nextSourceData = 'sourceData' in overrides ? overrides.sourceData : ngxSourceData;
		const sourceDataRequest =
			'sourceData' in overrides
				? { sourceData: hasNgxSourceData(nextSourceData) ? JSON.stringify(nextSourceData) : '{}' }
				: hasNgxSourceData(nextSourceData)
					? { sourceData: JSON.stringify(nextSourceData) }
					: {};
		const serial = ++ngxLoadSerial;
		void loadNgxSource(
			{
				id: pickerTarget.id,
				propertyName: pickerTarget.propertyName,
				filter: overrides.filter ?? ngxFilter,
				...sourceDataRequest,
				path: overrides.path ?? ngxPath,
				prefix: overrides.prefix ?? ngxPrefix,
				suffix: overrides.suffix ?? ngxSuffix,
				custom: overrides.custom ?? ngxCustom,
				useCustom: String(overrides.useCustom ?? ngxUseCustom)
			},
			serial,
			options
		);
	}

	/**
	 * @param {NgxNode} node
	 */
	function selectNgxSource(node) {
		if (!node?.sourceData) {
			return;
		}
		ngxSourceData = node.sourceData;
		ngxPath = '';
		if (ngxModel?.sources) {
			ngxModel = {
				...ngxModel,
				sources: markNgxSourceSelection(ngxModel.sources, node.sourceData)
			};
		}
		reloadNgxPreview({ sourceData: node.sourceData, path: '' });
	}

	/**
	 * @param {NgxNode} node
	 */
	function selectNgxPath(node) {
		if (!node?.path) {
			return;
		}
		ngxPath = node.path;
		if (ngxModel?.modelTree) {
			ngxModel = {
				...ngxModel,
				modelTree: markNgxPathSelection(ngxModel.modelTree, node.path)
			};
		}
		reloadNgxPreview({ path: node.path });
	}

	/**
	 * @param {NgxNode} node
	 * @param {any} sourceData
	 * @returns {NgxNode}
	 */
	function markNgxSourceSelection(node, sourceData) {
		return {
			...node,
			selected: sameNgxSourceData(node.sourceData, sourceData),
			children: (node.children ?? []).map((child) => markNgxSourceSelection(child, sourceData))
		};
	}

	/**
	 * @param {NgxNode} node
	 * @param {string} path
	 * @returns {NgxNode}
	 */
	function markNgxPathSelection(node, path) {
		return {
			...node,
			selected: Boolean(node.path && node.path === path),
			children: (node.children ?? []).map((child) => markNgxPathSelection(child, path))
		};
	}

	/**
	 * @param {any} left
	 * @param {any} right
	 * @returns {boolean}
	 */
	function sameNgxSourceData(left, right) {
		if (!left || !right) {
			return false;
		}
		try {
			return JSON.stringify(left) === JSON.stringify(right);
		} catch {
			return false;
		}
	}

	async function applyNgxSource() {
		if (!canApplyNgx || !pickerTarget?.propertyName) {
			return;
		}
		ngxApplying = true;
		ngxError = '';
		try {
			const response = await call('studio.ngxpicker.Apply', {
				id: pickerTarget.id,
				propertyName: pickerTarget.propertyName,
				filter: ngxFilter,
				sourceData: JSON.stringify(ngxSourceData),
				path: ngxPath,
				prefix: ngxPrefix,
				suffix: ngxSuffix,
				custom: ngxCustom,
				useCustom: String(ngxUseCustom)
			});
			if (response?.done) {
				ngxModel = {
					...(ngxModel ?? {}),
					sourceValue: response.sourceValue,
					computedValue: response.computedValue
				};
				await onApply?.(response.id || pickerTarget.id, response.sourceValue);
			} else {
				ngxError = response?.message || 'Unable to apply the selected NGX source';
			}
		} catch (err) {
			ngxError = String(err instanceof Error ? err.message : err);
		} finally {
			ngxApplying = false;
		}
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
	 * @param {NgxNode | null | undefined} node
	 * @returns {number}
	 */
	function countNgxNodes(node) {
		if (!node) {
			return 0;
		}
		return 1 + (node.children ?? []).reduce((total, child) => total + countNgxNodes(child), 0);
	}

	/**
	 * @param {any} sourceData
	 * @returns {boolean}
	 */
	function hasNgxSourceData(sourceData) {
		return Boolean(
			sourceData &&
			typeof sourceData === 'object' &&
			!Array.isArray(sourceData) &&
			Object.keys(sourceData).length > 0
		);
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
	 * @param {NgxNode} node
	 * @returns {{ className: string, label: string }}
	 */
	function ngxNodeMarker(node) {
		if (node.type === 'project') {
			return { className: 'studio-source-picker__marker--project', label: '' };
		}
		if (node.type === 'source') {
			return { className: 'studio-source-picker__marker--source', label: '' };
		}
		if (node.type === 'array') {
			return { className: 'studio-source-picker__marker--array', label: '' };
		}
		if (node.type === 'value') {
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
	 * @param {NgxNode} node
	 * @returns {string}
	 */
	function ngxNodeClass(node) {
		return [
			'studio-source-picker__node',
			node.type && `studio-source-picker__node--${node.type}`,
			node.selected && 'studio-source-picker__node--selected',
			node.path && ngxPath === node.path && 'studio-source-picker__node--selected'
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
	 * @param {string} namespace
	 * @param {NgxNode} node
	 * @param {string} path
	 * @returns {string}
	 */
	function ngxNodeKey(namespace, node, path) {
		return `${namespace}:${node.path || node.qname || node.source || node.name || node.label || path}`;
	}

	/**
	 * @param {string} namespace
	 * @param {NgxNode} node
	 * @param {string} path
	 */
	function toggleNgxNode(namespace, node, path) {
		const key = ngxNodeKey(namespace, node, path);
		const next = new SvelteSet(ngxCollapsedNodes);
		if (next.has(key)) {
			next.delete(key);
		} else {
			next.add(key);
		}
		ngxCollapsedNodes = next;
	}

	/**
	 * @param {string} namespace
	 * @param {NgxNode} node
	 * @param {string} path
	 * @returns {boolean}
	 */
	function isNgxCollapsed(namespace, node, path) {
		return ngxCollapsedNodes.has(ngxNodeKey(namespace, node, path));
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

{#snippet ngxNode(node, namespace, path, mode)}
	{@const marker = ngxNodeMarker(node)}
	{@const hasChildren = Boolean(node.children?.length)}
	{@const collapsed = isNgxCollapsed(namespace, node, path)}
	{@const selectable = mode === 'source' ? Boolean(node.sourceData) : Boolean(node.path)}
	<li>
		<div class={ngxNodeClass(node)}>
			<button
				type="button"
				class="studio-source-picker__toggle"
				class:studio-source-picker__toggle--open={!collapsed}
				disabled={!hasChildren}
				aria-label={collapsed ? 'Expand' : 'Collapse'}
				onclick={() => toggleNgxNode(namespace, node, path)}
			>
				<Ico icon="mdi:chevron-right" size={3} />
			</button>
			<button
				type="button"
				class="studio-source-picker__content"
				disabled={!selectable}
				title={node.path || node.source || node.qname || node.label}
				onclick={() => (mode === 'source' ? selectNgxSource(node) : selectNgxPath(node))}
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
				{#each node.children as child, index (`${child.path || child.source || child.label}-${index}`)}
					{@render ngxNode(child, namespace, `${path}/${index}`, mode)}
				{/each}
			</ul>
		{/if}
	</li>
{/snippet}

<div class="studio-source-picker layout-y-stretch" class:studio-source-picker--embedded={embedded}>
	{#if !embedded || !flowPicker}
		<div
			class="studio-source-picker__toolbar layout-x-low studio-panel-toolbar"
			class:studio-source-picker__toolbar--ngx={ngxPicker}
		>
			{#if flowPicker}
				<span class="studio-source-picker__flow-title studio-ellipsis">
					{pickerTarget?.displayName || pickerTarget?.propertyName || 'Flow picker'}
				</span>
			{:else if ngxPicker}
				<select
					class="studio-source-picker__filter input"
					value={ngxFilter}
					disabled={ngxLoading || ngxApplying || ngxRefreshing}
					aria-label="NGX source filter"
					onchange={(event) => {
						ngxFilter = event.currentTarget.value;
						ngxSourceData = null;
						ngxPath = '';
						reloadNgxPreview(
							{ filter: ngxFilter, sourceData: null, path: '' },
							{ preserveSources: false }
						);
					}}
				>
					{#each ngxModel?.filters ?? [{ value: 'Sequence', label: 'Sequence', supported: true }] as filter (filter.value)}
						<option value={filter.value} disabled={filter.supported === false}>
							{filter.label}{filter.supported === false ? ' (soon)' : ''}
						</option>
					{/each}
				</select>
				<button
					type="button"
					class="studio-source-picker__apply button-primary"
					disabled={!canApplyNgx || ngxApplying || ngxRefreshing}
					onclick={applyNgxSource}
				>
					<Ico icon="mdi:check" size={4} />
					Apply
				</button>
			{:else}
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
			{/if}
		</div>
	{/if}

	<div
		class="studio-source-picker__body layout-y-stretch-low"
		class:studio-source-picker__body--ngx={ngxPicker}
	>
		{#if flowPicker}
			<StudioFlowPickerFrame {active} {pickerTarget} {onApply} />
		{:else if ngxPicker}
			{#if !pickerTarget?.id}
				<StudioEmptyState message="No property selected" />
			{:else if ngxLoading}
				<StudioEmptyState message="Loading NGX sources" loading />
			{:else if ngxError && !ngxModel}
				<StudioEmptyState message={ngxError} icon="mdi:alert-circle-outline" />
			{:else if !ngxModel?.available}
				<StudioEmptyState message={ngxModel?.message || 'No NGX source available'} icon="mdi:hub" />
			{:else}
				<section class="studio-source-picker__tree">
					<header class="layout-x-between-low">
						<span class="studio-label">Sources</span>
						<span class="studio-pill">{ngxRefreshing ? '...' : ngxSourceCount}</span>
					</header>
					<div
						class="studio-source-picker__tree-scroll studio-source-picker__tree-scroll--sources"
						class:studio-source-picker__tree-scroll--busy={ngxRefreshing}
						aria-busy={ngxRefreshing}
					>
						{#if ngxModel?.sources?.children?.length}
							<ul>
								{#each ngxModel.sources.children as child, index (`${child.qname || child.label}-${index}`)}
									{@render ngxNode(child, 'ngx-sources', `${index}`, 'source')}
								{/each}
							</ul>
						{:else}
							<StudioEmptyState message="No source" small />
						{/if}
					</div>
				</section>

				{#if !ngxDirectFilter}
					<section class="studio-source-picker__tree">
						<header class="layout-x-between-low">
							<span class="studio-label">Model</span>
							<span class="studio-pill">{ngxRefreshing ? '...' : ngxModelCount}</span>
						</header>
						<div
							class="studio-source-picker__tree-scroll studio-source-picker__tree-scroll--model"
							class:studio-source-picker__tree-scroll--busy={ngxRefreshing}
							aria-busy={ngxRefreshing}
						>
							{#if ngxModel?.modelTree?.children?.length}
								<ul>
									{#each ngxModel.modelTree.children as child, index (`${child.path || child.label}-${index}`)}
										{@render ngxNode(child, 'ngx-model', `${index}`, 'path')}
									{/each}
								</ul>
							{:else}
								<StudioEmptyState message="Select a source" small />
							{/if}
						</div>
					</section>
				{/if}

				<section class="studio-source-picker__ngx-expression layout-y-stretch-low">
					<div class="studio-source-picker__ngx-grid">
						<label>
							<span class="studio-label">Prefix</span>
							<input
								class="input"
								value={ngxPrefix}
								oninput={(event) => (ngxPrefix = event.currentTarget.value)}
								onchange={() => reloadNgxPreview({ prefix: ngxPrefix })}
							/>
						</label>
						{#if !ngxDirectFilter}
							<label>
								<span class="studio-label">Path</span>
								<input
									class="input"
									value={ngxPath}
									oninput={(event) => (ngxPath = event.currentTarget.value)}
									onchange={() => reloadNgxPreview({ path: ngxPath })}
								/>
							</label>
						{/if}
						<label>
							<span class="studio-label">Suffix</span>
							<input
								class="input"
								value={ngxSuffix}
								oninput={(event) => (ngxSuffix = event.currentTarget.value)}
								onchange={() => reloadNgxPreview({ suffix: ngxSuffix })}
							/>
						</label>
					</div>
					<label class="studio-source-picker__custom layout-x-low">
						<input
							type="checkbox"
							checked={ngxUseCustom}
							onchange={(event) => {
								ngxUseCustom = event.currentTarget.checked;
								reloadNgxPreview({ useCustom: ngxUseCustom });
							}}
						/>
						<span class="studio-label">Custom</span>
					</label>
					{#if ngxUseCustom}
						<textarea
							class="input"
							rows="2"
							value={ngxCustom}
							oninput={(event) => (ngxCustom = event.currentTarget.value)}
							onchange={() => reloadNgxPreview({ custom: ngxCustom })}></textarea>
					{/if}
					<code class="studio-source-picker__preview">{ngxModel?.computedValue || 'empty'}</code>
				</section>
			{/if}
		{:else if !activeSourceId}
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

	.studio-source-picker--embedded {
		height: auto;
		min-height: 18rem;
		max-height: min(32rem, 62vh);
	}

	.studio-source-picker--embedded .studio-source-picker__body {
		min-height: 18rem;
	}

	.studio-source-picker__toolbar {
		min-width: 0;
		align-items: center;
	}

	.studio-source-picker__toolbar--ngx {
		padding-block: 0.34rem;
	}

	.studio-source-picker__flow-title {
		padding: 0.2rem 0.35rem;
		color: var(--color-surface-200-800);
		font-size: 0.72rem;
		font-weight: 700;
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

	.studio-source-picker__body--ngx {
		padding: 0;
	}

	.studio-source-picker__tree,
	.studio-source-picker__xpath,
	.studio-source-picker__result,
	.studio-source-picker__ngx-expression {
		flex: 0 0 auto;
		min-width: 0;
		border-block-end: 1px solid var(--color-surface-200-800);
		padding: 0.38rem;
	}

	.studio-source-picker__tree {
		min-height: 0;
	}

	.studio-source-picker__body--ngx .studio-source-picker__tree {
		padding-block: 0.3rem 0.36rem;
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

	.studio-source-picker__tree-scroll--sources {
		min-height: 11rem;
		max-height: 16rem;
	}

	.studio-source-picker__tree-scroll--model {
		min-height: 10rem;
		max-height: 16rem;
	}

	.studio-source-picker__tree-scroll--busy {
		opacity: 0.72;
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

	.studio-source-picker__marker--project::before {
		width: 0.58rem;
		height: 0.45rem;
		border: 1px solid var(--color-primary-500);
		border-radius: 0.12rem;
		background: color-mix(in oklab, var(--color-primary-500) 18%, transparent);
		content: '';
	}

	.studio-source-picker__marker--source::before {
		width: 0.48rem;
		height: 0.48rem;
		border-radius: 999px;
		background: var(--color-primary-500);
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

	.studio-source-picker__filter {
		min-width: 8rem;
		max-width: 11rem;
		min-height: 1.75rem;
		padding-block: 0;
	}

	.studio-source-picker__ngx-grid {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(7.5rem, 1fr));
		gap: 0.35rem;
	}

	.studio-source-picker__ngx-grid label {
		min-width: 0;
	}

	.studio-source-picker__ngx-grid :global(.input) {
		width: 100%;
		font-family: var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
		font-size: 0.72rem;
	}

	.studio-source-picker__custom {
		align-items: center;
	}

	.studio-source-picker__custom input {
		margin: 0;
	}

	.studio-source-picker__ngx-expression > textarea {
		min-height: 3.5rem;
		font-family: var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
		font-size: 0.72rem;
	}

	.studio-source-picker__preview {
		display: block;
		min-height: 1.75rem;
		overflow: auto;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.35rem;
		background: var(--color-surface-50-950);
		color: var(--color-surface-900-100);
		padding: 0.35rem 0.45rem;
		font-family: var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
		font-size: 0.72rem;
		white-space: pre-wrap;
	}
</style>
