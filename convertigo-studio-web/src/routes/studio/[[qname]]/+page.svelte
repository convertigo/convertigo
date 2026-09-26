<script>
	import { browser } from '$app/environment';
	import { goto, replaceState } from '$app/navigation';
	import { page } from '$app/state';
	import { subscribeAdminEvents } from '$lib/admin/adminEvents';
	import Projects from '$lib/common/Projects.svelte.js';
	import TestPlatform from '$lib/common/TestPlatform.svelte';
	import {
		blockDefinitionForInstance,
		blockDefinitionSourceId,
		findBlockDefinition,
		flowTypeDisplayName,
		isFrontendBlockDefinitionSourceId,
		objectPropertyValue,
		propertyDocumentationFromDefinition,
		propertyDocumentationFromProperties
	} from '$lib/studio/blockDefinition';
	import {
		inferMovedObjectId,
		parentObjectId,
		performDboDrop,
		shouldStartInlineRename
	} from '$lib/studio/dnd';
	import FlowViewer from '$lib/studio/flow/FlowViewer.svelte';
	import { contextAuthoringMutation, isFrontendAuthoringNodeId } from '$lib/studio/flowAuthoring';
	import { loadPaletteContext, parentPaletteId } from '$lib/studio/paletteContext';
	import {
		findPrimaryEditorProperty,
		isCodeEditorProperty,
		togglePropertyPickerTarget
	} from '$lib/studio/propertyEditors';
	import {
		decodeStudioSelectionId,
		studioSelectionIdFromUrl,
		studioSelectionUrl
	} from '$lib/studio/routeSelection';
	import { applySourcePickerDrop, sourceDefinitionFromPayload } from '$lib/studio/sourcePickerDnd';
	import StudioAboutDialog from '$lib/studio/StudioAboutDialog.svelte';
	import StudioActivityBar from '$lib/studio/StudioActivityBar.svelte';
	import StudioAssistantPanel from '$lib/studio/StudioAssistantPanel.svelte';
	import StudioBuilderPanel from '$lib/studio/StudioBuilderPanel.svelte';
	import {
		hasStudioClipboard,
		pasteStudioClipboard,
		putInStudioClipboard
	} from '$lib/studio/studioClipboard.svelte.js';
	import StudioDebugPanel from '$lib/studio/StudioDebugPanel.svelte';
	import StudioDeployDialog from '$lib/studio/StudioDeployDialog.svelte';
	import StudioDocPanel from '$lib/studio/StudioDocPanel.svelte';
	import StudioEditorPanel from '$lib/studio/StudioEditorPanel.svelte';
	import StudioEmptyState from '$lib/studio/StudioEmptyState.svelte';
	import StudioExecutionPanel from '$lib/studio/StudioExecutionPanel.svelte';
	import { flowBrowserPreview, flowSourceReveal } from '$lib/studio/studioFlowEvents';
	import StudioHandlersDialog from '$lib/studio/StudioHandlersDialog.svelte';
	import StudioIconButton from '$lib/studio/StudioIconButton.svelte';
	import StudioLogsPanel from '$lib/studio/StudioLogsPanel.svelte';
	import StudioMarketplace from '$lib/studio/StudioMarketplace.svelte';
	import { createStudioMutationEventTracker } from '$lib/studio/studioMutationEvents';
	import StudioNewProjectDialog from '$lib/studio/StudioNewProjectDialog.svelte';
	import StudioPalettePanel from '$lib/studio/StudioPalettePanel.svelte';
	import StudioPanel from '$lib/studio/StudioPanel.svelte';
	import StudioPreviewPanel from '$lib/studio/StudioPreviewPanel.svelte';
	import StudioPropertiesPanel from '$lib/studio/StudioPropertiesPanel.svelte';
	import StudioReferencesPanel from '$lib/studio/StudioReferencesPanel.svelte';
	import StudioSchemaPanel from '$lib/studio/StudioSchemaPanel.svelte';
	import StudioSearchPanel from '$lib/studio/StudioSearchPanel.svelte';
	import StudioSharedComponentDialog from '$lib/studio/StudioSharedComponentDialog.svelte';
	import StudioShell from '$lib/studio/StudioShell.svelte';
	import StudioSourceControlPanel from '$lib/studio/StudioSourceControlPanel.svelte';
	import StudioStatisticsDialog from '$lib/studio/StudioStatisticsDialog.svelte';
	import StudioStepsFromXmlDialog from '$lib/studio/StudioStepsFromXmlDialog.svelte';
	import StudioTabbedFrame from '$lib/studio/StudioTabbedFrame.svelte';
	import StudioTopbar from '$lib/studio/StudioTopbar.svelte';
	import StudioTranslationsDialog from '$lib/studio/StudioTranslationsDialog.svelte';
	import StudioTreePanel from '$lib/studio/StudioTreePanel.svelte';
	import StudioVariablesDialog from '$lib/studio/StudioVariablesDialog.svelte';
	import StudioWsImportDialog from '$lib/studio/StudioWsImportDialog.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { resolve } from '$lib/utils/route';
	import {
		call,
		checkArray,
		getStudioContextMenu,
		getUrl,
		removeDbo,
		runStudioContextAction,
		saveDboProject,
		toaster
	} from '$lib/utils/service';
	import { onMount, untrack } from 'svelte';
	import { SvelteSet } from 'svelte/reactivity';

	/** @typedef {'execution' | 'code' | 'flow' | 'doc'} WorkPanel */
	/** @typedef {'frontend' | 'execution'} VibeResult */
	/** @typedef {'frontend' | 'code' | 'doc'} FrontendResult */
	/**
	 * @typedef {Object} PaletteItem
	 * @property {string=} id
	 * @property {string=} name
	 * @property {string=} classname
	 * @property {string=} description
	 * @property {string=} shortDescriptionHtml
	 * @property {string=} longDescriptionText
	 * @property {string=} longDescriptionHtml
	 * @property {string=} shortDescriptionText
	 * @property {string=} propertiesDescriptionHtml
	 * @property {{ label: string, description: string }[]=} propertyDocumentation
	 * @property {string=} icon
	 * @property {string=} instanceName
	 * @property {string=} flowType
	 * @property {string=} provider
	 * @property {string=} sourceProject
	 * @property {string=} sourceRelativePath
	 * @property {string=} sourceRuntime
	 * @property {boolean=} isBlockDefinition
	 * @property {boolean=} builtin
	 * @property {boolean=} additional
	 */

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
	const localMutationEvents = createStudioMutationEventTracker();
	const SIDE_PANEL_IDS = ['palette', 'properties'];
	/** @type {WorkPanel[]} */
	const WORK_PANEL_IDS = ['execution', 'code', 'flow', 'doc'];
	/** @type {{ id: WorkPanel, label: string, icon: string }[]} */
	const WORK_VIEWS = [
		{ id: 'execution', label: 'Execution', icon: 'mdi:play-circle-outline' },
		{ id: 'code', label: 'Code', icon: 'mdi:code-tags' },
		{ id: 'flow', label: 'Flow', icon: 'mdi:source-branch' },
		{ id: 'doc', label: 'Doc', icon: 'mdi:book-open-variant' }
	];
	/** @type {{ id: VibeResult, label: string, icon: string }[]} */
	const VIBE_RESULT_VIEWS = [
		{ id: 'frontend', label: 'Frontend', icon: 'mdi:smartphone-link' },
		{ id: 'execution', label: 'Execution', icon: 'mdi:play-circle-outline' }
	];
	/** @type {{ id: FrontendResult, label: string, icon: string }[]} */
	const FRONTEND_RESULT_VIEWS = [
		{ id: 'frontend', label: 'Frontend', icon: 'mdi:smartphone-link' },
		{ id: 'code', label: 'Code', icon: 'mdi:code-tags' },
		{ id: 'doc', label: 'Doc', icon: 'mdi:book-open-variant' }
	];
	const VIBE_RESULT_IDS = VIBE_RESULT_VIEWS.map(({ id }) => id);
	const FRONTEND_RESULT_IDS = FRONTEND_RESULT_VIEWS.map(({ id }) => id);

	/**
	 * @typedef {Object} EditorTarget
	 * @property {string} id
	 * @property {string=} propertyName
	 * @property {string=} displayName
	 * @property {string=} editorClass
	 * @property {any=} value
	 * @property {boolean=} persisted whether the value is the one the engine now holds
	 * @property {boolean=} sourceDocument
	 * @property {number=} serial
	 */
	/**
	 * @typedef {Object} SourcePropertyCandidate
	 * @property {string=} name
	 * @property {string=} displayName
	 * @property {string=} kind
	 */
	/**
	 * @typedef {Object} SourceChoice
	 * @property {string} targetId
	 * @property {import('$lib/studio/sourcePickerDnd').SourcePickerDragPayload} payload
	 * @property {SourcePropertyCandidate[]} candidates
	 * @property {boolean} busy
	 * @property {string=} error
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
		},
		{
			id: 'vibe',
			label: 'Vibe',
			icon: 'mdi:robot-outline',
			description: 'Assistant and live result'
		}
	];
	const PROFILE_IDS = profiles.map(({ id }) => id);

	let profile = $state('backend');
	let selectedId = $state(initialSelectedId);
	let activeSidePanel = $state('properties');
	/** @type {WorkPanel} */
	let activeWorkPanel = $state(/** @type {WorkPanel} */ ('execution'));
	/** @type {VibeResult} */
	let activeVibeResult = $state('frontend');
	/** @type {FrontendResult} */
	let activeFrontendResult = $state(/** @type {FrontendResult} */ ('frontend'));
	let frontendDeviceId = $state('none');
	let frontendLandscape = $state(false);
	/** @type {{ projectName: string, url: string, mode: 'production' | 'development' }} */
	let frontendPreview = $state({ projectName: '', url: '', mode: 'production' });
	let frontendPreviewBusy = $state(false);
	let studioReady = $state(false);
	const reconciledFrontendProjects = new SvelteSet();
	/** @type {{ projectName: string, sourcePath: string } | null} */
	let pendingStudioNavigation = null;
	let frontendTheme = $state(
		/** @type {{ projectName: string, context: any }} */ ({
			projectName: '',
			context: null
		})
	);
	/** @type {import('$lib/studio/flowAuthoring').FlowAuthoringReference | null} */
	let frontendAuthoringReference = $state(null);
	/** @type {'browse' | 'select'} */
	let frontendAuthoringMode = $state('browse');
	let frontendAuthoringSerial = 0;
	let logsPanelOpen = $state(false);
	/** @type {EditorTarget | null} */
	let editorTarget = $state(null);
	/** @type {EditorTarget | null} */
	let pickerTarget = $state(null);
	/** @type {{ id: string, propertyName: string, serial: number } | null} */
	let pickerRequest = $state(null);
	/** @type {SourceChoice | null} */
	let sourceChoice = $state(null);
	/** @type {{ request: { id: string, objectType: string, oldName: string, newName: string }, resolve: (update: string | null) => void } | null} */
	let renameChoice = $state(null);
	let newProjectOpen = $state(false);
	let deployProjectName = $state('');
	let wsImportProjectName = $state('');
	/** The transaction whose variables the variables dialog chooses */
	let variablesTargetId = $state('');
	/** The NGX application whose translations files the translations dialog writes */
	let translationsTargetId = $state('');
	/** The sequence or step receiving the steps of the steps from XML dialog */
	let stepsFromXmlTargetId = $state('');
	/** @type {'xml' | 'xsd'} */
	let stepsFromXmlMode = $state('xml');
	/** The NGX component the shared component dialog extracts */
	let sharedComponentTargetId = $state('');
	let handlersTargetId = $state('');
	let statisticsProjectName = $state('');
	let aboutOpen = $state(false);
	/** the lib_* projects hidden from the tree, as the "Toggle libs" of the Eclipse Studio */
	let hideLibs = $state(false);
	let marketplaceOpen = $state(false);
	/** @type {PaletteItem | null} */
	let selectedPaletteItem = $state(null);
	let paletteRevealRequest = $state({ key: '', contextId: '', serial: 0 });
	/** @type {PaletteItem | null} */
	let selectedTreeDocItem = $state(null);
	let selectedTreeDocLoading = $state(false);
	let selectedTreeDocError = $state('');
	let selectedTreeDocRequestKey = '';
	let selectedTreeDocSerial = 0;
	let selectionMetaSerial = 0;
	let urlSyncReady = $state(false);
	let lastRouteSelectionId = initialSelectedId;
	let pendingRouteSelectionId = '';
	let treeRefreshSerial = $state(0);
	let flowRefreshSerial = $state(0);
	let propertiesRefreshSerial = $state(0);
	let renameTargetId = $state('');
	let paletteSelectionContext = initialSelectedId;
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
	let projectChangeRefreshTimer;
	let projectChangeRefreshPending = false;
	let projectChangeRefreshRunning = false;
	let localTreeMutationDepth = 0;
	let projectChangeDeferredByMutation = false;
	let localTreeMutationHandled = false;

	let selectedContext = $derived(parseSelection(selectedId));
	let selectedProjectName = $derived(selectedContext.projectName);
	let selectedProject = $derived(
		Projects.projects.find(({ name }) => name === selectedProjectName) ?? null
	);
	let assistantAgentProfile = $derived(
		selectedProject?.ref?.includes('lib_flow_engine') ? 'flow' : 'generalist'
	);
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
	let showVibe = $derived(profile === 'vibe');
	let frontendPreviewUrl = $derived(
		frontendPreview.projectName === selectedProjectName ? frontendPreview.url : ''
	);
	/** @type {'production' | 'development'} */
	let frontendPreviewMode = $derived(
		frontendPreview.projectName === selectedProjectName && frontendPreview.mode === 'development'
			? 'development'
			: 'production'
	);
	let frontendThemeContext = $derived(
		frontendTheme.projectName === selectedProjectName ? frontendTheme.context : null
	);
	let showFlowOverview = $derived(showStudioWork && flowReady);
	let showPalette = $derived(showStudioWork || profile === 'frontend');
	let sideViews = $derived([
		...(showPalette ? [{ id: 'palette', label: 'Palette', icon: 'mdi:palette-outline' }] : []),
		{ id: 'properties', label: 'Properties', icon: 'mdi:tune-vertical-variant' }
	]);
	let effectiveSidePanel = $derived(
		sideViews.some((item) => item.id === activeSidePanel) ? activeSidePanel : 'properties'
	);
	// the view of the left column, and the view of the bottom panel
	let leftView = $state(/** @type {'projects' | 'search' | 'git'} */ ('projects'));
	let bottomView = $state(
		/** @type {'logs' | 'references' | 'schema' | 'build' | 'debug'} */ ('logs')
	);
	const BOTTOM_VIEWS = [
		{ id: 'logs', label: 'Logs', icon: 'mdi:file-document-box-outline' },
		{ id: 'references', label: 'References', icon: 'mdi:link-variant' },
		{ id: 'schema', label: 'Schema', icon: 'mdi:file-code-outline' },
		{ id: 'build', label: 'Build', icon: 'mdi:wrench' },
		{ id: 'debug', label: 'Debug', icon: 'mdi:bug-outline' }
	];
	let activityItems = $derived([
		{
			id: 'tree',
			label: collapsedPanels.tree || leftView !== 'projects' ? 'Show projects' : 'Hide projects',
			icon: 'mdi:file-tree-outline',
			active: !collapsedPanels.tree && leftView === 'projects'
		},
		{
			id: 'search',
			label: collapsedPanels.tree || leftView !== 'search' ? 'Search' : 'Hide search',
			icon: 'mdi:magnify',
			active: !collapsedPanels.tree && leftView === 'search'
		},
		{
			id: 'git',
			label: collapsedPanels.tree || leftView !== 'git' ? 'Source control' : 'Hide source control',
			icon: 'mdi:source-branch',
			active: !collapsedPanels.tree && leftView === 'git'
		},
		{
			id: 'marketplace',
			label: 'Marketplace',
			icon: 'mdi:store-outline',
			active: marketplaceOpen
		},
		...(showVibe
			? [
					{
						id: 'assistant',
						label: collapsedPanels.tools ? 'Show assistant' : 'Hide assistant',
						icon: 'mdi:robot-outline',
						active: !collapsedPanels.tools
					}
				]
			: sideViews.map((item) => ({
					...item,
					active: !collapsedPanels.tools && effectiveSidePanel === item.id
				})))
	]);
	let activityFooterItems = $derived([
		{
			id: 'logs',
			label: logsPanelOpen ? 'Hide the panel' : 'Show the logs, references and schema',
			icon: 'mdi:dock-bottom',
			active: logsPanelOpen
		},
		{ id: 'about', label: 'About Convertigo', icon: 'mdi:help-circle-outline', active: aboutOpen },
		{ id: 'admin', label: 'Admin console', icon: 'mdi:lock-outline', href: resolve('/admin/') }
	]);
	let activeSideView = $derived(
		sideViews.find((item) => item.id === effectiveSidePanel) ?? sideViews.at(-1)
	);
	let selectedDocItem = $derived(selectedPaletteItem ?? selectedTreeDocItem);
	let selectedDocLoading = $derived(!selectedPaletteItem && selectedTreeDocLoading);
	let selectedDocError = $derived(!selectedPaletteItem ? selectedTreeDocError : '');
	let codeEditorActive = $derived(
		(profile === 'backend' && activeWorkPanel === 'code') ||
			(profile === 'frontend' && activeFrontendResult === 'code')
	);
	let breadcrumbs = $derived(buildBreadcrumb(selectedId));
	let workspaceStyle = $derived(
		[
			`--studio-tree-track:${collapsedPanels.tree ? '0px' : `${layoutSizes.treeWidth}px`}`,
			`--studio-tools-track:${collapsedPanels.tools ? '0px' : `${layoutSizes.toolsWidth}px`}`,
			`--studio-tree-resizer-track:${collapsedPanels.tree ? '0px' : '1px'}`,
			`--studio-tools-resizer-track:${collapsedPanels.tools ? '0px' : '1px'}`,
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
		studioReady = true;
		urlSyncReady = true;
		clearStudioRouteHash();
		const unsubscribeAdminEvents = subscribeAdminEvents(
			['projects.changed', 'admin.resync.required', 'flow.browser.open', 'flow.source.changed'],
			handleAdminEvent
		);
		return () => {
			unsubscribeAdminEvents();
			clearTimeout(projectChangeRefreshTimer);
			localMutationEvents.clear();
		};
	});

	$effect(() => {
		const projectName = selectedProjectName;
		const currentProfile = profile;
		const selection = selectedId;
		if (
			!studioReady ||
			!projectName ||
			!['frontend', 'vibe'].includes(currentProfile) ||
			reconciledFrontendProjects.has(projectName)
		) {
			return;
		}
		reconciledFrontendProjects.add(projectName);
		void reconcileFrontendPreview(projectName, selection);
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
		replaceState(selectionUrl(nextId), page.state);
		lastRouteSelectionId = nextId;
		pendingRouteSelectionId = '';
	});

	$effect(() => {
		const currentSelection = selectedId;
		if (currentSelection === paletteSelectionContext) {
			return;
		}
		paletteSelectionContext = currentSelection;
		selectedPaletteItem = null;
		paletteRevealRequest = {
			key: '',
			contextId: '',
			serial: paletteRevealRequest.serial
		};
		clearSelectedTreeDocumentation();
	});

	$effect(() => {
		const id = selectedId;
		const previewVisible =
			profile === 'frontend' || (profile === 'vibe' && activeVibeResult === 'frontend');
		const serial = ++frontendAuthoringSerial;
		if (!previewVisible || !id || id === 'ROOT') {
			frontendAuthoringReference = null;
			return;
		}
		void loadFrontendAuthoringReference(id, serial);
	});

	$effect(() => {
		const id = selectedId;
		const docVisible =
			(profile === 'backend' && activeWorkPanel === 'doc') ||
			(profile === 'frontend' && activeFrontendResult === 'doc');
		if (!docVisible || selectedPaletteItem) {
			return;
		}
		if (!id || id === 'ROOT') {
			clearSelectedTreeDocumentation();
			return;
		}
		if (selectedTreeDocRequestKey === id) {
			return;
		}
		selectedTreeDocRequestKey = id;
		const serial = ++selectedTreeDocSerial;
		void loadSelectedTreeDocumentation(id, serial);
	});

	/**
	 * @returns {string}
	 */
	function routeSelectionId() {
		if (browser) {
			return studioSelectionIdFromUrl(STUDIO_BASE, new URL(window.location.href));
		}
		return decodeStudioSelectionId(page.params.qname ?? '');
	}

	/**
	 * @param {string} id
	 * @returns {string}
	 */
	function selectionUrl(id) {
		return studioSelectionUrl(STUDIO_BASE, id, browser ? new URL(window.location.href) : page.url);
	}

	/**
	 * @param {ReturnType<typeof import('$lib/admin/adminEvents').parseAdminEvent>} event
	 */
	function handleAdminEvent(event) {
		if (!event) {
			return;
		}
		const browserPreview = flowBrowserPreview(event);
		if (browserPreview) {
			frontendPreview = { ...browserPreview, url: studioPreviewUrl(browserPreview.url) };
			activeVibeResult = 'frontend';
			activeFrontendResult = 'frontend';
			if (profile !== 'vibe') {
				setProfile('frontend');
			}
			if (browserPreview.projectName !== selectedProjectName) {
				scheduleStudioNavigation(browserPreview.projectName);
			}
			return;
		}
		const sourceReveal = flowSourceReveal(event);
		if (sourceReveal) {
			scheduleStudioNavigation(sourceReveal.projectName, sourceReveal.sourcePath);
			return;
		}
		if (!['projects.changed', 'admin.resync.required'].includes(event.topic)) return;
		const eventProject = String(event.payload.project ?? '');
		if (eventProject && eventProject !== selectedProjectName) {
			return;
		}
		if (localMutationEvents.consume(event)) {
			return;
		}
		projectChangeRefreshPending = true;
		clearTimeout(projectChangeRefreshTimer);
		if (localTreeMutationDepth > 0) {
			projectChangeDeferredByMutation = true;
			return;
		}
		projectChangeRefreshTimer = setTimeout(flushProjectChangeRefresh, 120);
	}

	/**
	 * Explicit Flow actions may target a project other than the one currently
	 * selected in Studio. Coalesce its source reveal and viewer-open events so
	 * the more precise source selection wins regardless of event order.
	 *
	 * @param {string} projectName
	 * @param {string=} sourcePath
	 */
	function scheduleStudioNavigation(projectName, sourcePath = '') {
		if (!projectName) {
			return;
		}
		const previousSourcePath =
			pendingStudioNavigation?.projectName === projectName
				? pendingStudioNavigation.sourcePath
				: '';
		pendingStudioNavigation = {
			projectName,
			sourcePath: sourcePath || previousSourcePath
		};
		projectChangeRefreshPending = true;
		clearTimeout(projectChangeRefreshTimer);
		projectChangeRefreshTimer = setTimeout(flushProjectChangeRefresh, 120);
	}

	/**
	 * A local tree mutation already refreshes its exact parents. Its own SSE event
	 * may arrive before the mutation response; defer that event so it cannot tear
	 * down the tree while a precise DnD operation is still completing.
	 *
	 * @param {boolean} busy
	 * @param {boolean=} handled
	 */
	function onStudioMutationBusyChange(busy, handled = false) {
		if (busy) {
			localTreeMutationDepth += 1;
			return;
		}
		localTreeMutationHandled ||= handled;
		localTreeMutationDepth = Math.max(0, localTreeMutationDepth - 1);
		if (localTreeMutationDepth > 0) {
			return;
		}
		if (!projectChangeDeferredByMutation) {
			localTreeMutationHandled = false;
			return;
		}
		projectChangeDeferredByMutation = false;
		clearTimeout(projectChangeRefreshTimer);
		if (localTreeMutationHandled) {
			projectChangeRefreshPending = false;
		} else if (projectChangeRefreshPending) {
			projectChangeRefreshTimer = setTimeout(flushProjectChangeRefresh, 120);
		}
		localTreeMutationHandled = false;
	}

	async function flushProjectChangeRefresh() {
		if (projectChangeRefreshRunning || !projectChangeRefreshPending) {
			return;
		}
		projectChangeRefreshPending = false;
		projectChangeRefreshRunning = true;
		try {
			const navigation = pendingStudioNavigation;
			pendingStudioNavigation = null;
			const projectName = navigation?.projectName || selectedProjectName;
			if (!projectName) {
				return;
			}
			if (!Projects.projects.some((project) => project?.name === projectName)) {
				await Projects.refresh();
			}
			if (projectName !== selectedProjectName) {
				selectedId = projectName;
			}
			await refreshStudioProject(projectName);
			const sourcePath = navigation?.sourcePath || '';
			if (sourcePath) {
				const response = await call('studio.treeview.Authoring', {
					project: projectName,
					sourcePath
				});
				const id = String(response?.id ?? '');
				if (id) selectedId = id;
			}
			propertiesRefreshSerial += 1;
			refreshStudioViews();
		} finally {
			projectChangeRefreshRunning = false;
			if (projectChangeRefreshPending) {
				projectChangeRefreshTimer = setTimeout(flushProjectChangeRefresh, 120);
			}
		}
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
			activeVibeResult,
			activeFrontendResult,
			logsPanelOpen,
			hideLibs,
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
		activeWorkPanel = /** @type {WorkPanel} */ (
			storedChoice(preferences.activeWorkPanel, WORK_PANEL_IDS, activeWorkPanel)
		);
		activeVibeResult = /** @type {VibeResult} */ (
			storedChoice(preferences.activeVibeResult, VIBE_RESULT_IDS, activeVibeResult)
		);
		activeFrontendResult = /** @type {FrontendResult} */ (
			storedChoice(preferences.activeFrontendResult, FRONTEND_RESULT_IDS, activeFrontendResult)
		);
		logsPanelOpen = Boolean(preferences.logsPanelOpen);
		hideLibs = Boolean(preferences.hideLibs);
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

	function clearStudioRouteHash() {
		if (!browser || !page.url.hash) {
			return;
		}
		void goto(selectionUrl(selectedId), {
			replaceState: true,
			noScroll: true,
			keepFocus: true
		});
	}

	function normalizeLayoutPanels() {
		if (profile === 'vibe') {
			activeVibeResult = /** @type {VibeResult} */ (
				storedChoice(activeVibeResult, VIBE_RESULT_IDS, 'frontend')
			);
			return;
		}
		if (profile === 'frontend') {
			if (!SIDE_PANEL_IDS.includes(activeSidePanel)) {
				activeSidePanel = 'properties';
			}
			activeFrontendResult = /** @type {FrontendResult} */ (
				storedChoice(activeFrontendResult, FRONTEND_RESULT_IDS, 'frontend')
			);
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
				// the tools are on the right of the work area: they widen when their left edge moves left
				layoutSizes.toolsWidth = clamp(
					startSizes.toolsWidth - delta,
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
			// the arrow moves the line between the panels, which widens the tools when it goes left
			const towardRight = event.key === 'ArrowRight' ? step : -step;
			resizePanel(target, target === 'tools' ? -towardRight : towardRight);
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
	 * @param {string} id
	 * @param {number} serial
	 */
	async function loadFrontendAuthoringReference(id, serial) {
		const response = await call('studio.treeview.Authoring', { id });
		if (serial !== frontendAuthoringSerial || selectedId !== id) {
			return;
		}
		frontendAuthoringReference = response?.reference ?? null;
	}

	/**
	 * @param {import('$lib/studio/flowAuthoring').FlowAuthoringReference} reference
	 */
	async function selectFrontendAuthoringReference(reference) {
		const response = await call('studio.treeview.Authoring', {
			project: selectedProjectName,
			reference: JSON.stringify(reference)
		});
		const id = String(response?.id ?? '');
		if (!id) {
			return;
		}
		frontendAuthoringReference = response?.reference ?? reference;
		selectedId = id;
		setSidePanel('properties');
	}

	/**
	 * @param {string} id
	 */
	function canShowInFrontend(id) {
		return frontendPreviewMode === 'development' && isFrontendAuthoringNodeId(id);
	}

	/**
	 * Switch to the live development preview and arm the one-shot selector so
	 * the current tree object is both revealed and ready for a touch selection.
	 * @param {string} id
	 */
	function showInFrontend(id) {
		if (!canShowInFrontend(id)) {
			return;
		}
		selectedId = id;
		activeFrontendResult = 'frontend';
		setProfile('frontend');
		frontendAuthoringMode = 'select';
	}

	/** @param {any} item Palette insertion uses the same acceptance and mutation path as a drop. */
	async function addPaletteItem(item) {
		const target = selectedId;
		if (!target) return;
		let handled = false;
		onStudioMutationBusyChange(true);
		try {
			const result = await performDboDrop({
				payload: { type: 'paletteData', data: item, options: {} },
				target,
				position: 'inside',
				dropAction: 'copy'
			});
			if (!result.done)
				throw new Error('This component cannot be added inside the selected object.');
			handled = true;
			await onStudioMutation({ ...result, source: 'palette' });
		} finally {
			onStudioMutationBusyChange(false, handled);
		}
	}

	/**
	 * Route a palette drop from the same-origin development viewer through the
	 * exact tree mutation contract already used by Studio DnD.
	 * @param {{ reference: import('$lib/studio/flowAuthoring').FlowAuthoringReference, position: 'before' | 'inside' | 'after', payload: import('$lib/studio/dnd').DboDragPayload }} request
	 */
	async function dropInFrontend(request) {
		const mapping = await call('studio.treeview.Authoring', {
			project: selectedProjectName,
			reference: JSON.stringify(request.reference)
		});
		const target = String(mapping?.id ?? '');
		if (!target) {
			return;
		}
		let handled = false;
		onStudioMutationBusyChange(true);
		try {
			const result = await performDboDrop({
				payload: request.payload,
				target,
				position: request.position,
				dropAction: 'copy'
			});
			if (result.done) {
				handled = true;
				await onStudioMutation({ ...result, source: 'preview' });
			}
		} finally {
			onStudioMutationBusyChange(false, handled);
		}
	}

	/**
	 * Move one source-backed frontend node through the same precise mutation
	 * contract as tree DnD, after resolving both DOM references back to AST ids.
	 * @param {{ source: import('$lib/studio/flowAuthoring').FlowAuthoringReference, reference: import('$lib/studio/flowAuthoring').FlowAuthoringReference, position: 'before' | 'inside' | 'after' }} request
	 */
	async function moveInFrontend(request) {
		const [sourceMapping, targetMapping] = await Promise.all([
			call('studio.treeview.Authoring', {
				project: selectedProjectName,
				reference: JSON.stringify(request.source)
			}),
			call('studio.treeview.Authoring', {
				project: selectedProjectName,
				reference: JSON.stringify(request.reference)
			})
		]);
		const source = String(sourceMapping?.id ?? '');
		const target = String(targetMapping?.id ?? '');
		if (!source || !target || source === target) {
			return;
		}
		let handled = false;
		onStudioMutationBusyChange(true);
		try {
			const result = await performDboDrop({
				payload: { type: 'treeData', data: { id: source } },
				target,
				position: request.position,
				dropAction: 'move'
			});
			if (result.done) {
				handled = true;
				await onStudioMutation({ ...result, source: 'preview' });
			}
		} finally {
			onStudioMutationBusyChange(false, handled);
		}
	}

	/** @param {{ mode: string, palette: string, tokens: any[] }} context */
	function updateFrontendThemeContext(context) {
		frontendTheme = { projectName: selectedProjectName, context };
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
		if (profile === 'frontend') {
			setFrontendResult('code');
		} else {
			setWorkPanel('code');
		}
	}

	/**
	 * @param {EditorTarget} target
	 */
	function openPropertyPicker(target) {
		if (!target?.id) {
			return;
		}
		pickerTarget = togglePropertyPickerTarget(pickerTarget, target);
		if (!pickerTarget) {
			return;
		}
		selectedId = target.id;
		setSidePanel('properties');
	}

	/**
	 * @param {PaletteItem} item
	 */
	function selectPaletteItem(item) {
		selectedPaletteItem = item;
	}

	function clearSelectedTreeDocumentation() {
		selectedTreeDocSerial += 1;
		selectedTreeDocRequestKey = '';
		selectedTreeDocItem = null;
		selectedTreeDocLoading = false;
		selectedTreeDocError = '';
	}

	/**
	 * @param {string} id
	 * @param {number} serial
	 */
	async function loadSelectedTreeDocumentation(id, serial) {
		selectedTreeDocLoading = true;
		selectedTreeDocError = '';
		try {
			const item = await resolveSelectedTreeDocumentation(id);
			if (serial === selectedTreeDocSerial) {
				selectedTreeDocItem = item;
			}
		} catch (error) {
			if (serial === selectedTreeDocSerial) {
				selectedTreeDocItem = null;
				selectedTreeDocError = String(error instanceof Error ? error.message : error);
			}
		} finally {
			if (serial === selectedTreeDocSerial) {
				selectedTreeDocLoading = false;
			}
		}
	}

	/**
	 * @param {string} id
	 * @returns {Promise<PaletteItem | null>}
	 */
	async function resolveSelectedTreeDocumentation(id) {
		const response = await call('studio.properties.Get', { id });
		const properties = response?.properties ?? {};
		if (isFrontendBlockDefinitionSourceId(id)) {
			return selectedObjectDocFallback(id, properties);
		}
		const flowType = propertyValue(properties, 'Flow type', 'virtualType');
		const javaClass = propertyValue(properties, 'Java class', 'P_JavaClass');
		if (flowType || javaClass) {
			const paletteItem = await findPaletteItem(id, { flowType, javaClass });
			if (paletteItem) {
				return blockDefinitionForInstance(paletteItem, id, properties);
			}
		}
		return selectedObjectDocFallback(id, properties);
	}

	/**
	 * @param {string} id
	 * @param {{ flowType?: string, javaClass?: string }} identity
	 * @returns {Promise<PaletteItem | null>}
	 */
	async function findPaletteItem(id, identity) {
		const parentId = parentPaletteId(id);
		if (!parentId) {
			return null;
		}
		const context = await loadPaletteContext(parentId);
		return findBlockDefinition(context.categories, identity);
	}

	/**
	 * @param {string} id
	 * @param {Record<string, any>} properties
	 * @returns {PaletteItem | null}
	 */
	function selectedObjectDocFallback(id, properties) {
		const flowType = propertyValue(properties, 'Flow type', 'virtualType');
		const name =
			flowTypeDisplayName(flowType) ||
			propertyValue(properties, 'Type') ||
			propertyValue(properties, 'Summary', 'summary') ||
			propertyValue(properties, 'Name') ||
			selectionLabel(id);
		const classname = propertyValue(properties, 'Java class');
		if (!name && !classname) {
			return null;
		}
		const definitionDocumentation = propertyDocumentationFromDefinition(properties);
		const propertyDocumentation = definitionDocumentation.length
			? definitionDocumentation
			: propertyDocumentationFromProperties(properties);
		return {
			id,
			name,
			classname,
			instanceName: propertyValue(properties, 'Summary', 'summary'),
			flowType,
			description: propertyValue(properties, 'Description', 'description'),
			longDescriptionText: propertyValue(properties, 'Long description', 'longDescription'),
			isBlockDefinition: false,
			propertyDocumentation
		};
	}

	/**
	 * @param {Record<string, any>} properties
	 * @param {string} name
	 * @returns {string}
	 */
	function propertyValue(properties, name, technicalName = '') {
		return objectPropertyValue(properties, name, technicalName);
	}

	function canRevealBlockDefinition(id) {
		return isFrontendAuthoringNodeId(id);
	}

	function canRevealInPalette(id) {
		return isFrontendAuthoringNodeId(id);
	}

	async function revealInPalette(id) {
		const item = await resolveSelectedTreeDocumentation(id);
		if (!item?.isBlockDefinition) {
			return;
		}
		selectedTreeDocItem = item;
		selectedPaletteItem = item;
		paletteRevealRequest = {
			key: item.id || item.classname || item.name || '',
			contextId: parentPaletteId(id),
			serial: paletteRevealRequest.serial + 1
		};
		setSidePanel('palette');
	}

	/**
	 * Opens the code of a definition implemented as source (Rhino block, Svelte component).
	 * @param {string} id
	 */
	function openSource(id) {
		selectedId = id;
		setSidePanel('properties');
		editorTarget = { id, sourceDocument: true, serial: Date.now() };
		if (profile === 'frontend') {
			setFrontendResult('code');
		} else {
			setWorkPanel('code');
		}
	}

	/**
	 * @param {string} id
	 * @returns {boolean} whether the tree id is a file of a project, as "Project/path/name.ext"
	 */
	function isProjectFileId(id) {
		return Boolean(id?.includes('/')) && id.slice(id.lastIndexOf('/') + 1).includes('.');
	}

	// a file of a project selected in the tree opens in the code editor, as in the Eclipse Studio
	$effect(() => {
		const id = selectedId;
		if (!isProjectFileId(id) || profile === 'vibe') {
			return;
		}
		untrack(() => {
			if (editorTarget?.id === id && editorTarget?.sourceDocument) {
				return;
			}
			editorTarget = { id, sourceDocument: true, serial: Date.now() };
			if (profile === 'frontend') {
				setFrontendResult('code');
			} else {
				setWorkPanel('code');
			}
		});
	});

	async function revealBlockDefinition(id) {
		const item = await resolveSelectedTreeDocumentation(id);
		const sourceId = blockDefinitionSourceId(item);
		if (!sourceId) {
			return;
		}
		selectedId = sourceId;
		setSidePanel('properties');
		editorTarget = {
			id: sourceId,
			sourceDocument: true,
			serial: Date.now()
		};
		if (profile === 'frontend') {
			setFrontendResult('code');
		} else {
			setWorkPanel('code');
		}
	}

	/**
	 * @param {string} id
	 * @returns {string}
	 */
	function selectionLabel(id) {
		if (id.includes('/')) {
			return id.split('/').filter(Boolean).at(-1) || 'Files';
		}
		const segment = id.split('.').at(-1) ?? id;
		return segment.replace(/^[^:]+:/, '') || id;
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
	 * @param {boolean} followSelection
	 */
	async function onStudioMutation(mutation, followSelection = true) {
		const serial = ++mutationRefreshSerial;
		localMutationEvents.remember(mutation);
		lastStudioMutation = mutation;
		studioMutationSerial += 1;
		const nextSelection = mutation?.selectedId || mutation?.id;
		if (
			mutation?.payload?.type === 'paletteData' &&
			(mutation?.selectionSourcePath || mutation?.projectedSourcePath)
		) {
			// A freshly inserted source-backed widget is immediately editable. Avoid
			// starting a parent-sensitive palette request for the new child while its
			// properties are the user's next interaction.
			activeSidePanel = 'properties';
		}
		if (nextSelection && followSelection) {
			selectedId = nextSelection;
		}
		if (shouldStartInlineRename(mutation) && nextSelection) {
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
				// A mutation can keep the selected id (for example Enable/Disable).
				// Properties must reload even when the selection itself did not change.
				propertiesRefreshSerial += 1;
				// StudioTreePanel refreshes only the affected mutation context; a global tree
				// refresh can collapse expanded branches while rename/reveal is in progress.
				refreshStudioViews({ tree: false, flow: true });
			}
		}
	}

	/**
	 * @param {{ nodeId: string, action: { id?: string }, result: any }} event
	 */
	async function onStudioContextAction(event) {
		const actionId = String(event?.action?.id ?? '');
		const result = event?.result;
		if (actionId.startsWith('object.') && result?.message) {
			// the actions on the objects outside the Flows tell why they did nothing
			(result.ok === false ? toaster.error : toaster.info)({ description: String(result.message) });
		}
		if (result?.ok === false) {
			return;
		}
		const projectName = parseSelection(event?.nodeId ?? '').projectName || selectedProjectName;
		if (
			(actionId === 'frontbuilder.svelte.dev.start' ||
				actionId === 'frontbuilder.svelte.dev.open') &&
			result?.openUrl &&
			projectName
		) {
			frontendPreview = {
				projectName,
				url: studioPreviewUrl(result.openUrl),
				mode: 'development'
			};
		} else if (
			actionId === 'frontbuilder.svelte.dev.stop' ||
			actionId === 'frontbuilder.svelte.build' ||
			actionId === 'frontbuilder.svelte.openBuilt'
		) {
			frontendPreview = { projectName: '', url: '', mode: 'production' };
		}
		if (result?.projects) {
			// another project was loaded, as a referenced project updated from its repository
			await Projects.refresh();
		}
		const mutation = contextAuthoringMutation(result, event.nodeId);
		if (mutation) {
			await onStudioMutation(mutation);
		} else if (result?.refresh) {
			await refreshStudioProject(event.nodeId);
			refreshTreeContext(event.nodeId, 'contextAction');
			refreshStudioViews();
		}
		if (result?.changed) {
			markProjectDirty(event.nodeId);
		}
	}

	/**
	 * @param {'production' | 'development'} mode
	 */
	async function changeFrontendPreviewMode(mode) {
		if (frontendPreviewBusy || !selectedProjectName) {
			return;
		}
		if (mode === 'production') {
			frontendPreview = {
				projectName: selectedProjectName,
				url: '',
				mode: 'production'
			};
			return;
		}
		await openFrontendDevelopmentPreview(selectedProjectName, selectedId, true);
	}

	/**
	 * Restores a running development viewer after a Studio refresh without
	 * starting a new process. A user can still switch back to Prod for the
	 * current Studio session without stopping the server.
	 * @param {string} projectName
	 * @param {string} selection
	 */
	async function reconcileFrontendPreview(projectName, selection) {
		await openFrontendDevelopmentPreview(projectName, selection, false);
	}

	/**
	 * @param {string} projectName
	 * @param {string} selection
	 * @param {boolean} startWhenStopped
	 */
	async function openFrontendDevelopmentPreview(projectName, selection, startWhenStopped) {
		if (frontendPreviewBusy) {
			return false;
		}
		frontendPreviewBusy = true;
		try {
			const target = await findFrontendDevAction(projectName, selection, startWhenStopped);
			if (!target) {
				return false;
			}
			const result = await runStudioContextAction(target.nodeId, target.action);
			await onStudioContextAction({ nodeId: target.nodeId, action: target.action, result });
			return result?.ok !== false;
		} catch (error) {
			console.warn('Unable to reconcile the frontend development preview', error);
			return false;
		} finally {
			frontendPreviewBusy = false;
		}
	}

	/**
	 * @param {string} projectName
	 * @param {string} selection
	 * @param {boolean} startWhenStopped
	 * @returns {Promise<{ nodeId: string, action: any } | null>}
	 */
	async function findFrontendDevAction(projectName, selection, startWhenStopped) {
		for (const nodeId of frontendBuilderCandidates(projectName, selection)) {
			const response = await getStudioContextMenu(nodeId);
			const actions = Array.isArray(response?.menu?.items)
				? response.menu.items
				: Array.isArray(response?.items)
					? response.items
					: [];
			const open = actions.find(
				(action) => action?.id === 'frontbuilder.svelte.dev.open' && action?.enabled !== false
			);
			if (open) {
				return { nodeId, action: open };
			}
			if (startWhenStopped) {
				const start = actions.find(
					(action) => action?.id === 'frontbuilder.svelte.dev.start' && action?.enabled !== false
				);
				if (start) {
					return { nodeId, action: start };
				}
			}
		}
		return null;
	}

	/**
	 * Prefer the builder encoded in the current source selection. The two
	 * conventional fallbacks keep old Flow projects and tests discoverable
	 * when only the project root is selected.
	 * @param {string} projectName
	 * @param {string} selection
	 */
	function frontendBuilderCandidates(projectName, selection) {
		const candidates = [];
		const segments = String(selection ?? '').split('.');
		const frontendsIndex = segments.indexOf('frontends');
		if (frontendsIndex >= 0 && segments[frontendsIndex + 1]) {
			candidates.push(segments.slice(0, frontendsIndex + 2).join('.'));
		}
		for (const candidate of [
			`${projectName}.FlowEngine.frontends.builder_svelte`,
			`${projectName}.Engine.frontends.svelte`
		]) {
			if (!candidates.includes(candidate)) {
				candidates.push(candidate);
			}
		}
		return candidates;
	}

	/**
	 * Gateway tickets belong to the Convertigo origin serving Studio. A backend
	 * action can only know its loopback origin, so keep the capability path while
	 * rebasing it onto the browser-visible origin.
	 * @param {unknown} value
	 */
	function studioPreviewUrl(value) {
		const candidate = String(value ?? '');
		if (!candidate || typeof window === 'undefined') {
			return candidate;
		}
		try {
			const url = new URL(candidate, window.location.href);
			if (url.pathname.includes('/gw/')) {
				return new URL(`${url.pathname}${url.search}${url.hash}`, window.location.origin).href;
			}
		} catch {
			// Keep non-URL action values unchanged for backward compatibility.
		}
		return candidate;
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
	async function refreshAfterPropertySave(id, /** @type {any} */ result = null) {
		if (!id) {
			return;
		}
		if (result?.done) {
			await onStudioMutation(
				{
					...result,
					previousId: result.previousId || id,
					selectedId: result.selectedId || result.id || id,
					target: id,
					source: 'properties',
					payload: { type: 'propertyData', data: { id } }
				},
				selectedId === id
			);
			return;
		}
		await refreshStudioProject(id);
		markProjectDirty(id);
		propertiesRefreshSerial += 1;
		refreshTreeContext(id, 'properties');
		refreshStudioViews({ tree: false, flow: true });
	}

	/**
	 * @param {string} id
	 * @param {string} source
	 */
	function refreshTreeContext(id, source) {
		if (!id) {
			return;
		}
		lastStudioMutation = {
			done: true,
			id,
			selectedId: id,
			target: id,
			source,
			payload: {
				type: 'propertyData',
				data: { id }
			}
		};
		studioMutationSerial += 1;
	}

	/**
	 * @param {string} id
	 * @param {any[]=} sourceDefinition
	 */
	async function refreshAfterPickerApply(id, sourceDefinition) {
		if (
			pickerTarget?.id === id &&
			sourceDefinition &&
			!String(pickerTarget.editorClass ?? '').startsWith('flow-')
		) {
			pickerTarget = {
				...pickerTarget,
				value: sourceDefinition,
				serial: Date.now()
			};
		}
		selectedId = id;
		await refreshAfterPropertySave(id);
	}

	/**
	 * @param {string} targetId
	 * @param {import('$lib/studio/sourcePickerDnd').SourcePickerDragPayload} payload
	 * @param {string=} propertyName
	 */
	async function applySourceDrop(targetId, payload, propertyName = '') {
		if (!targetId || !payload) {
			return;
		}
		selectedId = targetId;
		let response;
		try {
			response = await applySourcePickerDrop(targetId, payload, propertyName);
		} catch (error) {
			sourceChoice = {
				targetId,
				payload,
				candidates: [],
				busy: false,
				error: String(error instanceof Error ? error.message : error)
			};
			return;
		}
		const candidates = checkArray(response?.candidates);
		if (response?.state === 'choice' && candidates.length) {
			sourceChoice = {
				targetId,
				payload,
				candidates,
				busy: false,
				error: ''
			};
			return;
		}
		if (response?.done) {
			sourceChoice = null;
			const sourceDefinition = checkArray(response.sourceDefinition);
			await refreshAfterPickerApply(
				response.id || targetId,
				sourceDefinition.length ? sourceDefinition : sourceDefinitionFromPayload(payload)
			);
			return;
		}
		sourceChoice = {
			targetId,
			payload,
			candidates: [],
			busy: false,
			error: response?.message || 'Unable to apply source on this object'
		};
	}

	/**
	 * @param {SourcePropertyCandidate} candidate
	 */
	async function chooseSourceProperty(candidate) {
		if (!sourceChoice || !candidate?.name) {
			return;
		}
		const choice = sourceChoice;
		sourceChoice = { ...choice, busy: true, error: '' };
		await applySourceDrop(choice.targetId, choice.payload, candidate.name);
	}

	function cancelSourceChoice() {
		sourceChoice = null;
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

	/**
	 * Saves every modified project, as the "Save all" of the Eclipse Studio.
	 */
	async function saveAllProjects() {
		if (!dirtyProjectNames.size || projectActionBusy) {
			return;
		}
		projectActionBusy = 'saveAll';
		try {
			for (const projectName of [...dirtyProjectNames]) {
				await saveDboProject(projectName, projectName);
				await refreshStudioProject(projectName);
				clearProjectDirty(projectName);
			}
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
			// The selected QName can survive Reload while its model has been replaced.
			propertiesRefreshSerial += 1;
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
			activeSidePanel = 'properties';
		}
		if (previousProfile !== profile && profile === 'vibe') {
			collapsedPanels.tools = false;
			layoutSizes.toolsWidth = Math.max(layoutSizes.toolsWidth, 440);
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
	 * @param {WorkPanel} panel
	 */
	function setWorkPanel(panel) {
		if (isWorkPanelDisabled(panel)) {
			return;
		}
		activeWorkPanel = panel;
		persistStudioLayoutPreferences();
	}

	/**
	 * @param {WorkPanel} panel
	 * @returns {boolean}
	 */
	function isWorkPanelDisabled(panel) {
		return panel === 'flow' && !showFlowOverview && activeWorkPanel !== 'flow';
	}

	/**
	 * @param {string} panel
	 */
	function setSidePanel(panel) {
		activeSidePanel = panel;
		persistStudioLayoutPreferences();
	}

	/**
	 * @param {string} id
	 * @returns {boolean} whether the tree id is a database object, not a project or a folder
	 */
	function isTreeObjectId(id) {
		return Boolean(id?.includes('.')) && !isTreeFolderId(id);
	}

	/**
	 * @param {string} id
	 * @returns {boolean}
	 */
	function isTreeFolderId(id) {
		// a folder is `owner:type`, whereas an object is `owner.type:name`
		const folder = id?.match(/^(.*):[a-z]{2,4}$/);
		return Boolean(folder && !/\.[a-z]{2,4}$/.test(folder[1]));
	}

	/**
	 * @param {string} nodeId
	 * @returns {boolean}
	 */
	function canPasteInto(nodeId) {
		return Boolean(nodeId) && hasStudioClipboard();
	}

	/**
	 * Runs an action of the tree on one of its objects, from its menu or its shortcuts, as the tree of
	 * Eclipse does.
	 *
	 * @param {string} action
	 * @param {string} nodeId
	 */
	async function runTreeAction(action, nodeId) {
		const projectName = parseSelection(nodeId).projectName;
		if (action === 'edit.copy' || action === 'edit.cut') {
			if (isTreeObjectId(nodeId)) {
				await putInStudioClipboard(action === 'edit.cut' ? 'cut' : 'copy', [nodeId]);
			}
		} else if (action === 'edit.paste') {
			await pasteIntoTree(nodeId);
		} else if (action === 'object.rename') {
			if (isTreeObjectId(nodeId)) {
				selectedId = nodeId;
				renameTargetId = nodeId;
			}
		} else if (action === 'object.delete') {
			if (isTreeObjectId(nodeId)) {
				await deleteTreeObject(nodeId);
			} else if (!nodeId.includes('.') && !isTreeFolderId(nodeId)) {
				await deleteProject(projectName);
			}
		} else if (action === 'state.enable' || action === 'state.disable') {
			await setTreeObjectEnabled(nodeId, action === 'state.enable');
		} else if (action === 'project.save') {
			await saveSelectedProject();
		} else if (action === 'project.reload') {
			await reloadSelectedProject();
		} else if (action === 'dialog.variables') {
			variablesTargetId = nodeId;
		} else if (action === 'dialog.translations') {
			translationsTargetId = nodeId;
		} else if (action === 'code.class') {
			// the TypeScript class of an NGX component, as the "Edit class" action of the Eclipse Studio
			selectedId = nodeId;
			editorTarget = { id: `${nodeId}#class`, sourceDocument: true, serial: Date.now() };
			if (profile === 'frontend') {
				setFrontendResult('code');
			} else {
				setWorkPanel('code');
			}
		} else if (action.startsWith('picker.')) {
			// the picker of a property, as the "Show source in Picker" action of the Eclipse Studio
			selectedId = nodeId;
			pickerRequest = { id: nodeId, propertyName: action.slice(7), serial: Date.now() };
			setSidePanel('properties');
		} else if (action === 'code.handlers') {
			// the JavaScript handlers of a transaction, as the "Edit handlers" action of the Eclipse Studio
			openPropertyEditor({ id: nodeId, propertyName: 'handlers', displayName: 'Handlers' });
		} else if (action === 'dialog.handlers') {
			handlersTargetId = nodeId;
		} else if (action === 'dialog.sharedComponent') {
			sharedComponentTargetId = nodeId;
		} else if (action === 'dialog.stepsFromXml' || action === 'dialog.stepsFromXsd') {
			stepsFromXmlMode = action === 'dialog.stepsFromXsd' ? 'xsd' : 'xml';
			stepsFromXmlTargetId = nodeId;
		} else if (action === 'project.importWs') {
			wsImportProjectName = projectName;
		} else if (action === 'project.deploy') {
			deployProjectName = projectName;
		} else if (action === 'project.export') {
			await call('projects.Export', { projectName });
		} else if (action === 'project.dashboard') {
			window.open(resolve(`/dashboard/${encodeURIComponent(projectName)}/`), '_blank');
		} else if (action === 'project.swagger') {
			// the REST API of the project in the Swagger console of the engine, as the Eclipse Studio opens it
			const openapi = new URL(
				getUrl(`openapi?YAML&__project=${encodeURIComponent(projectName)}`),
				location.href
			);
			window.open(
				getUrl(`swagger/dist/index.html?url=${encodeURIComponent(openapi.href)}`),
				'_blank'
			);
		} else if (action === 'project.builtApp') {
			window.open(
				getUrl(`projects/${encodeURIComponent(projectName)}/DisplayObjects/mobile/`),
				'_blank'
			);
		} else if (action === 'project.checkReferences') {
			// the projects the projects reference and are missing load, as the "Check remote
			// dependencies" of the Eclipse Studio
			const result = await call('projects.CheckDependencies');
			const loaded = String(result?.admin?.result?.loaded) === 'true';
			await Projects.refresh();
			if (loaded) {
				refreshStudioViews();
				toaster.success({ description: 'The missing referenced projects are loaded.' });
			} else if (result?.admin?.result) {
				toaster.info({ description: 'No referenced project is missing.' });
			}
		} else if (action === 'project.statistics') {
			statisticsProjectName = projectName;
		} else if (action === 'project.readme') {
			await generateReadme(projectName);
		} else if (action === 'project.symbols') {
			const result = await call('studio.project.DeclareSymbols', { projectName });
			const symbols = checkArray(result?.symbols);
			if (symbols.length) {
				toaster.success({ description: `Global symbols declared: ${symbols.join(', ')}.` });
			} else if (result?.symbols) {
				toaster.info({ description: 'The project uses no undeclared global symbol.' });
			}
		} else if (action === 'project.remoteUrl') {
			await copyRemoteUrl(projectName);
		} else if (action === 'project.delete') {
			await deleteProject(projectName);
		}
	}

	/**
	 * Generates the readme.md file of a project, replacing an existing one after a confirmation.
	 * @param {string} projectName
	 */
	async function generateReadme(projectName) {
		let result = await call('studio.project.Readme', { projectName });
		if (result?.exists) {
			if (!window.confirm(`The project ${projectName} already has a readme.md file. Replace it?`)) {
				return;
			}
			result = await call('studio.project.Readme', { projectName, overwrite: 'true' });
		}
		if (result?.done) {
			toaster.success({ description: `${result.file} is generated.` });
			await refreshStudioProject(projectName);
			refreshTreeContext(projectName, 'contextAction');
		} else if (result && 'done' in result) {
			toaster.error({ description: `The readme.md of ${projectName} is not generated.` });
		}
	}

	/**
	 * Copies the remote URL of a project in a Git repository, the one another Studio imports it from.
	 * @param {string} projectName
	 */
	async function copyRemoteUrl(projectName) {
		const result = await call('studio.project.RemoteUrl', { projectName });
		if (!result || !('url' in result)) {
			return;
		}
		if (!result.url) {
			toaster.info({ description: `The project ${projectName} is not in a Git repository.` });
			return;
		}
		try {
			await navigator.clipboard.writeText(result.url);
			toaster.success({ description: `Copied ${result.url}` });
		} catch {
			toaster.error({ description: `The remote URL cannot be copied: ${result.url}` });
		}
	}

	/**
	 * Pastes the Studio clipboard into an object of the tree, or into the object holding a folder.
	 * @param {string} nodeId
	 */
	async function pasteIntoTree(nodeId) {
		const target = isTreeFolderId(nodeId) ? nodeId.replace(/:[a-z]{2,4}$/, '') : nodeId;
		if (!target || !hasStudioClipboard()) {
			return;
		}
		let handled = false;
		onStudioMutationBusyChange(true);
		try {
			const result = await pasteStudioClipboard(target);
			if (!result.ids.length) {
				toaster.error({
					description: result.error || 'The clipboard cannot be pasted inside the selected object.'
				});
				return;
			}
			handled = true;
			const sourceId = result.sourceIds[0] ?? '';
			// a cut object keeps its name under its new parent
			const pastedId =
				result.kind === 'cut'
					? inferMovedObjectId({
							payload: { type: 'treeData', data: { id: sourceId } },
							target,
							position: 'inside'
						})
					: result.ids[0];
			await onStudioMutation({
				done: true,
				id: pastedId,
				selectedId: pastedId,
				target,
				parentId: target,
				previousParentId: result.kind === 'cut' ? parentObjectId(sourceId) : undefined,
				position: 'inside',
				source: 'studio',
				payload: { type: 'treeData', data: { id: sourceId } }
			});
		} finally {
			onStudioMutationBusyChange(false, handled);
		}
	}

	/**
	 * @param {string} id
	 */
	async function deleteTreeObject(id) {
		const name = id.slice(Math.max(id.lastIndexOf('.'), id.lastIndexOf(':')) + 1);
		if (!window.confirm(`Delete "${name}"?\n\nThis action cannot be undone.`)) {
			return;
		}
		const result = await removeDbo(id);
		if (!result?.done) {
			return;
		}
		const parentId = parentObjectId(id);
		await onStudioMutation({
			done: true,
			id: parentId,
			selectedId: parentId,
			target: id,
			position: 'inside',
			source: 'studio',
			payload: { type: 'deleteData', data: { id } }
		});
	}

	/**
	 * @param {string} id
	 * @param {boolean} enabled
	 */
	async function setTreeObjectEnabled(id, enabled) {
		const result = await call('studio.properties.Set', {
			id,
			prop: JSON.stringify({ name: 'isEnabled', value: String(enabled) })
		});
		if (result?.done) {
			await onStudioMutation({
				done: true,
				id,
				selectedId: id,
				target: id,
				position: 'inside',
				source: 'studio',
				payload: { type: 'propertyData', data: { id } }
			});
		}
	}

	/**
	 * @param {string} projectName
	 */
	async function deleteProject(projectName) {
		if (
			!projectName ||
			!window.confirm(
				`Delete the project "${projectName}"?\n\nIts folder is deleted from the workspace: this action cannot be undone.`
			)
		) {
			return;
		}
		const result = await call('projects.Delete', { projectName });
		if (result?.isError) {
			return;
		}
		selectedId = '';
		await Projects.refresh();
		refreshStudioViews();
	}

	/**
	 * Shows the application served by the development build in the frontend preview.
	 * @param {string} url
	 */
	function showDevelopmentBuild(url) {
		if (selectedProjectName) {
			frontendPreview = {
				projectName: selectedProjectName,
				url: studioPreviewUrl(url),
				mode: 'development'
			};
		}
	}

	/**
	 * Shows the transaction whose variables the variables dialog changed.
	 * @param {string} id
	 */
	async function showChangedVariables(id) {
		variablesTargetId = '';
		await refreshStudioProject(id);
		refreshTreeContext(id, 'contextAction');
		refreshStudioViews();
		markProjectDirty(id);
	}

	/**
	 * Shows the connector of a web service imported into a project.
	 * @param {string} id
	 */
	async function showImportedWebService(id) {
		const projectName = wsImportProjectName;
		wsImportProjectName = '';
		await refreshStudioProject(projectName);
		refreshStudioViews();
		markProjectDirty(projectName);
		if (id) {
			selectedId = id;
		}
	}

	/**
	 * Shows a project created or imported by the new project dialog.
	 * @param {string} projectName
	 */
	async function showNewProject(projectName) {
		newProjectOpen = false;
		await Projects.refresh();
		selectedId = projectName;
		refreshStudioViews();
	}

	/**
	 * Asks where to update the references of a renamed object, as Eclipse does.
	 * @param {{ id: string, objectType: string, oldName: string, newName: string }} request
	 * @returns {Promise<string | null>} UPDATE_ALL, UPDATE_LOCAL, UPDATE_NONE, or null to cancel
	 */
	function chooseRenameUpdate(request) {
		return new Promise((resolve) => {
			renameChoice = { request, resolve };
		});
	}

	/**
	 * @param {string | null} update
	 */
	function answerRenameChoice(update) {
		renameChoice?.resolve(update);
		renameChoice = null;
	}

	/**
	 * Shows or hides the panel of an item of the activity bar: a side view shows the tools on its tab,
	 * or hides them when its tab is already visible.
	 *
	 * @param {string} id
	 */
	function selectActivity(id) {
		if (id === 'marketplace') {
			marketplaceOpen = !marketplaceOpen;
		} else if (id === 'about') {
			aboutOpen = !aboutOpen;
		} else if (id === 'tree' || id === 'search' || id === 'git') {
			// the projects, the search and the source control share the left column, as the views of the side bar of Cursor
			const view = id === 'tree' ? 'projects' : id;
			if (collapsedPanels.tree) {
				leftView = view;
				toggleCollapsedPanel('tree');
			} else if (leftView === view) {
				toggleCollapsedPanel('tree');
			} else {
				leftView = view;
			}
		} else if (id === 'logs') {
			setLogsPanelOpen(!logsPanelOpen);
		} else if (id === 'assistant' || (!collapsedPanels.tools && effectiveSidePanel === id)) {
			toggleCollapsedPanel('tools');
		} else {
			if (collapsedPanels.tools) {
				toggleCollapsedPanel('tools');
			}
			setSidePanel(id);
		}
	}

	/**
	 * @param {string} result
	 */
	function setVibeResult(result) {
		activeVibeResult = /** @type {VibeResult} */ (
			storedChoice(result, VIBE_RESULT_IDS, activeVibeResult)
		);
		persistStudioLayoutPreferences();
	}

	/**
	 * @param {string} result
	 */
	function setFrontendResult(result) {
		activeFrontendResult = /** @type {FrontendResult} */ (
			storedChoice(result, FRONTEND_RESULT_IDS, activeFrontendResult)
		);
		persistStudioLayoutPreferences();
	}
</script>

<svelte:head>
	<title>Convertigo Studio</title>
</svelte:head>

{#snippet projectActions()}
	<StudioIconButton
		icon="mdi:folder-plus-outline"
		title="New project"
		ariaLabel="New project"
		onclick={() => (newProjectOpen = true)}
	/>
	<StudioIconButton
		icon={projectActionBusy === 'save' ? 'mdi:sync' : 'mdi:content-save-edit-outline'}
		dirty={selectedProjectDirty}
		title={selectedProjectDirty ? 'Save project - unsaved changes' : 'Save project'}
		ariaLabel={selectedProjectDirty ? 'Save project - unsaved changes' : 'Save project'}
		disabled={!selectedProjectName || Boolean(projectActionBusy)}
		onclick={saveSelectedProject}
	/>
	<StudioIconButton
		icon={projectActionBusy === 'saveAll' ? 'mdi:sync' : 'mdi:content-save-all-outline'}
		dirty={dirtyProjectNames.size > 1}
		title={dirtyProjectNames.size
			? `Save all - ${dirtyProjectNames.size} modified project${dirtyProjectNames.size > 1 ? 's' : ''}`
			: 'Save all'}
		ariaLabel="Save all"
		disabled={!dirtyProjectNames.size || Boolean(projectActionBusy)}
		onclick={saveAllProjects}
	/>
	<StudioIconButton
		icon="mdi:library-outline"
		active={hideLibs}
		title={hideLibs ? 'Show the libraries' : 'Hide the libraries'}
		ariaLabel={hideLibs ? 'Show the libraries' : 'Hide the libraries'}
		onclick={() => {
			hideLibs = !hideLibs;
			persistStudioLayoutPreferences();
		}}
	/>
	<StudioIconButton
		icon={projectActionBusy === 'reload' ? 'mdi:sync' : 'mdi:reload'}
		title="Reload project"
		ariaLabel="Reload project"
		disabled={!selectedProjectName || Boolean(projectActionBusy)}
		onclick={reloadSelectedProject}
	/>
{/snippet}

{#snippet logsToolbarTrail()}
	<StudioIconButton
		icon="mdi:chevron-down"
		title="Hide the panel"
		ariaLabel="Hide the panel"
		size="xs"
		onclick={() => setLogsPanelOpen(false)}
	/>
{/snippet}

{#snippet topbar()}
	<StudioTopbar
		{profile}
		{profiles}
		{collapsedPanels}
		toolsLabel={showVibe ? 'assistant' : 'palette and properties'}
		{breadcrumbs}
		{showFlowOverview}
		onSelectBreadcrumb={selectObject}
		onSetProfile={setProfile}
		onTogglePanel={toggleCollapsedPanel}
		onShowFlow={() => setWorkPanel('flow')}
	/>
{/snippet}

{#snippet activity()}
	<StudioActivityBar
		items={activityItems}
		footerItems={activityFooterItems}
		onSelect={selectActivity}
	/>
{/snippet}

{#snippet tree()}
	{#if leftView === 'search'}
		<StudioPanel
			title="Search"
			icon="mdi:magnify"
			class="studio__tree-panel"
			contentClass="studio__panel-fill"
		>
			<StudioSearchPanel projectName={selectedProjectName} onSelect={selectObject} />
		</StudioPanel>
	{:else if leftView === 'git'}
		<StudioPanel
			title="Source control"
			icon="mdi:source-branch"
			class="studio__tree-panel"
			contentClass="studio__panel-fill"
		>
			<StudioSourceControlPanel
				projectName={selectedProjectName}
				dirty={selectedProjectDirty}
				onPulled={async (name) => {
					// the files changed on disk: the project is loaded again
					await call('projects.Reload', { projectName: name });
					await refreshStudioProject(name);
					refreshStudioViews();
				}}
			/>
		</StudioPanel>
	{/if}
	<StudioPanel
		title="Projects"
		icon="mdi:folder-outline"
		class={['studio__tree-panel', leftView !== 'projects' && 'studio__tree-panel--hidden']
			.filter(Boolean)
			.join(' ')}
		actions={projectActions}
	>
		<StudioTreePanel
			{hideLibs}
			bind:selectedId
			bind:renameTargetId
			refreshSerial={treeRefreshSerial}
			refreshMutation={lastStudioMutation}
			refreshMutationSerial={studioMutationSerial}
			onMutation={onStudioMutation}
			onMutationBusyChange={onStudioMutationBusyChange}
			onContextAction={onStudioContextAction}
			{canShowInFrontend}
			onShowInFrontend={showInFrontend}
			{canRevealInPalette}
			onRevealInPalette={revealInPalette}
			{canRevealBlockDefinition}
			onRevealBlockDefinition={revealBlockDefinition}
			onOpenSource={openSource}
			onSourceDrop={applySourceDrop}
			onTreeAction={runTreeAction}
			{canPasteInto}
			onChooseRenameUpdate={chooseRenameUpdate}
		/>
	</StudioPanel>
{/snippet}

{#snippet executionPane()}
	<StudioExecutionPanel
		projectName={selectedProjectName}
		requestable={executionTarget?.requestable ?? null}
		requestableKind={executionTarget?.kind ?? ''}
		connectorName={executionTarget?.connectorName ?? ''}
	/>
{/snippet}

{#snippet frontendPane()}
	<StudioPreviewPanel
		projectName={selectedProjectName}
		previewUrlOverride={frontendPreviewUrl}
		previewMode={frontendPreviewMode}
		previewModeBusy={frontendPreviewBusy}
		onPreviewModeChange={changeFrontendPreviewMode}
		bind:selectedDeviceId={frontendDeviceId}
		bind:landscape={frontendLandscape}
		showDeviceSelector={false}
		showDeviceDrawer
		bind:authoringMode={frontendAuthoringMode}
		selectedAuthoringReference={frontendAuthoringReference}
		onAuthoringSelect={selectFrontendAuthoringReference}
		onAuthoringDrop={dropInFrontend}
		onAuthoringMove={moveInFrontend}
		onThemeContext={updateFrontendThemeContext}
	/>
{/snippet}

{#snippet codePane()}
	<StudioEditorPanel
		{selectedId}
		{editorTarget}
		active={codeEditorActive}
		onSave={refreshAfterPropertySave}
		onMutationBusyChange={onStudioMutationBusyChange}
		onSelectObject={selectObject}
	/>
{/snippet}

{#snippet flowPane()}
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
			onSourceDrop={applySourceDrop}
		/>
	{:else}
		<StudioEmptyState message="No sequence selected" full />
	{/if}
{/snippet}

{#snippet docPane()}
	<StudioDocPanel
		paletteItem={selectedDocItem}
		loading={selectedDocLoading}
		error={selectedDocError}
		emptyMessage="No documentation available for the current selection."
	/>
{/snippet}

{#snippet main()}
	{#if showStudioWork}
		<StudioTabbedFrame
			items={WORK_VIEWS}
			active={activeWorkPanel}
			ariaLabel="Studio workspace views"
			class="studio-work"
			fillIds={['code', 'flow', 'doc']}
			lazyIds={['flow']}
			isDisabled={(id) => isWorkPanelDisabled(/** @type {WorkPanel} */ (id))}
			onSelect={(id) => setWorkPanel(/** @type {WorkPanel} */ (id))}
			panes={{
				execution: executionPane,
				code: codePane,
				flow: flowPane,
				doc: docPane
			}}
		/>
	{:else if profile === 'frontend'}
		<StudioTabbedFrame
			items={FRONTEND_RESULT_VIEWS}
			active={activeFrontendResult}
			ariaLabel="Frontend workspace views"
			class="studio__primary-panel"
			fillIds={['frontend', 'code', 'doc']}
			onSelect={setFrontendResult}
			panes={{
				frontend: frontendPane,
				code: codePane,
				doc: docPane
			}}
		/>
	{:else}
		<StudioTabbedFrame
			items={VIBE_RESULT_VIEWS}
			active={activeVibeResult}
			ariaLabel="Vibe result views"
			class="studio-vibe-result"
			fillIds={['frontend']}
			onSelect={setVibeResult}
			panes={{
				frontend: frontendPane,
				execution: executionPane
			}}
		/>
	{/if}
{/snippet}

{#snippet palettePane()}
	<StudioPalettePanel
		{selectedId}
		active={effectiveSidePanel === 'palette'}
		{selectedPaletteItem}
		revealRequest={paletteRevealRequest}
		onPaletteItemSelect={selectPaletteItem}
		onPaletteItemAdd={addPaletteItem}
	/>
{/snippet}

{#snippet propertiesPane()}
	<StudioPropertiesPanel
		{selectedId}
		active={effectiveSidePanel === 'properties'}
		refreshSerial={propertiesRefreshSerial}
		onSave={refreshAfterPropertySave}
		onMutationBusyChange={onStudioMutationBusyChange}
		onOpenPropertyEditor={openPropertyEditor}
		onOpenPropertyPicker={openPropertyPicker}
		{pickerTarget}
		{pickerRequest}
		identityItem={selectedTreeDocItem}
		{frontendThemeContext}
		onPickerApply={refreshAfterPickerApply}
	/>
{/snippet}

{#snippet tools()}
	{#if showVibe}
		<StudioPanel
			title="Assistant"
			icon="mdi:robot-outline"
			class="studio__assistant-panel"
			contentClass="studio__panel-fill"
		>
			<StudioAssistantPanel
				projectName={selectedProjectName}
				agentProfile={assistantAgentProfile}
			/>
		</StudioPanel>
	{:else}
		<StudioTabbedFrame
			items={sideViews}
			active={effectiveSidePanel}
			ariaLabel="Studio side views"
			panelLabel={activeSideView?.label ?? 'Side view'}
			fillIds={SIDE_PANEL_IDS}
			onSelect={(id) => setSidePanel(id)}
			panes={{
				palette: palettePane,
				properties: propertiesPane
			}}
		/>
	{/if}
{/snippet}

{#snippet logsPane()}
	<StudioLogsPanel />
{/snippet}

{#snippet referencesPane()}
	<StudioReferencesPanel
		{selectedId}
		active={bottomView === 'references'}
		onSelect={selectObject}
	/>
{/snippet}

{#snippet schemaPane()}
	<StudioSchemaPanel
		{selectedId}
		projectName={selectedProjectName}
		active={bottomView === 'schema'}
	/>
{/snippet}

{#snippet buildPane()}
	<StudioBuilderPanel
		projectName={selectedProjectName}
		active={bottomView === 'build'}
		onLoad={showDevelopmentBuild}
	/>
{/snippet}

{#snippet debugPane()}
	<StudioDebugPanel
		active={bottomView === 'debug'}
		onStopped={() => {
			// a script stopped on a breakpoint: the debugger shows, as the Eclipse Studio shows it
			bottomView = 'debug';
			setLogsPanelOpen(true);
		}}
	/>
{/snippet}

{#snippet logs()}
	<StudioTabbedFrame
		items={BOTTOM_VIEWS}
		active={bottomView}
		ariaLabel="Bottom panel views"
		fillIds={['logs', 'references', 'schema', 'build', 'debug']}
		lazyIds={['references', 'schema', 'build']}
		onSelect={(id) =>
			(bottomView = /** @type {'logs' | 'references' | 'schema' | 'build' | 'debug'} */ (id))}
		panes={{
			logs: logsPane,
			references: referencesPane,
			schema: schemaPane,
			build: buildPane,
			debug: debugPane
		}}
		trail={logsToolbarTrail}
	/>
{/snippet}

<StudioShell
	{profile}
	{collapsedPanels}
	{workspaceStyle}
	{logsPanelOpen}
	onResizeStart={startResize}
	onResizeKey={resizeWithKeyboard}
	onOpenLogs={() => setLogsPanelOpen(true)}
	{topbar}
	{activity}
	{tree}
	{main}
	{tools}
	{logs}
/>

{#if sourceChoice}
	<div class="studio-source-choice" role="presentation" onclick={cancelSourceChoice}>
		<div
			class="studio-source-choice__dialog layout-y-stretch-low"
			role="dialog"
			aria-modal="true"
			aria-labelledby="studio-source-choice-title"
			tabindex="-1"
			onclick={(event) => event.stopPropagation()}
			onkeydown={(event) => event.stopPropagation()}
		>
			<header class="studio-source-choice__header layout-x-between-low">
				<div class="layout-y-none">
					<strong id="studio-source-choice-title">Choose source property</strong>
					<span class="studio-source-choice__target studio-ellipsis">
						{selectionLabel(sourceChoice.targetId)}
					</span>
				</div>
				<StudioIconButton
					icon="mdi:close"
					size="xs"
					title="Close"
					ariaLabel="Close source property choice"
					onclick={cancelSourceChoice}
				/>
			</header>
			{#if sourceChoice.error}
				<p class="studio-source-choice__error">{sourceChoice.error}</p>
			{/if}
			{#if sourceChoice.candidates.length}
				<div class="studio-source-choice__list layout-y-stretch-low">
					{#each sourceChoice.candidates as candidate (candidate.name)}
						<button
							type="button"
							class="studio-source-choice__option"
							disabled={sourceChoice.busy}
							onclick={() => chooseSourceProperty(candidate)}
						>
							<span class="studio-source-choice__option-label studio-ellipsis">
								{candidate.displayName || candidate.name}
							</span>
							<span class="studio-pill">{candidate.kind}</span>
						</button>
					{/each}
				</div>
			{:else}
				<button
					type="button"
					class="button-secondary"
					disabled={sourceChoice.busy}
					onclick={cancelSourceChoice}
				>
					Close
				</button>
			{/if}
		</div>
	</div>
{/if}

{#if marketplaceOpen}
	<StudioMarketplace
		onInstalled={async (projectName) => {
			selectedId = projectName;
			refreshStudioViews();
		}}
		onClose={() => (marketplaceOpen = false)}
	/>
{/if}
{#if aboutOpen}
	<StudioAboutDialog onClose={() => (aboutOpen = false)} />
{/if}
{#if statisticsProjectName}
	<StudioStatisticsDialog
		projectName={statisticsProjectName}
		onClose={() => (statisticsProjectName = '')}
	/>
{/if}
{#if handlersTargetId}
	<StudioHandlersDialog
		id={handlersTargetId}
		onDone={async (id, handlers) => {
			handlersTargetId = '';
			await refreshStudioProject(id);
			markProjectDirty(id);
			propertiesRefreshSerial += 1;
			// the new functions show in the handlers editor, as the Eclipse Studio reloads it
			openPropertyEditor({
				id,
				propertyName: 'handlers',
				displayName: 'Handlers',
				value: handlers,
				persisted: true
			});
		}}
		onClose={() => (handlersTargetId = '')}
	/>
{/if}
{#if sharedComponentTargetId}
	<StudioSharedComponentDialog
		id={sharedComponentTargetId}
		onDone={async (id) => {
			const target = sharedComponentTargetId;
			sharedComponentTargetId = '';
			await refreshStudioProject(target);
			refreshTreeContext(target, 'contextAction');
			markProjectDirty(target);
			selectedId = id;
		}}
		onClose={() => (sharedComponentTargetId = '')}
	/>
{/if}
{#if stepsFromXmlTargetId}
	<StudioStepsFromXmlDialog
		id={stepsFromXmlTargetId}
		mode={stepsFromXmlMode}
		onDone={async (id) => {
			const target = stepsFromXmlTargetId;
			stepsFromXmlTargetId = '';
			await refreshStudioProject(target);
			refreshTreeContext(target, 'contextAction');
			markProjectDirty(target);
			selectedId = id;
		}}
		onClose={() => (stepsFromXmlTargetId = '')}
	/>
{/if}
{#if translationsTargetId}
	<StudioTranslationsDialog
		id={translationsTargetId}
		onDone={async (id) => {
			translationsTargetId = '';
			await refreshStudioProject(id);
			refreshTreeContext(id, 'contextAction');
		}}
		onClose={() => (translationsTargetId = '')}
	/>
{/if}
{#if variablesTargetId}
	<StudioVariablesDialog
		id={variablesTargetId}
		onDone={showChangedVariables}
		onClose={() => (variablesTargetId = '')}
	/>
{/if}
{#if wsImportProjectName}
	<StudioWsImportDialog
		projectName={wsImportProjectName}
		onDone={showImportedWebService}
		onClose={() => (wsImportProjectName = '')}
	/>
{/if}

{#if deployProjectName}
	<StudioDeployDialog projectName={deployProjectName} onClose={() => (deployProjectName = '')} />
{/if}

{#if newProjectOpen}
	<StudioNewProjectDialog onDone={showNewProject} onClose={() => (newProjectOpen = false)} />
{/if}

{#if renameChoice}
	<div class="studio-source-choice" role="presentation" onclick={() => answerRenameChoice(null)}>
		<div
			class="studio-source-choice__dialog layout-y-stretch-low"
			role="dialog"
			aria-modal="true"
			aria-labelledby="studio-rename-choice-title"
			tabindex="-1"
			onclick={(event) => event.stopPropagation()}
			onkeydown={(event) => {
				event.stopPropagation();
				if (event.key === 'Escape') answerRenameChoice(null);
			}}
		>
			<header class="studio-source-choice__header layout-x-between-low">
				<div class="layout-y-none">
					<strong id="studio-rename-choice-title"
						>Update {renameChoice.request.objectType} references</strong
					>
					<span class="studio-source-choice__target">
						Replace '{renameChoice.request.oldName}' by '{renameChoice.request.newName}'
					</span>
				</div>
			</header>
			<div class="studio-source-choice__list layout-y-stretch-low">
				<!-- svelte-ignore a11y_autofocus -->
				<button
					type="button"
					class="studio-source-choice__option"
					autofocus
					onclick={() => answerRenameChoice('UPDATE_ALL')}
				>
					<span class="studio-source-choice__option-label">In all loaded projects</span>
				</button>
				<button
					type="button"
					class="studio-source-choice__option"
					onclick={() => answerRenameChoice('UPDATE_LOCAL')}
				>
					<span class="studio-source-choice__option-label">In the current project only</span>
				</button>
				<button
					type="button"
					class="studio-source-choice__option"
					onclick={() => answerRenameChoice('UPDATE_NONE')}
				>
					<span class="studio-source-choice__option-label">Nowhere, rename only</span>
				</button>
			</div>
		</div>
	</div>
{/if}

<style>
	:global(.studio__primary-panel) {
		height: 100%;
		min-width: 0;
		min-height: 0;
	}

	:global(.studio__tree-panel--hidden) {
		display: none !important;
	}

	:global(.studio__assistant-panel) {
		height: 100%;
		min-width: 0;
		min-height: 0;
	}

	:global(.studio__panel-fill) {
		overflow: hidden;
	}

	:global(.studio-work .flow-dashboard) {
		height: 100%;
		min-height: 0;
		border: 0;
		border-radius: 0;
	}

	.studio-source-choice {
		position: fixed;
		inset: 0;
		z-index: 80;
		display: grid;
		place-items: center;
		background: color-mix(in oklab, var(--color-surface-950-50) 22%, transparent);
		padding: var(--spacing);
	}

	.studio-source-choice__dialog {
		width: min(24rem, 100%);
		max-height: min(30rem, 90vh);
		overflow: auto;
		border: 1px solid var(--color-surface-200-800);
		border-radius: var(--radius-base);
		background: var(--color-surface-50-950);
		box-shadow: var(--shadow-follow);
		padding: var(--spacing);
	}

	.studio-source-choice__header {
		align-items: start;
		color: var(--color-surface-900-100);
	}

	.studio-source-choice__target {
		max-width: 18rem;
		color: var(--color-surface-500-400);
		font-size: 0.72rem;
	}

	.studio-source-choice__error {
		margin: 0;
		border-radius: var(--radius-base);
		background: color-mix(in oklab, var(--color-error-500) 12%, transparent);
		color: var(--color-error-700-300);
		padding: 0.5rem 0.65rem;
		font-size: 0.78rem;
	}

	.studio-source-choice__option {
		display: grid;
		grid-template-columns: minmax(0, 1fr) auto;
		align-items: center;
		gap: var(--spacing);
		border: 1px solid var(--color-surface-200-800);
		border-radius: var(--radius-base);
		background: var(--color-surface-100-900);
		color: var(--color-surface-900-100);
		padding: 0.5rem 0.65rem;
		text-align: start;
	}

	.studio-source-choice__option:hover:not(:disabled) {
		border-color: var(--color-primary-500);
		background: color-mix(in oklab, var(--color-primary-500) 12%, transparent);
		color: var(--color-primary-700-300);
	}

	.studio-source-choice__option:disabled {
		cursor: wait;
		opacity: 0.68;
	}
</style>
