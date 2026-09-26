<script>
	import SaveCancelButtons from '$lib/admin/components/SaveCancelButtons.svelte';
	import { createDatabaseObjectProperties } from '$lib/common/DatabaseObjectProperties.svelte.js';
	import LightSvelte from '$lib/common/Light.svelte';
	import Editor from '$lib/studio/editor/Editor.svelte';
	import {
		asEditorValue,
		findPrimaryEditorProperty,
		getPropertyLanguage
	} from '$lib/studio/propertyEditors';
	import Ico from '$lib/utils/Ico.svelte';
	import { call } from '$lib/utils/service';
	import { untrack } from 'svelte';
	import { debugSession, followDebugger } from './debugSession.svelte.js';
	import StudioEmptyState from './StudioEmptyState.svelte';
	import StudioIconButton from './StudioIconButton.svelte';

	/** the code documents of an NGX component: its class, the function of an action, a template, a style */
	const COMPONENT_CODE = /#(class|action|html|style)$/;

	/**
	 * @typedef {Object} EditorTab
	 * @property {string} key
	 * @property {string} id
	 * @property {string} propertyName
	 * @property {string} displayName
	 * @property {string} content
	 * @property {string} originalValue
	 * @property {string} language
	 * @property {boolean=} readOnly
	 * @property {boolean=} sourceDocument
	 * @property {string=} revision
	 * @property {boolean=} focused
	 * @property {number[]=} breakpoints
	 * @property {string=} scriptUrl the script of the property, as the debugger names it
	 * @property {number=} scriptOffset the lines the engine adds before the property
	 * @property {number=} revealLine a line to show, as a line a search found
	 * @property {number=} revealSerial when the line was asked, the editor shows it once for each
	 */

	/**
	 * @type {{
	 *  selectedId?: string,
	 *  editorTarget?: { id?: string, propertyName?: string, displayName?: string, value?: any, persisted?: boolean, sourceDocument?: boolean, serial?: number, line?: number } | null,
	 *  active?: boolean,
	 *  onSave?: (id: string, result?: any) => void | Promise<void>,
	 *  onMutationBusyChange?: (busy: boolean, handled?: boolean) => void,
	 *  onSelectObject?: (id: string) => void
	 * }}
	 */
	let {
		selectedId = '',
		editorTarget = null,
		active = false,
		onSave,
		onMutationBusyChange = () => {},
		onSelectObject = () => {}
	} = $props();

	let { properties, onSelectionChange, save, cancel } = $derived(createDatabaseObjectProperties());
	let loading = $state(false);
	let saving = $state(false);
	let error = $state('');
	let loadedId = $state('');
	let loadToken = 0;
	let fullscreen = $state(false);
	let lastOpenRequest = '';
	let lastSourceRequest = '';
	/** @type {EditorTab[]} */
	let editorTabs = $state([]);
	let activeTabKey = $state('');

	let requestedProperty = $derived(
		!editorTarget?.sourceDocument && loadedId === selectedId
			? findEditorProperty(properties, selectedId, editorTarget)
			: undefined
	);
	let requestedPropertyKey = $derived(
		requestedProperty && selectedId
			? [
					selectedId,
					requestedProperty.name ?? requestedProperty.displayName,
					editorTarget?.id === selectedId
						? (editorTarget?.propertyName ?? editorTarget?.displayName ?? '')
						: '',
					editorTarget?.id === selectedId ? (editorTarget?.serial ?? '') : ''
				].join(':')
			: ''
	);
	let activeTab = $derived(editorTabs.find((tab) => tab.key === activeTabKey) ?? null);
	let activeTabDirty = $derived(
		Boolean(activeTab && !activeTab.readOnly && activeTab.content !== activeTab.originalValue)
	);
	let theme = $derived(LightSvelte.light ? '' : 'vs-dark');
	let debuggable = $derived(Boolean(activeTab && isDebuggable(activeTab)));
	let debuggerAttached = $derived(debugSession.state?.attached !== false);
	let currentLine = $derived.by(() => {
		// the line where the debugger stopped in the script of the tab
		const frame = debugSession.state?.stopped
			? debugSession.state.frames?.find((frame) => frame.url === activeTab?.scriptUrl)
			: undefined;
		return frame ? frame.line - (activeTab?.scriptOffset ?? 0) : 0;
	});
	let breakpointNotice = $state('');
	let canSave = $derived(Boolean(activeTab && activeTabDirty && !loading && !saving));

	$effect(() => {
		const nextId = selectedId;
		if (!active || !nextId || editorTarget?.sourceDocument) {
			return;
		}
		untrack(() => {
			void loadProperties(nextId);
		});
	});

	$effect(() => {
		// the source of the selected object, or the TypeScript class of the selected NGX component
		const sourceId =
			active &&
			editorTarget?.sourceDocument &&
			(editorTarget?.id === selectedId ||
				editorTarget?.id?.replace(COMPONENT_CODE, '') === selectedId)
				? editorTarget.id
				: '';
		const requestKey = sourceId ? `${sourceId}:${editorTarget?.serial ?? ''}` : '';
		if (!requestKey || requestKey === lastSourceRequest) {
			return;
		}
		lastSourceRequest = requestKey;
		untrack(() => {
			void openSourceDocument(sourceId, editorTarget?.line ?? 0);
		});
	});

	$effect(() => {
		const requestKey = requestedPropertyKey;
		const property = requestedProperty;
		if (!active || !requestKey || requestKey === lastOpenRequest || !property) {
			return;
		}
		lastOpenRequest = requestKey;
		untrack(() => {
			openEditorTab(selectedId, property, editorTarget);
		});
	});

	$effect(() => {
		// the breakpoints of the script, which the debug panel may have changed
		const tab = active && activeTab && isDebuggable(activeTab) ? activeTab : null;
		const key = tab?.key;
		breakpointNotice = '';
		if (tab && key) {
			untrack(() => void loadBreakpoints(tab));
		}
	});

	/**
	 * The JavaScript properties of the objects the engine runs, sequences and connectors, can hold
	 * breakpoints of the debugger.
	 * @param {EditorTab} tab
	 */
	function isDebuggable(tab) {
		return !tab.sourceDocument && tab.language === 'javascript' && /\.(sq|cn):/.test(tab.id);
	}

	/**
	 * @param {EditorTab} tab
	 * @param {Record<string, string>} [params]
	 */
	async function callDebugger(tab, params = { action: 'breakpoints' }) {
		const result = await call('studio.debug.Debugger', {
			...params,
			id: tab.id,
			property: tab.propertyName
		});
		if (Array.isArray(result?.breakpoints)) {
			tab.breakpoints = result.breakpoints.map(Number);
		}
		if (result?.url) {
			tab.scriptUrl = String(result.url);
			tab.scriptOffset = Number(result.offset) || 0;
		}
		followDebugger(result?.state);
		return result;
	}

	/**
	 * @param {EditorTab} tab
	 */
	async function loadBreakpoints(tab) {
		try {
			await callDebugger(tab);
		} catch {
			tab.breakpoints = [];
		}
	}

	/**
	 * Sets or removes a breakpoint, even before the first run of the script.
	 * @param {number} line
	 */
	async function toggleBreakpoint(line) {
		const tab = activeTab;
		if (!tab) {
			return;
		}
		const result = await callDebugger(tab, {
			action: 'breakpoint',
			line: String(line),
			set: String(!(tab.breakpoints ?? []).includes(line))
		});
		breakpointNotice = result?.accepted === false ? `No breakpoint can stop at line ${line}` : '';
	}

	/**
	 * Selects in the tree the object of a tab, the NGX component of a TypeScript class.
	 * @param {string} id
	 */
	function selectObject(id) {
		onSelectObject(id.replace(COMPONENT_CODE, ''));
	}

	/**
	 * @param {string} objectId
	 * @returns {Promise<boolean>}
	 */
	async function loadProperties(objectId) {
		if (!objectId) {
			return false;
		}
		if (objectId === loadedId) {
			return true;
		}
		const token = ++loadToken;
		error = '';
		loading = true;
		try {
			await onSelectionChange({ selectedValue: [objectId] });
			if (token !== loadToken) {
				return false;
			}
			loadedId = objectId;
			return true;
		} catch (err) {
			if (token === loadToken) {
				error = String(err instanceof Error ? err.message : err);
				loadedId = '';
			}
			return false;
		} finally {
			if (token === loadToken) {
				loading = false;
			}
		}
	}

	/**
	 * @param {string} objectId
	 * @param {any} property
	 * @param {{ id?: string, value?: any, persisted?: boolean } | null} target
	 */
	function openEditorTab(objectId, property, target) {
		const propertyName = String(property?.name ?? property?.displayName ?? 'value');
		const displayName = String(property?.displayName ?? property?.name ?? propertyName);
		const key = createTabKey(objectId, propertyName);
		// a value the engine now holds, as handlers just added: it is the saved value of the tab
		const persisted =
			target?.id === objectId && target.persisted && target.value !== undefined
				? asEditorValue(target.value)
				: undefined;
		closeUnfocusedEditorTabs(key);
		const existing = editorTabs.find((tab) => tab.key === key);
		if (existing) {
			if (persisted !== undefined && existing.content === existing.originalValue) {
				existing.content = persisted;
				existing.originalValue = persisted;
			}
			activeTabKey = existing.key;
			selectObject(existing.id);
			return;
		}

		const initialValue = getInitialEditorValue(property, target, objectId);
		const content = asEditorValue(initialValue);
		editorTabs.push({
			key,
			id: objectId,
			propertyName,
			displayName,
			content,
			originalValue: persisted ?? asEditorValue(property?.value),
			language: getPropertyLanguage({ ...property, value: content }, objectId),
			focused: false
		});
		activeTabKey = key;
		selectObject(objectId);
	}

	/**
	 * Opens the source document resolved by the Engine from the selected Flow
	 * virtual object. The browser never receives or submits an arbitrary path.
	 * @param {string} objectId
	 */
	/**
	 * @param {string} objectId
	 * @param {number} [line] a line to show, as a line a search found
	 */
	async function openSourceDocument(objectId, line = 0) {
		loading = true;
		error = '';
		try {
			const response = await call('studio.source.Get', { id: objectId });
			const displayName = String(response?.fileName ?? 'Flow source');
			const relativePath = String(response?.relativePath ?? displayName);
			const key = createTabKey(objectId, `source:${relativePath}`);
			closeUnfocusedEditorTabs(key);
			const existing = editorTabs.find((tab) => tab.key === key);
			if (existing) {
				existing.content = String(response?.content ?? '');
				existing.originalValue = existing.content;
				existing.revision = String(response?.revision ?? '');
				existing.language = String(response?.language ?? 'text');
				if (line) {
					existing.revealLine = line;
					existing.revealSerial = Date.now();
				}
				activeTabKey = existing.key;
				return;
			}
			const content = String(response?.content ?? '');
			editorTabs.push({
				key,
				id: objectId,
				propertyName: `source:${relativePath}`,
				displayName,
				content,
				originalValue: content,
				language: String(response?.language ?? 'text'),
				readOnly: response?.readOnly !== false,
				sourceDocument: true,
				revision: String(response?.revision ?? ''),
				focused: false,
				...(line ? { revealLine: line, revealSerial: Date.now() } : {})
			});
			activeTabKey = key;
			selectObject(objectId);
		} catch (err) {
			error = String(err instanceof Error ? err.message : err);
		} finally {
			loading = false;
		}
	}

	/**
	 * @param {EditorTab} tab
	 */
	function selectEditorTab(tab) {
		tab.focused = true;
		activeTabKey = tab.key;
		selectObject(tab.id);
	}

	/**
	 * @param {string} nextKey
	 */
	function closeUnfocusedEditorTabs(nextKey) {
		for (let index = editorTabs.length - 1; index >= 0; index -= 1) {
			const tab = editorTabs[index];
			if (tab.key !== nextKey && !tab.focused && tab.content === tab.originalValue) {
				editorTabs.splice(index, 1);
			}
		}
		if (!editorTabs.some((tab) => tab.key === activeTabKey)) {
			activeTabKey = editorTabs[0]?.key ?? '';
		}
	}

	function markActiveTabFocused() {
		const tab = activeTab;
		if (tab) {
			tab.focused = true;
		}
	}

	/**
	 * @param {EditorTab} tab
	 * @returns {boolean}
	 */
	function canCloseEditorTab(tab) {
		if (tab.content === tab.originalValue) {
			return true;
		}
		return window.confirm(`Discard unsaved changes in ${tab.displayName}?`);
	}

	/**
	 * @param {MouseEvent} event
	 * @param {string} key
	 */
	function closeEditorTab(event, key) {
		event.stopPropagation();
		const index = editorTabs.findIndex((tab) => tab.key === key);
		if (index < 0) {
			return;
		}
		if (!canCloseEditorTab(editorTabs[index])) {
			return;
		}
		const wasActive = activeTabKey === key;
		editorTabs.splice(index, 1);
		if (wasActive) {
			const nextTab = editorTabs[Math.min(index, editorTabs.length - 1)] ?? editorTabs.at(-1);
			activeTabKey = nextTab?.key ?? '';
			if (nextTab) {
				selectObject(nextTab.id);
			}
		}
	}

	async function saveEditor() {
		const tab = activeTab;
		if (!tab || tab.readOnly || !activeTabDirty) {
			return;
		}
		if (tab.sourceDocument) {
			await saveSourceDocument(tab);
			return;
		}
		saving = true;
		let handled = false;
		onMutationBusyChange(true);
		if (tab.sourceDocument) {
			// A Flow source is stored as a working copy of its FlowEngine; the
			// project Save writes it like any other Flow source.
			try {
				const response = await call('studio.source.Set', {
					id: tab.id,
					content: tab.content,
					revision: tab.revision ?? ''
				});
				tab.revision = String(response?.revision ?? '');
				tab.originalValue = tab.content;
				await onSave?.(tab.id, { id: tab.id });
				handled = true;
			} catch (err) {
				error = String(err instanceof Error ? err.message : err);
			} finally {
				saving = false;
				onMutationBusyChange(false, handled);
			}
			return;
		}
		try {
			const loaded = await loadProperties(tab.id);
			if (!loaded) {
				return;
			}
			const row = findEditorProperty(properties, tab.id, {
				id: tab.id,
				propertyName: tab.propertyName,
				displayName: tab.displayName
			});
			if (!row) {
				error = `No editable property found for ${tab.displayName}`;
				return;
			}
			row.value = tab.content;
			if (
				!(await save({
					persist: false,
					onSaved: async (savedId, result) => {
						await onSave?.(savedId, result);
						handled = true;
						const nextId = result.selectedId || result.id || savedId;
						tab.id = nextId;
						tab.key = createTabKey(nextId, tab.propertyName);
						activeTabKey = tab.key;
					}
				}))
			) {
				return;
			}
			tab.originalValue = tab.content;
			tab.language = getPropertyLanguage({ ...row, value: tab.content }, tab.id);
		} finally {
			saving = false;
			onMutationBusyChange(false, handled);
		}
	}

	/**
	 * Saves a text file of a project, if it did not change on the disk since it was opened.
	 * @param {EditorTab} tab
	 */
	async function saveSourceDocument(tab) {
		saving = true;
		error = '';
		try {
			const result = await call('studio.source.Set', {
				id: tab.id,
				content: tab.content,
				revision: tab.revision ?? ''
			});
			if (result?.done) {
				tab.originalValue = tab.content;
				tab.revision = String(result.revision ?? '');
				if (result.changed) {
					// the class of a component keeps its code in the component, whose project changes
					await onSave?.(tab.id.replace(COMPONENT_CODE, ''));
				}
			} else {
				error = String(result?.error?.message ?? result?.message ?? 'The file was not saved.');
			}
		} finally {
			saving = false;
		}
	}

	function cancelEditor() {
		const tab = activeTab;
		if (!tab) {
			return;
		}
		if (loadedId === tab.id) {
			cancel();
		}
		tab.content = tab.originalValue;
	}

	/**
	 * @param {any[]} rows
	 * @param {string} objectId
	 * @param {{ id?: string, propertyName?: string, displayName?: string } | null} target
	 * @returns {any}
	 */
	function findEditorProperty(rows, objectId, target) {
		const requestedName =
			target?.id === objectId
				? normalizePropertyName(target.propertyName ?? target.displayName)
				: '';
		if (requestedName) {
			const requested = rows.find((row) =>
				[row?.name, row?.displayName].some((name) => normalizePropertyName(name) === requestedName)
			);
			if (requested) {
				return requested;
			}
		}
		return findPrimaryEditorProperty(rows, objectId);
	}

	/**
	 * @param {string} objectId
	 * @param {string} propertyName
	 * @returns {string}
	 */
	function createTabKey(objectId, propertyName) {
		return `${objectId}:${normalizePropertyName(propertyName)}`;
	}

	/**
	 * @param {any} value
	 * @returns {string}
	 */
	function normalizePropertyName(value) {
		return String(value ?? '')
			.trim()
			.toLowerCase();
	}

	/**
	 * @param {any} property
	 * @param {{ id?: string, value?: any } | null} target
	 * @param {string} objectId
	 * @returns {any}
	 */
	function getInitialEditorValue(property, target, objectId) {
		if (target?.id === objectId && target.value !== undefined) {
			return target.value;
		}
		return property?.value;
	}
</script>

<div class="studio-editor" class:studio-editor--fullscreen={fullscreen}>
	{#if editorTabs.length > 0}
		<div class="studio-editor__tabs layout-x-none" role="tablist" aria-label="Open editors">
			{#each editorTabs as tab (tab.key)}
				<div class="studio-editor__tab" class:studio-editor__tab--active={tab.key === activeTabKey}>
					<button
						type="button"
						role="tab"
						aria-selected={tab.key === activeTabKey}
						class="studio-editor__tab-main"
						title={`${tab.displayName} - ${tab.id}`}
						onclick={() => selectEditorTab(tab)}
					>
						<span class="studio-editor__tab-dirty"
							>{tab.content !== tab.originalValue ? '*' : ''}</span
						>
						<span class="studio-editor__tab-label studio-ellipsis">{tab.displayName}</span>
					</button>
					<button
						type="button"
						class="studio-editor__tab-close"
						aria-label={`Close ${tab.displayName}`}
						title={tab.content !== tab.originalValue
							? 'Close editor - unsaved changes'
							: 'Close editor'}
						onclick={(event) => closeEditorTab(event, tab.key)}
					>
						<Ico icon="mdi:close" size={3.4} />
					</button>
				</div>
			{/each}
		</div>
	{/if}

	{#if activeTab}
		<div class="studio-editor__toolbar layout-x-between-low">
			<div class="studio-editor__title">
				<strong class="studio-ellipsis">{activeTab.displayName}</strong>
				<span class="studio-ellipsis"
					>{activeTab.sourceDocument
						? activeTab.propertyName.replace(/^source:/, '')
						: activeTab.id}</span
				>
			</div>
			<div class="studio-editor__actions layout-x-low">
				{#if debuggable && breakpointNotice}
					<span class="studio-editor__readonly" role="status">{breakpointNotice}</span>
				{:else if debuggable && !debuggerAttached && activeTab.breakpoints?.length}
					<button
						type="button"
						class="studio-editor__readonly studio-editor__debugger"
						title="The breakpoints stop the scripts once the debugger runs"
						onclick={() => activeTab && callDebugger(activeTab, { action: 'start' })}
					>
						<Ico icon="mdi:bug-outline" size={3.4} />
						Start the debugger
					</button>
				{/if}
				{#if activeTab.readOnly}
					<span class="studio-editor__readonly" title="Library sources are shown read-only">
						<Ico icon="mdi:lock-outline" size={3.4} />
						Read-only source
					</span>
				{:else}
					<SaveCancelButtons
						class="w-fit"
						saveLabel="Apply"
						cancelLabel="Cancel"
						onSave={saveEditor}
						onCancel={cancelEditor}
						changesPending={activeTabDirty}
						disabled={!canSave}
					/>
				{/if}
				<StudioIconButton
					icon={fullscreen ? 'mdi:fullscreen-exit' : 'mdi:fullscreen'}
					size="md"
					title={fullscreen ? 'Exit fullscreen' : 'Enter fullscreen'}
					ariaLabel={fullscreen ? 'Exit fullscreen' : 'Enter fullscreen'}
					onclick={() => (fullscreen = !fullscreen)}
				/>
			</div>
		</div>
		<div class="studio-editor__monaco" onfocusin={markActiveTabFocused}>
			<Editor
				bind:content={activeTab.content}
				language={activeTab.language}
				{theme}
				readOnly={activeTab.readOnly === true}
				breakpoints={debuggable ? (activeTab.breakpoints ?? []) : null}
				onBreakpointToggle={toggleBreakpoint}
				currentLine={debuggable ? currentLine : 0}
				revealLine={activeTab.revealLine ?? 0}
				revealSerial={activeTab.revealSerial ?? 0}
			/>
		</div>
	{:else if loading}
		<StudioEmptyState message="Loading" loading full class="studio-editor__empty" />
	{:else if error}
		<StudioEmptyState message={error} full class="studio-editor__empty" />
	{:else if !selectedId}
		<StudioEmptyState message="No object selected" full class="studio-editor__empty" />
	{:else}
		<StudioEmptyState
			message="No text editor for this selection"
			icon="mdi:code-braces"
			full
			class="studio-editor__empty"
		/>
	{/if}
</div>

<style>
	.studio-editor {
		--studio-editor-bg: var(--studio-main-bg, var(--color-surface-50-950));
		--studio-editor-tabs-bg: var(--studio-chrome-bg, var(--color-surface-100-900));
		--studio-editor-tab-bg: transparent;
		--studio-editor-tab-active-bg: var(--studio-editor-bg);
		--studio-editor-text: var(--color-surface-950-50);
		--studio-editor-muted: var(--color-surface-600-400);
		display: grid;
		grid-template-rows: auto auto minmax(0, 1fr);
		height: 100%;
		min-height: 0;
		background: var(--studio-editor-bg);
	}

	.studio-editor--fullscreen {
		position: fixed;
		z-index: 140;
		inset: 0.65rem;
		height: auto;
		overflow: hidden;
		border: 1px solid var(--color-surface-600);
		border-radius: 0.45rem;
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 38%, transparent);
	}

	/* The tabs of the editors of Cursor: separated by lines, the active one on the background of the
	   code with a top line of the accent */
	.studio-editor__tabs {
		min-width: 0;
		overflow-x: auto;
		box-shadow: inset 0 -1px var(--studio-line, var(--color-surface-200-800));
		background: var(--studio-editor-tabs-bg);
		padding: 0;
	}

	.studio-editor__tab {
		display: grid;
		min-width: 7rem;
		max-width: 15rem;
		grid-template-columns: minmax(0, 1fr) auto;
		overflow: hidden;
		border: 0;
		border-right: 1px solid var(--studio-line, var(--color-surface-200-800));
		border-radius: 0;
		background: var(--studio-editor-tab-bg);
		color: var(--studio-text-idle, var(--studio-editor-text));
	}

	.studio-editor__tab--active {
		background: var(--studio-editor-tab-active-bg);
		box-shadow: inset 0 1px var(--color-primary-500);
		color: var(--studio-text-strong, var(--color-primary-700-300));
	}

	.studio-editor__tab-main,
	.studio-editor__tab-close {
		border: 0;
		background: transparent;
		color: inherit;
	}

	.studio-editor__tab-main {
		display: grid;
		min-width: 0;
		grid-template-columns: 0.55rem minmax(0, 1fr);
		align-items: center;
		gap: 0.2rem;
		padding: 0.55rem 0.18rem 0.55rem 0.6rem;
		text-align: left;
	}

	.studio-editor__tab-dirty {
		color: var(--color-primary-400);
		font-size: 0.82rem;
		line-height: 1;
		text-align: center;
	}

	.studio-editor__tab-label {
		font-size: 0.78rem;
		font-weight: 400;
	}

	.studio-editor__tab-close {
		display: grid;
		width: 1.6rem;
		place-items: center;
		opacity: 0.72;
	}

	.studio-editor__tab-close:hover {
		background: var(--studio-hover-bg, color-mix(in oklab, white 10%, transparent));
		opacity: 1;
	}

	.studio-editor__toolbar {
		border-bottom: 1px solid var(--studio-line, var(--color-surface-200-800));
		background: var(--studio-editor-bg);
		padding: 0.45rem 0.55rem;
	}

	.studio-editor__title {
		display: grid;
		min-width: 0;
		gap: 0.08rem;
		color: var(--studio-editor-text);
	}

	.studio-editor__title strong {
		font-size: 0.82rem;
	}

	.studio-editor__title span {
		color: var(--studio-editor-muted);
		font-size: 0.68rem;
	}

	.studio-editor__actions {
		flex: 0 0 auto;
	}

	.studio-editor__readonly {
		display: inline-flex;
		align-items: center;
		gap: 0.28rem;
		color: var(--studio-editor-muted);
		font-size: 0.7rem;
		white-space: nowrap;
	}

	.studio-editor__debugger {
		border: 0;
		border-radius: 0.3rem;
		background: transparent;
		padding: 0.2rem 0.35rem;
	}

	.studio-editor__debugger:hover {
		background: var(--studio-hover-bg);
		color: var(--studio-editor-text);
	}

	.studio-editor__monaco {
		min-height: 0;
	}

	.studio-editor :global(.studio-editor__empty) {
		grid-row: 1 / -1;
		background: var(--color-surface-50-950);
	}
</style>
