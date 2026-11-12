<script>
	import { setExperimentalFeatures } from './experimentalFeatures.svelte.js';
	import { registration } from './registration.svelte.js';
	import {
		DEFAULT_MARKETPLACE_URL,
		saveStudioPreferences,
		studioPreferences
	} from './studioPreferences.svelte.js';
	import StudioProxyDialog from './StudioProxyDialog.svelte';

	/**
	 * The preferences of the Studio, as the Studio preference page of the Eclipse Studio.
	 *
	 * @type {{ onClose?: () => void }}
	 */
	let { onClose } = $props();

	let gitRepositoryForNewProjects = $state(studioPreferences.gitRepositoryForNewProjects);
	let readmeOnSave = $state(studioPreferences.readmeOnSave);
	let marketplaceUrl = $state(studioPreferences.marketplaceUrl);
	let projectReferences = $state(studioPreferences.projectReferences);
	let experimentalFeatures = $state(studioPreferences.experimentalFeatures === true);
	let assistantUrl = $state(String(studioPreferences.assistantUrl ?? ''));
	let assistantChannel = $state(
		studioPreferences.assistantChannel === 'preview' ? 'preview' : 'stable'
	);

	/** the proxy of the engine, in its own dialog */
	let proxyOpen = $state(false);

	/** the registration of a desktop Studio, in its own dialog */
	function openRegistration() {
		registration.open = true;
		onClose?.();
	}

	function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		saveStudioPreferences({
			gitRepositoryForNewProjects,
			readmeOnSave,
			marketplaceUrl: marketplaceUrl.trim() || DEFAULT_MARKETPLACE_URL,
			projectReferences,
			assistantUrl: assistantUrl.trim(),
			assistantChannel: assistantChannel === 'preview' ? 'preview' : 'stable'
		});
		if (experimentalFeatures !== (studioPreferences.experimentalFeatures === true)) {
			void setExperimentalFeatures(experimentalFeatures);
		}
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
							>With an initial commit, for the projects created, or started from a Marketplace
							starter.</small
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
				<label class="studio-dialog__check">
					<input type="checkbox" bind:checked={experimentalFeatures} />
					<span>
						Show new experimental features
						<small
							>Native JSON sequences, native JSON steps and JSON path sources, in the palette, the
							properties and the pickers.</small
						>
					</span>
				</label>
				<label class="studio-dialog__field">
					<span>References of the used projects</span>
					<select class="input-common" bind:value={projectReferences}>
						<option value="ask">Ask on save</option>
						<option value="always">Always add them on save</option>
						<option value="never">Never add them</option>
					</select>
				</label>
				<label class="studio-dialog__field">
					<span>Marketplace URL</span>
					<input
						class="input-common"
						bind:value={marketplaceUrl}
						placeholder={DEFAULT_MARKETPLACE_URL}
					/>
				</label>
				<label class="studio-dialog__field">
					<span>Releases of the local Assistant</span>
					<select class="input-common" bind:value={assistantChannel}>
						<option value="stable">Stable</option>
						<option value="preview">Preview (pre-releases)</option>
					</select>
				</label>
				<label class="studio-dialog__field">
					<span>Assistant URL</span>
					<input
						class="input-common"
						bind:value={assistantUrl}
						placeholder="Automatic: the local Assistant once installed, else assistant.convertigo.com"
					/>
				</label>
				<p class="studio-dialog__hint">These preferences are kept in this browser.</p>
				<div class="studio-dialog__field">
					<span>Network proxy</span>
					<div class="studio-dialog__row">
						<small>The proxy the engine uses to reach the Internet, kept in the workspace</small>
						<button type="button" class="button-secondary" onclick={() => (proxyOpen = true)}>
							Configure…
						</button>
					</div>
				</div>
				{#if registration.state?.desktop}
					<div class="studio-dialog__field">
						<span>Personal Studio Configuration</span>
						<div class="studio-dialog__row">
							<small
								>{registration.state.registered
									? `Registered for ${registration.state.owner}`
									: 'Not registered: the Convertigo agent of the Assistant needs it'}</small
							>
							<button type="button" class="button-secondary" onclick={openRegistration}>
								{registration.state.registered ? 'Change…' : 'Register…'}
							</button>
						</div>
					</div>
				{/if}
			</div>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" onclick={() => onClose?.()}>Cancel</button>
				<button type="submit" class="button-primary">Apply</button>
			</footer>
		</form>
	</div>
</div>

{#if proxyOpen}
	<StudioProxyDialog onClose={() => (proxyOpen = false)} />
{/if}

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

	.studio-dialog__row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 0.6rem;
	}

	.studio-dialog__row small {
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-dialog__row button {
		flex: none;
		min-height: 1.8rem;
		padding-block: 0;
		font-size: 0.75rem;
	}

	.studio-dialog__field select,
	.studio-dialog__field input {
		height: 2rem;
		padding-block: 0;
		padding-inline: 0.6rem;
		font-size: 0.8rem;
	}
</style>
