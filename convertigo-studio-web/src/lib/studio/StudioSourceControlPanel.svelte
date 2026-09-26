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
	 * Eclipse Studio: its changed files and their differences, staging, commit, pull and push.
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
				if (action === 'pull' && result.head !== head) {
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
			<span class="studio-git__branch" title={status.remote || 'No remote'}>
				<Ico icon="mdi:source-branch" size={4} />
				{status.branch}
				{#if status.ahead}<small title="Commits to push">↑{status.ahead}</small>{/if}
				{#if status.behind}<small title="Commits to pull">↓{status.behind}</small>{/if}
			</span>
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
				onclick={() => run('pull')}><Ico icon="mdi:arrow-down" size={4} /></button
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
				{/if}
			</div>
			{@render fileList(changes, 'stage')}
			{#if !staged.length && !changes.length}
				<p class="studio-git__message">No change since the last commit.</p>
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
		color: var(--studio-text-strong);
		font-weight: 600;
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
