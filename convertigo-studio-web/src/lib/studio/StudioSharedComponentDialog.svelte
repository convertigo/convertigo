<script>
	import Ico from '#lib/utils/Ico.svelte';
	import { call } from '#lib/utils/service.js';

	/**
	 * @typedef {{ name: string, info: string, chosen: string }} FoundVariable
	 */

	/**
	 * Creates a shared component from an NGX component, as the shared component wizard of the Eclipse
	 * Studio: the variables of its page the component uses become variables of the shared component,
	 * whose names can change, and a use of the shared component takes its place.
	 *
	 * @type {{ id: string, onDone?: (id: string) => void | Promise<void>, onClose?: () => void }}
	 */
	let { id, onDone, onClose } = $props();

	let name = $state('');
	let keep = $state(true);
	/** @type {FoundVariable[]} */
	let variables = $state([]);
	let loading = $state(true);
	let busy = $state(false);
	let error = $state('');

	let componentName = $derived(id.split(/[.:]/).pop() ?? id);

	$effect(() => {
		load(id);
	});

	/**
	 * @param {string} target
	 */
	async function load(target) {
		loading = true;
		error = '';
		try {
			const result = await call('studio.ngxbuilder.SharedComponent', { id: target });
			name = String(result?.name ?? '');
			variables = (Array.isArray(result?.variables) ? result.variables : []).map(
				(/** @type {any} */ variable) => ({
					name: String(variable.name),
					info: String(variable.info ?? ''),
					chosen: String(variable.name)
				})
			);
			if (!result?.name) {
				error = String(
					result?.error?.message ?? 'This component cannot become a shared component.'
				);
			}
		} finally {
			loading = false;
		}
	}

	async function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		if (busy || !name.trim()) {
			return;
		}
		busy = true;
		error = '';
		try {
			const result = await call('studio.ngxbuilder.SharedComponent', {
				id,
				name: name.trim(),
				keep: String(keep),
				variables: JSON.stringify(
					Object.fromEntries(variables.map((variable) => [variable.name, variable.chosen.trim()]))
				)
			});
			if (result?.done) {
				await onDone?.(String(result.id ?? id));
			} else {
				error = String(result?.error?.message ?? 'The shared component was not created.');
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
		aria-labelledby="studio-shared-component-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape' && !busy) onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-shared-component-title">Shared component from {componentName}</strong>
		</header>
		<form class="studio-dialog__form" onsubmit={submit}>
			<fieldset class="studio-dialog__body" disabled={busy || loading}>
				{#if loading}
					<p class="studio-dialog__hint">
						<Ico icon="mdi:sync" size={4} /> Looking for its variables…
					</p>
				{:else}
					<label class="studio-dialog__field">
						<span>Name of the shared component</span>
						<input class="input-common" bind:value={name} />
					</label>
					<div class="studio-dialog__choices" role="radiogroup" aria-label="Original component">
						<label class="studio-dialog__check">
							<input type="radio" bind:group={keep} value={true} /> Keep the component, disabled
						</label>
						<label class="studio-dialog__check">
							<input type="radio" bind:group={keep} value={false} /> Remove the component
						</label>
					</div>
					{#if variables.length}
						<section class="studio-dialog__field">
							<span>Variables of the shared component</span>
							<div class="studio-shared__variables">
								{#each variables as variable (variable.name)}
									<label class="studio-shared__variable">
										<input
											class="input-common"
											aria-label="Name of the variable {variable.name}"
											bind:value={variable.chosen}
										/>
										<small>{variable.info}</small>
									</label>
								{/each}
							</div>
						</section>
					{:else}
						<p class="studio-dialog__hint">The component uses no variable of its page.</p>
					{/if}
					<p class="studio-dialog__hint">
						Its inner actions remain the same, its disabled components are ignored.
					</p>
				{/if}
				{#if error}
					<p class="studio-dialog__error" role="alert">{error}</p>
				{/if}
			</fieldset>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" disabled={busy} onclick={() => onClose?.()}
					>Cancel</button
				>
				<button type="submit" class="button-primary" disabled={busy || loading || !name.trim()}>
					{#if busy}<Ico icon="mdi:sync" size={4} />{/if}
					Create
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
		width: min(36rem, 100%);
		max-height: min(40rem, 100%);
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
		grid-template-columns: minmax(0, 1fr);
		align-content: start;
		gap: 0.8rem;
		overflow-y: auto;
		margin: 0;
		border: 0;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-dialog__field {
		display: grid;
		gap: 0.3rem;
	}

	.studio-dialog__field > span {
		font-weight: 600;
	}

	.studio-dialog__field > input,
	.studio-shared__variable input {
		height: 2rem;
		padding-block: 0;
		padding-inline: 0.6rem;
		font-size: 0.8rem;
	}

	.studio-dialog__choices {
		display: grid;
		gap: 0.35rem;
	}

	.studio-dialog__check {
		display: flex;
		align-items: center;
		gap: 0.45rem;
	}

	.studio-shared__variables {
		display: grid;
		gap: 0.5rem;
	}

	.studio-shared__variable {
		display: grid;
		gap: 0.15rem;
	}

	.studio-shared__variable small,
	.studio-dialog__hint {
		margin: 0;
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-dialog__hint {
		display: flex;
		align-items: center;
		gap: 0.4rem;
	}

	.studio-dialog__error {
		margin: 0;
		color: var(--color-error-600-400);
		font-size: 0.75rem;
	}
</style>
