<script>
	import Light from '#lib/common/Light.svelte.js';
	import { untrack } from 'svelte';
	import { pscOfMessage, registration, registrationUrl, savePsc } from './registration.svelte.js';
	import StudioProxyDialog from './StudioProxyDialog.svelte';

	/**
	 * The registration of the Studio, as the Convertigo Setup wizard of the Eclipse Studio: the Convertigo Cloud
	 * page where the user registers or logs in gives the Personal Studio Configuration (PSC) back to the Electron
	 * Studio, or the user pastes the PSC received by email. The engine writes it in the workspace (studio/psc.txt).
	 *
	 * @type {{ onClose?: () => void }}
	 */
	let { onClose } = $props();

	let frame = $state(/** @type {HTMLIFrameElement | undefined} */ (undefined));
	let paste = $state(false);
	let psc = $state('');
	let busy = $state(false);
	let message = $state('');
	let failed = $state(false);
	/** the proxy of the engine, as the Configure proxy step of the Convertigo Setup wizard */
	let proxyOpen = $state(false);
	/** a registered Studio shows its owner, until the user changes its PSC */
	let changing = $state(false);
	let current = $derived(registration.state);
	let registered = $derived(current?.registered === true && !changing);
	/** the theme of the page when the dialog opened: a change would load the page again */
	const url = registrationUrl(untrack(() => Light.dark));

	/**
	 * @param {string} value
	 */
	async function save(value) {
		if (busy || !value) {
			return;
		}
		busy = true;
		message = '';
		failed = false;
		try {
			const result = await savePsc(value);
			if (result.saved) {
				changing = false;
				paste = false;
				psc = '';
				// a registered Studio shows its owner; a PSC without owner does not register it
				message = result.registered
					? ''
					: 'The PSC is saved, but it has no owner: register or log in to use the Convertigo agent.';
				failed = !result.registered;
			} else {
				message = result.invalid || 'The PSC cannot be saved.';
				failed = true;
			}
		} finally {
			busy = false;
		}
	}

	/**
	 * @param {MessageEvent} event
	 */
	function onMessage(event) {
		const value = pscOfMessage(event, frame?.contentWindow);
		if (value) {
			void save(value);
		}
	}

	function submit(/** @type {SubmitEvent} */ event) {
		event.preventDefault();
		void save(psc.trim());
	}
</script>

<svelte:window onmessage={onMessage} />

<div class="studio-dialog" role="presentation" onclick={() => onClose?.()}>
	<div
		class="studio-dialog__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-registration-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape') onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-registration-title">Personal Studio Configuration</strong>
		</header>
		<form class="studio-dialog__form" onsubmit={submit}>
			<div class="studio-dialog__body">
				{#if registered}
					<p>The Studio is registered for <strong>{current?.owner}</strong>.</p>
					{#if current?.servers?.length}
						<p class="studio-dialog__hint">
							It deploys to {current.servers.join(', ')}.
						</p>
					{/if}
				{:else}
					<p>
						Register or log in to your Convertigo account: the Studio gets its Personal Studio
						Configuration (PSC), which the Convertigo agent of the Assistant needs, with the
						Convertigo Cloud servers it deploys to.
					</p>
					{#if current?.anonymous}
						<p class="studio-dialog__hint">The PSC of this workspace has no owner.</p>
					{:else if current?.error}
						<p class="studio-dialog__hint">{current.error}</p>
					{/if}
					{#if paste}
						<label class="studio-dialog__field">
							<span>PSC</span>
							<textarea
								class="input-common"
								bind:value={psc}
								rows="8"
								spellcheck="false"
								placeholder="Paste the PSC received by email"></textarea>
						</label>
					{:else}
						<iframe
							bind:this={frame}
							src={url}
							title="Convertigo registration"
							class="studio-registration"
						></iframe>
						<p class="studio-dialog__hint">After signing up, you also receive your PSC by email.</p>
					{/if}
					<label class="studio-dialog__check">
						<input type="checkbox" bind:checked={paste} />
						<span>I want to paste my PSC</span>
					</label>
				{/if}
				{#if message}
					<p
						class="studio-dialog__message"
						class:studio-dialog__message--error={failed}
						role="status"
					>
						{message}
					</p>
				{/if}
			</div>
			<footer class="studio-dialog__footer">
				{#if registered}
					<button type="button" class="button-secondary" onclick={() => (changing = true)}>
						Change the PSC
					</button>
					<button type="button" class="button-primary" onclick={() => onClose?.()}>Close</button>
				{:else}
					<button
						type="button"
						class="studio-dialog__proxy button-secondary"
						onclick={() => (proxyOpen = true)}
					>
						Network proxy…
					</button>
					<button type="button" class="button-secondary" onclick={() => onClose?.()}>
						{current?.registered ? 'Cancel' : 'Later'}
					</button>
					{#if paste}
						<button type="submit" class="button-primary" disabled={busy || !psc.trim()}>
							Validate
						</button>
					{/if}
				{/if}
			</footer>
		</form>
	</div>
</div>

{#if proxyOpen}
	<StudioProxyDialog onClose={() => (proxyOpen = false)} />
{/if}

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
		width: min(46rem, 100%);
		max-height: min(42rem, 100%);
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

	.studio-dialog__proxy {
		margin-inline-end: auto;
	}

	.studio-dialog__body {
		display: grid;
		min-width: 0;
		min-height: 0;
		align-content: start;
		gap: 0.75rem;
		overflow-y: auto;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-dialog__body p {
		margin: 0;
	}

	.studio-dialog__hint {
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-dialog__message {
		color: var(--studio-text-strong);
	}

	.studio-dialog__message--error {
		color: var(--color-error-500);
	}

	.studio-dialog__check {
		display: flex;
		align-items: center;
		gap: 0.5rem;
	}

	.studio-dialog__field {
		display: grid;
		gap: 0.3rem;
	}

	.studio-dialog__field > span {
		font-weight: 600;
	}

	.studio-dialog__field textarea {
		padding: 0.5rem 0.6rem;
		font-family: var(--font-mono, monospace);
		font-size: 0.72rem;
		word-break: break-all;
	}

	.studio-registration {
		display: block;
		width: 100%;
		height: 24rem;
		border: 1px solid var(--studio-line);
		border-radius: 0.35rem;
		background: white;
	}
</style>
