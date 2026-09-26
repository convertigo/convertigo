<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { call } from '$lib/utils/service';

	/**
	 * Chooses the endpoint of a mobile application, as the endpoint editor of the Eclipse Studio: the URL
	 * the application reaches the Convertigo server with, among this Studio, its local addresses and the
	 * servers it deploys to, empty for the default endpoint of the engine.
	 *
	 * @type {{
	 *  value?: string,
	 *  onApply?: (value: string) => void,
	 *  onClose?: () => void
	 * }}
	 */
	let { value = '', onApply, onClose } = $props();

	let endpoint = $state('');
	let defaultEndpoint = $state('');
	/** @type {{ url: string, from: string }[]} */
	let endpoints = $state([]);
	let busy = $state(false);

	$effect.pre(() => {
		endpoint = value;
	});

	/**
	 * @returns {{ url: string, from: string }[]} the servers of the deployments of this browser
	 */
	function deploymentEndpoints() {
		try {
			const servers = JSON.parse(localStorage.getItem('convertigo.studio.deploy.servers') ?? '[]');
			return (Array.isArray(servers) ? servers : [])
				.filter((server) => typeof server?.server === 'string' && server.server)
				.map((server) => ({
					url: `http${server.https === false ? '' : 's'}://${server.server}`,
					from: 'from deployment'
				}));
		} catch {
			return [];
		}
	}

	/**
	 * @param {Record<string, string>} [params]
	 */
	async function load(params = {}) {
		busy = true;
		try {
			const result = await call('studio.project.Endpoints', params);
			defaultEndpoint = String(result?.default ?? '');
			const found = [
				...deploymentEndpoints(),
				...(Array.isArray(result?.endpoints) ? result.endpoints : [])
			];
			endpoints = found.filter(
				(candidate, index) => found.findIndex((other) => other.url === candidate.url) === index
			);
		} finally {
			busy = false;
		}
	}

	$effect(() => {
		void load();
	});

	async function updateDefault() {
		await load({ action: 'setDefault', value: endpoint });
		endpoint = '';
	}
</script>

<div class="studio-dialog" role="presentation" onclick={() => !busy && onClose?.()}>
	<div
		class="studio-dialog__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-endpoint-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape' && !busy) onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-endpoint-title">Mobile endpoint</strong>
		</header>
		<form
			class="studio-dialog__form"
			onsubmit={(event) => {
				event.preventDefault();
				onApply?.(endpoint.trim());
			}}
		>
			<div class="studio-dialog__body">
				<p class="studio-dialog__hint">
					The URL the mobile application uses to reach the Convertigo server: this Studio or one of
					its local addresses to test with its sequences, a deployment server for production.
				</p>
				<label class="studio-dialog__field">
					<span>Endpoint</span>
					<!-- svelte-ignore a11y_autofocus -->
					<input
						class="input-common"
						bind:value={endpoint}
						autofocus
						placeholder={defaultEndpoint}
					/>
				</label>
				<div class="studio-dialog__default">
					<small
						>An empty endpoint is the default endpoint of the engine: <code
							>{defaultEndpoint || 'none'}</code
						></small
					>
					<button
						type="button"
						class="button-secondary"
						disabled={busy || !endpoint.trim()}
						onclick={updateDefault}>Make it the default endpoint</button
					>
				</div>
				<div class="studio-dialog__field" role="group" aria-label="Known endpoints">
					<span>Known endpoints</span>
					<ul class="studio-dialog__endpoints">
						{#each endpoints as candidate (candidate.url)}
							<li>
								<button
									type="button"
									class={[endpoint === candidate.url && 'studio-dialog__endpoint--active']}
									onclick={() => (endpoint = candidate.url)}
								>
									<code>{candidate.url}</code>
									<small>{candidate.from}</small>
								</button>
							</li>
						{/each}
					</ul>
				</div>
			</div>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" onclick={() => onClose?.()}>Cancel</button>
				<button type="submit" class="button-primary" disabled={busy}>
					{#if busy}<Ico icon="mdi:sync" size={4} />{/if}
					Apply
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
		width: min(34rem, 100%);
		max-height: 90vh;
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
		align-content: start;
		gap: 0.8rem;
		overflow: auto;
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

	.studio-dialog__field input {
		height: 2rem;
		padding-block: 0;
		padding-inline: 0.6rem;
		font-size: 0.8rem;
	}

	.studio-dialog__default {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		justify-content: space-between;
		gap: 0.5rem;
		color: var(--studio-text-idle);
	}

	.studio-dialog__endpoints {
		display: grid;
		gap: 0.2rem;
		margin: 0;
		padding: 0;
		list-style: none;
	}

	.studio-dialog__endpoints button {
		display: flex;
		width: 100%;
		align-items: center;
		justify-content: space-between;
		gap: 0.6rem;
		border: 1px solid var(--studio-line);
		border-radius: 0.3rem;
		background: transparent;
		color: var(--studio-text);
		padding: 0.35rem 0.5rem;
		text-align: left;
	}

	.studio-dialog__endpoints button:hover,
	.studio-dialog__endpoint--active {
		border-color: var(--color-primary-500) !important;
	}

	.studio-dialog__endpoints small {
		color: var(--studio-text-idle);
		white-space: nowrap;
	}
</style>
