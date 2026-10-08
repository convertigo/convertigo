<script>
	import Ico from '#lib/utils/Ico.svelte';
	import { call, getUrl } from '#lib/utils/service.js';

	/**
	 * About Convertigo, as the About dialog and the Convertigo menu of the Eclipse Studio: the versions of
	 * the engine and its Java, the documentation and the Swagger console of all the projects.
	 *
	 * @type {{ onClose?: () => void }}
	 */
	let { onClose } = $props();

	let status = $state(/** @type {Record<string, any> | null} */ (null));

	$effect(() => {
		void call('engine.JsonStatus').then((result) => {
			status = result?.product ? result : {};
		});
	});

	let rows = $derived(
		status
			? [
					['Version', status.product],
					['Engine', status.engine],
					['Beans', status.beans],
					['Build', status.buildDate],
					['Licence', status.licenceType],
					['Java', [status.javaVersion, status.javaVendor].filter(Boolean).join(' ')],
					[
						'System',
						[status.osName, status.osVersion, status.osArchitecture].filter(Boolean).join(' ')
					]
				].filter(([, value]) => value)
			: []
	);

	function openSwagger() {
		// the REST API of all the projects, as the "Open Swagger console" of the Eclipse Studio
		const openapi = new URL(getUrl('openapi?YAML'), location.href);
		window.open(
			getUrl(`swagger/dist/index.html?url=${encodeURIComponent(openapi.href)}&showErrors`),
			'_blank'
		);
	}
</script>

<div class="studio-dialog" role="presentation" onclick={() => onClose?.()}>
	<div
		class="studio-dialog__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-about-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape') onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-about-title">About Convertigo</strong>
		</header>
		<div class="studio-dialog__body">
			{#if !status}
				<p class="studio-dialog__hint"><Ico icon="mdi:sync" size={4} /> Reading the versions…</p>
			{:else}
				<dl class="studio-about__rows">
					{#each rows as [label, value] (label)}
						<dt>{label}</dt>
						<dd>{value}</dd>
					{/each}
				</dl>
			{/if}
			<div class="studio-about__links">
				<a
					class="button-secondary"
					href="https://doc.convertigo.com"
					target="_blank"
					rel="noopener"
				>
					<Ico icon="mdi:book-open-variant" size={4} /> Documentation
				</a>
				<button type="button" class="button-secondary" onclick={openSwagger}>
					<Ico icon="mdi:api" size={4} /> Swagger console
				</button>
			</div>
		</div>
		<footer class="studio-dialog__footer">
			<button type="button" class="button-primary" onclick={() => onClose?.()}>Close</button>
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
		width: min(28rem, 100%);
		max-height: min(36rem, 100%);
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
		min-width: 0;
		min-height: 0;
		align-content: start;
		gap: 1rem;
		overflow-y: auto;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-about__rows {
		display: grid;
		grid-template-columns: auto minmax(0, 1fr);
		gap: 0.3rem 1rem;
		margin: 0;
	}

	.studio-about__rows dt {
		color: var(--studio-text-idle);
	}

	.studio-about__rows dd {
		margin: 0;
		overflow-wrap: anywhere;
	}

	.studio-about__links {
		display: flex;
		flex-wrap: wrap;
		gap: 0.5rem;
	}

	.studio-about__links > * {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
		font-size: 0.78rem;
	}

	.studio-dialog__hint {
		display: flex;
		align-items: center;
		gap: 0.4rem;
		margin: 0;
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}
</style>
