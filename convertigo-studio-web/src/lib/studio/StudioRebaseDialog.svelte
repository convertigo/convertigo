<script>
	import Ico from '#lib/utils/Ico.svelte';
	import { call } from '#lib/utils/service.js';
	import { untrack } from 'svelte';
	import {
		initialSteps,
		moveStep,
		REBASE_ACTIONS,
		stepsError,
		stepsParameter
	} from './rebaseSteps.js';

	/**
	 * The interactive rebase of the commits after a commit, as git rebase -i: each commit picked, reworded,
	 * stopped to edit, squashed, fixed up or dropped, in the order chosen.
	 *
	 * @type {{
	 *  projectName: string,
	 *  branch?: string,
	 *  upstream: { id: string, subject: string } | null,
	 *  onStart: (steps: ReturnType<typeof stepsParameter>) => void | Promise<void>,
	 *  onClose: () => void
	 * }}
	 */
	let { projectName, branch = '', upstream, onStart, onClose } = $props();

	/** @type {import('./rebaseSteps.js').RebaseStep[]} */
	let steps = $state([]);
	let loading = $state(false);
	let error = $derived(steps.length ? stepsError(steps) : '');

	$effect(() => {
		const base = upstream;
		untrack(async () => {
			steps = [];
			if (!base) {
				return;
			}
			loading = true;
			const result = await call('studio.git.SourceControl', {
				projectName,
				action: 'rebaseTodo',
				upstream: base.id
			});
			if (upstream === base) {
				steps = initialSteps(Array.isArray(result?.todo) ? result.todo : []);
				loading = false;
			}
		});
	});

	/**
	 * @param {HTMLElement} node
	 */
	function focus(node) {
		node.focus();
	}

	/**
	 * Shows the dialog over the whole window, out of the view whose panel would contain it: in the body of
	 * the document of the view, the Studio or a window the view was moved to.
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

{#if upstream}
	<div class="studio-dialog" role="presentation" use:portal onclick={onClose}>
		<div
			class="studio-dialog__box studio-rebase"
			role="dialog"
			aria-modal="true"
			aria-labelledby="studio-rebase-title"
			tabindex="-1"
			use:focus
			onclick={(event) => event.stopPropagation()}
			onkeydown={(event) => {
				event.stopPropagation();
				if (event.key === 'Escape') onClose();
			}}
		>
			<header class="studio-rebase__head">
				<Ico icon="mdi:source-branch-sync" size={4} />
				<div>
					<strong id="studio-rebase-title">Rebase {branch} interactively</strong>
					<small>The commits after {upstream.id} {upstream.subject}, oldest first</small>
				</div>
			</header>
			<div class="studio-rebase__body">
				{#if loading}
					<p class="studio-rebase__note">Reading the commits…</p>
				{:else if !steps.length}
					<p class="studio-rebase__note">No commit after {upstream.id} to rebase.</p>
				{:else}
					<ol class="studio-rebase__steps">
						{#each steps as step, index (step.id)}
							<li class={['studio-rebase__step', `studio-rebase__step--${step.action}`]}>
								<div class="studio-rebase__move">
									<button
										type="button"
										title="Replay it earlier"
										aria-label="Move {step.id} up"
										disabled={index === 0}
										onclick={() => (steps = moveStep(steps, index, -1))}
										><Ico icon="mdi:chevron-up" size={3.5} /></button
									>
									<button
										type="button"
										title="Replay it later"
										aria-label="Move {step.id} down"
										disabled={index === steps.length - 1}
										onclick={() => (steps = moveStep(steps, index, 1))}
										><Ico icon="mdi:chevron-down" size={3.5} /></button
									>
								</div>
								<select
									class="studio-rebase__action"
									aria-label="Action of {step.id}"
									title={REBASE_ACTIONS.find((action) => action.value === step.action)?.hint}
									bind:value={step.action}
								>
									{#each REBASE_ACTIONS as action (action.value)}
										<option value={action.value}>{action.label}</option>
									{/each}
								</select>
								<code>{step.id}</code>
								{#if step.action === 'reword'}
									<textarea
										class="studio-rebase__message input-common"
										aria-label="New message of {step.id}"
										rows="2"
										bind:value={step.message}></textarea>
								{:else}
									<span class="studio-rebase__subject" title={step.body ?? step.subject}
										>{step.subject}</span
									>
								{/if}
							</li>
						{/each}
					</ol>
					<p class="studio-rebase__note">
						{#each REBASE_ACTIONS as action, index (action.value)}<strong>{action.label}</strong>:
							{action.hint}{index < REBASE_ACTIONS.length - 1 ? ' · ' : '.'}{/each}
					</p>
				{/if}
				{#if error}
					<p class="studio-rebase__note studio-rebase__note--error">{error}</p>
				{/if}
			</div>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" onclick={onClose}>Cancel</button>
				<button
					type="button"
					class="button-primary"
					disabled={loading || !steps.length || Boolean(error)}
					onclick={() => onStart(stepsParameter(steps))}>Start the rebase</button
				>
			</footer>
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
		max-height: calc(100vh - 2rem);
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
		grid-template-rows: auto minmax(0, 1fr) auto;
	}

	.studio-rebase {
		width: min(44rem, 100%);
	}

	.studio-rebase__head {
		display: flex;
		align-items: flex-start;
		gap: 0.5rem;
		border-bottom: 1px solid var(--studio-line);
		padding: 0.75rem 1rem;
	}

	.studio-rebase__head div {
		display: grid;
		gap: 0.1rem;
	}

	.studio-rebase__head strong {
		color: var(--studio-text-strong);
		font-size: 0.85rem;
	}

	.studio-rebase__head small {
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-rebase__body {
		overflow: auto;
		padding: 0.6rem 1rem;
		font-size: 0.78rem;
	}

	.studio-rebase__steps {
		display: grid;
		gap: 0.25rem;
		margin: 0;
		padding: 0;
		list-style: none;
	}

	.studio-rebase__step {
		display: grid;
		align-items: center;
		gap: 0.5rem;
		border: 1px solid var(--studio-line);
		border-radius: 0.35rem;
		padding: 0.25rem 0.5rem 0.25rem 0.25rem;
		grid-template-columns: auto 6.5rem auto minmax(0, 1fr);
	}

	.studio-rebase__step--drop .studio-rebase__subject,
	.studio-rebase__step--drop code {
		opacity: 0.5;
		text-decoration: line-through;
	}

	.studio-rebase__step--squash,
	.studio-rebase__step--fixup {
		margin-left: 1.5rem;
	}

	.studio-rebase__step--edit {
		border-color: color-mix(in oklab, var(--color-warning-500) 55%, transparent);
	}

	.studio-rebase__move {
		display: grid;
	}

	.studio-rebase__move button {
		display: grid;
		height: 0.95rem;
		place-items: center;
		border: 0;
		border-radius: 0.2rem;
		background: transparent;
		color: var(--studio-text-idle);
		padding: 0 0.1rem;
	}

	.studio-rebase__move button:hover:not(:disabled) {
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

	.studio-rebase__move button:disabled {
		opacity: 0.3;
	}

	.studio-rebase__action {
		height: 1.55rem;
		border: 1px solid var(--studio-line);
		border-radius: 0.25rem;
		background-color: transparent;
		color: var(--studio-text-strong);
		padding: 0 1.3rem 0 0.35rem;
		font-size: 0.75rem;
	}

	.studio-rebase__step code {
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-rebase__subject {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-rebase__message {
		padding: 0.25rem 0.4rem;
		font-size: 0.76rem;
		resize: vertical;
	}

	.studio-rebase__note {
		margin: 0.6rem 0 0;
		color: var(--studio-text-idle);
		font-size: 0.7rem;
		line-height: 1.5;
	}

	.studio-rebase__note--error {
		color: var(--color-error-600-400);
	}

	.studio-dialog__footer {
		display: flex;
		justify-content: flex-end;
		gap: 0.6rem;
		padding: 0.75rem 1rem;
		border-top: 1px solid var(--studio-line);
	}
</style>
