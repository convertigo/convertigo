<script>
	import Ico from '$lib/utils/Ico.svelte';

	/**
	 * The credentials a remote asks to fetch, pull or push: a user and a password or a token, which the
	 * engine keeps in memory for the remote until it stops.
	 *
	 * @type {{
	 *  url: string | null,
	 *  onAnswer: (credentials: { username: string, password: string } | null) => void
	 * }}
	 */
	let { url, onAnswer } = $props();

	let username = $state('');
	let password = $state('');

	$effect.pre(() => {
		if (url) {
			password = '';
		}
	});

	/**
	 * @param {HTMLInputElement} node
	 */
	function focus(node) {
		node.focus();
	}

	/**
	 * Shows the dialog over the whole window, out of the view whose panel would contain it.
	 * @param {HTMLElement} node
	 */
	function portal(node) {
		node.ownerDocument.body.appendChild(node);
		return {
			destroy() {
				node.remove();
			}
		};
	}
</script>

{#if url}
	<div class="studio-dialog" role="presentation" use:portal onclick={() => onAnswer(null)}>
		<div
			class="studio-dialog__box"
			role="dialog"
			aria-modal="true"
			aria-labelledby="studio-credentials-title"
			tabindex="-1"
			onclick={(event) => event.stopPropagation()}
			onkeydown={(event) => {
				event.stopPropagation();
				if (event.key === 'Escape') onAnswer(null);
			}}
		>
			<form
				onsubmit={(event) => {
					event.preventDefault();
					onAnswer({ username: username.trim(), password });
				}}
			>
				<div class="studio-dialog__body">
					<div class="studio-credentials__title">
						<Ico icon="mdi:key-outline" size={4} />
						<strong id="studio-credentials-title">The remote asks who you are</strong>
					</div>
					<small class="studio-credentials__url">{url}</small>
					<label class="studio-credentials__field">
						<span>User</span>
						<input class="input-common" autocomplete="username" bind:value={username} use:focus />
					</label>
					<label class="studio-credentials__field">
						<span>Password or token</span>
						<input
							class="input-common"
							type="password"
							autocomplete="current-password"
							bind:value={password}
						/>
					</label>
					<small class="studio-credentials__note"
						>Kept in memory for this remote until the Studio stops, never written.</small
					>
				</div>
				<footer class="studio-dialog__footer">
					<button type="button" class="button-secondary" onclick={() => onAnswer(null)}
						>Cancel</button
					>
					<button type="submit" class="button-primary" disabled={!password}>Sign in</button>
				</footer>
			</form>
		</div>
	</div>
{/if}

<style>
	.studio-dialog {
		position: fixed;
		inset: 0;
		z-index: 95;
		display: grid;
		place-items: center;
		background: color-mix(in oklab, black 45%, transparent);
		padding: 1rem;
	}

	.studio-dialog__box {
		display: grid;
		width: min(26rem, 100%);
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
	}

	.studio-dialog__body {
		display: grid;
		gap: 0.6rem;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-credentials__title {
		display: flex;
		align-items: center;
		gap: 0.4rem;
		color: var(--studio-text-strong);
	}

	.studio-credentials__url {
		overflow-wrap: anywhere;
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-credentials__field {
		display: grid;
		gap: 0.3rem;
	}

	.studio-credentials__note {
		color: var(--studio-text-idle);
		font-size: 0.7rem;
	}

	.studio-dialog__footer {
		display: flex;
		justify-content: flex-end;
		gap: 0.6rem;
		padding: 0.75rem 1rem;
		border-top: 1px solid var(--studio-line);
	}
</style>
