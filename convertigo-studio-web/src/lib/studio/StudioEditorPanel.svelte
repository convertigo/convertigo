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
	import { untrack } from 'svelte';
	import StudioEmptyState from './StudioEmptyState.svelte';
	import StudioIconButton from './StudioIconButton.svelte';

	/**
	 * @typedef {Object} EditorTab
	 * @property {string} key
	 * @property {string} id
	 * @property {string} propertyName
	 * @property {string} displayName
	 * @property {string} content
	 * @property {string} originalValue
	 * @property {string} language
	 * @property {boolean=} focused
	 */

	/**
	 * @type {{
	 *  selectedId?: string,
	 *  editorTarget?: { id?: string, propertyName?: string, displayName?: string, value?: any, serial?: number } | null,
	 *  active?: boolean,
	 *  onSave?: (id: string) => void | Promise<void>,
	 *  onSelectObject?: (id: string) => void
	 * }}
	 */
	let {
		selectedId = '',
		editorTarget = null,
		active = false,
		onSave,
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
	/** @type {EditorTab[]} */
	let editorTabs = $state([]);
	let activeTabKey = $state('');

	let requestedProperty = $derived(
		loadedId === selectedId ? findEditorProperty(properties, selectedId, editorTarget) : undefined
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
		Boolean(activeTab && activeTab.content !== activeTab.originalValue)
	);
	let theme = $derived(LightSvelte.light ? '' : 'vs-dark');
	let canSave = $derived(Boolean(activeTab && activeTabDirty && !loading && !saving));

	$effect(() => {
		const nextId = selectedId;
		if (!active || !nextId) {
			return;
		}
		untrack(() => {
			void loadProperties(nextId);
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
	 * @param {{ id?: string, value?: any } | null} target
	 */
	function openEditorTab(objectId, property, target) {
		const propertyName = String(property?.name ?? property?.displayName ?? 'value');
		const displayName = String(property?.displayName ?? property?.name ?? propertyName);
		const key = createTabKey(objectId, propertyName);
		closeUnfocusedEditorTabs(key);
		const existing = editorTabs.find((tab) => tab.key === key);
		if (existing) {
			activeTabKey = existing.key;
			onSelectObject(existing.id);
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
			originalValue: asEditorValue(property?.value),
			language: getPropertyLanguage({ ...property, value: content }, objectId),
			focused: false
		});
		activeTabKey = key;
		onSelectObject(objectId);
	}

	/**
	 * @param {EditorTab} tab
	 */
	function selectEditorTab(tab) {
		tab.focused = true;
		activeTabKey = tab.key;
		onSelectObject(tab.id);
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
				onSelectObject(nextTab.id);
			}
		}
	}

	async function saveEditor() {
		const tab = activeTab;
		if (!tab || !activeTabDirty) {
			return;
		}
		saving = true;
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
			if (!(await save())) {
				return;
			}
			tab.originalValue = tab.content;
			tab.language = getPropertyLanguage({ ...row, value: tab.content }, tab.id);
			await onSave?.(tab.id);
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
				<span class="studio-ellipsis">{activeTab.id}</span>
			</div>
			<div class="studio-editor__actions layout-x-low">
				<SaveCancelButtons
					class="w-fit"
					saveLabel="Save"
					cancelLabel="Cancel"
					onSave={saveEditor}
					onCancel={cancelEditor}
					changesPending={activeTabDirty}
					disabled={!canSave}
				/>
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
				readOnly={false}
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
		--studio-editor-bg: var(--color-surface-50-950);
		--studio-editor-tabs-bg: var(--color-surface-100-900);
		--studio-editor-tab-bg: var(--color-surface-50-950);
		--studio-editor-tab-active-bg: var(--color-surface-100-900);
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

	.studio-editor__tabs {
		min-width: 0;
		overflow-x: auto;
		border-bottom: 1px solid var(--color-surface-200-800);
		background: var(--studio-editor-tabs-bg);
		padding: 0.22rem;
	}

	.studio-editor__tab {
		display: grid;
		min-width: 7rem;
		max-width: 15rem;
		grid-template-columns: minmax(0, 1fr) auto;
		overflow: hidden;
		border: 1px solid transparent;
		border-radius: 0.28rem;
		background: var(--studio-editor-tab-bg);
		color: var(--studio-editor-text);
	}

	.studio-editor__tab--active {
		border-color: color-mix(in oklab, var(--color-primary-500) 55%, transparent);
		background: var(--studio-editor-tab-active-bg);
		color: var(--color-primary-700-300);
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
		padding: 0.34rem 0.18rem 0.34rem 0.45rem;
		text-align: left;
	}

	.studio-editor__tab-dirty {
		color: var(--color-primary-400);
		font-size: 0.82rem;
		line-height: 1;
		text-align: center;
	}

	.studio-editor__tab-label {
		font-size: 0.74rem;
		font-weight: 700;
	}

	.studio-editor__tab-close {
		display: grid;
		width: 1.6rem;
		place-items: center;
		opacity: 0.72;
	}

	.studio-editor__tab-close:hover {
		background: color-mix(in oklab, white 10%, transparent);
		opacity: 1;
	}

	.studio-editor__toolbar {
		border-bottom: 1px solid var(--color-surface-200-800);
		background: color-mix(in oklab, var(--studio-editor-bg) 92%, var(--color-primary-500));
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

	.studio-editor__monaco {
		min-height: 0;
	}

	.studio-editor :global(.studio-editor__empty) {
		grid-row: 1 / -1;
		background: var(--color-surface-50-950);
	}
</style>
