<script>
	import { call } from '#lib/utils/service.js';
	import { onMount } from 'svelte';

	/**
	 * The deletion of a project, as in the Eclipse Studio: the project leaves the Studio, and its folder on disk is
	 * deleted only when "Delete the project folder on disk" is checked, which it is not by default. Unchecked, the
	 * link to a project of another folder is removed, and a project of the workspace is closed.
	 *
	 * @type {{ projectName: string, onClose?: () => void, onRemoved?: (removed: string) => void }}
	 */
	let { projectName, onClose, onRemoved } = $props();

	let info = $state(/** @type {{ path: string, linked: boolean, git: boolean } | null} */ (null));
	let deleteFiles = $state(false);
	let busy = $state(false);
	let error = $state('');

	onMount(() => {
		void call('studio.project.Remove', { projectName }, { silentError: () => true }).then(
			(result) => {
				if (result?.path) {
					info = result;
				} else {
					error = String(result?.error?.message ?? result?.error ?? 'The project cannot be found.');
				}
			}
		);
	});

	async function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		if (busy || !info) {
			return;
		}
		busy = true;
		error = '';
		try {
			const result = await call(
				'studio.project.Remove',
				{ projectName, action: 'remove', deleteFiles: String(deleteFiles) },
				{ silentError: () => true }
			);
			if (result?.removed) {
				onRemoved?.(result.removed);
			} else {
				error = String(result?.error?.message ?? result?.error ?? 'The project cannot be removed.');
			}
		} finally {
			busy = false;
		}
	}
</script>

<div class="studio-dialog" role="presentation" onclick={() => onClose?.()}>
	<div
		class="studio-dialog__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-delete-project-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape') onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-delete-project-title">Delete the project {projectName}</strong>
		</header>
		<form class="studio-dialog__form" onsubmit={submit}>
			<div class="studio-dialog__body">
				{#if info}
					<p>
						Location: <code>{info.path}</code>{info.git ? ' (a Git repository)' : ''}
					</p>
					<label class="studio-dialog__check">
						<input type="checkbox" bind:checked={deleteFiles} />
						<span>Delete the project folder on disk (cannot be undone)</span>
					</label>
					<p class="studio-dialog__hint" role="status">
						{#if deleteFiles}
							The folder and everything in it are deleted{info.git
								? ', with the Git repository: its uncommitted changes, local branches and stashes'
								: ''}.
						{:else if info.linked}
							The project leaves the Studio; its folder stays on disk.
						{:else}
							The project is closed: its folder stays in the workspace, and the project can be
							opened again.
						{/if}
					</p>
				{:else if !error}
					<p class="studio-dialog__hint">Reading the project…</p>
				{/if}
				{#if error}
					<p class="studio-dialog__error" role="alert">{error}</p>
				{/if}
			</div>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" onclick={() => onClose?.()}>Cancel</button>
				<button type="submit" class="button-primary" disabled={busy || !info}>
					{deleteFiles ? 'Delete' : 'Remove'}
				</button>
			</footer>
		</form>
	</div>
</div>

<style>
	.studio-dialog {
		position: fixed;
		inset: 0;
		z-index: 90;
		display: grid;
		place-items: center;
		background: color-mix(in oklab, black 45%, transparent);
		padding: 1rem;
	}

	.studio-dialog__box {
		display: grid;
		width: min(34rem, 100%);
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
	}

	.studio-dialog__header,
	.studio-dialog__footer {
		display: flex;
		align-items: center;
		justify-content: flex-end;
		gap: 0.6rem;
		padding: 0.75rem 1rem;
	}

	.studio-dialog__header {
		justify-content: flex-start;
		border-bottom: 1px solid var(--studio-line);
	}

	.studio-dialog__header strong {
		color: var(--studio-text-strong);
		font-size: 0.9rem;
		font-weight: 600;
	}

	.studio-dialog__footer {
		border-top: 1px solid var(--studio-line);
	}

	.studio-dialog__body {
		display: grid;
		gap: 0.75rem;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-dialog__body p {
		margin: 0;
	}

	.studio-dialog__body code {
		word-break: break-all;
	}

	.studio-dialog__check {
		display: flex;
		align-items: center;
		gap: 0.5rem;
	}

	.studio-dialog__hint {
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-dialog__error {
		color: var(--color-error-500);
	}
</style>
