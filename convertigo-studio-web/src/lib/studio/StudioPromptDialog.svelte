<script>
	import { answerStudioPrompt, studioPromptState } from './studioPrompt.svelte.js';

	/** the question the page asks, as window.prompt, which the desktop Studio does not support */
	let request = $derived(studioPromptState.request);
	let value = $state('');

	$effect.pre(() => {
		value = request?.value ?? '';
	});

	/**
	 * @param {HTMLInputElement} node
	 */
	function focus(node) {
		node.focus();
		node.select();
	}
</script>

{#if request}
	<div class="studio-dialog" role="presentation" onclick={() => answerStudioPrompt(null)}>
		<div
			class="studio-dialog__box"
			role="dialog"
			aria-modal="true"
			aria-labelledby="studio-prompt-title"
			tabindex="-1"
			onclick={(event) => event.stopPropagation()}
			onkeydown={(event) => {
				event.stopPropagation();
				if (event.key === 'Escape') answerStudioPrompt(null);
			}}
		>
			<form
				onsubmit={(event) => {
					event.preventDefault();
					answerStudioPrompt(value);
				}}
			>
				<div class="studio-dialog__body">
					<label class="studio-prompt__field">
						<span id="studio-prompt-title">{request.message}</span>
						<input class="input-common" bind:value use:focus />
					</label>
				</div>
				<footer class="studio-dialog__footer">
					<button type="button" class="button-secondary" onclick={() => answerStudioPrompt(null)}
						>Cancel</button
					>
					<button type="submit" class="button-primary">OK</button>
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
		width: min(28rem, 100%);
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
	}

	.studio-dialog__body {
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-prompt__field {
		display: grid;
		gap: 0.45rem;
	}

	.studio-prompt__field span {
		color: var(--studio-text-strong);
	}

	.studio-dialog__footer {
		display: flex;
		justify-content: flex-end;
		gap: 0.6rem;
		padding: 0.75rem 1rem;
		border-top: 1px solid var(--studio-line);
	}
</style>
