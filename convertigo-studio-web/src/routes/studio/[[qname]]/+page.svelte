<script>
	import { browser } from '$app/environment';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import Button from '$lib/admin/components/Button.svelte';
	import LightSwitch from '$lib/common/components/LightSwitch.svelte';
	import Projects from '$lib/common/Projects.svelte.js';
	import TestPlatform from '$lib/common/TestPlatform.svelte';
	import FlowViewer from '$lib/studio/flow/FlowViewer.svelte';
	import { findPrimaryEditorProperty, isCodeEditorProperty } from '$lib/studio/propertyEditors';
	import { decodeStudioSelectionId, studioSelectionUrl } from '$lib/studio/routeSelection';
	import StudioDevicePanel from '$lib/studio/StudioDevicePanel.svelte';
	import StudioEditorPanel from '$lib/studio/StudioEditorPanel.svelte';
	import StudioExecutionPanel from '$lib/studio/StudioExecutionPanel.svelte';
	import StudioLogsPanel from '$lib/studio/StudioLogsPanel.svelte';
	import StudioPalettePanel from '$lib/studio/StudioPalettePanel.svelte';
	import StudioPanel from '$lib/studio/StudioPanel.svelte';
	import StudioPreviewPanel from '$lib/studio/StudioPreviewPanel.svelte';
	import StudioPropertiesPanel from '$lib/studio/StudioPropertiesPanel.svelte';
	import StudioSourcePickerPanel from '$lib/studio/StudioSourcePickerPanel.svelte';
	import StudioTreePanel from '$lib/studio/StudioTreePanel.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { resolve } from '$lib/utils/route';
	import { call, checkArray, saveDboProject } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import { SvelteSet } from 'svelte/reactivity';

	const STUDIO_BASE = resolve('/studio/');
	const STUDIO_LAYOUT_STORAGE_KEY = 'convertigo.studio.layout.v1';
	const initialSelectedId = routeSelectionId();
	const MIN_TREE_WIDTH = 208;
	const MAX_TREE_WIDTH = 520;
	const MIN_TOOLS_WIDTH = 260;
	const MAX_TOOLS_WIDTH = 560;
	const MIN_LOGS_HEIGHT = 128;
	const MAX_LOGS_HEIGHT = 520;
	const DEFAULT_LAYOUT_SIZES = {
		treeWidth: 304,
		toolsWidth: 360,
		logsHeight: 260
	};
	const DEFAULT_COLLAPSED_PANELS = {
		tree: false,
		tools: false
	};
	const SIDE_PANEL_IDS = ['devices', 'palette', 'picker', 'properties'];
	const WORK_PANEL_IDS = ['execution', 'code', 'flow'];

	/**
	 * @typedef {Object} EditorTarget
	 * @property {string} id
	 * @property {string=} propertyName
	 * @property {string=} displayName
	 * @property {any=} value
	 * @property {number=} serial
	 */

	const profiles = [
		{
			id: 'backend',
			label: 'Backend',
			icon: 'mdi:source-branch',
			description: 'Tree, palette, flow, properties'
		},
		{
			id: 'frontend',
			label: 'Frontend',
			icon: 'mdi:smartphone-link',
			description: 'Tree, preview, properties'
		}
	];
	const PROFILE_IDS = profiles.map(({ id }) => id);

	let profile = $state('backend');
	let selectedId = $state(initialSelectedId);
	let activeSidePanel = $state('properties');
	let activeWorkPanel = $state(workPanelFromHash(page.url.hash) || 'execution');
	let frontendDeviceId = $state('none');
	let frontendLandscape = $state(false);
	let logsPanelOpen = $state(false);
	/** @type {EditorTarget | null} */
	let editorTarget = $state(null);
	let selectionMetaSerial = 0;
	let urlSyncReady = $state(false);
	let lastRouteSelectionId = initialSelectedId;
	let lastRouteWorkPanelHash = page.url.hash;
	let pendingRouteSelectionId = '';
	let treeRefreshSerial = $state(0);
	let flowRefreshSerial = $state(0);
	let renameTargetId = $state('');
	let mutationRefreshSerial = 0;
	let studioMutationSerial = $state(0);
	/** @type {import('$lib/studio/dnd').DboDropResult | null} */
	let lastStudioMutation = $state(null);
	let projectActionBusy = $state('');
	let dirtyProjectNames = $state.raw(new SvelteSet());
	let executionFallbackKey = '';
	/** @type {{ kind: 'transaction', connectorName?: string, requestable: any } | null} */
	let executionFallbackTarget = $state(null);
	let collapsedPanels = $state({ ...DEFAULT_COLLAPSED_PANELS });
	let layoutSizes = $state({ ...DEFAULT_LAYOUT_SIZES });

	let selectedContext = $derived(parseSelection(selectedId));
	let selectedProjectName = $derived(selectedContext.projectName);
	let project = $derived.by(() => (selectedProjectName ? TestPlatform(selectedProjectName) : null));
	let sequences = $derived.by(() => {
		const list = project?.sequence?.filter((sequence) => sequence.name) ?? [];
		const selectedSequence = selectedContext.sequenceName;
		if (selectedSequence && !list.some((sequence) => sequence.name === selectedSequence)) {
			return [...list, { name: selectedSequence, variable: [], testcase: [] }];
		}
		return list;
	});
	let primaryExecutionTarget = $derived(resolveExecutionTarget(project, selectedContext));
	let executionTarget = $derived(primaryExecutionTarget ?? executionFallbackTarget);
	let selectedSequenceName = $derived(selectedContext.sequenceName || sequences[0]?.name || '');
	let selectedProjectDirty = $derived(
		Boolean(selectedProjectName && dirtyProjectNames.has(selectedProjectName))
	);
	let selectedFlowSequenceName = $derived(selectedContext.sequenceName || '');
	let flowReady = $derived(
		Boolean(selectedProjectName && selectedFlowSequenceName && sequences.length)
	);
	let showStudioWork = $derived(profile === 'backend');
	let showDevicePicker = $derived(profile === 'frontend');
	let showFlowOverview = $derived(showStudioWork && flowReady);
	let showPalette = $derived(showStudioWork);
	let showSourcePicker = $derived(showStudioWork);
	let sideViews = $derived([
		...(showDevicePicker ? [{ id: 'devices', label: 'Devices', icon: 'mdi:devices' }] : []),
		...(showPalette ? [{ id: 'palette', label: 'Palette', icon: 'mdi:palette-outline' }] : []),
		...(showSourcePicker ? [{ id: 'picker', label: 'Picker', icon: 'mdi:hub' }] : []),
		{ id: 'properties', label: 'Properties', icon: 'mdi:tune-vertical-variant' }
	]);
	let effectiveSidePanel = $derived(
		sideViews.some((item) => item.id === activeSidePanel) ? activeSidePanel : 'properties'
	);
	let activeSideView = $derived(
		sideViews.find((item) => item.id === effectiveSidePanel) ?? sideViews.at(-1)
	);
	let breadcrumbs = $derived(buildBreadcrumb(selectedId));
	let workspaceStyle = $derived(
		[
			`--studio-tree-track:${collapsedPanels.tree ? '0px' : `${layoutSizes.treeWidth}px`}`,
			`--studio-tools-track:${collapsedPanels.tools ? '0px' : `${layoutSizes.toolsWidth}px`}`,
			`--studio-tree-resizer-track:${collapsedPanels.tree ? '0px' : '0.45rem'}`,
			`--studio-tools-resizer-track:${collapsedPanels.tools ? '0px' : '0.45rem'}`,
			`--studio-tree-row:${collapsedPanels.tree ? '2.65rem' : 'minmax(12rem, 18rem)'}`,
			`--studio-tools-row:minmax(18rem, 24rem)`,
			`--studio-logs-height:${layoutSizes.logsHeight}px`
		].join(';')
	);

	$effect(() => {
		const id = selectedId;
		const currentProfile = profile;
		const serial = ++selectionMetaSerial;
		if (currentProfile === 'frontend') {
			return;
		}
		if (!isStepSelection(id)) {
			return;
		}
		void loadSelectionEditorMeta(id, serial);
	});

	$effect(() => {
		const projectName = selectedProjectName;
		const connectorName = selectedContext.connectorName;
		const transactionName = selectedContext.transactionName;
		const primaryTarget = primaryExecutionTarget;
		if (!connectorName || !transactionName || primaryTarget) {
			executionFallbackTarget = null;
			executionFallbackKey = '';
			return;
		}
		if (!projectName) {
			return;
		}
		const key = `${projectName}.${connectorName}.${transactionName}`;
		if (key === executionFallbackKey) {
			return;
		}
		executionFallbackKey = key;
		executionFallbackTarget = null;
		void loadExecutionFallback(key, projectName, connectorName, transactionName);
	});

	onMount(() => {
		restoreStudioLayoutPreferences();
		urlSyncReady = true;
	});

	$effect(() => {
		const routeId = routeSelectionId();
		if (routeId === lastRouteSelectionId) {
			return;
		}
		if (pendingRouteSelectionId && routeId !== pendingRouteSelectionId) {
			return;
		}
		lastRouteSelectionId = routeId;
		if (pendingRouteSelectionId === routeId) {
			pendingRouteSelectionId = '';
		}
		selectedId = routeId;
	});

	$effect(() => {
		if (!urlSyncReady) {
			return;
		}
		const hash = page.url.hash;
		if (hash === lastRouteWorkPanelHash) {
			return;
		}
		lastRouteWorkPanelHash = hash;
		const routePanel = workPanelFromHash(hash);
		if (!routePanel || routePanel === activeWorkPanel) {
			return;
		}
		activeWorkPanel = routePanel;
		persistStudioLayoutPreferences();
	});

	$effect(() => {
		if (!urlSyncReady) {
			return;
		}
		const nextId = selectedId.trim();
		const currentId = routeSelectionId();
		if (nextId === currentId) {
			pendingRouteSelectionId = '';
			lastRouteSelectionId = currentId;
			return;
		}
		if (pendingRouteSelectionId === nextId) {
			return;
		}
		pendingRouteSelectionId = nextId;
		void goto(selectionUrl(nextId), {
			replaceState: true,
			noScroll: true,
			keepFocus: true
		}).catch(() => {
			if (pendingRouteSelectionId === nextId) {
				pendingRouteSelectionId = '';
			}
		});
	});

	/**
	 * @returns {string}
	 */
	function routeSelectionId() {
		return decodeStudioSelectionId(page.params.qname ?? '');
	}

	/**
	 * @param {string} id
	 * @returns {string}
	 */
	function selectionUrl(id, hash = page.url.hash) {
		return studioSelectionUrl(STUDIO_BASE, id, page.url, hash);
	}

	/**
	 * @param {string} hash
	 * @returns {'execution' | 'code' | 'flow' | ''}
	 */
	function workPanelFromHash(hash) {
		const panel = String(hash ?? '').replace(/^#/, '');
		return /** @type {'execution' | 'code' | 'flow' | ''} */ (
			WORK_PANEL_IDS.includes(panel) ? panel : ''
		);
	}

	/**
	 * @param {'execution' | 'code' | 'flow'} panel
	 * @returns {string}
	 */
	function workPanelHash(panel) {
		return WORK_PANEL_IDS.includes(panel) ? `#${panel}` : '';
	}

	/**
	 * @param {string} id
	 * @returns {{ projectName: string, sequenceName: string, connectorName: string, transactionName: string }}
	 */
	function parseSelection(id) {
		if (!id || id === 'ROOT') {
			return { projectName: '', sequenceName: '', connectorName: '', transactionName: '' };
		}
		const segments = id.split('.');
		const projectName = segments[0]?.replace(/:.*/, '') ?? '';
		const byType = Object.fromEntries(
			segments
				.map((segment) => segment.match(/^([^:]+):(.*)$/))
				.filter(Boolean)
				.map((match) => [match?.[1], match?.[2] ?? ''])
		);
		return {
			projectName,
			sequenceName: byType.sq ?? '',
			connectorName: byType.cn ?? '',
			transactionName: byType.tr ?? ''
		};
	}

	/**
	 * @param {any} project
	 * @param {{ sequenceName: string, connectorName: string, transactionName: string }} context
	 * @returns {{ kind: 'sequence' | 'transaction', connectorName?: string, requestable: any } | null}
	 */
	function resolveExecutionTarget(project, context) {
		if (!project) {
			return null;
		}
		if (context.connectorName) {
			if (!context.transactionName) {
				return null;
			}
			return findConnectorRequestable(project, context);
		}
		if (!context.sequenceName) {
			return null;
		}
		const sequence = (project.sequence ?? []).find(
			(item) => item?.name === context.sequenceName
		) ?? {
			name: context.sequenceName,
			variable: [],
			testcase: []
		};
		return { kind: 'sequence', requestable: normalizeRequestable(sequence) };
	}

	/**
	 * @param {any} projectData
	 * @param {{ connectorName: string, transactionName: string }} context
	 * @returns {{ kind: 'transaction', connectorName?: string, requestable: any } | null}
	 */
	function findConnectorRequestable(projectData, context) {
		const connector = checkArray(projectData?.connector).find(
			(item) => item?.name === context.connectorName
		);
		const transactions = checkArray(connector?.transaction).filter(
			(transaction) => transaction?.name
		);
		const requestable =
			transactions.find((transaction) => transaction.name === context.transactionName) ??
			transactions[0] ??
			null;
		if (!requestable) {
			return null;
		}
		return {
			kind: 'transaction',
			connectorName: connector?.name,
			requestable: normalizeRequestable(requestable)
		};
	}

	/**
	 * @param {any} requestable
	 * @returns {any}
	 */
	function normalizeRequestable(requestable) {
		return {
			...requestable,
			variable: checkArray(requestable.variable).map((variable) => ({
				...variable,
				send: 'false'
			})),
			testcase: checkArray(requestable.testcase).map((testcase) => ({
				...testcase,
				variable: checkArray(testcase.variable).map((variable) => ({
					...variable,
					send: 'false'
				}))
			}))
		};
	}

	/**
	 * @param {string} key
	 * @param {string} projectName
	 * @param {string} connectorName
	 * @param {string} transactionName
	 */
	async function loadExecutionFallback(key, projectName, connectorName, transactionName) {
		const lang = navigator.languages?.[0] ?? navigator.language ?? 'en';
		const response = await call('projects.GetTestPlatform', { projectName, lang });
		if (key !== executionFallbackKey) {
			return;
		}
		executionFallbackTarget = findConnectorRequestable(response?.admin?.project, {
			connectorName,
			transactionName
		});
	}

	/**
	 * @param {string} id
	 * @returns {{ id: string, label: string, title: string }[]}
	 */
	function buildBreadcrumb(id) {
		if (!id || id === 'ROOT') {
			return [];
		}
		return id.split('.').reduce((items, segment) => {
			const previous = items[items.length - 1];
			const itemId = previous ? `${previous.id}.${segment}` : segment;
			const [, kind = '', name = segment] = segment.match(/^([^:]+):(.*)$/) ?? [];
			items.push({
				id: itemId,
				label: name || segment,
				title: kind ? `${kind}:${name}` : segment
			});
			return items;
		}, /** @type {{ id: string, label: string, title: string }[]} */ ([]));
	}

	/**
	 * @param {number} value
	 * @param {number} min
	 * @param {number} max
	 * @returns {number}
	 */
	function clamp(value, min, max) {
		return Math.min(max, Math.max(min, value));
	}

	/**
	 * @param {any} value
	 * @param {number} fallback
	 * @param {number} min
	 * @param {number} max
	 * @returns {number}
	 */
	function clampStoredNumber(value, fallback, min, max) {
		const number = Number(value);
		return Number.isFinite(number) ? clamp(number, min, max) : fallback;
	}

	/**
	 * @param {any} value
	 * @param {string[]} allowed
	 * @param {string} fallback
	 * @returns {string}
	 */
	function storedChoice(value, allowed, fallback) {
		return allowed.includes(value) ? value : fallback;
	}

	/**
	 * @returns {any}
	 */
	function readStudioLayoutPreferences() {
		if (!browser) {
			return null;
		}
		try {
			const raw = localStorage.getItem(STUDIO_LAYOUT_STORAGE_KEY);
			return raw ? JSON.parse(raw) : null;
		} catch (error) {
			console.warn('Unable to read Studio layout preferences', error);
			return null;
		}
	}

	/**
	 * @returns {any}
	 */
	function studioLayoutPreferences() {
		return {
			profile,
			activeSidePanel,
			activeWorkPanel,
			logsPanelOpen,
			collapsedPanels: {
				tree: collapsedPanels.tree,
				tools: collapsedPanels.tools
			},
			layoutSizes: {
				treeWidth: layoutSizes.treeWidth,
				toolsWidth: layoutSizes.toolsWidth,
				logsHeight: layoutSizes.logsHeight
			}
		};
	}

	function persistStudioLayoutPreferences() {
		if (!browser) {
			return;
		}
		try {
			localStorage.setItem(STUDIO_LAYOUT_STORAGE_KEY, JSON.stringify(studioLayoutPreferences()));
		} catch (error) {
			console.warn('Unable to save Studio layout preferences', error);
		}
	}

	function restoreStudioLayoutPreferences() {
		const preferences = readStudioLayoutPreferences();
		if (!preferences) {
			return;
		}
		profile = storedChoice(preferences.profile, PROFILE_IDS, profile);
		activeSidePanel = storedChoice(preferences.activeSidePanel, SIDE_PANEL_IDS, activeSidePanel);
		activeWorkPanel =
			workPanelFromHash(page.url.hash) ||
			/** @type {'execution' | 'code' | 'flow'} */ (
				storedChoice(preferences.activeWorkPanel, WORK_PANEL_IDS, activeWorkPanel)
			);
		logsPanelOpen = Boolean(preferences.logsPanelOpen);
		collapsedPanels = {
			...DEFAULT_COLLAPSED_PANELS,
			tree: Boolean(preferences.collapsedPanels?.tree),
			tools: Boolean(preferences.collapsedPanels?.tools)
		};
		layoutSizes = {
			treeWidth: clampStoredNumber(
				preferences.layoutSizes?.treeWidth,
				DEFAULT_LAYOUT_SIZES.treeWidth,
				MIN_TREE_WIDTH,
				MAX_TREE_WIDTH
			),
			toolsWidth: clampStoredNumber(
				preferences.layoutSizes?.toolsWidth,
				DEFAULT_LAYOUT_SIZES.toolsWidth,
				MIN_TOOLS_WIDTH,
				MAX_TOOLS_WIDTH
			),
			logsHeight: clampStoredNumber(
				preferences.layoutSizes?.logsHeight,
				DEFAULT_LAYOUT_SIZES.logsHeight,
				MIN_LOGS_HEIGHT,
				getMaxLogsHeight()
			)
		};
		normalizeLayoutPanels();
	}

	function normalizeLayoutPanels() {
		if (profile === 'frontend') {
			if (!['devices', 'properties'].includes(activeSidePanel)) {
				activeSidePanel = 'devices';
			}
			activeWorkPanel = 'execution';
			return;
		}
		if (collapsedPanels.tools || !SIDE_PANEL_IDS.includes(activeSidePanel)) {
			activeSidePanel = 'properties';
		}
		if (!WORK_PANEL_IDS.includes(activeWorkPanel)) {
			activeWorkPanel = 'execution';
		}
	}

	function getMaxLogsHeight() {
		return Math.max(MIN_LOGS_HEIGHT, Math.min(MAX_LOGS_HEIGHT, window.innerHeight - 180));
	}

	/**
	 * @param {'tree' | 'tools' | 'logs'} target
	 * @param {number} delta
	 */
	function resizePanel(target, delta) {
		if (target === 'tree') {
			layoutSizes.treeWidth = clamp(layoutSizes.treeWidth + delta, MIN_TREE_WIDTH, MAX_TREE_WIDTH);
		} else if (target === 'tools') {
			layoutSizes.toolsWidth = clamp(
				layoutSizes.toolsWidth + delta,
				MIN_TOOLS_WIDTH,
				MAX_TOOLS_WIDTH
			);
		} else {
			layoutSizes.logsHeight = clamp(
				layoutSizes.logsHeight + delta,
				MIN_LOGS_HEIGHT,
				getMaxLogsHeight()
			);
		}
		persistStudioLayoutPreferences();
	}

	/**
	 * @param {PointerEvent} event
	 * @param {'tree' | 'tools' | 'logs'} target
	 */
	function startResize(event, target) {
		if (event.button !== 0) {
			return;
		}
		if (event.currentTarget instanceof HTMLElement) {
			event.currentTarget.focus();
		}
		event.preventDefault();
		const startX = event.clientX;
		const startY = event.clientY;
		const startSizes = { ...layoutSizes };
		function onMove(moveEvent) {
			if (target === 'logs') {
				layoutSizes.logsHeight = clamp(
					startSizes.logsHeight - (moveEvent.clientY - startY),
					MIN_LOGS_HEIGHT,
					getMaxLogsHeight()
				);
				return;
			}
			const delta = moveEvent.clientX - startX;
			if (target === 'tree') {
				layoutSizes.treeWidth = clamp(startSizes.treeWidth + delta, MIN_TREE_WIDTH, MAX_TREE_WIDTH);
			} else {
				layoutSizes.toolsWidth = clamp(
					startSizes.toolsWidth + delta,
					MIN_TOOLS_WIDTH,
					MAX_TOOLS_WIDTH
				);
			}
		}
		function onUp() {
			window.removeEventListener('pointermove', onMove);
			window.removeEventListener('pointerup', onUp);
			persistStudioLayoutPreferences();
		}
		window.addEventListener('pointermove', onMove);
		window.addEventListener('pointerup', onUp, { once: true });
	}

	/**
	 * @param {KeyboardEvent} event
	 * @param {'tree' | 'tools' | 'logs'} target
	 */
	function resizeWithKeyboard(event, target) {
		const step = event.shiftKey ? 32 : 16;
		if (target === 'logs' && (event.key === 'ArrowUp' || event.key === 'ArrowDown')) {
			event.preventDefault();
			resizePanel('logs', event.key === 'ArrowUp' ? step : -step);
		} else if (target !== 'logs' && (event.key === 'ArrowLeft' || event.key === 'ArrowRight')) {
			event.preventDefault();
			resizePanel(target, event.key === 'ArrowRight' ? step : -step);
		}
	}

	/**
	 * @param {string} id
	 * @returns {boolean}
	 */
	function isStepSelection(id) {
		return /\.st:|\.step:|Step/i.test(id ?? '');
	}

	/**
	 * @param {string} id
	 * @param {number} serial
	 */
	async function loadSelectionEditorMeta(id, serial) {
		const response = await call('studio.properties.Get', { id });
		if (serial !== selectionMetaSerial) {
			return;
		}
		const properties = Object.entries(response?.properties ?? {}).map(([key, property]) => ({
			displayName: key,
			originalValue: property.value,
			...property
		}));
		const editorProperty = findPrimaryEditorProperty(properties, id);
		if (
			editorProperty &&
			isCodeEditorProperty(editorProperty, id) &&
			activeWorkPanel === 'execution'
		) {
			setWorkPanel('code');
		}
	}

	/**
	 * @param {EditorTarget} target
	 */
	function openPropertyEditor(target) {
		if (!target?.id) {
			return;
		}
		editorTarget = {
			...target,
			serial: Date.now()
		};
		selectedId = target.id;
		setWorkPanel('code');
	}

	/**
	 * @param {{ id: string, data: any }} node
	 */
	function onFlowNodeSelected(node) {
		if (node?.data?.originalId) {
			selectedId = node.data.originalId;
		}
	}

	/**
	 * @param {import('$lib/studio/dnd').DboDropResult} mutation
	 */
	async function onStudioMutation(mutation) {
		const serial = ++mutationRefreshSerial;
		lastStudioMutation = mutation;
		studioMutationSerial += 1;
		const nextSelection = mutation?.selectedId || mutation?.id;
		if (nextSelection) {
			selectedId = nextSelection;
		}
		if (mutation?.payload?.type === 'paletteData' && mutation?.source !== 'flow' && nextSelection) {
			renameTargetId = nextSelection;
		} else if (mutation?.payload?.type === 'renameData') {
			renameTargetId = '';
		}
		try {
			await refreshStudioProject(
				nextSelection || mutation?.parentId || mutation?.target || selectedId
			);
			markProjectDirty(nextSelection || mutation?.parentId || mutation?.target || selectedId);
		} finally {
			if (serial === mutationRefreshSerial) {
				// StudioTreePanel refreshes only the affected mutation context; a global tree
				// refresh can collapse expanded branches while rename/reveal is in progress.
				refreshStudioViews({ tree: false, flow: true });
			}
		}
	}

	/**
	 * @param {string} id
	 */
	async function refreshStudioProject(id) {
		const projectName = parseSelection(id).projectName || selectedProjectName;
		if (!projectName) {
			return;
		}
		await TestPlatform(projectName).refresh();
	}

	/**
	 * @param {string} id
	 */
	async function refreshAfterPropertySave(id) {
		if (!id) {
			return;
		}
		await refreshStudioProject(id);
		markProjectDirty(id);
		refreshStudioViews();
	}

	async function saveSelectedProject() {
		if (!selectedProjectName || projectActionBusy) {
			return;
		}
		projectActionBusy = 'save';
		try {
			await saveDboProject(selectedProjectName, selectedId);
			await refreshStudioProject(selectedProjectName);
			clearProjectDirty(selectedProjectName);
			refreshStudioViews();
		} finally {
			projectActionBusy = '';
		}
	}

	async function reloadSelectedProject() {
		if (!selectedProjectName || projectActionBusy) {
			return;
		}
		projectActionBusy = 'reload';
		try {
			await call('projects.Reload', { projectName: selectedProjectName });
			await Projects.refresh();
			await refreshStudioProject(selectedProjectName);
			clearProjectDirty(selectedProjectName);
			refreshStudioViews();
		} finally {
			projectActionBusy = '';
		}
	}

	/**
	 * @param {{ tree?: boolean, flow?: boolean }} [options]
	 */
	function refreshStudioViews(options = {}) {
		const { tree = true, flow = true } = options;
		if (tree) {
			treeRefreshSerial += 1;
		}
		if (flow) {
			flowRefreshSerial += 1;
		}
	}

	/**
	 * @param {string} id
	 */
	function markProjectDirty(id) {
		const projectName = parseSelection(id).projectName || selectedProjectName;
		if (!projectName || dirtyProjectNames.has(projectName)) {
			return;
		}
		dirtyProjectNames = new SvelteSet([...dirtyProjectNames, projectName]);
	}

	/**
	 * @param {string} projectName
	 */
	function clearProjectDirty(projectName) {
		if (!projectName || !dirtyProjectNames.has(projectName)) {
			return;
		}
		const nextDirtyProjects = new SvelteSet(dirtyProjectNames);
		nextDirtyProjects.delete(projectName);
		dirtyProjectNames = nextDirtyProjects;
	}

	/**
	 * @param {string} id
	 */
	function selectObject(id) {
		if (id) {
			selectedId = id;
		}
	}

	/**
	 * @param {string} nextProfile
	 */
	function setProfile(nextProfile) {
		const previousProfile = profile;
		profile = storedChoice(nextProfile, PROFILE_IDS, profile);
		if (previousProfile !== profile && profile === 'frontend') {
			activeSidePanel = 'devices';
		}
		normalizeLayoutPanels();
		persistStudioLayoutPreferences();
	}

	/**
	 * @param {'tree' | 'tools'} panel
	 */
	function toggleCollapsedPanel(panel) {
		collapsedPanels[panel] = !collapsedPanels[panel];
		normalizeLayoutPanels();
		persistStudioLayoutPreferences();
	}

	/**
	 * @param {boolean} open
	 */
	function setLogsPanelOpen(open) {
		logsPanelOpen = open;
		persistStudioLayoutPreferences();
	}

	/**
	 * @param {'execution' | 'code' | 'flow'} panel
	 */
	function setWorkPanel(panel) {
		activeWorkPanel = panel;
		persistStudioLayoutPreferences();
		if (!browser) {
			return;
		}
		const hash = workPanelHash(panel);
		lastRouteWorkPanelHash = hash;
		void goto(selectionUrl(selectedId, hash), {
			replaceState: true,
			noScroll: true,
			keepFocus: true
		});
	}

	/**
	 * @param {string} panel
	 */
	function setSidePanel(panel) {
		activeSidePanel = panel;
		persistStudioLayoutPreferences();
	}
</script>

<svelte:head>
	<title>Convertigo Studio</title>
</svelte:head>

{#snippet projectActions()}
	<Button
		full={false}
		icon={projectActionBusy === 'save' ? 'mdi:sync' : 'mdi:content-save-edit-outline'}
		class={`studio__panel-action button-ico-secondary h-8! w-8! justify-center p-0! ${
			selectedProjectDirty ? 'studio__panel-action--dirty' : ''
		}`}
		title={selectedProjectDirty ? 'Save project - unsaved changes' : 'Save project'}
		ariaLabel={selectedProjectDirty ? 'Save project - unsaved changes' : 'Save project'}
		disabled={!selectedProjectName || Boolean(projectActionBusy)}
		onclick={saveSelectedProject}
	/>
	<Button
		full={false}
		icon={projectActionBusy === 'reload' ? 'mdi:sync' : 'mdi:reload'}
		class="studio__panel-action button-ico-secondary h-8! w-8! justify-center p-0!"
		title="Reload project"
		ariaLabel="Reload project"
		disabled={!selectedProjectName || Boolean(projectActionBusy)}
		onclick={reloadSelectedProject}
	/>
{/snippet}

<section
	class={`studio studio--${profile}`}
	class:studio--tree-hidden={collapsedPanels.tree}
	class:studio--tools-hidden={collapsedPanels.tools}
	style={workspaceStyle}
>
	<header class="studio__topbar">
		<div class="studio__brand">
			<span class="studio__logo">
				<Ico icon="convertigo:logo" size={5} />
			</span>
			<div class="studio__title">
				<strong>Convertigo Studio</strong>
			</div>
			<div class="studio__view-toggles" aria-label="Studio views">
				<button
					type="button"
					aria-label={collapsedPanels.tree ? 'Show projects' : 'Hide projects'}
					aria-pressed={!collapsedPanels.tree}
					title={collapsedPanels.tree ? 'Show projects' : 'Hide projects'}
					class:studio__view-toggle--active={!collapsedPanels.tree}
					class="studio__view-toggle"
					onclick={() => toggleCollapsedPanel('tree')}
				>
					<Ico icon="mdi:folder-outline" size={4} />
				</button>
				<button
					type="button"
					aria-label={collapsedPanels.tools
						? 'Show palette and properties'
						: 'Hide palette and properties'}
					aria-pressed={!collapsedPanels.tools}
					title={collapsedPanels.tools
						? 'Show palette and properties'
						: 'Hide palette and properties'}
					class:studio__view-toggle--active={!collapsedPanels.tools}
					class="studio__view-toggle"
					onclick={() => toggleCollapsedPanel('tools')}
				>
					<Ico icon="mdi:tune-vertical-variant" size={4} />
				</button>
			</div>
		</div>

		<nav class="studio__breadcrumb" aria-label="Selection path">
			{#if breadcrumbs.length}
				{#each breadcrumbs as item, index (item.id)}
					{#if index > 0}
						<Ico icon="mdi:chevron-right" size={3} />
					{/if}
					<button type="button" title={item.title} onclick={() => selectObject(item.id)}>
						{item.label}
					</button>
				{/each}
			{:else}
				<span>Projects</span>
			{/if}
		</nav>

		<div class="studio__actions">
			<div class="studio__profiles" role="radiogroup" aria-label="Studio profile">
				{#each profiles as item (item.id)}
					<button
						type="button"
						role="radio"
						aria-checked={profile === item.id}
						class:studio__profile--active={profile === item.id}
						class="studio__profile"
						title={item.description}
						onclick={() => setProfile(item.id)}
					>
						<Ico icon={item.icon} size={4} />
						<span>{item.label}</span>
					</button>
				{/each}
			</div>
			<span class="studio__theme-switch">
				<LightSwitch />
			</span>
			{#if showFlowOverview}
				<button
					type="button"
					class="studio__flow-button"
					title="Show flow"
					aria-label="Show flow"
					onclick={() => setWorkPanel('flow')}
				>
					<Ico icon="mdi:source-branch" size={4} />
				</button>
			{/if}
			<Button
				full={false}
				href={resolve('/admin/')}
				icon="mdi:lock-outline"
				class="button-ico-secondary h-9! w-9! justify-center p-0!"
				title="Admin console"
				ariaLabel="Admin console"
			/>
		</div>
	</header>

	<div class="studio__workspace">
		<div class="studio__tree" class:studio__tree--hidden={collapsedPanels.tree}>
			<StudioPanel
				title="Projects"
				icon="mdi:folder-outline"
				class="studio__tree-panel"
				actions={projectActions}
			>
				<StudioTreePanel
					bind:selectedId
					bind:renameTargetId
					refreshSerial={treeRefreshSerial}
					refreshMutation={lastStudioMutation}
					refreshMutationSerial={studioMutationSerial}
					onMutation={onStudioMutation}
				/>
			</StudioPanel>
		</div>
		<button
			type="button"
			class={`studio-resizer studio-resizer--vertical studio-resizer--tree ${
				collapsedPanels.tree ? 'studio-resizer--hidden' : ''
			}`}
			aria-label="Resize projects panel"
			title="Resize projects panel"
			onpointerdown={(event) => startResize(event, 'tree')}
			onkeydown={(event) => resizeWithKeyboard(event, 'tree')}
		></button>

		<main class="studio__main">
			{#if showStudioWork}
				<div class="studio-accordion studio-work">
					<section
						class="studio-accordion__section"
						class:studio-accordion__section--active={activeWorkPanel === 'execution'}
					>
						<button
							type="button"
							class="studio-accordion__trigger"
							aria-expanded={activeWorkPanel === 'execution'}
							onclick={() => setWorkPanel('execution')}
						>
							<span><Ico icon="mdi:play-circle-outline" size={4} />Execution</span>
							<Ico
								icon={activeWorkPanel === 'execution' ? 'mdi:chevron-down' : 'mdi:chevron-right'}
								size={4}
							/>
						</button>
						<div class="studio-accordion__content" hidden={activeWorkPanel !== 'execution'}>
							<StudioExecutionPanel
								projectName={selectedProjectName}
								requestable={executionTarget?.requestable ?? null}
								requestableKind={executionTarget?.kind ?? ''}
								connectorName={executionTarget?.connectorName ?? ''}
							/>
						</div>
					</section>

					<section
						class="studio-accordion__section"
						class:studio-accordion__section--active={activeWorkPanel === 'code'}
					>
						<button
							type="button"
							class="studio-accordion__trigger"
							aria-expanded={activeWorkPanel === 'code'}
							onclick={() => setWorkPanel('code')}
						>
							<span><Ico icon="mdi:code-tags" size={4} />Code</span>
							<Ico
								icon={activeWorkPanel === 'code' ? 'mdi:chevron-down' : 'mdi:chevron-right'}
								size={4}
							/>
						</button>
						<div
							class="studio-accordion__content studio-accordion__content--fill"
							hidden={activeWorkPanel !== 'code'}
						>
							<StudioEditorPanel
								{selectedId}
								{editorTarget}
								active={activeWorkPanel === 'code'}
								onSave={refreshAfterPropertySave}
								onSelectObject={selectObject}
							/>
						</div>
					</section>

					<section
						class="studio-accordion__section"
						class:studio-accordion__section--active={activeWorkPanel === 'flow'}
					>
						<button
							type="button"
							class="studio-accordion__trigger"
							aria-expanded={activeWorkPanel === 'flow'}
							disabled={!showFlowOverview}
							onclick={() => setWorkPanel('flow')}
						>
							<span><Ico icon="mdi:source-branch" size={4} />Flow</span>
							<Ico
								icon={activeWorkPanel === 'flow' ? 'mdi:chevron-down' : 'mdi:chevron-right'}
								size={4}
							/>
						</button>
						{#if activeWorkPanel === 'flow'}
							<div class="studio-accordion__content studio-accordion__content--fill">
								{#if showFlowOverview}
									<FlowViewer
										projectName={selectedProjectName}
										{sequences}
										selectedSequenceName={selectedFlowSequenceName}
										autoSelectFirst={false}
										selectedObjectId={selectedId}
										refreshSerial={flowRefreshSerial}
										refreshMutation={lastStudioMutation}
										refreshMutationSerial={studioMutationSerial}
										onSelectNode={onFlowNodeSelected}
										onMutation={onStudioMutation}
									/>
								{:else}
									<div class="studio__empty">No sequence selected</div>
								{/if}
							</div>
						{/if}
					</section>
				</div>
			{:else}
				<StudioPanel
					title="Frontend"
					icon="mdi:smartphone-link"
					class="studio__primary-panel"
					contentClass="studio__panel-fill"
				>
					<StudioPreviewPanel
						projectName={selectedProjectName}
						bind:selectedDeviceId={frontendDeviceId}
						bind:landscape={frontendLandscape}
						showDeviceSelector={false}
					/>
				</StudioPanel>
			{/if}
		</main>

		<aside class="studio__tools" class:studio__tools--hidden={collapsedPanels.tools}>
			<div class="studio-side-stack">
				<div class="studio-view-picker" role="tablist" aria-label="Studio side views">
					{#each sideViews as view (view.id)}
						<button
							type="button"
							role="tab"
							class="studio-view-picker__item"
							class:studio-view-picker__item--active={effectiveSidePanel === view.id}
							aria-selected={effectiveSidePanel === view.id}
							title={view.label}
							onclick={() => setSidePanel(view.id)}
						>
							<Ico icon={view.icon} size={4} />
							<span>{view.label}</span>
						</button>
					{/each}
				</div>

				<div
					class="studio-side-stack__body"
					role="tabpanel"
					aria-label={activeSideView?.label ?? 'Side view'}
				>
					{#if showPalette}
						<div class="studio-side-stack__pane" hidden={effectiveSidePanel !== 'palette'}>
							<StudioPalettePanel {selectedId} active={effectiveSidePanel === 'palette'} />
						</div>
					{/if}
					{#if showDevicePicker}
						<div class="studio-side-stack__pane" hidden={effectiveSidePanel !== 'devices'}>
							<StudioDevicePanel
								bind:selectedDeviceId={frontendDeviceId}
								bind:landscape={frontendLandscape}
							/>
						</div>
					{/if}
					{#if showSourcePicker}
						<div class="studio-side-stack__pane" hidden={effectiveSidePanel !== 'picker'}>
							<StudioSourcePickerPanel
								{selectedId}
								active={effectiveSidePanel === 'picker'}
								onSelectObject={selectObject}
							/>
						</div>
					{/if}
					<div class="studio-side-stack__pane" hidden={effectiveSidePanel !== 'properties'}>
						<StudioPropertiesPanel
							{selectedId}
							active={effectiveSidePanel === 'properties'}
							onSave={refreshAfterPropertySave}
							onOpenPropertyEditor={openPropertyEditor}
						/>
					</div>
				</div>
			</div>
		</aside>
		<button
			type="button"
			class={`studio-resizer studio-resizer--vertical studio-resizer--tools ${
				collapsedPanels.tools ? 'studio-resizer--hidden' : ''
			}`}
			aria-label="Resize tools panel"
			title="Resize tools panel"
			onpointerdown={(event) => startResize(event, 'tools')}
			onkeydown={(event) => resizeWithKeyboard(event, 'tools')}
		></button>
	</div>

	{#if logsPanelOpen}
		<section class="studio__logs-panel" aria-label="Logs">
			<button
				type="button"
				class="studio-resizer studio-resizer--logs"
				aria-label="Resize logs panel"
				title="Resize logs panel"
				onpointerdown={(event) => startResize(event, 'logs')}
				onkeydown={(event) => resizeWithKeyboard(event, 'logs')}
			></button>
			<header class="studio__logs-panel-header">
				<strong><Ico icon="mdi:file-document-box-outline" size={4} />Logs</strong>
				<Button
					full={false}
					icon="mdi:chevron-down"
					class="button-ico-secondary h-8! w-8! justify-center p-0!"
					title="Collapse logs"
					ariaLabel="Collapse logs"
					onclick={() => setLogsPanelOpen(false)}
				/>
			</header>
			<div class="studio__logs-panel-body">
				<StudioLogsPanel />
			</div>
		</section>
	{/if}

	{#if !logsPanelOpen}
		<button
			type="button"
			class="studio__logs-bar"
			aria-expanded={logsPanelOpen}
			onclick={() => setLogsPanelOpen(true)}
		>
			<span><Ico icon="mdi:file-document-box-outline" size={4} />Logs</span>
			<Ico icon="mdi:chevron-up" size={4} />
		</button>
	{/if}
</section>

<style>
	.studio {
		--studio-shell-bg: color-mix(
			in oklab,
			var(--color-surface-100-900) 82%,
			var(--color-surface-200-800)
		);
		--studio-panel-bg: var(--color-surface-50-950);
		--studio-panel-header-bg: color-mix(in oklab, var(--color-surface-100-900) 88%, transparent);
		display: grid;
		width: 100%;
		height: 100vh;
		min-width: 0;
		min-height: 0;
		grid-template-rows: auto minmax(0, 1fr) auto;
		background: var(--studio-shell-bg);
		color: var(--color-surface-950-50);
	}

	.studio__topbar {
		display: grid;
		grid-template-columns: minmax(11rem, auto) minmax(0, 1fr) auto;
		align-items: center;
		gap: 0.75rem;
		border-bottom: 1px solid var(--color-surface-200-800);
		background: color-mix(in oklab, var(--studio-panel-bg) 92%, transparent);
		padding: 0.55rem 0.75rem;
	}

	.studio__brand,
	.studio__actions,
	.studio__profiles,
	.studio__profile,
	.studio__breadcrumb {
		display: flex;
		align-items: center;
	}

	.studio__brand {
		min-width: 0;
		gap: 0.65rem;
	}

	.studio__logo {
		display: grid;
		width: 2rem;
		height: 2rem;
		flex: 0 0 auto;
		place-items: center;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: var(--color-surface-50-950);
		color: var(--color-primary-600-400);
	}

	.studio__title {
		display: grid;
		min-width: 0;
		gap: 0.08rem;
	}

	.studio__view-toggles {
		display: flex;
		flex: 0 0 auto;
		align-items: center;
		gap: 0.18rem;
		margin-left: 0.15rem;
	}

	.studio__view-toggle {
		display: grid;
		width: 2rem;
		height: 2rem;
		place-items: center;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.38rem;
		background: var(--studio-shell-bg);
		color: var(--color-surface-600-400);
		padding: 0;
	}

	.studio__view-toggle:hover,
	.studio__view-toggle--active {
		border-color: color-mix(in oklab, var(--color-primary-500) 38%, transparent);
		background: color-mix(in oklab, var(--color-primary-500) 10%, transparent);
		color: var(--color-primary-600-400);
	}

	.studio__title strong,
	.studio__breadcrumb button,
	.studio__breadcrumb span {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio__title strong {
		font-size: 0.9rem;
		line-height: 1.1;
	}

	.studio__profiles {
		flex: 0 0 auto;
		gap: 0.12rem;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: var(--studio-shell-bg);
		padding: 0.16rem;
	}

	.studio__profile {
		gap: 0.35rem;
		height: 2rem;
		border: 0;
		border-radius: 0.3rem;
		background: transparent;
		color: var(--color-surface-700-300);
		padding: 0 0.65rem;
		font-size: 0.78rem;
		font-weight: 700;
	}

	.studio__profile:hover {
		color: var(--color-surface-950-50);
		background: color-mix(in oklab, var(--color-surface-300-700) 40%, transparent);
	}

	.studio__profile--active {
		background: var(--color-primary-500);
		color: var(--color-primary-contrast-500);
	}

	.studio__actions {
		min-width: 0;
		justify-content: flex-end;
		gap: 0.5rem;
	}

	.studio__breadcrumb {
		min-width: 0;
		justify-self: stretch;
		gap: 0.18rem;
		color: var(--color-surface-600-400);
		font-size: 0.76rem;
	}

	.studio__breadcrumb button,
	.studio__breadcrumb span {
		max-width: 14rem;
		border: 0;
		border-radius: 0.3rem;
		background: transparent;
		color: inherit;
		padding: 0.28rem 0.32rem;
		font-size: inherit;
		font-weight: 650;
		text-align: left;
	}

	.studio__breadcrumb button:hover {
		background: color-mix(in oklab, var(--color-primary-500) 10%, transparent);
		color: var(--color-primary-700-300);
	}

	.studio__theme-switch {
		display: flex;
		flex: 0 0 auto;
		align-items: center;
	}

	.studio__flow-button {
		display: grid;
		width: 2.25rem;
		height: 2.25rem;
		place-items: center;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.4rem;
		background: var(--studio-panel-bg);
		color: var(--color-surface-700-300);
		padding: 0;
	}

	.studio__flow-button:hover {
		background: color-mix(in oklab, var(--color-primary-500) 10%, transparent);
		color: var(--color-primary-600-400);
	}

	:global(.studio__panel-action--dirty) {
		position: relative;
		border-color: color-mix(in oklab, var(--color-primary-500) 55%, transparent);
		color: var(--color-primary-600-400);
	}

	:global(.studio__panel-action--dirty)::after {
		position: absolute;
		top: 0.28rem;
		right: 0.28rem;
		width: 0.42rem;
		height: 0.42rem;
		border: 1px solid var(--studio-panel-bg);
		border-radius: 999px;
		background: var(--color-primary-500);
		content: '';
	}

	.studio__workspace {
		display: grid;
		min-width: 0;
		min-height: 0;
		column-gap: 0.28rem;
		row-gap: 0.55rem;
		padding: 0.55rem;
	}

	.studio--backend .studio__workspace,
	.studio--frontend .studio__workspace {
		grid-template-columns:
			var(--studio-tree-track) var(--studio-tree-resizer-track) var(--studio-tools-track)
			var(--studio-tools-resizer-track)
			minmax(0, 1fr);
		grid-template-areas: 'tree tree-resizer tools tools-resizer main';
	}

	.studio__tree--hidden,
	.studio__tools--hidden,
	.studio-resizer--hidden {
		display: none;
	}

	.studio__tree {
		grid-area: tree;
		min-width: 0;
		min-height: 0;
	}

	.studio__tree :global(.studio__tree-panel) {
		height: 100%;
	}

	.studio__main {
		grid-area: main;
		min-width: 0;
		min-height: 0;
		overflow: hidden;
	}

	.studio__tools {
		grid-area: tools;
		min-width: 0;
		min-height: 0;
	}

	.studio-resizer {
		position: relative;
		min-width: 0;
		min-height: 0;
		border: 0;
		background: transparent;
		padding: 0;
	}

	.studio-resizer::before {
		position: absolute;
		border-radius: 999px;
		background: transparent;
		content: '';
		transition:
			background 0.14s ease,
			inset 0.14s ease;
	}

	.studio-resizer:hover::before,
	.studio-resizer:focus-visible::before {
		background: color-mix(in oklab, var(--color-primary-500) 42%, transparent);
	}

	.studio-resizer--vertical {
		cursor: col-resize;
	}

	.studio-resizer--vertical::before {
		inset: 0.35rem 0.16rem;
	}

	.studio-resizer--vertical:hover::before,
	.studio-resizer--vertical:focus-visible::before {
		inset: 0.15rem 0.08rem;
	}

	.studio-resizer--tree {
		grid-area: tree-resizer;
	}

	.studio-resizer--tools {
		grid-area: tools-resizer;
	}

	.studio-resizer--logs {
		position: absolute;
		z-index: 2;
		top: -0.28rem;
		right: 0;
		left: 0;
		height: 0.55rem;
		cursor: row-resize;
	}

	.studio-resizer--logs::before {
		inset: 0.2rem 48%;
	}

	.studio-resizer--logs:hover::before,
	.studio-resizer--logs:focus-visible::before {
		inset: 0.12rem 44%;
	}

	.studio__primary-panel {
		height: 100%;
		min-width: 0;
		min-height: 0;
	}

	.studio-side-stack {
		display: grid;
		height: 100%;
		min-width: 0;
		min-height: 0;
		grid-template-rows: auto minmax(0, 1fr);
		overflow: hidden;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: var(--studio-panel-bg);
	}

	.studio-view-picker {
		display: grid;
		grid-auto-columns: minmax(0, 1fr);
		grid-auto-flow: column;
		gap: 0.18rem;
		border-bottom: 1px solid var(--color-surface-200-800);
		background: var(--studio-panel-header-bg);
		padding: 0.22rem;
	}

	.studio-view-picker__item {
		display: flex;
		min-width: 0;
		height: 2.15rem;
		align-items: center;
		justify-content: center;
		gap: 0.35rem;
		border: 1px solid transparent;
		border-radius: 0.3rem;
		background: transparent;
		color: var(--color-surface-700-300);
		padding: 0 0.45rem;
		font-size: 0.72rem;
		font-weight: 750;
		text-transform: uppercase;
	}

	.studio-view-picker__item:hover,
	.studio-view-picker__item--active {
		border-color: color-mix(in oklab, var(--color-primary-500) 38%, transparent);
		background: color-mix(in oklab, var(--color-primary-500) 11%, transparent);
		color: var(--color-primary-600-400);
	}

	.studio-view-picker__item span {
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-side-stack__body {
		min-width: 0;
		min-height: 0;
		overflow: hidden;
	}

	.studio-side-stack__pane {
		height: 100%;
		min-width: 0;
		min-height: 0;
	}

	.studio-side-stack__pane[hidden] {
		display: none;
	}

	.studio-accordion {
		display: flex;
		height: 100%;
		min-width: 0;
		min-height: 0;
		flex-direction: column;
		gap: 0.45rem;
	}

	.studio-accordion__section {
		display: flex;
		min-width: 0;
		min-height: 0;
		flex: 0 0 auto;
		flex-direction: column;
		overflow: hidden;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: var(--studio-panel-bg);
	}

	.studio-accordion__section--active {
		flex: 1 1 0;
	}

	.studio-accordion__trigger {
		display: flex;
		min-height: 2.45rem;
		width: 100%;
		align-items: center;
		justify-content: space-between;
		gap: 0.75rem;
		border: 0;
		border-bottom: 1px solid var(--color-surface-200-800);
		background: var(--studio-panel-header-bg);
		color: var(--color-surface-800-200);
		padding: 0.45rem 0.65rem;
		font-size: 0.78rem;
		font-weight: 700;
		text-align: left;
		text-transform: uppercase;
	}

	.studio-accordion__trigger:hover:not(:disabled) {
		background: color-mix(in oklab, var(--color-primary-500) 9%, transparent);
		color: var(--color-surface-950-50);
	}

	.studio-accordion__trigger:disabled {
		color: var(--color-surface-500);
		cursor: not-allowed;
	}

	.studio-accordion__trigger span {
		display: flex;
		min-width: 0;
		align-items: center;
		gap: 0.45rem;
	}

	.studio-accordion__content {
		min-height: 0;
		flex: 1;
		overflow: auto;
	}

	.studio-accordion__content--fill {
		overflow: hidden;
	}

	.studio-accordion__content[hidden] {
		display: none;
	}

	.studio-accordion :global(.flow-dashboard) {
		height: 100%;
		min-height: 0;
		border: 0;
		border-radius: 0;
	}

	:global(.studio__panel-fill) {
		overflow: hidden;
	}

	.studio__empty {
		display: grid;
		height: 100%;
		place-items: center;
		color: var(--color-surface-600-400);
		font-size: 0.86rem;
	}

	.studio__logs-bar {
		display: flex;
		min-height: 2.45rem;
		align-items: center;
		justify-content: space-between;
		gap: 0.75rem;
		margin: 0 0.55rem 0.55rem;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: var(--studio-panel-header-bg);
		color: var(--color-surface-800-200);
		padding: 0.45rem 0.65rem;
		font-size: 0.78rem;
		font-weight: 700;
		text-transform: uppercase;
	}

	.studio__logs-bar:hover {
		background: color-mix(in oklab, var(--color-primary-500) 9%, transparent);
		color: var(--color-surface-950-50);
	}

	.studio__logs-bar span {
		display: flex;
		align-items: center;
		gap: 0.45rem;
	}

	.studio__logs-panel {
		position: relative;
		display: grid;
		height: min(var(--studio-logs-height), calc(100vh - 10rem));
		min-width: 0;
		min-height: 0;
		grid-template-rows: auto minmax(0, 1fr);
		overflow: hidden;
		margin: 0 0.55rem 0.45rem;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: var(--studio-panel-bg);
		box-shadow: 0 -0.75rem 1.75rem color-mix(in oklab, var(--color-surface-950) 8%, transparent);
	}

	.studio__logs-panel-header {
		display: flex;
		min-height: 2.45rem;
		min-width: 0;
		align-items: center;
		justify-content: space-between;
		gap: 0.75rem;
		border-bottom: 1px solid var(--color-surface-200-800);
		background: var(--studio-panel-header-bg);
		color: var(--color-surface-800-200);
		padding: 0.45rem 0.65rem;
	}

	.studio__logs-panel-header strong {
		display: flex;
		min-width: 0;
		align-items: center;
		gap: 0.45rem;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		font-size: 0.78rem;
		line-height: 1.1;
		text-transform: uppercase;
	}

	.studio__logs-panel-body {
		min-width: 0;
		min-height: 0;
		overflow: hidden;
	}

	@media (max-width: 1180px) {
		.studio__topbar {
			grid-template-columns: 1fr;
			align-items: stretch;
		}

		.studio__breadcrumb {
			overflow-x: auto;
		}

		.studio__profiles {
			justify-self: stretch;
			overflow-x: auto;
		}

		.studio__profile {
			flex: 1 0 auto;
			justify-content: center;
		}

		.studio__actions {
			justify-content: space-between;
		}

		.studio__workspace,
		.studio--backend .studio__workspace,
		.studio--frontend .studio__workspace {
			grid-template-columns: minmax(0, 1fr);
			grid-template-rows: var(--studio-tree-row) var(--studio-tools-row) minmax(24rem, 1fr);
			grid-template-areas:
				'tree'
				'tools'
				'main';
			overflow: auto;
		}

		.studio-resizer--vertical {
			display: none;
		}
	}
</style>
