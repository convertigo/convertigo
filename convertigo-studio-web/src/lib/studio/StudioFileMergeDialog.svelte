<script>
	import Ico from '#lib/utils/Ico.svelte';
	import { resolveConflict } from './treeMerge.svelte.js';

	/**
	 * @typedef {{ kind: 'same', text: string } | { kind: 'conflict', index: number, base: string,
	 *  mine: string, theirs: string }} FileBlock
	 * @typedef {'mine' | 'theirs' | 'both' | 'theirsFirst' | { edit: string }} BlockChoice
	 */

	/**
	 * The conflicts of a text file of a project, as Git merged it line by line: the lines merged, and each
	 * conflict resolved with my lines, theirs, both in an order, or lines edited; or the whole file of a side.
	 *
	 * @type {{
	 *  projectName: string,
	 *  conflict: import('./treeMerge.svelte.js').MergeConflict & { blocks?: FileBlock[] } | null,
	 *  mineLabel?: string,
	 *  theirsLabel?: string,
	 *  onClose: () => void
	 * }}
	 */
	let { projectName, conflict, mineLabel = 'Mine', theirsLabel = 'Theirs', onClose } = $props();

	/** @type {Record<string, BlockChoice>} */
	let choices = $state({});
	/** the conflicts whose lines are edited */
	let editing = $state(/** @type {Record<string, boolean>} */ ({}));
	/** the lines merged shown whole */
	let unfolded = $state(/** @type {Record<number, boolean>} */ ({}));
	let busy = $state(false);

	$effect.pre(() => {
		const current = conflict;
		try {
			choices =
				current?.value && current.resolution !== 'mine' && current.resolution !== 'theirs'
					? JSON.parse(current.value)
					: {};
		} catch {
			choices = {};
		}
		editing = {};
		unfolded = {};
	});

	let conflicts = $derived(
		/** @type {(FileBlock & { kind: 'conflict' })[]} */ (
			(conflict?.blocks ?? []).filter((block) => block.kind === 'conflict')
		)
	);
	let chosen = $derived(conflicts.filter((block) => choices[block.index] !== undefined).length);

	/**
	 * @param {number} index
	 * @param {BlockChoice} choice
	 */
	function choose(index, choice) {
		choices = { ...choices, [index]: choice };
		editing = { ...editing, [index]: false };
	}

	/**
	 * @param {FileBlock & { kind: 'conflict' }} block
	 */
	function edit(block) {
		const current = choices[block.index];
		const text =
			typeof current === 'object'
				? current.edit
				: current === 'theirs'
					? block.theirs
					: current === 'both'
						? block.mine + block.theirs
						: current === 'theirsFirst'
							? block.theirs + block.mine
							: block.mine;
		choices = { ...choices, [block.index]: { edit: text } };
		editing = { ...editing, [block.index]: true };
	}

	/**
	 * @param {'blocks' | 'mine' | 'theirs'} choice
	 */
	async function apply(choice) {
		if (!conflict) {
			return;
		}
		busy = true;
		try {
			await resolveConflict(
				projectName,
				conflict.id,
				choice,
				choice === 'blocks' ? JSON.stringify(choices) : undefined
			);
			onClose();
		} finally {
			busy = false;
		}
	}

	/**
	 * @param {string} text
	 * @returns {string[]}
	 */
	function lines(text) {
		const all = text.split('\n');
		if (all.at(-1) === '') {
			all.pop();
		}
		return all;
	}

	/**
	 * Shows a dialog over the whole window, out of the view whose panel would contain it.
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

	const OPTIONS = /** @type {('mine' | 'theirs' | 'both' | 'theirsFirst')[]} */ ([
		'mine',
		'theirs',
		'both',
		'theirsFirst'
	]);

	/** @type {Record<string, string>} */
	const LABELS = {
		mine: 'Mine',
		theirs: 'Theirs',
		both: 'Both, mine first',
		theirsFirst: 'Both, theirs first'
	};
</script>

{#snippet code(/** @type {string} */ text, /** @type {string} */ kind)}
	<pre
		class={[
			'studio-file-merge__code',
			`studio-file-merge__code--${kind}`
		]}>{#each lines(text) as line, index (index)}<span>{line}{'\n'}</span>{:else}<span
				class="studio-file-merge__empty">no line</span
			>{/each}</pre>
{/snippet}

{#if conflict}
	<div class="studio-dialog" role="presentation" use:portal onclick={onClose}>
		<div
			class="studio-dialog__box studio-file-merge"
			role="dialog"
			aria-modal="true"
			aria-labelledby="studio-file-merge-title"
			tabindex="-1"
			onclick={(event) => event.stopPropagation()}
			onkeydown={(event) => {
				event.stopPropagation();
				if (event.key === 'Escape') onClose();
			}}
		>
			<header class="studio-file-merge__head">
				<Ico icon="mdi:file-compare" size={4} />
				<div>
					<strong id="studio-file-merge-title">{conflict.name}</strong>
					<small
						>{conflicts.length} conflict{conflicts.length > 1 ? 's' : ''} · {chosen} resolved · the other
						lines are merged</small
					>
				</div>
			</header>
			<div class="studio-file-merge__body">
				{#each conflict.blocks ?? [] as block, position (position)}
					{#if block.kind === 'same'}
						{@const all = lines(block.text)}
						{#if all.length > 8 && !unfolded[position]}
							{@render code(all.slice(0, 3).join('\n'), 'same')}
							<button
								type="button"
								class="studio-file-merge__fold"
								onclick={() => (unfolded = { ...unfolded, [position]: true })}
								>{all.length - 6} lines merged</button
							>
							{@render code(all.slice(-3).join('\n'), 'same')}
						{:else}
							{@render code(block.text, 'same')}
						{/if}
					{:else}
						{@const choice = choices[block.index]}
						<div
							class={[
								'studio-file-merge__conflict',
								choice !== undefined && 'studio-file-merge__conflict--resolved'
							]}
						>
							<div class="studio-file-merge__sides">
								<div>
									<span class="studio-file-merge__side">{mineLabel}</span>
									{@render code(block.mine, choice === 'mine' ? 'chosen' : 'mine')}
								</div>
								<div>
									<span class="studio-file-merge__side">{theirsLabel}</span>
									{@render code(block.theirs, choice === 'theirs' ? 'chosen' : 'theirs')}
								</div>
							</div>
							{#if editing[block.index] && typeof choice === 'object'}
								<textarea
									class="studio-file-merge__edit input-common"
									aria-label="The lines of conflict {block.index + 1}"
									rows={Math.max(3, lines(choice.edit).length + 1)}
									value={choice.edit}
									oninput={(event) =>
										(choices = { ...choices, [block.index]: { edit: event.currentTarget.value } })}
								></textarea>
							{/if}
							<div class="studio-file-merge__choices">
								{#each OPTIONS as option (option)}
									<button
										type="button"
										class={[
											'studio-file-merge__choice',
											choice === option && 'studio-file-merge__choice--chosen'
										]}
										aria-pressed={choice === option}
										onclick={() => choose(block.index, option)}>{LABELS[option]}</button
									>
								{/each}
								<button
									type="button"
									class={[
										'studio-file-merge__choice',
										typeof choice === 'object' && 'studio-file-merge__choice--chosen'
									]}
									aria-pressed={typeof choice === 'object'}
									onclick={() => edit(block)}>Edit…</button
								>
								{#if block.base}
									<details class="studio-file-merge__base">
										<summary>Base</summary>
										{@render code(block.base, 'base')}
									</details>
								{/if}
							</div>
						</div>
					{/if}
				{/each}
			</div>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" disabled={busy} onclick={() => apply('mine')}
					>Keep my file</button
				>
				<button
					type="button"
					class="button-secondary"
					disabled={busy}
					onclick={() => apply('theirs')}>Take their file</button
				>
				<span class="studio-file-merge__spacer"></span>
				<button type="button" class="button-secondary" onclick={onClose}>Cancel</button>
				<button
					type="button"
					class="button-primary"
					disabled={busy || chosen < conflicts.length}
					title={chosen < conflicts.length ? 'Choose the lines of each conflict first' : ''}
					onclick={() => apply('blocks')}>Apply the choices</button
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

	.studio-file-merge {
		width: min(72rem, 100%);
	}

	.studio-file-merge__head {
		display: flex;
		align-items: flex-start;
		gap: 0.5rem;
		border-bottom: 1px solid var(--studio-line);
		padding: 0.75rem 1rem;
	}

	.studio-file-merge__head div {
		display: grid;
		gap: 0.1rem;
	}

	.studio-file-merge__head strong {
		color: var(--studio-text-strong);
		font-size: 0.85rem;
	}

	.studio-file-merge__head small {
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-file-merge__body {
		display: grid;
		align-content: start;
		gap: 0.2rem;
		overflow: auto;
		padding: 0.6rem 1rem;
	}

	.studio-file-merge__code {
		margin: 0;
		overflow-x: auto;
		padding: 0.1rem 0.5rem;
		font-family: var(--font-mono, ui-monospace, monospace);
		font-size: 0.72rem;
		line-height: 1.45;
		white-space: pre;
	}

	.studio-file-merge__code span {
		display: block;
	}

	.studio-file-merge__code--same {
		color: var(--studio-text-idle);
	}

	.studio-file-merge__code--mine {
		background: color-mix(in oklab, var(--color-primary-500) 10%, transparent);
	}

	.studio-file-merge__code--theirs {
		background: color-mix(in oklab, var(--color-secondary-500, #a78bfa) 12%, transparent);
	}

	.studio-file-merge__code--chosen {
		background: color-mix(in oklab, var(--color-success-500) 18%, transparent);
	}

	.studio-file-merge__code--base {
		background: var(--studio-hover-bg);
	}

	.studio-file-merge__empty {
		color: var(--studio-text-idle);
		font-style: italic;
	}

	.studio-file-merge__fold {
		justify-self: start;
		border: 0;
		background: transparent;
		color: var(--studio-text-idle);
		padding: 0 0.5rem;
		font-size: 0.7rem;
	}

	.studio-file-merge__fold:hover {
		color: var(--studio-text-strong);
	}

	.studio-file-merge__conflict {
		display: grid;
		gap: 0.3rem;
		margin: 0.25rem 0;
		border: 1px solid color-mix(in oklab, var(--color-error-500) 50%, transparent);
		border-radius: 0.35rem;
		padding: 0.35rem;
	}

	.studio-file-merge__conflict--resolved {
		border-color: color-mix(in oklab, var(--color-success-500) 50%, transparent);
	}

	.studio-file-merge__sides {
		display: grid;
		gap: 0.35rem;
		grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
	}

	.studio-file-merge__sides > div {
		display: grid;
		min-width: 0;
		align-content: start;
		gap: 0.15rem;
	}

	.studio-file-merge__side {
		color: var(--studio-text-idle);
		font-size: 0.68rem;
		font-weight: 600;
	}

	.studio-file-merge__edit {
		padding: 0.3rem 0.5rem;
		font-family: var(--font-mono, ui-monospace, monospace);
		font-size: 0.72rem;
		white-space: pre;
	}

	.studio-file-merge__choices {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 0.25rem;
	}

	.studio-file-merge__choice {
		border: 1px solid var(--studio-line);
		border-radius: 0.25rem;
		background: transparent;
		color: var(--studio-text);
		padding: 0.1rem 0.45rem;
		font-size: 0.72rem;
	}

	.studio-file-merge__choice:hover {
		background: var(--studio-hover-bg);
	}

	.studio-file-merge__choice--chosen {
		border-color: var(--color-primary-500);
		background: color-mix(in oklab, var(--color-primary-500) 18%, transparent);
		color: var(--studio-text-strong);
	}

	.studio-file-merge__base {
		margin-left: auto;
		color: var(--studio-text-idle);
		font-size: 0.7rem;
	}

	.studio-file-merge__spacer {
		flex: 1;
	}

	.studio-dialog__footer {
		display: flex;
		justify-content: flex-end;
		gap: 0.6rem;
		padding: 0.75rem 1rem;
		border-top: 1px solid var(--studio-line);
	}
</style>
