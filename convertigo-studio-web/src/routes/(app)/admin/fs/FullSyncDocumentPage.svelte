<script>
	import { goto } from '$app/navigation';
	import ActionBar from '$lib/admin/components/ActionBar.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import FileUploadField from '$lib/admin/components/FileUploadField.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import LightSvelte from '$lib/common/Light.svelte.js';
	import Editor from '$lib/studio/editor/Editor.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { toaster } from '$lib/utils/service';
	import { getContext } from 'svelte';
	import RightPart from '../RightPart.svelte';
	import {
		cloneDocument,
		fullSyncBaseUrl,
		getDocument,
		getUuids,
		removeDocument,
		updateDocument,
		uploadAttachment
	} from './fullsync-api';
	import { fullSyncDbTabHref, fullSyncDocHref } from './fullsync-route';

	/** @type {{database: string, docId: string}} */
	let { database = '', docId = '' } = $props();

	RightPart.snippet = undefined;

	let modalYesNo;
	try {
		modalYesNo = getContext('modalYesNo');
	} catch {
		modalYesNo = undefined;
	}

	let cloneModal;
	let uploadModal;

	let loadingDocument = $state(true);
	let working = $state(false);
	let uploadingAttachment = $state(false);
	let lastError = $state('');
	let documentText = $state('');
	let originalDocumentText = $state('');
	let currentDocId = $state('');
	let loadedKey = $state('');
	let cloneDocId = $state('');
	let cloneError = $state('');
	let uploadError = $state('');

	let isNewDocument = $derived(currentDocId == '_new');
	let hasUnsavedChanges = $derived(documentText != originalDocumentText);
	let editorTheme = $derived(LightSvelte.light ? '' : 'vs-dark');
	let rawDocumentUrl = $derived.by(() => {
		if (!database || !currentDocId || isNewDocument) return '#';
		return `${fullSyncBaseUrl()}${encodeURIComponent(database)}/${encodeDocPath(currentDocId)}`;
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

	function parseJson(content, label) {
		try {
			return JSON.parse(content);
		} catch (error) {
			const message = error instanceof Error ? error.message : String(error);
			showError(`${label}: invalid JSON (${message})`);
			return null;
		}
	}

	function parseJsonSilent(content) {
		try {
			return JSON.parse(content);
		} catch {
			return null;
		}
	}

	function pretty(value) {
		return JSON.stringify(value ?? {}, null, 2);
	}

	function encodeDocPath(docId) {
		const raw = String(docId ?? '');
		if (raw.startsWith('_design/')) {
			return `_design/${encodeURIComponent(raw.slice('_design/'.length))}`;
		}
		return raw
			.split('/')
			.map((part) => encodeURIComponent(part))
			.join('/');
	}

	function getPartitionPrefix(docId) {
		const raw = String(docId ?? '');
		const idx = raw.indexOf(':');
		return idx > 0 ? raw.slice(0, idx + 1) : '';
	}

	function fallbackCloneId(prefix = '') {
		const now = Date.now().toString(36);
		const random = Math.random().toString(36).slice(2, 10);
		return `${prefix}${now}${random}`;
	}

	async function suggestCloneId(prefix = '') {
		try {
			const uuids = await getUuids(1);
			if (uuids[0]) {
				return `${prefix}${uuids[0]}`;
			}
		} catch {
			// ignore and fallback below
		}
		return fallbackCloneId(prefix);
	}

	function openRawJson() {
		if (!rawDocumentUrl || rawDocumentUrl == '#') return;
		window.open(rawDocumentUrl, '_blank', 'noopener');
	}

	function openDocumentation() {
		window.open(
			'https://docs.couchdb.org/en/stable/api/document/common.html',
			'_blank',
			'noopener'
		);
	}

	async function askConfirmation(event, title, message) {
		if (modalYesNo?.open) {
			return await modalYesNo.open({ event, title, message });
		}
		return window.confirm(`${title}\n\n${message}`);
	}

	async function refreshDocument() {
		if (!database || !currentDocId) return;
		loadingDocument = true;
		lastError = '';
		try {
			if (currentDocId == '_new') {
				originalDocumentText = pretty({
					_id: 'sample-document',
					type: 'sample'
				});
				documentText = originalDocumentText;
				return;
			}
			const document = await getDocument(database, currentDocId);
			originalDocumentText = pretty(document);
			documentText = originalDocumentText;
		} catch (error) {
			showError(error);
		} finally {
			loadingDocument = false;
		}
	}

	function cancelDocumentEdit() {
		documentText = originalDocumentText;
	}

	function closeCloneModal(value = false) {
		cloneError = '';
		cloneModal?.close(value);
	}

	function closeUploadModal(value = false) {
		uploadError = '';
		uploadModal?.close(value);
	}

	async function openCloneModal(event) {
		if (!database || isNewDocument || working || loadingDocument || uploadingAttachment) return;
		const sourceDocument = parseJsonSilent(originalDocumentText);
		const partitionPrefix = getPartitionPrefix(sourceDocument?._id);
		cloneDocId = await suggestCloneId(partitionPrefix);
		cloneError = '';
		await cloneModal?.open({ event });
	}

	async function openUploadModal(event) {
		if (!database || isNewDocument || working || loadingDocument || uploadingAttachment) return;
		uploadError = '';
		await uploadModal?.open({ event });
	}

	async function cloneDocumentAction() {
		if (!database || working || loadingDocument) return;
		const targetId = cloneDocId.trim();
		if (!targetId) {
			cloneError = 'Please provide a target document ID.';
			return;
		}

		const sourceDocument = parseJson(originalDocumentText, 'Clone source');
		if (!sourceDocument) return;
		if (!sourceDocument._id) {
			cloneError = 'The source document must contain "_id".';
			showError(cloneError);
			return;
		}

		working = true;
		lastError = '';
		cloneError = '';
		try {
			await cloneDocument(database, sourceDocument, targetId);
			showSuccess(`Document "${targetId}" cloned`);
			closeCloneModal(true);
			await goto(fullSyncDocHref(database, targetId));
		} catch (error) {
			cloneError = asErrorMessage(error);
			showError(error);
		} finally {
			working = false;
		}
	}

	async function uploadAttachmentAction(event) {
		if (!database || uploadingAttachment || working) return;
		const target = /** @type {HTMLFormElement | null} */ (event?.currentTarget ?? null);
		const formData = target ? new FormData(target) : new FormData();
		const fileValue = formData.get('attachment');
		const file = fileValue instanceof File && fileValue.size > 0 ? fileValue : null;
		if (!file) {
			uploadError = 'Please select a file to upload.';
			return;
		}

		const sourceDocument = parseJson(originalDocumentText, 'Attachment upload');
		if (!sourceDocument) return;
		if (!sourceDocument._id || !sourceDocument._rev) {
			uploadError = 'The document must contain "_id" and "_rev" to upload attachments.';
			showError(uploadError);
			return;
		}

		uploadingAttachment = true;
		lastError = '';
		uploadError = '';
		try {
			await uploadAttachment(database, sourceDocument._id, sourceDocument._rev, file);
			showSuccess(`Attachment "${file.name}" uploaded`);
			closeUploadModal(true);
			await refreshDocument();
		} catch (error) {
			uploadError = asErrorMessage(error);
			showError(error);
		} finally {
			uploadingAttachment = false;
		}
	}

	async function saveDocument() {
		if (!database || !currentDocId) return;
		const document = parseJson(documentText, 'Document editor');
		if (!document) return;
		if (!document._id) {
			showError('The document must contain "_id"');
			return;
		}

		working = true;
		lastError = '';
		try {
			const response = await updateDocument(database, document._id, document);
			const nextDocId = response?.id ?? document._id;
			showSuccess(`Document "${nextDocId}" saved`);
			if (nextDocId != currentDocId) {
				await goto(fullSyncDocHref(database, nextDocId));
			} else {
				await refreshDocument();
			}
		} catch (error) {
			showError(error);
		} finally {
			working = false;
		}
	}

	async function deleteDocument(event) {
		if (isNewDocument) return;
		const document = parseJson(documentText, 'Document editor');
		if (!document) return;
		if (!document._id || !document._rev) {
			showError('The document must contain "_id" and "_rev"');
			return;
		}

		const ok = await askConfirmation(
			event,
			'Delete document',
			`Do you confirm deleting "${document._id}"?`
		);
		if (!ok) return;

		working = true;
		lastError = '';
		try {
			await removeDocument(database, document._id, document._rev);
			showSuccess(`Document "${document._id}" deleted`);
			await goto(fullSyncDbTabHref(database, 'all'));
		} catch (error) {
			showError(error);
		} finally {
			working = false;
		}
	}

	$effect(() => {
		const nextKey = `${database}::${docId}`;
		if (!database || !docId) return;
		if (nextKey == loadedKey) return;
		loadedKey = nextKey;
		currentDocId = docId;
		documentText = '';
		originalDocumentText = '';
		cloneDocId = '';
		cloneError = '';
		uploadError = '';
		void refreshDocument();
	});
</script>

<div class="layout-y-stretch">
	<section class="fullsync-doc-panel">
		<header class="doc-header-row">
			<div class="doc-title-row">
				<a
					href={fullSyncDbTabHref(database, 'all')}
					class="doc-back-link"
					title="All Documents"
					aria-label="All Documents"
				>
					<Ico icon="mdi:arrow-left-bold-outline" size={5} />
				</a>
				<a href={fullSyncDbTabHref(database, 'all')} class="doc-db-link" title={database}
					>{database}</a
				>
				<Ico icon="mdi:chevron-right" size={4} class="text-strong" />
				<span class="doc-id-text" title={currentDocId}>{currentDocId}</span>
			</div>
			<ActionBar full={false} wrap={true} justify="start" class="w-fit gap-2 max-md:w-full">
				<Button
					label="JSON"
					icon="mdi:code-braces"
					class="button-secondary h-9 w-fit!"
					disabled={rawDocumentUrl == '#'}
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

		<div class="doc-actions-row">
			<ActionBar full={false} wrap={true} justify="start" class="w-fit gap-2 max-md:w-full">
				<Button
					label="Save Changes"
					icon="mdi:content-save-edit-outline"
					class="button-primary h-9 w-fit!"
					disabled={working || uploadingAttachment || loadingDocument || !hasUnsavedChanges}
					onclick={saveDocument}
				/>
				<Button
					label="Cancel"
					icon="mdi:close-circle-outline"
					class="button-secondary h-9 w-fit!"
					disabled={working || uploadingAttachment || loadingDocument || !hasUnsavedChanges}
					onclick={cancelDocumentEdit}
				/>
			</ActionBar>

			<ActionBar full={false} wrap={true} justify="start" class="w-fit gap-2 max-md:w-full">
				<Button
					label="Upload Attachment"
					icon="mdi:briefcase-upload-outline"
					class="button-secondary h-9 w-fit!"
					disabled={working || uploadingAttachment || loadingDocument || isNewDocument}
					onclick={openUploadModal}
				/>
				<Button
					label="Clone Document"
					icon="mdi:cached"
					class="button-secondary h-9 w-fit!"
					disabled={working || uploadingAttachment || loadingDocument || isNewDocument}
					onclick={openCloneModal}
				/>
				<Button
					label="Delete"
					icon="mdi:delete-outline"
					class="button-secondary h-9 w-fit!"
					disabled={working || uploadingAttachment || loadingDocument || isNewDocument}
					onclick={deleteDocument}
				/>
			</ActionBar>
		</div>

		{#if lastError}
			<div
				class="rounded-base border border-error-300-700 bg-error-100-900 px-3 py-2 text-sm text-error-900-100"
			>
				{lastError}
			</div>
		{/if}

		{#if loadingDocument}
			<div class="doc-loading">
				<Ico icon="mdi:refresh" size={6} />
				<span>Loading document...</span>
			</div>
		{:else}
			<div class="doc-editor-shell">
				<Editor
					bind:content={documentText}
					language="json"
					readOnly={working || uploadingAttachment}
					theme={editorTheme}
				/>
			</div>
		{/if}
	</section>
</div>

<ModalDynamic bind:this={uploadModal}>
	{#snippet children({ close })}
		<Card title="Upload Attachment" style="width: min(42rem, calc(100vw - (var(--spacing) * 4)));">
			<form
				onsubmit={async (event) => {
					event.preventDefault();
					await uploadAttachmentAction(event);
				}}
			>
				<fieldset class="layout-y-stretch gap-3" disabled={uploadingAttachment}>
					<p class="text-sm text-strong">
						Select a file to upload as an attachment to this document. Uploading a file saves the
						document as a new revision.
					</p>
					<FileUploadField
						name="attachment"
						required
						allowDrop
						title="Drop or choose a file"
						hint="then press Upload Attachment"
						dropIcon="mdi:briefcase-upload-outline"
						itemIcon="mdi:file-outline"
						deleteIcon="mdi:delete-outline"
					/>
					{#if uploadError}
						<div
							class="rounded-base border border-error-300-700 bg-error-100-900 px-3 py-2 text-sm text-error-900-100"
						>
							{uploadError}
						</div>
					{/if}
					<ActionBar full={false} wrap={true} class="justify-end gap-2">
						<Button
							label="Cancel"
							icon="mdi:close-circle-outline"
							class="button-secondary w-fit!"
							disabled={uploadingAttachment}
							onclick={() => {
								uploadError = '';
								close(false);
							}}
						/>
						<Button
							label="Upload Attachment"
							icon="mdi:briefcase-upload-outline"
							class="button-primary w-fit!"
							disabled={uploadingAttachment}
							type="submit"
						/>
					</ActionBar>
				</fieldset>
			</form>
		</Card>
	{/snippet}
</ModalDynamic>

<ModalDynamic bind:this={cloneModal}>
	{#snippet children({ close })}
		<Card title="Clone Document" style="width: min(42rem, calc(100vw - (var(--spacing) * 4)));">
			<div class="layout-y-stretch gap-3">
				<p class="text-sm text-strong">
					Document cloning copies the saved version of the document. Unsaved document changes will
					be discarded.
				</p>
				<label class="layout-y-stretch gap-1">
					<span class="label-common">New document ID</span>
					<input
						type="text"
						class="h-10 input-common px-3 text-sm"
						bind:value={cloneDocId}
						disabled={working}
						onkeydown={(event) => {
							if (event.key === 'Enter') {
								event.preventDefault();
								void cloneDocumentAction();
							}
						}}
					/>
				</label>
				{#if cloneError}
					<div
						class="rounded-base border border-error-300-700 bg-error-100-900 px-3 py-2 text-sm text-error-900-100"
					>
						{cloneError}
					</div>
				{/if}
				<ActionBar full={false} wrap={true} class="justify-end gap-2">
					<Button
						label="Cancel"
						icon="mdi:close-circle-outline"
						class="button-secondary w-fit!"
						disabled={working}
						onclick={() => {
							cloneError = '';
							close(false);
						}}
					/>
					<Button
						label="Clone Document"
						icon="mdi:cached"
						class="button-primary w-fit!"
						disabled={working || cloneDocId.trim().length == 0}
						onclick={cloneDocumentAction}
					/>
				</ActionBar>
			</div>
		</Card>
	{/snippet}
</ModalDynamic>

<style lang="postcss">
	@reference "../../../../app.css";

	.fullsync-doc-panel {
		display: grid;
		gap: calc(var(--spacing) * 1.25);
		padding: calc(var(--spacing) * 2);
		border: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
		border-radius: var(--radius-container);
		background: light-dark(var(--color-surface-100), var(--color-surface-900));
		min-height: calc(100dvh - 13rem);
	}

	.doc-header-row {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		justify-content: space-between;
		gap: calc(var(--spacing) * 1.25);
		padding-bottom: calc(var(--spacing) * 1);
		border-bottom: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
	}

	.doc-title-row {
		display: flex;
		align-items: center;
		gap: calc(var(--spacing) * 1);
		min-width: 0;
	}

	.doc-back-link {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 1.9rem;
		height: 1.9rem;
		color: var(--color-primary-500);
		transition: color 140ms ease;
	}

	.doc-back-link:hover {
		color: var(--color-primary-600);
	}

	.doc-db-link {
		font-size: 1.7rem;
		font-weight: 600;
		line-height: 1.1;
		color: var(--color-primary-500);
		max-width: min(20rem, 34vw);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.doc-id-text {
		font-size: 1.7rem;
		font-weight: 500;
		line-height: 1.1;
		color: var(--convertigo-text-strong);
		max-width: min(27rem, 46vw);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.doc-actions-row {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		justify-content: space-between;
		gap: calc(var(--spacing) * 1);
		padding-bottom: calc(var(--spacing) * 1);
		border-bottom: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
	}

	.doc-loading {
		display: grid;
		place-items: center;
		gap: calc(var(--spacing) * 0.75);
		min-height: 20rem;
		border: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
		border-radius: var(--radius-base);
		color: var(--convertigo-text-muted);
		font-size: 0.92rem;
	}

	.doc-editor-shell {
		min-height: calc(100dvh - 20rem);
		height: 100%;
		border: 1px solid light-dark(var(--color-surface-300), var(--color-surface-700));
		border-radius: var(--radius-base);
		overflow: hidden;
	}

	@media (max-width: 960px) {
		.fullsync-doc-panel {
			min-height: calc(100dvh - 10rem);
		}

		.doc-db-link,
		.doc-id-text {
			font-size: 1.24rem;
			max-width: min(13rem, 42vw);
		}

		.doc-actions-row {
			align-items: stretch;
		}

		.doc-editor-shell {
			min-height: 58dvh;
		}
	}
</style>
