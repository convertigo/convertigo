<script>
	import { Switch } from '@skeletonlabs/skeleton-svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import ActionBar from '$lib/admin/components/ActionBar.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import InputGroup from '$lib/common/components/InputGroup.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { toaster } from '$lib/utils/service';
	import { getContext, onDestroy, onMount } from 'svelte';
	import RightPart from '../RightPart.svelte';
	import {
		fullSyncBaseUrl,
		listDesignDocuments,
		listDocuments,
		removeDocuments,
		runMangoQuery,
		runViewQuery
	} from './fullsync-api';
	import {
		fullSyncDbTabHref,
		fullSyncDbViewHref,
		fullSyncDocHref,
		fullSyncHomeHref
	} from './fullsync-route';

	/** @type {{database: string}} */
	let { database = '' } = $props();

	let modalYesNo;
	try {
		modalYesNo = getContext('modalYesNo');
	} catch {
		modalYesNo = undefined;
	}

	let loadingDocuments = $state(false);
	let working = $state(false);
	let loadingDesignDocs = $state(false);
	let lastError = $state('');

	let documents = $state([]);
	let docsTotalRows = $state(0);
	let mangoQueryText = $state('{\n  "selector": {},\n  "limit": 25\n}');
	let mangoResultText = $state('');
	let docsLimitValue = $state('20');
	let docsSkip = $state(0);
	let docsDescending = $state(true);
	let docsIncludeDocs = $state(false);
	let docsStable = $state(false);
	let docsUpdate = $state('true');
	let docsRequestCounter = 0;
	let loadedDatabase = $state('');
	let loadedQueryScope = $state('');

	let queryOptionsOpen = $state(false);
	let quickDocId = $state('');
	let currentLayout = $state('metadata');
	let selectedDocIds = $state(/** @type {Record<string, boolean>} */ ({}));

	let designDocs = $state([]);
	let designDocExpanded = $state(/** @type {Record<string, boolean>} */ ({}));

	let currentTab = $derived.by(() =>
		page.url.searchParams.get('tab') == 'mango' ? 'mango' : 'all'
	);
	let selectedDesignDocId = $derived(page.url.searchParams.get('ddoc') ?? '');
	let selectedViewName = $derived(page.url.searchParams.get('view') ?? '');
	let isViewQuery = $derived(
		currentTab == 'all' && selectedDesignDocId.length > 0 && selectedViewName.length > 0
	);
	let hasLimit = $derived(docsLimitValue != 'none');
	let docsLimitNumber = $derived(Math.max(1, Number(docsLimitValue) || 100));
	let canGoPreviousDocs = $derived(hasLimit && docsSkip > 0);
	let canGoNextDocs = $derived.by(() => {
		if (!hasLimit) return false;
		const total = Number(docsTotalRows);
		if (Number.isFinite(total) && total > 0) {
			return docsSkip + docsLimitNumber < total;
		}
		return documents.length >= docsLimitNumber;
	});
	let documentRows = $derived(
		documents.map((row) => {
			const value = row?.value ?? {};
			const key = row?.key ?? row?.id ?? '';
			const doc = row?.doc ?? null;
			return {
				id: row?.id ?? '',
				key: typeof key == 'string' ? key : JSON.stringify(key),
				rev: value?.rev ?? doc?._rev ?? '',
				type: doc?.type ?? (row?.id?.startsWith('_design/') ? 'design' : ''),
				valueText: pretty(value),
				valuePreview: JSON.stringify(value ?? {}),
				jsonText: pretty(doc ?? row),
				hasDocument: Boolean(row?.id),
				raw: doc ?? row
			};
		})
	);
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
	let tableColspan = $derived(currentLayout == 'metadata' ? 5 : currentLayout == 'table' ? 4 : 3);
	let quickDocDatalistId = $derived(`fullsync-doc-ids-${database.replace(/[^a-zA-Z0-9_-]/g, '-')}`);
	let rawJsonUrl = $derived.by(() => {
		if (!database || currentTab != 'all') return '#';
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

	function asErrorMessage(error) {
		if (typeof error == 'string') return error;
		if (error?.message) return error.message;
		return 'Unknown FullSync error';
	}

	function showError(error) {
		const message = asErrorMessage(error);
		lastError = message;
		toaster.error({
			description: message,
			duration: 4200
		});
	}

	function showSuccess(message) {
		toaster.success({
			description: message,
			duration: 2400
		});
	}

	async function askConfirmation(event, title, message) {
		if (modalYesNo?.open) {
			return await modalYesNo.open({ event, title, message });
		}
		return window.confirm(`${title}\n\n${message}`);
	}

	function parseJson(content, label) {
		try {
			return JSON.parse(content);
		} catch (error) {
			const message = error instanceof Error ? error.message : String(error);
			showError(`${label}: invalid JSON (${message})`);
			return null;
		}
	}

	function pretty(value) {
		return JSON.stringify(value ?? {}, null, 2);
	}

	function encodeDesignDocPath(designDocId) {
		const raw = String(designDocId ?? '').replace(/^_design\//, '');
		return `_design/${encodeURIComponent(raw)}`;
	}

	function buildDocsQuery() {
		return {
			include_docs: docsIncludeDocs,
			descending: docsDescending,
			stable: docsStable || undefined,
			update: docsUpdate == 'true' ? undefined : docsUpdate,
			limit: hasLimit ? docsLimitNumber : undefined,
			skip: hasLimit ? docsSkip : undefined
		};
	}

	function setLayout(layout) {
		if (layout == currentLayout) return;
		currentLayout = layout;
		docsIncludeDocs = layout != 'metadata';
		docsSkip = 0;
		if (currentTab == 'all') {
			void refreshDocuments();
		}
	}

	function setRowSelection(docId, checked) {
		if (!docId) return;
		const next = { ...selectedDocIds };
		if (checked) {
			next[docId] = true;
		} else {
			delete next[docId];
		}
		selectedDocIds = next;
	}

	function setAllRowSelection(checked) {
		if (!checked) {
			selectedDocIds = {};
			return;
		}
		const next = /** @type {Record<string, boolean>} */ ({});
		for (const row of selectableDocumentRows) {
			next[row.id] = true;
		}
		selectedDocIds = next;
	}

	function toggleDesignDoc(designDocId) {
		const current = designDocExpanded[designDocId] ?? false;
		designDocExpanded = { ...designDocExpanded, [designDocId]: !current };
	}

	function isDesignDocExpanded(designDocId) {
		return designDocExpanded[designDocId] ?? selectedDesignDocId == designDocId;
	}

	async function openDocument(docId) {
		if (!docId) return;
		await goto(fullSyncDocHref(database, docId));
	}

	async function openDocumentFromToolbar(event) {
		event?.preventDefault?.();
		const docId = quickDocId.trim();
		if (!docId) return;
		await openDocument(docId);
	}

	function openRawJson() {
		if (!rawJsonUrl || rawJsonUrl == '#') return;
		window.open(rawJsonUrl, '_blank', 'noopener');
	}

	function openDocumentation() {
		window.open(
			'https://docs.couchdb.org/en/stable/api/document/common.html',
			'_blank',
			'noopener'
		);
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
		if (value && currentLayout == 'metadata') {
			currentLayout = 'table';
		}
		if (!value && currentLayout != 'metadata') {
			currentLayout = 'metadata';
		}
	}

	async function refreshDocuments() {
		if (!database || currentTab != 'all') return;
		const requestId = ++docsRequestCounter;
		loadingDocuments = true;
		try {
			const query = {
				limit: hasLimit ? docsLimitNumber : undefined,
				skip: hasLimit ? docsSkip : 0,
				includeDocs: docsIncludeDocs,
				descending: docsDescending,
				stable: docsStable,
				update: docsUpdate
			};
			const response = isViewQuery
				? await runViewQuery(database, selectedDesignDocId, selectedViewName, query)
				: await listDocuments(database, query);
			if (requestId != docsRequestCounter) return;
			documents = Array.isArray(response?.rows) ? response.rows : [];
			docsTotalRows = Number(response?.total_rows ?? documents.length) || 0;
			selectedDocIds = {};
		} catch (error) {
			documents = [];
			docsTotalRows = 0;
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

		const ok = await askConfirmation(
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
			const rows = await listDesignDocuments(database, {
				limit: 1000,
				includeDocs: true
			});
			designDocs = rows
				.map((row) => {
					const id = row?.id ?? row?.doc?._id ?? '';
					const doc = row?.doc ?? {};
					return {
						id,
						name: id.replace('_design/', ''),
						views: Object.keys(doc?.views ?? {}).sort((a, b) => a.localeCompare(b))
					};
				})
				.filter((designDoc) => Boolean(designDoc.id))
				.sort((a, b) => a.name.localeCompare(b.name));
		} catch {
			designDocs = [];
		} finally {
			loadingDesignDocs = false;
		}
	}

	async function refreshAll() {
		const jobs = [refreshDesignDocs()];
		if (currentTab == 'all') {
			jobs.push(refreshDocuments());
		}
		await Promise.all(jobs);
	}

	async function previousDocsPage() {
		if (!hasLimit) return;
		docsSkip = Math.max(0, docsSkip - docsLimitNumber);
		await refreshDocuments();
	}

	async function nextDocsPage() {
		if (!hasLimit) return;
		docsSkip += docsLimitNumber;
		await refreshDocuments();
	}

	async function executeOptionsQuery() {
		if (!hasLimit) {
			docsSkip = 0;
		}
		await refreshDocuments();
		queryOptionsOpen = false;
	}

	async function createDocumentAction() {
		if (!database) return;
		await goto(fullSyncDocHref(database, '_new'));
	}

	async function runMangoAction() {
		if (!database) return;
		const payload = parseJson(mangoQueryText, 'Mango editor');
		if (!payload) return;

		working = true;
		lastError = '';
		try {
			const result = await runMangoQuery(database, payload);
			mangoResultText = pretty(result);
			showSuccess('Mango query executed');
		} catch (error) {
			showError(error);
		} finally {
			working = false;
		}
	}

	$effect(() => {
		if (!database) return;
		if (database == loadedDatabase) return;
		loadedDatabase = database;
		loadedQueryScope = '';
		docsSkip = 0;
		documents = [];
		docsTotalRows = 0;
		designDocs = [];
		designDocExpanded = {};
		mangoResultText = '';
		lastError = '';
		queryOptionsOpen = false;
		quickDocId = '';
		selectedDocIds = {};
		currentLayout = 'metadata';
		docsIncludeDocs = false;
		void refreshAll();
	});

	$effect(() => {
		if (!database || currentTab != 'all') return;
		const queryScope = `${database}|${selectedDesignDocId}|${selectedViewName}`;
		if (queryScope == loadedQueryScope) return;
		loadedQueryScope = queryScope;
		docsSkip = 0;
		quickDocId = '';
		selectedDocIds = {};
		if (selectedDesignDocId) {
			designDocExpanded = {
				...designDocExpanded,
				[selectedDesignDocId]: true
			};
		}
		void refreshDocuments();
	});

	const rightPartOwner = Symbol('fullsync-database');
	onMount(() => {
		RightPart.claim(rightPartOwner, rightPart);
	});
	onDestroy(() => {
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
				<Ico icon="mdi:arrow-left-bold-outline" size={5} />
			</a>
			<div class="db-sidebar-name">{database}</div>
		</div>

		<div class="sidebar-row-with-action">
			<a
				href={fullSyncDbTabHref(database, 'all')}
				aria-current={currentTab == 'all' && !isViewQuery ? 'page' : undefined}
				class="rail-link"
			>
				{#if currentTab == 'all' && !isViewQuery}
					<span class="absolute inset-0 rounded-sm bg-primary-100/70 dark:bg-primary-500/20"></span>
				{/if}
				<Ico
					icon="mdi:file-document-box-outline"
					size={5}
					class={`z-10 ${currentTab == 'all' && !isViewQuery ? 'rail-active' : 'text-strong'}`}
				/>
				<span
					class={`z-10 ${
						currentTab == 'all' && !isViewQuery
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
				title="Create Document"
				aria-label="Create Document"
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
				title="Create Design Document"
				aria-label="Create Design Document"
				onclick={createDocumentAction}
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
						<a href={fullSyncDocHref(database, designDoc.id)} class="design-doc-link">
							{#if selectedDesignDoc}
								<span class="absolute inset-0 rounded-sm bg-primary-100/70 dark:bg-primary-500/20"
								></span>
							{/if}
							<Ico
								icon="mdi:file-document-box-outline"
								size={4}
								class={`z-10 ${selectedDesignDoc ? 'rail-active' : 'text-strong'}`}
							/>
							<span
								class={`z-10 truncate text-[13px] ${
									selectedDesignDoc ? 'font-medium rail-active' : 'text-strong'
								}`}
							>
								{designDoc.name}
							</span>
						</a>
						<button
							type="button"
							class="sidebar-action-btn design-doc-action"
							title="Open design document"
							aria-label="Open design document"
							onclick={() => openDocument(designDoc.id)}
						>
							<Ico icon="mdi:plus" size={4} />
						</button>
					</div>
					{#if expanded}
						<a href={fullSyncDocHref(database, designDoc.id)} class="design-doc-sub-link">
							Metadata
						</a>
						{#if designDoc.views.length > 0}
							<div class="design-doc-sub-title">Views</div>
							{#each designDoc.views as viewName (viewName)}
								{@const isCurrentView =
									isViewQuery &&
									selectedDesignDocId == designDoc.id &&
									selectedViewName == viewName}
								<a
									href={fullSyncDbViewHref(database, designDoc.id, viewName)}
									aria-current={isCurrentView ? 'page' : undefined}
									class="design-doc-sub-link"
								>
									{#if isCurrentView}
										<span
											class="absolute inset-0 rounded-sm bg-primary-100/70 dark:bg-primary-500/20"
										></span>
									{/if}
									<span
										class={`z-10 truncate ${isCurrentView ? 'font-medium rail-active' : 'text-strong'}`}
									>
										{viewName}
									</span>
								</a>
							{/each}
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
		<section class="fullsync-main-panel">
			<div class="fullsync-toolbar">
				<InputGroup
					class="doc-open-form h-9 min-w-0"
					type="text"
					placeholder="Document ID"
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
					{#each documents as row (row.id ?? row.key)}
						{#if row?.id}
							<option value={row.id}></option>
						{/if}
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
								onCheckedChange={(event) => onIncludeDocsToggle(event.checked)}
							/>
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
										class="h-9 input-common px-2 text-sm"
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

						<div class="layout-y-stretch gap-2 border-t border-surface-200-800 pt-3">
							<div class="text-sm font-medium text-strong">Additional Parameters</div>
							<div class="layout-x-wrap items-center gap-2 text-sm">
								<label class="layout-x-low items-center text-strong">
									<span>Limit</span>
									<select
										class="h-9 input-common px-2 text-sm"
										value={docsLimitValue}
										onchange={(event) => {
											docsLimitValue = event.currentTarget.value;
											docsSkip = 0;
										}}
									>
										<option value="5">5</option>
										<option value="10">10</option>
										<option value="20">20</option>
										<option value="30">30</option>
										<option value="50">50</option>
										<option value="100">100</option>
										<option value="500">500</option>
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
				<div class="layout-x-low items-center gap-2">
					<Switch
						name="select-all-documents"
						aria-label="Select all documents"
						checked={allSelectableRowsSelected}
						disabled={selectableDocumentRows.length == 0 || working}
						onCheckedChange={(event) => setAllRowSelection(event.checked)}
					>
						<Switch.Control class="c8o-switch transition-surface">
							<Switch.Thumb />
						</Switch.Control>
						<Switch.HiddenInput />
					</Switch>
					{#if selectedDocumentCount > 0}
						<Button
							full={false}
							icon="mdi:delete-outline"
							class="button-ico-primary h-9 w-9 min-w-9 justify-center p-0!"
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
					item={[
						{ value: 'table', text: 'Table' },
						{ value: 'metadata', text: 'Metadata' },
						{ value: 'json', text: 'JSON' }
					]}
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

			<div class="docs-table-wrap">
				<table class={`docs-table ${currentLayout == 'metadata' ? 'docs-table-metadata' : ''}`}>
					<thead>
						{#if currentLayout == 'metadata'}
							<tr>
								<th class="col-select"></th>
								<th class="col-copy"></th>
								<th class="metadata-col-id">id</th>
								<th class="metadata-col-key">key</th>
								<th class="metadata-col-value">value</th>
							</tr>
						{:else if currentLayout == 'table'}
							<tr>
								<th class="col-select"></th>
								<th>id</th>
								<th>type</th>
								<th>rev</th>
							</tr>
						{:else}
							<tr>
								<th class="col-select"></th>
								<th class="col-copy"></th>
								<th>json</th>
							</tr>
						{/if}
					</thead>
					<tbody>
						{#if loadingDocuments && documentRows.length == 0}
							<tr>
								<td colspan={tableColspan} class="empty-row"> Loading documents... </td>
							</tr>
						{:else if documentRows.length == 0}
							<tr>
								<td colspan={tableColspan} class="empty-row"> This table is empty </td>
							</tr>
						{:else}
							{#each documentRows as row (row.id || row.key)}
								<tr>
									<td class="col-select">
										{#if row.id}
											<Switch
												name={`select-document-${row.id}`}
												aria-label={`Select ${row.id}`}
												checked={Boolean(selectedDocIds[row.id])}
												disabled={!row.rev || working}
												onCheckedChange={(event) => setRowSelection(row.id, event.checked)}
											>
												<Switch.Control class="c8o-switch transition-surface">
													<Switch.Thumb />
												</Switch.Control>
												<Switch.HiddenInput />
											</Switch>
										{/if}
									</td>
									{#if currentLayout == 'metadata'}
										<td class="col-copy">
											<button
												type="button"
												class="button-ico-primary h-8 w-8 justify-center p-0!"
												title="Copy row"
												aria-label="Copy row"
												onclick={() => copyRow(row)}
											>
												<Ico icon="mdi:content-copy" size={4.5} />
											</button>
										</td>
										<td class="metadata-col-id">
											<button
												type="button"
												class="metadata-link text-left text-primary-500 transition-surface hover:underline"
												title={row.id}
												onclick={() => openDocument(row.id)}
											>
												<span class="metadata-ellipsis">{row.id}</span>
											</button>
										</td>
										<td class="metadata-col-key">
											<span class="metadata-ellipsis" title={row.key}>{row.key}</span>
										</td>
										<td class="metadata-col-value">
											<span class="metadata-ellipsis" title={row.valuePreview}
												>{row.valuePreview}</span
											>
										</td>
									{:else if currentLayout == 'table'}
										<td class="break-all">
											<button
												type="button"
												class="text-left break-all text-primary-500 transition-surface hover:underline"
												onclick={() => openDocument(row.id)}
											>
												{row.id}
											</button>
										</td>
										<td>{row.type || '-'}</td>
										<td class="break-all">{row.rev || '-'}</td>
									{:else}
										<td class="col-copy">
											<button
												type="button"
												class="button-ico-primary h-8 w-8 justify-center p-0!"
												title="Copy row"
												aria-label="Copy row"
												onclick={() => copyRow(row)}
											>
												<Ico icon="mdi:content-copy" size={4.5} />
											</button>
										</td>
										<td><pre class="text-xs leading-5">{row.jsonText}</pre></td>
									{/if}
								</tr>
							{/each}
						{/if}
					</tbody>
				</table>
			</div>

			<div class="fullsync-footer">
				<div class="layout-x-wrap items-center gap-2 text-sm text-muted">
					<span
						>Showing document {documentRows.length > 0 ? docsSkip + 1 : 0} - {docsSkip +
							documentRows.length}.</span
					>
				</div>
				<div class="layout-x-wrap items-center gap-2">
					<label class="layout-x-low items-center text-sm text-muted">
						<span>Documents per page:</span>
						<select
							class="h-9 input-common px-2 text-sm"
							value={docsLimitValue}
							onchange={(event) => {
								docsLimitValue = event.currentTarget.value;
								docsSkip = 0;
								void refreshDocuments();
							}}
						>
							<option value="5">5</option>
							<option value="10">10</option>
							<option value="20">20</option>
							<option value="30">30</option>
							<option value="50">50</option>
							<option value="100">100</option>
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
						<Ico icon="mdi:arrow-left-bold-outline" />
					</button>
					<button
						type="button"
						class="button-ico-primary h-7 w-7 justify-center p-0! disabled:cursor-not-allowed disabled:opacity-35"
						title="Next page"
						aria-label="Next page"
						disabled={!canGoNextDocs}
						onclick={nextDocsPage}
					>
						<Ico icon="mdi:arrow-right-bold-outline" />
					</button>
				</div>
			</div>
		</section>
	{:else}
		<Card title="Run A Query with Mango">
			{#snippet cornerOption()}
				<Button
					label="Run Query"
					icon="mdi:magnify"
					class="button-primary w-fit!"
					disabled={working}
					onclick={runMangoAction}
				/>
			{/snippet}
			<div class="layout-cols-2 w-full">
				<label class="layout-y-low">
					<span class="text-xs text-muted">Mango payload</span>
					<textarea
						class="min-h-[360px] input-common resize-y p-3 font-mono text-xs"
						bind:value={mangoQueryText}
					></textarea>
				</label>
				<label class="layout-y-low">
					<span class="text-xs text-muted">Result</span>
					{#if mangoResultText}
						<pre
							class="min-h-[360px] input-common overflow-auto p-3 text-xs leading-5">{mangoResultText}</pre>
					{:else}
						<div
							class="layout-y-none min-h-[360px] input-common place-items-center gap-2 p-3 text-sm text-muted"
						>
							<Ico icon="mdi:magnify" size={6} />
							<span>No result yet.</span>
						</div>
					{/if}
				</label>
			</div>
		</Card>
	{/if}
</div>

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

	.fullsync-main-panel {
		position: relative;
		display: grid;
		gap: calc(var(--spacing) * 1.25);
		padding: calc(var(--spacing) * 2);
		border: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
		border-radius: var(--radius-container);
		background: light-dark(var(--color-surface-100), var(--color-surface-900));
	}

	.fullsync-toolbar {
		display: grid;
		grid-template-columns: minmax(16rem, 1fr) auto;
		gap: calc(var(--spacing) * 1.5);
		align-items: center;
		padding-bottom: calc(var(--spacing) * 1);
		border-bottom: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
	}

	.fullsync-toolbar-actions {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		justify-content: flex-end;
		gap: calc(var(--spacing) * 1.25);
	}

	.fullsync-layout-bar {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: calc(var(--spacing) * 1.5);
		padding-bottom: calc(var(--spacing) * 0.5);
	}

	.fullsync-footer {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		justify-content: space-between;
		gap: calc(var(--spacing) * 2);
		padding-top: calc(var(--spacing) * 0.75);
	}

	.doc-open-form {
		min-width: 0;
		width: 100%;
		max-width: none;
	}

	.query-options-panel {
		border: 1px solid var(--color-surface-500);
		background: light-dark(var(--color-surface-100), var(--color-surface-900));
		border-radius: var(--radius-container);
		padding: calc(var(--spacing) * 3);
	}

	.query-options-floating {
		position: absolute;
		top: calc(var(--spacing) * 7.4);
		right: calc(var(--spacing) * 2);
		width: min(30rem, calc(100% - (var(--spacing) * 4)));
		z-index: 20;
		box-shadow: var(--shadow-follow);
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

	.design-doc-sub-link {
		position: relative;
		display: flex;
		align-items: center;
		padding: calc(var(--spacing) * 1.5) calc(var(--spacing) * 2.5) calc(var(--spacing) * 1.5)
			calc(var(--spacing) * 9);
		font-size: 13px;
		color: var(--convertigo-text-strong);
	}

	.design-doc-sub-link:hover,
	.design-doc-link:hover {
		background: light-dark(
			color-mix(in srgb, var(--color-primary-300) 26%, transparent),
			color-mix(in srgb, var(--color-primary-600) 24%, transparent)
		);
	}

	.design-doc-sub-title {
		padding: calc(var(--spacing) * 1) calc(var(--spacing) * 2.5) calc(var(--spacing) * 1)
			calc(var(--spacing) * 9);
		font-size: 12px;
		color: var(--convertigo-text-muted);
		text-transform: uppercase;
		letter-spacing: 0.02em;
	}

	.docs-table-wrap {
		overflow-x: auto;
	}

	.docs-table {
		width: 100%;
		border-collapse: collapse;
	}

	.docs-table th,
	.docs-table td {
		padding: calc(var(--spacing) * 1.5) calc(var(--spacing) * 1.5);
		vertical-align: top;
	}

	.docs-table thead th {
		border-bottom: 1px solid light-dark(var(--color-surface-500), var(--color-surface-500));
		font-size: 0.98rem;
		font-weight: 600;
		color: var(--convertigo-text-strong);
		text-align: left;
	}

	.docs-table tbody tr {
		border-bottom: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
	}

	.docs-table tbody tr:last-child {
		border-bottom: 0;
	}

	.docs-table td pre {
		margin: 0;
	}

	.docs-table-metadata {
		table-layout: fixed;
	}

	.docs-table-metadata .metadata-col-id {
		width: 30%;
	}

	.docs-table-metadata .metadata-col-key {
		width: 30%;
	}

	.docs-table-metadata .metadata-col-value {
		width: 40%;
	}

	.docs-table-metadata td {
		overflow: hidden;
	}

	.metadata-link {
		display: block;
		width: 100%;
		min-width: 0;
	}

	.metadata-ellipsis {
		display: block;
		max-width: 100%;
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.col-copy {
		width: 2.6rem;
	}

	.col-select {
		width: 3rem;
		text-align: center;
	}

	.col-select :global([data-part='root']) {
		display: inline-flex;
		align-items: center;
		justify-content: center;
	}

	.empty-row {
		padding: calc(var(--spacing) * 4) calc(var(--spacing) * 1.5);
		color: var(--convertigo-text-muted);
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
	}
</style>
