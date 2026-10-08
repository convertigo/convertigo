<script>
	import Ico from '#lib/utils/Ico.svelte';
	import { call, toaster } from '#lib/utils/service.js';

	/**
	 * Imports a remote REST or SOAP web service into a project, as the web service reference wizards of
	 * the Eclipse Studio: an HTTP connector with a transaction per operation, and a sequence per
	 * transaction if asked. The definition is an URL or a file of the user, which the project keeps.
	 *
	 * @type {{ projectName: string, onDone?: (id: string) => void | Promise<void>, onClose?: () => void }}
	 */
	let { projectName, onDone, onClose } = $props();

	/** @type {'rest' | 'soap'} */
	let type = $state('rest');
	let url = $state('');
	/** @type {'url' | 'file'} */
	let source = $state('url');
	/** @type {File | null} */
	let definition = $state(null);
	let ready = $derived(source === 'url' ? Boolean(url.trim()) : Boolean(definition));
	let authenticated = $state(false);
	let user = $state('');
	let password = $state('');
	let sequences = $state('');
	let sequencesAuthenticated = $state(false);
	let busy = $state(false);
	let error = $state('');

	async function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		if (busy || !ready) {
			return;
		}
		busy = true;
		error = '';
		try {
			/** @type {Record<string, string>} */
			const params = {
				projectName,
				type,
				...(sequences ? { sequences, sequencesAuthenticated: String(sequencesAuthenticated) } : {})
			};
			/** @type {any} */
			let data;
			if (source === 'file' && definition) {
				data = new FormData();
				for (const [key, value] of Object.entries(params)) {
					data.append(key, value);
				}
				data.append('definition', definition, definition.name);
			} else {
				data = { ...params, url: url.trim(), ...(authenticated ? { user, password } : {}) };
			}
			const result = await call('studio.project.ImportWsReference', data);
			if (result?.done) {
				toaster.success({
					description: `The web service is imported with ${result.transactions} transaction${result.transactions > 1 ? 's' : ''}.`
				});
				await onDone?.(String(result.id ?? ''));
			} else {
				error = String(
					result?.error?.message ?? result?.message ?? 'The web service was not imported.'
				);
			}
		} finally {
			busy = false;
			password = '';
		}
	}
</script>

<div class="studio-dialog" role="presentation" onclick={() => !busy && onClose?.()}>
	<div
		class="studio-dialog__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-ws-import-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape' && !busy) onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-ws-import-title">Import a web service into {projectName}</strong>
		</header>
		<form class="studio-dialog__form" onsubmit={submit}>
			<fieldset class="studio-dialog__body" disabled={busy}>
				<div class="studio-dialog__modes" role="radiogroup" aria-label="Kind of web service">
					<button
						type="button"
						role="radio"
						aria-checked={type === 'rest'}
						class={['studio-dialog__mode', type === 'rest' && 'studio-dialog__mode--active']}
						onclick={() => (type = 'rest')}>REST (Swagger, OpenAPI)</button
					>
					<button
						type="button"
						role="radio"
						aria-checked={type === 'soap'}
						class={['studio-dialog__mode', type === 'soap' && 'studio-dialog__mode--active']}
						onclick={() => (type = 'soap')}>SOAP (WSDL)</button
					>
				</div>
				<div class="studio-dialog__modes" role="radiogroup" aria-label="Source of the definition">
					<button
						type="button"
						role="radio"
						aria-checked={source === 'url'}
						class={['studio-dialog__mode', source === 'url' && 'studio-dialog__mode--active']}
						onclick={() => (source = 'url')}>From a URL</button
					>
					<button
						type="button"
						role="radio"
						aria-checked={source === 'file'}
						class={['studio-dialog__mode', source === 'file' && 'studio-dialog__mode--active']}
						onclick={() => (source = 'file')}>From a file</button
					>
				</div>
				{#if source === 'url'}
					<label class="studio-dialog__field">
						<span>URL of the {type === 'rest' ? 'Swagger or OpenAPI definition' : 'WSDL'}</span>
						<!-- svelte-ignore a11y_autofocus -->
						<input
							class="input-common"
							bind:value={url}
							autofocus
							placeholder={type === 'rest'
								? 'https://api.example.com/openapi.json'
								: 'https://example.com/service?wsdl'}
						/>
					</label>
					<label class="studio-dialog__check">
						<input type="checkbox" bind:checked={authenticated} /> The definition needs an authentication
					</label>
				{:else}
					<label class="studio-dialog__field">
						<span>{type === 'rest' ? 'Swagger or OpenAPI definition' : 'WSDL'} file</span>
						<input
							class="studio-dialog__file input-common"
							type="file"
							accept={type === 'rest' ? '.json,.yaml,.yml' : '.wsdl,.xml'}
							onchange={(event) => (definition = event.currentTarget.files?.[0] ?? null)}
						/>
						<small
							>The project keeps the file in its {type === 'rest' ? 'openapi' : 'wsdl'} folder.</small
						>
					</label>
				{/if}
				{#if source === 'url' && authenticated}
					<label class="studio-dialog__field">
						<span>User</span>
						<input class="input-common" bind:value={user} autocomplete="username" />
					</label>
					<label class="studio-dialog__field">
						<span>Password</span>
						<input
							class="input-common"
							type="password"
							bind:value={password}
							autocomplete="current-password"
						/>
					</label>
				{/if}
				<label class="studio-dialog__field">
					<span>Sequences</span>
					<select class="select-common" bind:value={sequences}>
						<option value="">No sequence</option>
						<option value="Public">A public sequence per transaction</option>
						<option value="Hidden">A hidden sequence per transaction</option>
						<option value="Private">A private sequence per transaction</option>
					</select>
				</label>
				{#if sequences}
					<label class="studio-dialog__check">
						<input type="checkbox" bind:checked={sequencesAuthenticated} /> The sequences require an authenticated
						context
					</label>
				{/if}
				{#if error}
					<p class="studio-dialog__error" role="alert">{error}</p>
				{/if}
			</fieldset>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" disabled={busy} onclick={() => onClose?.()}
					>Cancel</button
				>
				<button type="submit" class="button-primary" disabled={busy || !ready}>
					{#if busy}<Ico icon="mdi:sync" size={4} />{/if}
					Import
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
		width: min(32rem, 100%);
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
		min-width: 0;
		display: grid;
		align-content: start;
		gap: 0.7rem;
		margin: 0;
		border: 0;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-dialog__modes {
		display: flex;
		width: fit-content;
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

	.studio-dialog__field {
		display: grid;
		gap: 0.3rem;
	}

	.studio-dialog__field > span {
		font-weight: 600;
	}

	.studio-dialog__field :is(input, select) {
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
	}

	.studio-dialog__error {
		margin: 0;
		color: var(--color-error-600-400);
		font-size: 0.75rem;
	}

	.studio-dialog__footer button {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
	}
</style>
