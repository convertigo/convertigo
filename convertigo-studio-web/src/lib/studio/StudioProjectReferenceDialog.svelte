<script>
	import Projects from '#lib/common/Projects.svelte.js';
	import { formatProjectReference, parseProjectReference } from './projectReference';

	/**
	 * Edits the reference of a project, as the project reference editor of the Eclipse Studio: a project
	 * of the workspace by its name, or a project of a Git repository or of an archive by its remote URL,
	 * with its path, its branch and its automatic reset and pull.
	 *
	 * @type {{
	 *  label?: string,
	 *  value?: string,
	 *  onApply?: (value: string) => void,
	 *  onClose?: () => void
	 * }}
	 */
	let { label = 'Project reference', value = '', onApply, onClose } = $props();

	let reference = $state(parseProjectReference(''));
	let whole = $state('');

	$effect.pre(() => {
		reference = parseProjectReference(value);
		whole = value;
	});

	let projectNames = $derived(
		(Projects.projects ?? []).map((project) => String(project.name ?? '')).filter(Boolean)
	);

	/**
	 * @param {Partial<import('./projectReference').ProjectReference>} changes
	 */
	function change(changes) {
		reference = { ...reference, ...changes };
		whole = formatProjectReference(reference);
	}
</script>

<div class="studio-dialog" role="presentation" onclick={() => onClose?.()}>
	<div
		class="studio-dialog__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-reference-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape') onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-reference-title">{label}</strong>
		</header>
		<form
			class="studio-dialog__form"
			onsubmit={(event) => {
				event.preventDefault();
				onApply?.(whole.trim());
			}}
		>
			<div class="studio-dialog__body">
				<p class="studio-dialog__hint">
					A project is referenced by its name, or by its remote URL:
					<code><project name>=<git or http URL>[:path=…][:branch=…]</git></project></code>
				</p>
				<label class="studio-dialog__field">
					<span>Project remote URL</span>
					<input
						class="input-common"
						value={whole}
						oninput={(event) => {
							whole = event.currentTarget.value;
							reference = parseProjectReference(whole);
						}}
					/>
				</label>
				<hr />
				<label class="studio-dialog__field">
					<span>Project name</span>
					<input
						class="input-common"
						list="studio-reference-projects"
						value={reference.projectName}
						oninput={(event) => change({ projectName: event.currentTarget.value })}
					/>
					<datalist id="studio-reference-projects">
						{#each projectNames as name (name)}
							<option value={name}></option>
						{/each}
					</datalist>
				</label>
				<label class="studio-dialog__field">
					<span>Git or http URL</span>
					<input
						class="input-common"
						value={reference.url}
						placeholder="https://github.com/…/project.git"
						oninput={(event) => change({ url: event.currentTarget.value })}
					/>
				</label>
				<div class="studio-dialog__row">
					<label class="studio-dialog__field">
						<span>Project path</span>
						<input
							class="input-common"
							value={reference.path}
							disabled={!reference.url}
							oninput={(event) => change({ path: event.currentTarget.value })}
						/>
					</label>
					<label class="studio-dialog__field">
						<span>Git branch</span>
						<input
							class="input-common"
							value={reference.branch}
							disabled={!reference.url}
							oninput={(event) => change({ branch: event.currentTarget.value })}
						/>
					</label>
				</div>
				<label class="studio-dialog__check">
					<input
						type="checkbox"
						checked={reference.autoPull}
						disabled={!reference.url}
						onchange={(event) => change({ autoPull: event.currentTarget.checked })}
					/>
					Reset and pull the repository automatically
				</label>
			</div>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" onclick={() => onClose?.()}>Cancel</button>
				<button type="submit" class="button-primary" disabled={!reference.projectName}>Apply</button
				>
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
		display: grid;
		gap: 0.7rem;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-dialog__body hr {
		margin: 0;
		border: 0;
		border-top: 1px solid var(--studio-line);
	}

	.studio-dialog__hint {
		margin: 0;
		color: var(--studio-text-idle);
	}

	.studio-dialog__row {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 0.6rem;
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

	.studio-dialog__check {
		display: flex;
		align-items: center;
		gap: 0.4rem;
	}
</style>
