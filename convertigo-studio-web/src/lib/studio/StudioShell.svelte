<script>
	import Ico from '#lib/utils/Ico.svelte';
	import './studioTheme.css';

	/** @type {{
	 * profile: string;
	 * collapsedPanels: { tree?: boolean, tools?: boolean };
	 * logsPanelOpen?: boolean;
	 * onOpenLogs?: () => void;
	 * topbar?: import('svelte').Snippet;
	 * activity?: import('svelte').Snippet;
	 * workspace?: import('svelte').Snippet;
	 * status?: import('svelte').Snippet;
	 * }} */
	let {
		profile,
		collapsedPanels = {},
		logsPanelOpen = false,
		onOpenLogs,
		topbar,
		activity,
		workspace,
		status
	} = $props();
</script>

<!--
	The Studio is laid out as Cursor or Visual Studio Code: the activity bar on the left, the views laid
	out by the dock, which moves and resizes them, and a status bar at the bottom.
-->
<section
	class={[
		'studio-shell',
		`studio-shell--${profile}`,
		collapsedPanels.tree && 'studio-shell--tree-hidden',
		collapsedPanels.tools && 'studio-shell--tools-hidden'
	]
		.filter(Boolean)
		.join(' ')}
>
	{@render topbar?.()}

	<div class="studio-shell__workspace">
		<div class="studio-shell__activity">
			{@render activity?.()}
		</div>
		<main class="studio-shell__dock">
			{@render workspace?.()}
		</main>
	</div>

	<footer class="studio-shell__status">
		{#if !logsPanelOpen}
			<button
				type="button"
				class="studio-shell__status-item"
				aria-expanded={false}
				onclick={onOpenLogs}
			>
				<Ico icon="mdi:file-document-box-outline" size={3} />Logs
				<Ico icon="mdi:chevron-up" size={3} />
			</button>
		{/if}
		<div class="studio-shell__status-trail">
			{@render status?.()}
		</div>
	</footer>
</section>

<style>
	/* The neutral grays of Cursor for the whole Studio page, its menus and dialogs included: each shade
	   serves the light theme on one side of the scale and the dark theme on the other. The pairings
	   are declared again to take these shades. */
	:global(html:has(.studio-shell)) {
		--color-surface-50: #f8f8f8;
		--color-surface-100: #f0f0f0;
		--color-surface-200: #e5e5e5;
		--color-surface-300: #d4d4d4;
		--color-surface-400: #9d9d9d;
		--color-surface-500: #858585;
		--color-surface-600: #6b6b6b;
		--color-surface-700: #3c3c3c;
		--color-surface-800: #2b2b2b;
		--color-surface-900: #1f1f1f;
		--color-surface-950: #181818;
		--color-surface-50-950: light-dark(var(--color-surface-50), var(--color-surface-950));
		--color-surface-100-900: light-dark(var(--color-surface-100), var(--color-surface-900));
		--color-surface-200-800: light-dark(var(--color-surface-200), var(--color-surface-800));
		--color-surface-300-700: light-dark(var(--color-surface-300), var(--color-surface-700));
		--color-surface-400-600: light-dark(var(--color-surface-400), var(--color-surface-600));
		--color-surface-600-400: light-dark(var(--color-surface-600), var(--color-surface-400));
		--color-surface-700-300: light-dark(var(--color-surface-700), var(--color-surface-300));
		--color-surface-800-200: light-dark(var(--color-surface-800), var(--color-surface-200));
		--color-surface-900-100: light-dark(var(--color-surface-900), var(--color-surface-100));
		--color-surface-950-50: light-dark(var(--color-surface-950), var(--color-surface-50));
		/* the colors of the Studio, for its menus and dialogs too */
		--studio-chrome-bg: var(--color-surface-50-950);
		--studio-shell-bg: var(--studio-chrome-bg);
		--studio-panel-bg: var(--studio-chrome-bg);
		--studio-panel-header-bg: var(--studio-chrome-bg);
		--studio-main-bg: light-dark(#ffffff, #1f1f1f);
		--studio-line: var(--color-surface-200-800);
		--studio-text: light-dark(#3b3b3b, #cccccc);
		--studio-text-strong: light-dark(#1f1f1f, #ffffff);
		--studio-text-idle: light-dark(#616161, #858585);
		--studio-hover-bg: light-dark(#f0f0f0, #2a2d2e);
		--studio-selection-bg: light-dark(#e4e6f1, #37373d);
		--studio-selection-focus-bg: light-dark(#d6ebff, #04395e);
		/* the window under the panels, which show as cards on it */
		--studio-canvas-bg: light-dark(#e8e8ea, #0f0f10);
		--studio-card-radius: 0.65rem;
		--studio-gutter: 0.4rem;
	}

	.studio-shell {
		display: grid;
		width: 100%;
		height: 100vh;
		height: 100dvh;
		min-width: 0;
		min-height: 0;
		grid-template-rows: auto minmax(0, 1fr) auto;
		background: var(--studio-chrome-bg);
		color: var(--studio-text);
	}

	.studio-shell__workspace {
		display: grid;
		min-width: 0;
		min-height: 0;
		grid-template-columns: 3rem minmax(0, 1fr);
		grid-template-rows: minmax(0, 1fr);
		grid-template-areas: 'activity dock';
	}

	.studio-shell__activity {
		grid-area: activity;
		min-height: 0;
	}

	.studio-shell__dock {
		grid-area: dock;
		min-width: 0;
		min-height: 0;
	}

	.studio-shell__status {
		display: flex;
		height: 1.6rem;
		min-width: 0;
		align-items: stretch;
		border-top: 1px solid var(--studio-line);
		background: var(--studio-chrome-bg);
		color: var(--studio-text-idle);
		font-size: 0.72rem;
	}

	.studio-shell__status-item {
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
		border: 0;
		background: transparent;
		color: inherit;
		padding: 0 0.6rem;
	}

	.studio-shell__status-item:hover {
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

	.studio-shell__status-trail {
		display: flex;
		min-width: 0;
		margin-left: auto;
		align-items: center;
		padding-right: 0.6rem;
	}

	/* On a large screen, the views are cards on the window, as in Claude Desktop */
	@media (min-width: 981px) {
		.studio-shell {
			background: var(--studio-canvas-bg);
		}

		.studio-shell :global(.studio-topbar),
		.studio-shell :global(.studio-activity-bar),
		.studio-shell__status {
			border-color: transparent;
			background: transparent;
		}

		.studio-shell__workspace {
			padding: 0 var(--studio-gutter) 0 0;
		}
	}

	/* On a small screen the views are stacked, without the activity bar whose items are in the top bar */
	@media (max-width: 980px) {
		.studio-shell__workspace {
			grid-template-columns: minmax(0, 1fr);
			grid-template-areas: 'dock';
		}

		.studio-shell__activity {
			display: none;
		}
	}
</style>
