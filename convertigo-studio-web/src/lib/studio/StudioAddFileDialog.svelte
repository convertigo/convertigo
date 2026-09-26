<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { call } from '$lib/utils/service';

	/**
	 * Adds to a project a file of the engine, as the "Add files" wizard of the Eclipse Studio: the
	 * stylesheets handling the errors of Convertigo and the minimal one of web clipping.
	 *
	 * @type {{ projectName: string, onDone?: (fileId: string) => void | Promise<void>, onClose?: () => void }}
	 */
	let { projectName, onDone, onClose } = $props();

	/** @type {{ name: string, description: string }[]} */
	let files = $state([]);
	let chosen = $state('');
	let busy = $state(false);

	$effect(() => {
		void call('studio.project.AddFile', {}).then((result) => {
			files = Array.isArray(result?.files) ? result.files : [];
			chosen = files[0]?.name ?? '';
		});
	});

	async function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		if (!chosen || busy) {
			return;
		}
		busy = true;
		try {
			let result = await call('studio.project.AddFile', { projectName, name: chosen });
			if (result?.exists) {
				if (
					!window.confirm(`The project ${projectName} already has the file ${chosen}. Replace it?`)
				) {
					return;
				}
				result = await call('studio.project.AddFile', {
					projectName,
					name: chosen,
					overwrite: 'true'
				});
			}
			if (result?.done) {
				await onDone?.(`${projectName}/${result.file}`);
			}
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
		aria-labelledby="studio-add-file-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape' && !busy) onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-add-file-title">Add a file to {projectName}</strong>
		</header>
		<form class="studio-dialog__form" onsubmit={submit}>
			<fieldset class="studio-dialog__body" disabled={busy}>
				<legend class="studio-dialog__legend">XSL files</legend>
				{#each files as file (file.name)}
					<label class="studio-dialog__choice">
						<input type="radio" name="file" value={file.name} bind:group={chosen} />
						<span>
							<strong>{file.name}</strong>
							<small>{file.description}</small>
						</span>
					</label>
				{/each}
			</fieldset>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" disabled={busy} onclick={() => onClose?.()}
					>Cancel</button
				>
				<button type="submit" class="button-primary" disabled={busy || !chosen}>
					{#if busy}<Ico icon="mdi:sync" size={4} />{/if}
					Add
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

	.studio-dialog__footer button {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
	}

	.studio-dialog__body {
		display: grid;
		min-width: 0;
		min-height: 0;
		align-content: start;
		gap: 0.6rem;
		overflow-y: auto;
		margin: 0;
		border: 0;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-dialog__legend {
		padding: 0;
		margin-bottom: 0.2rem;
		font-weight: 600;
	}

	.studio-dialog__choice {
		display: flex;
		align-items: flex-start;
		gap: 0.5rem;
	}

	.studio-dialog__choice span {
		display: grid;
		gap: 0.1rem;
	}

	.studio-dialog__choice small {
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}
</style>
