<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { call } from '$lib/utils/service';

	/**
	 * The version of a project and what its archive includes, asked before an export or a deployment as
	 * the Eclipse Studio does, and kept for the next ones.
	 *
	 * @type {{
	 *  projectName: string,
	 *  mode?: 'export' | 'deploy',
	 *  onContinue?: (result: { versionChanged: boolean, options: Record<string, boolean> }) => void | Promise<void>,
	 *  onClose?: () => void
	 * }}
	 */
	let { projectName, mode = 'export', onContinue, onClose } = $props();

	/** @type {{ name: string, display: string, size?: string, selected: boolean }[]} */
	let options = $state([]);
	let version = $state('');
	let loaded = $state(false);
	let busy = $state(false);

	$effect(() => {
		void call('studio.project.ArchiveOptions', { projectName }).then((result) => {
			options = Array.isArray(result?.options) ? result.options : [];
			version = String(result?.version ?? '');
			loaded = true;
		});
	});

	async function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		if (busy || !loaded) {
			return;
		}
		busy = true;
		try {
			const chosen = Object.fromEntries(options.map((option) => [option.name, option.selected]));
			const result = await call('studio.project.ArchiveOptions', {
				projectName,
				version,
				options: JSON.stringify(chosen)
			});
			if (Array.isArray(result?.options)) {
				await onContinue?.({ versionChanged: result.versionChanged === true, options: chosen });
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
		aria-labelledby="studio-archive-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape' && !busy) onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-archive-title"
				>{mode === 'deploy' ? 'Deploy' : 'Export'} {projectName}</strong
			>
		</header>
		<form class="studio-dialog__form" onsubmit={submit}>
			<fieldset class="studio-dialog__body" disabled={busy || !loaded}>
				<p class="studio-dialog__hint">
					You can update the version of the project before its {mode === 'deploy'
						? 'deployment'
						: 'export'}.
				</p>
				<label class="studio-dialog__field">
					<span>Version</span>
					<input class="input-common" bind:value={version} />
				</label>
				{#if options.length}
					<div class="studio-dialog__field" role="group" aria-label="Include">
						<span>Include</span>
						{#each options as option (option.name)}
							<label class="studio-dialog__check">
								<input type="checkbox" bind:checked={option.selected} />
								{option.display.replace(/^include /, '')}
								{#if option.size}<small>{option.size}</small>{/if}
							</label>
						{/each}
					</div>
				{/if}
			</fieldset>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" disabled={busy} onclick={() => onClose?.()}
					>Cancel</button
				>
				<button type="submit" class="button-primary" disabled={busy || !loaded}>
					{#if busy}<Ico icon="mdi:sync" size={4} />{/if}
					{mode === 'deploy' ? 'Continue' : 'Export'}
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
		width: min(28rem, 100%);
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

	.studio-dialog__footer button {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
	}

	.studio-dialog__body {
		display: grid;
		gap: 0.8rem;
		margin: 0;
		border: 0;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-dialog__hint {
		margin: 0;
		color: var(--studio-text-idle);
	}

	.studio-dialog__field {
		display: grid;
		gap: 0.3rem;
	}

	.studio-dialog__field > span {
		font-weight: 600;
	}

	.studio-dialog__field input:not([type='checkbox']) {
		height: 2rem;
		padding-block: 0;
		padding-inline: 0.6rem;
		font-size: 0.8rem;
	}

	.studio-dialog__check {
		display: flex;
		align-items: center;
		gap: 0.4rem;
	}

	.studio-dialog__check small {
		color: var(--studio-text-idle);
	}
</style>
