<script>
	import AutoSvg from '$lib/utils/AutoSvg.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { call, getUrl } from '$lib/utils/service';
	import StudioEmptyState from './StudioEmptyState.svelte';

	/**
	 * Searches the definition of the objects, as the Convertigo search of the Eclipse Studio, or the files
	 * of the projects, as its file search, with its results grouped by project as the search of Cursor.
	 *
	 * @typedef {{ id: string, name: string, type: string, project: string, path: string[], icon: string }} SearchResult
	 * @typedef {{ id: string, project: string, path: string, line: number, text: string }} FileResult
	 */

	/** @type {{
	 *  projectName?: string,
	 *  onSelect?: (id: string) => void,
	 *  onSelectFile?: (id: string, line: number) => void
	 * }}
	 */
	let { projectName = '', onSelect, onSelectFile } = $props();

	const OBJECT_TYPES = [
		'*',
		'Sequence',
		'Step',
		'Connector',
		'Transaction',
		'Variable',
		'TestCase',
		'MobileComponent',
		'UrlMapper',
		'UrlMapping',
		'UrlMappingOperation',
		'Reference',
		'Document',
		'Listener',
		'Pool',
		'Sheet',
		'ScreenClass',
		'Criteria',
		'ExtractionRule'
	];

	/** @type {'objects' | 'files'} */
	let what = $state('objects');
	let text = $state('');
	let matchCase = $state(false);
	let regExp = $state(false);
	let type = $state('*');
	/** @type {'workspace' | 'project'} */
	let scope = $state('workspace');
	let searching = $state(false);
	let error = $state('');
	/** @type {SearchResult[] | null} */
	let results = $state.raw(null);
	let truncated = $state(false);
	/** @type {Set<string>} */
	let collapsed = $state(new Set());
	let groups = $derived(groupByProject(results ?? []));
	/** @type {FileResult[] | null} */
	let fileResults = $state.raw(null);
	let fileGroups = $derived(groupFiles(fileResults ?? []));

	/**
	 * @param {FileResult[]} items
	 */
	function groupFiles(items) {
		/** @type {Map<string, Map<string, FileResult[]>>} */
		const byProject = new Map();
		for (const item of items) {
			const files = byProject.get(item.project) ?? new Map();
			files.set(item.path, [...(files.get(item.path) ?? []), item]);
			byProject.set(item.project, files);
		}
		return [...byProject.entries()].map(([project, files]) => ({
			project,
			count: [...files.values()].reduce((total, lines) => total + lines.length, 0),
			files: [...files.entries()].map(([path, lines]) => ({ path, lines }))
		}));
	}

	/**
	 * @param {SearchResult[]} items
	 */
	function groupByProject(items) {
		/** @type {Map<string, SearchResult[]>} */
		const byProject = new Map();
		for (const item of items) {
			byProject.set(item.project, [...(byProject.get(item.project) ?? []), item]);
		}
		return [...byProject.entries()].map(([project, entries]) => ({ project, entries }));
	}

	async function search(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		if (!text || searching) {
			return;
		}
		searching = true;
		error = '';
		try {
			if (what === 'files') {
				const response = await call('studio.source.Search', {
					text,
					matchCase: String(matchCase),
					regExp: String(regExp),
					...(scope === 'project' && projectName ? { scope: projectName } : {})
				});
				if (Array.isArray(response?.results)) {
					fileResults = response.results;
					truncated = Boolean(response.truncated);
					collapsed = new Set();
				} else {
					error = String(response?.error?.message ?? response?.message ?? 'The search failed.');
				}
				return;
			}
			const response = await call('studio.treeview.Search', {
				text,
				matchCase: String(matchCase),
				regExp: String(regExp),
				type,
				...(scope === 'project' && projectName ? { scope: projectName } : {})
			});
			if (Array.isArray(response?.results)) {
				results = response.results;
				truncated = Boolean(response.truncated);
				collapsed = new Set();
			} else {
				error = String(response?.error?.message ?? response?.message ?? 'The search failed.');
			}
		} finally {
			searching = false;
		}
	}

	/**
	 * @param {string} project
	 */
	function toggleGroup(project) {
		const next = new Set(collapsed);
		if (!next.delete(project)) {
			next.add(project);
		}
		collapsed = next;
	}
</script>

<div class="studio-search">
	<form class="studio-search__form" onsubmit={search}>
		<div class="studio-search__what" role="radiogroup" aria-label="Search in">
			{#each [{ value: 'objects', label: 'Objects' }, { value: 'files', label: 'Files' }] as choice (choice.value)}
				<button
					type="button"
					role="radio"
					aria-checked={what === choice.value}
					class={[
						'studio-search__what-choice',
						what === choice.value && 'studio-search__what-choice--on'
					]}
					onclick={() => (what = /** @type {'objects' | 'files'} */ (choice.value))}
					>{choice.label}</button
				>
			{/each}
		</div>
		<div class="studio-search__query">
			<input
				class="input-common"
				bind:value={text}
				placeholder={what === 'files' ? 'Search in the files' : 'Search in the objects'}
				aria-label={what === 'files' ? 'Search in the files' : 'Search in the objects'}
			/>
			<button
				type="button"
				class={['studio-search__toggle', matchCase && 'studio-search__toggle--on']}
				title="Match case"
				aria-pressed={matchCase}
				onclick={() => (matchCase = !matchCase)}>Aa</button
			>
			<button
				type="button"
				class={['studio-search__toggle', regExp && 'studio-search__toggle--on']}
				title="Use a regular expression"
				aria-pressed={regExp}
				onclick={() => (regExp = !regExp)}>.*</button
			>
		</div>
		<div class="studio-search__options">
			{#if what === 'objects'}
				<select class="select-common" bind:value={type} aria-label="Object type">
					{#each OBJECT_TYPES as objectType (objectType)}
						<option value={objectType}>{objectType === '*' ? 'All objects' : objectType}</option>
					{/each}
				</select>
			{/if}
			<select class="select-common" bind:value={scope} aria-label="Scope">
				<option value="workspace">All projects</option>
				<option value="project" disabled={!projectName}>{projectName || 'Selected project'}</option>
			</select>
			<button type="submit" class="button-primary" disabled={!text || searching}>
				<Ico icon={searching ? 'mdi:sync' : 'mdi:magnify'} size={4} />
			</button>
		</div>
	</form>

	<div class="studio-search__results">
		{#if error}
			<p class="studio-search__message studio-search__message--error">{error}</p>
		{:else if searching}
			<StudioEmptyState message="Searching" loading small />
		{:else if what === 'files'}
			{#if fileResults && !fileResults.length}
				<p class="studio-search__message">No line found.</p>
			{:else if fileResults}
				<p class="studio-search__message">
					{fileResults.length} line{fileResults.length > 1 ? 's' : ''} in {fileGroups.length} project{fileGroups.length >
					1
						? 's'
						: ''}{truncated ? ', more results are not shown' : ''}
				</p>
				{#each fileGroups as group (group.project)}
					<button
						type="button"
						class="studio-search__group"
						aria-expanded={!collapsed.has(group.project)}
						onclick={() => toggleGroup(group.project)}
					>
						<Ico
							icon={collapsed.has(group.project) ? 'mdi:chevron-right' : 'mdi:chevron-down'}
							size={4}
						/>
						<span>{group.project}</span>
						<small>{group.count}</small>
					</button>
					{#if !collapsed.has(group.project)}
						{#each group.files as file (file.path)}
							<div class="studio-search__file" title={file.path}>
								<Ico icon="mdi:file-document-outline" size={4} />
								<span class="studio-search__name">{file.path.split('/').pop()}</span>
								<small class="studio-search__path">{file.path}</small>
							</div>
							{#each file.lines as entry (entry.line)}
								<button
									type="button"
									class="studio-search__result studio-search__line"
									title={`${file.path}:${entry.line}`}
									onclick={() => onSelectFile?.(entry.id, entry.line)}
								>
									<small class="studio-search__line-number">{entry.line}</small>
									<span class="studio-search__line-text">{entry.text}</span>
								</button>
							{/each}
						{/each}
					{/if}
				{/each}
			{:else}
				<StudioEmptyState message="Search a text in the files of the projects" small />
			{/if}
		{:else if results && !results.length}
			<p class="studio-search__message">No object found.</p>
		{:else if results}
			<p class="studio-search__message">
				{results.length} object{results.length > 1 ? 's' : ''} in {groups.length} project{groups.length >
				1
					? 's'
					: ''}{truncated ? ', more results are not shown' : ''}
			</p>
			{#each groups as group (group.project)}
				<button
					type="button"
					class="studio-search__group"
					aria-expanded={!collapsed.has(group.project)}
					onclick={() => toggleGroup(group.project)}
				>
					<Ico
						icon={collapsed.has(group.project) ? 'mdi:chevron-right' : 'mdi:chevron-down'}
						size={4}
					/>
					<span>{group.project}</span>
					<small>{group.entries.length}</small>
				</button>
				{#if !collapsed.has(group.project)}
					{#each group.entries as entry (entry.id)}
						<button
							type="button"
							class="studio-search__result"
							title={[...entry.path, entry.name].join(' › ')}
							onclick={() => onSelect?.(entry.id)}
						>
							<AutoSvg class="h-4 w-4" fill="currentColor" src="{getUrl()}{entry.icon}" alt="" />
							<span class="studio-search__name">{entry.name}</span>
							<small class="studio-search__path">{entry.path.slice(1).join(' › ')}</small>
						</button>
					{/each}
				{/if}
			{/each}
		{:else}
			<StudioEmptyState message="Search a text in the definition of the objects" small />
		{/if}
	</div>
</div>

<style>
	.studio-search {
		display: grid;
		height: 100%;
		min-height: 0;
		grid-template-rows: auto minmax(0, 1fr);
	}

	.studio-search__form {
		display: grid;
		gap: 0.4rem;
		padding: 0 0.6rem 0.6rem;
	}

	.studio-search__query {
		position: relative;
		display: flex;
		align-items: center;
	}

	.studio-search__query input {
		height: 1.9rem;
		padding: 0 3.6rem 0 0.5rem;
		font-size: 0.8rem;
	}

	.studio-search__toggle {
		position: absolute;
		right: 0.25rem;
		width: 1.5rem;
		height: 1.4rem;
		border: 1px solid transparent;
		border-radius: 0.2rem;
		background: transparent;
		color: var(--studio-text-idle);
		font-size: 0.7rem;
		font-weight: 600;
	}

	.studio-search__toggle:first-of-type {
		right: 1.85rem;
	}

	.studio-search__toggle:hover {
		background: var(--studio-hover-bg);
	}

	.studio-search__toggle--on {
		border-color: var(--color-primary-500);
		background: color-mix(in oklab, var(--color-primary-500) 18%, transparent);
		color: var(--studio-text-strong);
	}

	.studio-search__options {
		display: grid;
		grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
		gap: 0.4rem;
	}

	.studio-search__options select {
		height: 1.75rem;
		min-width: 0;
		padding: 0 0.4rem;
		font-size: 0.75rem;
	}

	.studio-search__options button {
		display: grid;
		width: 1.9rem;
		height: 1.75rem;
		place-items: center;
		padding: 0;
	}

	.studio-search__results {
		min-height: 0;
		overflow: auto;
		padding-bottom: 0.5rem;
	}

	.studio-search__message {
		margin: 0;
		color: var(--studio-text-idle);
		padding: 0.25rem 0.85rem 0.4rem;
		font-size: 0.72rem;
	}

	.studio-search__message--error {
		color: var(--color-error-600-400);
	}

	.studio-search__group,
	.studio-search__result {
		display: flex;
		width: 100%;
		min-width: 0;
		align-items: center;
		gap: 0.35rem;
		border: 0;
		background: transparent;
		color: var(--studio-text);
		text-align: left;
	}

	.studio-search__group {
		padding: 0.2rem 0.5rem;
		font-size: 0.78rem;
		font-weight: 600;
	}

	.studio-search__group small {
		margin-left: auto;
		border-radius: 999px;
		background: var(--studio-selection-bg);
		padding: 0 0.4rem;
		font-size: 0.68rem;
	}

	.studio-search__result {
		padding: 0.18rem 0.5rem 0.18rem 1.6rem;
		font-size: 0.78rem;
	}

	.studio-search__group:hover,
	.studio-search__result:hover {
		background: var(--studio-hover-bg);
	}

	.studio-search__name {
		flex: 0 0 auto;
		white-space: nowrap;
	}

	.studio-search__path {
		min-width: 0;
		overflow: hidden;
		color: var(--studio-text-idle);
		font-size: 0.7rem;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-search__what {
		display: flex;
		gap: 0.2rem;
	}

	.studio-search__what-choice {
		flex: 1;
		border: 1px solid var(--studio-line);
		border-radius: 0.25rem;
		background: transparent;
		color: var(--studio-text-idle);
		padding: 0.15rem 0.4rem;
		font-size: 0.74rem;
	}

	.studio-search__what-choice--on {
		border-color: var(--color-primary-500);
		color: var(--studio-text-strong);
	}

	.studio-search__file {
		display: flex;
		min-width: 0;
		align-items: center;
		gap: 0.35rem;
		padding: 0.18rem 0.5rem 0.18rem 1.1rem;
		font-size: 0.76rem;
	}

	.studio-search__line {
		padding-left: 2.1rem;
		font-family: var(--font-mono, monospace);
		font-size: 0.72rem;
	}

	.studio-search__line-number {
		flex: 0 0 2rem;
		color: var(--studio-text-idle);
		text-align: right;
	}

	.studio-search__line-text {
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
</style>
