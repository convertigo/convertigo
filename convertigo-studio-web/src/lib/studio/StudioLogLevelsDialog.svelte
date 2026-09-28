<script>
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import Configuration from '$lib/admin/Configuration.svelte';

	/**
	 * The levels of the logs of the engine, as the "Configure Log level" of the engine log view of the
	 * Eclipse Studio: the log4j levels of the Logs settings of the engine.
	 *
	 * @type {{ onClose?: () => void }}
	 */
	let { onClose } = $props();

	let saving = $state(false);
	let levels = $derived(
		Configuration.categories
			.find((/** @type {any} */ category) => category?.name == 'Logs')
			?.property?.filter((/** @type {any} */ property) => property?.name?.startsWith('LOG4J')) ?? []
	);
	let changed = $derived(
		levels.filter((/** @type {any} */ property) => property.value != property.originalValue)
	);

	async function save() {
		if (!changed.length || saving) {
			return;
		}
		saving = true;
		try {
			await Configuration.updateConfigurations(
				changed.map((/** @type {any} */ property) => ({
					'@_key': property.name,
					'@_value': property.value
				}))
			);
			onClose?.();
		} finally {
			saving = false;
		}
	}
</script>

<div class="studio-dialog" role="presentation" onclick={() => !saving && onClose?.()}>
	<div
		class="studio-dialog__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-log-levels-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape' && !saving) onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-log-levels-title">Log levels</strong>
		</header>
		<div class="studio-dialog__body">
			{#each levels as property (property.name)}
				<div class="studio-log-levels__property">
					<PropertyType {...property} bind:value={property.value} />
				</div>
			{:else}
				<p class="studio-dialog__hint">Loading the levels…</p>
			{/each}
		</div>
		<footer class="studio-dialog__footer">
			<button type="button" class="button-secondary" disabled={saving} onclick={() => onClose?.()}
				>Cancel</button
			>
			<button
				type="button"
				class="button-primary"
				disabled={saving || !changed.length}
				onclick={() => void save()}>Save</button
			>
		</footer>
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
		max-height: 100%;
		grid-template-rows: auto minmax(0, 1fr) auto;
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
		min-height: 0;
		grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr));
		gap: 0.6rem;
		overflow-y: auto;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-dialog__hint {
		margin: 0;
		color: var(--studio-text-idle);
	}
</style>
