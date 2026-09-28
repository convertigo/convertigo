<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { call } from '$lib/utils/service';
	import { untrack } from 'svelte';
	import StudioEmptyState from './StudioEmptyState.svelte';

	/**
	 * @typedef {{ path: string, kind: 'added' | 'modified' | 'deleted' | 'untracked' | 'conflicting' }} ChangedFile
	 */

	/**
	 * @typedef {{
	 *  repository: boolean,
	 *  branch?: string,
	 *  head?: string,
	 *  remote?: string,
	 *  ahead?: number,
	 *  behind?: number,
	 *  prefix?: string,
	 *  staged?: ChangedFile[],
	 *  changes?: ChangedFile[]
	 * }} SourceControlStatus
	 */

	/**
	 * The source control of the selected project in its Git repository, as the Git staging view of the
	 * Eclipse Studio: its changed files and their differences, staging, discard, commit, pull, push and
	 * fetch, its branches and its history.
	 *
	 * @type {{
	 *  projectName?: string,
	 *  dirty?: boolean,
	 *  onPulled?: (projectName: string) => void | Promise<void>
	 * }}
	 */
	let { projectName = '', dirty = false, onPulled } = $props();

	const STATUS_LETTERS = {
		added: 'A',
		modified: 'M',
		deleted: 'D',
		untracked: 'U',
		conflicting: 'C'
	};

	let status = $state(/** @type {SourceControlStatus | null} */ (null));
	let message = $state('');
	let busy = $state('');
	let error = $state('');
	let diffPath = $state('');
	let diff = $state('');
	let branchesOpen = $state(false);
	/** @type {{ local: string[], remote: string[] }} */
	let branches = $state({ local: [], remote: [] });
	let historyOpen = $state(false);
	/** @type {{ id: string, subject: string, author: string, time: number }[]} */
	let commits = $state([]);

	let staged = $derived(status?.staged ?? []);
	let changes = $derived(status?.changes ?? []);

	$effect(() => {
		const name = projectName;
		untrack(() => {
			status = null;
			diffPath = '';
			if (name) {
				void run('status');
			}
		});
	});

	// a save writes the files of the project: their changes show again
	let wasDirty = false;
	$effect(() => {
		const isDirty = dirty;
		if (wasDirty && !isDirty) {
			untrack(() => projectName && void run('status'));
		}
		wasDirty = isDirty;
	});

	/**
	 * @param {string} action what reloads the project from its files
	 * @returns {boolean} whether the changes of the project not saved can be lost
	 */
	function confirmUnsaved(action) {
		return (
			!dirty ||
			window.confirm(
				`The project ${projectName} has changes not saved, which the ${action} loses as it loads the project again from its files.\n\nContinue?`
			)
		);
	}

	async function pull() {
		if (confirmUnsaved('pull')) {
			await run('pull');
		}
	}

	/**
	 * @param {string} action
	 * @param {Record<string, string>} [parameters]
	 */
	async function run(action, parameters = {}) {
		const name = projectName;
		busy = action;
		error = '';
		try {
			const head = status?.head;
			const result = await call('studio.git.SourceControl', {
				projectName: name,
				action,
				...parameters
			});
			if (name !== projectName) {
				return null;
			}
			if (result && 'repository' in result) {
				status = result;
				// the project loads again from its files a pull, a checkout or a discard changed
				if ((action === 'pull' && result.head !== head) || result.reloaded) {
					await onPulled?.(name);
				}
			} else {
				error = String(result?.error?.message ?? `The ${action} did not succeed.`);
			}
			return result;
		} finally {
			busy = '';
		}
	}

	/**
	 * @param {ChangedFile[]} files
	 * @param {'stage' | 'unstage'} action
	 */
	async function move(files, action) {
		await run(action, { paths: JSON.stringify(files.map((file) => file.path)) });
		if (diffPath && files.some((file) => file.path === diffPath)) {
			await showDiff(diffPath);
		}
	}

	async function toggleBranches() {
		branchesOpen = !branchesOpen;
		if (branchesOpen) {
			const result = await run('branches');
			branches = {
				local: Array.isArray(result?.local) ? result.local : [],
				remote: Array.isArray(result?.remoteBranches) ? result.remoteBranches : []
			};
		}
	}

	/**
	 * @param {string} branch
	 * @param {boolean} [create]
	 */
	async function checkout(branch, create = false) {
		// a new branch starts from the current commit, whose files do not change
		if (!create && !confirmUnsaved('checkout')) {
			return;
		}
		const result = await run('checkout', { branch, create: String(create) });
		if (result?.reloaded) {
			branchesOpen = false;
			diffPath = '';
			if (historyOpen) {
				await loadHistory();
			}
		}
	}

	async function newBranch() {
		const branch = window.prompt('Name of the new branch, from the current commit')?.trim();
		if (branch) {
			await checkout(branch, true);
		}
	}

	/**
	 * @param {ChangedFile[]} files
	 */
	async function discard(files) {
		const names = files.map((file) => fileName(file.path).name);
		if (
			!window.confirm(
				`Discard the changes of ${names.length > 1 ? `${names.length} files` : names[0]}?\n\nThe new files are removed, the others come back as the last commit has them.`
			) ||
			!confirmUnsaved('discard')
		) {
			return;
		}
		await run('discard', { paths: JSON.stringify(files.map((file) => file.path)) });
		if (diffPath && files.some((file) => file.path === diffPath)) {
			diffPath = '';
		}
	}

	async function loadHistory() {
		const result = await run('log');
		commits = Array.isArray(result?.commits) ? result.commits : [];
	}

	async function toggleHistory() {
		historyOpen = !historyOpen;
		if (historyOpen) {
			await loadHistory();
		}
	}

	async function commit() {
		if (!message.trim() || !staged.length) {
			return;
		}
		const result = await run('commit', { message: message.trim() });
		if (result?.commit) {
			message = '';
			diffPath = '';
		}
	}

	/**
	 * @param {string} path
	 */
	async function showDiff(path) {
		if (diffPath === path && diff) {
			diffPath = '';
			return;
		}
		diffPath = path;
		diff = '';
		const result = await call('studio.git.SourceControl', { projectName, action: 'diff', path });
		if (diffPath === path) {
			diff = String(result?.diff ?? result?.error?.message ?? '');
		}
	}

	/**
	 * @param {string} path
	 * @returns {{ name: string, dir: string }}
	 */
	function fileName(path) {
		const relative = path.slice(status?.prefix?.length ?? 0);
		const index = relative.lastIndexOf('/');
		return { name: relative.slice(index + 1), dir: index < 0 ? '' : relative.slice(0, index) };
	}

	/**
	 * @param {string} line
	 */
	function lineKind(line) {
		if (
			line.startsWith('+++') ||
			line.startsWith('---') ||
			line.startsWith('diff ') ||
			line.startsWith('index ')
		) {
			return 'meta';
		}
		return line.startsWith('+')
			? 'added'
			: line.startsWith('-')
				? 'removed'
				: line.startsWith('@@')
					? 'hunk'
					: '';
	}
</script>

{#snippet fileList(/** @type {ChangedFile[]} */ files, /** @type {'stage' | 'unstage'} */ action)}
	{#each files as file (file.path)}
		{@const parts = fileName(file.path)}
		<div class={['studio-git__file', diffPath === file.path && 'studio-git__file--open']}>
			<button
				type="button"
				class="studio-git__file-name"
				title={file.path}
				onclick={() => showDiff(file.path)}
			>
				<span>{parts.name}</span>
				<small>{parts.dir}</small>
			</button>
			<button
				type="button"
				class="studio-git__icon"
				title={action === 'stage' ? 'Stage the file' : 'Unstage the file'}
				aria-label={action === 'stage' ? `Stage ${parts.name}` : `Unstage ${parts.name}`}
				disabled={Boolean(busy)}
				onclick={() => move([file], action)}
				><Ico icon={action === 'stage' ? 'mdi:plus' : 'mdi:minus'} size={4} /></button
			>
			{#if action === 'stage'}
				<button
					type="button"
					class="studio-git__icon"
					title="Discard the changes"
					aria-label={`Discard ${parts.name}`}
					disabled={Boolean(busy)}
					onclick={() => discard([file])}><Ico icon="mdi:undo" size={4} /></button
				>
			{/if}
			<span class={['studio-git__status', `studio-git__status--${file.kind}`]} title={file.kind}
				>{STATUS_LETTERS[file.kind] ?? '?'}</span
			>
		</div>
		{#if diffPath === file.path}
			<pre class="studio-git__diff">{#if diff}{#each diff.split('\n') as line, index (index)}<span
							class={['studio-git__line', lineKind(line) && `studio-git__line--${lineKind(line)}`]}
							>{line}{'\n'}</span
						>{/each}{:else}Reading the differences…{/if}</pre>
		{/if}
	{/each}
{/snippet}

<div class="studio-git">
	{#if !projectName}
		<StudioEmptyState message="Select a project to see its changes" small />
	{:else if !status}
		{#if error}
			<p class="studio-git__message studio-git__message--error">{error}</p>
		{:else}
			<StudioEmptyState message="Reading the repository" loading small />
		{/if}
	{:else if !status.repository}
		<div class="studio-git__init">
			<p class="studio-git__message">The project {projectName} is not in a Git repository.</p>
			<button
				type="button"
				class="button-primary"
				disabled={Boolean(busy)}
				onclick={() => run('init')}
			>
				<Ico icon="mdi:source-branch" size={4} /> Initialize a repository
			</button>
		</div>
	{:else}
		<div class="studio-git__head">
			<button
				type="button"
				class="studio-git__branch"
				title={`${status.remote || 'No remote'}: switch or create a branch`}
				aria-expanded={branchesOpen}
				disabled={Boolean(busy)}
				onclick={toggleBranches}
			>
				<Ico icon="mdi:source-branch" size={4} />
				{status.branch}
				{#if status.ahead}<small title="Commits to push">↑{status.ahead}</small>{/if}
				{#if status.behind}<small title="Commits to pull">↓{status.behind}</small>{/if}
			</button>
			<button
				type="button"
				class="studio-git__icon"
				title="Fetch"
				aria-label="Fetch"
				disabled={Boolean(busy) || !status.remote}
				onclick={() => run('fetch')}><Ico icon="mdi:cloud-download-outline" size={4} /></button
			>
			<button
				type="button"
				class="studio-git__icon"
				title="Refresh"
				aria-label="Refresh"
				disabled={Boolean(busy)}
				onclick={() => run('status')}><Ico icon="mdi:reload" size={4} /></button
			>
			<button
				type="button"
				class="studio-git__icon"
				title="Pull"
				aria-label="Pull"
				disabled={Boolean(busy) || !status.remote}
				onclick={() => pull()}><Ico icon="mdi:arrow-down" size={4} /></button
			>
			<button
				type="button"
				class="studio-git__icon"
				title="Push"
				aria-label="Push"
				disabled={Boolean(busy) || !status.remote}
				onclick={() => run('push')}><Ico icon="mdi:arrow-up" size={4} /></button
			>
		</div>
		{#if branchesOpen}
			<div class="studio-git__branches" role="listbox" aria-label="Branches">
				<button type="button" class="studio-git__branch-item" onclick={newBranch}>
					<Ico icon="mdi:plus" size={4} /> New branch…
				</button>
				{#each branches.local as branch (branch)}
					<button
						type="button"
						role="option"
						aria-selected={branch === status.branch}
						class={[
							'studio-git__branch-item',
							branch === status.branch && 'studio-git__branch-item--current'
						]}
						disabled={branch === status.branch || Boolean(busy)}
						onclick={() => checkout(branch)}
					>
						<Ico icon="mdi:source-branch" size={4} />
						{branch}
					</button>
				{/each}
				{#each branches.remote as branch (branch)}
					<button
						type="button"
						role="option"
						aria-selected="false"
						class="studio-git__branch-item studio-git__branch-item--remote"
						disabled={Boolean(busy)}
						onclick={() => checkout(branch)}
					>
						<Ico icon="mdi:cloud-outline" size={4} />
						{branch}
					</button>
				{/each}
			</div>
		{/if}
		<form
			class="studio-git__commit"
			onsubmit={(event) => {
				event.preventDefault();
				void commit();
			}}
		>
			<textarea
				class="input-common"
				aria-label="Commit message"
				placeholder="Message of the commit"
				bind:value={message}
				disabled={Boolean(busy)}></textarea>
			<button
				type="submit"
				class="button-primary"
				disabled={Boolean(busy) || !message.trim() || !staged.length}
			>
				<Ico icon={busy === 'commit' ? 'mdi:sync' : 'mdi:check'} size={4} /> Commit
			</button>
			{#if dirty}
				<small class="studio-git__hint">Save the project to commit its last changes.</small>
			{/if}
		</form>
		{#if error}
			<p class="studio-git__message studio-git__message--error">{error}</p>
		{/if}
		<div class="studio-git__lists">
			<div class="studio-git__section">
				<span>Staged changes</span>
				<small>{staged.length}</small>
				{#if staged.length}
					<button
						type="button"
						class="studio-git__icon"
						title="Unstage all"
						aria-label="Unstage all"
						disabled={Boolean(busy)}
						onclick={() => move(staged, 'unstage')}><Ico icon="mdi:minus" size={4} /></button
					>
				{/if}
			</div>
			{@render fileList(staged, 'unstage')}
			<div class="studio-git__section">
				<span>Changes</span>
				<small>{changes.length}</small>
				{#if changes.length}
					<button
						type="button"
						class="studio-git__icon"
						title="Stage all"
						aria-label="Stage all"
						disabled={Boolean(busy)}
						onclick={() => move(changes, 'stage')}><Ico icon="mdi:plus" size={4} /></button
					>
					<button
						type="button"
						class="studio-git__icon"
						title="Discard all the changes"
						aria-label="Discard all the changes"
						disabled={Boolean(busy)}
						onclick={() => discard(changes)}><Ico icon="mdi:undo" size={4} /></button
					>
				{/if}
			</div>
			{@render fileList(changes, 'stage')}
			{#if !staged.length && !changes.length}
				<p class="studio-git__message">No change since the last commit.</p>
			{/if}
			<button
				type="button"
				class="studio-git__section studio-git__history-toggle"
				aria-expanded={historyOpen}
				onclick={toggleHistory}
			>
				<Ico icon={historyOpen ? 'mdi:chevron-down' : 'mdi:chevron-right'} size={4} />
				<span>History</span>
			</button>
			{#if historyOpen}
				{#each commits as entry (entry.id)}
					<div class="studio-git__commit-entry" title={`${entry.id} ${entry.author}`}>
						<code>{entry.id}</code>
						<span>{entry.subject}</span>
						<small>{entry.author} · {new Date(entry.time).toLocaleDateString()}</small>
					</div>
				{:else}
					<p class="studio-git__message">No commit yet.</p>
				{/each}
			{/if}
		</div>
	{/if}
</div>

<style>
	.studio-git {
		display: grid;
		min-height: 0;
		height: 100%;
		grid-template-rows: auto auto auto minmax(0, 1fr);
		font-size: 0.78rem;
	}

	.studio-git__init {
		display: grid;
		gap: 0.5rem;
		justify-items: start;
		padding: 0.5rem 0.85rem;
	}

	.studio-git__head {
		display: flex;
		align-items: center;
		gap: 0.15rem;
		padding: 0.35rem 0.5rem 0.2rem 0.85rem;
	}

	.studio-git__branch {
		display: flex;
		min-width: 0;
		flex: 1;
		align-items: center;
		gap: 0.3rem;
		border: 0;
		border-radius: 0.25rem;
		background: transparent;
		color: var(--studio-text-strong);
		padding: 0.1rem 0.25rem;
		font-weight: 600;
		text-align: left;
	}

	.studio-git__branch:hover:not(:disabled) {
		background: var(--studio-hover-bg);
	}

	.studio-git__branches {
		display: grid;
		max-height: 14rem;
		overflow-y: auto;
		border-bottom: 1px solid var(--studio-line);
		padding: 0.2rem 0.4rem;
	}

	.studio-git__branch-item {
		display: flex;
		align-items: center;
		gap: 0.35rem;
		border: 0;
		border-radius: 0.25rem;
		background: transparent;
		color: var(--studio-text);
		padding: 0.2rem 0.4rem;
		text-align: left;
	}

	.studio-git__branch-item:hover:not(:disabled) {
		background: var(--studio-hover-bg);
	}

	.studio-git__branch-item--current {
		color: var(--studio-text-strong);
		font-weight: 600;
	}

	.studio-git__branch-item--remote {
		color: var(--studio-text-idle);
	}

	.studio-git__history-toggle {
		width: 100%;
		border: 0;
		background: transparent;
		text-align: left;
	}

	.studio-git__commit-entry {
		display: grid;
		grid-template-columns: auto minmax(0, 1fr);
		column-gap: 0.4rem;
		padding: 0.2rem 0.5rem 0.2rem 1.1rem;
	}

	.studio-git__commit-entry code {
		color: var(--studio-text-idle);
		font-size: 0.7rem;
	}

	.studio-git__commit-entry span {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-git__commit-entry small {
		grid-column: 2;
		color: var(--studio-text-idle);
		font-size: 0.68rem;
	}

	.studio-git__branch small {
		color: var(--studio-text-idle);
		font-weight: 500;
	}

	.studio-git__icon {
		display: inline-grid;
		width: 1.5rem;
		height: 1.5rem;
		flex: none;
		place-items: center;
		border: 0;
		border-radius: 0.25rem;
		background: transparent;
		color: var(--studio-text-idle);
		padding: 0;
	}

	.studio-git__icon:hover:not(:disabled) {
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

	.studio-git__icon:disabled {
		opacity: 0.4;
	}

	.studio-git__commit {
		display: grid;
		gap: 0.35rem;
		padding: 0.25rem 0.85rem 0.5rem;
	}

	.studio-git__commit textarea {
		min-height: 3.2rem;
		padding: 0.4rem 0.55rem;
		font-size: 0.78rem;
		resize: vertical;
	}

	.studio-git__commit button {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		gap: 0.35rem;
	}

	.studio-git__hint,
	.studio-git__message {
		margin: 0;
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-git__message {
		padding: 0.25rem 0.85rem;
	}

	.studio-git__message--error {
		color: var(--color-error-600-400);
	}

	.studio-git__lists {
		min-height: 0;
		overflow: auto;
		padding-bottom: 0.5rem;
	}

	.studio-git__section {
		display: flex;
		align-items: center;
		gap: 0.35rem;
		padding: 0.3rem 0.5rem 0.2rem 0.85rem;
		color: var(--studio-text-idle);
		font-size: 0.7rem;
		font-weight: 600;
		letter-spacing: 0.04em;
		text-transform: uppercase;
	}

	.studio-git__section small {
		margin-right: auto;
		border-radius: 999px;
		background: var(--studio-selection-bg);
		padding: 0 0.4rem;
		font-size: 0.68rem;
		letter-spacing: 0;
	}

	.studio-git__file {
		display: flex;
		align-items: center;
		gap: 0.15rem;
		padding: 0 0.5rem 0 1rem;
	}

	.studio-git__file:hover,
	.studio-git__file--open {
		background: var(--studio-hover-bg);
	}

	.studio-git__file-name {
		display: flex;
		min-width: 0;
		flex: 1;
		align-items: baseline;
		gap: 0.4rem;
		border: 0;
		background: transparent;
		color: var(--studio-text);
		padding: 0.18rem 0;
		text-align: left;
	}

	.studio-git__file-name span {
		white-space: nowrap;
	}

	.studio-git__file-name small {
		min-width: 0;
		overflow: hidden;
		color: var(--studio-text-idle);
		font-size: 0.7rem;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-git__status {
		width: 1rem;
		flex: none;
		font-family: var(--font-mono, ui-monospace, monospace);
		font-size: 0.72rem;
		font-weight: 700;
		text-align: center;
	}

	.studio-git__status--added,
	.studio-git__status--untracked {
		color: var(--color-success-600-400);
	}

	.studio-git__status--modified {
		color: var(--color-warning-600-400);
	}

	.studio-git__status--deleted,
	.studio-git__status--conflicting {
		color: var(--color-error-600-400);
	}

	.studio-git__diff {
		max-height: 18rem;
		overflow: auto;
		margin: 0.15rem 0.5rem 0.4rem 1rem;
		border: 1px solid var(--studio-line);
		border-radius: 0.3rem;
		background: var(--studio-chrome-bg);
		padding: 0.35rem 0;
		font-family: var(--font-mono, ui-monospace, monospace);
		font-size: 0.7rem;
		line-height: 1.45;
	}

	.studio-git__line {
		display: block;
		padding: 0 0.5rem;
		white-space: pre;
	}

	.studio-git__line--added {
		background: color-mix(in oklab, var(--color-success-500) 16%, transparent);
	}

	.studio-git__line--removed {
		background: color-mix(in oklab, var(--color-error-500) 16%, transparent);
	}

	.studio-git__line--hunk {
		color: var(--color-primary-600-400);
	}

	.studio-git__line--meta {
		color: var(--studio-text-idle);
	}
</style>
