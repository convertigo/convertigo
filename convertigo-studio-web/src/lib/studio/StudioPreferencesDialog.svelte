<script>
	import {
		DEFAULT_MARKETPLACE_URL,
		saveStudioPreferences,
		studioPreferences
	} from './studioPreferences.svelte.js';

	/**
	 * The preferences of the Studio, as the Studio preference page of the Eclipse Studio.
	 *
	 * @type {{ onClose?: () => void }}
	 */
	let { onClose } = $props();

	let gitRepositoryForNewProjects = $state(studioPreferences.gitRepositoryForNewProjects);
	let readmeOnSave = $state(studioPreferences.readmeOnSave);
	let marketplaceUrl = $state(studioPreferences.marketplaceUrl);

	function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		saveStudioPreferences({
			gitRepositoryForNewProjects,
			readmeOnSave,
			marketplaceUrl: marketplaceUrl.trim() || DEFAULT_MARKETPLACE_URL
		});
		onClose?.();
	}
</script>

<div class="studio-dialog" role="presentation" onclick={() => onClose?.()}>
	<div
		class="studio-dialog__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-preferences-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape') onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-preferences-title">Studio preferences</strong>
		</header>
		<form class="studio-dialog__form" onsubmit={submit}>
			<div class="studio-dialog__body">
				<label class="studio-dialog__check">
					<input type="checkbox" bind:checked={gitRepositoryForNewProjects} />
					<span>
						Create a Git repository for the new projects
						<small
							>With an initial commit, for the projects created or installed from the Marketplace.</small
						>
					</span>
				</label>
				<label class="studio-dialog__check">
					<input type="checkbox" bind:checked={readmeOnSave} />
					<span>
						Update the readme.md file on save
						<small>The project.md file is always written again.</small>
					</span>
				</label>
				<label class="studio-dialog__field">
					<span>Marketplace URL</span>
					<input
						class="input-common"
						bind:value={marketplaceUrl}
						placeholder={DEFAULT_MARKETPLACE_URL}
					/>
				</label>
				<p class="studio-dialog__hint">These preferences are kept in this browser.</p>
			</div>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" onclick={() => onClose?.()}>Cancel</button>
				<button type="submit" class="button-primary">Apply</button>
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
		width: min(30rem, 100%);
		max-height: min(36rem, 100%);
		grid-template-rows: auto minmax(0, 1fr);
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
	}

	.studio-dialog__form {
		display: grid;
		min-height: 0;
		grid-template-rows: minmax(0, 1fr) auto;
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
		min-width: 0;
		min-height: 0;
		align-content: start;
		gap: 0.9rem;
		overflow-y: auto;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-dialog__check {
		display: flex;
		align-items: flex-start;
		gap: 0.5rem;
	}

	.studio-dialog__check span {
		display: grid;
		gap: 0.1rem;
	}

	.studio-dialog__check small,
	.studio-dialog__hint {
		margin: 0;
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-dialog__field {
		display: grid;
		gap: 0.3rem;
	}

	.studio-dialog__field > span {
		font-weight: 600;
	}

	.studio-dialog__field input {
		height: 2rem;
		padding-block: 0;
		padding-inline: 0.6rem;
		font-size: 0.8rem;
	}
</style>
