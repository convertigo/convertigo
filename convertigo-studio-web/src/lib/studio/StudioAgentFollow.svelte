<script>
	import Ico from '#lib/utils/Ico.svelte';
	import { describeEntry, elapsedLabel } from './agentActivity.js';
	import {
		agentFollow,
		clearAgentActivity,
		pauseFollowing,
		resumeFollowing,
		showAgentEntry
	} from './agentFollow.svelte.js';

	/** the icons of the changes of the agent */
	const CHANGE_ICONS = {
		created: 'mdi:plus-circle-outline',
		changed: 'mdi:pencil-outline',
		renamed: 'mdi:rename-outline',
		moved: 'mdi:arrow-right-bold-outline',
		deleted: 'mdi:delete-outline',
		opened: 'mdi:eye-outline'
	};

	let open = $state(false);
	let now = $state(Date.now());
	/** @type {HTMLElement | undefined} */
	let root = $state();

	/** the last change of the agent, rather than the application it opens again to check it */
	let last = $derived(
		agentFollow.entries.find((entry) => entry.change !== 'opened') ?? agentFollow.entries[0]
	);
	let lastText = $derived(last ? describeEntry(last) : null);
	let shown = $derived(agentFollow.enabled || agentFollow.entries.length > 0);
	let mode = $derived(
		agentFollow.paused ? 'paused' : agentFollow.working ? 'working' : last ? 'idle' : 'waiting'
	);
	let chipTitle = $derived(
		mode === 'paused'
			? agentFollow.pauseReason === 'user'
				? 'Following the agent paused while you work in the Studio'
				: 'Following the agent paused'
			: mode === 'working'
				? 'The Studio follows the agent of the Assistant'
				: 'The actions of the agent of the Assistant'
	);

	$effect(() => {
		// the times of the feed, while it shows
		if (!open) return;
		now = Date.now();
		const timer = setInterval(() => (now = Date.now()), 5_000);
		return () => clearInterval(timer);
	});

	$effect(() => {
		if (!open) return;
		/** @param {PointerEvent} event */
		const onPointerDown = (event) => {
			if (root && !root.contains(/** @type {Node} */ (event.target))) open = false;
		};
		/** @param {KeyboardEvent} event */
		const onKeyDown = (event) => {
			if (event.key === 'Escape') open = false;
		};
		document.addEventListener('pointerdown', onPointerDown, true);
		document.addEventListener('keydown', onKeyDown, true);
		return () => {
			document.removeEventListener('pointerdown', onPointerDown, true);
			document.removeEventListener('keydown', onKeyDown, true);
		};
	});

	/** @param {import('./agentActivity.js').AgentEntry} entry */
	function entryTitle(entry) {
		const text = describeEntry(entry);
		return [
			entry.summary,
			`${text.verb} ${text.type} ${text.name}`.replace(/\s+/g, ' ').trim(),
			entry.tool && `with ${entry.tool}`,
			`in ${entry.projectName}`
		]
			.filter(Boolean)
			.join(' · ');
	}
</script>

{#if shown}
	<div class="studio-agent-follow" data-agent-follow bind:this={root}>
		<div class={['studio-agent-follow__chip', `studio-agent-follow__chip--${mode}`]}>
			<button
				type="button"
				class="studio-agent-follow__toggle"
				aria-expanded={open}
				aria-haspopup="dialog"
				aria-label="Agent activity"
				title={chipTitle}
				onclick={() => (open = !open)}
			>
				<span class="studio-agent-follow__beacon" aria-hidden="true">
					<Ico icon="mdi:robot-outline" size={4} />
				</span>
				{#if mode === 'paused'}
					<span class="studio-agent-follow__text">
						{agentFollow.pauseReason === 'user'
							? 'Paused while you work'
							: 'Following paused'}{agentFollow.missed ? ` · ${agentFollow.missed} new` : ''}
					</span>
				{:else if lastText && mode === 'working'}
					<span class="studio-agent-follow__text">
						<strong>{lastText.verb}</strong>
						{lastText.type}
						<em>{lastText.name}</em>
					</span>
				{:else if last}
					<span class="studio-agent-follow__text studio-agent-follow__text--quiet">
						{agentFollow.entries.length} change{agentFollow.entries.length > 1 ? 's' : ''}
					</span>
				{/if}
			</button>
			{#if mode === 'paused'}
				<button
					type="button"
					class="studio-agent-follow__resume"
					title="Show what the agent does again"
					onclick={() => resumeFollowing()}
				>
					<Ico icon="mdi:play" size={3.5} />
					<span>Resume</span>
				</button>
			{:else if mode === 'working'}
				<button
					type="button"
					class="studio-agent-follow__icon-button"
					title="Pause following the agent"
					aria-label="Pause following the agent"
					onclick={() => pauseFollowing()}
				>
					<Ico icon="mdi:pause" size={3.5} />
				</button>
			{/if}
		</div>

		{#if open}
			<div class="studio-agent-follow__panel" role="dialog" aria-label="Agent activity">
				<header class="studio-agent-follow__header">
					<strong>Agent activity</strong>
					<span class="studio-agent-follow__status">
						{#if !agentFollow.enabled}
							Not following
						{:else if agentFollow.paused}
							Paused
						{:else if agentFollow.working}
							Following
						{:else}
							Waiting for the agent
						{/if}
					</span>
					<span class="studio-agent-follow__spacer"></span>
					{#if agentFollow.enabled}
						<button
							type="button"
							class="studio-agent-follow__icon-button"
							title={agentFollow.paused
								? 'Resume following the agent'
								: 'Pause following the agent'}
							aria-label={agentFollow.paused
								? 'Resume following the agent'
								: 'Pause following the agent'}
							onclick={() => (agentFollow.paused ? resumeFollowing() : pauseFollowing())}
						>
							<Ico
								icon={agentFollow.paused ? 'mdi:play-circle-outline' : 'mdi:pause-circle-outline'}
								size={4}
							/>
						</button>
					{/if}
					<button
						type="button"
						class="studio-agent-follow__icon-button"
						title="Clear the activity"
						aria-label="Clear the activity"
						disabled={!agentFollow.entries.length}
						onclick={() => clearAgentActivity()}
					>
						<Ico icon="mdi:delete-sweep-outline" size={4} />
					</button>
				</header>
				{#if agentFollow.paused && agentFollow.pauseReason === 'user'}
					<p class="studio-agent-follow__hint">
						You work in the Studio: it no longer moves with the agent. Resume to see its last
						change; it resumes too once the agent stops.
					</p>
				{/if}
				{#if agentFollow.entries.length}
					<ol class="studio-agent-follow__list">
						{#each agentFollow.entries as entry (entry.key)}
							{@const text = describeEntry(entry)}
							<li>
								<button
									type="button"
									class={[
										'studio-agent-follow__entry',
										`studio-agent-follow__entry--${entry.change}`,
										entry.key === agentFollow.currentKey && 'studio-agent-follow__entry--current'
									]}
									title={entryTitle(entry)}
									onclick={() => showAgentEntry(entry)}
								>
									<span class="studio-agent-follow__entry-icon">
										<Ico icon={CHANGE_ICONS[entry.change] ?? CHANGE_ICONS.changed} size={3.5} />
									</span>
									<span class="studio-agent-follow__entry-text">
										<span class="studio-agent-follow__entry-main">
											<strong>{text.verb}</strong>
											{text.type}
											<em>{text.name}</em>
										</span>
										{#if text.detail || entry.summary}
											<span class="studio-agent-follow__entry-detail">
												{entry.summary || text.detail}
											</span>
										{/if}
									</span>
									<span class="studio-agent-follow__entry-time"
										>{elapsedLabel(entry.updatedAt, now)}</span
									>
								</button>
							</li>
						{/each}
					</ol>
				{:else}
					<p class="studio-agent-follow__empty">
						{agentFollow.enabled
							? 'What the agent of the Assistant creates or changes, with its Reveal mode on, shows here as it works.'
							: 'The Studio does not follow the agent in this profile: its preferences tell when it does.'}
					</p>
				{/if}
			</div>
		{/if}
	</div>
{/if}

<style>
	.studio-agent-follow {
		position: relative;
		flex: 0 1 auto;
		min-width: 0;
		--agent-accent: light-dark(#6d4ed6, #a891ff);
		--agent-created: light-dark(#15803d, #4ade80);
		--agent-changed: light-dark(#2563eb, #60a5fa);
		--agent-renamed: light-dark(#7c3aed, #c4b5fd);
		--agent-moved: light-dark(#0e7490, #67e8f9);
		--agent-deleted: light-dark(#dc2626, #f87171);
		--agent-paused: light-dark(#b45309, #fbbf24);
	}

	.studio-agent-follow__chip {
		display: flex;
		height: 1.9rem;
		min-width: 0;
		max-width: 24rem;
		align-items: center;
		gap: 0.1rem;
		border: 1px solid var(--studio-control-line, var(--studio-line));
		border-radius: 999px;
		color: var(--studio-text);
		padding: 0 0.15rem 0 0.1rem;
		transition:
			border-color 0.2s ease,
			background-color 0.2s ease;
	}

	.studio-agent-follow__chip--working {
		border-color: color-mix(in oklab, var(--agent-accent) 55%, transparent);
		background: color-mix(in oklab, var(--agent-accent) 9%, transparent);
	}

	.studio-agent-follow__chip--paused {
		border-color: color-mix(in oklab, var(--agent-paused) 60%, transparent);
		background: color-mix(in oklab, var(--agent-paused) 10%, transparent);
	}

	.studio-agent-follow__toggle {
		display: flex;
		min-width: 0;
		height: 100%;
		align-items: center;
		gap: 0.35rem;
		border: 0;
		border-radius: 999px;
		background: transparent;
		color: inherit;
		padding: 0 0.45rem 0 0.2rem;
		font-size: 0.74rem;
	}

	.studio-agent-follow__toggle:focus-visible,
	.studio-agent-follow__resume:focus-visible,
	.studio-agent-follow__icon-button:focus-visible,
	.studio-agent-follow__entry:focus-visible {
		outline: 2px solid var(--agent-accent);
		outline-offset: 1px;
	}

	.studio-agent-follow__beacon {
		position: relative;
		display: grid;
		width: 1.45rem;
		height: 1.45rem;
		flex: none;
		place-items: center;
		border-radius: 999px;
		color: var(--studio-text-idle);
	}

	.studio-agent-follow__chip--working .studio-agent-follow__beacon {
		color: var(--agent-accent);
	}

	.studio-agent-follow__chip--working .studio-agent-follow__beacon::after {
		position: absolute;
		inset: 0;
		border-radius: inherit;
		box-shadow: 0 0 0 0 color-mix(in oklab, var(--agent-accent) 55%, transparent);
		animation: studio-agent-follow-beacon 1.8s ease-out infinite;
		content: '';
	}

	.studio-agent-follow__chip--paused .studio-agent-follow__beacon {
		color: var(--agent-paused);
	}

	@keyframes studio-agent-follow-beacon {
		0% {
			box-shadow: 0 0 0 0 color-mix(in oklab, var(--agent-accent) 55%, transparent);
		}
		100% {
			box-shadow: 0 0 0 0.55rem transparent;
		}
	}

	.studio-agent-follow__text {
		overflow: hidden;
		min-width: 0;
		max-width: 17rem;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-agent-follow__text strong {
		font-weight: 650;
	}

	.studio-agent-follow__text em,
	.studio-agent-follow__entry-main em {
		color: var(--studio-text-strong);
		font-style: normal;
		font-weight: 600;
	}

	.studio-agent-follow__text--quiet {
		color: var(--studio-text-idle);
	}

	.studio-agent-follow__resume {
		display: flex;
		height: 1.45rem;
		flex: none;
		align-items: center;
		gap: 0.2rem;
		border: 0;
		border-radius: 999px;
		background: var(--agent-paused);
		color: light-dark(#ffffff, #1f1300);
		padding: 0 0.55rem 0 0.4rem;
		font-size: 0.72rem;
		font-weight: 650;
	}

	.studio-agent-follow__icon-button {
		display: grid;
		width: 1.5rem;
		height: 1.5rem;
		flex: none;
		place-items: center;
		border: 0;
		border-radius: 999px;
		background: transparent;
		color: var(--studio-text-idle);
	}

	.studio-agent-follow__icon-button:hover:not(:disabled),
	.studio-agent-follow__toggle:hover {
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

	.studio-agent-follow__icon-button:disabled {
		opacity: 0.4;
	}

	.studio-agent-follow__panel {
		position: absolute;
		z-index: 60;
		top: calc(100% + 0.4rem);
		right: 0;
		display: flex;
		width: min(26rem, calc(100vw - 1rem));
		max-height: min(30rem, calc(100vh - 5rem));
		flex-direction: column;
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.6rem;
		background: var(--studio-main-bg, var(--studio-panel-bg));
		box-shadow: 0 14px 38px rgb(0 0 0 / 0.28);
		color: var(--studio-text);
		animation: studio-agent-follow-open 0.14s ease-out;
	}

	@keyframes studio-agent-follow-open {
		from {
			opacity: 0;
			transform: translateY(-0.25rem);
		}
	}

	.studio-agent-follow__header {
		display: flex;
		align-items: center;
		gap: 0.45rem;
		border-bottom: 1px solid var(--studio-line);
		padding: 0.45rem 0.45rem 0.45rem 0.75rem;
		font-size: 0.78rem;
	}

	.studio-agent-follow__status {
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-agent-follow__spacer {
		flex: 1;
	}

	.studio-agent-follow__hint,
	.studio-agent-follow__empty {
		margin: 0;
		color: var(--studio-text-idle);
		padding: 0.55rem 0.75rem;
		font-size: 0.72rem;
		line-height: 1.4;
	}

	.studio-agent-follow__hint {
		border-bottom: 1px solid var(--studio-line);
		background: color-mix(in oklab, var(--agent-paused) 8%, transparent);
	}

	.studio-agent-follow__list {
		overflow: auto;
		margin: 0;
		padding: 0.3rem;
		list-style: none;
	}

	.studio-agent-follow__entry {
		display: grid;
		width: 100%;
		grid-template-columns: 1.4rem minmax(0, 1fr) auto;
		align-items: start;
		gap: 0.4rem;
		border: 0;
		border-radius: 0.4rem;
		background: transparent;
		color: inherit;
		padding: 0.35rem 0.45rem;
		font-size: 0.75rem;
		text-align: left;
	}

	.studio-agent-follow__entry:hover {
		background: var(--studio-hover-bg);
	}

	.studio-agent-follow__entry--current {
		background: color-mix(in oklab, var(--agent-accent) 12%, transparent);
	}

	.studio-agent-follow__entry-icon {
		display: grid;
		width: 1.4rem;
		height: 1.4rem;
		place-items: center;
		border-radius: 999px;
		background: color-mix(in oklab, var(--agent-color, var(--agent-changed)) 14%, transparent);
		color: var(--agent-color, var(--agent-changed));
	}

	.studio-agent-follow__entry--created {
		--agent-color: var(--agent-created);
	}

	.studio-agent-follow__entry--renamed {
		--agent-color: var(--agent-renamed);
	}

	.studio-agent-follow__entry--moved {
		--agent-color: var(--agent-moved);
	}

	.studio-agent-follow__entry--deleted {
		--agent-color: var(--agent-deleted);
	}

	.studio-agent-follow__entry--opened {
		--agent-color: var(--agent-accent);
	}

	.studio-agent-follow__entry-text {
		display: grid;
		min-width: 0;
		gap: 0.05rem;
	}

	.studio-agent-follow__entry-main,
	.studio-agent-follow__entry-detail {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-agent-follow__entry-detail {
		color: var(--studio-text-idle);
		font-size: 0.7rem;
	}

	.studio-agent-follow__entry-time {
		color: var(--studio-text-idle);
		font-size: 0.68rem;
		font-variant-numeric: tabular-nums;
		padding-top: 0.12rem;
	}

	@media (prefers-reduced-motion: reduce) {
		.studio-agent-follow__chip--working .studio-agent-follow__beacon::after,
		.studio-agent-follow__panel {
			animation: none;
		}
	}

	@media (max-width: 1180px) {
		.studio-agent-follow__text {
			max-width: 9rem;
		}
	}

	@media (max-width: 720px) {
		.studio-agent-follow__text {
			display: none;
		}
	}
</style>
