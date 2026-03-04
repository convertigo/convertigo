<script>
	import { Switch } from '@skeletonlabs/skeleton-svelte';
	import { goto } from '$app/navigation';
	import ActionBar from '$lib/admin/components/ActionBar.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import InputGroup from '$lib/common/components/InputGroup.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import LightSvelte from '$lib/common/Light.svelte.js';
	import Editor from '$lib/studio/editor/Editor.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { onDestroy, onMount } from 'svelte';
	import RightPart from '../RightPart.svelte';
	import {
		createMangoIndex,
		encodeFullSyncDesignDocPath as encodeDesignDocPath,
		explainMangoQuery,
		fullSyncBaseUrl,
		getDesignDocument,
		listDesignDocuments,
		listDocumentIdSuggestions,
		listDocuments,
		listMangoIndexes,
		removeDesignDocument,
		removeDocuments,
		removeMangoIndexes,
		runMangoQuery,
		runViewQuery,
		saveDesignDocument
	} from './fullsync-api';
	import { createFullSyncFeedback, fullSyncErrorMessage } from './fullsync-feedback';
	import { fullSyncPretty, parseFullSyncJson, parseFullSyncJsonSilent } from './fullsync-json';
	import { FULLSYNC_DOCS, openFullSyncJsonPayload, openFullSyncLink } from './fullsync-links';
	import { getFullSyncConfirmModal, openFullSyncConfirmation } from './fullsync-modal';
	import {
		fullSyncDbAllDocsHref,
		fullSyncDbIndexHref,
		fullSyncDbMangoHref,
		fullSyncDbTabHref,
		fullSyncDbViewEditHref,
		fullSyncDbViewHref,
		fullSyncDocHref,
		fullSyncHomeHref
	} from './fullsync-route';
	import {
		FULLSYNC_PAGE_OPTIONS_BASE,
		FULLSYNC_PAGE_OPTIONS_WITH_500,
		FULLSYNC_PAGE_OPTIONS_WITH_500_AND_NONE,
		fullSyncOptionLabel
	} from './fullsync-ui';
	import FullSyncRowsPanel from './FullSyncRowsPanel.svelte';

	/** @type {{database: string, section?: 'all' | 'mango' | 'index', designDocId?: string, viewName?: string, mode?: string}} */
	let { database = '', section = 'all', designDocId = '', viewName = '', mode = '' } = $props();

	const modalYesNo = getFullSyncConfirmModal();

	let cloneViewModal = $state(/** @type {any} */ (undefined));

	let loadingDocuments = $state(false);
	let loadingDesignDocs = $state(false);
	let working = $state(false);
	let savingView = $state(false);
	let lastError = $state('');
	const { showError, showSuccess } = createFullSyncFeedback((message) => {
		lastError = message;
	});
	const parseJsonSilent = parseFullSyncJsonSilent;
	const pretty = fullSyncPretty;
	const parseJson = (content, label) => parseFullSyncJson(content, label, showError);

	let documents = $state(/** @type {any[]} */ ([]));
	let docsTotalRows = $state(0);
	let docsSkip = $state(0);
	let docsPageSizeValue = $state('20');
	let docsQueryLimitValue = $state('none');
	let docsDescending = $state(false);
	let docsIncludeDocs = $state(false);
	let docsReduce = $state(false);
	let docsGroupLevel = $state('exact');
	let docsShowAllColumns = $state(false);
	let docsColumnSelection = $state(/** @type {string[]} */ ([]));
	let docsHasNextPage = $state(false);
	let docsStable = $state(false);
	let docsUpdate = $state('true');
	let docsKeyMode = $state('by-keys');
	let docsKeyValue = $state('');
	let docsStartKeyValue = $state('');
	let docsEndKeyValue = $state('');
	let docsRequestCounter = 0;
	let queryOptionsOpen = $state(false);

	let quickDocId = $state('');
	let quickDocLiveSuggestions = $state(/** @type {string[]} */ ([]));
	let quickDocSuggestRequestId = 0;
	let quickDocSuggestTimer = /** @type {ReturnType<typeof setTimeout> | undefined} */ (undefined);
	let currentLayout = $state('metadata');
	let selectedDocIds = $state(/** @type {Record<string, boolean>} */ ({}));
	let loadedDatabase = $state('');
	let loadedQueryScope = $state('');
	let loadedMangoIndexScope = $state('');
	let docsQueryType = $state('');

	let designDocs = $state(
		/** @type {Array<{id: string, name: string, views: Array<{name: string, map?: string, reduce?: string}>, doc: any}>} */ ([])
	);
	let designDocExpanded = $state(/** @type {Record<string, boolean>} */ ({}));

	let mangoQueryText = $state(
		'{\n  "selector": {\n    "_id": {\n      "$gt": null\n    }\n  },\n  "limit": 25\n}'
	);
	let mangoResult = $state(/** @type {any} */ (null));
	let mangoExplainText = $state('');
	let mangoResultLayout = $state('table');
	let mangoShowAllColumns = $state(false);
	let mangoColumnSelection = $state(/** @type {string[]} */ ([]));
	let mangoSelectedDocIds = $state(/** @type {Record<string, boolean>} */ ({}));
	let mangoHasNextPage = $state(false);
	let mangoSkip = $state(0);
	let mangoLimitValue = $state('20');
	let mangoLastRunMs = $state(0);
	let mangoHistory = $state(/** @type {Array<{label: string, value: string}>} */ ([]));
	let selectedMangoHistory = $state('');
	let mangoIndexesLoading = $state(false);
	let mangoIndexes = $state(/** @type {any[]} */ ([]));
	let mangoIndexesSelection = $state(/** @type {Record<string, boolean>} */ ({}));
	let mangoIndexText = $state(
		'{\n  "index": {\n    "fields": [\n      "foo"\n    ]\n  },\n  "name": "foo-json-index",\n  "type": "json"\n}'
	);
	let mangoIndexExample = $state('example');
	let mangoIndexSkip = $state(0);
	let mangoIndexLimitValue = $state('20');

	let cloneSourceDesignDocId = $state('');
	let cloneSourceViewName = $state('');
	let cloneTargetDesignDocId = $state('');
	let cloneNewDesignDocName = $state('');
	let cloneViewName = $state('');
	let cloneViewError = $state('');

	let viewEditorDesignDocId = $state('');
	let viewEditorOriginalDesignDocId = $state('');
	let viewEditorName = $state('');
	let viewEditorOriginalName = $state('');
	let viewEditorMap = $state('');
	let viewEditorReduceOption = $state('NONE');
	let viewEditorReduceCustom = $state(
		'function (keys, values, rereduce) {\n  if (rereduce) {\n    return sum(values);\n  }\n  return values.length;\n}'
	);
	let viewEditorNewDesignDocName = $state('');
	let viewEditorLoadedKey = $state('');

	let currentTab = $derived(section == 'mango' || section == 'index' ? section : 'all');
	let currentMode = $derived(mode || '');
	let selectedDesignDocId = $derived(normalizeDesignDocId((designDocId || '').trim()));
	let selectedViewName = $derived((viewName || '').trim());
	let isViewEditor = $derived(
		currentTab == 'all' &&
			currentMode == 'view-edit' &&
			selectedDesignDocId.length > 0 &&
			selectedViewName.length > 0
	);
	let isViewQuery = $derived(
		currentTab == 'all' &&
			currentMode != 'view-edit' &&
			selectedDesignDocId.length > 0 &&
			selectedViewName.length > 0
	);

	let docsPageSize = $derived(Math.max(1, Number(docsPageSizeValue) || 20));
	let docsQueryHasLimit = $derived(docsQueryLimitValue != 'none');
	let docsQueryLimit = $derived(Math.max(1, Number(docsQueryLimitValue) || docsPageSize + 1));
	let docsFetchLimit = $derived.by(() => {
		const pageFetchLimit = docsPageSize + 1;
		return docsQueryHasLimit ? Math.min(pageFetchLimit, docsQueryLimit) : pageFetchLimit;
	});
	let canGoPreviousDocs = $derived(docsSkip > 0);
	let canGoNextDocs = $derived.by(() => {
		if (docsQueryHasLimit && docsSkip + docsPageSize >= docsQueryLimit) {
			return false;
		}
		if (docsHasNextPage) return true;
		const total = Number(docsTotalRows);
		if (Number.isFinite(total) && total > 0) {
			return docsSkip + docsPageSize < total;
		}
		return false;
	});

	let documentRows = $derived(
		documents.map((row) => {
			const hasValue = Boolean(row && typeof row == 'object' && 'value' in row);
			const value = hasValue ? row.value : {};
			const key = row?.key ?? row?.id ?? '';
			const doc = row?.doc ?? null;
			const docRaw = doc && typeof doc == 'object' ? doc : null;
			const rowId = row?.id ?? docRaw?._id ?? '';
			const rowRev = value?.rev ?? docRaw?._rev ?? '';
			const rowType = docRaw?.type ?? (rowId.startsWith('_design/') ? 'design' : '');
			const raw = isViewQuery ? row : (docRaw ?? row);
			const tableRaw = isViewQuery
				? (() => {
						const base =
							docRaw && typeof docRaw == 'object'
								? parseJsonSilent(JSON.stringify(docRaw), {})
								: {};
						if (rowId && !Object.prototype.hasOwnProperty.call(base, 'id')) {
							base.id = rowId;
						}
						if (rowRev && !Object.prototype.hasOwnProperty.call(base, 'rev')) {
							base.rev = rowRev;
						}
						if (row?.key !== undefined && !Object.prototype.hasOwnProperty.call(base, 'key')) {
							base.key = row.key;
						}
						if (value !== null && value !== undefined) {
							if (value && typeof value == 'object' && !Array.isArray(value)) {
								for (const [field, fieldValue] of Object.entries(value)) {
									const name = String(field ?? '');
									if (!name) continue;
									if (Object.prototype.hasOwnProperty.call(base, name)) {
										base[`value:${name}`] = fieldValue;
									} else {
										base[name] = fieldValue;
									}
								}
							} else if (Object.prototype.hasOwnProperty.call(base, 'value')) {
								base['value:value'] = value;
							} else {
								base.value = value;
							}
						}
						return base;
					})()
				: (docRaw ?? row);
			const attachments = docRaw?._attachments;
			const attachmentCount = countEntries(attachments);
			const conflictCount = countEntries(docRaw?._conflicts);
			return {
				id: rowId,
				key: typeof key == 'string' ? key : JSON.stringify(key),
				rev: rowRev,
				type: rowType,
				valueText: pretty(value),
				valuePreview: formatMetadataValue(value),
				jsonText: pretty(raw),
				hasDocument: Boolean(rowId),
				raw,
				tableRaw,
				attachmentCount,
				conflictCount
			};
		})
	);
	let mangoRows = $derived.by(() => {
		const docs = Array.isArray(mangoResult?.docs) ? mangoResult.docs : [];
		return docs.map((doc) => {
			const attachments = doc?._attachments;
			const attachmentCount = countEntries(attachments);
			const conflictCount = countEntries(doc?._conflicts);
			return {
				id: doc?._id ?? '',
				rev: doc?._rev ?? '',
				type: doc?.type ?? '',
				jsonText: pretty(doc),
				raw: doc,
				attachmentCount,
				conflictCount
			};
		});
	});
	let hasMangoDocs = $derived(mangoRows.length > 0);
	let mangoHasLimit = $derived(mangoLimitValue != 'none');
	let mangoLimitNumber = $derived(Math.max(1, Number(mangoLimitValue) || 20));
	let mangoPagedRows = $derived.by(() => {
		if (!mangoHasLimit) return mangoRows;
		return mangoRows.slice(0, mangoLimitNumber);
	});
	let mangoSchemaDocs = $derived(
		mangoPagedRows.map((row) => row?.raw).filter((doc) => Boolean(doc) && typeof doc == 'object')
	);
	let mangoSchema = $derived(getPseudoSchema(mangoSchemaDocs));
	let mangoDisplayableColumns = $derived(
		mangoSchema.filter((column) => typeof column == 'string' && column != '_attachments')
	);
	let mangoDefaultColumns = $derived(getPrioritizedFields(mangoSchemaDocs, 5));
	let mangoVisibleColumnCount = $derived(
		mangoShowAllColumns
			? mangoDisplayableColumns.length
			: Math.min(5, mangoDisplayableColumns.length)
	);
	let mangoVisibleColumns = $derived(
		resolveVisibleColumns(
			mangoDisplayableColumns,
			mangoDefaultColumns,
			mangoColumnSelection,
			mangoVisibleColumnCount
		)
	);
	let mangoCanGoPrevious = $derived(mangoHasLimit && mangoSkip > 0);
	let mangoCanGoNext = $derived.by(() => {
		if (!mangoHasLimit) return false;
		return mangoHasNextPage;
	});
	let mangoSelectableRows = $derived(
		mangoPagedRows.filter((row) => Boolean(row?.id) && Boolean(row?.rev))
	);
	let isMangoExplainMode = $derived(mangoExplainText.trim().length > 0);
	let mangoSelectedRows = $derived(
		mangoPagedRows.filter(
			(row) => Boolean(mangoSelectedDocIds[row.id]) && Boolean(row?.id) && Boolean(row?.rev)
		)
	);
	let mangoSelectedCount = $derived(mangoSelectedRows.length);
	let mangoAllSelectableRowsSelected = $derived(
		mangoSelectableRows.length > 0 &&
			mangoSelectableRows.every((row) => Boolean(mangoSelectedDocIds[row.id]))
	);
	let mangoTableColspan = $derived(mangoVisibleColumns.length + 3);
	let mangoIndexRows = $derived.by(() =>
		mangoIndexes.map((index, indexPosition) => {
			const ddoc = typeof index?.ddoc == 'string' ? index.ddoc : null;
			const name = String(index?.name ?? '').trim();
			const type = String(index?.type ?? '').trim() || 'json';
			const firstSpecialField =
				typeof index?.def?.fields?.[0] == 'object' && index?.def?.fields?.[0] != null
					? (Object.keys(index.def.fields[0])[0] ?? '')
					: '';
			const id = type == 'special' ? '_all_docs' : ddoc || '';
			const key = `${ddoc ?? '_all_docs'}::${name || indexPosition}`;
			return {
				key,
				id,
				ddoc,
				name,
				displayName: type == 'special' ? firstSpecialField || '_id' : name,
				type,
				isBulkDeletable: id.length > 0 && id != '_all_docs',
				jsonText: pretty(index),
				raw: index
			};
		})
	);
	let mangoIndexHasLimit = $derived(mangoIndexLimitValue != 'none');
	let mangoIndexLimitNumber = $derived(Math.max(1, Number(mangoIndexLimitValue) || 20));
	let mangoIndexPagedRows = $derived.by(() => {
		if (!mangoIndexHasLimit) return mangoIndexRows;
		return mangoIndexRows.slice(mangoIndexSkip, mangoIndexSkip + mangoIndexLimitNumber);
	});
	let mangoIndexCanGoPrevious = $derived(mangoIndexHasLimit && mangoIndexSkip > 0);
	let mangoIndexCanGoNext = $derived.by(() => {
		if (!mangoIndexHasLimit) return false;
		return mangoIndexSkip + mangoIndexLimitNumber < mangoIndexRows.length;
	});
	let mangoIndexSelectableRows = $derived(mangoIndexPagedRows.filter((row) => row.isBulkDeletable));
	let mangoIndexSelectedRows = $derived(
		mangoIndexRows.filter((row) => row.isBulkDeletable && Boolean(mangoIndexesSelection[row.key]))
	);
	let mangoIndexSelectedCount = $derived(mangoIndexSelectedRows.length);
	let mangoIndexAllSelectableRowsSelected = $derived(
		mangoIndexSelectableRows.length > 0 &&
			mangoIndexSelectableRows.every((row) => Boolean(mangoIndexesSelection[row.key]))
	);

	let designDocSelectItems = $derived(
		designDocs.map((designDoc) => ({
			value: designDoc.id,
			text: designDoc.id
		}))
	);
	let viewEditorDesignDocItems = $derived([
		{ value: 'new-doc', text: 'Create new design document' },
		...designDocSelectItems
	]);

	let selectableDocumentRows = $derived(
		documentRows.filter((row) => Boolean(row?.id) && Boolean(row?.rev))
	);
	let selectedDocumentRows = $derived(
		documentRows.filter(
			(row) => Boolean(selectedDocIds[row.id]) && Boolean(row?.id) && Boolean(row?.rev)
		)
	);
	let selectedDocumentCount = $derived(selectedDocumentRows.length);
	let allSelectableRowsSelected = $derived(
		selectableDocumentRows.length > 0 &&
			selectableDocumentRows.every((row) => Boolean(selectedDocIds[row.id]))
	);
	let docsSchemaRows = $derived(
		documentRows
			.map((row) => (isViewQuery ? row?.tableRaw : row?.raw))
			.filter((doc) => Boolean(doc) && typeof doc == 'object')
	);
	let docsSchema = $derived(getPseudoSchema(docsSchemaRows));
	let docsDisplayableColumns = $derived(
		docsSchema.filter(
			(column) => typeof column == 'string' && column != 'doc' && column != '_attachments'
		)
	);
	let docsDefaultColumns = $derived.by(() => {
		return getPrioritizedFields(docsSchemaRows, 5);
	});
	let docsVisibleColumnCount = $derived(
		docsShowAllColumns ? docsDisplayableColumns.length : Math.min(5, docsDisplayableColumns.length)
	);
	let docsVisibleColumns = $derived(
		resolveVisibleColumns(
			docsDisplayableColumns,
			docsDefaultColumns,
			docsColumnSelection,
			docsVisibleColumnCount
		)
	);
	let showDocsAttachmentColumn = $derived(currentLayout == 'table');
	let tableColspan = $derived(
		currentLayout == 'metadata'
			? 5
			: currentLayout == 'table'
				? docsVisibleColumns.length + 2 + (showDocsAttachmentColumn ? 1 : 0)
				: 3
	);
	let quickDocDatalistId = $derived(`fullsync-doc-ids-${database.replace(/[^a-zA-Z0-9_-]/g, '-')}`);
	let quickDocCandidates = $derived.by(() => {
		const prefix = quickDocId.trim().toLowerCase();
		const visibleIds = documents
			.map((row) => (typeof row?.id == 'string' ? row.id : ''))
			.filter((id) => id.length > 0);
		const sourceIds = quickDocLiveSuggestions.length > 0 ? quickDocLiveSuggestions : visibleIds;
		const unique = [];
		for (const id of sourceIds) {
			if (prefix && !id.toLowerCase().startsWith(prefix)) continue;
			if (unique.includes(id)) continue;
			unique.push(id);
		}
		return unique.slice(0, 24);
	});
	let editorTheme = $derived(LightSvelte.light ? '' : 'vs-dark');
	let mangoHistoryStorageKey = $derived(
		`fullsync:mango:${database.replace(/[^a-zA-Z0-9_-]/g, '_') || '_'}`
	);
	let rawJsonUrl = $derived.by(() => {
		if (!database || currentTab != 'all' || isViewEditor) return '#';
		const query = buildDocsQuery();
		const queryString = new URLSearchParams(
			Object.entries(query)
				.filter(([, value]) => value != null && value !== '')
				.map(([key, value]) => [key, String(value)])
		).toString();
		const path = isViewQuery
			? `${encodeURIComponent(database)}/${encodeDesignDocPath(selectedDesignDocId)}/_view/${encodeURIComponent(selectedViewName)}`
			: `${encodeURIComponent(database)}/_all_docs`;
		return `${fullSyncBaseUrl()}${path}${queryString ? `?${queryString}` : ''}`;
	});
	let docsLayoutOptions = $derived.by(() =>
		isViewQuery && docsReduce
			? [{ value: 'metadata', text: 'Metadata' }]
			: [
					{ value: 'table', text: 'Table' },
					{ value: 'metadata', text: 'Metadata' },
					{ value: 'json', text: 'JSON' }
				]
	);
	let mangoIndexesUrl = $derived.by(() => {
		if (!database || currentTab != 'mango') return '#';
		return fullSyncDbIndexHref(database);
	});
	let mangoIndexRawJsonUrl = $derived.by(() => {
		if (!database || currentTab != 'index') return '#';
		return `${fullSyncBaseUrl()}${encodeURIComponent(database)}/_index`;
	});

	function parseViewKeyValue(raw, label, reportErrors = false) {
		const input = String(raw ?? '').trim();
		if (!input) {
			return { isSet: false };
		}
		const looksJson =
			/^[[{"]/.test(input) || /^-?\d/.test(input) || /^(true|false|null)\b/.test(input);
		if (!looksJson) {
			return { isSet: true, value: input };
		}
		try {
			return { isSet: true, value: JSON.parse(input) };
		} catch (error) {
			if (reportErrors) {
				const message = error instanceof Error ? error.message : String(error);
				showError(`${label}: invalid JSON (${message})`);
			}
			return { isSet: false, invalid: true };
		}
	}

	function buildViewKeyQuery(reportErrors = false) {
		if (!isViewQuery) return {};
		if (docsKeyMode == 'between-keys') {
			const start = parseViewKeyValue(docsStartKeyValue, 'Start key', reportErrors);
			const end = parseViewKeyValue(docsEndKeyValue, 'End key', reportErrors);
			if (start.invalid || end.invalid) {
				return null;
			}
			return {
				startkey: start.isSet ? start.value : undefined,
				endkey: end.isSet ? end.value : undefined
			};
		}
		const keyValue = parseViewKeyValue(docsKeyValue, 'Key', reportErrors);
		if (keyValue.invalid) {
			return null;
		}
		if (!keyValue.isSet) {
			return {};
		}
		if (Array.isArray(keyValue.value)) {
			return { keys: keyValue.value };
		}
		return { key: keyValue.value };
	}

	function getPseudoSchema(docs = []) {
		const cache = [];
		for (const doc of docs) {
			if (!doc || typeof doc != 'object') continue;
			for (const key of Object.keys(doc)) {
				cache.push(key);
			}
		}
		const unique = Array.from(new Set(cache));
		const idIndex = unique.indexOf('_id');
		if (idIndex > 0) {
			unique.splice(idIndex, 1);
			unique.unshift('_id');
		}
		return unique;
	}

	function getPrioritizedFields(docs = [], max = 5) {
		const counts = {};
		for (const doc of docs) {
			if (!doc || typeof doc != 'object') continue;
			for (const key of Object.keys(doc)) {
				counts[key] = (counts[key] ?? 0) + 1;
			}
		}
		delete counts.id;
		delete counts._rev;
		return Object.entries(counts)
			.sort((a, b) => {
				if (a[1] != b[1]) return b[1] - a[1];
				if (a[0] == b[0]) return 0;
				return a[0] < b[0] ? -1 : 1;
			})
			.slice(0, Math.max(1, Number(max) || 5))
			.map(([field]) => field);
	}

	function resolveVisibleColumns(available = [], preferred = [], selected = [], visibleCount = 5) {
		const safeAvailable = Array.isArray(available) ? available : [];
		const safePreferred = (Array.isArray(preferred) ? preferred : []).filter((column) =>
			safeAvailable.includes(column)
		);
		const safeSelected = Array.isArray(selected) ? selected : [];
		const used = [];
		const columns = [];
		for (let index = 0; index < Math.max(0, Number(visibleCount) || 0); index += 1) {
			let next = safeSelected[index];
			if (!next || !safeAvailable.includes(next) || used.includes(next)) {
				next =
					safePreferred.find((column) => !used.includes(column)) ??
					safeAvailable.find((column) => !used.includes(column)) ??
					'';
			}
			if (!next) break;
			columns.push(next);
			used.push(next);
		}
		return columns;
	}

	function toggleSelectionMap(selection, key, checked) {
		if (!key) return selection;
		const next = { ...selection };
		if (checked) {
			next[key] = true;
		} else {
			delete next[key];
		}
		return next;
	}

	function selectAllFromRows(rows, checked, keyField, selection = {}) {
		if (!checked) {
			return {};
		}
		const next = { ...selection };
		for (const row of rows) {
			const key = String(row?.[keyField] ?? '');
			if (!key) continue;
			next[key] = true;
		}
		return next;
	}

	function setColumnSelection(selection, availableColumns, index, value) {
		const nextValue = String(value ?? '').trim();
		if (!nextValue) return selection;
		if (!availableColumns.includes(nextValue)) return selection;
		const next = [...selection];
		next[index] = nextValue;
		return next;
	}

	function formatMangoCellValue(value) {
		if (value == null) return '';
		if (typeof value == 'string') return value;
		if (typeof value == 'number' || typeof value == 'boolean') return String(value);
		try {
			return JSON.stringify(value);
		} catch {
			return String(value);
		}
	}

	function formatMetadataValue(value) {
		if (value === undefined) return '';
		if (value === null) return 'null';
		if (typeof value == 'string') return value;
		if (typeof value == 'number' || typeof value == 'boolean') return String(value);
		try {
			return JSON.stringify(value);
		} catch {
			return String(value);
		}
	}

	function countEntries(value) {
		if (!value || typeof value != 'object') return 0;
		if (Array.isArray(value)) return value.length;
		return Object.keys(value).length;
	}

	function docsColumnLabel(column) {
		const value = String(column ?? '');
		if (isViewQuery && value.startsWith('value:')) {
			return value.slice('value:'.length);
		}
		return value;
	}

	function getDocsCellValue(row, column) {
		const key = String(column ?? '');
		return row?.tableRaw?.[key];
	}

	function isDocsIdColumn(column) {
		const value = String(column ?? '');
		return value == 'id' || value == '_id';
	}

	function getDocsAttachmentCount(row) {
		return Number(row?.attachmentCount) || 0;
	}

	function getDocsConflictCount(row) {
		return Number(row?.conflictCount) || 0;
	}

	function getMangoAttachmentCount(row) {
		return Number(row?.attachmentCount) || 0;
	}

	function getMangoConflictCount(row) {
		return Number(row?.conflictCount) || 0;
	}

	function getDesignDocEntry(designDocId) {
		return designDocs.find((designDoc) => designDoc.id == designDocId) ?? null;
	}

	function getDesignDocModel(designDocId) {
		const entry = getDesignDocEntry(designDocId);
		const doc = entry?.doc;
		if (!doc || typeof doc != 'object') return null;
		return parseJsonSilent(JSON.stringify(doc), null);
	}

	function normalizeDesignDocId(input) {
		const raw = String(input ?? '').trim();
		if (!raw) return '';
		return raw.startsWith('_design/') ? raw : `_design/${raw}`;
	}

	function designDocIndexCount(doc) {
		const containers = ['views', 'lists', 'shows', 'updates', 'filters', 'indexes', 'search'];
		return containers.reduce((count, key) => {
			const value = doc?.[key];
			if (!value || typeof value != 'object') return count;
			return count + Object.keys(value).length;
		}, 0);
	}

	function defaultMapFunction() {
		return 'function (doc) {\n  emit(doc._id, doc);\n}';
	}

	function buildViewGroupingQuery() {
		if (!isViewQuery || !docsReduce) {
			return {
				group: undefined,
				groupLevel: undefined
			};
		}
		const raw = String(docsGroupLevel ?? 'exact')
			.trim()
			.toLowerCase();
		if (!raw || raw == 'exact') {
			return {
				group: true,
				groupLevel: undefined
			};
		}
		const parsed = Number(raw);
		if (Number.isInteger(parsed) && parsed >= 0) {
			return {
				group: undefined,
				groupLevel: parsed
			};
		}
		return {
			group: true,
			groupLevel: undefined
		};
	}

	function buildDocsQuery() {
		const viewKeyQuery = buildViewKeyQuery(false) ?? {};
		const useIncludeDocs = docsIncludeDocs && (!isViewQuery || !docsReduce);
		const groupingQuery = buildViewGroupingQuery();
		return {
			include_docs: useIncludeDocs ? true : undefined,
			descending: docsDescending ? true : undefined,
			stable: docsStable || undefined,
			update: docsUpdate == 'true' ? undefined : docsUpdate,
			limit: docsFetchLimit,
			skip: docsSkip,
			conflicts: useIncludeDocs ? true : undefined,
			reduce: isViewQuery ? docsReduce : undefined,
			group: groupingQuery.group,
			group_level: groupingQuery.groupLevel,
			...viewKeyQuery
		};
	}

	function setLayout(layout) {
		if (isViewQuery && docsReduce && layout != 'metadata') {
			layout = 'metadata';
		}
		if (layout == currentLayout) return;
		currentLayout = layout;
		docsSkip = 0;
		if (currentTab == 'all' && !isViewEditor) {
			const nextIncludeDocs = layout != 'metadata';
			docsIncludeDocs = isViewQuery && docsReduce ? false : nextIncludeDocs;
			loadedQueryScope = `${database}|${selectedDesignDocId}|${selectedViewName}|${currentLayout}`;
			void refreshDocuments();
		}
	}

	function setRowSelection(docId, checked) {
		selectedDocIds = toggleSelectionMap(selectedDocIds, docId, checked);
	}

	function setAllRowSelection(checked) {
		selectedDocIds = selectAllFromRows(selectableDocumentRows, checked, 'id');
	}

	function toggleDesignDoc(designDocId) {
		const current = designDocExpanded[designDocId] ?? false;
		designDocExpanded = { ...designDocExpanded, [designDocId]: !current };
	}

	function isDesignDocExpanded(designDocId) {
		return designDocExpanded[designDocId] ?? selectedDesignDocId == designDocId;
	}

	async function openDocument(docId) {
		if (!database || !docId) return;
		await goto(fullSyncDocHref(database, docId));
	}

	async function openDocumentFromToolbar(event) {
		event?.preventDefault?.();
		const docId = quickDocId.trim();
		if (!docId) return;
		await openDocument(docId);
	}

	function clearQuickDocSuggestTimer() {
		if (quickDocSuggestTimer !== undefined) {
			clearTimeout(quickDocSuggestTimer);
			quickDocSuggestTimer = undefined;
		}
	}

	async function refreshQuickDocSuggestions(prefix, requestId) {
		if (!database) return;
		try {
			const ids = await listDocumentIdSuggestions(database, { prefix, limit: 24 });
			if (requestId != quickDocSuggestRequestId) return;
			quickDocLiveSuggestions = ids;
		} catch {
			if (requestId != quickDocSuggestRequestId) return;
			quickDocLiveSuggestions = [];
		}
	}

	function openRawJson() {
		if (isViewEditor) {
			if (!database || !selectedDesignDocId || selectedDesignDocId == 'new-doc') return;
			const url = `${fullSyncBaseUrl()}${encodeURIComponent(database)}/${encodeDesignDocPath(selectedDesignDocId)}`;
			openFullSyncLink(url);
			return;
		}
		openFullSyncLink(rawJsonUrl);
	}

	function openDocumentation() {
		openFullSyncLink(FULLSYNC_DOCS.documentApi);
	}

	function openMangoDocumentation() {
		openFullSyncLink(FULLSYNC_DOCS.mango);
	}

	function openMangoResultJson() {
		let payload = '{}';
		if (mangoExplainText.trim().length > 0) {
			payload = mangoExplainText;
		} else if (mangoResult) {
			payload = pretty(mangoResult);
		}
		openFullSyncJsonPayload(payload);
	}

	function openMangoIndexJson() {
		openFullSyncLink(mangoIndexRawJsonUrl);
	}

	async function copyRow(row) {
		if (!row) return;
		try {
			await navigator.clipboard.writeText(pretty(row.raw ?? row));
			showSuccess('Row copied');
		} catch (error) {
			showError(error);
		}
	}

	function onIncludeDocsToggle(value) {
		docsIncludeDocs = value;
		if (value && docsReduce) {
			docsReduce = false;
		}
	}

	function onReduceToggle(value) {
		docsReduce = value;
		if (!isViewQuery) {
			docsReduce = false;
			return;
		}
		if (value) {
			docsIncludeDocs = false;
			if (currentLayout != 'metadata') {
				currentLayout = 'metadata';
			}
		}
	}

	async function refreshDocuments() {
		if (!database || currentTab != 'all' || isViewEditor) return;
		const requestId = ++docsRequestCounter;
		loadingDocuments = true;
		try {
			const viewKeyQuery = isViewQuery ? buildViewKeyQuery(true) : {};
			if (isViewQuery && !viewKeyQuery) {
				docsHasNextPage = false;
				documents = [];
				docsTotalRows = 0;
				selectedDocIds = {};
				return;
			}
			const useIncludeDocs = docsIncludeDocs && (!isViewQuery || !docsReduce);
			const groupingQuery = buildViewGroupingQuery();
			const query = {
				limit: docsFetchLimit,
				skip: docsSkip,
				includeDocs: useIncludeDocs,
				descending: docsDescending,
				stable: docsStable,
				update: docsUpdate,
				conflicts: useIncludeDocs,
				...viewKeyQuery
			};
			const response = isViewQuery
				? await runViewQuery(database, selectedDesignDocId, selectedViewName, {
						...query,
						reduce: docsReduce,
						group: groupingQuery.group,
						groupLevel: groupingQuery.groupLevel
					})
				: await listDocuments(database, query);
			if (requestId != docsRequestCounter) return;
			const rows = Array.isArray(response?.rows) ? response.rows : [];
			const hasOverflowRows = rows.length > docsPageSize;
			const reachedQueryLimit = docsQueryHasLimit && docsSkip + docsPageSize >= docsQueryLimit;
			docsHasNextPage = !reachedQueryLimit && hasOverflowRows;
			documents = hasOverflowRows ? rows.slice(0, docsPageSize) : rows;
			docsTotalRows = Number(response?.total_rows ?? documents.length) || 0;
			selectedDocIds = {};
		} catch (error) {
			documents = [];
			docsTotalRows = 0;
			docsHasNextPage = false;
			selectedDocIds = {};
			showError(error);
		} finally {
			loadingDocuments = false;
		}
	}

	async function deleteSelectedDocuments(event) {
		if (!database || selectedDocumentCount == 0) return;

		const docs = selectedDocumentRows.map((row) => ({
			_id: row.id,
			_rev: row.rev
		}));
		if (docs.length == 0) return;

		const ok = await openFullSyncConfirmation(
			modalYesNo,
			event,
			'Delete selected documents',
			`Do you confirm deleting ${docs.length} selected document(s)?`
		);
		if (!ok) return;

		working = true;
		lastError = '';
		try {
			const result = await removeDocuments(database, docs);
			const errors = Array.isArray(result) ? result.filter((item) => item?.error) : [];
			if (errors.length > 0) {
				showError(`${errors.length} document(s) failed to delete`);
			} else {
				showSuccess(`${docs.length} document(s) deleted`);
			}
			selectedDocIds = {};
			await refreshAll();
		} catch (error) {
			showError(error);
		} finally {
			working = false;
		}
	}

	async function refreshDesignDocs() {
		if (!database) return;
		loadingDesignDocs = true;
		try {
			const rows = await listDesignDocuments(database, { limit: 500, includeDocs: true });
			designDocs = rows
				.map((row) => {
					const id = row?.id ?? row?.doc?._id ?? '';
					const doc = row?.doc ?? {};
					const views = Object.entries(doc?.views ?? {}).map(([name, view]) => ({
						name,
						map: typeof view?.map == 'string' ? view.map : undefined,
						reduce: typeof view?.reduce == 'string' ? view.reduce : undefined
					}));
					views.sort((left, right) => left.name.localeCompare(right.name));
					return {
						id,
						name: id.replace('_design/', ''),
						views,
						doc
					};
				})
				.filter((designDoc) => {
					if (!designDoc?.id) return false;
					const language = String(designDoc?.doc?.language ?? '')
						.trim()
						.toLowerCase();
					return language != 'query';
				})
				.sort((left, right) => {
					const a = String(left?.id ?? '');
					const b = String(right?.id ?? '');
					if (a == b) return 0;
					return a < b ? -1 : 1;
				});
		} catch {
			designDocs = [];
		} finally {
			loadingDesignDocs = false;
		}
	}

	async function refreshAll() {
		const jobs = [];
		if (currentTab == 'all') {
			jobs.push(refreshDesignDocs());
		}
		if (currentTab == 'all' && !isViewEditor) {
			jobs.push(refreshDocuments());
		}
		await Promise.all(jobs);
	}

	function loadMangoHistory() {
		try {
			const raw = localStorage.getItem(mangoHistoryStorageKey);
			const value = parseJsonSilent(
				raw || '[]',
				/** @type {Array<{label: string, value: string}>} */ ([])
			);
			if (!Array.isArray(value)) {
				mangoHistory = [];
				return;
			}
			mangoHistory = value
				.filter((item) => item && typeof item.value == 'string')
				.slice(0, 30)
				.map((item, index) => ({
					label: item.label || `Query ${index + 1}`,
					value: item.value
				}));
		} catch {
			mangoHistory = [];
		}
	}

	function saveMangoHistory() {
		try {
			localStorage.setItem(mangoHistoryStorageKey, JSON.stringify(mangoHistory.slice(0, 30)));
		} catch {
			// ignore storage failures
		}
	}

	function rememberMangoQuery(queryText) {
		const value = String(queryText ?? '').trim();
		if (!value) return;
		const compact = value.replace(/\s+/g, ' ').trim();
		const label = compact.length > 56 ? `${compact.slice(0, 56)}…` : compact;
		const withoutCurrent = mangoHistory.filter((item) => item.value != value);
		mangoHistory = [{ label, value }, ...withoutCurrent].slice(0, 30);
		saveMangoHistory();
	}

	function onMangoHistorySelected(value) {
		selectedMangoHistory = String(value ?? '');
		const item = mangoHistory.find((entry) => entry.value == selectedMangoHistory);
		if (item) {
			mangoQueryText = item.value;
		}
	}

	function setMangoColumn(index, value) {
		mangoColumnSelection = setColumnSelection(
			mangoColumnSelection,
			mangoDisplayableColumns,
			index,
			value
		);
	}

	function setDocsColumn(index, value) {
		docsColumnSelection = setColumnSelection(
			docsColumnSelection,
			docsDisplayableColumns,
			index,
			value
		);
	}

	function setMangoRowSelection(docId, checked) {
		mangoSelectedDocIds = toggleSelectionMap(mangoSelectedDocIds, docId, checked);
	}

	function setAllMangoRowSelection(checked) {
		mangoSelectedDocIds = selectAllFromRows(
			mangoSelectableRows,
			checked,
			'id',
			mangoSelectedDocIds
		);
	}

	function buildMangoRequestPayload(sourcePayload) {
		const payload =
			sourcePayload && typeof sourcePayload == 'object' && !Array.isArray(sourcePayload)
				? { ...sourcePayload }
				: {};
		payload.execution_stats = true;
		if (mangoHasLimit) {
			payload.limit = mangoLimitNumber + 1;
			payload.skip = mangoSkip;
		} else if (payload.skip == null) {
			payload.skip = 0;
		}
		return payload;
	}

	async function executeMangoQuery({ resetSkip = false, showToast = false } = {}) {
		if (!database || working) return false;
		const payload = parseJson(mangoQueryText, 'Mango editor');
		if (!payload) return false;
		if (resetSkip) {
			mangoSkip = 0;
		}

		working = true;
		lastError = '';
		try {
			const start = performance.now();
			const requestPayload = buildMangoRequestPayload(payload);
			const result = await runMangoQuery(database, requestPayload);
			let docs = Array.isArray(result?.docs) ? result.docs : [];
			let hasNextPage = false;
			if (mangoHasLimit && docs.length > mangoLimitNumber) {
				hasNextPage = true;
				docs = docs.slice(0, mangoLimitNumber);
			}
			mangoHasNextPage = hasNextPage;
			mangoResult = { ...(result ?? {}), docs };
			mangoExplainText = '';
			mangoResultLayout = 'table';
			mangoSelectedDocIds = {};
			const serverMs = Number(result?.execution_stats?.execution_time_ms);
			mangoLastRunMs = Number.isFinite(serverMs)
				? Math.max(0, Math.round(serverMs))
				: Math.max(0, Math.round(performance.now() - start));
			rememberMangoQuery(mangoQueryText);
			if (showToast) {
				showSuccess('Mango query executed');
			}
			return true;
		} catch (error) {
			showError(error);
			return false;
		} finally {
			working = false;
		}
	}

	async function runMangoAction() {
		await executeMangoQuery({ resetSkip: true, showToast: true });
	}

	async function explainMangoAction() {
		if (!database || working) return;
		const payload = parseJson(mangoQueryText, 'Mango editor');
		if (!payload) return;

		working = true;
		lastError = '';
		try {
			const start = performance.now();
			const result = await explainMangoQuery(database, payload);
			mangoExplainText = pretty(result);
			mangoResult = null;
			mangoHasNextPage = false;
			mangoResultLayout = 'json';
			mangoSkip = 0;
			mangoSelectedDocIds = {};
			mangoLastRunMs = Math.max(0, Math.round(performance.now() - start));
			rememberMangoQuery(mangoQueryText);
			showSuccess('Mango explain executed');
		} catch (error) {
			showError(error);
		} finally {
			working = false;
		}
	}

	function onMangoIndexExampleChange(value) {
		mangoIndexExample = String(value ?? '');
		if (mangoIndexExample == 'example') {
			mangoIndexText =
				'{\n  "index": {\n    "fields": [\n      "foo"\n    ]\n  },\n  "name": "foo-json-index",\n  "type": "json"\n}';
			return;
		}
		if (mangoIndexExample == 'text') {
			mangoIndexText =
				'{\n  "index": {\n    "fields": [\n      {\n        "message": "asc"\n      }\n    ]\n  },\n  "name": "message-idx",\n  "type": "json"\n}';
		}
	}

	function setMangoIndexRowSelection(rowKey, checked) {
		mangoIndexesSelection = toggleSelectionMap(mangoIndexesSelection, rowKey, checked);
	}

	function setAllMangoIndexRowSelection(checked) {
		mangoIndexesSelection = selectAllFromRows(
			mangoIndexSelectableRows,
			checked,
			'key',
			mangoIndexesSelection
		);
	}

	async function refreshMangoIndexes() {
		if (!database || currentTab != 'index') return;
		mangoIndexesLoading = true;
		try {
			const response = await listMangoIndexes(database);
			mangoIndexes = Array.isArray(response?.indexes) ? response.indexes : [];
			mangoIndexesSelection = {};
		} catch (error) {
			mangoIndexes = [];
			mangoIndexesSelection = {};
			showError(error);
		} finally {
			mangoIndexesLoading = false;
		}
	}

	async function createMangoIndexAction(event) {
		event?.preventDefault?.();
		if (!database || working) return;
		const payload = parseJson(mangoIndexText, 'Mango index editor');
		if (!payload) return;

		working = true;
		lastError = '';
		try {
			await createMangoIndex(database, payload);
			showSuccess('Index created');
			await refreshMangoIndexes();
		} catch (error) {
			showError(error);
		} finally {
			working = false;
		}
	}

	async function deleteSelectedMangoIndexes(event) {
		if (!database || mangoIndexSelectedCount == 0 || working) return;

		const docIds = Array.from(
			new Set(
				mangoIndexSelectedRows
					.map((row) => row.id)
					.filter((id) => typeof id == 'string' && id.trim().length > 0 && id != '_all_docs')
			)
		);
		if (docIds.length == 0) return;

		const ok = await openFullSyncConfirmation(
			modalYesNo,
			event,
			'Delete selected indexes',
			`Do you confirm deleting ${docIds.length} selected index document(s)?`
		);
		if (!ok) return;

		working = true;
		lastError = '';
		try {
			await removeMangoIndexes(database, docIds);
			showSuccess(`${docIds.length} index document(s) deleted`);
			mangoIndexesSelection = {};
			await refreshMangoIndexes();
		} catch (error) {
			showError(error);
		} finally {
			working = false;
		}
	}

	function previousMangoIndexPage() {
		if (!mangoIndexHasLimit) return;
		mangoIndexSkip = Math.max(0, mangoIndexSkip - mangoIndexLimitNumber);
	}

	function nextMangoIndexPage() {
		if (!mangoIndexHasLimit) return;
		mangoIndexSkip += mangoIndexLimitNumber;
	}

	function previousMangoPage() {
		if (!mangoHasLimit) return;
		const previousSkip = mangoSkip;
		mangoSkip = Math.max(0, mangoSkip - mangoLimitNumber);
		if (mangoSkip == previousSkip) return;
		void executeMangoQuery({ resetSkip: false, showToast: false });
	}

	function nextMangoPage() {
		if (!mangoHasLimit || !mangoHasNextPage) return;
		mangoSkip += mangoLimitNumber;
		void executeMangoQuery({ resetSkip: false, showToast: false });
	}

	async function deleteSelectedMangoDocuments(event) {
		if (!database || mangoSelectedCount == 0) return;

		const docs = mangoSelectedRows.map((row) => ({
			_id: row.id,
			_rev: row.rev
		}));
		if (docs.length == 0) return;

		const ok = await openFullSyncConfirmation(
			modalYesNo,
			event,
			'Delete selected documents',
			`Do you confirm deleting ${docs.length} selected document(s)?`
		);
		if (!ok) return;

		working = true;
		lastError = '';
		try {
			const result = await removeDocuments(database, docs);
			const errors = Array.isArray(result) ? result.filter((item) => item?.error) : [];
			if (errors.length > 0) {
				showError(`${errors.length} document(s) failed to delete`);
			} else {
				showSuccess(`${docs.length} document(s) deleted`);
			}
			mangoSelectedDocIds = {};
			const removedIds = new Set(docs.map((doc) => doc._id));
			if (Array.isArray(mangoResult?.docs)) {
				const remaining = mangoResult.docs.filter((doc) => !removedIds.has(doc?._id ?? ''));
				mangoResult = { ...(mangoResult ?? {}), docs: remaining };
				mangoExplainText = '';
				mangoResultLayout = 'table';
				if (mangoHasLimit && mangoSkip >= remaining.length) {
					mangoSkip = Math.max(0, mangoSkip - mangoLimitNumber);
				}
			} else {
				await executeMangoQuery({ resetSkip: true, showToast: false });
			}
		} catch (error) {
			showError(error);
		} finally {
			working = false;
		}
	}

	async function previousDocsPage() {
		docsSkip = Math.max(0, docsSkip - docsPageSize);
		await refreshDocuments();
	}

	async function nextDocsPage() {
		docsSkip += docsPageSize;
		await refreshDocuments();
	}

	async function executeOptionsQuery() {
		const useIncludeDocs = docsIncludeDocs && (!isViewQuery || !docsReduce);
		const isMetadata = currentLayout == 'metadata';
		if (isMetadata && useIncludeDocs) {
			currentLayout = 'table';
		} else if (!isMetadata && !useIncludeDocs) {
			currentLayout = 'metadata';
		}
		loadedQueryScope = `${database}|${selectedDesignDocId}|${selectedViewName}|${currentLayout}`;
		await refreshDocuments();
		queryOptionsOpen = false;
	}

	async function createDocumentAction() {
		if (!database) return;
		await goto(fullSyncDocHref(database, '_new'));
	}

	async function createViewAction(designDocId = 'new-doc') {
		if (!database) return;
		const targetDesignDoc = String(designDocId || 'new-doc');
		await goto(fullSyncDbViewEditHref(database, targetDesignDoc, 'new-view'));
	}

	async function editViewAction(designDocId, viewName) {
		if (!database) return;
		await goto(fullSyncDbViewEditHref(database, designDocId, viewName));
	}

	async function deleteViewAction(event, designDocId, viewName) {
		if (!database || !designDocId || !viewName || working) return;
		const ok = await openFullSyncConfirmation(
			modalYesNo,
			event,
			'Delete view',
			`Do you confirm deleting view "${viewName}" from "${designDocId}"?`
		);
		if (!ok) return;

		working = true;
		lastError = '';
		try {
			const designDoc = await getDesignDocument(database, designDocId);
			const views = designDoc?.views ?? {};
			if (!views?.[viewName]) {
				throw new Error(`View "${viewName}" not found`);
			}
			delete views[viewName];
			designDoc.views = views;

			if (designDocIndexCount(designDoc) == 0) {
				await removeDesignDocument(database, designDoc._id, designDoc._rev);
			} else {
				await saveDesignDocument(database, designDoc);
			}

			showSuccess(`View "${viewName}" deleted`);
			await refreshDesignDocs();
			if (isViewQuery && selectedDesignDocId == designDocId && selectedViewName == viewName) {
				await goto(fullSyncDbTabHref(database, 'all'));
			} else {
				await refreshDocuments();
			}
		} catch (error) {
			showError(error);
		} finally {
			working = false;
		}
	}

	async function openCloneViewModal(event, designDocId, viewName) {
		if (!designDocId || !viewName) return;
		cloneSourceDesignDocId = designDocId;
		cloneSourceViewName = viewName;
		cloneTargetDesignDocId = designDocId;
		cloneNewDesignDocName = designDocId.replace(/^_design\//, '');
		cloneViewName = `${viewName}_copy`;
		cloneViewError = '';
		await cloneViewModal?.open({ event });
	}

	function closeCloneViewModal(value = false) {
		cloneViewError = '';
		cloneViewModal?.close(value);
	}

	async function cloneViewAction() {
		if (!database || working) return;
		const targetViewName = cloneViewName.trim();
		if (!targetViewName) {
			cloneViewError = 'Please provide a target view name.';
			return;
		}

		let targetDesignDocId = cloneTargetDesignDocId;
		if (targetDesignDocId == 'new-doc') {
			targetDesignDocId = normalizeDesignDocId(cloneNewDesignDocName);
			if (!targetDesignDocId) {
				cloneViewError = 'Please provide a target design document name.';
				return;
			}
		}

		working = true;
		cloneViewError = '';
		lastError = '';
		try {
			const sourceDesignDoc = await getDesignDocument(database, cloneSourceDesignDocId);
			const sourceView = sourceDesignDoc?.views?.[cloneSourceViewName];
			if (!sourceView) {
				throw new Error(`View "${cloneSourceViewName}" not found`);
			}

			let targetDesignDoc = null;
			if (targetDesignDocId == cloneSourceDesignDocId) {
				targetDesignDoc = sourceDesignDoc;
			} else if (cloneTargetDesignDocId == 'new-doc') {
				targetDesignDoc = {
					_id: targetDesignDocId,
					language: 'javascript',
					views: {}
				};
			} else {
				targetDesignDoc = await getDesignDocument(database, targetDesignDocId);
			}

			const views = targetDesignDoc?.views ?? {};
			if (views[targetViewName]) {
				throw new Error('That view name is already used in this design document.');
			}

			views[targetViewName] = parseJsonSilent(JSON.stringify(sourceView), {});
			targetDesignDoc.views = views;
			await saveDesignDocument(database, targetDesignDoc);
			showSuccess('View cloned');
			closeCloneViewModal(true);
			await refreshDesignDocs();
			await goto(fullSyncDbViewHref(database, targetDesignDocId, targetViewName));
		} catch (error) {
			cloneViewError = fullSyncErrorMessage(error);
			showError(error);
		} finally {
			working = false;
		}
	}

	async function loadViewEditorState() {
		if (!database || !isViewEditor) return;
		const stateKey = `${database}|${selectedDesignDocId}|${selectedViewName}`;
		if (stateKey == viewEditorLoadedKey) return;
		viewEditorLoadedKey = stateKey;

		let designDocId = selectedDesignDocId;
		let viewName = selectedViewName;
		let designDoc = getDesignDocModel(designDocId);
		if (!designDoc && designDocId != 'new-doc') {
			try {
				designDoc = await getDesignDocument(database, designDocId);
			} catch {
				designDoc = null;
			}
		}

		const currentView = designDoc?.views?.[viewName] ?? {};
		const mapCode =
			typeof currentView?.map == 'string' && currentView.map.trim().length > 0
				? currentView.map
				: defaultMapFunction();
		const reduceCode = typeof currentView?.reduce == 'string' ? currentView.reduce : '';
		const reduceOption = !reduceCode
			? 'NONE'
			: ['_sum', '_count', '_stats'].includes(reduceCode)
				? reduceCode
				: 'CUSTOM';

		viewEditorOriginalDesignDocId = designDocId;
		viewEditorOriginalName = viewName;
		viewEditorDesignDocId = designDocId == 'new-doc' ? 'new-doc' : designDocId;
		viewEditorName = viewName == 'new-view' ? '' : viewName;
		viewEditorMap = mapCode;
		viewEditorReduceOption = reduceOption;
		viewEditorReduceCustom =
			reduceOption == 'CUSTOM'
				? reduceCode
				: 'function (keys, values, rereduce) {\n  if (rereduce) {\n    return sum(values);\n  }\n  return values.length;\n}';
		viewEditorNewDesignDocName =
			designDocId && designDocId != 'new-doc'
				? designDocId.replace(/^_design\//, '')
				: 'new_design_doc';
	}

	function resolveViewReduceValue() {
		if (viewEditorReduceOption == 'NONE') return null;
		if (viewEditorReduceOption == 'CUSTOM') {
			const code = viewEditorReduceCustom.trim();
			return code || null;
		}
		return viewEditorReduceOption;
	}

	async function cancelViewEditor() {
		if (!database) return;
		if (
			selectedDesignDocId &&
			selectedDesignDocId != 'new-doc' &&
			selectedViewName &&
			selectedViewName != 'new-view'
		) {
			await goto(fullSyncDbViewHref(database, selectedDesignDocId, selectedViewName));
			return;
		}
		await goto(fullSyncDbTabHref(database, 'all'));
	}

	async function saveViewEditor(event) {
		event?.preventDefault?.();
		if (!database || savingView || working) return;

		const nextViewName = viewEditorName.trim();
		if (!nextViewName) {
			showError('View name is required.');
			return;
		}

		let targetDesignDocId = viewEditorDesignDocId;
		if (targetDesignDocId == 'new-doc') {
			targetDesignDocId = normalizeDesignDocId(viewEditorNewDesignDocName);
			if (!targetDesignDocId) {
				showError('Design document name is required.');
				return;
			}
		}

		const nextMap = viewEditorMap.trim() || defaultMapFunction();
		const nextReduce = resolveViewReduceValue();
		const sourceDesignDocId = viewEditorOriginalDesignDocId;
		const sourceViewName = viewEditorOriginalName;

		savingView = true;
		lastError = '';
		try {
			let targetDesignDoc;
			if (targetDesignDocId == sourceDesignDocId && sourceDesignDocId != 'new-doc') {
				targetDesignDoc = await getDesignDocument(database, targetDesignDocId);
			} else if (viewEditorDesignDocId == 'new-doc') {
				targetDesignDoc = {
					_id: targetDesignDocId,
					language: 'javascript',
					views: {}
				};
			} else {
				targetDesignDoc = await getDesignDocument(database, targetDesignDocId);
			}

			const targetViews = targetDesignDoc?.views ?? {};
			if (
				targetDesignDocId != sourceDesignDocId &&
				targetViews[nextViewName] &&
				!(sourceViewName == nextViewName && sourceDesignDocId == targetDesignDocId)
			) {
				throw new Error('That view name is already used in this design document.');
			}

			if (
				targetDesignDocId == sourceDesignDocId &&
				sourceViewName &&
				sourceViewName != 'new-view' &&
				sourceViewName != nextViewName
			) {
				delete targetViews[sourceViewName];
			}

			targetViews[nextViewName] = {
				map: nextMap,
				...(nextReduce ? { reduce: nextReduce } : {})
			};
			targetDesignDoc.views = targetViews;

			await saveDesignDocument(database, targetDesignDoc);

			if (
				sourceDesignDocId &&
				sourceDesignDocId != 'new-doc' &&
				(sourceDesignDocId != targetDesignDocId ||
					(sourceViewName && sourceViewName != 'new-view' && sourceViewName != nextViewName))
			) {
				const sourceDesignDoc = await getDesignDocument(database, sourceDesignDocId);
				if (sourceDesignDoc?.views?.[sourceViewName]) {
					delete sourceDesignDoc.views[sourceViewName];
					if (designDocIndexCount(sourceDesignDoc) == 0) {
						await removeDesignDocument(database, sourceDesignDoc._id, sourceDesignDoc._rev);
					} else {
						await saveDesignDocument(database, sourceDesignDoc);
					}
				}
			}

			showSuccess('View saved');
			await refreshDesignDocs();
			await goto(fullSyncDbViewHref(database, targetDesignDocId, nextViewName));
		} catch (error) {
			showError(error);
		} finally {
			savingView = false;
		}
	}

	$effect(() => {
		if (!isViewQuery) {
			if (docsReduce) docsReduce = false;
			if (docsGroupLevel != 'exact') docsGroupLevel = 'exact';
			return;
		}
		if (docsReduce && docsIncludeDocs) {
			docsIncludeDocs = false;
		}
		if (docsReduce && currentLayout != 'metadata') {
			currentLayout = 'metadata';
		}
	});

	$effect(() => {
		if (!database) return;
		if (database == loadedDatabase) return;
		loadedDatabase = database;
		loadedQueryScope = '';
		loadedMangoIndexScope = '';
		docsSkip = 0;
		documents = [];
		docsTotalRows = 0;
		docsHasNextPage = false;
		designDocs = [];
		designDocExpanded = {};
		mangoResult = null;
		mangoExplainText = '';
		mangoResultLayout = 'table';
		mangoShowAllColumns = false;
		mangoColumnSelection = [];
		docsShowAllColumns = false;
		docsColumnSelection = [];
		docsPageSizeValue = '20';
		docsQueryLimitValue = 'none';
		mangoSelectedDocIds = {};
		mangoSkip = 0;
		mangoLimitValue = '20';
		mangoLastRunMs = 0;
		mangoIndexes = [];
		mangoIndexesSelection = {};
		mangoIndexSkip = 0;
		mangoIndexLimitValue = '20';
		lastError = '';
		queryOptionsOpen = false;
		quickDocId = '';
		selectedDocIds = {};
		docsQueryType = isViewQuery ? 'view' : 'all';
		currentLayout = isViewQuery ? 'table' : 'metadata';
		docsIncludeDocs = isViewQuery;
		docsReduce = false;
		docsGroupLevel = 'exact';
		docsDescending = false;
		docsKeyMode = 'by-keys';
		docsKeyValue = '';
		docsStartKeyValue = '';
		docsEndKeyValue = '';
		viewEditorLoadedKey = '';
		loadMangoHistory();
		if (currentTab == 'all') {
			void refreshDesignDocs();
		}
		if (currentTab == 'all' && !isViewEditor) {
			loadedQueryScope = `${database}|${selectedDesignDocId}|${selectedViewName}|${currentLayout}`;
			void refreshDocuments();
		}
		if (currentTab == 'index') {
			loadedMangoIndexScope = `${database}|index`;
			void refreshMangoIndexes();
		}
	});

	$effect(() => {
		if (!database || currentTab != 'all' || isViewEditor) {
			clearQuickDocSuggestTimer();
			quickDocSuggestRequestId += 1;
			quickDocLiveSuggestions = [];
			return;
		}

		const prefix = quickDocId.trim();
		clearQuickDocSuggestTimer();
		quickDocSuggestRequestId += 1;
		const requestId = quickDocSuggestRequestId;

		if (!prefix) {
			quickDocLiveSuggestions = [];
			return;
		}

		quickDocSuggestTimer = setTimeout(() => {
			void refreshQuickDocSuggestions(prefix, requestId);
		}, 140);
	});

	$effect(() => {
		if (!database || currentTab != 'all' || isViewEditor) return;
		const queryScope = `${database}|${selectedDesignDocId}|${selectedViewName}|${currentLayout}`;
		if (queryScope == loadedQueryScope) return;
		loadedQueryScope = queryScope;
		const nextQueryType = isViewQuery ? 'view' : 'all';
		if (docsQueryType != nextQueryType) {
			docsQueryType = nextQueryType;
			docsIncludeDocs = isViewQuery;
			docsReduce = false;
			docsGroupLevel = 'exact';
		}
		docsSkip = 0;
		quickDocId = '';
		selectedDocIds = {};
		docsShowAllColumns = false;
		docsColumnSelection = [];
		if (!isViewQuery) {
			docsKeyMode = 'by-keys';
			docsKeyValue = '';
			docsStartKeyValue = '';
			docsEndKeyValue = '';
		}
		if (selectedDesignDocId) {
			designDocExpanded = {
				...designDocExpanded,
				[selectedDesignDocId]: true
			};
		}
		void refreshDocuments();
	});

	$effect(() => {
		if (!database || !isViewEditor) return;
		void loadViewEditorState();
	});

	$effect(() => {
		if (!database || currentTab != 'index') return;
		const scope = `${database}|index`;
		if (scope == loadedMangoIndexScope) return;
		loadedMangoIndexScope = scope;
		mangoIndexSkip = 0;
		mangoIndexesSelection = {};
		void refreshMangoIndexes();
	});

	$effect(() => {
		if (currentTab == 'index') return;
		loadedMangoIndexScope = '';
	});

	const rightPartOwner = Symbol('fullsync-database');
	$effect(() => {
		if (currentTab != 'all') {
			RightPart.release(rightPartOwner);
			return;
		}
		RightPart.claim(rightPartOwner, rightPart);
	});
	onMount(() => {
		loadMangoHistory();
	});
	onDestroy(() => {
		clearQuickDocSuggestTimer();
		quickDocSuggestRequestId += 1;
		RightPart.release(rightPartOwner);
	});
</script>

{#snippet rightPart()}
	<nav
		class="h-full w-full bg-surface-100-900 max-md:grid max-md:grid-cols-[repeat(auto-fit,minmax(11rem,1fr))] max-md:gap-1 md:layout-y-stretch-none md:w-[25rem]"
	>
		<div class="db-sidebar-head max-md:col-span-full">
			<a
				href={fullSyncHomeHref()}
				class="sidebar-head-back"
				title="Databases"
				aria-label="Databases"
			>
				<Ico icon="mdi:arrow-left" size={5} />
			</a>
			<div class="db-sidebar-name" title={database}>{database}</div>
		</div>

		<div class="sidebar-row-with-action">
			<a
				href={fullSyncDbTabHref(database, 'all')}
				aria-current={currentTab == 'all' && !isViewQuery && !isViewEditor ? 'page' : undefined}
				class="rail-link"
			>
				{#if currentTab == 'all' && !isViewQuery && !isViewEditor}
					<span class="absolute inset-0 rounded-sm bg-primary-100/70 dark:bg-primary-500/20"></span>
				{/if}
				<Ico
					icon="mdi:file-document-box-outline"
					size={5}
					class={`z-10 ${currentTab == 'all' && !isViewQuery && !isViewEditor ? 'rail-active' : 'text-strong'}`}
				/>
				<span
					class={`z-10 ${
						currentTab == 'all' && !isViewQuery && !isViewEditor
							? 'font-medium rail-active'
							: 'font-normal text-strong'
					}`}
				>
					All Documents
				</span>
			</a>
			<button
				type="button"
				class="sidebar-action-btn"
				title="Create document"
				aria-label="Create document"
				onclick={createDocumentAction}
			>
				<Ico icon="mdi:plus" size={4} />
			</button>
		</div>

		<a
			href={fullSyncDbTabHref(database, 'mango')}
			aria-current={currentTab == 'mango' ? 'page' : undefined}
			class="rail-link"
		>
			{#if currentTab == 'mango'}
				<span class="absolute inset-0 rounded-sm bg-primary-100/70 dark:bg-primary-500/20"></span>
			{/if}
			<Ico
				icon="mdi:magnify"
				size={5}
				class={`z-10 ${currentTab == 'mango' ? 'rail-active' : 'text-strong'}`}
			/>
			<span
				class={`z-10 ${currentTab == 'mango' ? 'font-medium rail-active' : 'font-normal text-strong'}`}
			>
				Run A Query with Mango
			</span>
		</a>

		<div class="sidebar-row-with-action border-t border-surface-200-800 px-5 pt-3 pb-2">
			<span class="text-[13px] font-medium text-strong">Design Documents</span>
			<button
				type="button"
				class="sidebar-action-btn"
				title="Create design document view"
				aria-label="Create design document view"
				onclick={() => createViewAction('new-doc')}
			>
				<Ico icon="mdi:plus" size={4} />
			</button>
		</div>
		{#if loadingDesignDocs}
			<div class="px-5 pb-2 text-xs text-muted">Loading...</div>
		{:else if designDocs.length == 0}
			<div class="px-5 pb-2 text-xs text-muted">No design documents</div>
		{:else}
			{#each designDocs as designDoc (designDoc.id)}
				{@const expanded = isDesignDocExpanded(designDoc.id)}
				{@const selectedDesignDoc = selectedDesignDocId == designDoc.id}
				<div class="design-doc-group">
					<div class="design-doc-head">
						<button
							type="button"
							class="design-doc-toggle"
							onclick={() => toggleDesignDoc(designDoc.id)}
							aria-label={expanded ? 'Collapse design document' : 'Expand design document'}
						>
							<Ico icon={expanded ? 'mdi:chevron-down' : 'mdi:chevron-right'} size={4} />
						</button>
						<button
							type="button"
							class="design-doc-link text-left"
							title={designDoc.id}
							aria-expanded={expanded}
							onclick={() => toggleDesignDoc(designDoc.id)}
						>
							{#if selectedDesignDoc || expanded}
								<span class="absolute inset-0 rounded-sm bg-primary-100/70 dark:bg-primary-500/20"
								></span>
							{/if}
							<Ico
								icon="mdi:file-document-box-outline"
								size={4}
								class={`z-10 ${selectedDesignDoc || expanded ? 'rail-active' : 'text-strong'}`}
							/>
							<span
								class={`z-10 truncate text-[13px] ${
									selectedDesignDoc || expanded ? 'font-medium rail-active' : 'text-strong'
								}`}
							>
								{designDoc.name}
							</span>
						</button>
						<button
							type="button"
							class="sidebar-action-btn design-doc-action"
							title="Create view"
							aria-label="Create view"
							onclick={() => createViewAction(designDoc.id)}
						>
							<Ico icon="mdi:plus" size={4} />
						</button>
					</div>
					{#if expanded}
						{#if designDoc.views.length > 0}
							<div class="design-doc-sub-title">Views</div>
							{#each designDoc.views as view (view.name)}
								{@const isCurrentView =
									isViewQuery &&
									selectedDesignDocId == designDoc.id &&
									selectedViewName == view.name}
								<div class="design-doc-view-row" data-selected={isCurrentView ? 'true' : 'false'}>
									<a
										href={fullSyncDbViewHref(database, designDoc.id, view.name)}
										aria-current={isCurrentView ? 'page' : undefined}
										class="design-doc-sub-link"
										title={view.name}
									>
										{#if isCurrentView}
											<span
												class="absolute inset-0 rounded-sm bg-primary-100/70 dark:bg-primary-500/20"
											></span>
										{/if}
										<span
											class={`z-10 truncate ${isCurrentView ? 'font-medium rail-active' : 'text-strong'}`}
										>
											{view.name}
										</span>
									</a>
									<div class="design-doc-view-actions">
										<button
											type="button"
											class="button-ico-primary h-6 w-6 justify-center p-0!"
											title="Edit view"
											aria-label="Edit view"
											onclick={() => void editViewAction(designDoc.id, view.name)}
										>
											<Ico icon="mdi:edit-outline" size={4} />
										</button>
										<button
											type="button"
											class="button-ico-primary h-6 w-6 justify-center p-0!"
											title="Clone view"
											aria-label="Clone view"
											onclick={(event) => void openCloneViewModal(event, designDoc.id, view.name)}
										>
											<Ico icon="mdi:cached" size={4} />
										</button>
										<button
											type="button"
											class="button-ico-primary h-6 w-6 justify-center p-0!"
											title="Delete view"
											aria-label="Delete view"
											onclick={(event) => void deleteViewAction(event, designDoc.id, view.name)}
										>
											<Ico icon="mdi:delete-outline" size={4} />
										</button>
									</div>
								</div>
							{/each}
						{:else}
							<div class="design-doc-sub-link text-muted">No views</div>
						{/if}
					{/if}
				</div>
			{/each}
		{/if}
	</nav>
{/snippet}

<div class="layout-y-stretch">
	{#if lastError}
		<div
			class="rounded-base border border-error-300-700 bg-error-100-900 px-3 py-2 text-sm text-error-900-100"
		>
			{lastError}
		</div>
	{/if}

	{#if currentTab == 'all'}
		{#if isViewEditor}
			<section class="fullsync-main-panel view-editor-panel">
				<header class="view-editor-header">
					<h3 class="text-xl font-semibold text-strong">
						{selectedViewName == 'new-view' ? 'Create View' : 'Edit View'}
					</h3>
					<ActionBar full={false} wrap={true} justify="start" class="w-fit gap-2 max-md:w-full">
						<Button
							label="JSON"
							icon="mdi:code-braces"
							class="button-secondary h-9 w-fit!"
							onclick={openRawJson}
						/>
						<Button
							icon="mdi:book-open-variant"
							class="button-secondary h-9 w-fit!"
							title="CouchDB documentation"
							ariaLabel="CouchDB documentation"
							onclick={openDocumentation}
						/>
					</ActionBar>
				</header>

				<form class="view-editor-form layout-y-stretch gap-4" onsubmit={saveViewEditor}>
					<fieldset
						class="view-editor-fieldset layout-y-stretch gap-3"
						disabled={savingView || working}
					>
						<div class="view-editor-fields">
							<PropertyType
								type="combo"
								name="viewEditorDesignDoc"
								label="Design Document"
								item={viewEditorDesignDocItems}
								bind:value={viewEditorDesignDocId}
								onchange={() => {
									if (viewEditorDesignDocId != 'new-doc') {
										viewEditorNewDesignDocName = viewEditorDesignDocId.replace(/^_design\//, '');
									}
								}}
							/>
							{#if viewEditorDesignDocId == 'new-doc'}
								<PropertyType
									type="text"
									name="viewEditorNewDesignDoc"
									label="New design document name"
									placeholder="new_design_doc"
									bind:value={viewEditorNewDesignDocName}
								/>
							{/if}
							<PropertyType
								type="text"
								name="viewEditorName"
								label="Index name"
								placeholder="myView"
								bind:value={viewEditorName}
							/>
						</div>

						<label class="view-editor-field layout-y-stretch gap-1">
							<span class="label-common">Map function</span>
							<div class="editor-shell view-editor-map-shell">
								<Editor
									bind:content={viewEditorMap}
									language="javascript"
									readOnly={savingView || working}
									theme={editorTheme}
								/>
							</div>
						</label>

						<div class="view-editor-fields">
							<PropertyType
								type="combo"
								name="viewEditorReduce"
								label="Reduce (optional)"
								item={[
									{ value: 'NONE', text: 'NONE' },
									{ value: '_sum', text: '_sum' },
									{ value: '_count', text: '_count' },
									{ value: '_stats', text: '_stats' },
									{ value: 'CUSTOM', text: 'CUSTOM' }
								]}
								bind:value={viewEditorReduceOption}
							/>
						</div>

						{#if viewEditorReduceOption == 'CUSTOM'}
							<label class="view-editor-field layout-y-stretch gap-1">
								<span class="label-common">Custom reduce function</span>
								<div class="editor-shell view-editor-reduce-shell">
									<Editor
										bind:content={viewEditorReduceCustom}
										language="javascript"
										readOnly={savingView || working}
										theme={editorTheme}
									/>
								</div>
							</label>
						{/if}

						<ActionBar full={false} wrap={true} class="justify-start gap-2">
							<Button
								label="Save Document and then Build Index"
								icon="mdi:content-save-edit-outline"
								class="button-primary h-9 w-fit!"
								disabled={savingView || working}
								type="submit"
							/>
							<Button
								label="Cancel"
								icon="mdi:close-circle-outline"
								class="button-secondary h-9 w-fit!"
								disabled={savingView || working}
								onclick={cancelViewEditor}
							/>
						</ActionBar>
					</fieldset>
				</form>
			</section>
		{:else}
			<section class="fullsync-main-panel">
				<div class="fullsync-toolbar">
					<InputGroup
						class="doc-open-form h-9 min-w-0"
						type="text"
						placeholder="Document ID"
						autocomplete="off"
						icon="mdi:file-document-box-outline"
						list={quickDocDatalistId}
						bind:value={quickDocId}
						onkeydown={(event) => {
							if (event.key === 'Enter') {
								event.preventDefault();
								void openDocumentFromToolbar();
							}
						}}
					>
						{#snippet actions()}
							<button
								type="button"
								class="button-ico-primary h-7 w-7 justify-center p-0! disabled:cursor-not-allowed disabled:opacity-35"
								title="Open document"
								aria-label="Open document"
								disabled={quickDocId.trim().length == 0}
								onclick={() => void openDocumentFromToolbar()}
							>
								<Ico icon="mdi:arrow-right" size={5} />
							</button>
						{/snippet}
					</InputGroup>
					<datalist id={quickDocDatalistId}>
						{#each quickDocCandidates as docId (docId)}
							<option value={docId}></option>
						{/each}
					</datalist>
					<div class="fullsync-toolbar-actions">
						<Button
							label="Options"
							icon="mdi:cog-outline"
							class={`${queryOptionsOpen ? 'button-primary' : 'button-secondary'} h-9 w-fit!`}
							onclick={() => (queryOptionsOpen = !queryOptionsOpen)}
						/>
						<Button
							label="JSON"
							icon="mdi:code-braces"
							class="button-secondary h-9 w-fit!"
							onclick={openRawJson}
						/>
						<Button
							icon="mdi:book-open-variant"
							class="button-secondary h-9 w-fit!"
							title="CouchDB documentation"
							ariaLabel="CouchDB documentation"
							onclick={openDocumentation}
						/>
					</div>
				</div>

				{#if queryOptionsOpen}
					<div class="query-options-panel query-options-floating">
						<div class="layout-y-stretch gap-4">
							<div class="layout-y-stretch gap-2">
								<div class="text-sm font-medium text-strong">Query Options</div>
								<PropertyType
									type="check"
									fit={true}
									label="Include Docs"
									checked={docsIncludeDocs}
									disabled={isViewQuery && docsReduce}
									onCheckedChange={(event) => onIncludeDocsToggle(event.checked)}
								/>
								{#if isViewQuery}
									<div class="layout-x-wrap items-center gap-2 text-sm">
										<PropertyType
											type="check"
											fit={true}
											label="Reduce"
											checked={docsReduce}
											onCheckedChange={(event) => onReduceToggle(event.checked)}
										/>
										{#if docsReduce}
											<label class="layout-x-low items-center text-strong">
												<span>Group Level</span>
												<select
													class="query-options-select h-9 input-common px-2 pr-8 text-sm"
													value={docsGroupLevel}
													onchange={(event) => (docsGroupLevel = event.currentTarget.value)}
												>
													<option value="0">None</option>
													<option value="1">1</option>
													<option value="2">2</option>
													<option value="3">3</option>
													<option value="4">4</option>
													<option value="5">5</option>
													<option value="6">6</option>
													<option value="7">7</option>
													<option value="8">8</option>
													<option value="9">9</option>
													<option value="exact">Exact</option>
												</select>
											</label>
										{/if}
									</div>
								{/if}
								<div class="layout-x-wrap items-center gap-2 text-sm">
									<PropertyType
										type="check"
										fit={true}
										label="Stable"
										checked={docsStable}
										onCheckedChange={(event) => (docsStable = event.checked)}
									/>
									<label class="layout-x-low items-center text-strong">
										<span>Update</span>
										<select
											class="query-options-select query-options-select-update h-9 input-common px-2 pr-8 text-sm"
											value={docsUpdate}
											onchange={(event) => (docsUpdate = event.currentTarget.value)}
										>
											<option value="true">true</option>
											<option value="lazy">lazy</option>
											<option value="false">false</option>
										</select>
									</label>
								</div>
							</div>

							{#if isViewQuery}
								<div class="layout-y-stretch gap-2 border-t border-surface-200-800 pt-3">
									<div class="text-sm font-medium text-strong">Keys</div>
									<PropertyType
										type="segment"
										fit={true}
										name="docsKeyMode"
										item={[
											{ value: 'by-keys', text: 'By Key(s)' },
											{ value: 'between-keys', text: 'Between Keys' }
										]}
										value={docsKeyMode}
										onValueChange={(event) => (docsKeyMode = event.value ?? 'by-keys')}
									/>
									{#if docsKeyMode == 'between-keys'}
										<div class="layout-x-wrap items-center gap-2 text-sm">
											<label class="layout-x-low items-center text-strong">
												<span>Start key</span>
												<input
													type="text"
													value={docsStartKeyValue}
													placeholder="e.g. a"
													class="query-options-key-input h-9 input-common px-2 text-sm"
													oninput={(event) => (docsStartKeyValue = event.currentTarget.value)}
												/>
											</label>
											<label class="layout-x-low items-center text-strong">
												<span>End key</span>
												<input
													type="text"
													value={docsEndKeyValue}
													placeholder="e.g. z"
													class="query-options-key-input h-9 input-common px-2 text-sm"
													oninput={(event) => (docsEndKeyValue = event.currentTarget.value)}
												/>
											</label>
										</div>
									{:else}
										<label class="layout-x-low items-center text-strong">
											<span>Key</span>
											<input
												type="text"
												value={docsKeyValue}
												placeholder="e.g. alice or [alice,bob]"
												class="query-options-key-input h-9 input-common px-2 text-sm"
												oninput={(event) => (docsKeyValue = event.currentTarget.value)}
											/>
										</label>
									{/if}
								</div>
							{/if}

							<div
								class={`layout-y-stretch gap-2 pt-3 ${isViewQuery ? 'border-t border-surface-200-800' : ''}`}
							>
								<div class="text-sm font-medium text-strong">Additional Parameters</div>
								<div class="layout-x-wrap items-center gap-2 text-sm">
									<label class="layout-x-low items-center text-strong">
										<span>Limit</span>
										<select
											class="query-options-select query-options-select-limit h-9 input-common px-2 pr-8 text-sm"
											value={docsQueryLimitValue}
											onchange={(event) => {
												docsQueryLimitValue = event.currentTarget.value;
												docsSkip = 0;
											}}
										>
											{#each FULLSYNC_PAGE_OPTIONS_WITH_500_AND_NONE as pageSizeOption (pageSizeOption)}
												<option value={pageSizeOption}>{fullSyncOptionLabel(pageSizeOption)}</option
												>
											{/each}
										</select>
									</label>
									<label class="layout-x-low items-center text-strong">
										<span>Skip</span>
										<input
											type="number"
											min="0"
											value={docsSkip}
											class="h-9 input-common w-28 px-2 text-sm"
											onchange={(event) => {
												docsSkip = Math.max(0, Number(event.currentTarget.value) || 0);
											}}
										/>
									</label>
									<PropertyType
										type="check"
										fit={true}
										label="Descending"
										checked={docsDescending}
										onCheckedChange={(event) => (docsDescending = event.checked)}
									/>
								</div>
							</div>

							<ActionBar full={false} wrap={false} class="justify-end gap-2">
								<Button
									label="Run Query"
									icon="mdi:magnify"
									class="button-primary h-9 w-fit!"
									onclick={executeOptionsQuery}
								/>
								<Button
									label="Cancel"
									class="button-secondary h-9 w-fit!"
									onclick={() => (queryOptionsOpen = false)}
								/>
							</ActionBar>
						</div>
					</div>
				{/if}

				<div class="fullsync-layout-bar">
					<div class="layout-x-wrap items-center gap-2">
						<Switch
							name="select-all-documents"
							aria-label="Select all documents"
							checked={allSelectableRowsSelected}
							disabled={selectableDocumentRows.length == 0 || working}
							onCheckedChange={(event) => setAllRowSelection(event.checked)}
						>
							<Switch.Control class="c8o-switch h-5 w-9 transition-surface">
								<Switch.Thumb />
							</Switch.Control>
							<Switch.HiddenInput />
						</Switch>
						{#if selectedDocumentCount > 0}
							<Button
								full={false}
								icon="mdi:delete-outline"
								class="button-ico-primary h-7 w-7 min-w-7 justify-center p-0!"
								title={`Delete ${selectedDocumentCount} selected document(s)`}
								ariaLabel={`Delete ${selectedDocumentCount} selected document(s)`}
								disabled={working}
								onclick={deleteSelectedDocuments}
							/>
						{/if}
					</div>
					<PropertyType
						type="segment"
						fit={true}
						name="layoutMode"
						item={docsLayoutOptions}
						value={currentLayout}
						onValueChange={(event) => setLayout(event.value ?? 'metadata')}
					/>
					<Button
						label="Create Document"
						icon="mdi:plus"
						class="create-doc-btn ml-auto button-primary h-9! w-fit!"
						onclick={createDocumentAction}
					/>
				</div>

				{#if currentLayout == 'json'}
					<FullSyncRowsPanel
						rows={documentRows}
						layoutMode="json"
						loading={loadingDocuments}
						loadingTitle="Loading documents..."
						emptyTitle="No Documents Found"
						showEmptyIcon={false}
						selectedIds={selectedDocIds}
						selectionPrefix="select-document-json"
						rowKeyPrefix="all-docs-json"
						hideSelectWithoutRev={true}
						disableSelectWithoutRev={false}
						{working}
						onSelectionChange={setRowSelection}
						onOpenDocument={openDocument}
						onCopyRow={copyRow}
						formatCellValue={formatMangoCellValue}
					/>
				{:else}
					<FullSyncRowsPanel
						rows={documentRows}
						layoutMode={currentLayout}
						tableClass=""
						loading={loadingDocuments}
						loadingTitle="Loading documents..."
						emptyTitle="This table is empty"
						tableColspanValue={tableColspan}
						visibleColumns={docsVisibleColumns}
						displayableColumns={docsDisplayableColumns}
						columnSelectionPrefix="all-docs"
						rowKeyPrefix="all-docs-row"
						selectedIds={selectedDocIds}
						hideSelectWithoutRev={true}
						disableSelectWithoutRev={false}
						idColumnMode="id-or-_id"
						columnLabel={docsColumnLabel}
						getCellValue={getDocsCellValue}
						isIdColumn={isDocsIdColumn}
						showAttachmentCountColumn={showDocsAttachmentColumn}
						getAttachmentCount={getDocsAttachmentCount}
						getConflictCount={getDocsConflictCount}
						{working}
						onSelectionChange={setRowSelection}
						onColumnSelect={setDocsColumn}
						onOpenDocument={openDocument}
						onCopyRow={copyRow}
						formatCellValue={formatMangoCellValue}
					/>
				{/if}

				<div class="fullsync-footer">
					<div class="layout-x-wrap items-center gap-2 text-sm text-muted">
						{#if currentLayout == 'table' && documentRows.length > 0}
							<span
								>Showing {docsVisibleColumns.length} of {docsDisplayableColumns.length} columns.</span
							>
							<Switch
								name="docs-show-all-columns"
								aria-label="Show all columns"
								checked={docsShowAllColumns}
								onCheckedChange={(event) => (docsShowAllColumns = event.checked)}
							>
								<Switch.Control class="c8o-switch h-5 w-9 transition-surface">
									<Switch.Thumb />
								</Switch.Control>
								<Switch.HiddenInput />
							</Switch>
							<span>Show all columns.</span>
						{/if}
						<span
							>Showing document {documentRows.length > 0 ? docsSkip + 1 : 0} - {docsSkip +
								documentRows.length}.</span
						>
					</div>
					<div class="layout-x-wrap items-center gap-2">
						<label class="docs-per-page-control layout-x-low items-center text-sm text-muted">
							<span>Documents per page:</span>
							<select
								class="docs-per-page-select h-9 input-common px-2 pr-8 text-sm"
								value={docsPageSizeValue}
								onchange={(event) => {
									docsPageSizeValue = event.currentTarget.value;
									docsSkip = 0;
									void refreshDocuments();
								}}
							>
								{#each FULLSYNC_PAGE_OPTIONS_WITH_500 as pageSizeOption (pageSizeOption)}
									<option value={pageSizeOption}>{fullSyncOptionLabel(pageSizeOption)}</option>
								{/each}
							</select>
						</label>
						<button
							type="button"
							class="button-ico-primary h-7 w-7 justify-center p-0! disabled:cursor-not-allowed disabled:opacity-35"
							title="Previous page"
							aria-label="Previous page"
							disabled={!canGoPreviousDocs}
							onclick={previousDocsPage}
						>
							<Ico icon="mdi:chevron-left" />
						</button>
						<button
							type="button"
							class="button-ico-primary h-7 w-7 justify-center p-0! disabled:cursor-not-allowed disabled:opacity-35"
							title="Next page"
							aria-label="Next page"
							disabled={!canGoNextDocs}
							onclick={nextDocsPage}
						>
							<Ico icon="mdi:chevron-right" />
						</button>
					</div>
				</div>
			</section>
		{/if}
	{:else if currentTab == 'mango'}
		<section class="mango-page-panel">
			<header class="mango-page-header">
				<div class="mango-title-row">
					<a
						href={fullSyncDbTabHref(database, 'all')}
						class="mango-back-link"
						title="All Documents"
						aria-label="All Documents"
					>
						<Ico icon="mdi:arrow-left" size={5} />
					</a>
					<a href={fullSyncDbTabHref(database, 'all')} class="mango-db-link" title={database}
						>{database}</a
					>
					<Ico icon="mdi:chevron-right" size={4} class="text-strong" />
					<span class="mango-title-text">Mango Query</span>
				</div>
				<div class="mango-header-actions">
					<Button
						label="Create Document"
						icon="mdi:plus"
						class="button-primary h-9 px-3"
						full={false}
						onclick={createDocumentAction}
					/>
					<Button
						label="JSON"
						icon="mdi:code-braces"
						class="button-secondary h-9 px-3"
						full={false}
						onclick={openMangoResultJson}
					/>
					<Button
						icon="mdi:book-open-variant"
						class="button-secondary h-9 w-9 justify-center p-0!"
						title="CouchDB Mango documentation"
						ariaLabel="CouchDB Mango documentation"
						full={false}
						onclick={openMangoDocumentation}
					/>
				</div>
			</header>

			<div class="mango-layout">
				<aside class="mango-editor-panel">
					<div class="layout-y-stretch gap-2">
						<PropertyType
							type="combo"
							name="mango-history"
							label="Query history"
							item={[
								{ value: '', text: 'Query history' },
								...mangoHistory.map((entry) => ({ value: entry.value, text: entry.label }))
							]}
							bind:value={selectedMangoHistory}
							onchange={() => onMangoHistorySelected(selectedMangoHistory)}
						/>
						<div class="layout-y-stretch gap-1">
							<span class="label-common">Mango Query</span>
							<div class="editor-shell mango-editor-shell">
								<Editor
									bind:content={mangoQueryText}
									language="json"
									readOnly={working}
									theme={editorTheme}
								/>
							</div>
						</div>
					</div>
					<div class="mango-editor-actions">
						<Button
							label="Run Query"
							icon="mdi:magnify"
							class="button-primary h-9 px-3"
							full={false}
							disabled={working}
							onclick={runMangoAction}
						/>
						<Button
							label="Explain"
							icon="mdi:cog-outline"
							class="button-secondary h-9 px-3"
							full={false}
							disabled={working}
							onclick={explainMangoAction}
						/>
						<a
							class="button-secondary inline-flex h-9 w-fit! items-center justify-center rounded-base px-3"
							href={mangoIndexesUrl}
							title="Manage Indexes"
							aria-label="Manage Indexes"
						>
							Manage Indexes
						</a>
					</div>
					{#if mangoLastRunMs > 0}
						<div class="mango-run-meta">Executed in {mangoLastRunMs} ms</div>
					{/if}
				</aside>

				<section class="mango-result-panel">
					{#if !isMangoExplainMode && hasMangoDocs}
						<header class="mango-result-toolbar">
							<div class="mango-result-toolbar-left">
								<Switch
									name="select-all-mango-documents"
									aria-label="Select all documents"
									checked={mangoAllSelectableRowsSelected}
									disabled={mangoSelectableRows.length == 0 || working}
									onCheckedChange={(event) => setAllMangoRowSelection(event.checked)}
								>
									<Switch.Control class="c8o-switch h-5 w-9 transition-surface">
										<Switch.Thumb />
									</Switch.Control>
									<Switch.HiddenInput />
								</Switch>
								{#if mangoSelectedCount > 0}
									<Button
										full={false}
										icon="mdi:delete-outline"
										class="button-ico-primary h-7 w-7 min-w-7 justify-center p-0!"
										title={`Delete ${mangoSelectedCount} selected document(s)`}
										ariaLabel={`Delete ${mangoSelectedCount} selected document(s)`}
										disabled={working}
										onclick={deleteSelectedMangoDocuments}
									/>
								{/if}
								<PropertyType
									type="segment"
									fit={true}
									name="mangoLayoutMode"
									item={[
										{ value: 'table', text: 'Table' },
										{ value: 'json', text: 'JSON' }
									]}
									value={mangoResultLayout}
									onValueChange={(event) => (mangoResultLayout = event.value ?? 'table')}
								/>
							</div>
						</header>
					{/if}

					{#if isMangoExplainMode}
						<div class="mango-result-body">
							<pre class="text-xs leading-5">{mangoExplainText}</pre>
						</div>
					{:else if mangoResultLayout == 'json'}
						<FullSyncRowsPanel
							rows={mangoPagedRows}
							layoutMode="json"
							loading={false}
							loadingTitle="Loading documents..."
							emptyTitle="No Documents Found"
							showEmptyIcon={true}
							selectedIds={mangoSelectedDocIds}
							selectionPrefix="select-mango-json-document"
							rowKeyPrefix={`mango-json-${mangoSkip}`}
							hideSelectWithoutRev={false}
							disableSelectWithoutRev={true}
							{working}
							onSelectionChange={setMangoRowSelection}
							onOpenDocument={openDocument}
							onCopyRow={copyRow}
							formatCellValue={formatMangoCellValue}
						/>
					{:else if hasMangoDocs}
						<FullSyncRowsPanel
							rows={mangoPagedRows}
							layoutMode="table"
							tableClass="mango-table"
							loading={false}
							loadingTitle="Loading documents..."
							emptyTitle="This table is empty"
							tableColspanValue={mangoTableColspan}
							visibleColumns={mangoVisibleColumns}
							displayableColumns={mangoDisplayableColumns}
							columnSelectionPrefix="mango"
							rowKeyPrefix={`mango-row-${mangoSkip}`}
							selectedIds={mangoSelectedDocIds}
							hideSelectWithoutRev={false}
							disableSelectWithoutRev={true}
							idColumnMode="_id-only"
							showAttachmentCountColumn={true}
							getAttachmentCount={getMangoAttachmentCount}
							getConflictCount={getMangoConflictCount}
							{working}
							onSelectionChange={setMangoRowSelection}
							onColumnSelect={setMangoColumn}
							onOpenDocument={openDocument}
							onCopyRow={copyRow}
							formatCellValue={formatMangoCellValue}
						/>
					{:else}
						<div class="mango-empty">
							<Ico icon="mdi:magnify" size={18} class="mango-empty-icon" />
							<div class="mango-empty-title">No Documents Found</div>
						</div>
					{/if}

					{#if !isMangoExplainMode}
						<footer class="fullsync-footer mango-footer">
							<div class="layout-x-wrap items-center gap-2 text-sm text-muted">
								{#if hasMangoDocs && mangoResultLayout == 'table'}
									<span
										>Showing {mangoVisibleColumns.length} of {mangoDisplayableColumns.length} columns.</span
									>
									<Switch
										name="mango-show-all-columns"
										aria-label="Show all columns"
										checked={mangoShowAllColumns}
										onCheckedChange={(event) => (mangoShowAllColumns = event.checked)}
									>
										<Switch.Control class="c8o-switch h-5 w-9 transition-surface">
											<Switch.Thumb />
										</Switch.Control>
										<Switch.HiddenInput />
									</Switch>
									<span>Show all columns.</span>
								{/if}
							</div>
							<div class="layout-x-wrap items-center gap-2">
								<label class="docs-per-page-control layout-x-low items-center text-sm text-muted">
									<span>Documents per page:</span>
									<select
										class="docs-per-page-select h-9 input-common px-2 pr-8 text-sm"
										value={mangoLimitValue}
										onchange={(event) => {
											mangoLimitValue = event.currentTarget.value;
											mangoSkip = 0;
											mangoSelectedDocIds = {};
										}}
									>
										{#each FULLSYNC_PAGE_OPTIONS_WITH_500_AND_NONE as pageSizeOption (pageSizeOption)}
											<option value={pageSizeOption}>{fullSyncOptionLabel(pageSizeOption)}</option>
										{/each}
									</select>
								</label>
								<button
									type="button"
									class="button-ico-primary h-7 w-7 justify-center p-0! disabled:cursor-not-allowed disabled:opacity-35"
									title="Previous page"
									aria-label="Previous page"
									disabled={!mangoCanGoPrevious}
									onclick={previousMangoPage}
								>
									<Ico icon="mdi:chevron-left" />
								</button>
								<button
									type="button"
									class="button-ico-primary h-7 w-7 justify-center p-0! disabled:cursor-not-allowed disabled:opacity-35"
									title="Next page"
									aria-label="Next page"
									disabled={!mangoCanGoNext}
									onclick={nextMangoPage}
								>
									<Ico icon="mdi:chevron-right" />
								</button>
								<div class="text-sm text-muted">
									Showing document {mangoPagedRows.length > 0 ? mangoSkip + 1 : 0} - {mangoSkip +
										mangoPagedRows.length}.
								</div>
							</div>
						</footer>
					{/if}
				</section>
			</div>
		</section>
	{:else}
		<section class="mango-page-panel">
			<header class="mango-page-header">
				<div class="mango-title-row">
					<a href={fullSyncDbAllDocsHref(database)} class="mango-back-link" title="All Documents">
						<Ico icon="mdi:arrow-left" size={5} />
					</a>
					<a href={fullSyncDbAllDocsHref(database)} class="mango-db-link" title={database}
						>{database}</a
					>
					<Ico icon="mdi:chevron-right" size={4} class="text-strong" />
					<span class="mango-title-text">Mango</span>
				</div>
				<div class="mango-header-actions">
					<Button
						label="Create Document"
						icon="mdi:plus"
						class="button-primary h-9 px-3"
						full={false}
						onclick={createDocumentAction}
					/>
					<Button
						label="JSON"
						icon="mdi:code-braces"
						class="button-secondary h-9 px-3"
						full={false}
						onclick={openMangoIndexJson}
					/>
					<Button
						icon="mdi:book-open-variant"
						class="button-secondary h-9 w-9 justify-center p-0!"
						title="CouchDB Mango documentation"
						ariaLabel="CouchDB Mango documentation"
						full={false}
						onclick={openMangoDocumentation}
					/>
				</div>
			</header>

			<div class="mango-layout">
				<aside class="mango-editor-panel">
					<div class="layout-y-stretch gap-2">
						<PropertyType
							type="combo"
							name="mango-index-example"
							label="Examples"
							item={[
								{ value: 'example', text: 'Examples' },
								{ value: 'text', text: 'message index' }
							]}
							bind:value={mangoIndexExample}
							onchange={() => onMangoIndexExampleChange(mangoIndexExample)}
						/>
						<div class="layout-y-stretch gap-1">
							<span class="label-common">Index</span>
							<div class="editor-shell mango-editor-shell">
								<Editor
									bind:content={mangoIndexText}
									language="json"
									readOnly={working}
									theme={editorTheme}
								/>
							</div>
						</div>
					</div>
					<div class="mango-editor-actions">
						<Button
							label="Create Index"
							class="button-primary h-9 px-3"
							full={false}
							disabled={working}
							onclick={createMangoIndexAction}
						/>
						<a
							class="button-secondary flex h-9 items-center justify-center rounded-base px-3"
							href={fullSyncDbMangoHref(database)}
						>
							Edit Query
						</a>
					</div>
				</aside>

				<section class="mango-result-panel">
					<header class="mango-result-toolbar">
						<div class="mango-result-toolbar-left">
							<Switch
								name="select-all-mango-indexes"
								aria-label="Select all indexes"
								checked={mangoIndexAllSelectableRowsSelected}
								disabled={mangoIndexSelectableRows.length == 0 || working}
								onCheckedChange={(event) => setAllMangoIndexRowSelection(event.checked)}
							>
								<Switch.Control class="c8o-switch h-5 w-9 transition-surface">
									<Switch.Thumb />
								</Switch.Control>
								<Switch.HiddenInput />
							</Switch>
							{#if mangoIndexSelectedCount > 0}
								<Button
									full={false}
									icon="mdi:delete-outline"
									class="button-ico-primary h-7 w-7 min-w-7 justify-center p-0!"
									title={`Delete ${mangoIndexSelectedCount} selected index document(s)`}
									ariaLabel={`Delete ${mangoIndexSelectedCount} selected index document(s)`}
									disabled={working}
									onclick={deleteSelectedMangoIndexes}
								/>
							{/if}
						</div>
					</header>

					<FullSyncRowsPanel
						rows={mangoIndexPagedRows}
						layoutMode="json"
						loading={mangoIndexesLoading}
						loadingTitle="Loading indexes..."
						emptyTitle="No indexes found"
						showEmptyIcon={true}
						selectedIds={mangoIndexesSelection}
						selectionPrefix="select-mango-index"
						rowKeyPrefix={`mango-index-${mangoIndexSkip}`}
						hideSelectWithoutRev={false}
						disableSelectWithoutRev={false}
						{working}
						onSelectionChange={setMangoIndexRowSelection}
						showJsonOpenButton={false}
						jsonHeaderText={(row) => `"${row.type}: ${row.displayName}"`}
						jsonHeaderTitle={(row) => `${row.type}: ${row.displayName}`}
						getSelectionKey={(row) => String(row?.key ?? '')}
						isRowSelectable={(row) => Boolean(row?.isBulkDeletable)}
						onOpenDocument={openDocument}
						onCopyRow={copyRow}
						formatCellValue={formatMangoCellValue}
					/>

					<footer class="fullsync-footer mango-footer">
						<div class="text-sm text-muted">
							Showing document {mangoIndexPagedRows.length > 0 ? mangoIndexSkip + 1 : 0} - {mangoIndexSkip +
								mangoIndexPagedRows.length}.
						</div>
						<div class="layout-x-wrap items-center gap-2">
							<label class="docs-per-page-control layout-x-low items-center text-sm text-muted">
								<span>Documents per page:</span>
								<select
									class="docs-per-page-select h-9 input-common px-2 pr-8 text-sm"
									value={mangoIndexLimitValue}
									onchange={(event) => {
										mangoIndexLimitValue = event.currentTarget.value;
										mangoIndexSkip = 0;
										mangoIndexesSelection = {};
									}}
								>
									{#each FULLSYNC_PAGE_OPTIONS_BASE as pageSizeOption (pageSizeOption)}
										<option value={pageSizeOption}>{fullSyncOptionLabel(pageSizeOption)}</option>
									{/each}
								</select>
							</label>
							<button
								type="button"
								class="button-ico-primary h-7 w-7 justify-center p-0! disabled:cursor-not-allowed disabled:opacity-35"
								title="Previous page"
								aria-label="Previous page"
								disabled={!mangoIndexCanGoPrevious}
								onclick={previousMangoIndexPage}
							>
								<Ico icon="mdi:chevron-left" />
							</button>
							<button
								type="button"
								class="button-ico-primary h-7 w-7 justify-center p-0! disabled:cursor-not-allowed disabled:opacity-35"
								title="Next page"
								aria-label="Next page"
								disabled={!mangoIndexCanGoNext}
								onclick={nextMangoIndexPage}
							>
								<Ico icon="mdi:chevron-right" />
							</button>
						</div>
					</footer>
				</section>
			</div>
		</section>
	{/if}
</div>

<ModalDynamic bind:this={cloneViewModal}>
	{#snippet children({ close })}
		<Card title="Clone View" style="width: min(42rem, calc(100vw - (var(--spacing) * 4)));">
			<div class="layout-y-stretch gap-3">
				<div class="text-sm text-strong">
					Clone view <b>{cloneSourceViewName || '-'}</b> from
					<b>{cloneSourceDesignDocId || '-'}</b>.
				</div>

				<PropertyType
					type="combo"
					name="cloneTargetDesignDoc"
					label="Target design document"
					item={[{ value: 'new-doc', text: 'Create new design document' }, ...designDocSelectItems]}
					bind:value={cloneTargetDesignDocId}
				/>
				{#if cloneTargetDesignDocId == 'new-doc'}
					<PropertyType
						type="text"
						name="cloneNewDesignDocName"
						label="New design document name"
						placeholder="new_design_doc"
						bind:value={cloneNewDesignDocName}
					/>
				{/if}
				<PropertyType
					type="text"
					name="cloneViewName"
					label="Target view name"
					placeholder="view_copy"
					bind:value={cloneViewName}
				/>

				{#if cloneViewError}
					<div
						class="rounded-base border border-error-300-700 bg-error-100-900 px-3 py-2 text-sm text-error-900-100"
					>
						{cloneViewError}
					</div>
				{/if}

				<ActionBar full={false} wrap={true} class="justify-end gap-2">
					<Button
						label="Cancel"
						icon="mdi:close-circle-outline"
						class="button-secondary w-fit!"
						disabled={working}
						onclick={() => {
							cloneViewError = '';
							close(false);
						}}
					/>
					<Button
						label="Clone View"
						icon="mdi:cached"
						class="button-primary w-fit!"
						disabled={working || cloneViewName.trim().length == 0}
						onclick={cloneViewAction}
					/>
				</ActionBar>
			</div>
		</Card>
	{/snippet}
</ModalDynamic>

<style lang="postcss">
	@reference "../../../../app.css";

	pre {
		white-space: pre-wrap;
		word-break: break-word;
		font-family:
			ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New',
			monospace;
	}

	.db-sidebar-head {
		display: grid;
		grid-template-columns: auto minmax(0, 1fr);
		align-items: center;
		gap: calc(var(--spacing) * 1.5);
		padding: calc(var(--spacing) * 1.5);
		border-bottom: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
	}

	.sidebar-head-back {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 1.9rem;
		height: 1.9rem;
		color: var(--color-primary-500);
		transition: color 140ms ease;
	}

	.sidebar-head-back:hover {
		color: var(--color-primary-600);
	}

	.db-sidebar-name {
		font-size: 0.94rem;
		font-weight: 600;
		color: var(--convertigo-text-strong);
		line-height: 1.2;
		padding-inline: calc(var(--spacing) * 0.5);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.sidebar-row-with-action {
		display: grid;
		grid-template-columns: minmax(0, 1fr) auto;
		align-items: center;
	}

	.sidebar-action-btn {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 2rem;
		min-width: 2rem;
		height: 100%;
		padding-inline: calc(var(--spacing) * 0.5);
		color: var(--convertigo-text-strong);
		transition: color 140ms ease;
	}

	.sidebar-action-btn:hover {
		color: var(--color-primary-500);
	}

	.design-doc-group {
		border-top: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
	}

	.design-doc-head {
		display: grid;
		grid-template-columns: auto minmax(0, 1fr) auto;
		align-items: stretch;
	}

	.design-doc-toggle {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		padding: 0 calc(var(--spacing) * 1.5);
		color: var(--convertigo-text-strong);
	}

	.design-doc-link {
		position: relative;
		width: 100%;
		border: 0;
		background: transparent;
		cursor: pointer;
		display: flex;
		align-items: center;
		gap: calc(var(--spacing) * 2);
		padding: calc(var(--spacing) * 2.5) calc(var(--spacing) * 2) calc(var(--spacing) * 2.5)
			calc(var(--spacing) * 1.5);
		min-width: 0;
	}

	.design-doc-action {
		width: 2.1rem;
	}

	.design-doc-sub-title {
		padding: calc(var(--spacing) * 1) calc(var(--spacing) * 2.5) calc(var(--spacing) * 1)
			calc(var(--spacing) * 9);
		font-size: 12px;
		color: var(--convertigo-text-muted);
		text-transform: uppercase;
		letter-spacing: 0.02em;
	}

	.design-doc-view-row {
		display: grid;
		grid-template-columns: minmax(0, 1fr) auto;
		align-items: center;
	}

	.design-doc-sub-link {
		position: relative;
		display: flex;
		align-items: center;
		padding: calc(var(--spacing) * 1.25) calc(var(--spacing) * 2) calc(var(--spacing) * 1.25)
			calc(var(--spacing) * 9);
		font-size: 13px;
		min-width: 0;
		color: var(--convertigo-text-strong);
	}

	.design-doc-view-actions {
		display: inline-flex;
		align-items: center;
		gap: calc(var(--spacing) * 0.25);
		padding-right: calc(var(--spacing) * 1);
	}

	.design-doc-sub-link:hover,
	.design-doc-link:hover,
	.design-doc-view-row:hover {
		background: light-dark(
			color-mix(in srgb, var(--color-primary-300) 26%, transparent),
			color-mix(in srgb, var(--color-primary-600) 24%, transparent)
		);
	}

	.fullsync-main-panel {
		position: relative;
		display: grid;
		gap: calc(var(--spacing) * 1.25);
		padding: calc(var(--spacing) * 1.5);
		border: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
		border-radius: var(--radius-container);
		background: light-dark(var(--color-surface-100), var(--color-surface-900));
	}

	.view-editor-panel {
		padding: calc(var(--spacing) * 2);
		min-width: 0;
		max-width: 100%;
		overflow-x: hidden;
	}

	.view-editor-form,
	.view-editor-fieldset,
	.view-editor-field,
	.view-editor-fields {
		min-width: 0;
		width: 100%;
		max-width: 100%;
	}

	.view-editor-fieldset {
		border: 0;
		margin: 0;
		padding: 0;
		min-inline-size: 0;
	}

	.view-editor-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: calc(var(--spacing) * 1);
		padding-bottom: calc(var(--spacing) * 1);
		border-bottom: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
		min-width: 0;
	}

	.view-editor-fields {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
		gap: calc(var(--spacing) * 1);
	}

	.editor-shell {
		height: 20rem;
		min-height: 18rem;
		min-width: 0;
		max-width: 100%;
		border: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
		border-radius: var(--radius-base);
		overflow: hidden;
	}

	.view-editor-panel .editor-shell :global(.monaco-editor),
	.view-editor-panel .editor-shell :global(.overflow-guard),
	.view-editor-panel .editor-shell :global(.monaco-scrollable-element) {
		max-width: 100%;
	}

	.view-editor-map-shell {
		height: clamp(18rem, 44dvh, 34rem);
	}

	.view-editor-reduce-shell {
		height: clamp(16rem, 34dvh, 28rem);
	}

	.fullsync-toolbar {
		display: grid;
		grid-template-columns: minmax(16rem, 1fr) auto;
		gap: calc(var(--spacing) * 1.25);
		align-items: center;
	}

	.fullsync-toolbar-actions {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		justify-content: flex-end;
		gap: calc(var(--spacing) * 1);
	}

	.fullsync-layout-bar {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: calc(var(--spacing) * 1.25);
		padding-top: calc(var(--spacing) * 0.5);
		padding-bottom: calc(var(--spacing) * 0.5);
		border-top: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
		border-bottom: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
	}

	.fullsync-footer {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		justify-content: space-between;
		gap: calc(var(--spacing) * 1.5);
		padding-top: calc(var(--spacing) * 0.75);
	}

	.docs-per-page-control {
		flex-wrap: nowrap;
		white-space: nowrap;
	}

	.docs-per-page-select {
		min-width: 4.5rem;
	}

	.doc-open-form {
		min-width: 0;
		width: 100%;
	}

	.query-options-panel {
		border: 1px solid var(--color-surface-500);
		background: light-dark(var(--color-surface-100), var(--color-surface-900));
		border-radius: var(--radius-container);
		padding: calc(var(--spacing) * 2);
	}

	.query-options-floating {
		position: absolute;
		top: calc(var(--spacing) * 6.5);
		right: calc(var(--spacing) * 1.5);
		width: min(30rem, calc(100% - (var(--spacing) * 3)));
		z-index: 20;
		box-shadow: var(--shadow-follow);
	}

	.query-options-select {
		min-width: 5.75rem;
	}

	.query-options-select-update {
		min-width: 6.25rem;
	}

	.query-options-select-limit {
		min-width: 5.5rem;
	}

	.query-options-key-input {
		min-width: 10.5rem;
	}

	.mango-page-panel {
		display: flex;
		flex-direction: column;
		border: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
		border-radius: var(--radius-container);
		background: light-dark(var(--color-surface-100), var(--color-surface-900));
		min-height: calc(100dvh - 13rem);
		overflow: hidden;
	}

	.mango-page-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: calc(var(--spacing) * 1);
		padding: calc(var(--spacing) * 1.25) calc(var(--spacing) * 1.5);
		border-bottom: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
	}

	.mango-title-row {
		display: flex;
		align-items: center;
		gap: calc(var(--spacing) * 0.75);
		min-width: 0;
	}

	.mango-back-link {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		color: var(--color-primary-500);
		transition: color 140ms ease;
	}

	.mango-back-link:hover {
		color: var(--color-primary-600);
	}

	.mango-db-link,
	.mango-title-text {
		font-size: 1.2rem;
		line-height: 1.2;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.mango-db-link {
		max-width: min(20rem, 30vw);
		font-weight: 600;
		color: var(--color-primary-500);
	}

	.mango-title-text {
		font-weight: 500;
		color: var(--convertigo-text-strong);
	}

	.mango-layout {
		display: grid;
		grid-template-columns: minmax(19rem, 25rem) minmax(0, 1fr);
		flex: 1 1 auto;
		min-height: 0;
	}

	.mango-header-actions {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: calc(var(--spacing) * 0.5);
	}

	.mango-editor-panel,
	.mango-result-panel {
		display: grid;
		min-height: 0;
		align-content: start;
		gap: calc(var(--spacing) * 1);
		padding: calc(var(--spacing) * 1.25);
	}

	.mango-editor-panel {
		border-right: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
		grid-template-rows: auto auto auto;
	}

	.mango-editor-shell {
		height: clamp(16rem, 36dvh, 22rem);
		min-height: 16rem;
	}

	.mango-editor-actions {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: calc(var(--spacing) * 0.5);
		padding-top: calc(var(--spacing) * 0.75);
		border-top: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
	}

	.mango-run-meta {
		font-size: 0.85rem;
		color: var(--convertigo-text-muted);
	}

	.mango-result-panel {
		grid-template-rows: auto minmax(0, 1fr) auto;
	}

	.mango-result-toolbar {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		justify-content: space-between;
		gap: calc(var(--spacing) * 1);
		padding-bottom: calc(var(--spacing) * 0.75);
		border-bottom: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
	}

	.mango-result-toolbar-left {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: calc(var(--spacing) * 0.75);
		min-width: 0;
	}

	.mango-result-body {
		min-height: 16rem;
		border: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
		border-radius: var(--radius-base);
		overflow: auto;
		padding: calc(var(--spacing) * 1);
	}

	.mango-result-body pre {
		margin: 0;
	}

	.mango-empty {
		display: grid;
		place-items: center;
		gap: calc(var(--spacing) * 0.5);
		min-height: 22rem;
		color: var(--convertigo-text-muted);
	}

	.mango-empty-icon {
		opacity: 0.45;
	}

	.mango-empty-title {
		font-size: 1.6rem;
		font-weight: 500;
	}

	.mango-footer {
		padding-top: calc(var(--spacing) * 1);
		border-top: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
		align-items: center;
		gap: calc(var(--spacing) * 0.75);
	}

	@media (max-width: 1100px) {
		.mango-layout {
			grid-template-columns: 1fr;
		}

		.mango-editor-panel {
			border-right: 0;
			border-bottom: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
		}
	}

	@media (max-width: 960px) {
		.fullsync-toolbar {
			grid-template-columns: 1fr;
		}

		.fullsync-toolbar-actions {
			justify-content: flex-start;
		}

		.fullsync-layout-bar {
			align-items: stretch;
		}

		.create-doc-btn {
			margin-left: 0 !important;
		}

		.doc-open-form {
			min-width: 0;
			max-width: 100%;
			width: 100%;
		}

		.db-sidebar-name {
			font-size: 0.9rem;
		}

		.view-editor-header {
			align-items: flex-start;
			flex-direction: column;
		}

		.mango-page-header {
			flex-direction: column;
			align-items: flex-start;
		}

		.mango-db-link,
		.mango-title-text {
			font-size: 1.05rem;
		}
	}
</style>
