<script>
	import Ico from '#lib/utils/Ico.svelte';
	import { call, toaster } from '#lib/utils/service.js';
	import { untrack } from 'svelte';
	import StudioCredentialsDialog from './StudioCredentialsDialog.svelte';
	import StudioEmptyState from './StudioEmptyState.svelte';
	import { studioPrompt } from './studioPrompt.svelte.js';
	import StudioRebaseDialog from './StudioRebaseDialog.svelte';
	import { setTreeDiffEnabled, setTreeDiffRef } from './treeDiff.svelte.js';
	import {
		describeOperation,
		gitEvents,
		notifyGitChange,
		tellMergedObjects
	} from './treeMerge.svelte.js';

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
	 *  changes?: ChangedFile[],
	 *  operation?: import('./treeMerge.svelte.js').GitOperation,
	 *  pullRebase?: boolean,
	 *  detached?: boolean,
	 *  stashCount?: number,
	 *  remoteName?: string,
	 *  remoteCount?: number,
	 *  upstream?: string
	 * }} SourceControlStatus
	 */

	/**
	 * The source control of the selected project in its Git repository, as the Git staging view of the
	 * Eclipse Studio: its changed files and their differences, staging, discard, commit, pull, push and
	 * fetch, merge, rebase, cherry-pick and revert, the operation stopped to continue, skip or abort, its
	 * branches and its history.
	 *
	 * @type {{
	 *  projectName?: string,
	 *  dirty?: boolean,
	 *  onPulled?: (projectName: string, reloaded?: string[]) => void | Promise<void>,
	 *  onMerging?: (projectName: string) => void | Promise<void>,
	 *  onShowTree?: () => void
	 * }}
	 */
	let { projectName = '', dirty = false, onPulled, onMerging, onShowTree } = $props();

	/** the actions that start an operation of Git or go on with it, which may stop */
	const STARTING = new Set([
		'pull',
		'merge',
		'rebase',
		'rebaseInteractive',
		'cherryPick',
		'revert',
		'continue',
		'skip',
		'stashApply',
		'stashPop'
	]);

	/** the actions that change the repository, of which the other views of Git are told */
	const CHANGING = new Set([
		'init',
		'stage',
		'unstage',
		'discard',
		'commit',
		'pull',
		'merge',
		'rebase',
		'cherryPick',
		'revert',
		'continue',
		'skip',
		'abort',
		'checkout',
		'fetch',
		'push',
		'reset',
		'createBranch',
		'deleteBranch',
		'renameBranch',
		'tag',
		'deleteTag',
		'stash',
		'stashApply',
		'stashPop',
		'stashDrop',
		'addRemote',
		'setRemoteUrl',
		'removeRemote',
		'deleteRemoteBranch',
		'rebaseInteractive'
	]);

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
	/** the branch whose history shows, HEAD when empty */
	let historyRef = $state('');
	/** @type {{ id: string, subject: string, author: string, time: number, inHead?: boolean, merge?: boolean, refs?: string[] }[]} */
	let commits = $state([]);
	/** the commit whose details show, its files changed and the differences of one of them */
	let commitOpen = $state('');
	/** @type {{ commit?: any, files: ChangedFile[] } | null} */
	let commitDetails = $state(null);
	let commitDiffPath = $state('');
	let commitDiff = $state('');
	/** @type {{ name: string, commit: string, annotation?: string }[]} */
	let tags = $state([]);
	let amend = $state(false);
	let stashesOpen = $state(false);
	/** @type {{ index: number, id: string, subject: string, time: number }[]} */
	let stashes = $state([]);
	/** @type {{ name: string, url: string, pushUrl?: string }[]} */
	let remotes = $state([]);
	/** the address of the remote whose credentials are asked, and the answer awaited */
	let credentialsUrl = $state(/** @type {string | null} */ (null));
	/** @type {((credentials: { username: string, password: string } | null) => void) | null} */
	let credentialsAnswer = null;

	/**
	 * @param {string} url
	 * @returns {Promise<{ username: string, password: string } | null>}
	 */
	function askCredentials(url) {
		credentialsAnswer?.(null);
		credentialsUrl = url;
		return new Promise((resolve) => {
			credentialsAnswer = (credentials) => {
				credentialsUrl = null;
				credentialsAnswer = null;
				resolve(credentials);
			};
		});
	}

	/** the commit an interactive rebase replays the commits after */
	let rebaseFrom = $state(/** @type {{ id: string, subject: string } | null} */ (null));
	/** the menu open: pull, or the id of a commit */
	let menu = $state('');

	let staged = $derived(status?.staged ?? []);
	let changes = $derived(status?.changes ?? []);
	let operation = $derived(status?.operation ?? null);
	let described = $derived(describeOperation(operation));

	// the other views of Git changed the repository: the status shows again, with the message of the
	// commit that ends a merge
	let seenSerial = gitEvents.serial;
	$effect(() => {
		const serial = gitEvents.serial;
		untrack(() => {
			if (serial === seenSerial) {
				return;
			}
			seenSerial = serial;
			const pending = gitEvents.messages[projectName];
			if (pending) {
				if (!message.trim()) {
					message = pending;
				}
				delete gitEvents.messages[projectName];
			}
			if (projectName) {
				void run('status');
				if (historyOpen) {
					void loadHistory();
				}
			}
		});
	});

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

	/**
	 * @param {'merge' | 'rebase'} [mode] as the branch is configured, when not given
	 */
	async function pull(mode) {
		menu = '';
		if (confirmUnsaved('pull')) {
			await run('pull', mode ? { mode } : {});
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
			if (result?.authRequired && result.remoteUrl) {
				// the remote refused the operation without credentials: asked, the operation is done again
				status = result;
				const credentials = await askCredentials(result.remoteUrl);
				if (credentials) {
					busy = '';
					return await run(action, {
						...parameters,
						authRemote: result.authRemote,
						...credentials
					});
				}
				return null;
			}
			if (result && 'repository' in result) {
				status = result;
				tellMergedObjects(result);
				const stopped = result.operation;
				if (
					stopped &&
					!message.trim() &&
					stopped.canContinue &&
					stopped.commitMessage &&
					stopped.kind !== 'rebase'
				) {
					// the commit that ends the operation, with the message of Git
					message = stopped.commitMessage;
				}
				const reloaded = Array.isArray(result.reloadedProjects) ? result.reloadedProjects : [];
				if ((action === 'pull' && result.head !== head) || result.reloaded || reloaded.length) {
					// the projects load again from their files the operation changed
					await onPulled?.(name, reloaded);
				}
				if (result.merging) {
					// stopped on conflicts: the tree merges the objects, the files Git filled with its markers
					// are not loaded
					const what = describeOperation(stopped);
					toaster.warning({
						title: `${what.title}: conflicts`,
						description: `Resolve the conflicts of ${name} in the Projects tree, then ${what.complete.toLowerCase()}.`
					});
					await onMerging?.(name);
				} else if (stopped && STARTING.has(action)) {
					const what = describeOperation(stopped);
					toaster.info({
						title: `${what.title}: stopped`,
						description:
							result.result === 'NOTHING_TO_COMMIT'
								? 'The commit has no change once resolved: skip it.'
								: result.result === 'EDIT'
									? 'Change the project and commit, then continue.'
									: stopped.conflicts
										? `${stopped.conflicts} file${stopped.conflicts > 1 ? 's' : ''} of other projects in conflict.`
										: 'Continue, skip or abort it.'
					});
				}
				if (CHANGING.has(action)) {
					seenSerial = gitEvents.serial + 1;
					notifyGitChange(name);
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

	/**
	 * Merges a branch into the current one, as a pull merges the remote one.
	 * @param {string} branch
	 */
	async function mergeBranch(branch) {
		if (!confirmUnsaved('merge')) {
			return;
		}
		const result = await run('merge', { branch });
		if (result && 'repository' in result) {
			branchesOpen = false;
		}
	}

	/**
	 * Replays the commits of the current branch on another one.
	 * @param {string} branch
	 */
	async function rebaseOnto(branch) {
		if (
			!window.confirm(
				`Rebase ${status?.branch} onto ${branch}?\n\nIts commits not in ${branch} are replayed on it, one by one.`
			) ||
			!confirmUnsaved('rebase')
		) {
			return;
		}
		const result = await run('rebase', { branch });
		if (result && 'repository' in result) {
			branchesOpen = false;
		}
	}

	/**
	 * Continues, skips or aborts the operation stopped.
	 * @param {'continue' | 'skip' | 'abort'} action
	 */
	async function endOperation(action) {
		// named as it is before it ends
		const what = described;
		if (
			action === 'abort' &&
			!window.confirm(
				`Abort: ${what.title}?\n\nThe repository and its files are given back as they were before.`
			)
		) {
			return;
		}
		if (
			action === 'skip' &&
			!window.confirm(
				`Skip the commit ${operation?.commit?.id ?? ''}? The rebased branch will not have its changes.`
			)
		) {
			return;
		}
		if (action !== 'continue' && !confirmUnsaved(action)) {
			return;
		}
		const result = await run(
			action,
			action === 'continue' && message.trim() ? { message: message.trim() } : {}
		);
		if (result && 'repository' in result && !result.operation) {
			message = '';
			toaster[result.result === 'EMPTY' ? 'info' : 'success']({
				title:
					action === 'abort'
						? 'Aborted'
						: result.result === 'EMPTY'
							? 'Nothing left to commit'
							: what.ended,
				description:
					action === 'abort'
						? `${what.title} is abandoned.`
						: result.result === 'EMPTY'
							? 'The commit changes nothing once resolved: it is dropped.'
							: ''
			});
		}
		if (historyOpen) {
			await loadHistory();
		}
	}

	/**
	 * Applies a commit of another branch on the current one, or undoes the changes of a commit by a new one.
	 * @param {'cherryPick' | 'revert'} action
	 * @param {{ id: string, subject: string }} entry
	 */
	async function applyCommit(action, entry) {
		menu = '';
		const what =
			action === 'cherryPick'
				? `Cherry-pick ${entry.id} "${entry.subject}" onto ${status?.branch}?`
				: `Revert ${entry.id} "${entry.subject}"?\n\nA new commit undoes its changes.`;
		if (
			!window.confirm(what) ||
			!confirmUnsaved(action === 'cherryPick' ? 'cherry-pick' : 'revert')
		) {
			return;
		}
		await run(action, { commit: entry.id });
		if (historyOpen) {
			await loadHistory();
		}
	}

	/**
	 * Shows or hides the details of a commit: its message, its author and the files of the project it changed.
	 * @param {string} id
	 */
	async function toggleCommit(id) {
		menu = '';
		commitDiffPath = '';
		if (commitOpen === id) {
			commitOpen = '';
			return;
		}
		commitOpen = id;
		commitDetails = null;
		const result = await call('studio.git.SourceControl', {
			projectName,
			action: 'show',
			commit: id
		});
		if (commitOpen === id) {
			commitDetails = {
				commit: result?.commit,
				files: Array.isArray(result?.files) ? result.files : []
			};
		}
	}

	/**
	 * @param {string} id the commit
	 * @param {string} path a file it changed
	 */
	async function showCommitDiff(id, path) {
		if (commitDiffPath === path) {
			commitDiffPath = '';
			return;
		}
		commitDiffPath = path;
		commitDiff = '';
		const result = await call('studio.git.SourceControl', {
			projectName,
			action: 'show',
			commit: id,
			path
		});
		if (commitDiffPath === path) {
			commitDiff = String(result?.diff ?? '');
		}
	}

	/**
	 * The actions of the history on a commit: checked out, a branch or a tag at it, the current branch reset
	 * to it.
	 * @param {'checkout' | 'branch' | 'tag' | 'soft' | 'mixed' | 'hard'} what
	 * @param {{ id: string, subject: string }} entry
	 */
	async function commitAction(what, entry) {
		menu = '';
		if (what === 'checkout') {
			if (
				window.confirm(
					`Check out ${entry.id} "${entry.subject}"?\n\nHEAD is detached at this commit: create a branch to commit on it.`
				) &&
				confirmUnsaved('checkout')
			) {
				await run('checkout', { branch: entry.id });
			}
		} else if (what === 'branch') {
			const branch = (await studioPrompt(`Name of the new branch, at ${entry.id}`))?.trim();
			if (branch && confirmUnsaved('checkout')) {
				await run('createBranch', { branch, commit: entry.id });
			}
		} else if (what === 'tag') {
			const name = (await studioPrompt(`Name of the tag, at ${entry.id}`))?.trim();
			if (name) {
				const annotation = await studioPrompt('Message of the tag, empty for a lightweight tag');
				if (annotation !== null) {
					await run('tag', { name, commit: entry.id, annotation: annotation ?? '' });
				}
			}
		} else {
			const detail = {
				soft: 'The changes since it stay staged.',
				mixed: 'The changes since it stay in the files, not staged.',
				hard: 'The changes since it, and those not committed, are lost.'
			}[what];
			if (
				window.confirm(`Reset ${status?.branch} to ${entry.id} "${entry.subject}"?\n\n${detail}`) &&
				(what !== 'hard' || confirmUnsaved('reset'))
			) {
				await run('reset', { commit: entry.id, mode: what });
			}
		}
		if (historyOpen) {
			await loadHistory();
		}
	}

	/**
	 * @param {string} branch
	 */
	async function renameBranch(branch) {
		const name = (await studioPrompt(`New name of the branch ${branch}`, branch))?.trim();
		if (name && name !== branch) {
			await run('renameBranch', { branch, name });
			await loadBranches();
		}
	}

	/**
	 * @param {string} branch
	 */
	async function deleteBranch(branch) {
		if (!window.confirm(`Delete the branch ${branch}?`)) {
			return;
		}
		const result = await run('deleteBranch', { branch });
		if (
			result?.notMerged &&
			window.confirm(
				`The branch ${branch} has commits merged nowhere, which are lost with it.\n\nDelete it anyway?`
			)
		) {
			await run('deleteBranch', { branch, force: 'true' });
		}
		await loadBranches();
	}

	/**
	 * @param {string} name
	 */
	async function deleteTag(name) {
		if (window.confirm(`Delete the tag ${name}?`)) {
			await run('deleteTag', { name });
			await loadBranches();
		}
	}

	async function loadBranches() {
		const result = await run('branches');
		branches = {
			local: Array.isArray(result?.local) ? result.local : [],
			remote: Array.isArray(result?.remoteBranches) ? result.remoteBranches : []
		};
		const tagged = await call('studio.git.SourceControl', { projectName, action: 'tags' });
		tags = Array.isArray(tagged?.tags) ? tagged.tags : [];
		const listed = await call('studio.git.SourceControl', { projectName, action: 'remotes' });
		remotes = Array.isArray(listed?.remotes) ? listed.remotes : [];
	}

	/**
	 * Pushes the current branch: to the branch of the remote it follows, else published to a remote, which it
	 * then follows.
	 * @param {{ tags?: boolean, force?: boolean, choose?: boolean }} [options]
	 */
	async function push(options = {}) {
		menu = '';
		/** @type {Record<string, string>} */
		const parameters = {};
		if (options.choose) {
			const remote = (
				await studioPrompt(
					`Remote to publish ${status?.branch} to: ${remotes.map((r) => r.name).join(', ') || 'origin'}`,
					status?.remoteName ?? 'origin'
				)
			)?.trim();
			if (!remote) {
				return;
			}
			parameters.remote = remote;
			parameters.publish = 'true';
		}
		if (options.tags) {
			parameters.tags = 'true';
		}
		if (options.force) {
			if (
				!window.confirm(
					`Force the push of ${status?.branch} to ${status?.upstream ?? status?.remoteName}?\n\nThe commits of the remote not in ${status?.branch} are lost on it. The push is refused when the remote changed since the last fetch.`
				)
			) {
				return;
			}
			parameters.force = 'true';
		}
		await run('push', parameters);
	}

	async function addRemote() {
		const name = (await studioPrompt('Name of the remote', remotes.length ? '' : 'origin'))?.trim();
		if (!name) {
			return;
		}
		const url = (await studioPrompt(`Address of the remote ${name}`))?.trim();
		if (url) {
			const result = await run('addRemote', { name, url });
			if (result && 'repository' in result) {
				await run('fetch', { remote: name });
			}
			await loadBranches();
		}
	}

	/**
	 * @param {{ name: string, url: string }} remote
	 */
	async function editRemote(remote) {
		const url = (await studioPrompt(`Address of the remote ${remote.name}`, remote.url))?.trim();
		if (url && url !== remote.url) {
			await run('setRemoteUrl', { name: remote.name, url });
			await loadBranches();
		}
	}

	/**
	 * @param {{ name: string }} remote
	 */
	async function removeRemote(remote) {
		if (
			window.confirm(
				`Remove the remote ${remote.name}?\n\nIts branches are no longer followed; the remote itself does not change.`
			)
		) {
			await run('removeRemote', { name: remote.name });
			await loadBranches();
		}
	}

	/**
	 * @param {string} branch a branch of a remote, as origin/feature
	 */
	async function deleteRemoteBranch(branch) {
		const slash = branch.indexOf('/');
		if (
			window.confirm(
				`Delete the branch ${branch.slice(slash + 1)} from the remote ${branch.slice(0, slash)}?\n\nThe deletion is pushed to the remote, for all who use it.`
			)
		) {
			await run('deleteRemoteBranch', { branch });
			await loadBranches();
		}
	}

	async function loadStashes() {
		const result = await run('stashes');
		stashes = Array.isArray(result?.stashes) ? result.stashes : [];
	}

	async function toggleStashes() {
		stashesOpen = !stashesOpen;
		if (stashesOpen) {
			await loadStashes();
		}
	}

	/**
	 * Sets the changes aside, the files given back as the last commit has them.
	 */
	async function stashChanges() {
		const stashMessage = await studioPrompt('Message of the stash, empty for the default one');
		if (stashMessage === null || !confirmUnsaved('stash')) {
			return;
		}
		const untracked =
			changes.some((file) => file.kind === 'untracked') &&
			window.confirm('Stash the new files too?\n\nCancel to leave them in the files.');
		const result = await run('stash', {
			message: stashMessage ?? '',
			untracked: String(untracked)
		});
		if (result && 'repository' in result) {
			stashesOpen = true;
			await loadStashes();
		}
	}

	/**
	 * @param {'stashApply' | 'stashPop' | 'stashDrop'} action
	 * @param {{ index: number, subject: string }} stash
	 */
	async function stashAction(action, stash) {
		if (action === 'stashDrop') {
			if (
				!window.confirm(`Drop stash@{${stash.index}} "${stash.subject}"? Its changes are lost.`)
			) {
				return;
			}
		} else if (!confirmUnsaved(action === 'stashPop' ? 'pop' : 'apply')) {
			return;
		}
		const result = await run(action, { index: String(stash.index) });
		if (result?.merging && action === 'stashPop') {
			toaster.info({
				title: 'The stash is kept',
				description: 'Its changes have conflicts: drop it once they are resolved.'
			});
		}
		await loadStashes();
	}

	/**
	 * Replays the commits after a commit with the actions chosen in the dialog of the interactive rebase.
	 * @param {{ id: string, action: string, message?: string }[]} steps
	 */
	async function rebaseInteractively(steps) {
		const base = rebaseFrom;
		if (!base || !confirmUnsaved('rebase')) {
			return;
		}
		rebaseFrom = null;
		await run('rebaseInteractive', { upstream: base.id, steps: JSON.stringify(steps) });
		if (historyOpen) {
			await loadHistory();
		}
	}

	/**
	 * Shows in the Git mode of the tree the changes since a commit.
	 * @param {string} id
	 */
	function compareWith(id) {
		menu = '';
		setTreeDiffEnabled(true);
		setTreeDiffRef(id);
		onShowTree?.();
	}

	async function toggleBranches() {
		branchesOpen = !branchesOpen;
		if (branchesOpen) {
			await loadBranches();
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
		if (result && 'repository' in result) {
			branchesOpen = false;
			diffPath = '';
			if (historyOpen) {
				await loadHistory();
			}
		}
	}

	async function newBranch() {
		const branch = (await studioPrompt('Name of the new branch, from the current commit'))?.trim();
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
		const result = await run('log', historyRef ? { ref: historyRef } : {});
		commits = Array.isArray(result?.commits) ? result.commits : [];
	}

	async function toggleHistory() {
		historyOpen = !historyOpen;
		if (historyOpen) {
			if (!branches.local.length) {
				const result = await run('branches');
				branches = {
					local: Array.isArray(result?.local) ? result.local : [],
					remote: Array.isArray(result?.remoteBranches) ? result.remoteBranches : []
				};
			}
			await loadHistory();
		}
	}

	async function commit() {
		if (!message.trim() || (!staged.length && !amend)) {
			return;
		}
		const result = await run('commit', {
			message: message.trim(),
			...(amend ? { amend: 'true' } : {})
		});
		if (result?.commit) {
			message = '';
			diffPath = '';
			amend = false;
			if (historyOpen) {
				await loadHistory();
			}
		}
	}

	/**
	 * Amends the last commit: its message is proposed.
	 */
	async function toggleAmend() {
		amend = !amend;
		if (amend && !message.trim()) {
			const result = await call('studio.git.SourceControl', {
				projectName,
				action: 'show',
				commit: 'HEAD'
			});
			if (amend && !message.trim()) {
				message = String(result?.commit?.body ?? '').trim();
			}
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

{#snippet commitDetailsBlock(/** @type {string} */ id)}
	<div class="studio-git__commit-details">
		{#if !commitDetails}
			<small>Reading the commit…</small>
		{:else}
			<pre class="studio-git__commit-body">{commitDetails.commit?.body ?? ''}</pre>
			<small
				>{commitDetails.commit?.author}{commitDetails.commit?.email
					? ` <${commitDetails.commit.email}>`
					: ''} · {new Date(commitDetails.commit?.time ?? 0).toLocaleString()}{commitDetails.commit
					?.parents?.length
					? ` · parent ${commitDetails.commit.parents.join(', ')}`
					: ''}</small
			>
			{#each commitDetails.files as file (file.path)}
				{@const parts = fileName(file.path)}
				<div class={['studio-git__file', commitDiffPath === file.path && 'studio-git__file--open']}>
					<button
						type="button"
						class="studio-git__file-name"
						title={file.path}
						onclick={() => showCommitDiff(id, file.path)}
					>
						<span>{parts.name}</span>
						<small>{parts.dir}</small>
					</button>
					<span class={['studio-git__status', `studio-git__status--${file.kind}`]} title={file.kind}
						>{STATUS_LETTERS[file.kind] ?? 'R'}</span
					>
				</div>
				{#if commitDiffPath === file.path}
					<pre
						class="studio-git__diff">{#if commitDiff}{#each commitDiff.split('\n') as line, index (index)}<span
									class={[
										'studio-git__line',
										lineKind(line) && `studio-git__line--${lineKind(line)}`
									]}>{line}{'\n'}</span
								>{/each}{:else}Reading the differences…{/if}</pre>
				{/if}
			{:else}
				<small>No file of the project changed.</small>
			{/each}
		{/if}
	</div>
{/snippet}

<svelte:window
	onpointerdown={(event) => {
		if (
			menu &&
			!(event.target instanceof Element && event.target.closest('.studio-git__menu-host'))
		) {
			menu = '';
		}
	}}
	onkeydown={(event) => {
		if (menu && event.key === 'Escape') {
			menu = '';
		}
	}}
/>

<StudioCredentialsDialog
	url={credentialsUrl}
	onAnswer={(credentials) => credentialsAnswer?.(credentials)}
/>

<StudioRebaseDialog
	{projectName}
	branch={status?.branch}
	upstream={rebaseFrom}
	onStart={rebaseInteractively}
	onClose={() => (rebaseFrom = null)}
/>

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
				{status.detached ? `detached at ${status.head}` : status.branch}
				{#if status.ahead}<small title="Commits to push">↑{status.ahead}</small>{/if}
				{#if status.behind}<small title="Commits to pull">↓{status.behind}</small>{/if}
			</button>
			<button
				type="button"
				class="studio-git__icon"
				title="Fetch all the remotes"
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
			<div class="studio-git__menu-host">
				<button
					type="button"
					class="studio-git__icon"
					title={status.pullRebase ? 'Pull and rebase' : 'Pull and merge'}
					aria-label="Pull"
					disabled={Boolean(busy) || !status.remote || Boolean(operation)}
					onclick={() => pull()}><Ico icon="mdi:arrow-down" size={4} /></button
				>
				<button
					type="button"
					class="studio-git__icon studio-git__icon--menu"
					title="Pull and merge, or rebase"
					aria-label="Pull options"
					aria-haspopup="menu"
					aria-expanded={menu === 'pull'}
					disabled={Boolean(busy) || !status.remote || Boolean(operation)}
					onclick={() => (menu = menu === 'pull' ? '' : 'pull')}
					><Ico icon="mdi:chevron-down" size={3} /></button
				>
				{#if menu === 'pull'}
					<div class="studio-git__menu" role="menu">
						<button type="button" role="menuitem" onclick={() => pull('merge')}>
							<Ico icon="mdi:source-merge" size={4} /> Pull and merge
						</button>
						<button type="button" role="menuitem" onclick={() => pull('rebase')}>
							<Ico icon="mdi:source-branch-sync" size={4} /> Pull and rebase
						</button>
					</div>
				{/if}
			</div>
			<div class="studio-git__menu-host">
				<button
					type="button"
					class="studio-git__icon"
					title={status.upstream
						? `Push to ${status.upstream}`
						: `Publish ${status.branch} to ${status.remoteName ?? 'a remote'}`}
					aria-label="Push"
					disabled={Boolean(busy) || !status.remote || Boolean(operation) || status.detached}
					onclick={() => push()}
					><Ico
						icon={status.upstream ? 'mdi:arrow-up' : 'mdi:cloud-upload-outline'}
						size={4}
					/></button
				>
				<button
					type="button"
					class="studio-git__icon studio-git__icon--menu"
					title="Push with the tags, forced, or to another remote"
					aria-label="Push options"
					aria-haspopup="menu"
					aria-expanded={menu === 'push'}
					disabled={Boolean(busy) || !status.remote || Boolean(operation) || status.detached}
					onclick={async () => {
						menu = menu === 'push' ? '' : 'push';
						if (menu === 'push' && !remotes.length) {
							const listed = await call('studio.git.SourceControl', {
								projectName,
								action: 'remotes'
							});
							remotes = Array.isArray(listed?.remotes) ? listed.remotes : [];
						}
					}}><Ico icon="mdi:chevron-down" size={3} /></button
				>
				{#if menu === 'push'}
					<div class="studio-git__menu" role="menu">
						<button type="button" role="menuitem" onclick={() => push()}>
							<Ico icon="mdi:arrow-up" size={4} />
							{status.upstream ? `Push to ${status.upstream}` : `Publish to ${status.remoteName}`}
						</button>
						<button type="button" role="menuitem" onclick={() => push({ tags: true })}>
							<Ico icon="mdi:tag-outline" size={4} /> Push with the tags
						</button>
						<button
							type="button"
							role="menuitem"
							disabled={!status.upstream}
							onclick={() => push({ force: true })}
						>
							<Ico icon="mdi:alert-circle-outline" size={4} /> Force push, if the remote did not change
						</button>
						{#if (status.remoteCount ?? 0) > 1}
							<button type="button" role="menuitem" onclick={() => push({ choose: true })}>
								<Ico icon="mdi:cloud-upload-outline" size={4} /> Publish to another remote…
							</button>
						{/if}
					</div>
				{/if}
			</div>
		</div>
		{#if operation}
			<div class="studio-git__operation" role="status" aria-label="Operation in progress">
				<div class="studio-git__operation-title">
					<Ico icon={described.icon} size={4} />
					<strong>{described.title}</strong>
				</div>
				{#if described.detail}
					<small title={described.detail}>{described.detail}</small>
				{/if}
				<small
					>{operation.conflicts
						? `${operation.conflicts} file${operation.conflicts > 1 ? 's' : ''} in conflict`
						: operation.kind === 'rebase'
							? 'Stopped, no conflict left'
							: 'No conflict left: commit to end it'}</small
				>
				<div class="studio-git__operation-actions">
					{#if operation.projectConflicts?.length}
						<button
							type="button"
							class="studio-git__action"
							disabled={Boolean(busy)}
							onclick={() => onMerging?.(projectName)}
							><Ico icon="mdi:file-tree-outline" size={4} /> Resolve in the tree</button
						>
					{/if}
					<span class="studio-git__spacer"></span>
					{#if operation.canSkip}
						<button
							type="button"
							class="studio-git__action"
							title="Skip the commit the rebase stopped at"
							disabled={Boolean(busy)}
							onclick={() => endOperation('skip')}
							><Ico icon="mdi:skip-next" size={4} /> Skip</button
						>
					{/if}
					<button
						type="button"
						class="studio-git__action"
						disabled={Boolean(busy)}
						onclick={() => endOperation('abort')}><Ico icon="mdi:close" size={4} /> Abort</button
					>
					<button
						type="button"
						class="studio-git__action studio-git__action--primary"
						title={operation.kind === 'rebase'
							? 'Commit the commit replayed and go on with the next ones'
							: 'Commit with the message above'}
						disabled={Boolean(busy) || !operation.canContinue}
						onclick={() => endOperation('continue')}
						><Ico icon={operation.kind === 'rebase' ? 'mdi:play' : 'mdi:check'} size={4} />
						{operation.kind === 'rebase' ? 'Continue' : 'Commit'}</button
					>
				</div>
			</div>
		{/if}
		{#if branchesOpen}
			<div class="studio-git__branches" role="listbox" aria-label="Branches">
				<button type="button" class="studio-git__branch-item" onclick={newBranch}>
					<Ico icon="mdi:plus" size={4} /> New branch…
				</button>
				{#each branches.local as branch (branch)}
					<div class="studio-git__branch-row">
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
						<button
							type="button"
							class="studio-git__branch-merge"
							title="Rename {branch}"
							aria-label="Rename {branch}"
							disabled={Boolean(busy)}
							onclick={() => renameBranch(branch)}
						>
							<Ico icon="mdi:rename-outline" size={4} />
						</button>
						{#if branch !== status.branch}
							<button
								type="button"
								class="studio-git__branch-merge"
								title="Delete {branch}"
								aria-label="Delete {branch}"
								disabled={Boolean(busy)}
								onclick={() => deleteBranch(branch)}
							>
								<Ico icon="mdi:delete-outline" size={4} />
							</button>
							<button
								type="button"
								class="studio-git__branch-merge"
								title="Rebase {status.branch} onto {branch}"
								aria-label="Rebase {status.branch} onto {branch}"
								disabled={Boolean(busy) || Boolean(operation)}
								onclick={() => rebaseOnto(branch)}
							>
								<Ico icon="mdi:source-branch-sync" size={4} />
							</button>
							<button
								type="button"
								class="studio-git__branch-merge"
								title="Merge {branch} into {status.branch}"
								aria-label="Merge {branch} into {status.branch}"
								disabled={Boolean(busy) || Boolean(operation)}
								onclick={() => mergeBranch(branch)}
							>
								<Ico icon="mdi:source-merge" size={4} />
							</button>
						{/if}
					</div>
				{/each}
				{#each branches.remote as branch (branch)}
					<div class="studio-git__branch-row">
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
						<button
							type="button"
							class="studio-git__branch-merge"
							title="Delete {branch} from its remote"
							aria-label="Delete {branch} from its remote"
							disabled={Boolean(busy)}
							onclick={() => deleteRemoteBranch(branch)}
						>
							<Ico icon="mdi:delete-outline" size={4} />
						</button>
						<button
							type="button"
							class="studio-git__branch-merge"
							title="Rebase {status.branch} onto {branch}"
							aria-label="Rebase {status.branch} onto {branch}"
							disabled={Boolean(busy) || Boolean(operation)}
							onclick={() => rebaseOnto(branch)}
						>
							<Ico icon="mdi:source-branch-sync" size={4} />
						</button>
						<button
							type="button"
							class="studio-git__branch-merge"
							title="Merge {branch} into {status.branch}"
							aria-label="Merge {branch} into {status.branch}"
							disabled={Boolean(busy) || Boolean(operation)}
							onclick={() => mergeBranch(branch)}
						>
							<Ico icon="mdi:source-merge" size={4} />
						</button>
					</div>
				{/each}
				{#each tags as tag (tag.name)}
					<div class="studio-git__branch-row">
						<button
							type="button"
							class="studio-git__branch-item studio-git__branch-item--remote"
							title={tag.annotation ? `${tag.name}: ${tag.annotation}` : tag.name}
							disabled={Boolean(busy) || Boolean(operation)}
							onclick={() => commitAction('checkout', { id: tag.commit, subject: tag.name })}
						>
							<Ico icon="mdi:tag-outline" size={4} />
							{tag.name}
							<small>{tag.commit}</small>
						</button>
						<button
							type="button"
							class="studio-git__branch-merge"
							title="Delete the tag {tag.name}"
							aria-label="Delete the tag {tag.name}"
							disabled={Boolean(busy)}
							onclick={() => deleteTag(tag.name)}
						>
							<Ico icon="mdi:delete-outline" size={4} />
						</button>
					</div>
				{/each}
				<span class="studio-git__menu-label">Remotes</span>
				{#each remotes as remote (remote.name)}
					<div class="studio-git__branch-row">
						<span
							class="studio-git__branch-item studio-git__branch-item--remote"
							title={remote.url}
						>
							<Ico icon="mdi:cloud-outline" size={4} />
							{remote.name}
							<small class="studio-git__remote-url">{remote.url}</small>
						</span>
						<button
							type="button"
							class="studio-git__branch-merge"
							title="Change the address of {remote.name}"
							aria-label="Change the address of {remote.name}"
							disabled={Boolean(busy)}
							onclick={() => editRemote(remote)}
						>
							<Ico icon="mdi:pencil-outline" size={4} />
						</button>
						<button
							type="button"
							class="studio-git__branch-merge"
							title="Remove the remote {remote.name}"
							aria-label="Remove the remote {remote.name}"
							disabled={Boolean(busy)}
							onclick={() => removeRemote(remote)}
						>
							<Ico icon="mdi:delete-outline" size={4} />
						</button>
					</div>
				{/each}
				<button
					type="button"
					class="studio-git__branch-item"
					disabled={Boolean(busy)}
					onclick={addRemote}
				>
					<Ico icon="mdi:plus" size={4} /> Add a remote…
				</button>
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
			<div class="studio-git__commit-row">
				<label
					class="studio-git__amend"
					title="Amend the last commit with the changes staged and this message"
				>
					<input type="checkbox" checked={amend} disabled={Boolean(busy)} onchange={toggleAmend} />
					Amend
				</label>
				<button
					type="submit"
					class="button-primary"
					disabled={Boolean(busy) || !message.trim() || (!staged.length && !amend)}
				>
					<Ico icon={busy === 'commit' ? 'mdi:sync' : 'mdi:check'} size={4} />
					{amend ? 'Amend' : 'Commit'}
				</button>
			</div>
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
				{#if changes.length || staged.length}
					<button
						type="button"
						class="studio-git__icon"
						title="Stash the changes: set them aside, the files given back as the last commit has them"
						aria-label="Stash the changes"
						disabled={Boolean(busy) || Boolean(operation)}
						onclick={stashChanges}><Ico icon="mdi:archive-arrow-down-outline" size={4} /></button
					>
				{/if}
			</div>
			{@render fileList(changes, 'stage')}
			{#if !staged.length && !changes.length}
				<p class="studio-git__message">No change since the last commit.</p>
			{/if}
			{#if status.stashCount || stashesOpen}
				<div class="studio-git__section">
					<button
						type="button"
						class="studio-git__history-toggle"
						aria-expanded={stashesOpen}
						onclick={toggleStashes}
					>
						<Ico icon={stashesOpen ? 'mdi:chevron-down' : 'mdi:chevron-right'} size={4} />
						<span>Stashes</span>
					</button>
					<small>{status.stashCount ?? 0}</small>
				</div>
				{#if stashesOpen}
					{#each stashes as stash (stash.id)}
						{@const key = `stash@{${stash.index}}`}
						<div
							class={[
								'studio-git__commit-entry',
								commitOpen === key && 'studio-git__commit-entry--open'
							]}
							title={`${key} ${stash.id}`}
						>
							<code>{key}</code>
							<button
								type="button"
								class="studio-git__commit-subject"
								aria-expanded={commitOpen === key}
								onclick={() => toggleCommit(key)}>{stash.subject}</button
							>
							<small>{new Date(stash.time).toLocaleString()}</small>
							<div class="studio-git__stash-actions">
								<button
									type="button"
									class="studio-git__icon"
									title="Apply the stash, kept in the list"
									aria-label="Apply {key}"
									disabled={Boolean(busy) || Boolean(operation)}
									onclick={() => stashAction('stashApply', stash)}
									><Ico icon="mdi:archive-arrow-up-outline" size={4} /></button
								>
								<button
									type="button"
									class="studio-git__icon"
									title="Pop the stash: apply it and drop it"
									aria-label="Pop {key}"
									disabled={Boolean(busy) || Boolean(operation)}
									onclick={() => stashAction('stashPop', stash)}
									><Ico icon="mdi:archive-outline" size={4} /></button
								>
								<button
									type="button"
									class="studio-git__icon"
									title="Drop the stash"
									aria-label="Drop {key}"
									disabled={Boolean(busy)}
									onclick={() => stashAction('stashDrop', stash)}
									><Ico icon="mdi:delete-outline" size={4} /></button
								>
							</div>
						</div>
						{#if commitOpen === key}
							{@render commitDetailsBlock(key)}
						{/if}
					{:else}
						<p class="studio-git__message">No stash.</p>
					{/each}
				{/if}
			{/if}
			<div class="studio-git__section">
				<button
					type="button"
					class="studio-git__history-toggle"
					aria-expanded={historyOpen}
					onclick={toggleHistory}
				>
					<Ico icon={historyOpen ? 'mdi:chevron-down' : 'mdi:chevron-right'} size={4} />
					<span>History</span>
				</button>
				{#if historyOpen}
					<select
						class="studio-git__history-ref"
						aria-label="Branch of the history"
						bind:value={historyRef}
						onchange={() => loadHistory()}
					>
						<option value="">{status.branch} (current)</option>
						{#each branches.local.filter((branch) => branch !== status?.branch) as branch (branch)}
							<option value={branch}>{branch}</option>
						{/each}
						{#each branches.remote as branch (branch)}
							<option value={branch}>{branch}</option>
						{/each}
					</select>
				{/if}
			</div>
			{#if historyOpen}
				{#each commits as entry (entry.id)}
					<div
						class={[
							'studio-git__commit-entry',
							commitOpen === entry.id && 'studio-git__commit-entry--open'
						]}
						title={`${entry.id} ${entry.author}`}
					>
						<code>{entry.id}</code>
						<button
							type="button"
							class="studio-git__commit-subject"
							aria-expanded={commitOpen === entry.id}
							onclick={() => toggleCommit(entry.id)}
						>
							{#each entry.refs ?? [] as ref (ref)}
								<span
									class={[
										'studio-git__ref',
										ref.startsWith('tag: ') && 'studio-git__ref--tag',
										ref.includes('/') && !ref.startsWith('tag: ') && 'studio-git__ref--remote'
									]}>{ref.replace(/^tag: /, '')}</span
								>
							{/each}
							{entry.subject}
						</button>
						<small>{entry.author} · {new Date(entry.time).toLocaleDateString()}</small>
						<div class="studio-git__menu-host studio-git__commit-menu">
							<button
								type="button"
								class="studio-git__icon"
								title="Actions of the commit"
								aria-label="Actions of {entry.id}"
								aria-haspopup="menu"
								aria-expanded={menu === entry.id}
								onclick={() => (menu = menu === entry.id ? '' : entry.id)}
								><Ico icon="mdi:dots-horizontal" size={4} /></button
							>
							{#if menu === entry.id}
								<div class="studio-git__menu studio-git__menu--end" role="menu">
									<button type="button" role="menuitem" onclick={() => compareWith(entry.id)}>
										<Ico icon="mdi:file-compare" size={4} /> Compare the tree with this commit
									</button>
									<button
										type="button"
										role="menuitem"
										disabled={Boolean(operation)}
										onclick={() => commitAction('checkout', entry)}
									>
										<Ico icon="mdi:source-commit" size={4} /> Check out this commit
									</button>
									<button
										type="button"
										role="menuitem"
										disabled={Boolean(operation)}
										onclick={() => commitAction('branch', entry)}
									>
										<Ico icon="mdi:source-branch-plus" size={4} /> New branch here…
									</button>
									<button type="button" role="menuitem" onclick={() => commitAction('tag', entry)}>
										<Ico icon="mdi:tag-plus-outline" size={4} /> Tag…
									</button>
									<button
										type="button"
										role="menuitem"
										disabled={entry.inHead !== false || entry.merge || Boolean(operation)}
										title={entry.inHead !== false ? `${status.branch} already has this commit` : ''}
										onclick={() => applyCommit('cherryPick', entry)}
									>
										<Ico icon="mdi:fruit-cherries" size={4} /> Cherry-pick onto {status.branch}
									</button>
									<button
										type="button"
										role="menuitem"
										disabled={entry.inHead === false || entry.merge || Boolean(operation)}
										title={entry.inHead === false ? `${status.branch} has not this commit` : ''}
										onclick={() => applyCommit('revert', entry)}
									>
										<Ico icon="mdi:undo-variant" size={4} /> Revert
									</button>
									<button
										type="button"
										role="menuitem"
										disabled={Boolean(operation) ||
											entry.inHead === false ||
											commits[0]?.id === entry.id ||
											Boolean(historyRef)}
										title="Pick, reword, edit, squash, fix up, drop or reorder the commits after this one"
										onclick={() => {
											menu = '';
											rebaseFrom = entry;
										}}
									>
										<Ico icon="mdi:source-branch-sync" size={4} /> Rebase the commits after this one…
									</button>
									<span class="studio-git__menu-label">Reset {status.branch} here</span>
									<button
										type="button"
										role="menuitem"
										disabled={Boolean(operation)}
										onclick={() => commitAction('soft', entry)}
									>
										<Ico icon="mdi:restore" size={4} /> Soft: the changes since stay staged
									</button>
									<button
										type="button"
										role="menuitem"
										disabled={Boolean(operation)}
										onclick={() => commitAction('mixed', entry)}
									>
										<Ico icon="mdi:restore" size={4} /> Mixed: they stay in the files
									</button>
									<button
										type="button"
										role="menuitem"
										disabled={Boolean(operation)}
										onclick={() => commitAction('hard', entry)}
									>
										<Ico icon="mdi:restore" size={4} /> Hard: they are lost
									</button>
								</div>
							{/if}
						</div>
					</div>
					{#if commitOpen === entry.id}
						{@render commitDetailsBlock(entry.id)}
					{/if}
				{:else}
					<p class="studio-git__message">No commit yet.</p>
				{/each}
			{/if}
		</div>
	{/if}
</div>

<style>
	.studio-git {
		display: flex;
		min-height: 0;
		height: 100%;
		flex-direction: column;
		font-size: 0.78rem;
	}

	.studio-git > :global(*) {
		flex: none;
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
		display: flex;
		align-items: center;
		gap: 0.35rem;
		border: 0;
		background: transparent;
		color: inherit;
		padding: 0;
		font: inherit;
		letter-spacing: inherit;
		text-align: left;
		text-transform: inherit;
	}

	.studio-git__history-ref {
		min-width: 0;
		max-width: 12rem;
		height: 1.35rem;
		margin-left: auto;
		border: 1px solid var(--studio-line);
		border-radius: 0.25rem;
		background-color: transparent;
		color: var(--studio-text);
		padding: 0 1.3rem 0 0.3rem;
		font-size: 0.7rem;
		font-weight: 500;
		letter-spacing: 0;
		text-transform: none;
	}

	.studio-git__commit-entry {
		display: grid;
		grid-template-columns: auto minmax(0, 1fr) auto;
		column-gap: 0.4rem;
		padding: 0.2rem 0.5rem 0.2rem 1.1rem;
	}

	.studio-git__commit-entry:hover {
		background: var(--studio-hover-bg);
	}

	.studio-git__commit-subject {
		display: block;
		min-width: 0;
		overflow: hidden;
		border: 0;
		background: transparent;
		color: var(--studio-text);
		padding: 0;
		text-align: left;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-git__commit-entry--open {
		background: var(--studio-hover-bg);
	}

	.studio-git__ref {
		display: inline-block;
		margin-right: 0.25rem;
		border-radius: 0.2rem;
		background: color-mix(in oklab, var(--color-primary-500) 18%, transparent);
		color: var(--studio-text-strong);
		padding: 0 0.25rem;
		font-size: 0.66rem;
		font-weight: 600;
	}

	.studio-git__ref--remote {
		background: color-mix(in oklab, var(--color-secondary-500, #888) 18%, transparent);
	}

	.studio-git__ref--tag {
		background: color-mix(in oklab, var(--color-warning-500) 22%, transparent);
	}

	.studio-git__commit-details {
		display: grid;
		gap: 0.2rem;
		padding: 0.15rem 0.5rem 0.4rem 1.1rem;
		background: var(--studio-hover-bg);
	}

	.studio-git__commit-details > small {
		color: var(--studio-text-idle);
		font-size: 0.68rem;
	}

	.studio-git__commit-body {
		margin: 0;
		font-family: inherit;
		font-size: 0.74rem;
		white-space: pre-wrap;
	}

	.studio-git__menu-label {
		padding: 0.35rem 0.5rem 0.1rem;
		color: var(--studio-text-idle);
		font-size: 0.66rem;
		font-weight: 600;
		letter-spacing: 0.04em;
		text-transform: uppercase;
	}

	.studio-git__commit-row {
		display: flex;
		align-items: center;
		gap: 0.5rem;
	}

	.studio-git__commit-row button {
		flex: 1;
	}

	.studio-git__amend {
		display: inline-flex;
		align-items: center;
		gap: 0.25rem;
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-git__branch-item small {
		color: var(--studio-text-idle);
		font-size: 0.66rem;
	}

	.studio-git__remote-url {
		min-width: 0;
		flex: 1 1 auto;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-git__branches {
		overflow-x: hidden;
	}

	.studio-git__branches > * {
		min-width: 0;
	}

	.studio-git__branch-item > :global(:first-child) {
		flex: none;
	}

	.studio-git__stash-actions {
		display: flex;
		grid-row: 1 / span 2;
		grid-column: 3;
		align-self: center;
	}

	.studio-git__section small:last-child {
		margin-right: 0;
	}

	.studio-git__commit-menu {
		grid-row: 1 / span 2;
		grid-column: 3;
		align-self: center;
		opacity: 0;
	}

	.studio-git__commit-entry:hover .studio-git__commit-menu,
	.studio-git__commit-menu:focus-within {
		opacity: 1;
	}

	.studio-git__menu-host {
		position: relative;
		display: flex;
		align-items: center;
	}

	.studio-git__icon--menu {
		width: 0.9rem;
		margin-left: -0.15rem;
	}

	.studio-git__menu {
		position: absolute;
		z-index: 20;
		top: calc(100% + 0.2rem);
		right: 0;
		display: grid;
		min-width: 12rem;
		border: 1px solid var(--studio-line);
		border-radius: 0.35rem;
		background: var(--studio-chrome-bg, var(--color-surface-50-950));
		box-shadow: 0 0.4rem 1.2rem rgb(0 0 0 / 0.18);
		padding: 0.2rem;
	}

	.studio-git__menu button {
		display: flex;
		align-items: center;
		gap: 0.4rem;
		border: 0;
		border-radius: 0.25rem;
		background: transparent;
		color: var(--studio-text);
		padding: 0.3rem 0.5rem;
		text-align: left;
		white-space: nowrap;
	}

	.studio-git__menu button:hover:not(:disabled) {
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

	.studio-git__menu button:disabled {
		opacity: 0.45;
	}

	.studio-git__operation {
		display: grid;
		gap: 0.2rem;
		margin: 0.1rem 0.5rem 0.35rem 0.85rem;
		border: 1px solid color-mix(in oklab, var(--color-warning-500) 45%, transparent);
		border-radius: 0.35rem;
		background: color-mix(in oklab, var(--color-warning-500) 10%, transparent);
		padding: 0.4rem 0.5rem;
	}

	.studio-git__operation-title {
		display: flex;
		min-width: 0;
		align-items: center;
		gap: 0.35rem;
		color: var(--studio-text-strong);
	}

	.studio-git__operation-title strong {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-git__operation small {
		overflow: hidden;
		color: var(--studio-text-idle);
		font-size: 0.7rem;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-git__operation-actions {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 0.25rem;
		margin-top: 0.15rem;
	}

	.studio-git__spacer {
		flex: 1;
	}

	.studio-git__action {
		display: inline-flex;
		align-items: center;
		gap: 0.25rem;
		border: 1px solid var(--studio-line);
		border-radius: 0.25rem;
		background: transparent;
		color: var(--studio-text);
		padding: 0.12rem 0.45rem;
		font-size: 0.72rem;
	}

	.studio-git__action:hover:not(:disabled) {
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

	.studio-git__action:disabled {
		opacity: 0.45;
	}

	.studio-git__action--primary:not(:disabled) {
		border-color: var(--color-primary-500);
		background: var(--color-primary-500);
		color: var(--color-primary-contrast-500, white);
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
		flex: 1 1 auto !important;
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
	.studio-git__branch-row {
		display: flex;
		align-items: stretch;
	}

	.studio-git__branch-row .studio-git__branch-item {
		flex: 1;
		min-width: 0;
	}

	.studio-git__branch-merge {
		display: inline-grid;
		flex: none;
		width: 1.8rem;
		place-items: center;
		border: 0;
		border-radius: 0.3rem;
		background: transparent;
		color: var(--studio-text-idle);
	}

	.studio-git__branch-merge:not(:disabled):hover {
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

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
