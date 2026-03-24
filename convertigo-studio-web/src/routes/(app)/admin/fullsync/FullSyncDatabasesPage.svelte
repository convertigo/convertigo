<script>
	import { building } from '$app/environment';
	import { goto } from '$app/navigation';
	import { getAdminPageDocHref } from '$lib/admin/AdminDocumentation.svelte';
	import ActionBar from '$lib/admin/components/ActionBar.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import InputGroup from '$lib/common/components/InputGroup.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { onMount } from 'svelte';
	import { fullSyncBaseUrl, getDatabaseInfo, listDatabases, removeDatabase } from './fullsync-api';
	import { createFullSyncFeedback } from './fullsync-feedback';
	import { FULLSYNC_DOCS } from './fullsync-links';
	import { getFullSyncConfirmModal, openFullSyncConfirmation } from './fullsync-modal';
	import { fullSyncDbHref } from './fullsync-route';
	import { FULLSYNC_DB_LIST_PAGE_OPTIONS, fullSyncOptionLabel } from './fullsync-ui';
	import { resolve } from '$lib/utils/route';

	const modalYesNo = getFullSyncConfirmModal();
	const fullSyncDocHref = getAdminPageDocHref('/admin/fullsync');
	const fullSyncConfigHref = resolve('/(app)/admin/config/[category]', {
		category: 'FullSync'
	});

	let loadingDatabases = $state(true);
	let working = $state(false);
	let lastError = $state('');
	const { showError, showSuccess } = createFullSyncFeedback((message) => {
		lastError = message;
	});

	let databases = $state([]);
	let databaseFilter = $state('');
	let dbInfoByName = $state(/** @type {Record<string, any>} */ ({}));
	let dbPage = $state(1);
	let dbPerPage = $state(20);
	let dbInfoRequestCounter = 0;
	let dbSearchListId = $state('fullsync-db-search-list');
	let allDbsJsonHref = $derived(building ? '#' : `${fullSyncBaseUrl()}_all_dbs`);
	let shouldShowConfigHint = $derived(
		!loadingDatabases &&
			!working &&
			!lastError &&
			databases.length == 0 &&
			databaseFilter.trim().length == 0
	);

	let filteredDatabases = $derived(
		databases.filter((db) => db.toLowerCase().includes(databaseFilter.trim().toLowerCase()))
	);
	let dbTotalPages = $derived(Math.max(1, Math.ceil(filteredDatabases.length / dbPerPage)));
	let dbPageSafe = $derived(Math.min(dbPage, dbTotalPages));
	let visibleDatabaseNames = $derived(
		filteredDatabases.slice((dbPageSafe - 1) * dbPerPage, dbPageSafe * dbPerPage)
	);

	let databaseRows = $derived.by(() => {
		if (loadingDatabases && databases.length == 0) {
			return Array.from({ length: 8 }, (_, i) => ({
				name: `loading-${i}`,
				sizeK: null,
				seq: null,
				docCount: null,
				isLoading: true
			}));
		}

		return visibleDatabaseNames.map((name) => {
			const info = detailsForDb(name);
			return {
				name,
				sizeK: formatSizeInK(info?.sizes?.active),
				seq: extractSeq(info?.update_seq),
				docCount: info?.doc_count ?? null,
				isLoading: false
			};
		});
	});

	$effect(() => {
		if (dbPage > dbTotalPages) {
			dbPage = dbTotalPages;
		}
	});

	$effect(() => {
		const names = visibleDatabaseNames;
		void loadDatabaseInfos(names);
	});

	function formatSizeInK(bytes) {
		if (bytes == null) return 'n/a';
		const num = Number(bytes);
		if (!Number.isFinite(num) || num < 0) return 'n/a';
		const kilo = num / 1024;
		if (kilo >= 1000) {
			const mega = kilo / 1000;
			const roundedM = Math.round(mega * 10) / 10;
			return `${roundedM % 1 == 0 ? roundedM.toFixed(0) : roundedM.toFixed(1)} M`;
		}
		const roundedK = Math.round(kilo * 10) / 10;
		return `${roundedK % 1 == 0 ? roundedK.toFixed(0) : roundedK.toFixed(1)} k`;
	}

	function extractSeq(updateSeq) {
		if (updateSeq == null) return null;
		const raw = String(updateSeq);
		const head = raw.split('-', 1)[0]?.trim();
		if (!head) return '?';
		const num = Number(head);
		return Number.isFinite(num) ? String(Math.trunc(num)) : '?';
	}

	function detailsForDb(name) {
		return dbInfoByName[name] ?? null;
	}

	async function openDatabase(dbName) {
		await goto(fullSyncDbHref(dbName));
	}

	async function openDatabaseFromFilter() {
		const query = databaseFilter.trim().toLowerCase();
		if (!query) return;
		const exact = databases.find((db) => db.toLowerCase() == query);
		const closest = filteredDatabases[0] ?? null;
		const target = exact ?? closest;
		if (!target) return;
		await openDatabase(target);
	}

	async function refreshDatabases() {
		loadingDatabases = true;
		lastError = '';
		try {
			databases = await listDatabases();
			dbInfoByName = {};
			dbPage = 1;
		} catch (error) {
			showError(error);
		} finally {
			loadingDatabases = false;
		}
	}

	async function loadDatabaseInfos(names) {
		const missing = names.filter((name) => dbInfoByName[name] == null);
		if (!missing.length) return;
		const token = ++dbInfoRequestCounter;
		const loaded = {};
		await Promise.all(
			missing.map(async (name) => {
				try {
					loaded[name] = await getDatabaseInfo(name);
				} catch {
					loaded[name] = {
						doc_count: '?',
						update_seq: '?',
						sizes: { active: -1 },
						partitioned: null,
						isError: true
					};
				}
			})
		);
		if (token != dbInfoRequestCounter) return;
		dbInfoByName = { ...dbInfoByName, ...loaded };
	}

	async function removeDatabaseAction(event, dbName) {
		const ok = await openFullSyncConfirmation(
			modalYesNo,
			event,
			'Delete database',
			`Do you confirm deleting "${dbName}" and all its documents?`
		);
		if (!ok) return;

		working = true;
		lastError = '';
		try {
			await removeDatabase(dbName);
			showSuccess(`Database "${dbName}" deleted`);
			await refreshDatabases();
		} catch (error) {
			showError(error);
		} finally {
			working = false;
		}
	}

	onMount(async () => {
		await refreshDatabases();
	});
</script>

<div class="layout-y-stretch">
	<Card title="Databases" cornerOptionClass="w-full md:w-auto" docHref={fullSyncDocHref}>
		{#snippet cornerOption()}
			<ActionBar full={false} wrap={false} class="db-card-actions">
				<Button
					full={false}
					icon="mdi:refresh"
					class="button-ico-primary h-9 w-9 min-w-9 justify-center p-0!"
					title="Refresh databases"
					ariaLabel="Refresh databases"
					onclick={refreshDatabases}
					disabled={loadingDatabases || working}
				/>
				<InputGroup
					class="db-search-input h-9 min-w-0"
					type="text"
					placeholder="Search databases"
					icon="mdi:magnify"
					list={dbSearchListId}
					bind:value={databaseFilter}
					oninput={() => {
						dbPage = 1;
					}}
					onkeydown={(event) => {
						if (event.key === 'Enter') {
							event.preventDefault();
							void openDatabaseFromFilter();
						}
					}}
				>
					{#snippet actions()}
						<button
							type="button"
							class="button-ico-primary h-7 w-7 justify-center p-0! disabled:cursor-not-allowed disabled:opacity-35"
							title="Open database"
							aria-label="Open database"
							disabled={databaseFilter.trim().length == 0}
							onclick={() => void openDatabaseFromFilter()}
						>
							<Ico icon="mdi:arrow-right" size={5} />
						</button>
					{/snippet}
				</InputGroup>
				<datalist id={dbSearchListId}>
					{#each filteredDatabases.slice(0, 200) as dbName (dbName)}
						<option value={dbName}></option>
					{/each}
				</datalist>
				<Button
					label="JSON"
					icon="mdi:code-braces"
					class="db-json-btn button-secondary h-9 w-fit!"
					href={allDbsJsonHref}
					target="_blank"
					rel="noopener noreferrer"
				/>
				<Button
					icon="mdi:book-open-variant"
					class="db-doc-btn button-secondary h-9 w-fit! px-2!"
					title="CouchDB documentation"
					ariaLabel="CouchDB documentation"
					href={FULLSYNC_DOCS.serverAllDbs}
					target="_blank"
					rel="noopener noreferrer"
				/>
			</ActionBar>
		{/snippet}

		{#if lastError}
			<div
				class="mb-2 rounded-base border border-error-300-700 bg-error-100-900 px-3 py-2 text-sm text-error-900-100"
			>
				<p>{lastError}</p>
				<p class="mt-2">
					Check the
					<a
						href={fullSyncConfigHref}
						class="font-semibold underline underline-offset-2 transition-surface hover:no-underline"
					>
						FullSync configuration
					</a>
					if the CouchDB server URL or credentials need to be updated.
				</p>
			</div>
		{:else if shouldShowConfigHint}
			<div
				class="mb-2 rounded-base border border-surface-300-700 bg-surface-100-900 px-3 py-2 text-sm text-strong"
			>
				No database was returned. If this is unexpected, check the
				<a
					href={fullSyncConfigHref}
					class="font-medium text-primary-500 transition-surface hover:underline"
				>
					FullSync configuration
				</a>.
			</div>
		{/if}

		<fieldset class="layout-y-stretch gap-3" disabled={loadingDatabases || working}>
			<TableAutoCard
				class="db-table"
				cardBreakpoint={1}
				definition={[
					{ name: 'Name', key: 'name', custom: true, class: 'min-w-52 break-all' },
					{ name: 'Docs', key: 'docCount', class: 'w-20' },
					{ name: 'Size', key: 'sizeK', class: 'w-24' },
					{ name: 'Seq', key: 'seq', class: 'w-20' },
					{ name: 'Actions', key: 'actions', custom: true, class: 'w-16' }
				]}
				data={databaseRows}
				fnRowId={(row) => row.name}
			>
				{#snippet children({ row, def })}
					{#if def.key == 'name'}
						{#if row.isLoading}
							<span class="text-muted">...</span>
						{:else}
							<button
								type="button"
								class="text-left break-all text-primary-500 transition-surface hover:underline"
								onclick={() => openDatabase(row.name)}
							>
								{row.name}
							</button>
						{/if}
					{:else if def.key == 'actions'}
						{#if !row.isLoading}
							<ResponsiveButtons
								class="w-full min-w-24"
								size="6"
								buttons={[
									{
										icon: 'mdi:delete-outline',
										cls: 'button-ico-primary',
										title: 'Delete database',
										onclick: (event) => removeDatabaseAction(event, row.name)
									}
								]}
							/>
						{/if}
					{/if}
				{/snippet}
			</TableAutoCard>

			<div class="layout-x-wrap items-center justify-between gap-3 text-sm">
				<div class="text-muted">
					Showing {(dbPageSafe - 1) * dbPerPage + 1}–{Math.min(
						dbPageSafe * dbPerPage,
						filteredDatabases.length
					)}
					of {filteredDatabases.length} databases
				</div>
				<ActionBar full={false} wrap={false} class="items-center gap-2">
					<label class="layout-x-low items-center text-sm text-muted">
						<span>Per page</span>
						<select
							class="select h-9 input-common w-20 px-2 text-sm"
							value={String(dbPerPage)}
							onchange={(event) => {
								dbPerPage = Number(event.currentTarget.value) || 20;
								dbPage = 1;
							}}
						>
							{#each FULLSYNC_DB_LIST_PAGE_OPTIONS as pageSizeOption (pageSizeOption)}
								<option value={pageSizeOption}>{fullSyncOptionLabel(pageSizeOption)}</option>
							{/each}
						</select>
					</label>
					<button
						type="button"
						class="button-ico-primary h-7 w-7 justify-center p-0! disabled:cursor-not-allowed disabled:opacity-35"
						title="Previous page"
						aria-label="Previous page"
						disabled={dbPageSafe <= 1}
						onclick={() => (dbPage = dbPageSafe - 1)}
					>
						<Ico icon="mdi:arrow-left-bold-outline" />
					</button>
					<span class="min-w-6 text-center font-medium">{dbPageSafe}</span>
					<button
						type="button"
						class="button-ico-primary h-7 w-7 justify-center p-0! disabled:cursor-not-allowed disabled:opacity-35"
						title="Next page"
						aria-label="Next page"
						disabled={dbPageSafe >= dbTotalPages}
						onclick={() => (dbPage = dbPageSafe + 1)}
					>
						<Ico icon="mdi:arrow-right-bold-outline" />
					</button>
				</ActionBar>
			</div>
		</fieldset>
	</Card>
</div>

<style lang="postcss">
	@reference "../../../../app.css";

	:global(.db-card-actions) {
		display: grid;
		grid-template-columns: auto minmax(13rem, 1fr) auto auto;
		align-items: center;
		gap: calc(var(--spacing) * 1.5);
		width: min(100%, 40rem);
	}

	:global(.db-search-input) {
		width: 100%;
	}

	:global(.db-table tr td:first-child button) {
		line-height: 1.35;
	}

	@media (max-width: 860px) {
		:global(.db-card-actions) {
			width: 100%;
			grid-template-columns: auto minmax(0, 1fr) auto auto;
		}
	}

	@media (max-width: 560px) {
		:global(.db-card-actions) {
			grid-template-columns: auto minmax(0, 1fr) auto;
		}

		:global(.db-doc-btn) {
			grid-column: 3;
			grid-row: 2;
			justify-self: end;
		}

		:global(.db-json-btn) {
			grid-column: 2;
			grid-row: 2;
			justify-self: end;
		}
	}
</style>
