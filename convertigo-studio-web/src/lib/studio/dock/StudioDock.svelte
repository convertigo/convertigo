<script>
	import { asset } from '$app/paths';
	import Ico from '$lib/utils/Ico.svelte';
	import { createDockview } from 'dockview';
	import 'dockview/dist/styles/dockview.css';
	import { mount, onMount, unmount } from 'svelte';
	import StudioDockTab from './StudioDockTab.svelte';

	/**
	 * The views of the Studio, laid out as the views of the Eclipse Studio: a view goes anywhere by its tab,
	 * beside another one, in a group of its own or in a floating window, a group is resized by its edges
	 * and maximized by a double-click on a tab. Each profile keeps its own layout, as a perspective.
	 *
	 * @typedef {import('./dockTypes.js').DockView} DockView
	 * @typedef {import('./dockTypes.js').DockArea} DockArea
	 * @typedef {import('./dockTypes.js').DockLayout} DockLayout
	 */

	/**
	 * @type {{
	 *  views: DockView[],
	 *  profile: string,
	 *  layouts: Record<string, DockLayout>,
	 *  storageKey?: string,
	 *  visible?: Record<string, boolean>,
	 *  open?: Record<string, boolean>,
	 *  menuOpen?: boolean
	 * }}
	 */
	let {
		views = [],
		profile = 'backend',
		layouts = {},
		storageKey = 'convertigo.studio.dock.v1',
		visible = $bindable({}),
		open = $bindable({}),
		menuOpen = $bindable(false)
	} = $props();

	const AREAS = /** @type {DockArea[]} */ (['center', 'left', 'right', 'bottom']);
	const AREA_DIRECTIONS = { left: 'left', right: 'right', bottom: 'below' };
	/** on a small screen the areas are stacked: the projects, the tools, the work area, the logs */
	const NARROW_AREAS = /** @type {DockArea[]} */ (['left', 'right', 'center', 'bottom']);
	const NARROW_DIRECTIONS = { left: 'above', right: 'below', center: 'below', bottom: 'below' };
	const NARROW_SCREEN = '(max-width: 980px)';
	const DEFAULT_SIZES = { left: 280, right: 340, bottom: 240 };
	/** the page the views moved to a window of their own open in, as a second screen shows them */
	const POPOUT_URL = asset('/studio-popout.html');
	/** the desktop Studio, which opens again the windows of the views where they were */
	const desktop =
		typeof window !== 'undefined' && Boolean(/** @type {any} */ (window).convertigoStudio);

	/** @type {HTMLElement | undefined} */
	let container = $state();
	/** @type {HTMLElement | undefined} */
	let holder = $state();
	/** @type {Record<string, HTMLElement>} */
	const elements = $state({});
	/** @type {Record<string, HTMLElement>} */
	const actionElements = $state({});
	/** the lazy views already shown, which keep their content while they stay open */
	let shown = $state(/** @type {Record<string, boolean>} */ ({}));
	/** @type {import('dockview').DockviewApi | undefined} */
	let api;
	let layoutProfile = '';
	/** whether the layout is the one of a small screen, kept apart from the layout of a large one */
	let narrow = false;
	let loading = false;
	/** @type {ReturnType<typeof setTimeout> | undefined} */
	let saveTimer;
	let stateQueued = false;
	/** where a closed view goes back: the view it was grouped with */
	const closedBeside = new Map();
	/** @type {Map<string, { alert?: string, detail?: string }>} the tabs, which show what their views tell */
	const tabs = new Map();
	/** the layout before a view is maximized, whose sizes the groups take again once it is restored */
	let beforeMaximize = /** @type {any} */ (null);

	/** @param {string} id */
	function viewOf(id) {
		return views.find((view) => view.id === id);
	}

	/**
	 * @param {DockArea} area
	 * @param {DockLayout | undefined} layout
	 */
	function areaViews(area, layout) {
		return (layout?.areas?.[area] ?? []).filter((id) => viewOf(id));
	}

	/**
	 * The content of a panel of the dock is the element of its view, rendered once by the Studio: moving
	 * a view keeps its state, as the text of an editor or the page of a preview.
	 * @param {{ id: string, name: string }} options
	 */
	function createContent(options) {
		const element = document.createElement('div');
		element.className = 'studio-dock-panel';
		return {
			element,
			init() {
				const view = elements[options.name];
				if (view) {
					element.appendChild(view);
				}
			},
			dispose() {
				const view = elements[options.name];
				if (view?.parentElement === element) {
					holder?.appendChild(view);
				}
			}
		};
	}

	/**
	 * @param {{ id: string, name: string }} options
	 */
	function createTab(options) {
		const element = document.createElement('div');
		element.className = 'studio-dock-tab-host';
		const view = viewOf(options.name);
		/** @type {any} */
		let component;
		return {
			element,
			/** @param {any} params */
			init(params) {
				const tab = $state({
					title: view?.title ?? options.name,
					icon: view?.icon,
					alert: viewOf(options.name)?.alert ?? '',
					detail: viewOf(options.name)?.detail ?? '',
					onClose: () => params.api.close()
				});
				tabs.set(options.name, tab);
				component = mount(StudioDockTab, { target: element, props: { tab } });
				// a double-click on a tab maximizes its view, as in the Eclipse Studio
				element.addEventListener('dblclick', () => toggleMaximize(params.api));
			},
			dispose() {
				tabs.delete(options.name);
				if (component) {
					void unmount(component);
					component = undefined;
				}
			}
		};
	}

	/**
	 * The header of a group shows the buttons of its visible view, and maximizes or restores the group.
	 * @param {any} group
	 */
	function createGroupActions(group) {
		const element = document.createElement('div');
		element.className = 'studio-dock-actions';
		const viewActions = document.createElement('div');
		viewActions.className = 'studio-dock-actions__view';
		const maximize = document.createElement('button');
		maximize.type = 'button';
		maximize.className = 'studio-dock-actions__button';
		const popout = document.createElement('button');
		popout.type = 'button';
		popout.className = 'studio-dock-actions__button';
		element.append(viewActions, popout, maximize);
		/** @type {any} */
		let icon;
		/** @type {any} */
		let popoutIcon;
		/** @type {{ dispose: () => void }[]} */
		const subscriptions = [];
		const update = () => {
			const panel = group.activePanel;
			const id = panel?.api?.component ?? '';
			for (const child of [...viewActions.children]) {
				if (child !== actionElements[id]) {
					holder?.appendChild(child);
				}
			}
			const actions = actionElements[id];
			if (actions && actions.parentElement !== viewActions) {
				viewActions.appendChild(actions);
			}
			group.element?.classList.toggle('studio-dock-group--main', Boolean(viewOf(id)?.main));
			const maximized = Boolean(group.api?.isMaximized?.());
			const location = group.api?.location?.type;
			const label = maximized ? 'Restore the views' : 'Maximize the view';
			maximize.title = label;
			maximize.setAttribute('aria-label', label);
			maximize.hidden = location === 'floating' || location === 'popout';
			if (icon) {
				void unmount(icon);
			}
			icon = mount(Ico, {
				target: maximize,
				props: { icon: maximized ? 'mdi:window-restore' : 'mdi:window-maximize', size: 4 }
			});
			const popoutLabel =
				location === 'popout' ? 'Move back to the Studio' : 'Move to a new window';
			popout.title = popoutLabel;
			popout.setAttribute('aria-label', popoutLabel);
			popout.hidden = maximized;
			if (popoutIcon) {
				void unmount(popoutIcon);
			}
			popoutIcon = mount(Ico, {
				target: popout,
				props: { icon: location === 'popout' ? 'mdi:dock-window' : 'mdi:open-in-new', size: 4 }
			});
		};
		popout.addEventListener('click', () => {
			if (group.api?.location?.type === 'popout') {
				movePopoutBack(group);
			} else {
				void popoutGroup(group);
			}
		});
		maximize.addEventListener('click', () => {
			if (group.activePanel) {
				toggleMaximize(group.activePanel.api);
			}
		});
		return {
			element,
			init() {
				subscriptions.push(
					group.api.onDidActivePanelChange(update),
					group.api.onDidLocationChange(update)
				);
				if (api) {
					subscriptions.push(api.onDidMaximizedGroupChange(update));
				}
				update();
			},
			dispose() {
				subscriptions.forEach((subscription) => subscription.dispose());
				for (const child of [...viewActions.children]) {
					holder?.appendChild(child);
				}
				if (icon) {
					void unmount(icon);
				}
				if (popoutIcon) {
					void unmount(popoutIcon);
				}
			}
		};
	}

	/**
	 * Moves a view, or its group, to a window of its own, placed over it, which the Studio can put on
	 * another screen.
	 * @param {any} item a group or a view
	 */
	async function popoutGroup(item) {
		if (!api) {
			return;
		}
		const group = item.group ?? item;
		const rect = group.element?.getBoundingClientRect();
		const chrome = Math.max(0, window.outerHeight - window.innerHeight);
		const position = rect
			? {
					left: Math.round(window.screenX + rect.left),
					top: Math.round(window.screenY + chrome + rect.top),
					width: Math.max(480, Math.round(rect.width)),
					height: Math.max(360, Math.round(rect.height))
				}
			: undefined;
		await api.addPopoutGroup(item, { popoutUrl: POPOUT_URL, position });
	}

	/**
	 * Moves the views of a window of their own back to the Studio, as the window closes.
	 * @param {any} group
	 */
	function movePopoutBack(group) {
		api
			?.getPopouts()
			.find((popout) => popout.group === group)
			?.window.close();
	}

	/**
	 * A window of views shows the Studio in its theme and its styles, those it loads later too.
	 * @param {Window} popup
	 */
	function preparePopout(popup) {
		const doc = popup.document;
		const root = document.documentElement;
		const sync = () => {
			doc.documentElement.className = root.className;
			for (const name of ['style', 'data-theme', 'data-mode', 'lang']) {
				const value = root.getAttribute(name);
				if (value == null) {
					doc.documentElement.removeAttribute(name);
				} else {
					doc.documentElement.setAttribute(name, value);
				}
			}
		};
		sync();
		// the colors of the Studio are declared for a page that shows it
		if (!doc.querySelector('.studio-shell')) {
			const marker = doc.createElement('div');
			marker.className = 'studio-shell';
			marker.style.display = 'none';
			doc.body.prepend(marker);
		}
		doc.body.style.background = 'var(--studio-canvas-bg, var(--studio-main-bg))';
		doc.body.style.color = 'var(--studio-text)';
		const theme = new MutationObserver(sync);
		theme.observe(root, { attributes: true });
		const styles = new MutationObserver((records) => {
			for (const record of records) {
				for (const node of record.addedNodes) {
					if (
						node instanceof HTMLStyleElement ||
						(node instanceof HTMLLinkElement && node.rel === 'stylesheet')
					) {
						doc.head.appendChild(node.cloneNode(true));
					}
				}
			}
		});
		styles.observe(document.head, { childList: true });
		popup.addEventListener(
			'pagehide',
			() => {
				theme.disconnect();
				styles.disconnect();
			},
			{ once: true }
		);
	}

	/**
	 * The sizes of a layout given to the same layout, or null when the groups changed.
	 * @param {any} node
	 * @param {any} sized
	 * @returns {any}
	 */
	function withSizes(node, sized) {
		if (!node || !sized || node.type !== sized.type) {
			return null;
		}
		if (node.type === 'leaf') {
			return node.data?.id === sized.data?.id ? { ...node, size: sized.size } : null;
		}
		if (!Array.isArray(node.data) || node.data.length !== sized.data?.length) {
			return null;
		}
		const data = node.data.map((/** @type {any} */ child, /** @type {number} */ index) =>
			withSizes(child, sized.data[index])
		);
		return data.every(Boolean) ? { ...node, data, size: sized.size } : null;
	}

	/**
	 * The groups hidden by a maximized view take their sizes again, which the grid does not give back
	 * to the groups after the maximized one. A layout changed meanwhile keeps its sizes.
	 */
	function restoreSizes() {
		const before = beforeMaximize;
		beforeMaximize = null;
		if (!api || !before || api.hasMaximizedGroup()) {
			return;
		}
		const current = api.toJSON();
		const root = withSizes(current.grid?.root, before.grid?.root);
		if (!root) {
			return;
		}
		loading = true;
		try {
			api.fromJSON(
				{
					...current,
					grid: { ...current.grid, root, width: before.grid.width, height: before.grid.height }
				},
				{ reuseExistingPanels: true }
			);
			if (container?.clientWidth) {
				api.layout(container.clientWidth, container.clientHeight);
			}
		} catch (error) {
			console.warn('Unable to restore the sizes of the Studio views', error);
		} finally {
			loading = false;
		}
		updateState();
		saveLayout();
	}

	/** @param {any} panelApi */
	function toggleMaximize(panelApi) {
		if (panelApi.location?.type === 'floating') {
			return;
		}
		if (panelApi.isMaximized()) {
			panelApi.exitMaximized();
		} else {
			beforeMaximize = api?.toJSON() ?? null;
			panelApi.maximize();
		}
	}

	/** the views open in the layout, and the visible ones, which the Studio follows */
	function updateState() {
		if (stateQueued) {
			return;
		}
		stateQueued = true;
		queueMicrotask(() => {
			stateQueued = false;
			if (!api || loading) {
				return;
			}
			for (const group of api.groups) {
				// a hidden group keeps no border in the grid
				group.element?.classList.toggle('studio-dock-group--hidden', !group.api.isVisible);
			}
			const nextOpen = /** @type {Record<string, boolean>} */ ({});
			const nextVisible = /** @type {Record<string, boolean>} */ ({});
			const maximized = api.hasMaximizedGroup();
			for (const panel of api.panels) {
				const id = panel.api.component;
				nextOpen[id] = true;
				const group = panel.group;
				const floating = ['floating', 'popout'].includes(group?.api?.location?.type);
				if (
					group?.activePanel === panel &&
					(floating || group.api.isVisible) &&
					(!maximized || floating || group.api.isMaximized())
				) {
					nextVisible[id] = true;
					if (!shown[id]) {
						shown = { ...shown, [id]: true };
					}
				}
			}
			for (const id of Object.keys(shown)) {
				if (!nextOpen[id]) {
					const { [id]: _closed, ...rest } = shown;
					shown = rest;
				}
			}
			if (!sameKeys(open, nextOpen)) {
				open = nextOpen;
			}
			if (!sameKeys(visible, nextVisible)) {
				visible = nextVisible;
			}
		});
	}

	/**
	 * @param {Record<string, boolean>} left
	 * @param {Record<string, boolean>} right
	 */
	function sameKeys(left, right) {
		const keys = Object.keys(left);
		return keys.length === Object.keys(right).length && keys.every((key) => right[key]);
	}

	function layoutKey(name = layoutProfile) {
		return `${storageKey}.${name}${narrow ? '.narrow' : ''}`;
	}

	function saveLayout() {
		clearTimeout(saveTimer);
		saveTimer = setTimeout(() => {
			if (!api || loading || !layoutProfile) {
				return;
			}
			try {
				localStorage.setItem(layoutKey(), JSON.stringify(api.toJSON()));
			} catch {
				// a browser without storage keeps the layout until the page is left
			}
		}, 300);
	}

	/** @param {string} name */
	function readLayout(name) {
		try {
			const raw = localStorage.getItem(layoutKey(name));
			return raw ? withoutPopouts(JSON.parse(raw)) : null;
		} catch {
			return null;
		}
	}

	/**
	 * A browser does not open windows without a click: the views of the windows of their own float in the
	 * Studio instead, which the desktop Studio opens again.
	 * @param {any} layout
	 */
	function withoutPopouts(layout) {
		if (desktop || !layout?.popoutGroups?.length) {
			return layout;
		}
		const floating = layout.popoutGroups
			.filter((/** @type {any} */ popout) => popout.data || popout.grid)
			.map((/** @type {any} */ popout, /** @type {number} */ index) => ({
				...(popout.grid ? { grid: popout.grid } : { data: popout.data }),
				position: {
					left: 48 + index * 32,
					top: 48 + index * 32,
					width: Math.min(popout.position?.width ?? 480, 720),
					height: Math.min(popout.position?.height ?? 360, 540)
				}
			}));
		const { popoutGroups: _popouts, ...rest } = layout;
		return { ...rest, floatingGroups: [...(layout.floatingGroups ?? []), ...floating] };
	}

	/**
	 * Lays out the views of a profile as its default layout says.
	 * @param {string} name
	 */
	function buildDefaultLayout(name) {
		if (!api) {
			return;
		}
		api.clear();
		if (container?.clientWidth) {
			// the sizes of the sides are taken on the size of the dock
			api.layout(container.clientWidth, container.clientHeight);
		}
		const layout = layouts[name] ?? layouts.backend;
		const first = /** @type {Record<string, any>} */ ({});
		for (const area of narrow ? NARROW_AREAS : AREAS) {
			// a view closed at first comes in its area once shown
			const ids = areaViews(area, layout).filter((id) => !layout?.closed?.includes(id));
			ids.forEach((id, index) => {
				const view = viewOf(id);
				/** @type {any} */
				const options = {
					id,
					component: id,
					tabComponent: id,
					title: view?.title ?? id,
					renderer: 'always',
					inactive: index > 0
				};
				if (index > 0) {
					options.position = { referencePanel: ids[0], direction: 'within' };
				} else if (api?.panels.length) {
					options.position = { direction: areaDirection(area) };
				}
				const panel = api?.addPanel(options);
				if (index === 0) {
					first[area] = panel;
				}
			});
		}
		const height = container?.clientHeight ?? 0;
		for (const area of /** @type {DockArea[]} */ (['left', 'right', 'bottom'])) {
			const size = narrow
				? Math.round(height * (area === 'bottom' ? 0.3 : 0.28))
				: (layout?.sizes?.[area] ?? DEFAULT_SIZES[area]);
			first[area]?.group?.api.setSize(
				narrow || area === 'bottom' ? { height: size } : { width: size }
			);
		}
		for (const id of layout?.active ?? []) {
			api.getPanel(id)?.api.setActive();
		}
		for (const area of layout?.hidden ?? []) {
			first[area]?.group?.api.setVisible(false);
		}
		first.center?.api.setActive();
	}

	/** @param {string} name */
	function loadLayout(name) {
		if (!api) {
			return;
		}
		loading = true;
		try {
			if (container?.clientWidth) {
				// the floating groups are placed in the size of the dock
				api.layout(container.clientWidth, container.clientHeight);
			}
			const saved = readLayout(name);
			let restored = false;
			if (saved) {
				try {
					api.fromJSON(saved);
					restored = api.panels.length > 0;
				} catch (error) {
					console.warn('Unable to restore the layout of the Studio views', error);
				}
			}
			if (!restored) {
				buildDefaultLayout(name);
			}
			// a view the Studio no longer has leaves the layout
			for (const panel of [...api.panels]) {
				if (!viewOf(panel.api.component)) {
					panel.api.close();
				}
			}
			layoutProfile = name;
			closedBeside.clear();
		} finally {
			loading = false;
		}
		updateState();
	}

	/**
	 * Takes the layout of another profile, the current one being kept for it.
	 * @param {string} name
	 */
	export function setProfile(name) {
		if (!api || name === layoutProfile) {
			return;
		}
		if (layoutProfile) {
			clearTimeout(saveTimer);
			try {
				localStorage.setItem(layoutKey(), JSON.stringify(api.toJSON()));
			} catch {
				// the layout is not kept
			}
		}
		loadLayout(name);
	}

	/** Lays out the views of the profile as they were at first. */
	export function resetLayout() {
		if (!api) {
			return;
		}
		try {
			localStorage.removeItem(layoutKey());
		} catch {
			// nothing was kept
		}
		loading = true;
		try {
			api.exitMaximizedGroup();
			buildDefaultLayout(layoutProfile);
		} finally {
			loading = false;
		}
		updateState();
		saveLayout();
	}

	/**
	 * Shows a view: its tab is selected, its group shown again, a closed view comes back where it was.
	 * @param {string} id
	 */
	export function show(id) {
		if (!api || !viewOf(id)) {
			return;
		}
		let panel = api.getPanel(id);
		if (!panel) {
			panel = addBack(id);
		}
		if (!panel) {
			return;
		}
		if (api.hasMaximizedGroup() && !panel.group?.api.isMaximized()) {
			api.exitMaximizedGroup();
		}
		if (panel.group && !panel.group.api.isVisible) {
			panel.group.api.setVisible(true);
		}
		panel.api.setActive();
		api
			.getPopouts()
			.find((popout) => popout.group === panel.group)
			?.window.focus();
		updateState();
	}

	/**
	 * Hides the group of a view, as the activity bar hides a side of the Studio: the group keeps its
	 * place and its size, which it takes again when it shows.
	 * @param {string} id
	 */
	export function hide(id) {
		const panel = api?.getPanel(id);
		if (!panel?.group) {
			return;
		}
		if (panel.group.api.location?.type === 'popout') {
			movePopoutBack(panel.group);
		} else if (panel.group.api.location?.type === 'floating') {
			panel.api.close();
		} else if (panel.group.api.isMaximized()) {
			panel.group.api.exitMaximized();
		} else if (api && api.groups.filter((group) => group.api.isVisible).length > 1) {
			panel.group.api.setVisible(false);
		}
		updateState();
	}

	/**
	 * Shows a view, or hides its group when it is visible, as an item of the activity bar.
	 * @param {string} id
	 */
	export function toggle(id) {
		if (visible[id]) {
			hide(id);
		} else {
			show(id);
		}
	}

	/** @param {string} id */
	export function close(id) {
		api?.getPanel(id)?.api.close();
	}

	/**
	 * A view comes back beside the one it was grouped with, or in the area of the default layout.
	 * @param {string} id
	 */
	function addBack(id) {
		if (!api) {
			return undefined;
		}
		const view = viewOf(id);
		/** @type {any} */
		const options = {
			id,
			component: id,
			tabComponent: id,
			title: view?.title ?? id,
			renderer: 'always'
		};
		const beside = closedBeside.get(id);
		const layout = layouts[layoutProfile] ?? layouts.backend;
		const area = AREAS.find((name) => areaViews(name, layout).includes(id)) ?? 'center';
		const reference =
			(beside && api.getPanel(beside)) ||
			areaViews(area, layout)
				.map((other) => api?.getPanel(other))
				.find(Boolean);
		if (reference) {
			options.position = { referencePanel: reference.id, direction: 'within' };
		} else if (api.panels.length && (narrow || area !== 'center')) {
			options.position = { direction: areaDirection(area) };
			if (narrow || area === 'bottom') {
				options.initialHeight = narrow
					? Math.round((container?.clientHeight ?? 600) * 0.3)
					: DEFAULT_SIZES.bottom;
			} else {
				options.initialWidth = DEFAULT_SIZES[area];
			}
		}
		return api.addPanel(options);
	}

	/**
	 * Where an area goes in the dock: beside the work area on a large screen, over or under it on a
	 * small one.
	 * @param {DockArea} area
	 */
	function areaDirection(area) {
		return narrow
			? NARROW_DIRECTIONS[area]
			: AREA_DIRECTIONS[/** @type {'left' | 'right' | 'bottom'} */ (area)];
	}

	$effect(() => {
		// the alerts of the views, on their tabs
		for (const view of views) {
			const tab = tabs.get(view.id);
			if (tab) {
				tab.alert = view.alert ?? '';
				tab.detail = view.detail ?? '';
			}
		}
	});

	$effect(() => {
		// a profile chosen before the dock was laid out, as the one the preferences keep
		const name = profile;
		if (api) {
			setProfile(name);
		}
	});

	onMount(() => {
		if (!container) {
			return;
		}
		api = createDockview(container, {
			className: 'studio-dock-theme',
			theme: {
				name: 'studio',
				className: 'studio-dock-theme',
				gap: 6,
				dndOverlayMounting: 'absolute',
				dndPanelOverlay: 'group'
			},
			defaultRenderer: 'always',
			createComponent: createContent,
			createTabComponent: createTab,
			createRightHeaderActionComponent: createGroupActions,
			floatingGroupBounds: 'boundedWithinViewport',
			popoutUrl: POPOUT_URL,
			getTabContextMenuItems: ({ panel }) =>
				/** @type {import('dockview').ContextMenuItem[]} */ ([
					'close',
					'closeOthers',
					'separator',
					...(panel.api.location?.type === 'popout'
						? [{ label: 'Move back to the Studio', action: () => movePopoutBack(panel.group) }]
						: [
								...(panel.api.location?.type === 'floating'
									? []
									: [
											{
												label: panel.api.isMaximized() ? 'Restore' : 'Maximize',
												action: () => toggleMaximize(panel.api)
											},
											'float'
										]),
								{ label: 'Move to a new window', action: () => void popoutGroup(panel) }
							])
				])
		});
		const subscriptions = [
			api.onDidLayoutChange(() => {
				updateState();
				saveLayout();
			}),
			api.onDidActivePanelChange(updateState),
			api.onDidAddPanel(updateState),
			api.onDidAddPopoutGroup((popout) => {
				preparePopout(popout.window);
				updateState();
			}),
			api.onDidRemovePopoutGroup(updateState),
			api.onDidMaximizedGroupChange((event) => {
				if (!event.isMaximized) {
					// once the grid has shown the groups again, and has done what exited the maximized view
					requestAnimationFrame(restoreSizes);
				}
				updateState();
			}),
			api.onDidRemovePanel((panel) => {
				// the view it was grouped with, where it comes back
				const other = panel.group?.panels.find((/** @type {any} */ p) => p !== panel);
				if (other) {
					closedBeside.set(panel.id, other.id);
				}
				updateState();
			})
		];
		const narrowScreen = window.matchMedia(NARROW_SCREEN);
		narrow = narrowScreen.matches;
		const changeScreen = () => {
			// the layout of the other size of screen, the current one being kept
			if (narrowScreen.matches === narrow || !api) {
				return;
			}
			clearTimeout(saveTimer);
			try {
				localStorage.setItem(layoutKey(), JSON.stringify(api.toJSON()));
			} catch {
				// the layout is not kept
			}
			narrow = narrowScreen.matches;
			const name = layoutProfile;
			layoutProfile = '';
			loadLayout(name);
		};
		narrowScreen.addEventListener('change', changeScreen);
		loadLayout(profile);
		return () => {
			narrowScreen.removeEventListener('change', changeScreen);
			subscriptions.forEach((subscription) => subscription.dispose());
			clearTimeout(saveTimer);
			api?.dispose();
			api = undefined;
		};
	});
</script>

<div class="studio-dock" bind:this={container}></div>

<!-- the views, which the dock shows in its panels, and their buttons -->
<div class="studio-dock-holder" bind:this={holder} hidden>
	{#each views as view (view.id)}
		<div
			class={[
				'studio-dock-view',
				view.main && 'studio-dock-view--main',
				view.scroll && 'studio-dock-view--scroll'
			]}
			data-dock-view={view.id}
			role="tabpanel"
			aria-label={view.title}
			bind:this={elements[view.id]}
		>
			{#if open[view.id] && (!view.lazy || shown[view.id])}
				{#if view.toolbar}
					<div
						class="studio-dock-view__toolbar"
						role="toolbar"
						aria-label={`${view.title} actions`}
					>
						{@render view.toolbar()}
					</div>
				{/if}
				<div class="studio-dock-view__body">
					{@render view.content()}
				</div>
			{/if}
		</div>
		{#if view.actions}
			<div class="studio-dock-view-actions" bind:this={actionElements[view.id]}>
				{@render view.actions()}
			</div>
		{/if}
	{/each}
</div>

{#if menuOpen}
	<div
		class="studio-dock-menu-backdrop"
		role="presentation"
		onclick={() => (menuOpen = false)}
	></div>
	<div class="studio-dock-menu" role="menu" aria-label="Views">
		{#each views as view (view.id)}
			<button
				type="button"
				role="menuitemcheckbox"
				aria-checked={Boolean(open[view.id])}
				class="studio-dock-menu__item layout-x-low"
				onclick={() => {
					if (open[view.id]) {
						close(view.id);
					} else {
						show(view.id);
					}
				}}
			>
				<span class="studio-dock-menu__check">
					{#if open[view.id]}<Ico icon="mdi:check" size={4} />{/if}
				</span>
				{#if view.icon}<Ico icon={view.icon} size={4} />{/if}
				<span>{view.title}</span>
			</button>
		{/each}
		<hr />
		<button
			type="button"
			role="menuitem"
			class="studio-dock-menu__item layout-x-low"
			onclick={() => {
				resetLayout();
				menuOpen = false;
			}}
		>
			<span class="studio-dock-menu__check"></span>
			<Ico icon="mdi:backup-restore" size={4} />
			<span>Reset the layout</span>
		</button>
	</div>
{/if}

<style>
	/* the views hidden in their group wait out of the dock, which does not scroll to them */
	.studio-dock {
		position: relative;
		width: 100%;
		height: 100%;
		min-width: 0;
		min-height: 0;
		overflow: clip;
	}

	.studio-dock-holder {
		display: none;
	}

	/* the content of a panel fills it, a view scrolls in it */
	:global(.studio-dock-panel),
	.studio-dock-view {
		width: 100%;
		height: 100%;
		min-width: 0;
		min-height: 0;
		overflow: hidden;
	}

	.studio-dock-view {
		display: flex;
		flex-direction: column;
		background: var(--studio-panel-bg);
	}

	.studio-dock-view__body {
		display: flex;
		min-height: 0;
		flex: 1;
		flex-direction: column;
		overflow: hidden;
	}

	.studio-dock-view__body > :global(*) {
		min-height: 0;
		flex: 1;
	}

	.studio-dock-view--scroll .studio-dock-view__body {
		display: block;
		overflow: auto;
	}

	/* the buttons of a view too many for the header of its group, as the actions of the projects */
	.studio-dock-view__toolbar {
		display: flex;
		min-height: 2rem;
		flex: none;
		flex-wrap: wrap;
		align-items: center;
		justify-content: flex-end;
		gap: 0.15rem;
		border-bottom: 1px solid var(--studio-line);
		padding: 0.15rem 0.35rem;
	}

	.studio-dock-view--main {
		--studio-panel-bg: var(--studio-main-bg);
		background: var(--studio-main-bg);
	}

	.studio-dock-view-actions {
		display: flex;
		align-items: center;
		gap: 0.15rem;
	}

	/* The groups are cards on the window, as the panels of the Studio: their tabs are the flat tabs of
	   Cursor, the visible one underlined with the accent */
	:global(.studio-dock-theme) {
		--dv-paneview-active-outline-color: var(--color-primary-500);
		--dv-tabs-and-actions-container-font-size: 0.72rem;
		--dv-tabs-and-actions-container-height: 2.5rem;
		--dv-drag-over-background-color: color-mix(in oklab, var(--color-primary-500) 18%, transparent);
		--dv-drag-over-border-color: var(--color-primary-500);
		--dv-drag-over-border: 1px solid var(--color-primary-500);
		--dv-tabs-container-scrollbar-color: var(--color-surface-400-600);
		--dv-icon-hover-background-color: var(--studio-hover-bg);
		--dv-floating-box-shadow: 0 10px 32px light-dark(rgb(0 0 0 / 0.18), rgb(0 0 0 / 0.55));
		--dv-floating-border: 1px solid var(--studio-line);
		--dv-group-view-background-color: var(--studio-panel-bg);
		--dv-tabs-and-actions-container-background-color: var(--studio-panel-header-bg);
		--dv-activegroup-visiblepanel-tab-background-color: transparent;
		--dv-activegroup-hiddenpanel-tab-background-color: transparent;
		--dv-inactivegroup-visiblepanel-tab-background-color: transparent;
		--dv-inactivegroup-hiddenpanel-tab-background-color: transparent;
		--dv-activegroup-visiblepanel-tab-color: var(--studio-text-strong);
		--dv-activegroup-hiddenpanel-tab-color: var(--studio-text-idle);
		--dv-inactivegroup-visiblepanel-tab-color: var(--studio-text-strong);
		--dv-inactivegroup-hiddenpanel-tab-color: var(--studio-text-idle);
		--dv-tab-divider-color: transparent;
		--dv-separator-border: transparent;
		--dv-paneview-header-border-color: var(--studio-line);
		--dv-sash-color: transparent;
		--dv-active-sash-color: var(--color-primary-500);
		--dv-active-sash-transition-duration: 0.1s;
		--dv-active-sash-transition-delay: 0.2s;
		--dv-border-radius: var(--studio-card-radius);
		--dv-tab-margin: 0;
		--dv-overlay-z-index: 40;
		--dv-floating-titlebar-height: 1.1rem;
		--dv-floating-titlebar-background-color: var(--studio-panel-header-bg);
		height: 100%;
		background: transparent;
		color: var(--studio-text);
		font-family: inherit;
	}

	:global(.studio-dock-theme .dv-groupview) {
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: var(--studio-card-radius);
		background: var(--studio-panel-bg);
		box-shadow: 0 1px 2px light-dark(rgb(0 0 0 / 0.06), rgb(0 0 0 / 0.4));
	}

	:global(.studio-dock-theme .dv-groupview.studio-dock-group--hidden) {
		border: 0;
		box-shadow: none;
	}

	:global(.studio-dock-tab-host) {
		display: flex;
		height: 100%;
		align-items: center;
	}

	:global(.studio-dock-theme .dv-groupview.studio-dock-group--main) {
		background: var(--studio-main-bg);
	}

	:global(.studio-dock-theme .dv-render-overlay) {
		overflow: hidden;
		border-bottom-left-radius: var(--studio-card-radius);
		border-bottom-right-radius: var(--studio-card-radius);
	}

	:global(.studio-dock-theme .dv-tabs-and-actions-container) {
		padding: 0 0.35rem;
		border-bottom: 0;
	}

	:global(.studio-dock-theme .dv-tab) {
		position: relative;
		padding: 0 0.25rem;
	}

	:global(.studio-dock-theme .dv-tab.dv-active-tab::after) {
		position: absolute;
		right: 0.5rem;
		bottom: 0.3rem;
		left: 0.5rem;
		height: 1px;
		background: var(--color-primary-500);
		content: '';
	}

	:global(.studio-dock-theme .dv-tab:focus-visible) {
		outline: 1px solid var(--color-primary-500);
		outline-offset: -2px;
	}

	:global(.studio-dock-theme .dv-resize-container) {
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: var(--studio-card-radius);
	}

	:global(.studio-dock-actions) {
		display: flex;
		height: 100%;
		align-items: center;
		gap: 0.15rem;
		padding-right: 0.15rem;
	}

	:global(.studio-dock-actions__view) {
		display: flex;
		align-items: center;
	}

	:global(.studio-dock-actions__button) {
		display: grid;
		width: 1.6rem;
		height: 1.6rem;
		place-items: center;
		border: 0;
		border-radius: 0.3rem;
		background: transparent;
		color: var(--studio-text-idle);
		padding: 0;
	}

	:global(.studio-dock-actions__button[hidden]) {
		display: none;
	}

	:global(.studio-dock-actions__button:hover),
	:global(.studio-dock-actions__button:focus-visible) {
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

	.studio-dock-menu-backdrop {
		position: fixed;
		z-index: 60;
		inset: 0;
	}

	.studio-dock-menu {
		position: fixed;
		z-index: 61;
		bottom: 4.5rem;
		left: 3.25rem;
		display: flex;
		min-width: 14rem;
		max-height: calc(100vh - 6rem);
		flex-direction: column;
		overflow: auto;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-panel-bg);
		box-shadow: 0 10px 32px light-dark(rgb(0 0 0 / 0.18), rgb(0 0 0 / 0.55));
		padding: 0.3rem;
	}

	.studio-dock-menu hr {
		margin: 0.3rem 0;
		border: 0;
		border-top: 1px solid var(--studio-line);
	}

	.studio-dock-menu__item {
		gap: 0.5rem;
		border: 0;
		border-radius: 0.3rem;
		background: transparent;
		color: var(--studio-text);
		padding: 0.35rem 0.6rem 0.35rem 0.35rem;
		font-size: 0.8rem;
		text-align: left;
	}

	.studio-dock-menu__item:hover,
	.studio-dock-menu__item:focus-visible {
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

	.studio-dock-menu__check {
		display: inline-grid;
		width: 1rem;
		place-items: center;
	}
</style>
