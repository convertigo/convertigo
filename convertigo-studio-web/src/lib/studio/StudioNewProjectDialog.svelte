<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { call } from '$lib/utils/service';
	import { PROJECT_TEMPLATES, projectNameError, projectTemplateSettings } from './projectTemplates';

	/**
	 * Creates a project from a template, as the new project wizards of the Eclipse Studio, or imports
	 * one from the URL of its git repository or of its archive, from an archive file, or from its folder on
	 * the disk of the engine.
	 *
	 * @type {{
	 *  onDone?: (projectName: string) => void | Promise<void>,
	 *  onClose?: () => void
	 * }}
	 */
	let { onDone, onClose } = $props();

	/** @type {'template' | 'url' | 'file' | 'folder'} */
	let mode = $state('template');
	let folder = $state('');
	/** @type {File | null} */
	let archive = $state(null);
	let templateId = $state(PROJECT_TEMPLATES[0].id);
	let name = $state('');
	/** @type {Record<string, any>} */
	let values = $state(defaultValues(PROJECT_TEMPLATES[0]));
	let url = $state('');
	/** the name of the project of a URL, as the Project name of the import wizard of the Eclipse Studio */
	let urlName = $state('');
	/** a name typed before the URL, as "ProjectName=https://…", which the engine reads */
	let namedUrl = $derived(/^[^=:/\s]+=/.test(url.trim()));
	/** an archive keeps the name of its project, a Git repository needs the name of its project */
	let archiveUrl = $derived(/^(?:[^=:/\s]+=)?https?:\/\/.*\.(?:zip|car)\b/i.test(url.trim()));
	let urlNameError = $derived(
		urlName.trim()
			? projectNameError(urlName.trim())
			: url.trim() && !namedUrl && !archiveUrl
				? 'A Git repository needs the name of its project.'
				: ''
	);
	let busy = $state(false);
	let error = $state('');
	let template = $derived(
		PROJECT_TEMPLATES.find((candidate) => candidate.id === templateId) ?? PROJECT_TEMPLATES[0]
	);
	let nameError = $derived(name ? projectNameError(name) : '');
	let missingField = $derived(
		template.fields.find((field) => field.required && !String(values[field.name] ?? '').trim())
	);
	let canCreate = $derived(
		mode === 'template'
			? !projectNameError(name) && !missingField
			: mode === 'url'
				? Boolean(url.trim()) && !urlNameError
				: mode === 'folder'
					? Boolean(folder.trim())
					: Boolean(archive)
	);

	/**
	 * @param {import('./projectTemplates').ProjectTemplate} candidate
	 */
	function defaultValues(candidate) {
		return Object.fromEntries(candidate.fields.map((field) => [field.name, field.value ?? '']));
	}

	/**
	 * @param {string} id
	 */
	function chooseTemplate(id) {
		templateId = id;
		values = defaultValues(template);
	}

	async function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		if (!canCreate || busy) {
			return;
		}
		busy = true;
		error = '';
		try {
			if (mode === 'template') {
				const result = await call('studio.project.Create', {
					name: name.trim(),
					template: template.url,
					settings: JSON.stringify(projectTemplateSettings(template, values))
				});
				if (result?.done) {
					await onDone?.(String(result.name ?? name.trim()));
				} else {
					error = String(
						result?.error?.message ?? result?.message ?? 'The project was not created.'
					);
				}
			} else if (mode === 'folder') {
				const result = await call('studio.project.ImportFolder', { path: folder.trim() });
				if (result?.done) {
					await onDone?.(String(result.project));
				} else {
					error = String(
						result?.error?.message ?? result?.message ?? 'The project was not imported.'
					);
				}
			} else if (mode === 'file' && archive) {
				const form = new FormData();
				form.append('file', archive);
				form.append('bAssembleXsl', 'false');
				const result = await call('projects.Deploy', form);
				const deployed = String(result?.admin?.message ?? '').match(/project '([^']+)'/)?.[1];
				if (deployed) {
					await onDone?.(deployed);
				} else {
					error = String(result?.admin?.error ?? 'The project was not imported.');
				}
			} else {
				const typed = url.trim();
				const result = await call('projects.ImportURL', {
					url: !namedUrl && urlName.trim() ? `${urlName.trim()}=${typed}` : typed
				});
				const success = String(result?.admin?.success ?? '');
				const imported = success.match(/project '([^']+)'/)?.[1];
				if (imported) {
					await onDone?.(imported);
				} else {
					error = String(result?.admin?.error ?? 'The project was not imported.');
				}
			}
		} catch (exception) {
			error = String(exception instanceof Error ? exception.message : exception);
		} finally {
			busy = false;
		}
	}
</script>

<div class="studio-dialog" role="presentation" onclick={() => !busy && onClose?.()}>
	<div
		class="studio-dialog__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-new-project-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape' && !busy) onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-new-project-title">New project</strong>
			<div class="studio-dialog__modes" role="tablist" aria-label="Project source">
				<button
					type="button"
					role="tab"
					aria-selected={mode === 'template'}
					class={['studio-dialog__mode', mode === 'template' && 'studio-dialog__mode--active']}
					onclick={() => (mode = 'template')}>From a template</button
				>
				<button
					type="button"
					role="tab"
					aria-selected={mode === 'url'}
					class={['studio-dialog__mode', mode === 'url' && 'studio-dialog__mode--active']}
					onclick={() => (mode = 'url')}>From a URL</button
				>
				<button
					type="button"
					role="tab"
					aria-selected={mode === 'file'}
					class={['studio-dialog__mode', mode === 'file' && 'studio-dialog__mode--active']}
					onclick={() => (mode = 'file')}>From a file</button
				>
				<button
					type="button"
					role="tab"
					aria-selected={mode === 'folder'}
					class={['studio-dialog__mode', mode === 'folder' && 'studio-dialog__mode--active']}
					onclick={() => (mode = 'folder')}>From a folder</button
				>
			</div>
		</header>

		<form class="studio-dialog__form" onsubmit={submit}>
			<fieldset class="studio-dialog__body" disabled={busy}>
				{#if mode === 'template'}
					<div class="studio-dialog__templates" role="radiogroup" aria-label="Template">
						{#snippet templateChoice(/** @type {(typeof PROJECT_TEMPLATES)[number]} */ candidate)}
							<button
								type="button"
								role="radio"
								aria-checked={candidate.id === templateId}
								class={[
									'studio-dialog__template',
									candidate.id === templateId && 'studio-dialog__template--active'
								]}
								onclick={() => chooseTemplate(candidate.id)}
							>
								<Ico icon={candidate.icon} size={5} />
								<span>
									<strong>{candidate.label}</strong>
									<small>{candidate.description}</small>
								</span>
							</button>
						{/snippet}
						{#each PROJECT_TEMPLATES.filter((candidate) => !candidate.legacy) as candidate (candidate.id)}
							{@render templateChoice(candidate)}
						{/each}
						<details class="studio-dialog__legacy" open={Boolean(template.legacy)}>
							<summary>Screen and CICS connectors</summary>
							<div class="studio-dialog__templates">
								{#each PROJECT_TEMPLATES.filter((candidate) => candidate.legacy) as candidate (candidate.id)}
									{@render templateChoice(candidate)}
								{/each}
							</div>
						</details>
					</div>
					<label class="studio-dialog__field">
						<span>Project name</span>
						<!-- svelte-ignore a11y_autofocus -->
						<input class="input-common" bind:value={name} autofocus placeholder="MyProject" />
						{#if nameError}
							<small class="studio-dialog__error">{nameError}</small>
						{/if}
					</label>
					{#each template.fields as field (field.name)}
						{#if field.type === 'checkbox'}
							<label class="studio-dialog__check">
								<input type="checkbox" bind:checked={values[field.name]} />
								<span>{field.label}</span>
							</label>
						{:else}
							<label class="studio-dialog__field">
								<span>{field.label}{field.required ? '' : ' (optional)'}</span>
								<input
									class="input-common"
									type={field.type ?? 'text'}
									placeholder={field.placeholder ?? ''}
									bind:value={values[field.name]}
								/>
							</label>
						{/if}
					{/each}
				{:else if mode === 'folder'}
					<label class="studio-dialog__field">
						<span>Project folder</span>
						<!-- svelte-ignore a11y_autofocus -->
						<input
							class="input-common"
							bind:value={folder}
							autofocus
							placeholder="/path/to/MyProject or /path/to/MyProject/c8oProject.yaml"
						/>
						<small
							>The folder of a project, or its c8oProject.yaml, on the disk of the engine. The
							project stays in its folder.</small
						>
					</label>
				{:else if mode === 'file'}
					<label class="studio-dialog__field">
						<span>Project archive</span>
						<input
							class="studio-dialog__file input-common"
							type="file"
							accept=".car,.zip"
							onchange={(event) => (archive = event.currentTarget.files?.[0] ?? null)}
						/>
						<small
							>A .car or .zip archive exported from a Studio. It replaces a project of the same
							name.</small
						>
					</label>
				{:else}
					<label class="studio-dialog__field">
						<span>Git repository or project archive</span>
						<!-- svelte-ignore a11y_autofocus -->
						<input
							class="input-common"
							bind:value={url}
							autofocus
							placeholder="https://github.com/owner/project.git or https://…/project.car"
						/>
						<small>
							Add a branch with <code>:branch=name</code> or a folder with <code>:path=folder</code>
							for a git repository.
						</small>
					</label>
					{#if !namedUrl}
						<label class="studio-dialog__field">
							<span>Project name</span>
							<input
								class="input-common"
								bind:value={urlName}
								placeholder={archiveUrl ? 'The name of the project of the archive' : 'MyProject'}
							/>
							{#if urlNameError}
								<small class="studio-dialog__error">{urlNameError}</small>
							{:else}
								<small
									>{archiveUrl
										? 'Leave it empty to keep the name of the project of the archive.'
										: 'The name of the project of the repository, as in its c8oProject.yaml.'}</small
								>
							{/if}
						</label>
					{/if}
				{/if}
				{#if error}
					<p class="studio-dialog__error" role="alert">{error}</p>
				{/if}
			</fieldset>

			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" disabled={busy} onclick={() => onClose?.()}
					>Cancel</button
				>
				<button type="submit" class="button-primary" disabled={!canCreate || busy}>
					{#if busy}<Ico icon="mdi:sync" size={4} />{/if}
					{mode === 'template' ? 'Create' : 'Import'}
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
		width: min(40rem, 100%);
		max-height: min(44rem, calc(100vh - 2rem));
		grid-template-rows: auto minmax(0, 1fr) auto;
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
	}

	.studio-dialog__form {
		display: contents;
	}

	.studio-dialog__header,
	.studio-dialog__footer {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 1rem;
		padding: 0.75rem 1rem;
	}

	.studio-dialog__header {
		border-bottom: 1px solid var(--studio-line);
	}

	.studio-dialog__header strong {
		color: var(--studio-text-strong);
		font-size: 0.9rem;
		font-weight: 600;
	}

	.studio-dialog__footer {
		justify-content: flex-end;
		border-top: 1px solid var(--studio-line);
	}

	.studio-dialog__modes {
		display: flex;
		gap: 0.1rem;
		border: 1px solid var(--studio-line);
		border-radius: 0.4rem;
		padding: 0.12rem;
	}

	.studio-dialog__mode {
		border: 0;
		border-radius: 0.28rem;
		background: transparent;
		color: var(--studio-text-idle);
		padding: 0.25rem 0.6rem;
		font-size: 0.75rem;
		font-weight: 600;
	}

	.studio-dialog__mode--active {
		background: var(--studio-selection-bg);
		color: var(--studio-text-strong);
	}

	.studio-dialog__body {
		min-width: 0;
		display: grid;
		min-height: 0;
		align-content: start;
		gap: 0.85rem;
		overflow: auto;
		margin: 0;
		border: 0;
		padding: 1rem;
	}

	.studio-dialog__templates {
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr));
		gap: 0.5rem;
	}

	.studio-dialog__legacy {
		grid-column: 1 / -1;
	}

	.studio-dialog__legacy summary {
		cursor: pointer;
		padding: 0.25rem 0;
		color: var(--studio-text-idle);
		font-size: 0.75rem;
		font-weight: 600;
	}

	.studio-dialog__legacy .studio-dialog__templates {
		margin-top: 0.4rem;
	}

	.studio-dialog__template {
		display: grid;
		grid-template-columns: auto minmax(0, 1fr);
		align-items: start;
		gap: 0.6rem;
		border: 1px solid var(--studio-line);
		border-radius: 0.4rem;
		background: transparent;
		color: var(--studio-text-idle);
		padding: 0.6rem 0.7rem;
		text-align: left;
	}

	.studio-dialog__template:hover {
		background: var(--studio-hover-bg);
	}

	.studio-dialog__template--active {
		border-color: var(--color-primary-500);
		background: var(--studio-selection-bg);
		color: var(--color-primary-500);
	}

	.studio-dialog__template span {
		display: grid;
		gap: 0.15rem;
	}

	.studio-dialog__template strong {
		color: var(--studio-text-strong);
		font-size: 0.8rem;
		font-weight: 600;
	}

	.studio-dialog__template small {
		color: var(--studio-text-idle);
		font-size: 0.72rem;
		line-height: 1.3;
	}

	.studio-dialog__field {
		display: grid;
		gap: 0.3rem;
		font-size: 0.78rem;
	}

	.studio-dialog__field > span {
		color: var(--studio-text);
		font-weight: 600;
	}

	.studio-dialog__field input {
		height: 2rem;
		padding-block: 0;
		padding-inline: 0.6rem;
		font-size: 0.8rem;
	}

	.studio-dialog__field input.studio-dialog__file {
		height: auto;
		padding: 0.35rem 0.6rem;
	}

	.studio-dialog__file::file-selector-button {
		margin-right: 0.6rem;
		border: 1px solid var(--studio-line);
		border-radius: 0.3rem;
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
		padding: 0.2rem 0.6rem;
		font: inherit;
	}

	.studio-dialog__field small {
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-dialog__check {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		font-size: 0.78rem;
	}

	.studio-dialog__error {
		margin: 0;
		color: var(--color-error-600-400) !important;
		font-size: 0.75rem;
	}

	.studio-dialog__footer button {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
	}
</style>
