<script>
	import { Popover, Portal } from '@skeletonlabs/skeleton-svelte';
	import { goto } from '$app/navigation';
	import ActionBar from '$lib/admin/components/ActionBar.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import FileUploadField from '$lib/admin/components/FileUploadField.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import LightSvelte from '$lib/common/Light.svelte.js';
	import Editor from '$lib/studio/editor/Editor.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { tick } from 'svelte';
	import RightPart from '../RightPart.svelte';
	import {
		cloneDocument,
		encodeFullSyncDocPath as encodeDocPath,
		fullSyncBaseUrl,
		getDocument,
		getUuids,
		removeDocument,
		updateDocument,
		uploadAttachment
	} from './fullsync-api';
	import { createFullSyncFeedback, fullSyncErrorMessage } from './fullsync-feedback';
	import { fullSyncPretty, parseFullSyncJson, parseFullSyncJsonSilent } from './fullsync-json';
	import { FULLSYNC_DOCS, openFullSyncLink } from './fullsync-links';
	import { getFullSyncConfirmModal, openFullSyncConfirmation } from './fullsync-modal';
	import { fullSyncDbTabHref, fullSyncDocHref } from './fullsync-route';

	/** @type {{database: string, docId: string}} */
	let { database = '', docId = '' } = $props();

	RightPart.snippet = undefined;

	const modalYesNo = getFullSyncConfirmModal();

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
	let uploadHasFile = $state(false);
	let attachmentsPopoverOpen = $state(false);
	const { showError, showSuccess } = createFullSyncFeedback((message) => {
		lastError = message;
	});
	const parseJsonSilent = parseFullSyncJsonSilent;
	const pretty = fullSyncPretty;
	const parseJson = (content, label) => parseFullSyncJson(content, label, showError);

	let isNewDocument = $derived(currentDocId == '_new');
	let documentTitle = $derived(isNewDocument ? 'New Document' : currentDocId);
	let hasUnsavedChanges = $derived(documentText != originalDocumentText);
	let saveButtonLabel = $derived(isNewDocument ? 'Create Document' : 'Save Changes');
	let editorTheme = $derived(LightSvelte.light ? '' : 'vs-dark');
	let attachmentItems = $derived.by(() => {
		const parsedCurrent = parseJsonSilent(documentText);
		const parsedOriginal = parseJsonSilent(originalDocumentText);
		const attachments =
			(parsedCurrent?._attachments && typeof parsedCurrent._attachments == 'object'
				? parsedCurrent._attachments
				: parsedOriginal?._attachments) ?? {};

		if (!attachments || typeof attachments != 'object') {
			return [];
		}

		return Object.entries(attachments)
			.map(([name, meta]) => ({
				name,
				contentType:
					typeof meta?.content_type == 'string' && meta.content_type.trim().length > 0
						? meta.content_type
						: 'application/octet-stream',
				size:
					typeof meta?.length == 'number' && Number.isFinite(meta.length) && meta.length >= 0
						? meta.length
						: 0
			}))
			.sort((a, b) => a.name.localeCompare(b.name));
	});
	let rawDocumentUrl = $derived.by(() => {
		if (!database || !currentDocId) return '#';
		if (isNewDocument) return `${fullSyncBaseUrl()}${encodeURIComponent(database)}`;
		return `${fullSyncBaseUrl()}${encodeURIComponent(database)}/${encodeDocPath(currentDocId)}`;
	});

	function formatAttachmentSize(bytes) {
		const value = Math.max(0, Number(bytes) || 0);
		if (value >= 1024 * 1024) {
			return `${(value / (1024 * 1024)).toFixed(1)} MB`;
		}
		if (value >= 1024) {
			return `${(value / 1024).toFixed(1)} KB`;
		}
		return `${value} B`;
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

	function fallbackNewDocumentId() {
		const alphabet = '0123456789abcdef';
		let id = '';
		for (let i = 0; i < 32; i += 1) {
			id += alphabet[Math.floor(Math.random() * alphabet.length)];
		}
		return id;
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
		openFullSyncLink(rawDocumentUrl);
	}

	function openDocumentation() {
		openFullSyncLink(FULLSYNC_DOCS.documentApi);
	}

	function openAttachment(attachmentName) {
		if (!database || !currentDocId || isNewDocument) return;
		const name = String(attachmentName ?? '').trim();
		if (!name) return;
		const url = `${fullSyncBaseUrl()}${encodeURIComponent(database)}/${encodeDocPath(currentDocId)}/${encodeURIComponent(name)}`;
		openFullSyncLink(url);
		attachmentsPopoverOpen = false;
	}

	async function refreshDocument() {
		if (!database || !currentDocId) return;
		loadingDocument = true;
		lastError = '';
		try {
			if (currentDocId == '_new') {
				let generatedId = fallbackNewDocumentId();
				try {
					const uuids = await getUuids(1);
					if (uuids[0]) {
						generatedId = uuids[0];
					}
				} catch {
					// keep fallback id to let user proceed even if _uuids fails
				}
				originalDocumentText = pretty({
					_id: generatedId
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

	async function cancelDocumentEdit(event) {
		event?.preventDefault?.();
		await goto(fullSyncDbTabHref(database, 'all'));
	}

	function closeCloneModal(value = false) {
		cloneError = '';
		cloneModal?.close(value);
	}

	function closeUploadModal(value = false) {
		uploadError = '';
		uploadHasFile = false;
		uploadModal?.close(value);
	}

	async function openCloneModal(event) {
		if (!database || isNewDocument || working || loadingDocument || uploadingAttachment) return;
		const sourceDocument = parseJsonSilent(originalDocumentText);
		const partitionPrefix = getPartitionPrefix(sourceDocument?._id);
		cloneDocId = await suggestCloneId(partitionPrefix);
		cloneError = '';
		await cloneModal?.open({ event });
		await tick();
		if (typeof document != 'undefined') {
			const input = document.querySelector('#fullsync-clone-doc-id');
			if (input instanceof HTMLInputElement) {
				input.focus();
			}
		}
	}

	async function openUploadModal(event) {
		if (!database || isNewDocument || working || loadingDocument || uploadingAttachment) return;
		uploadError = '';
		uploadHasFile = false;
		await uploadModal?.open({ event });
	}

	function updateUploadFileState(target) {
		const form = /** @type {HTMLFormElement | null} */ (target ?? null);
		if (!form) {
			uploadHasFile = false;
			return;
		}
		const formData = new FormData(form);
		const fileValue = formData.get('attachment');
		uploadHasFile = fileValue instanceof File && String(fileValue.name ?? '').trim().length > 0;
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
		closeCloneModal(false);
		try {
			await cloneDocument(database, sourceDocument, targetId);
			showSuccess(`Document "${targetId}" cloned`);
			await goto(fullSyncDocHref(database, targetId));
		} catch (error) {
			showError(`Could not duplicate document, reason: ${fullSyncErrorMessage(error)}.`);
		} finally {
			working = false;
		}
	}

	async function uploadAttachmentAction(event) {
		if (!database || uploadingAttachment || working) return;
		const target = /** @type {HTMLFormElement | null} */ (event?.currentTarget ?? null);
		const formData = target ? new FormData(target) : new FormData();
		const fileValue = formData.get('attachment');
		const file =
			fileValue instanceof File && String(fileValue.name ?? '').trim().length > 0
				? fileValue
				: null;
		if (!file) {
			uploadHasFile = false;
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
			uploadError = fullSyncErrorMessage(error);
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
		if (!isNewDocument && document._id != currentDocId) {
			showError(
				"You cannot edit the _id of an existing document. Try this: Click 'Clone Document', then change the _id on the clone before saving."
			);
			return;
		}

		working = true;
		lastError = '';
		try {
			const response = await updateDocument(database, document._id, document);
			const nextDocId = response?.id ?? document._id;
			showSuccess(
				isNewDocument ? `Document "${nextDocId}" created` : `Document "${nextDocId}" saved`
			);
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
		const sourceDocument = parseJsonSilent(originalDocumentText);
		const sourceId = String(sourceDocument?._id ?? '').trim();
		const sourceRev = String(sourceDocument?._rev ?? '').trim();
		if (!sourceId || !sourceRev) {
			showError('The saved document must contain "_id" and "_rev"');
			return;
		}

		const ok = await openFullSyncConfirmation(
			modalYesNo,
			event,
			'Delete document',
			`Do you confirm deleting "${sourceId}"?`
		);
		if (!ok) return;

		working = true;
		lastError = '';
		try {
			await removeDocument(database, sourceId, sourceRev);
			showSuccess(`Document "${sourceId}" deleted`);
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
		attachmentsPopoverOpen = false;
		uploadHasFile = false;
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
				<span class="doc-id-text" title={documentTitle}>{documentTitle}</span>
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
					label={saveButtonLabel}
					icon="mdi:content-save-edit-outline"
					class="button-primary h-9 w-fit!"
					disabled={working ||
						uploadingAttachment ||
						loadingDocument ||
						(!isNewDocument && !hasUnsavedChanges)}
					onclick={saveDocument}
				/>
				<Button
					label="Cancel"
					icon="mdi:close-circle-outline"
					class="button-secondary h-9 w-fit!"
					disabled={working || uploadingAttachment || loadingDocument}
					onclick={cancelDocumentEdit}
				/>
			</ActionBar>

			{#if !isNewDocument}
				<ActionBar full={false} wrap={true} justify="start" class="w-fit gap-2 max-md:w-full">
					{#if attachmentItems.length > 0}
						<Popover
							open={attachmentsPopoverOpen}
							onOpenChange={(event) => (attachmentsPopoverOpen = event.open)}
						>
							<Popover.Trigger
								class="button-secondary inline-flex h-9 items-center gap-2 px-3 text-sm"
								title="View attachments"
								aria-label="View attachments"
							>
								<Ico icon="mdi:paperclip" size={4.4} />
								<span>View Attachments</span>
								<Ico icon="mdi:chevron-down" size={4} />
							</Popover.Trigger>
							<Portal>
								<Popover.Positioner class="z-[250]" style="z-index: 250;">
									<Popover.Content class="border-none bg-transparent p-0 shadow-none">
										<Card
											class="border-none! p-0! shadow-follow"
											style="width: min(28rem, calc(100vw - (var(--spacing) * 4)));"
										>
											<div class="attachment-menu-list">
												{#each attachmentItems as attachment (attachment.name)}
													<button
														type="button"
														class="attachment-menu-item"
														title={`${attachment.name} - ${attachment.contentType}, ${formatAttachmentSize(attachment.size)}`}
														onclick={() => openAttachment(attachment.name)}
													>
														<span class="attachment-menu-name">{attachment.name}</span>
														<span class="attachment-menu-meta">
															{attachment.contentType}, {formatAttachmentSize(attachment.size)}
														</span>
													</button>
												{/each}
											</div>
										</Card>
										<Popover.Arrow class="fill-primary-100-900" />
									</Popover.Content>
								</Popover.Positioner>
							</Portal>
						</Popover>
					{/if}
					<Button
						label="Upload Attachment"
						icon="mdi:briefcase-upload-outline"
						class="button-secondary h-9 w-fit!"
						disabled={working || uploadingAttachment || loadingDocument}
						onclick={openUploadModal}
					/>
					<Button
						label="Clone Document"
						icon="mdi:cached"
						class="button-secondary h-9 w-fit!"
						disabled={working || uploadingAttachment || loadingDocument}
						onclick={openCloneModal}
					/>
					<Button
						label="Delete"
						icon="mdi:delete-outline"
						class="button-secondary h-9 w-fit!"
						disabled={working || uploadingAttachment || loadingDocument}
						onclick={deleteDocument}
					/>
				</ActionBar>
			{/if}
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
				onchange={(event) => updateUploadFileState(event.currentTarget)}
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
								uploadHasFile = false;
								close(false);
							}}
						/>
						<Button
							label="Upload Attachment"
							icon="mdi:briefcase-upload-outline"
							class="button-primary w-fit!"
							disabled={uploadingAttachment || !uploadHasFile}
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
				<p class="text-sm text-strong">
					You can modify the following generated ID for your new document.
				</p>
				<label class="layout-y-stretch gap-1">
					<span class="label-common">New document ID</span>
					<input
						type="text"
						id="fullsync-clone-doc-id"
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
		flex: 1 1 auto;
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
		font-size: 1.24rem;
		font-weight: 600;
		line-height: 1.1;
		color: var(--color-primary-500);
		max-width: min(18rem, 24vw);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.doc-id-text {
		flex: 1 1 auto;
		min-width: 0;
		font-size: 1.24rem;
		font-weight: 500;
		line-height: 1.1;
		color: var(--convertigo-text-strong);
		max-width: none;
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

	.attachment-menu-list {
		display: grid;
	}

	.attachment-menu-item {
		display: grid;
		gap: calc(var(--spacing) * 0.25);
		padding: calc(var(--spacing) * 1) calc(var(--spacing) * 1.5);
		text-align: left;
		transition: background-color 140ms ease;
	}

	.attachment-menu-item:hover {
		background: light-dark(
			color-mix(in srgb, var(--color-primary-300) 18%, transparent),
			color-mix(in srgb, var(--color-primary-600) 22%, transparent)
		);
	}

	.attachment-menu-name {
		color: var(--convertigo-text-strong);
		font-size: 0.92rem;
		font-weight: 600;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.attachment-menu-meta {
		color: var(--convertigo-text-muted);
		font-size: 0.8rem;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	@media (max-width: 960px) {
		.fullsync-doc-panel {
			min-height: calc(100dvh - 10rem);
		}

		.doc-db-link,
		.doc-id-text {
			font-size: 1rem;
		}

		.doc-actions-row {
			align-items: stretch;
		}

		.doc-editor-shell {
			min-height: 58dvh;
		}

		.doc-db-link {
			max-width: min(10rem, 36vw);
		}

		.doc-id-text {
			max-width: 56vw;
		}
	}
</style>
