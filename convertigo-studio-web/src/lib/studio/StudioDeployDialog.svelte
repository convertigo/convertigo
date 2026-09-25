<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { call } from '$lib/utils/service';

	/**
	 * Deploys a project on a remote Convertigo server, as the deployment wizard of the Eclipse Studio. The
	 * servers already used are remembered in this browser, without their password.
	 *
	 * @typedef {{ server: string, https: boolean, trustAllCertificates: boolean, user: string }} DeployServer
	 */

	/** @type {{ projectName: string, onClose?: () => void }} */
	let { projectName, onClose } = $props();

	const STORAGE_KEY = 'convertigo.studio.deploy.servers';

	/** @type {DeployServer[]} */
	const servers = readServers();
	let server = $state(servers[0]?.server ?? '');
	let https = $state(servers[0]?.https ?? true);
	let trustAllCertificates = $state(servers[0]?.trustAllCertificates ?? false);
	let user = $state(servers[0]?.user ?? '');
	let password = $state('');
	let assembleXsl = $state(false);
	let busy = $state(false);
	let error = $state('');
	/** @type {{ dashboard?: string, application?: string } | null} */
	let deployed = $state(null);

	/** @returns {DeployServer[]} */
	function readServers() {
		try {
			const value = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]');
			return Array.isArray(value) ? value : [];
		} catch {
			return [];
		}
	}

	function rememberServer() {
		const entry = { server: server.trim(), https, trustAllCertificates, user: user.trim() };
		const next = [entry, ...servers.filter((candidate) => candidate.server !== entry.server)];
		try {
			localStorage.setItem(STORAGE_KEY, JSON.stringify(next.slice(0, 10)));
		} catch {
			// the list of servers is a convenience of this browser
		}
	}

	/**
	 * @param {string} value
	 */
	function chooseServer(value) {
		const known = servers.find((candidate) => candidate.server === value);
		if (known) {
			https = known.https;
			trustAllCertificates = known.trustAllCertificates;
			user = known.user;
		}
	}

	async function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		if (busy || !server.trim() || !user.trim() || !password) {
			return;
		}
		busy = true;
		error = '';
		try {
			const result = await call('studio.project.Deploy', {
				projectName,
				server: server.trim(),
				https: String(https),
				trustAllCertificates: String(trustAllCertificates),
				user: user.trim(),
				password,
				assembleXsl: String(assembleXsl)
			});
			if (result?.done) {
				rememberServer();
				deployed = { dashboard: result.dashboard, application: result.application };
			} else {
				error = String(result?.error?.message ?? result?.message ?? 'The deployment failed.');
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
		aria-labelledby="studio-deploy-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape' && !busy) onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-deploy-title">Deploy {projectName}</strong>
		</header>
		{#if deployed}
			<div class="studio-dialog__body">
				<p>{projectName} is deployed on {server}.</p>
				{#if deployed.dashboard}
					<a href={deployed.dashboard} target="_blank" rel="noopener">
						<Ico icon="mdi:open-in-new-variant" size={4} /> Backend dashboard
					</a>
				{/if}
				{#if deployed.application}
					<a href={deployed.application} target="_blank" rel="noopener">
						<Ico icon="mdi:open-in-new-variant" size={4} /> Frontend application
					</a>
				{/if}
			</div>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-primary" onclick={() => onClose?.()}>Close</button>
			</footer>
		{:else}
			<form class="studio-dialog__form" onsubmit={submit}>
				<fieldset class="studio-dialog__body" disabled={busy}>
					<label class="studio-dialog__field">
						<span>Convertigo server</span>
						<!-- svelte-ignore a11y_autofocus -->
						<input
							class="input-common"
							list="studio-deploy-servers"
							bind:value={server}
							onchange={() => chooseServer(server)}
							placeholder="myserver.example.com/convertigo"
							autofocus
						/>
						<datalist id="studio-deploy-servers">
							{#each servers as candidate (candidate.server)}
								<option value={candidate.server}></option>
							{/each}
						</datalist>
					</label>
					<label class="studio-dialog__check">
						<input type="checkbox" bind:checked={https} /> HTTPS
					</label>
					<label class="studio-dialog__check">
						<input type="checkbox" bind:checked={trustAllCertificates} /> Trust all certificates
					</label>
					<label class="studio-dialog__field">
						<span>Administrator of the server</span>
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
					<label class="studio-dialog__check">
						<input type="checkbox" bind:checked={assembleXsl} /> Assemble the XSL sheets (legacy web clipping
						projects)
					</label>
					{#if error}
						<p class="studio-dialog__error" role="alert">{error}</p>
					{/if}
				</fieldset>
				<footer class="studio-dialog__footer">
					<button type="button" class="button-secondary" disabled={busy} onclick={() => onClose?.()}
						>Cancel</button
					>
					<button
						type="submit"
						class="button-primary"
						disabled={busy || !server.trim() || !user.trim() || !password}
					>
						{#if busy}<Ico icon="mdi:sync" size={4} />{/if}
						Deploy
					</button>
				</footer>
			</form>
		{/if}
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
		display: grid;
		align-content: start;
		gap: 0.7rem;
		margin: 0;
		border: 0;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-dialog__body a {
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
		color: var(--color-primary-500);
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
		padding-inline: 0.6rem;
		font-size: 0.8rem;
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
