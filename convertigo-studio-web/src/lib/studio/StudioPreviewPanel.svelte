<script>
	import Button from '#lib/admin/components/Button.svelte';
	import MaxRectangle from '#lib/admin/components/MaxRectangle.svelte';
	import Projects from '#lib/common/Projects.svelte.js';
	import { bezelImage, bezelThumbnail } from '#lib/dashboard/bezelAssets.js';
	import Bezels from '#lib/dashboard/Bezels.js';
	import Ico from '#lib/utils/Ico.svelte';
	import { call, getFrontendUrl, toaster } from '#lib/utils/service.js';
	import { tick, untrack } from 'svelte';
	import { fade } from 'svelte/transition';
	import { captureElement } from './elementCapture';
	import {
		authoringActionDoneMessage,
		authoringActionRequest,
		authoringActionsMessage,
		authoringActionsRequest,
		authoringDropRequest,
		authoringModeFromMessage,
		authoringModeMessage,
		authoringMoveDoneMessage,
		authoringMoveRequest,
		highlightAuthoringMessage,
		isFlowAuthoringMessage,
		selectedAuthoringReference as referenceFromAuthoringMessage,
		themeContextFromMessage,
		themeContextRequestMessage
	} from './flowAuthoring';
	import { attachNgxAuthoring } from './ngxAuthoring';
	import { startStyleEditor, styleEditorChanges } from './ngxStyleEditor';
	import {
		addCustomDevice,
		deviceById,
		deviceOsOf,
		previewDevices,
		removeCustomDevice,
		selectDeviceOs,
		withDeviceOs
	} from './previewDevices.svelte.js';
	import { studioActivity } from './studioActivity.svelte.js';
	import StudioCaptureDialog from './StudioCaptureDialog.svelte';
	import StudioDevicePanel from './StudioDevicePanel.svelte';
	import StudioEmptyState from './StudioEmptyState.svelte';
	import { studioPrompt } from './studioPrompt.svelte.js';
	import StudioShareQr from './StudioShareQr.svelte';

	const familyDefinitions = [
		{
			id: 'responsive',
			title: 'Responsive',
			match: (device) => device.id === 'none'
		},
		{
			id: 'phone',
			title: 'Phones',
			match: (device) => device.type === 'phone'
		},
		{
			id: 'tablet',
			title: 'Tablets',
			match: (device) => device.type === 'tablet'
		},
		{
			id: 'desktop',
			title: 'Desktop',
			match: (device) => device.type === 'desktop' && device.id !== 'none'
		}
	];
	const BANNER_MIN_TIME = 900;
	const BANNER_RESULT_TIME = 1600;
	const BANNER_FAILED_TIME = 4000;
	const ZOOM_STEP = 0.15;
	const ZOOM_CHOICES = [0.5, 0.75, 1, 1.25, 1.5, 2];
	const MIN_ZOOM = 0.4;
	const MAX_ZOOM = 2.5;
	const FIT_PADDING = 24;
	const iconButtonClasses = 'button-ico-secondary h-8! w-8! justify-center p-0!';
	const labeledButtonClasses =
		'button-ico-secondary studio-preview__labeled h-8! w-fit! justify-center px-2!';

	/** @type {{ projectName?: string, previewUrlOverride?: string, previewMode?: 'production' | 'development', previewModeBusy?: boolean, onPreviewModeChange?: (mode: 'production' | 'development') => void | Promise<void>, selectedDeviceId?: string, landscape?: boolean, showDeviceSelector?: boolean, showDeviceDrawer?: boolean, authoringMode?: 'browse' | 'select' | 'move', selectedAuthoringReference?: import('./flowAuthoring').FlowAuthoringReference | null, onAuthoringSelect?: (reference: import('./flowAuthoring').FlowAuthoringReference) => void | Promise<void>, onAuthoringDrop?: (request: { reference: import('./flowAuthoring').FlowAuthoringReference, position: 'before' | 'inside' | 'after', payload: any }) => void | Promise<void>, onAuthoringMove?: (request: { source: import('./flowAuthoring').FlowAuthoringReference, reference: import('./flowAuthoring').FlowAuthoringReference, position: 'before' | 'inside' | 'after' }) => void | Promise<void>, onAuthoringActions?: (reference: import('./flowAuthoring').FlowAuthoringReference) => import('./flowAuthoring').FlowAuthoringAction[] | Promise<import('./flowAuthoring').FlowAuthoringAction[]>, onAuthoringAction?: (request: { reference: import('./flowAuthoring').FlowAuthoringReference, action: string }) => void | Promise<void>, onThemeContext?: (context: { mode: string, palette: string, tokens: any[] }) => void, reloadSerial?: number, onNgxStyleChanges?: (changes: any) => void | Promise<void>, ngxReference?: { id: string, classes: string[], segment: string } | null, onNgxSelect?: (priority: string) => void | Promise<void>, onNgxDrop?: (request: { priority: string, position: 'before' | 'inside' | 'after' }) => void | Promise<void>, ngxCanDrop?: () => boolean, onNgxDragStart?: (priority: string) => void, onNgxDragEnd?: () => void, activity?: { phase: string, progress: number, result?: string, serial?: number } }} */
	let {
		projectName = '',
		previewUrlOverride = '',
		previewMode = 'production',
		previewModeBusy = false,
		onPreviewModeChange,
		selectedDeviceId = $bindable('none'),
		landscape = $bindable(false),
		showDeviceSelector = true,
		showDeviceDrawer = false,
		authoringMode = $bindable('browse'),
		selectedAuthoringReference = null,
		onAuthoringSelect,
		onAuthoringDrop,
		onAuthoringMove,
		onAuthoringActions,
		onAuthoringAction,
		onThemeContext,
		reloadSerial = 0,
		onNgxStyleChanges,
		ngxReference = null,
		onNgxSelect,
		onNgxDrop,
		ngxCanDrop,
		onNgxDragStart,
		onNgxDragEnd,
		activity = { phase: '', progress: -1, result: '', serial: 0 }
	} = $props();

	/** @type {HTMLIFrameElement | undefined} */
	let iframe = $state();
	/** what the builder of the application does, shown over the preview as the Eclipse Studio shows it */
	let activityLabel = $derived(
		activity?.phase === 'installing'
			? 'Installing the packages of the application…'
			: activity?.phase === 'building'
				? `Building the application…${activity.progress > 0 && activity.progress < 100 ? ` ${activity.progress}%` : ''}`
				: ''
	);
	/**
	 * The banner of the preview, as a toast: a build shows long enough to be read, even the one of the
	 * development server after a change, which takes a fraction of a second, then tells how it ended.
	 */
	let banner = $state({ label: '', tone: 'busy', progress: -1 });
	let bannerShownAt = 0;
	/** @type {ReturnType<typeof setTimeout> | undefined} */
	let bannerTimer;
	let bannerSerial = untrack(() => activity?.serial ?? 0);
	/** how the last build ended, told once the banner of the build was read */
	let bannerResult = '';

	$effect(() => {
		const label = activityLabel;
		const progress = activity?.progress ?? -1;
		const serial = activity?.serial ?? 0;
		const result = activity?.result ?? '';
		untrack(() => updateBanner(label, progress, serial, result));
	});

	$effect(() => () => clearTimeout(bannerTimer));

	/**
	 * @param {string} label what the builder does, empty when it ended
	 * @param {number} progress
	 * @param {number} serial the count of the builds ended
	 * @param {string} result how the last one ended
	 */
	function updateBanner(label, progress, serial, result) {
		if (serial !== bannerSerial) {
			bannerSerial = serial;
			bannerResult = result;
		}
		if (label) {
			clearTimeout(bannerTimer);
			if (banner.tone !== 'busy' || !banner.label) {
				bannerShownAt = Date.now();
			}
			banner = { label, tone: 'busy', progress };
			return;
		}
		if (banner.label && banner.tone !== 'busy' && !bannerResult) {
			// the end of a build shows until its time is over
			return;
		}
		clearTimeout(bannerTimer);
		const tell = () => {
			const ended = bannerResult;
			bannerResult = '';
			if (!ended) {
				banner = { label: '', tone: 'busy', progress: -1 };
				return;
			}
			const failed = ended === 'failed';
			banner = {
				label: failed ? 'The build failed, see the Build view' : 'Application updated',
				tone: failed ? 'failed' : 'success',
				progress: -1
			};
			bannerTimer = setTimeout(
				() => (banner = { label: '', tone: 'busy', progress: -1 }),
				failed ? BANNER_FAILED_TIME : BANNER_RESULT_TIME
			);
		};
		if (banner.label && banner.tone === 'busy') {
			bannerTimer = setTimeout(tell, Math.max(0, bannerShownAt + BANNER_MIN_TIME - Date.now()));
		} else {
			tell();
		}
	}
	let clientHeight = $state(0);
	let clientWidth = $state(0);
	let addressOverride = $state({ base: '', value: '' });
	let iframeOverride = $state({ base: '', value: '' });
	let zoomOverride = $state({ base: '', value: 1 });
	let zoomModeOverride = $state({ base: '', value: 'fit' });
	let deviceDrawerOpen = $state(false);
	let authoringReadyUrl = $state('');
	let authoringReadySerial = $state(0);
	/** a project without an application, as a library of sequences, has nothing to preview */
	let noFrontend = $derived(
		!previewUrlOverride &&
			Projects.projects.find((project) => project?.name === projectName)?.hasFrontend === 'false'
	);
	let previewUrl = $derived(
		previewUrlOverride || (projectName && !noFrontend ? getFrontendUrl(projectName) : '')
	);
	let addressBar = $derived(
		addressOverride.base === previewUrl ? addressOverride.value : previewUrl
	);
	let iframeUrl = $derived(iframeOverride.base === previewUrl ? iframeOverride.value : previewUrl);
	/** what the preview of each project keeps, as the application editor of the Eclipse Studio */
	let projectPreview = $state(
		/** @type {{ device?: string, landscape?: boolean, zoom?: number, zoomMode?: string, dataset?: string }} */ ({})
	);
	let zoom = $derived(
		zoomOverride.base === previewUrl ? zoomOverride.value : (projectPreview.zoom ?? 1)
	);
	let zoomMode = $derived(
		zoomModeOverride.base === previewUrl
			? zoomModeOverride.value
			: (projectPreview.zoomMode ?? 'fit')
	);
	let trimmedAddress = $derived(addressBar.trim());
	/** an address typed and not loaded yet, which the ↵ button of the field loads as Enter */
	let addressEdited = $derived(
		Boolean(trimmedAddress) && trimmedAddress !== '#' && trimmedAddress !== iframeUrl
	);
	let moreOpen = $state(false);
	/** the desktop Studio, which opens the developer tools of an application in a window of its own */
	const desktopStudio =
		typeof window !== 'undefined' && Boolean(/** @type {any} */ (window).convertigoStudio);
	let barHeight = $state(0);
	let deviceGroups = $derived.by(buildDeviceGroups);
	let selectedDevice = $derived(deviceById(selectedDeviceId));
	let effectiveOs = $derived(deviceOsOf(selectedDevice));
	let frameUrl = $derived(withDeviceOs(iframeUrl, effectiveOs));
	let isResponsivePreview = $derived(selectedDevice.id === 'none');
	let deviceViewportWidth = $derived(getDeviceMetric(selectedDevice.iframe?.width, 1280));
	let deviceViewportHeight = $derived(getDeviceMetric(selectedDevice.iframe?.height, 800));
	let viewportWidth = $derived(
		isResponsivePreview ? 0 : landscape ? deviceViewportHeight : deviceViewportWidth
	);
	let viewportHeight = $derived(
		isResponsivePreview ? 0 : landscape ? deviceViewportWidth : deviceViewportHeight
	);
	let isFramedDevice = $derived(
		!isResponsivePreview && !landscape && Boolean(selectedDevice.bezel)
	);
	let frameWidth = $derived(
		isFramedDevice ? getDeviceMetric(selectedDevice.bezel?.width, viewportWidth) : viewportWidth
	);
	let frameHeight = $derived(
		isFramedDevice ? getDeviceMetric(selectedDevice.bezel?.height, viewportHeight) : viewportHeight
	);
	let fitScale = $derived.by(() => {
		if (isResponsivePreview) {
			return 1;
		}
		if (!clientWidth || !clientHeight || !frameWidth || !frameHeight) {
			return 0.45;
		}
		const availableWidth = Math.max(1, clientWidth - FIT_PADDING);
		const availableHeight = Math.max(1, clientHeight - FIT_PADDING);
		return Math.max(0.08, Math.min(1, availableWidth / frameWidth, availableHeight / frameHeight));
	});
	let appliedScale = $derived(zoomMode === 'fit' ? fitScale : fitScale * zoom);
	let viewportLabel = $derived(
		isResponsivePreview
			? 'Responsive'
			: `${selectedDevice.title}${landscape ? ' landscape' : ''} - ${viewportWidth}x${viewportHeight}`
	);
	/** the zooms the toolbar offers, with the current one when the zoom buttons reached another */
	let zoomChoices = $derived(
		[...new Set([...ZOOM_CHOICES, ...(zoomMode === 'fit' ? [] : [zoom])])].sort((a, b) => a - b)
	);
	let deviceChipLabel = $derived(
		isResponsivePreview
			? 'Responsive'
			: `${selectedDevice.title} · ${viewportWidth}×${viewportHeight}`
	);
	let deviceIcon = $derived(
		isResponsivePreview
			? 'mdi:devices'
			: selectedDevice.type === 'tablet'
				? 'mdi:tablet'
				: selectedDevice.type === 'desktop'
					? 'mdi:monitor'
					: 'mdi:cellphone'
	);
	let previewStyle = $derived(
		[
			barHeight ? `--studio-preview-bar-height:${barHeight}px` : '',
			`--studio-preview-scale:${appliedScale}`,
			frameWidth ? `--studio-preview-width:${frameWidth}px` : '',
			frameHeight ? `--studio-preview-height:${frameHeight}px` : '',
			frameWidth ? `--studio-preview-render-width:${Math.round(frameWidth * appliedScale)}px` : '',
			frameHeight
				? `--studio-preview-render-height:${Math.round(frameHeight * appliedScale)}px`
				: '',
			viewportWidth ? `--studio-preview-frame-width:${viewportWidth}px` : '',
			viewportHeight ? `--studio-preview-frame-height:${viewportHeight}px` : '',
			isFramedDevice
				? `--studio-preview-frame-top:${getDeviceMetric(selectedDevice.iframe?.marginTop, 0)}px`
				: '',
			isFramedDevice
				? `--studio-preview-frame-left:${getDeviceMetric(selectedDevice.iframe?.marginLeft, 0)}px`
				: '',
			isFramedDevice
				? `--studio-preview-frame-radius:${getDeviceMetric(selectedDevice.iframe?.borderRadius, 0)}px`
				: ''
		]
			.filter(Boolean)
			.join(';')
	);
	let authoringHighlightMessage = $derived(highlightAuthoringMessage(selectedAuthoringReference));
	let authoringModeSetMessage = $derived(authoringModeMessage(authoringMode));

	$effect(() => {
		const target = iframe?.contentWindow;
		const readySerial = authoringReadySerial;
		if (!readySerial || authoringReadyUrl !== iframeUrl || !target) {
			return;
		}
		target.postMessage(authoringModeSetMessage, window.location.origin);
		target.postMessage(authoringHighlightMessage, window.location.origin);
	});
	function applyAddressBar() {
		if (!trimmedAddress || trimmedAddress === '#') {
			return;
		}
		iframeOverride = { base: previewUrl, value: trimmedAddress };
		addressOverride = { base: previewUrl, value: trimmedAddress };
	}

	/** the authoring of the NGX application the preview shows */
	let ngxAuthoring = $state(/** @type {ReturnType<typeof attachNgxAuthoring> | null} */ (null));
	let ngxSelecting = $state(false);
	let ngxShowGrids = $state(false);
	/** the highlight of the selected component hidden, as the Remove highlight of the Eclipse Studio */
	let ngxHighlightHidden = $state(false);

	/**
	 * Removes the highlight of the selected component, which shows again with the next one selected.
	 * @param {boolean} leaveSelecting whether the select mode ends too, as with Escape
	 */
	function deselectNgxComponent(leaveSelecting) {
		ngxHighlightHidden = true;
		if (leaveSelecting) {
			ngxSelecting = false;
		}
	}

	/**
	 * Escape ends the select mode of the preview, as it does in the application.
	 * @param {KeyboardEvent} event
	 */
	function handlePreviewEscape(event) {
		const target = /** @type {HTMLElement | null} */ (
			event.target instanceof HTMLElement ? event.target : null
		);
		if (
			event.key !== 'Escape' ||
			event.defaultPrevented ||
			!ngxSelecting ||
			target?.closest('input, textarea, select, [role="dialog"], [role="menu"]')
		) {
			return;
		}
		deselectNgxComponent(true);
	}
	/** the style editor of the NGX application, GrapesJS in the preview */
	let ngxStyleEditing = $state(false);

	async function toggleNgxStyleEditor() {
		const win = iframe?.contentWindow;
		if (!win) {
			return;
		}
		if (!ngxStyleEditing) {
			ngxSelecting = false;
			ngxShowGrids = false;
			ngxAuthoring?.highlight([]);
			const projectUrl = getFrontendUrl(projectName).replace(/\/DisplayObjects\/.*$/, '');
			startStyleEditor(
				win,
				projectUrl,
				isResponsivePreview ? 'desktop' : landscape ? 'mobileLandscape' : 'mobilePortrait'
			);
			ngxStyleEditing = true;
			return;
		}
		const changes = styleEditorChanges(win);
		ngxStyleEditing = false;
		if (changes) {
			await onNgxStyleChanges?.(changes);
		}
		reloadIframe();
	}
	/** the datasets of the NGX application, its recorded session data */
	let ngxDatasets = $state(/** @type {string[]} */ ([]));
	let ngxDataset = $state('none');
	/** the data of the dataset chosen, which the application takes again each time it loads */
	let ngxDatasetData = '';
	/** the project whose preview settings are restored */
	let restoredProject = '';

	/**
	 * @param {string} project
	 */
	function previewKey(project) {
		return `convertigo.studio.preview.${project}`;
	}

	$effect(() => {
		// the device, the zoom and the dataset of the preview of the project, as it left them
		const project = projectName;
		if (!project || project === restoredProject) {
			return;
		}
		restoredProject = project;
		untrack(() => {
			let saved = {};
			try {
				saved = JSON.parse(localStorage.getItem(previewKey(project)) ?? '{}') ?? {};
			} catch {
				saved = {};
			}
			projectPreview = saved;
			if (typeof saved.device === 'string' && deviceById(saved.device).id === saved.device) {
				selectedDeviceId = saved.device;
				landscape = Boolean(saved.landscape);
			}
			ngxDataset = 'none';
			ngxDatasetData = '';
		});
	});

	/**
	 * Keeps a setting of the preview of the project.
	 * @param {Record<string, any>} settings
	 */
	function keepPreview(settings) {
		if (!projectName) {
			return;
		}
		projectPreview = { ...projectPreview, ...settings };
		try {
			localStorage.setItem(previewKey(projectName), JSON.stringify(projectPreview));
		} catch {
			// the settings are not kept
		}
	}

	$effect(() => {
		// the device chosen for the project
		const device = selectedDeviceId;
		const turned = landscape;
		untrack(() => {
			if (
				projectName === restoredProject &&
				(projectPreview.device !== device || Boolean(projectPreview.landscape) !== turned)
			) {
				keepPreview({ device, landscape: turned });
			}
		});
	});
	const SESSION_DATA = '_c8ocafsession_storage_data';
	const SAVE_DATASET = '__save';
	const REMOVE_DATASET = '__remove';
	try {
		// the mobile builder mode of the Convertigo Angular Framework, which records the session data of an
		// NGX application, set before the preview loads it
		sessionStorage.setItem('_c8ocafsession_storage_mode', 'session');
	} catch {
		// no session storage
	}

	$effect(() => {
		// the application records its session data in the session storage it shares with the Studio, as in
		// the application editor of the Eclipse Studio
		if (ngxAuthoring && projectName) {
			untrack(() => void loadNgxDatasets(projectName));
		}
	});

	/**
	 * @param {string} project
	 */
	async function loadNgxDatasets(project) {
		const result = await call('studio.ngxbuilder.Datasets', { project });
		if (Array.isArray(result?.datasets)) {
			ngxDatasets = result.datasets.map(String);
			// the dataset the project had, applied again
			const saved = projectPreview.dataset;
			if (project === projectName && saved && ngxDataset !== saved && ngxDatasets.includes(saved)) {
				await applyNgxDataset(saved);
			}
		}
	}

	/**
	 * Restores a dataset in the application, which reloads with it.
	 * @param {string} name
	 */
	async function applyNgxDataset(name) {
		ngxDataset = name;
		keepPreview({ dataset: name === 'none' ? '' : name });
		if (name === 'none') {
			ngxDatasetData = '';
			sessionStorage.removeItem(SESSION_DATA);
		} else {
			const result = await call('studio.ngxbuilder.Datasets', {
				project: projectName,
				action: 'get',
				name
			});
			ngxDatasetData = String(result?.data ?? '[]');
			sessionStorage.setItem(SESSION_DATA, ngxDatasetData);
		}
		reloadIframe();
	}

	/**
	 * The page of the application leaves, as when it is built again: the edits of the style editor, which
	 * would be lost with it, are applied to the application.
	 * @param {Window} win
	 */
	function leavePreviewPage(win) {
		reapplyNgxDataset();
		if (!ngxStyleEditing) {
			return;
		}
		const changes = styleEditorChanges(win);
		ngxStyleEditing = false;
		if (changes) {
			void onNgxStyleChanges?.(changes);
		}
	}

	/**
	 * The application takes the dataset chosen again each time it loads, as when it is built again: the
	 * session data it recorded meanwhile give way.
	 */
	function reapplyNgxDataset() {
		if (ngxDataset !== 'none' && ngxDatasetData) {
			try {
				sessionStorage.setItem(SESSION_DATA, ngxDatasetData);
			} catch {
				// no session storage
			}
		}
	}

	async function removeNgxDataset() {
		const name = ngxDataset;
		if (name === 'none' || !window.confirm(`Remove the dataset ${name}?`)) {
			return;
		}
		const result = await call('studio.ngxbuilder.Datasets', {
			project: projectName,
			action: 'remove',
			name
		});
		if (result?.done) {
			ngxDataset = 'none';
			ngxDatasetData = '';
			keepPreview({ dataset: '' });
			sessionStorage.removeItem(SESSION_DATA);
		}
		// the list shows the datasets on the disk, even when the removal failed
		await loadNgxDatasets(projectName);
	}

	/**
	 * The dataset menu restores a dataset, or saves or removes one.
	 * @param {Event} event
	 */
	function chooseNgxDataset(event) {
		const select = /** @type {HTMLSelectElement} */ (event.currentTarget);
		const value = select.value;
		if (value === SAVE_DATASET || value === REMOVE_DATASET) {
			select.value = ngxDataset;
			void (value === SAVE_DATASET ? saveNgxDataset() : removeNgxDataset());
		} else {
			void applyNgxDataset(value);
		}
	}

	async function saveNgxDataset() {
		const data = sessionStorage.getItem(SESSION_DATA);
		if (!data) {
			window.alert('The application recorded no session data yet.');
			return;
		}
		const name = (
			await studioPrompt('Name of the dataset', ngxDataset === 'none' ? '' : ngxDataset)
		)?.trim();
		if (!name || name === 'none') {
			return;
		}
		const result = await call('studio.ngxbuilder.Datasets', {
			project: projectName,
			action: 'save',
			name,
			data
		});
		if (result?.done) {
			ngxDataset = name;
			ngxDatasetData = data;
			keepPreview({ dataset: name });
			await loadNgxDatasets(projectName);
		}
	}
	/** the segment of the page the preview was sent to, for a component it did not show */
	let ngxNavigatedFor = '';

	/**
	 * Follows the document the preview loaded when it shows an NGX application.
	 */
	function attachNgxDocument() {
		ngxAuthoring?.destroy();
		ngxAuthoring = null;
		let doc;
		try {
			doc = iframe?.contentDocument;
			// the page of the application previewed, which a tutorial may wait for
			studioActivity.previewProject = projectName;
			studioActivity.previewUrl = String(iframe?.contentWindow?.location.href ?? '');
		} catch {
			return;
		}
		if (!doc) {
			return;
		}
		// the page of the application leaves: its style edits are applied, the next one starts with the dataset
		const win = iframe?.contentWindow;
		win?.addEventListener('pagehide', () => leavePreviewPage(win));
		let tries = 0;
		const wait = () => {
			// the application creates its ion-app once bootstrapped
			if (iframe?.contentDocument !== doc) {
				return;
			}
			if (doc.querySelector('ion-app')) {
				ngxAuthoring = attachNgxAuthoring(doc, {
					onSelect: (priority) => {
						// the component picked again shows its highlight
						ngxHighlightHidden = false;
						void onNgxSelect?.(priority);
					},
					onDeselect: (leaveSelecting) => deselectNgxComponent(leaveSelecting),
					onDrop: (request) => void onNgxDrop?.(request),
					canDrop: () => ngxCanDrop?.() ?? false,
					onDragStart: (priority) => onNgxDragStart?.(priority),
					onDragEnd: () => onNgxDragEnd?.()
				});
				ngxAuthoring.setSelecting(untrack(() => ngxSelecting));
			} else if (tries++ < 40) {
				setTimeout(wait, 250);
			}
		};
		wait();
	}

	$effect(() => {
		ngxAuthoring?.setSelecting(ngxSelecting);
	});

	$effect(() => {
		ngxAuthoring?.setShowGrids(ngxShowGrids);
	});

	$effect(() => {
		// another component selected shows its highlight again
		void ngxReference?.id;
		untrack(() => (ngxHighlightHidden = false));
	});

	$effect(() => {
		const authoring = ngxAuthoring;
		const reference = ngxReference;
		const hidden = ngxHighlightHidden;
		if (!authoring) {
			return;
		}
		untrack(() => {
			const shown = authoring.highlight(hidden ? [] : (reference?.classes ?? []));
			if (hidden) {
				return;
			}
			if (shown || !reference?.segment || ngxNavigatedFor === reference.id) {
				return;
			}
			// the application may still be showing its page: the one of the component opens only if it
			// does not show the component soon
			setTimeout(() => {
				if (
					ngxAuthoring !== authoring ||
					ngxReference !== reference ||
					ngxNavigatedFor === reference.id ||
					authoring.highlight(reference.classes)
				) {
					return;
				}
				ngxNavigatedFor = reference.id;
				try {
					const win = /** @type {any} */ (iframe?.contentWindow);
					if (typeof win?._c8o_changePage === 'function') {
						win._c8o_changePage(reference.segment);
					} else if (win) {
						win.location.href = new URL(reference.segment, win.document.baseURI).href;
					}
				} catch (error) {
					console.warn('Unable to open the page of the component', error);
				}
			}, 1000);
		});
	});

	let reloadedSerial = untrack(() => reloadSerial);
	$effect(() => {
		// a new build of the shown application, as a local build in DisplayObjects/mobile
		if (reloadSerial !== reloadedSerial) {
			reloadedSerial = reloadSerial;
			untrack(reloadIframe);
		}
	});

	function reloadIframe() {
		reapplyNgxDataset();
		try {
			iframe?.contentWindow?.location?.reload();
		} catch (error) {
			console.warn('Unable to reload iframe', error);
		}
	}

	/**
	 * @param {'production' | 'development'} mode
	 */
	function choosePreviewMode(mode) {
		if (previewModeBusy || mode === previewMode) {
			return;
		}
		void onPreviewModeChange?.(mode);
	}

	function navigateBack() {
		try {
			iframe?.contentWindow?.history?.back();
		} catch (error) {
			console.warn('Unable to navigate iframe back', error);
		}
	}

	function navigateForward() {
		try {
			iframe?.contentWindow?.history?.forward();
		} catch (error) {
			console.warn('Unable to navigate iframe forward', error);
		}
	}

	/**
	 * @param {number} direction
	 */
	function adjustZoom(direction) {
		const currentZoom = zoomMode === 'fit' ? 1 : zoom;
		setZoom(
			Math.max(
				MIN_ZOOM,
				Math.min(MAX_ZOOM, Math.round((currentZoom + direction * ZOOM_STEP) * 100) / 100)
			)
		);
	}

	/**
	 * @param {number} value
	 */
	function setZoom(value) {
		zoomModeOverride = { base: previewUrl, value: 'manual' };
		zoomOverride = { base: previewUrl, value };
		keepPreview({ zoom: value, zoomMode: 'manual' });
	}

	/**
	 * @param {Event} event the zoom menu, whose value is fit or a zoom
	 */
	function chooseZoom(event) {
		const value = /** @type {HTMLSelectElement} */ (event.currentTarget).value;
		if (value === 'fit') {
			fitPreview();
		} else {
			setZoom(Number(value));
		}
	}

	function fitPreview() {
		zoomModeOverride = { base: previewUrl, value: 'fit' };
		zoomOverride = { base: previewUrl, value: 1 };
		keepPreview({ zoom: 1, zoomMode: 'fit' });
	}

	/**
	 * @param {Event} event
	 */
	async function selectDevice(event) {
		const select = /** @type {HTMLSelectElement | null} */ (event.currentTarget);
		if (select?.value === ADD_DEVICE) {
			const added = await addCustomDevice();
			select.value = added || selectedDeviceId;
			if (!added) {
				return;
			}
		}
		selectedDeviceId = select?.value || 'none';
		fitPreview();
		if (selectedDeviceId === 'none') {
			landscape = false;
		}
	}

	function toggleLandscape() {
		if (isResponsivePreview) {
			return;
		}
		landscape = !landscape;
		fitPreview();
	}

	function toggleDeviceDrawer() {
		deviceDrawerOpen = !deviceDrawerOpen;
	}

	function closeDeviceDrawer() {
		deviceDrawerOpen = false;
	}

	function handleWindowKeydown(event) {
		if (event.key === 'Escape' && moreOpen) {
			moreOpen = false;
			return;
		}
		if (event.key === 'Escape' && deviceDrawerOpen) {
			closeDeviceDrawer();
			return;
		}
		handlePreviewEscape(event);
	}

	/**
	 * @param {MessageEvent} event
	 */
	function handleAuthoringMessage(event) {
		if (event.source !== iframe?.contentWindow || event.origin !== window.location.origin) {
			return;
		}
		if (!isFlowAuthoringMessage(event.data)) {
			return;
		}
		if (event.data.type === 'viewer.ready') {
			authoringReadyUrl = iframeUrl;
			authoringReadySerial += 1;
			iframe?.contentWindow?.postMessage(themeContextRequestMessage(), window.location.origin);
			return;
		}
		const themeContext = themeContextFromMessage(event.data);
		if (themeContext) {
			onThemeContext?.(themeContext);
			return;
		}
		const nextMode = authoringModeFromMessage(event.data);
		if (nextMode) {
			authoringMode = nextMode;
			return;
		}
		const drop = authoringDropRequest(event.data);
		if (drop) {
			void onAuthoringDrop?.(drop);
			return;
		}
		const move = authoringMoveRequest(event.data);
		if (move) {
			// the viewer waits for the end of its move, which can keep the id of the moved node
			const target = iframe?.contentWindow;
			void Promise.resolve()
				.then(() => onAuthoringMove?.(move))
				.catch(() => undefined)
				.then(() =>
					target?.postMessage(authoringMoveDoneMessage(move.source), window.location.origin)
				);
			return;
		}
		// the chip of the selection in the application offers the actions of the tree on its object
		const actionsRequest = authoringActionsRequest(event.data);
		if (actionsRequest) {
			const target = iframe?.contentWindow;
			void Promise.resolve()
				.then(() => onAuthoringActions?.(actionsRequest.reference) ?? [])
				.catch(() => [])
				.then((actions) =>
					target?.postMessage(
						authoringActionsMessage(actionsRequest.reference, actions),
						window.location.origin
					)
				);
			return;
		}
		const actionRequest = authoringActionRequest(event.data);
		if (actionRequest) {
			const target = iframe?.contentWindow;
			void Promise.resolve()
				.then(() => onAuthoringAction?.(actionRequest))
				.catch(() => undefined)
				.then(() =>
					target?.postMessage(
						authoringActionDoneMessage(actionRequest.reference, actionRequest.action),
						window.location.origin
					)
				);
			return;
		}
		const reference = referenceFromAuthoringMessage(event.data);
		if (reference) {
			void onAuthoringSelect?.(reference);
		}
	}

	/**
	 * @param {Event} event
	 */
	function updateAddressBar(event) {
		const input = /** @type {HTMLInputElement | null} */ (event.currentTarget);
		addressOverride = { base: previewUrl, value: input?.value ?? '' };
	}

	/**
	 * Escape in the address gives back the one of the page loaded, as in a browser.
	 * @param {KeyboardEvent} event
	 */
	function handleAddressKeydown(event) {
		if (event.key === 'Escape' && addressEdited) {
			event.preventDefault();
			addressOverride = { base: previewUrl, value: iframeUrl };
		}
	}

	/**
	 * @param {HTMLIFrameElement} node
	 */
	/** the Capture Manager of the application, as in the application editor of Eclipse */
	let captureOpen = $state(false);
	let capturePicture = $state('');
	let captureError = $state('');

	/**
	 * Captures the preview of the application, and opens the Capture Manager to save it.
	 */
	async function captureApplication() {
		captureOpen = false;
		capturePicture = '';
		captureError = '';
		// the capture waits for the frames without the dialog
		await tick();
		try {
			if (!iframe) {
				throw new Error('no preview');
			}
			capturePicture = await captureElement(iframe);
		} catch (error) {
			captureError = `The browser did not capture the preview (${error instanceof Error ? error.message : error}). Choose a picture instead.`;
		}
		captureOpen = true;
	}

	/**
	 * @returns {string} the page the preview shows, the application having navigated since it opened
	 */
	function currentPageUrl() {
		try {
			const href = iframe?.contentWindow?.location.href;
			if (href && href !== 'about:blank') {
				return href;
			}
		} catch {
			// an application of another origin keeps its page
		}
		return trimmedAddress || previewUrl;
	}

	/** Opens the page the preview shows in a window of its own, as the Eclipse Studio opens it in a browser. */
	function openCurrentPage() {
		const url = currentPageUrl();
		if (url) {
			window.open(url, '_blank', 'noopener,noreferrer');
		}
	}

	/**
	 * Opens the developer tools on the application, as the DevTools button of the Eclipse Studio. The desktop
	 * Studio opens the page of the application in a window of its own, whose developer tools show its code
	 * only, with the session data of the preview; with Alt, it inspects the preview in the Studio. A browser
	 * has its own tools.
	 * @param {MouseEvent} [event]
	 */
	function openDevTools(event) {
		const studio = /** @type {any} */ (window).convertigoStudio;
		if (studio?.debugApplication && !event?.altKey) {
			const data = sessionStorage.getItem(SESSION_DATA);
			studio.debugApplication(currentPageUrl(), data == null ? {} : { [SESSION_DATA]: data });
			return;
		}
		const rect = iframe?.getBoundingClientRect();
		if (studio?.inspect && rect) {
			studio.inspect(
				Math.round(rect.left + rect.width / 2),
				Math.round(rect.top + rect.height / 2)
			);
			return;
		}
		const mac = /Mac|iPhone|iPad/.test(navigator.platform);
		toaster.info({
			title: 'Developer tools',
			description: `Open the developer tools of the browser (${mac ? '⌥⌘I' : 'Ctrl+Shift+I'}) and choose the frame of the application, or open the application in a window of its own.`
		});
	}

	function registerIframe(node) {
		iframe = node;
		authoringReadyUrl = '';
		node.addEventListener('load', attachNgxDocument);
		return () => {
			node.removeEventListener('load', attachNgxDocument);
			ngxAuthoring?.destroy();
			ngxAuthoring = null;
			if (iframe === node) {
				iframe = undefined;
			}
		};
	}

	/**
	 * @param {unknown} value
	 * @param {number} fallback
	 * @returns {number}
	 */
	function getDeviceMetric(value, fallback) {
		return Number.isFinite(value) ? Number(value) : fallback;
	}

	const ADD_DEVICE = '__add';

	function removeSelectedDevice() {
		if (removeCustomDevice(selectedDevice.id)) {
			selectedDeviceId = 'none';
			landscape = false;
			fitPreview();
		}
	}

	function buildDeviceGroups() {
		const devices = Object.values(Bezels)
			.filter(Boolean)
			.sort((a, b) => (a.index ?? 0) - (b.index ?? 0));
		const remaining = [...devices];
		/** @type {{ id: string, title: string, devices: any[] }[]} */
		const groups = [];
		for (const family of familyDefinitions) {
			const familyDevices = [];
			for (let index = remaining.length - 1; index >= 0; index -= 1) {
				const device = remaining[index];
				if (!family.match(device)) {
					continue;
				}
				familyDevices.push(device);
				remaining.splice(index, 1);
			}
			if (familyDevices.length) {
				familyDevices.sort((a, b) => (a.index ?? 0) - (b.index ?? 0));
				groups.push({ id: family.id, title: family.title, devices: familyDevices });
			}
		}
		if (previewDevices.custom.length) {
			groups.push({ id: 'custom', title: 'Custom devices', devices: previewDevices.custom });
		}
		return groups;
	}
</script>

<svelte:window onkeydown={handleWindowKeydown} onmessage={handleAuthoringMessage} />

<div class="studio-preview" style={previewStyle}>
	{#if previewUrl}
		<!--
			The toolbar has a row for the navigation, as the one of a browser, and a row for the edition of
			the application on the left and its display on the right.
		-->
		<form
			class="studio-preview__bar"
			bind:offsetHeight={barHeight}
			onsubmit={(event) => {
				event.preventDefault();
				applyAddressBar();
			}}
		>
			<div class="studio-preview__row">
				<div class="studio-preview__group">
					<Button
						full={false}
						icon="mdi:arrow-left"
						class={iconButtonClasses}
						title="Go back"
						ariaLabel="Go back"
						onclick={navigateBack}
					/>
					<Button
						full={false}
						icon="mdi:arrow-right"
						class={[iconButtonClasses, 'studio-preview__forward']}
						title="Go forward"
						ariaLabel="Go forward"
						onclick={navigateForward}
					/>
					<Button
						full={false}
						icon="mdi:reload"
						class={iconButtonClasses}
						title="Reload"
						ariaLabel="Reload"
						onclick={reloadIframe}
					/>
				</div>
				<div
					class="studio-preview__modes"
					role="radiogroup"
					aria-label="Build previewed"
					aria-busy={previewModeBusy}
				>
					<button
						type="button"
						role="radio"
						aria-checked={previewMode === 'development'}
						class="studio-preview__mode studio-preview__mode--development"
						disabled={previewModeBusy}
						title="The development build, which follows the changes of the project"
						onclick={() => choosePreviewMode('development')}>Dev</button
					>
					<button
						type="button"
						role="radio"
						aria-checked={previewMode !== 'development'}
						class="studio-preview__mode"
						disabled={previewModeBusy}
						title="The latest production build"
						onclick={() => choosePreviewMode('production')}>Prod</button
					>
				</div>
				<div class="studio-preview__address">
					<Ico icon="mdi:web" size={4} class="studio-preview__address-icon" />
					<input
						type="text"
						value={addressBar}
						oninput={updateAddressBar}
						onkeydown={handleAddressKeydown}
						placeholder="URL"
						aria-label="Address of the page"
						spellcheck="false"
					/>
					{#if addressEdited}
						<button
							type="button"
							class="studio-preview__enter"
							title="Load this address (Enter), Escape gives back the one of the page"
							aria-label="Load the address"
							onclick={applyAddressBar}
						>
							<Ico icon="mdi:keyboard-return" size={4} />
						</button>
					{/if}
				</div>
				<Button
					full={false}
					label="Go"
					class="button-secondary h-8! w-fit! px-3!"
					title="Go to the address"
					disabled={!trimmedAddress || trimmedAddress === '#'}
					onclick={applyAddressBar}
				/>
				<span class="studio-preview__sep studio-preview__wide" aria-hidden="true"></span>
				<Button
					full={false}
					icon="mdi:bug-outline"
					class={[iconButtonClasses, 'studio-preview__wide']}
					title={desktopStudio
						? 'Developer tools of the application, in a window of its own (Alt: inspect the preview in the Studio)'
						: 'Developer tools of the application'}
					ariaLabel="Developer tools"
					disabled={!iframe}
					onclick={openDevTools}
				/>
				<Button
					full={false}
					icon="mdi:open-in-new-variant"
					class={[iconButtonClasses, 'studio-preview__wide']}
					title="Open the page of the application in a window of its own"
					ariaLabel="Open frontend"
					onclick={openCurrentPage}
				/>
				<StudioShareQr
					resolveUrl={currentPageUrl}
					available={Boolean(previewUrl)}
					buttonClass={[iconButtonClasses, 'studio-preview__wide'].join(' ')}
					title={previewMode === 'development'
						? 'QR code of the page, to open it on a phone: it follows the edits (hot reload)'
						: 'QR code of the page of the built application, to open it on a phone'}
				/>
				<div class="studio-preview__more studio-preview__narrow">
					<Button
						full={false}
						icon="mdi:dots-horizontal"
						class={[iconButtonClasses, moreOpen && 'studio-preview__icon-button--active']}
						title="More actions"
						ariaLabel="More actions"
						aria-haspopup="menu"
						aria-expanded={moreOpen}
						onclick={() => (moreOpen = !moreOpen)}
					/>
					{#if moreOpen}
						<div class="studio-preview__menu" role="menu" aria-label="More actions">
							{#if ngxAuthoring}
								<button
									type="button"
									role="menuitemcheckbox"
									aria-checked={ngxShowGrids}
									onclick={() => {
										ngxShowGrids = !ngxShowGrids;
										moreOpen = false;
									}}
								>
									<Ico icon="mdi:grid" size={4} />Show the grids
									{#if ngxShowGrids}<Ico icon="mdi:check" size={4} class="ml-auto" />{/if}
								</button>
								<button
									type="button"
									role="menuitem"
									onclick={() => {
										moreOpen = false;
										void captureApplication();
									}}
								>
									<Ico icon="mdi:camera-outline" size={4} />Capture the application
								</button>
								<hr />
							{/if}
							<button
								type="button"
								role="menuitem"
								disabled={!iframe}
								onclick={(event) => {
									moreOpen = false;
									openDevTools(event);
								}}
							>
								<Ico icon="mdi:bug-outline" size={4} />Developer tools
							</button>
							<button
								type="button"
								role="menuitem"
								onclick={() => {
									moreOpen = false;
									openCurrentPage();
								}}
							>
								<Ico icon="mdi:open-in-new-variant" size={4} />Open in a window
							</button>
						</div>
					{/if}
				</div>
			</div>

			<div class="studio-preview__row studio-preview__row--tools">
				{#if ngxAuthoring}
					<div class="studio-preview__group">
						<Button
							full={false}
							icon="mdi:target"
							label="Select"
							class={[labeledButtonClasses, ngxSelecting && 'studio-preview__icon-button--active']}
							title={ngxSelecting
								? 'Stop selecting components in the application'
								: 'Select a component by clicking it in the application'}
							ariaLabel="Select a component in the application"
							aria-pressed={ngxSelecting}
							onclick={() => (ngxSelecting = !ngxSelecting)}
						/>
						{#if ngxReference}
							<Button
								full={false}
								icon={ngxHighlightHidden ? 'mdi:eye-outline' : 'mdi:eye-off-outline'}
								class={iconButtonClasses}
								title={ngxHighlightHidden
									? 'Show the highlight of the selected component'
									: 'Hide the highlight of the selected component'}
								ariaLabel={ngxHighlightHidden ? 'Show the highlight' : 'Hide the highlight'}
								onclick={() => (ngxHighlightHidden = !ngxHighlightHidden)}
							/>
						{/if}
						<Button
							full={false}
							icon="mdi:palette-swatch-outline"
							label="Styles"
							class={[
								labeledButtonClasses,
								ngxStyleEditing && 'studio-preview__icon-button--active'
							]}
							title={ngxStyleEditing
								? 'Apply the styles, texts and moves of the style editor'
								: 'Edit the styles of the application'}
							ariaLabel="Style editor"
							aria-pressed={ngxStyleEditing}
							onclick={() => void toggleNgxStyleEditor()}
						/>
						<Button
							full={false}
							icon="mdi:grid"
							class={[
								iconButtonClasses,
								'studio-preview__wide',
								ngxShowGrids && 'studio-preview__icon-button--active'
							]}
							title={ngxShowGrids ? 'Hide the grids' : 'Show the grids, rows and columns'}
							ariaLabel="Show the grids"
							aria-pressed={ngxShowGrids}
							onclick={() => (ngxShowGrids = !ngxShowGrids)}
						/>
					</div>
					<span class="studio-preview__sep" aria-hidden="true"></span>
					<label
						class="studio-preview__chip studio-preview__dataset"
						title="Dataset of the application: its recorded session data"
					>
						<Ico icon="mdi:database-outline" size={4} />
						<select aria-label="Dataset" value={ngxDataset} onchange={chooseNgxDataset}>
							<option value="none">No dataset</option>
							{#each ngxDatasets as dataset (dataset)}
								<option value={dataset}>{dataset}</option>
							{/each}
							<optgroup label="Actions">
								<option value={SAVE_DATASET}>Save the session data as a dataset…</option>
								{#if ngxDataset !== 'none'}
									<option value={REMOVE_DATASET}>Remove the dataset {ngxDataset}</option>
								{/if}
							</optgroup>
						</select>
					</label>
				{/if}
				<span class="studio-preview__spacer"></span>
				<div class="studio-preview__group">
					{#if showDeviceDrawer}
						<button
							type="button"
							class="studio-preview__chip studio-preview__device"
							class:studio-preview__chip--open={deviceDrawerOpen}
							title={deviceDrawerOpen ? 'Close device drawer' : 'Choose preview device'}
							aria-label={deviceDrawerOpen ? 'Close device drawer' : 'Choose preview device'}
							aria-expanded={deviceDrawerOpen}
							aria-controls="studio-preview-device-drawer"
							onclick={toggleDeviceDrawer}
						>
							<Ico icon={deviceIcon} size={4} />
							<span class="studio-preview__device-name studio-ellipsis">{deviceChipLabel}</span>
							<Ico icon="mdi:chevron-down" size={3} class="studio-preview__chevron" />
						</button>
					{/if}
					{#if showDeviceSelector}
						<label class="studio-preview__device-select layout-x-low">
							{#if isResponsivePreview || !selectedDevice.bezel}
								<Ico icon="mdi:devices" size={4} />
							{:else}
								<img
									class="studio-preview__device-thumb"
									src={bezelThumbnail(selectedDevice.id)}
									alt=""
									loading="lazy"
								/>
							{/if}
							<select value={selectedDeviceId} aria-label="Preview device" onchange={selectDevice}>
								{#each deviceGroups as group (group.id)}
									<optgroup label={group.title}>
										{#each group.devices as device (device.id)}
											<option value={device.id}
												>{device.id === 'none' ? 'Responsive' : device.title}</option
											>
										{/each}
									</optgroup>
								{/each}
								<option value={ADD_DEVICE}>Add a custom device…</option>
							</select>
						</label>
						{#if 'custom' in selectedDevice}
							<Button
								full={false}
								icon="mdi:delete-outline"
								class={iconButtonClasses}
								title="Remove this custom device"
								ariaLabel="Remove this custom device"
								onclick={removeSelectedDevice}
							/>
						{/if}
						<select
							class="studio-preview__os"
							value={previewDevices.os}
							aria-label="Device OS"
							title="The OS the application shows, as its Ionic mode"
							onchange={(event) =>
								selectDeviceOs(
									/** @type {'auto' | 'android' | 'ios'} */ (event.currentTarget.value)
								)}
						>
							<option value="auto"
								>OS of the device ({effectiveOs === 'ios' ? 'iOS' : 'Android'})</option
							>
							<option value="android">Android</option>
							<option value="ios">iOS</option>
						</select>
						<span class="studio-preview__size studio-ellipsis">{viewportLabel}</span>
					{/if}
					{#if !isResponsivePreview}
						<Button
							full={false}
							icon="mdi:camera-rotate-outline"
							class={[iconButtonClasses, landscape && 'studio-preview__icon-button--active']}
							title={landscape
								? 'Rotate the viewport to portrait'
								: 'Rotate the viewport to landscape'}
							ariaLabel="Rotate viewport"
							aria-pressed={landscape}
							onclick={toggleLandscape}
						/>
					{/if}
				</div>
				<span class="studio-preview__sep" aria-hidden="true"></span>
				<div class="studio-preview__group">
					<Button
						full={false}
						icon="mdi:minus"
						class={[iconButtonClasses, 'studio-preview__zoom-step']}
						title="Zoom out"
						ariaLabel="Zoom out"
						onclick={() => adjustZoom(-1)}
					/>
					<label class="studio-preview__chip studio-preview__zoom" title="Zoom of the preview">
						<select
							aria-label="Zoom"
							value={zoomMode === 'fit' ? 'fit' : String(zoom)}
							onchange={chooseZoom}
						>
							<option value="fit">Fit</option>
							{#each zoomChoices as choice (choice)}
								<option value={String(choice)}>{Math.round(choice * 100)}%</option>
							{/each}
						</select>
					</label>
					<Button
						full={false}
						icon="mdi:plus"
						class={[iconButtonClasses, 'studio-preview__zoom-step']}
						title="Zoom in"
						ariaLabel="Zoom in"
						onclick={() => adjustZoom(1)}
					/>
				</div>
				{#if ngxAuthoring}
					<span class="studio-preview__sep studio-preview__wide" aria-hidden="true"></span>
					<Button
						full={false}
						icon="mdi:camera-outline"
						class={[iconButtonClasses, 'studio-preview__wide']}
						title="Capture the application as its thumbnail or a Marketplace screen"
						ariaLabel="Capture the application"
						onclick={() => void captureApplication()}
					/>
				{/if}
			</div>
		</form>

		{#if moreOpen}
			<div
				class="studio-preview__menu-backdrop"
				role="presentation"
				onclick={() => (moreOpen = false)}
			></div>
		{/if}

		{#if showDeviceDrawer}
			{#if deviceDrawerOpen}
				<button
					type="button"
					class="studio-preview__drawer-backdrop"
					aria-label="Close device drawer"
					onclick={closeDeviceDrawer}
				></button>
			{/if}
			<aside
				id="studio-preview-device-drawer"
				class="studio-preview__device-drawer"
				class:studio-preview__device-drawer--open={deviceDrawerOpen}
				aria-label="Preview devices"
				aria-hidden={!deviceDrawerOpen}
				inert={!deviceDrawerOpen}
			>
				<StudioDevicePanel bind:selectedDeviceId bind:landscape onSelect={closeDeviceDrawer} />
			</aside>
		{/if}

		<MaxRectangle bind:clientHeight bind:clientWidth class="studio-preview__viewport">
			{#if banner.label}
				<div
					class={['studio-preview__activity', `studio-preview__activity--${banner.tone}`]}
					role="status"
					transition:fade={{ duration: 150 }}
				>
					<Ico
						icon={banner.tone === 'success'
							? 'mdi:check-circle-outline'
							: banner.tone === 'failed'
								? 'mdi:alert-circle-outline'
								: 'mdi:sync'}
						size={4}
						class="studio-preview__activity-icon"
					/>
					<span>{banner.label}</span>
					{#if banner.progress > 0 && banner.progress < 100}
						<span class="studio-preview__activity-bar" style:width={`${banner.progress}%`}></span>
					{/if}
				</div>
			{/if}
			<div class="studio-preview__center">
				<div
					class={[
						'studio-preview__scaled-box',
						isResponsivePreview && 'studio-preview__scaled-box--fluid'
					]}
				>
					<div
						class={[
							'studio-preview__stage',
							isResponsivePreview && 'studio-preview__stage--responsive',
							isFramedDevice && 'studio-preview__stage--framed',
							!isResponsivePreview && !isFramedDevice && 'studio-preview__stage--viewport'
						]}
					>
						<iframe
							{@attach registerIframe}
							class={['studio-preview__frame', isFramedDevice && 'studio-preview__frame--framed']}
							title={`${projectName} frontend`}
							src={frameUrl}
						></iframe>
						{#if isFramedDevice}
							<picture class="studio-preview__bezel" aria-hidden="true">
								<source srcset={bezelImage(selectedDevice.id)} type="image/webp" />

								<img src={bezelImage(selectedDevice.id)} alt="" loading="lazy" />
							</picture>
						{/if}
					</div>
				</div>
			</div>
		</MaxRectangle>
	{:else if noFrontend}
		<StudioEmptyState
			message={`The project ${projectName} has no frontend application.`}
			icon="mdi:application-outline"
			class="studio-preview__empty"
		/>
	{:else}
		<StudioEmptyState
			message="No project selected"
			icon="mdi:folder-outline"
			class="studio-preview__empty"
		/>
	{/if}
	{#if captureOpen && projectName}
		<StudioCaptureDialog
			{projectName}
			capture={capturePicture}
			error={captureError}
			onCapture={captureApplication}
			onClose={() => (captureOpen = false)}
		/>
	{/if}
</div>

<style>
	/* over the preview, which stays usable while its application builds */
	.studio-preview__activity {
		position: absolute;
		z-index: 5;
		top: 0.6rem;
		left: 50%;
		display: flex;
		overflow: hidden;
		align-items: center;
		gap: 0.45rem;
		border: 1px solid var(--studio-line);
		border-radius: 999px;
		background: color-mix(in oklab, var(--studio-panel-bg) 92%, transparent);
		box-shadow: 0 4px 16px light-dark(rgb(0 0 0 / 0.12), rgb(0 0 0 / 0.45));
		color: var(--studio-text);
		padding: 0.35rem 0.85rem;
		font-size: 0.75rem;
		pointer-events: none;
		transform: translateX(-50%);
		white-space: nowrap;
	}

	.studio-preview__activity--busy :global(.studio-preview__activity-icon) {
		animation: studio-preview-spin 1.2s linear infinite;
		color: var(--color-primary-500);
	}

	.studio-preview__activity--success :global(.studio-preview__activity-icon) {
		color: var(--color-success-600-400);
	}

	.studio-preview__activity--failed {
		border-color: color-mix(in oklab, var(--color-error-500) 45%, transparent);
	}

	.studio-preview__activity--failed :global(.studio-preview__activity-icon) {
		color: var(--color-error-600-400);
	}

	.studio-preview__activity-bar {
		position: absolute;
		bottom: 0;
		left: 0;
		height: 2px;
		background: var(--color-primary-500);
		transition: width 0.2s ease;
	}

	@keyframes studio-preview-spin {
		to {
			transform: rotate(360deg);
		}
	}

	/* the toolbar follows the width of the view, which the dock sets, rather than the one of the window */
	.studio-preview {
		position: relative;
		display: grid;
		height: 100%;
		min-height: 0;
		grid-template-rows: auto minmax(0, 1fr);
		background: var(--color-surface-100-900);
		container-type: inline-size;
	}

	.studio-preview__drawer-backdrop {
		position: absolute;
		z-index: 18;
		inset: var(--studio-preview-bar-height, 2.8rem) 0 0;
		border: 0;
		background: color-mix(in oklab, var(--color-surface-950-50) 18%, transparent);
		cursor: default;
	}

	.studio-preview__device-drawer {
		position: absolute;
		z-index: 20;
		top: var(--studio-preview-bar-height, 2.8rem);
		bottom: 0;
		left: 0;
		width: min(22rem, calc(100% - 2rem));
		overflow: hidden;
		border-right: 1px solid var(--color-surface-200-800);
		background: var(--color-surface-50-950);
		box-shadow: var(--shadow-follow);
		transform: translateX(-102%);
		transition: transform 180ms ease;
	}

	.studio-preview__device-drawer--open {
		transform: translateX(0);
	}

	@media (prefers-reduced-motion: reduce) {
		.studio-preview__device-drawer {
			transition: none;
		}
	}

	.studio-preview__bar {
		display: grid;
		gap: 0.3rem;
		border-bottom: 1px solid var(--color-surface-200-800);
		background: color-mix(in oklab, var(--color-surface-50-950) 88%, transparent);
		padding: 0.35rem 0.4rem;
	}

	/* the buttons of the toolbar read as the ones of the other views of the Studio */
	.studio-preview__bar :global(.button-ico-secondary) {
		color: var(--studio-text-idle);
	}

	.studio-preview__bar :global(.button-ico-secondary:not(:disabled):hover) {
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

	.studio-preview__bar :global(.button-ico-secondary:disabled) {
		opacity: 0.4;
	}

	.studio-preview__bar :global(.studio-preview__icon-button--active),
	.studio-preview__bar :global(.studio-preview__icon-button--active:not(:disabled):hover) {
		background: color-mix(in oklab, var(--color-primary-500) 16%, transparent);
		color: var(--color-primary-600-400);
	}

	.studio-preview__row {
		display: flex;
		min-width: 0;
		align-items: center;
		gap: 0.25rem;
	}

	.studio-preview__group {
		display: flex;
		flex: none;
		align-items: center;
		gap: 0.125rem;
	}

	.studio-preview__sep {
		flex: none;
		width: 1px;
		height: 1.1rem;
		margin: 0 0.25rem;
		background: var(--color-surface-200-800);
	}

	.studio-preview__spacer {
		flex: 1 1 0;
		min-width: 0.25rem;
	}

	/* Dev and Prod, the build the preview shows, as a segmented control */
	.studio-preview__modes {
		display: inline-flex;
		flex: none;
		height: 2rem;
		align-items: stretch;
		gap: 2px;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.35rem;
		background: color-mix(in oklab, var(--color-surface-100-900) 78%, transparent);
		padding: 2px;
	}

	.studio-preview__mode {
		font: inherit;
		border: 0;
		border-radius: 0.25rem;
		background: transparent;
		color: var(--color-surface-600-400);
		padding: 0 0.55rem;
		font-size: 0.72rem;
		font-weight: 650;
		cursor: pointer;
	}

	.studio-preview__mode:not(:disabled):hover {
		color: var(--color-surface-950-50);
	}

	.studio-preview__mode[aria-checked='true'] {
		background: var(--color-surface-300-700);
		color: var(--color-surface-950-50);
	}

	.studio-preview__mode--development[aria-checked='true'] {
		background: color-mix(in oklab, var(--color-success-500) 22%, transparent);
		color: var(--color-success-700-300);
	}

	.studio-preview__mode:disabled {
		cursor: wait;
	}

	.studio-preview__modes[aria-busy='true'] {
		opacity: 0.65;
	}

	.studio-preview__mode:focus-visible {
		outline: 2px solid var(--color-primary-500);
		outline-offset: 1px;
	}

	.studio-preview__address {
		display: flex;
		flex: 1 1 12rem;
		min-width: 6rem;
		height: 2rem;
		align-items: center;
		gap: 0.35rem;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.3rem;
		background: var(--color-surface-50-950);
		padding: 0 0.2rem 0 0.5rem;
	}

	.studio-preview__address:focus-within {
		border-color: var(--color-primary-500);
	}

	.studio-preview__address :global(.studio-preview__address-icon) {
		flex: none;
		color: var(--color-surface-500);
	}

	.studio-preview__address input {
		flex: 1;
		min-width: 0;
		height: 100%;
		border: 0;
		background: transparent;
		color: var(--color-surface-950-50);
		padding: 0;
		font-size: 0.76rem;
		outline: none;
		box-shadow: none;
	}

	.studio-preview__enter {
		display: inline-flex;
		flex: none;
		width: 1.6rem;
		height: 1.6rem;
		align-items: center;
		justify-content: center;
		border: 0;
		border-radius: 0.25rem;
		background: color-mix(in oklab, var(--color-primary-500) 14%, transparent);
		color: var(--color-primary-600-400);
		cursor: pointer;
	}

	.studio-preview__enter:hover {
		background: color-mix(in oklab, var(--color-primary-500) 26%, transparent);
	}

	/* the dataset, device and zoom menus */
	.studio-preview__chip {
		display: inline-flex;
		flex: none;
		min-width: 0;
		height: 2rem;
		align-items: center;
		gap: 0.35rem;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.3rem;
		background: color-mix(in oklab, var(--color-surface-100-900) 78%, transparent);
		color: var(--color-surface-700-300);
		padding: 0 0.45rem;
		font: inherit;
		font-size: 0.74rem;
	}

	.studio-preview__chip select {
		field-sizing: content;
		min-width: 0;
		height: 100%;
		border: 0;
		background-color: transparent;
		background-position: right 0.05rem center;
		background-size: 1rem 1rem;
		color: var(--color-surface-950-50);
		padding: 0 1.2rem 0 0;
		font-size: 0.74rem;
		outline: none;
		box-shadow: none;
		cursor: pointer;
	}

	.studio-preview__chip:focus-within,
	.studio-preview__chip:hover,
	.studio-preview__chip--open {
		border-color: color-mix(in oklab, var(--color-surface-400-600) 70%, transparent);
	}

	.studio-preview__dataset select {
		max-width: 9rem;
	}

	.studio-preview__device {
		cursor: pointer;
	}

	.studio-preview__device-name {
		max-width: 14rem;
		color: var(--color-surface-950-50);
		font-weight: 600;
	}

	.studio-preview__device :global(.studio-preview__chevron) {
		color: var(--color-surface-500);
	}

	.studio-preview__zoom select {
		min-width: 3.2rem;
		font-weight: 650;
	}

	.studio-preview :global(.studio-preview__labeled) {
		gap: 0.3rem;
		font-size: 0.74rem;
		font-weight: 600;
	}

	.studio-preview__more {
		position: relative;
		flex: none;
	}

	.studio-preview__menu-backdrop {
		position: absolute;
		z-index: 30;
		inset: 0;
	}

	.studio-preview__menu {
		position: absolute;
		z-index: 31;
		top: calc(100% + 0.3rem);
		right: 0;
		display: grid;
		min-width: 13rem;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: var(--color-surface-50-950);
		box-shadow: var(--shadow-follow);
		padding: 0.25rem;
	}

	.studio-preview__menu button {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		border: 0;
		border-radius: 0.3rem;
		background: transparent;
		color: var(--color-surface-950-50);
		padding: 0.4rem 0.5rem;
		font-size: 0.78rem;
		text-align: left;
	}

	.studio-preview__menu button:not(:disabled):hover {
		background: var(--studio-hover-bg);
	}

	.studio-preview__menu button:disabled {
		opacity: 0.5;
	}

	.studio-preview__menu hr {
		margin: 0.25rem 0;
		border-color: var(--color-surface-200-800);
	}

	.studio-preview__narrow {
		display: none;
	}

	.studio-preview__device-select {
		min-width: 10rem;
		height: 2rem;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.3rem;
		background: color-mix(in oklab, var(--color-surface-100-900) 78%, transparent);
		color: var(--color-surface-700-300);
		padding: 0 0.45rem;
	}

	.studio-preview__os {
		max-width: 9rem;
		height: 2rem;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.3rem;
		background: color-mix(in oklab, var(--color-surface-100-900) 78%, transparent);
		color: var(--color-surface-950-50);
		padding: 0 0.45rem;
		font-size: 0.74rem;
	}

	.studio-preview__device-select select {
		min-width: 0;
		max-width: 12rem;
		border: 0;
		background: transparent;
		color: var(--color-surface-950-50);
		font-size: 0.74rem;
		font-weight: 650;
		outline: none;
	}

	.studio-preview__device-thumb {
		width: 1.4rem;
		height: 1.4rem;
		object-fit: contain;
	}

	:global(.studio-preview__icon-button--active) {
		border-color: color-mix(in oklab, var(--color-primary-500) 44%, transparent);
		background: color-mix(in oklab, var(--color-primary-500) 12%, transparent);
		color: var(--color-primary-600-400);
	}

	.studio-preview__size {
		max-width: 15rem;
		min-width: 5rem;
		color: var(--color-surface-600-400);
		font-size: 0.7rem;
		font-weight: 700;
		text-align: center;
	}

	:global(.studio-preview__viewport) {
		position: relative;
		min-width: 0;
		min-height: 0;
		overflow: auto;
		background:
			linear-gradient(
				45deg,
				color-mix(in oklab, var(--color-surface-200-800) 42%, transparent) 25%,
				transparent 25%
			),
			linear-gradient(
				-45deg,
				color-mix(in oklab, var(--color-surface-200-800) 42%, transparent) 25%,
				transparent 25%
			),
			var(--color-surface-100-900);
		background-position:
			0 0,
			0 0;
		background-size: 1rem 1rem;
		padding: 0.75rem;
	}

	.studio-preview__center {
		display: grid;
		min-width: 100%;
		min-height: 100%;
		place-items: center;
	}

	.studio-preview__scaled-box {
		width: var(--studio-preview-render-width, 100%);
		height: var(--studio-preview-render-height, 100%);
		min-width: 0;
		min-height: 0;
	}

	.studio-preview__scaled-box--fluid {
		width: 100%;
		height: 100%;
	}

	.studio-preview__stage {
		width: var(--studio-preview-width, calc(100% / var(--studio-preview-scale)));
		height: var(--studio-preview-height, calc(100% / var(--studio-preview-scale)));
		min-width: 24rem;
		min-height: 20rem;
		overflow: hidden;
		border: 1px solid var(--color-surface-300-700);
		border-radius: 0.35rem;
		background: white;
		box-shadow: 0 0.5rem 1.4rem color-mix(in oklab, var(--color-surface-950) 10%, transparent);
		transform: scale(var(--studio-preview-scale));
		transform-origin: top left;
	}

	.studio-preview__stage--framed,
	.studio-preview__stage--viewport {
		position: relative;
		min-width: 0;
		min-height: 0;
	}

	.studio-preview__stage--framed {
		overflow: visible;
		border: 0;
		background: transparent;
		box-shadow: none;
	}

	.studio-preview__stage--viewport {
		width: var(--studio-preview-frame-width);
		height: var(--studio-preview-frame-height);
		border-style: dashed;
	}

	.studio-preview__frame {
		width: 100%;
		height: 100%;
		min-height: 0;
		border: 0;
		background: white;
	}

	.studio-preview__frame--framed {
		position: absolute;
		z-index: 1;
		top: var(--studio-preview-frame-top);
		left: var(--studio-preview-frame-left);
		width: var(--studio-preview-frame-width);
		height: var(--studio-preview-frame-height);
		overflow: hidden;
		border-radius: var(--studio-preview-frame-radius);
	}

	.studio-preview__bezel {
		position: absolute;
		z-index: 2;
		inset: 0;
		pointer-events: none;
		user-select: none;
	}

	.studio-preview__bezel img {
		display: block;
		width: 100%;
		height: 100%;
	}

	.studio-preview :global(.studio-preview__empty) {
		min-height: 16rem;
	}

	/* a view less wide: the zoom takes its menu only */
	@container (max-width: 820px) {
		.studio-preview :global(.studio-preview__zoom-step) {
			display: none;
		}

		.studio-preview__device-name {
			max-width: 9rem;
		}
	}

	/* a narrow view: the tools show their icons only, the actions used less go in the More menu */
	@container (max-width: 700px) {
		.studio-preview :global(.studio-preview__wide) {
			display: none;
		}

		.studio-preview__narrow {
			display: block;
		}

		.studio-preview :global(.studio-preview__labeled) {
			width: 2rem !important;
			padding: 0 !important;
		}

		.studio-preview :global(.studio-preview__labeled > span + span) {
			display: none;
		}

		.studio-preview__row--tools {
			flex-wrap: wrap;
		}

		.studio-preview__device-name {
			max-width: 8rem;
		}

		.studio-preview__dataset select {
			max-width: 7rem;
		}
	}

	@container (max-width: 460px) {
		.studio-preview :global(.studio-preview__forward) {
			display: none;
		}
	}
</style>
