<script>
	import Ico from '#lib/utils/Ico.svelte';
	import { call } from '#lib/utils/service.js';
	import { readProjectStatistics } from './projectStatistics';

	/**
	 * The statistics of a project, as the Statistics dialog of the Eclipse Studio: the count of its objects
	 * by kind.
	 *
	 * @type {{ projectName: string, onClose?: () => void }}
	 */
	let { projectName, onClose } = $props();

	let statistics = $state(/** @type {ReturnType<typeof readProjectStatistics> | null} */ (null));
	let error = $state('');

	$effect(() => {
		load(projectName);
	});

	/**
	 * @param {string} name
	 */
	async function load(name) {
		statistics = null;
		error = '';
		const result = await call('projects.GetStatistic', { projectName: name });
		if (result?.admin?.statistics) {
			statistics = readProjectStatistics(result.admin.statistics);
		} else {
			error = String(result?.error?.message ?? 'The statistics are not available.');
		}
	}
</script>

<div class="studio-dialog" role="presentation" onclick={() => onClose?.()}>
	<div
		class="studio-dialog__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-statistics-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape') onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-statistics-title">Statistics of {projectName}</strong>
		</header>
		<div class="studio-dialog__body">
			{#if error}
				<p class="studio-dialog__error" role="alert">{error}</p>
			{:else if !statistics}
				<p class="studio-dialog__hint"><Ico icon="mdi:sync" size={4} /> Counting its objects…</p>
			{:else}
				{#if statistics.summary.length}
					<p class="studio-statistics__summary">{statistics.summary.join(', ')}</p>
				{/if}
				{#each statistics.groups as group (group.title)}
					<section class="studio-statistics__group" aria-label={group.title}>
						<h3>{group.title}</h3>
						<dl>
							{#each group.rows as row, index (index)}
								<dt>{row.label}</dt>
								<dd>{row.value}</dd>
							{/each}
						</dl>
					</section>
				{/each}
			{/if}
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
		width: min(30rem, 100%);
		max-height: min(40rem, 100%);
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
		gap: 0.9rem;
		overflow-y: auto;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-statistics__summary {
		margin: 0;
		color: var(--studio-text-strong);
		font-weight: 600;
	}

	.studio-statistics__group h3 {
		margin: 0 0 0.35rem;
		color: var(--studio-text-strong);
		font-size: 0.78rem;
		font-weight: 600;
	}

	.studio-statistics__group dl {
		display: grid;
		grid-template-columns: minmax(0, 1fr) auto;
		gap: 0.2rem 1rem;
		margin: 0;
	}

	.studio-statistics__group dt {
		color: var(--studio-text-idle);
	}

	.studio-statistics__group dd {
		margin: 0;
		font-variant-numeric: tabular-nums;
		text-align: right;
	}

	.studio-dialog__hint {
		display: flex;
		align-items: center;
		gap: 0.4rem;
		margin: 0;
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-dialog__error {
		margin: 0;
		color: var(--color-error-600-400);
		font-size: 0.75rem;
	}
</style>
