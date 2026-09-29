<script>
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import SaveCancelButtons from '$lib/admin/components/SaveCancelButtons.svelte';
	import AccordionGroup from '$lib/common/components/AccordionGroup.svelte';
	import { createDatabaseObjectProperties } from '$lib/common/DatabaseObjectProperties.svelte.js';
	import LightSvelte from '$lib/common/Light.svelte';
	import Editor from '$lib/studio/editor/Editor.svelte';
	import {
		asEditorValue,
		canOpenCodeProperty,
		choiceControl,
		flowBindingPreview,
		getPropertyLanguage,
		hasPropertyPossibleValues,
		isIonProperty,
		isSamePropertyPickerTarget,
		isSemanticColorProperty,
		isSmartSourceProperty,
		SMART_TYPE_MODES
	} from '$lib/studio/propertyEditors';
	import Ico from '$lib/utils/Ico.svelte';
	import { tick, untrack } from 'svelte';
	import { flowTypeDisplayName } from './blockDefinition';
	import { trackPropertyApply } from './propertyApply.svelte.js';
	import StudioBooleanField from './StudioBooleanField.svelte';
	import StudioEmptyState from './StudioEmptyState.svelte';
	import StudioEndpointDialog from './StudioEndpointDialog.svelte';
	import StudioFontDialog from './StudioFontDialog.svelte';
	import StudioIconButton from './StudioIconButton.svelte';
	import StudioLifetimeDialog from './StudioLifetimeDialog.svelte';
	import StudioMergeTrees from './StudioMergeTrees.svelte';
	import StudioMergeVersions from './StudioMergeVersions.svelte';
	import StudioNamedSourceDialog from './StudioNamedSourceDialog.svelte';
	import StudioObjectIdentity from './StudioObjectIdentity.svelte';
	import StudioProjectReferenceDialog from './StudioProjectReferenceDialog.svelte';
	import StudioQNameDialog from './StudioQNameDialog.svelte';
	import StudioSection from './StudioSection.svelte';
	import StudioSourcePickerPanel from './StudioSourcePickerPanel.svelte';
	import StudioTableProperty from './StudioTableProperty.svelte';
	import { nodeDiff, revertTreeProperty, treeDiff } from './treeDiff.svelte.js';
	import { describeOperation, mergeOfNode, resolveConflict } from './treeMerge.svelte.js';
	import { treeSelectionOf } from './treeSelection.svelte.js';

	/**
	 * @type {{
	 *  selectedId?: string,
	 *  active?: boolean,
	 *  refreshSerial?: number,
	 *  onSave?: (id: string, result?: any) => void | Promise<void>,
	 *  onMutationBusyChange?: (busy: boolean, handled?: boolean) => void,
	 *  onOpenPropertyEditor?: (target: { id: string, propertyName?: string, displayName?: string, value?: any }) => void,
	 *  onOpenPropertyPicker?: (target: { id: string, propertyName?: string, displayName?: string, value?: any, kind?: string, editorClass?: string, mode?: string, row?: number, column?: number }) => void,
	 *  pickerTarget?: { id: string, propertyName?: string, displayName?: string, value?: any, kind?: string, editorClass?: string, mode?: string, row?: number, column?: number } | null,
	 *  pickerRequest?: { id: string, propertyName: string, serial: number } | null,
	 *  identityItem?: { id?: string, name?: string, classname?: string, instanceName?: string, icon?: string } | null,
	 *  frontendThemeContext?: { mode: string, palette: string, tokens: any[] } | null,
	 *  onPickerApply?: (id: string, value?: any) => void | Promise<void>,
	 *  onSaveProject?: () => void | Promise<void>,
	 *  onSelectObject?: (id: string) => void
	 * }}
	 */
	let {
		selectedId = '',
		active = true,
		refreshSerial = 0,
		onSave,
		onMutationBusyChange = () => {},
		onOpenPropertyEditor,
		onOpenPropertyPicker,
		pickerTarget = null,
		pickerRequest = null,
		identityItem = null,
		frontendThemeContext = null,
		onPickerApply = () => {},
		onSaveProject,
		onSelectObject
	} = $props();

	/** the changes of the object since the commit of the Git mode of the tree */
	let diffChange = $derived(selectedId ? nodeDiff(selectedId).change : undefined);
	let revertingProperty = $state('');
	/** the merge of the project stopped on conflicts: the conflicts of the object and what came from them */
	let merge = $derived(selectedId ? mergeOfNode(selectedId) : undefined);
	let mergeConflicts = $derived((merge && merge.conflictsById[selectedId]) || []);
	let mergeChange = $derived(merge ? merge.changesById[selectedId] : undefined);
	/** the value typed for a conflict resolved by an edit */
	let mergeEdits = $state(/** @type {Record<string, string>} */ ({}));
	let mergeEditing = $state('');

	/**
	 * @param {import('./treeMerge.svelte.js').MergeConflict} conflict
	 * @param {string} choice
	 * @param {string} [value]
	 */
	function resolve(conflict, choice, value) {
		const projectName = String(selectedId).match(/^[^.:/~]+/)?.[0] ?? '';
		mergeEditing = '';
		return resolveConflict(projectName, conflict.id, choice, value);
	}

	/** how the two versions of an object in conflict show: their differences, or two trees side by side */
	let versionsView = $state(readVersionsView());
	/** the conflict whose two trees show over the whole window */
	let wideConflict = $state(
		/** @type {import('./treeMerge.svelte.js').MergeConflict | null} */ (null)
	);

	function readVersionsView() {
		try {
			return localStorage.getItem('convertigo.studio.merge.versionsView') === 'trees'
				? 'trees'
				: 'differences';
		} catch {
			return 'differences';
		}
	}

	/**
	 * @param {'differences' | 'trees'} view
	 */
	function setVersionsView(view) {
		versionsView = view;
		try {
			localStorage.setItem('convertigo.studio.merge.versionsView', view);
		} catch {
			// kept for this view only
		}
	}

	/**
	 * @param {import('./treeMerge.svelte.js').MergeConflict} conflict
	 * @returns {{ props?: Record<string, 'mine' | 'theirs'>, children?: Record<string, boolean> }} what
	 *  is kept of each side, as chosen
	 */
	function combinationOf(conflict) {
		try {
			return conflict.value ? JSON.parse(conflict.value) : {};
		} catch {
			return {};
		}
	}

	/**
	 * @param {string | undefined} side mine, theirs or base
	 */
	function sideLabel(side) {
		return side === 'mine'
			? merge?.ours
				? `Mine · ${merge.ours}`
				: 'Mine'
			: side === 'theirs'
				? merge?.theirs
					? `Theirs · ${merge.theirs}`
					: 'Theirs'
				: 'Base';
	}

	/**
	 * Shows a dialog over the whole window, out of the view whose panel would contain it.
	 * @param {HTMLElement} node
	 */
	function portal(node) {
		node.ownerDocument.body.appendChild(node);
		return {
			destroy() {
				node.remove();
			}
		};
	}

	/** @type {Record<string, string>} */
	const CHOICE_LABELS = {
		mine: 'Keep mine',
		theirs: 'Take theirs',
		both: 'Keep both',
		merge: 'Combine…',
		edit: 'Edit…'
	};

	/**
	 * Gives back the value of a property at the commit, as a change of the Studio.
	 * @param {string} name
	 */
	async function revertProperty(name) {
		const id = selectedId;
		revertingProperty = name;
		try {
			if (await revertTreeProperty(id, name)) {
				await onSave?.(id);
			}
		} finally {
			revertingProperty = '';
		}
	}

	/**
	 * Ctrl or ⌘ with S applies the changes of the properties and saves the project, as the Eclipse Studio
	 * sets the properties at once and saves the project.
	 * @param {KeyboardEvent} event
	 */
	async function handleSaveKey(event) {
		if (!(event.metaKey || event.ctrlKey) || event.altKey || event.key.toLowerCase() !== 's') {
			return;
		}
		if (event.shiftKey) {
			// the page saves all the projects once the change is applied, as the Save All of the Eclipse Studio
			void saveChanges();
			return;
		}
		event.preventDefault();
		event.stopPropagation();
		await saveChanges();
		await onSaveProject?.();
	}

	let openedCategories = $state(/** @type {string[]} */ ([]));
	let clickedCategories = $state(/** @type {string[]} */ ([]));
	let monacoRow = $state();
	let monacoValue = $state('');
	let requestedSelectionId = '';
	let lastRefreshSerial = 0;
	let saving = $state(false);
	let {
		id,
		properties,
		categories,
		onSelectionChange,
		hasChanges,
		valid,
		updateDraft,
		loading,
		getChanges,
		save,
		cancel
	} = $derived(createDatabaseObjectProperties());
	let monacoLanguage = $derived(getPropertyLanguage(monacoRow, selectedId));
	let monacoTitle = $derived(monacoRow?.displayName ?? monacoRow?.name ?? 'Editor');
	let monacoTheme = $derived(LightSvelte.light ? '' : 'vs-dark');
	let displayedIdentity = $derived(identityItem ?? propertyIdentity(categories));
	/** the other objects selected with the shown one, which get its changes when they are of its type */
	let otherSelected = $derived(
		treeSelectionOf(selectedId).filter((other) => other !== selectedId && /[.:]/.test(other))
	);
	/** the objects selected with the shown one, which its changes still reach once another is selected */
	/** @type {string[]} */
	let shownOthers = [];
	$effect(() => {
		if (id && id === selectedId) {
			shownOthers = otherSelected;
		}
	});

	function propertyIdentity(propertyCategories) {
		const rows = (propertyCategories ?? []).flatMap((category) => category?.properties ?? []);
		const flowType = String(rows.find((row) => row?.name === 'virtualType')?.value ?? '').trim();
		if (!flowType) {
			return null;
		}
		const summary = String(rows.find((row) => row?.name === 'summary')?.value ?? '').trim();
		return {
			id: `flowtype:${flowType}`,
			name: flowTypeDisplayName(flowType),
			instanceName: summary || String(selectedId).split('.').at(-1) || ''
		};
	}

	$effect(() => {
		const nextId = selectedId;
		const nextRefreshSerial = refreshSerial;
		if (!active) {
			return;
		}
		if (!nextId) {
			requestedSelectionId = '';
			return;
		}
		const forceRefresh = nextRefreshSerial !== lastRefreshSerial;
		if (!forceRefresh && (nextId === id || nextId === requestedSelectionId)) {
			return;
		}
		lastRefreshSerial = nextRefreshSerial;
		requestedSelectionId = nextId;
		untrack(() => void showObject(nextId));
	});

	/**
	 * Shows the properties of an object once the changes of the object left are applied, as the Eclipse
	 * Studio sets a property when its field loses the focus.
	 * @param {string} nextId
	 */
	async function showObject(nextId) {
		// the changes are applied before the object shows again, or another one
		if (id && hasChanges && valid && !saving) {
			await saveChanges();
			if (requestedSelectionId !== nextId) {
				return;
			}
		}
		await onSelectionChange({ selectedValue: [nextId] }).finally(() => {
			if (requestedSelectionId === nextId) {
				requestedSelectionId = '';
			}
		});
	}

	let handledPickerRequest = 0;

	$effect(() => {
		// a picker asked from elsewhere opens once the properties of its object show
		const request = pickerRequest;
		if (!request || request.serial === handledPickerRequest || request.id !== id) {
			return;
		}
		const found = categories.find(({ properties: rows }) =>
			rows.some((row) => row?.name === request.propertyName)
		);
		if (!found) {
			return;
		}
		handledPickerRequest = request.serial;
		untrack(() => {
			const opened = openedCategories.length ? openedCategories : getDefaultOpenedCategories();
			if (!opened.includes(found.category)) {
				openedCategories = [...opened, found.category];
			}
			const row = found.properties.find((row) => row?.name === request.propertyName);
			if (!isPickerOpen(row)) {
				openPicker(row);
			}
			void tick().then(() =>
				setTimeout(() =>
					document
						.querySelector('.studio-properties__inline-picker')
						?.scrollIntoView({ block: 'start', behavior: 'smooth' })
				)
			);
		});
	});

	function getDefaultOpenedCategories() {
		const open = categories.filter(
			({ category, properties: rows }) => clickedCategories.includes(category) && rows.length > 0
		);
		if (open.length > 0) {
			return open.map(({ category }) => category);
		}
		const first = categories.find(({ properties: rows }) => rows.length > 0);
		return first ? [first.category] : [];
	}

	function getType(row) {
		if (isFlowBindingProperty(row)) {
			return 'flow-binding';
		}
		if (isSmartSourceProperty(row)) {
			return 'smarttype';
		}
		let { class: cls, type: ionType, value, values } = row;
		if (row.symbols && value != row.originalValue) {
			return 'text';
		}
		untrack(() => {
			row.symbols = false;
		});
		if (row.isMasked === true && !row.isMultiline && typeof value === 'string') {
			// a masked property, as a password, as the Eclipse Studio masks it
			return 'password';
		}
		if (isSemanticColorProperty(row)) {
			return 'color-combo';
		}
		if (row.freeText && Array.isArray(values) && values.length) {
			// a combo of the Eclipse Studio that takes a typed text as well
			return 'combo-text';
		}
		if (hasPropertyPossibleValues(row)) {
			return choiceControl(values);
		}
		if (row?.flowKind === 'color') {
			return 'color';
		}
		if (row.isMultiline) {
			return 'textarea';
		}
		if (isIonProperty(row)) {
			if (ionType === 'boolean') {
				return 'boolean';
			}
			if (ionType === 'number') {
				return 'number';
			}
			if (ionType === 'object' || ionType === 'array') {
				return 'textarea';
			}
		}
		if (cls?.endsWith('Boolean')) {
			return 'boolean';
		}
		if (
			cls?.endsWith('Integer') ||
			cls?.endsWith('Long') ||
			cls?.endsWith('Double') ||
			cls?.endsWith('Float') ||
			cls?.endsWith('Short') ||
			cls?.endsWith('Byte')
		) {
			return 'number';
		}
		return 'text';
	}

	/**
	 * @param {any} row
	 * @returns {boolean}
	 */
	function isFlowBindingProperty(row) {
		return row?.editorClass === 'flow-binding-editor';
	}

	/**
	 * @param {any} value
	 * @returns {Record<string, any> | null}
	 */
	function parseFlowBinding(value) {
		try {
			const binding = typeof value === 'string' ? JSON.parse(value) : value;
			return binding && typeof binding === 'object' && !Array.isArray(binding) ? binding : null;
		} catch {
			return null;
		}
	}

	/** @param {any} row */
	function isLiteralFlowBinding(row) {
		return (
			row?.value === '' || row?.value == null || parseFlowBinding(row?.value)?.mode === 'literal'
		);
	}

	/** @param {any} row */
	function flowBindingLiteralValue(row) {
		return parseFlowBinding(row?.value)?.value ?? '';
	}

	/** @param {any} row */
	function flowBindingOriginalLiteralValue(row) {
		return parseFlowBinding(row?.originalValue)?.value ?? '';
	}

	/**
	 * @param {any} row
	 * @param {any} value
	 */
	function setFlowBindingLiteralValue(row, value) {
		const binding = parseFlowBinding(row?.value) ?? { mode: 'literal' };
		row.value = JSON.stringify({ ...binding, mode: 'literal', value });
	}

	/** @param {any} row */
	function flowBindingLiteralType(row) {
		const type = String(row?.flowType ?? '').toLowerCase();
		if (type === 'boolean') {
			return 'boolean';
		}
		if (['number', 'integer'].includes(type)) {
			return 'number';
		}
		if (isSemanticColorProperty(row)) {
			return 'color-combo';
		}
		return hasPropertyPossibleValues(row) ? choiceControl(row.values) : 'text';
	}

	/**
	 * @param {any} row
	 * @returns {boolean}
	 */
	function isInlineEditable(row) {
		return isIonProperty(row) || String(row?.class ?? '').startsWith('java.lang.');
	}

	/**
	 * @param {any} row
	 * @returns {string}
	 */
	function smartMode(row) {
		return ['plain', 'script', 'source'].includes(row?.mode) ? row.mode : 'plain';
	}

	/**
	 * @param {any} row
	 * @param {string} mode
	 */
	function setSmartMode(row, mode) {
		row.mode = mode;
		if (mode === 'source' && row.value == null) {
			row.value = [];
		} else if (mode !== 'source' && Array.isArray(row.value)) {
			row.value = row.value.join('\n');
		}
	}

	/** the qualified name property the QName dialog edits */
	let qnameRow = $state(/** @type {any} */ (null));
	/** the font property the font dialog edits */
	let fontRow = $state(/** @type {any} */ (null));
	/** the property naming another object the named source dialog edits */
	let namedSourceRow = $state(/** @type {any} */ (null));
	/** the response lifetime the lifetime dialog builds */
	let lifetimeRow = $state(/** @type {any} */ (null));
	/** the project reference the reference dialog edits */
	let referenceRow = $state(/** @type {any} */ (null));
	let endpointRow = $state(/** @type {any} */ (null));

	/**
	 * @param {any} row
	 * @returns {{ icon: string, title: string, onclick: () => void }[]}
	 */
	function namedSourceButtons(row) {
		return row?.namedSource
			? [
					{
						icon: 'mdi:target',
						title: `Choose ${row.displayName ?? row.name}`,
						onclick: () => (namedSourceRow = row)
					}
				]
			: [];
	}

	/**
	 * @param {any} row
	 * @returns {string}
	 */
	function previewValue(row) {
		if (row?.font) {
			// a font of the NGX fonts, as its JSON definition
			try {
				const font = JSON.parse(String(row.value || '{}'));
				return font.fontId
					? `${font.fontFamily} (${font.fontWeight} ${font.fontStyle} ${font.fontSubset})`
					: 'inherits';
			} catch {
				return 'inherits';
			}
		}
		if (row?.qname) {
			// a type or an element of the schemas, as {namespace}name
			const match = /^\{(.*)\}(.*)$/.exec(String(row.value ?? ''));
			return match ? `${match[2]}${match[1] ? ` (${match[1]})` : ''}` : String(row.value || 'none');
		}
		if (typeof row?.sourceLabel === 'string') {
			// the source of a step, as its step and xpath
			return row.sourceLabel || 'empty';
		}
		const value = asEditorValue(row?.value);
		return value === '' ? 'empty' : value;
	}

	/**
	 * @param {any} row
	 * @returns {{ icon: string, title: string, active?: boolean, ariaExpanded?: boolean, onclick: () => void }[]}
	 */
	function smartTypeButtons(row) {
		if (smartMode(row) === 'plain' && row?.namedSource) {
			return namedSourceButtons(row);
		}
		if (smartMode(row) === 'source') {
			return [
				{
					icon: 'mdi:hub',
					title: isPickerOpen(row) ? 'Close source picker' : 'Open source picker',
					active: isPickerOpen(row),
					ariaExpanded: isPickerOpen(row),
					onclick: () => openPicker(row)
				}
			];
		}
		if (smartMode(row) === 'script') {
			return [
				{
					icon: 'mdi:open-in-new-variant',
					title: 'Open code editor',
					onclick: () => openMonaco(row)
				}
			];
		}
		return [];
	}

	/**
	 * Sets a value that can be null to null, or gives it a value again.
	 * @param {any} row
	 */
	function toggleNull(row) {
		row.isNull = !row.isNull;
		if (row.isNull) {
			row.value = row.table ? [] : '';
		}
	}

	/**
	 * @param {any} row
	 * @returns {{ icon: string, title: string, active?: boolean, ariaExpanded?: boolean, onclick: () => void }[]}
	 */
	function propertyButtons(row) {
		const buttons = [];
		if (row?.nillable) {
			// the null value of a variable, as the null button of the Eclipse Studio
			buttons.push({
				icon: 'mdi:null',
				title: 'Set it to null',
				onclick: () => toggleNull(row)
			});
		}
		if (String(row?.editorClass ?? '').startsWith('flow-')) {
			const binding = isFlowBindingProperty(row);
			const label = String(row?.displayName ?? row?.name ?? 'property');
			buttons.push({
				icon: 'mdi:hub',
				title: isPickerOpen(row)
					? `Close ${label} options`
					: binding
						? `Choose a source or compose ${label}`
						: `Open ${label} options`,
				active: isPickerOpen(row),
				ariaExpanded: isPickerOpen(row),
				onclick: () => openPicker(row)
			});
		}
		if (canOpenCodeProperty(row, selectedId)) {
			buttons.push({
				icon: 'mdi:open-in-new-variant',
				title: 'Open code editor',
				onclick: () => openMonaco(row)
			});
		}
		buttons.push(...namedSourceButtons(row));
		if (row?.endpoint) {
			buttons.push({
				icon: 'mdi:lan-connect',
				title: 'Choose the endpoint',
				onclick: () => (endpointRow = row)
			});
		}
		if (row?.projectReference) {
			buttons.push({
				icon: 'mdi:source-pull',
				title: 'Edit the project reference',
				onclick: () => (referenceRow = row)
			});
		}
		if (row?.kind === 'dbo' && row?.name === 'responseExpiryDate') {
			buttons.push({
				icon: 'mdi:calendar-clock',
				title: 'Build the response lifetime',
				onclick: () => (lifetimeRow = row)
			});
		}
		return buttons;
	}

	/**
	 * @param {any} row
	 * @returns {number}
	 */
	function textareaRows(row) {
		const value = asEditorValue(row?.value);
		const lines = value ? value.split(/\r\n|\r|\n/).length : 1;
		return Math.min(Math.max(lines, 1), 3);
	}

	/**
	 * @param {any} row
	 * @param {string} category
	 * @returns {boolean} whether the property only shows its value: the information on the object, and
	 *  the properties its object does not let change, as the tag name of an NGX component
	 */
	function isReadOnlyRow(row, category) {
		return category === 'Information' || row?.isDisabled === true;
	}

	/**
	 * @param {any} row
	 * @param {string} category
	 * @param {string} type
	 * @returns {boolean}
	 */
	function isWideField(row, category, type) {
		if (isReadOnlyRow(row, category)) {
			return false;
		}
		if (row?.table) {
			return true;
		}
		if (type === 'textarea') {
			return textareaRows(row) > 1;
		}
		if (type === 'flow-binding') {
			return true;
		}
		if (isSmartSourceProperty(row)) {
			return true;
		}
		if (!isInlineEditable(row)) {
			const value = previewValue(row);
			return value.includes('\n') || value.length > 96;
		}
		return false;
	}

	/**
	 * @param {any} row
	 * @param {string} category
	 * @returns {boolean}
	 */
	function isChanged(row, category) {
		return (
			!isReadOnlyRow(row, category) &&
			(row.value != row.originalValue || ('mode' in row && row.mode != row.originalMode))
		);
	}

	/**
	 * A value committed in a field, by Enter, by leaving the field or by a choice, is applied at once, as the
	 * property sheet of the Eclipse Studio sets it.
	 * @param {Event} event
	 */
	function commitField(event) {
		const target = event.target;
		if (
			!(target instanceof HTMLInputElement) &&
			!(target instanceof HTMLSelectElement) &&
			!(target instanceof HTMLTextAreaElement)
		) {
			return;
		}
		const choice =
			target instanceof HTMLSelectElement ||
			(target instanceof HTMLInputElement && /^(checkbox|radio)$/.test(target.type));
		if (choice) {
			void saveChanges();
			return;
		}
		// a text applies a moment after its field is left, which leaves the time of a click on Cancel; a save of
		// the project waits for it
		void trackPropertyApply(
			new Promise((resolve) => setTimeout(() => resolve(applyChanges()), 250))
		);
	}

	/**
	 * @param {KeyboardEvent} event
	 */
	function commitOnEnter(event) {
		if (
			event.key === 'Enter' &&
			!event.isComposing &&
			!event.shiftKey &&
			event.target instanceof HTMLInputElement
		) {
			void saveChanges();
		}
	}

	function saveChanges() {
		return trackPropertyApply(applyChanges());
	}

	async function applyChanges() {
		if (saving || !valid || !getChanges().length) {
			return;
		}
		let handled = false;
		saving = true;
		onMutationBusyChange(true);
		try {
			await save({
				persist: false,
				alsoIds: id === selectedId ? otherSelected : shownOthers,
				onSaved: async (savedId, result) => {
					if (selectedId === savedId) closeCurrentPicker();
					await onSave?.(savedId, result);
					handled = true;
				}
			});
		} finally {
			saving = false;
			onMutationBusyChange(false, handled);
		}
	}

	function cancelChanges() {
		cancel();
		closeCurrentPicker();
	}

	function closeCurrentPicker() {
		if (pickerTarget?.id === selectedId) {
			onOpenPropertyPicker?.(pickerTarget);
		}
	}

	/** @param {any} value @param {{valid?: boolean, error?: string}} [validation] */
	function updatePickerDraft(value, validation) {
		const propertyName = pickerTarget?.propertyName;
		updateDraft(propertyName, value, validation);
	}

	function openMonaco(row) {
		if (onOpenPropertyEditor) {
			onOpenPropertyEditor({
				id: selectedId,
				propertyName: row?.name,
				displayName: row?.displayName,
				value: row?.value
			});
			return;
		}
		monacoRow = row;
		monacoValue = asEditorValue(row?.value);
	}

	function openPicker(row) {
		onOpenPropertyPicker?.({
			id: selectedId,
			propertyName: row?.name,
			displayName: row?.displayName,
			value: row?.value,
			kind: row?.kind,
			editorClass: row?.editorClass,
			mode: row?.mode
		});
	}

	/**
	 * Opens the source picker on a step source of a table, as the Sources of the XML action steps.
	 * @param {any} row
	 * @param {number} rowIndex
	 * @param {number} column
	 * @param {string[]} source
	 */
	function openTableSourcePicker(row, rowIndex, column, source) {
		onOpenPropertyPicker?.({
			id: selectedId,
			propertyName: row?.name,
			displayName: row?.displayName,
			value: source,
			kind: row?.kind,
			editorClass: row?.editorClass,
			row: rowIndex,
			column
		});
	}

	/**
	 * @param {any} row
	 * @returns {{ row: number, column: number } | null} the cell of a table the picker chooses a source for
	 */
	function pickingCellOf(row) {
		const target = pickerTarget;
		return target &&
			isPickerOpen(row) &&
			typeof target.row === 'number' &&
			typeof target.column === 'number'
			? { row: target.row, column: target.column }
			: null;
	}

	/**
	 * Puts the source chosen in the picker in its cell, which the Apply saves with the table.
	 * @param {any} row
	 * @param {string[]} source
	 * @param {string} label
	 */
	function pickTableSource(row, source, label) {
		const target = pickerTarget;
		if (
			!target ||
			typeof target.row !== 'number' ||
			typeof target.column !== 'number' ||
			!Array.isArray(row.value?.[target.row])
		) {
			return;
		}
		const { row: targetRow, column: targetColumn } = target;
		row.value = row.value.map((/** @type {any[]} */ cells, /** @type {number} */ index) =>
			index === targetRow
				? cells.map((cell, column) => (column === targetColumn ? source : cell))
				: cells
		);
		// the label of the source until the engine names it
		row.sourceLabels = { ...row.sourceLabels, [`${source[0]} ${source[1]}`]: label };
		// the picker closes, as the cell editor of the Eclipse Studio
		onOpenPropertyPicker?.(target);
	}

	function isPickerOpen(row) {
		return isSamePropertyPickerTarget(pickerTarget, {
			id: selectedId,
			propertyName: row?.name
		});
	}

	function applyMonaco() {
		if (monacoRow) {
			monacoRow.value = monacoValue;
		}
		monacoRow = undefined;
	}
</script>

<!-- svelte-ignore a11y_no_static_element_interactions -->
<div class="studio-properties layout-y-stretch" onkeydown={handleSaveKey}>
	<div class="studio-properties__actions studio-panel-toolbar">
		<SaveCancelButtons
			class="w-full"
			saveLabel="Apply"
			cancelLabel="Cancel"
			onSave={saveChanges}
			onCancel={cancelChanges}
			changesPending={hasChanges}
			saveDisabled={!valid}
			disabled={saving || !selectedId || properties.length == 0}
		/>
	</div>
	<StudioObjectIdentity item={displayedIdentity} compact />
	{#if otherSelected.length}
		<p class="studio-properties__multi" role="status">
			The changes apply as well to the {otherSelected.length} other selected object{otherSelected.length >
			1
				? 's'
				: ''} of the same type, their name apart.
		</p>
	{/if}

	<div
		class="studio-properties__body"
		class:studio-properties__body--loading={loading}
		onchange={commitField}
		onkeydown={commitOnEnter}
		role="presentation"
	>
		{#if !selectedId}
			<StudioEmptyState message="No object selected" icon="mdi:cursor-default-click-outline" />
		{:else}
			{#if mergeConflicts.length || mergeChange}
				{#if wideConflict?.versions?.trees}
					<div
						class="studio-properties__wide"
						role="presentation"
						use:portal
						onclick={() => (wideConflict = null)}
					>
						<div
							class="studio-properties__wide-box"
							role="dialog"
							aria-modal="true"
							aria-label="The two versions of {wideConflict.name}"
							tabindex="-1"
							onclick={(event) => event.stopPropagation()}
							onkeydown={(event) => {
								event.stopPropagation();
								if (event.key === 'Escape') wideConflict = null;
							}}
						>
							<header class="studio-properties__wide-head">
								<strong>{wideConflict.name}</strong>
								<span>{wideConflict.description}</span>
								<button
									type="button"
									class="studio-properties__versions-wide"
									aria-label="Close"
									onclick={() => (wideConflict = null)}><Ico icon="mdi:close" size={4} /></button
								>
							</header>
							<StudioMergeTrees
								trees={wideConflict.versions.trees}
								leftLabel={sideLabel(wideConflict.versions.left)}
								rightLabel={sideLabel(wideConflict.versions.right)}
								onSelect={(id) => {
									wideConflict = null;
									onSelectObject?.(id);
								}}
							/>
						</div>
					</div>
				{/if}
				<section class="studio-properties__merge" aria-label="Conflicts with {merge?.theirs}">
					<header class="studio-properties__merge-title">
						<Ico icon={describeOperation(merge?.operation).icon} size={3.6} />
						{mergeConflicts.length
							? `Conflict${mergeConflicts.length > 1 ? 's' : ''} with ${merge?.theirs}`
							: `Merged from ${merge?.theirs}`}
					</header>
					{#each mergeConflicts as conflict (conflict.id)}
						<div class="studio-properties__merge-conflict">
							<div class="studio-properties__merge-what">
								<strong>{conflict.label ?? conflict.description}</strong>
								{#if conflict.label}<span>{conflict.description}</span>{/if}
							</div>
							{#if conflict.kind === 'property' || conflict.kind === 'order'}
								<dl
									class={[
										'studio-properties__merge-values',
										conflict.kind === 'order' && 'studio-properties__merge-values--lines'
									]}
								>
									<dt>base</dt>
									<dd>{conflict.base || '—'}</dd>
									<dt title={merge?.ours}>mine</dt>
									<dd class:studio-properties__merge-chosen={conflict.resolution === 'mine'}>
										{conflict.mine || '—'}
									</dd>
									<dt title={merge?.theirs}>theirs</dt>
									<dd class:studio-properties__merge-chosen={conflict.resolution === 'theirs'}>
										{conflict.theirs || '—'}
									</dd>
									{#if conflict.resolution === 'edit'}
										<dt>edited</dt>
										<dd class="studio-properties__merge-chosen">{conflict.value}</dd>
									{/if}
								</dl>
							{/if}
							{#if conflict.versions}
								<div class="studio-properties__versions-bar">
									<div
										class="studio-properties__versions-view"
										role="radiogroup"
										aria-label="Show the two versions"
									>
										<button
											type="button"
											role="radio"
											aria-checked={versionsView === 'differences'}
											onclick={() => setVersionsView('differences')}
											><Ico icon="mdi:format-list-bulleted" size={3.5} /> Differences</button
										>
										<button
											type="button"
											role="radio"
											aria-checked={versionsView === 'trees'}
											onclick={() => setVersionsView('trees')}
											><Ico icon="mdi:file-tree-outline" size={3.5} /> Two trees</button
										>
									</div>
									{#if conflict.versions.trees}
										<button
											type="button"
											class="studio-properties__versions-wide"
											title="Show the two trees over the whole window"
											aria-label="Show the two trees over the whole window"
											onclick={() => (wideConflict = conflict)}
											><Ico icon="mdi:window-maximize" size={3.5} /></button
										>
									{/if}
								</div>
								{#if versionsView === 'trees' && conflict.versions.trees && conflict.resolution !== 'merge'}
									<StudioMergeTrees
										trees={conflict.versions.trees}
										leftLabel={sideLabel(conflict.versions.left)}
										rightLabel={sideLabel(conflict.versions.right)}
										onSelect={onSelectObject}
									/>
								{:else}
									<StudioMergeVersions
										versions={conflict.versions}
										labels={{ base: 'Base', mine: sideLabel('mine'), theirs: sideLabel('theirs') }}
										onSelect={onSelectObject}
										combination={conflict.resolution === 'merge' ? combinationOf(conflict) : null}
										onCombine={(next) => void resolve(conflict, 'merge', JSON.stringify(next))}
									/>
									{#if conflict.resolution === 'merge'}
										<p class="studio-properties__versions-hint">
											Click the value to keep of each property, mine by default; uncheck the objects
											of a single side to leave out.
										</p>
									{/if}
								{/if}
							{/if}
							{#if mergeEditing === conflict.id}
								<form
									class="studio-properties__merge-edit"
									onsubmit={(event) => {
										event.preventDefault();
										void resolve(conflict, 'edit', mergeEdits[conflict.id] ?? '');
									}}
								>
									<input
										aria-label="Value of {conflict.label}"
										bind:value={mergeEdits[conflict.id]}
									/>
									<button type="submit" class="studio-properties__merge-choice">Apply</button>
									<button
										type="button"
										class="studio-properties__merge-choice"
										onclick={() => (mergeEditing = '')}>Cancel</button
									>
								</form>
							{:else}
								<div class="studio-properties__merge-choices">
									{#each conflict.choices as choice (choice)}
										<button
											type="button"
											class="studio-properties__merge-choice"
											class:studio-properties__merge-choice--chosen={conflict.resolution === choice}
											aria-pressed={conflict.resolution === choice}
											onclick={() => {
												if (choice === 'edit') {
													mergeEdits[conflict.id] = conflict.value ?? conflict.mine ?? '';
													mergeEditing = conflict.id;
												} else {
													void resolve(conflict, choice);
												}
											}}>{CHOICE_LABELS[choice] ?? choice}</button
										>
									{/each}
									{#if conflict.resolution}
										<button
											type="button"
											class="studio-properties__merge-choice studio-properties__merge-choice--clear"
											title="Choose again"
											aria-label="Choose again"
											onclick={() => void resolve(conflict, 'clear')}
										>
											<Ico icon="mdi:undo" size={3.4} />
										</button>
									{/if}
								</div>
							{/if}
						</div>
					{/each}
					{#if mergeChange}
						<p class="studio-properties__merge-from">
							{mergeChange.status === 'removed'
								? 'Removed by them: the merge removes it'
								: mergeChange.status === 'moved'
									? 'Moved by them'
									: `Changed by them, taken: ${mergeChange.properties?.join(', ') ?? ''}`}
						</p>
					{/if}
				</section>
			{/if}
			{#if diffChange}
				<section class="studio-properties__diff" aria-label="Changes since {treeDiff.ref}">
					<header class="studio-properties__diff-title">
						<Ico icon="mdi:file-compare" size={3.6} />
						{diffChange.status === 'added' ? 'Added' : 'Changed'} since {treeDiff.ref}
					</header>
					{#if diffChange.oldName}
						<div class="studio-properties__diff-row">
							<span class="studio-properties__diff-name">Name</span>
							<span><del>{diffChange.oldName}</del> → <ins>{diffChange.name}</ins></span>
						</div>
					{/if}
					{#if diffChange.moved}
						<div class="studio-properties__diff-row">
							<span class="studio-properties__diff-name">Place</span>
							<span>moved to another parent</span>
						</div>
					{/if}
					{#each diffChange.properties ?? [] as property (property.name)}
						<div class="studio-properties__diff-row">
							<span class="studio-properties__diff-name" title={property.name}
								>{property.label}</span
							>
							<span class="studio-properties__diff-values">
								{#if property.old}<del>{property.old}</del>{:else}<em>default</em>{/if}
								→
								{#if property.new}<ins>{property.new}</ins>{:else}<em>default</em>{/if}
							</span>
							{#if diffChange.status === 'modified'}
								<button
									type="button"
									class="studio-properties__diff-revert"
									title="Give back the value of {treeDiff.ref}"
									aria-label="Revert {property.label}"
									disabled={revertingProperty === property.name}
									onclick={() => void revertProperty(property.name)}
								>
									<Ico icon="mdi:undo" size={3.4} />
								</button>
							{/if}
						</div>
					{/each}
				</section>
			{/if}
			<AccordionGroup
				class="studio-properties__sections"
				value={openedCategories.length ? openedCategories : getDefaultOpenedCategories()}
				onValueChange={({ value }) => {
					openedCategories = value;
					clickedCategories = value;
				}}
				multiple
			>
				{#each categories as { category, properties: rows } (category)}
					{@const total = rows.length}
					<StudioSection
						value={category}
						disabled={total == 0}
						title={category}
						count={total}
						countVariant="number"
					>
						{#snippet panel()}
							{#if total === 0}
								<StudioEmptyState message="Empty" small />
							{:else}
								<div class="studio-properties__fields layout-y-start-none">
									{#each rows as row (row.name ?? row.displayName)}
										{@const { value, originalValue, values } = row}
										{@const label = row.displayName ?? row.name ?? ''}
										{@const type = getType(row)}
										{@const changed = isChanged(row, category)}
										{@const inlineEditable = isInlineEditable(row)}
										{@const smartType = isSmartSourceProperty(row)}
										{@const wideField = isWideField(row, category, type)}
										<div
											class="studio-properties__field"
											class:studio-properties__field--wide={wideField}
											class:studio-properties__field--textarea={type === 'textarea' || smartType}
											class:studio-properties__field--changed={changed}
											class:studio-properties__field--picker-open={isPickerOpen(row)}
										>
											<div class="studio-properties__field-header layout-x-between-none">
												<span
													class="studio-properties__field-label"
													title={String(row.shortDescription ?? '')
														.split('|')[0]
														.trim() || label}
												>
													{label}
												</span>
												{#if changed}
													<span class="studio-properties__field-dirty" aria-label="Modified"></span>
												{/if}
											</div>
											<div class="studio-properties__field-control">
												{#if isReadOnlyRow(row, category)}
													<span class="studio-properties__static"
														>{row.isMasked === true && value ? '••••••••' : value}</span
													>
												{:else if row.isNull}
													<div class="studio-properties__fallback layout-x-low">
														<code class="studio-properties__fallback-value studio-properties__null"
															>null</code
														>
														<div class="studio-properties__fallback-actions layout-x-low">
															<StudioIconButton
																icon="mdi:null"
																size="xs"
																active
																title="Give it a value"
																ariaLabel={`Give ${label} a value`}
																onclick={() => toggleNull(row)}
															/>
														</div>
													</div>
												{:else if row.table}
													{#if row.nillable}
														<div class="studio-properties__table-actions layout-x-end-none">
															<StudioIconButton
																icon="mdi:null"
																size="xs"
																title="Set it to null"
																ariaLabel={`Set ${label} to null`}
																onclick={() => toggleNull(row)}
															/>
														</div>
													{/if}
													<StudioTableProperty
														value={row.value}
														editorClass={row.editorClass}
														name={row.name}
														sourceLabels={row.sourceLabels}
														pickingCell={pickingCellOf(row)}
														onChange={(rows) => (row.value = rows)}
														onPickSource={(rowIndex, column, source) =>
															openTableSourcePicker(row, rowIndex, column, source)}
													/>
												{:else if row.flags}
													<div class="studio-properties__flags" role="group" aria-label={label}>
														{#each row.flags as flag (flag.mask)}
															<label class="studio-properties__flag">
																<input
																	type="checkbox"
																	checked={(Number(row.value) & flag.mask) !== 0}
																	onchange={(event) => {
																		const mask = Number(row.value) || 0;
																		row.value = String(
																			event.currentTarget.checked
																				? mask | flag.mask
																				: mask & ~flag.mask
																		);
																	}}
																/>
																{flag.label}
															</label>
														{/each}
													</div>
												{:else if row.font}
													<div class="studio-properties__fallback layout-x-low">
														<code
															class="studio-properties__fallback-value"
															class:studio-properties__fallback-value--compact={!wideField}
															>{previewValue(row)}</code
														>
														<div class="studio-properties__fallback-actions layout-x-low">
															<StudioIconButton
																icon="mdi:format-font"
																size="xs"
																title="Choose the font"
																ariaLabel={`Choose ${label}`}
																onclick={() => (fontRow = row)}
															/>
														</div>
													</div>
												{:else if row.qname}
													<div class="studio-properties__fallback layout-x-low">
														<code
															class="studio-properties__fallback-value"
															class:studio-properties__fallback-value--compact={!wideField}
															>{previewValue(row)}</code
														>
														<div class="studio-properties__fallback-actions layout-x-low">
															<StudioIconButton
																icon="mdi:file-tree-outline"
																size="xs"
																title={`Choose the ${row.qname === 'element' ? 'element' : 'type'} in the schemas of the project`}
																ariaLabel={`Choose ${label}`}
																onclick={() => (qnameRow = row)}
															/>
														</div>
													</div>
												{:else if type === 'flow-binding'}
													{#if isLiteralFlowBinding(row) && flowBindingLiteralType(row) === 'boolean'}
														<StudioBooleanField
															value={flowBindingLiteralValue(row)}
															onChange={(nextValue) => setFlowBindingLiteralValue(row, nextValue)}
															{label}
															buttons={propertyButtons(row)}
														/>
													{:else if isLiteralFlowBinding(row)}
														<PropertyType
															type={flowBindingLiteralType(row)}
															bind:value={
																() => flowBindingLiteralValue(row),
																(nextValue) => setFlowBindingLiteralValue(row, nextValue)
															}
															item={values}
															originalValue={flowBindingOriginalLiteralValue(row)}
															segmentCompact={flowBindingLiteralType(row) === 'segment'}
															actionsHorizontal
															buttons={propertyButtons(row)}
														/>
													{:else}
														<div class="studio-properties__fallback layout-x-low">
															<code class="studio-properties__fallback-value"
																>{flowBindingPreview(row.value)}</code
															>
															<StudioIconButton
																icon="mdi:hub"
																size="xs"
																title={isPickerOpen(row)
																	? 'Close binding options'
																	: 'Choose a source or compose value'}
																ariaLabel={isPickerOpen(row)
																	? 'Close binding options'
																	: 'Choose a source or compose value'}
																active={isPickerOpen(row)}
																aria-expanded={isPickerOpen(row)}
																onclick={() => openPicker(row)}
															/>
														</div>
													{/if}
												{:else if smartType}
													<PropertyType
														type="smarttype"
														bind:value={() => value, (nextValue) => (row.value = nextValue)}
														bind:mode={() => smartMode(row), (mode) => setSmartMode(row, mode)}
														item={SMART_TYPE_MODES}
														{originalValue}
														originalMode={row.originalMode}
														rows={smartMode(row) === 'script' ? textareaRows(row) : undefined}
														actionsHorizontal
														buttons={smartTypeButtons(row)}
													/>
												{:else if inlineEditable && type === 'boolean'}
													<StudioBooleanField
														{value}
														onChange={(nextValue) => (row.value = nextValue)}
														{label}
														buttons={propertyButtons(row)}
													/>
												{:else if inlineEditable}
													<PropertyType
														{type}
														bind:value={() => value, (nextValue) => (row.value = nextValue)}
														item={values}
														{originalValue}
														rows={type === 'textarea' ? textareaRows(row) : undefined}
														adaptiveTextarea={type === 'textarea'}
														segmentCompact={type === 'segment'}
														actionsHorizontal={type !== 'textarea'}
														buttons={propertyButtons(row)}
													/>
												{:else}
													<div class="studio-properties__fallback layout-x-low">
														<code
															class="studio-properties__fallback-value"
															class:studio-properties__fallback-value--compact={!wideField}
															>{previewValue(row)}</code
														>
														<div class="studio-properties__fallback-actions layout-x-low">
															{#if canOpenCodeProperty(row, selectedId)}
																<StudioIconButton
																	icon="mdi:code-tags"
																	size="xs"
																	title="Open code editor"
																	ariaLabel="Open code editor"
																	onclick={() => openMonaco(row)}
																/>
															{/if}
															<StudioIconButton
																icon="mdi:hub"
																size="xs"
																title={isPickerOpen(row) ? 'Close picker' : 'Open picker'}
																ariaLabel={isPickerOpen(row) ? 'Close picker' : 'Open picker'}
																active={isPickerOpen(row)}
																aria-expanded={isPickerOpen(row)}
																onclick={() => openPicker(row)}
															/>
														</div>
													</div>
												{/if}
											</div>
											{#if row.validation?.value === row.value && row.validation?.valid === false}
												<p class="text-sm text-error-500" role="alert">
													{row.validation.error || 'This value is not valid.'}
												</p>
											{/if}
											{#if !changed && row.compiledValue !== undefined}
												<!-- the value of its global symbols, as the properties of the Eclipse Studio show it -->
												<small
													class="studio-properties__compiled"
													title="The value of the global symbols of this property"
													>⇒ {row.compiledValue}</small
												>
											{:else if !changed && row.undefinedSymbols}
												<small
													class="studio-properties__compiled studio-properties__compiled--undefined"
													title="A global symbol of this property has no value"
													>Undefined global symbol</small
												>
											{/if}
											{#if isPickerOpen(row)}
												<div class="studio-properties__inline-picker">
													<StudioSourcePickerPanel
														{selectedId}
														active
														{pickerTarget}
														{frontendThemeContext}
														onApply={onPickerApply}
														onChange={updatePickerDraft}
														onPick={row.table && typeof pickerTarget?.row === 'number'
															? (source, label) => pickTableSource(row, source, label)
															: undefined}
														embedded
													/>
												</div>
											{/if}
										</div>
									{/each}
								</div>
							{/if}
						{/snippet}
					</StudioSection>
				{/each}
			</AccordionGroup>
			{#if loading}
				<span class="studio-properties__loading studio-spinner" aria-label="Loading properties"
				></span>
			{/if}
		{/if}
	</div>

	{#if endpointRow}
		<StudioEndpointDialog
			value={String(endpointRow.value ?? '')}
			onApply={(next) => {
				endpointRow.value = next;
				endpointRow = null;
			}}
			onClose={() => (endpointRow = null)}
		/>
	{/if}
	{#if referenceRow}
		<StudioProjectReferenceDialog
			label={referenceRow.displayName ?? referenceRow.name}
			value={String(referenceRow.value ?? '')}
			onApply={(next) => {
				referenceRow.value = next;
				referenceRow = null;
			}}
			onClose={() => (referenceRow = null)}
		/>
	{/if}

	{#if lifetimeRow}
		<StudioLifetimeDialog
			label={lifetimeRow.displayName ?? lifetimeRow.name}
			value={String(lifetimeRow.value ?? '')}
			onApply={(next) => {
				lifetimeRow.value = next;
				lifetimeRow = null;
			}}
			onClose={() => (lifetimeRow = null)}
		/>
	{/if}

	{#if namedSourceRow}
		<StudioNamedSourceDialog
			id={selectedId}
			property={namedSourceRow.name}
			label={namedSourceRow.displayName ?? namedSourceRow.name}
			value={Array.isArray(namedSourceRow.value) ? '' : String(namedSourceRow.value ?? '')}
			onApply={(next) => {
				namedSourceRow.value = next;
				namedSourceRow = null;
			}}
			onClose={() => (namedSourceRow = null)}
		/>
	{/if}

	{#if fontRow}
		<StudioFontDialog
			label={fontRow.displayName ?? fontRow.name}
			value={String(fontRow.value ?? '{}')}
			onApply={(next) => {
				fontRow.value = next;
				fontRow = null;
			}}
			onClose={() => (fontRow = null)}
		/>
	{/if}

	{#if qnameRow}
		<StudioQNameDialog
			id={selectedId}
			property={qnameRow.name}
			label={qnameRow.displayName ?? qnameRow.name}
			value={String(qnameRow.value ?? '')}
			onApply={(next) => {
				qnameRow.value = next;
				qnameRow = null;
			}}
			onClose={() => (qnameRow = null)}
		/>
	{/if}

	{#if monacoRow}
		<div class="studio-properties__editor" role="dialog" aria-modal="true">
			<header class="studio-properties__editor-header">
				<div>
					<strong>{monacoTitle}</strong>
					<span>{selectedId}</span>
				</div>
				<div class="studio-properties__editor-actions">
					<button type="button" class="button-secondary" onclick={() => (monacoRow = undefined)}>
						Cancel
					</button>
					<button type="button" class="button-primary" onclick={applyMonaco}>Apply</button>
				</div>
			</header>
			<div class="studio-properties__editor-body">
				<Editor
					bind:content={monacoValue}
					language={monacoLanguage}
					theme={monacoTheme}
					readOnly={false}
				/>
			</div>
		</div>
	{/if}
</div>

<style>
	.studio-properties__flags {
		display: grid;
		gap: 0.2rem;
		font-size: 0.78rem;
	}

	.studio-properties__flag {
		display: flex;
		align-items: center;
		gap: 0.4rem;
	}

	.studio-properties {
		height: 100%;
		min-height: 0;
	}

	/* the conflicts of the object in a merge stopped on them */
	.studio-properties__merge-values--lines dd {
		white-space: pre-line;
	}

	.studio-properties__versions-hint {
		margin: 0;
		color: var(--studio-text-idle);
		font-size: 0.7rem;
	}

	.studio-properties__versions-bar {
		display: flex;
		align-items: center;
		gap: 0.4rem;
	}

	.studio-properties__versions-view {
		display: inline-flex;
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.3rem;
	}

	.studio-properties__versions-view button {
		display: inline-flex;
		align-items: center;
		gap: 0.25rem;
		border: 0;
		background: transparent;
		color: var(--studio-text-idle);
		padding: 0.12rem 0.5rem;
		font-size: 0.72rem;
	}

	.studio-properties__versions-view button + button {
		border-left: 1px solid var(--studio-line);
	}

	.studio-properties__versions-view button[aria-checked='true'] {
		background: var(--studio-selection-bg);
		color: var(--studio-text-strong);
	}

	.studio-properties__versions-wide {
		display: inline-grid;
		width: 1.5rem;
		height: 1.5rem;
		margin-left: auto;
		place-items: center;
		border: 0;
		border-radius: 0.25rem;
		background: transparent;
		color: var(--studio-text-idle);
	}

	.studio-properties__versions-wide:hover {
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

	.studio-properties__wide {
		position: fixed;
		inset: 0;
		z-index: 95;
		display: grid;
		place-items: center;
		background: color-mix(in oklab, black 45%, transparent);
		padding: 1rem;
	}

	.studio-properties__wide-box {
		display: grid;
		width: min(90rem, 100%);
		max-height: calc(100vh - 2rem);
		gap: 0.5rem;
		overflow: auto;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
		padding: 0.75rem 1rem 1rem;
		--studio-trees-height: calc(100vh - 20rem);
	}

	.studio-properties__wide-head {
		display: flex;
		align-items: baseline;
		gap: 0.5rem;
	}

	.studio-properties__wide-head strong {
		color: var(--studio-text-strong);
	}

	.studio-properties__wide-head span {
		color: var(--studio-text-idle);
		font-size: 0.78rem;
	}

	.studio-properties__merge {
		display: grid;
		gap: 0.45rem;
		margin: 0.4rem 0.5rem 0.6rem;
		border: 1px solid color-mix(in oklab, #d32f2f 40%, transparent);
		border-radius: 0.45rem;
		background: color-mix(in oklab, #d32f2f 7%, transparent);
		padding: 0.5rem 0.65rem;
		font-size: 0.76rem;
	}

	.studio-properties__merge-title {
		display: flex;
		align-items: center;
		gap: 0.35rem;
		color: light-dark(#b71c1c, #ff8a80);
		font-weight: 700;
	}

	.studio-properties__merge-conflict {
		display: grid;
		gap: 0.35rem;
		border-top: 1px solid color-mix(in oklab, #d32f2f 18%, transparent);
		padding-top: 0.4rem;
	}

	.studio-properties__merge-what {
		display: flex;
		flex-wrap: wrap;
		align-items: baseline;
		gap: 0.4rem;
	}

	.studio-properties__merge-what span {
		color: var(--studio-text-idle);
		font-size: 0.7rem;
	}

	.studio-properties__merge-values {
		display: grid;
		grid-template-columns: 3.4rem minmax(0, 1fr);
		gap: 0.15rem 0.5rem;
		margin: 0;
	}

	.studio-properties__merge-values dt {
		color: var(--studio-text-idle);
	}

	.studio-properties__merge-values dd {
		margin: 0;
		overflow-wrap: anywhere;
	}

	.studio-properties__merge-chosen {
		color: light-dark(#2e7d32, #81c784);
		font-weight: 600;
	}

	.studio-properties__merge-choices,
	.studio-properties__merge-edit {
		display: flex;
		flex-wrap: wrap;
		gap: 0.3rem;
	}

	.studio-properties__merge-edit input {
		flex: 1;
		min-width: 8rem;
		height: 1.6rem;
		border: 1px solid var(--studio-line, var(--color-surface-200-800));
		border-radius: 0.3rem;
		background: var(--studio-main-bg, transparent);
		color: var(--studio-text-strong);
		padding: 0 0.4rem;
		font-size: 0.74rem;
	}

	.studio-properties__merge-choice {
		height: 1.6rem;
		border: 1px solid var(--studio-line, var(--color-surface-200-800));
		border-radius: 0.3rem;
		background: transparent;
		color: var(--studio-text-strong);
		padding: 0 0.5rem;
		font-size: 0.72rem;
	}

	.studio-properties__merge-choice:hover {
		background: var(--studio-hover-bg);
	}

	.studio-properties__merge-choice--chosen {
		border-color: transparent;
		background: color-mix(in oklab, #2e7d32 24%, transparent);
		color: light-dark(#1b5e20, #a5d6a7);
		font-weight: 700;
	}

	.studio-properties__merge-choice--clear {
		display: inline-grid;
		width: 1.6rem;
		place-items: center;
		padding: 0;
	}

	.studio-properties__merge-from {
		margin: 0;
		color: var(--studio-text-idle);
	}

	/* the changes of the object since the commit of the Git mode */
	.studio-properties__diff {
		display: grid;
		gap: 0.3rem;
		margin: 0.4rem 0.5rem 0.6rem;
		border: 1px solid color-mix(in oklab, #e2a23b 40%, transparent);
		border-radius: 0.45rem;
		background: color-mix(in oklab, #e2a23b 8%, transparent);
		padding: 0.5rem 0.65rem;
		font-size: 0.76rem;
	}

	.studio-properties__diff-title {
		display: flex;
		align-items: center;
		gap: 0.35rem;
		color: light-dark(#b26a00, #e2c08d);
		font-weight: 700;
	}

	.studio-properties__diff-row {
		display: grid;
		grid-template-columns: minmax(6rem, 30%) minmax(0, 1fr) auto;
		align-items: start;
		gap: 0.5rem;
	}

	.studio-properties__diff-revert {
		display: inline-grid;
		width: 1.4rem;
		height: 1.4rem;
		place-items: center;
		border: 0;
		border-radius: 0.3rem;
		background: transparent;
		color: var(--studio-text-idle);
	}

	.studio-properties__diff-revert:hover {
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

	.studio-properties__diff-name {
		overflow: hidden;
		color: var(--studio-text-idle);
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-properties__diff-values {
		min-width: 0;
		overflow-wrap: anywhere;
	}

	.studio-properties__diff del {
		color: light-dark(#c62828, #ef9a9a);
	}

	.studio-properties__diff ins {
		color: light-dark(#2e7d32, #81c784);
		text-decoration: none;
	}

	.studio-properties__diff em {
		color: var(--studio-text-idle);
	}

	.studio-properties__compiled {
		display: block;
		overflow: hidden;
		margin-top: 0.2rem;
		color: var(--studio-text-idle);
		font-size: 0.7rem;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-properties__compiled--undefined {
		color: var(--color-warning-600-400);
	}

	.studio-properties__body {
		position: relative;
		min-height: 0;
		flex: 1;
		overflow: auto;
		padding: 0;
	}

	.studio-properties__body--loading {
		opacity: 0.82;
	}

	.studio-properties__loading {
		position: sticky;
		z-index: 2;
		right: 0.5rem;
		bottom: 0.5rem;
		display: block;
		margin-left: auto;
		background: color-mix(in oklab, var(--color-surface-50-950) 92%, transparent);
		box-shadow: 0 0.5rem 1.4rem color-mix(in oklab, black 18%, transparent);
		pointer-events: none;
	}

	:global(.studio-properties__sections) {
		width: 100%;
	}

	.studio-properties__fields {
		width: 100%;
	}

	.studio-properties__field {
		display: grid;
		width: 100%;
		grid-template-columns: minmax(5.8rem, 34%) minmax(0, 1fr);
		align-items: center;
		column-gap: 0.55rem;
		row-gap: 0.32rem;
		border-bottom: 1px solid color-mix(in oklab, var(--color-surface-200-800) 62%, transparent);
		padding: 0.46rem 0.65rem;
	}

	.studio-properties__field:last-child {
		border-bottom: 0;
	}

	.studio-properties__field--wide {
		grid-template-columns: minmax(0, 1fr);
		align-items: stretch;
		padding-block: 0.58rem 0.65rem;
	}

	.studio-properties__field--changed {
		background: color-mix(in oklab, var(--color-primary-500) 5%, transparent);
	}

	.studio-properties__field--picker-open {
		grid-template-columns: minmax(0, 1fr);
		align-items: stretch;
		background: color-mix(in oklab, var(--color-primary-500) 4%, transparent);
	}

	.studio-properties__inline-picker {
		min-width: 0;
		overflow: hidden;
		border: 1px solid
			color-mix(in oklab, var(--color-primary-500) 34%, var(--color-surface-200-800));
		border-radius: var(--radius-base);
		background: var(--color-surface-50-950);
	}

	.studio-properties__field-header {
		min-width: 0;
		gap: 0.4rem;
	}

	.studio-properties__field-label {
		min-width: 0;
		overflow: hidden;
		color: var(--studio-text, var(--color-surface-700-300));
		font-size: 0.76rem;
		font-weight: 500;
		letter-spacing: 0;
		line-height: 1.2;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-properties__field-dirty {
		width: 0.45rem;
		height: 0.45rem;
		flex: 0 0 auto;
		border-radius: 999px;
		background: var(--color-primary-500);
	}

	.studio-properties__field-control {
		min-width: 0;
	}

	.studio-properties__field-control :global(.layout-y-low.sm\:layout-x-low > div:first-child) {
		width: auto;
		min-width: 0;
		flex: 1 1 7rem;
	}

	.studio-properties__field-control :global(.layout-y-low.sm\:layout-x-low) {
		width: 100%;
		flex-direction: row;
		flex-wrap: wrap;
		align-items: center;
		gap: 0.32rem;
	}

	.studio-properties__field--wide.studio-properties__field--textarea
		.studio-properties__field-control
		:global(.layout-y-low.sm\:layout-x-low) {
		align-items: flex-start;
	}

	.studio-properties__field-control :global(.sm\:grow) {
		width: 100%;
		min-width: 0;
	}

	.studio-properties__field-control :global(.layout-x-low.h-fit) {
		flex: 0 0 auto;
		margin-left: auto;
		align-self: center;
		flex-wrap: nowrap;
	}

	.studio-properties__field-control :global(.input-common),
	.studio-properties__field-control :global(input),
	.studio-properties__field-control :global(textarea),
	.studio-properties__field-control :global(select) {
		width: 100%;
		min-width: 0;
	}

	/* a text grows with its lines, those it wraps too, then scrolls */
	.studio-properties__field-control :global(textarea) {
		field-sizing: content;
		min-height: 2.25rem;
		max-height: 9.5rem;
		resize: vertical;
	}

	.studio-properties__static {
		display: block;
		max-width: 100%;
		overflow-wrap: anywhere;
		font-size: 0.76rem;
	}

	.studio-properties__table-actions {
		margin-bottom: 0.15rem;
	}

	.studio-properties__multi {
		margin: 0;
		border-bottom: 1px solid var(--studio-line);
		color: var(--studio-text-idle);
		padding: 0.35rem 0.6rem;
		font-size: 0.72rem;
	}

	.studio-properties__null {
		color: var(--studio-text-idle);
		font-family: var(--font-mono, monospace);
		font-style: italic;
	}

	.studio-properties__fallback {
		min-width: 0;
		align-items: center;
	}

	.studio-properties__fallback-value {
		display: block;
		flex: 1 1 auto;
		max-height: 7.5rem;
		min-height: 2rem;
		overflow: auto;
		margin: 0;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.35rem;
		background: color-mix(in oklab, var(--color-surface-100-900) 72%, transparent);
		color: var(--color-surface-800-200);
		padding: 0.45rem 0.55rem;
		font-family: var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
		font-size: 0.7rem;
		line-height: 1.35;
		overflow-wrap: anywhere;
		white-space: pre-wrap;
	}

	.studio-properties__fallback-value--compact {
		min-height: 1.75rem;
		max-height: 1.75rem;
		overflow: hidden;
		padding-block: 0.34rem;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-properties__fallback-actions {
		flex: 0 0 auto;
		justify-content: flex-end;
	}

	.studio-properties__editor {
		position: fixed;
		inset: 3rem;
		z-index: 95;
		display: grid;
		grid-template-rows: auto minmax(0, 1fr);
		overflow: hidden;
		border: 1px solid var(--color-surface-300-700);
		border-radius: 0.45rem;
		background: #1e1e1e;
		box-shadow: 0 1.5rem 4rem rgb(0 0 0 / 0.35);
	}

	.studio-properties__editor::before {
		position: fixed;
		inset: -3rem;
		z-index: -1;
		background: rgb(0 0 0 / 0.45);
		content: '';
	}

	.studio-properties__editor-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 1rem;
		border-bottom: 1px solid var(--color-surface-700);
		background: color-mix(in oklab, #1e1e1e 90%, var(--color-primary-500));
		padding: 0.55rem 0.7rem;
		color: white;
	}

	.studio-properties__editor-header div:first-child {
		display: grid;
		min-width: 0;
		gap: 0.1rem;
	}

	.studio-properties__editor-header strong,
	.studio-properties__editor-header span {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-properties__editor-header span {
		color: #b8c2d6;
		font-size: 0.7rem;
	}

	.studio-properties__editor-actions {
		display: flex;
		flex: 0 0 auto;
		gap: 0.45rem;
	}

	.studio-properties__editor-body {
		min-height: 0;
	}

	@media (max-width: 760px) {
		.studio-properties__editor {
			inset: 0.5rem;
		}
	}
</style>
