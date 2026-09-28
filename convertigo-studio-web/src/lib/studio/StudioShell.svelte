<script>
	import Ico from '$lib/utils/Ico.svelte';
	import './studioTheme.css';

	/** @type {{
	 * profile: string;
	 * collapsedPanels: { tree?: boolean, tools?: boolean };
	 * workspaceStyle?: string;
	 * logsPanelOpen?: boolean;
	 * onResizeStart?: (event: PointerEvent, target: 'tree' | 'tools' | 'logs') => void;
	 * onResizeKey?: (event: KeyboardEvent, target: 'tree' | 'tools' | 'logs') => void;
	 * onOpenLogs?: () => void;
	 * topbar?: import('svelte').Snippet;
	 * activity?: import('svelte').Snippet;
	 * tree?: import('svelte').Snippet;
	 * main?: import('svelte').Snippet;
	 * tools?: import('svelte').Snippet;
	 * logs?: import('svelte').Snippet;
	 * status?: import('svelte').Snippet;
	 * }} */
	let {
		profile,
		collapsedPanels = {},
		workspaceStyle = '',
		logsPanelOpen = false,
		onResizeStart,
		onResizeKey,
		onOpenLogs,
		topbar,
		activity,
		tree,
		main,
		tools,
		logs,
		status
	} = $props();
</script>

<!--
	The Studio is laid out as Cursor or Visual Studio Code: flat panels separated by lines, the activity
	bar and the projects on the left, the tools on the right of the work area, the logs under them and a
	status bar at the bottom.
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
	style={workspaceStyle}
>
	{@render topbar?.()}

	<div class="studio-shell__workspace">
		<div class="studio-shell__activity">
			{@render activity?.()}
		</div>

		<div class="studio-shell__tree" hidden={collapsedPanels.tree}>
			{@render tree?.()}
		</div>
		<button
			type="button"
			class="studio-resizer studio-resizer--vertical studio-resizer--tree"
			hidden={collapsedPanels.tree}
			aria-label="Resize projects panel"
			title="Resize projects panel"
			onpointerdown={(event) => onResizeStart?.(event, 'tree')}
			onkeydown={(event) => onResizeKey?.(event, 'tree')}
		></button>

		<main class="studio-shell__main">
			{@render main?.()}
		</main>

		<button
			type="button"
			class="studio-resizer studio-resizer--vertical studio-resizer--tools"
			hidden={collapsedPanels.tools}
			aria-label="Resize tools panel"
			title="Resize tools panel"
			onpointerdown={(event) => onResizeStart?.(event, 'tools')}
			onkeydown={(event) => onResizeKey?.(event, 'tools')}
		></button>
		<aside class="studio-shell__tools" hidden={collapsedPanels.tools}>
			{@render tools?.()}
		</aside>

		<!-- kept while hidden: a build or a debugger it follows goes on -->
		<section class="studio-shell__logs-panel" aria-label="Logs" hidden={!logsPanelOpen}>
			<button
				type="button"
				class="studio-resizer studio-resizer--logs"
				aria-label="Resize logs panel"
				title="Resize logs panel"
				onpointerdown={(event) => onResizeStart?.(event, 'logs')}
				onkeydown={(event) => onResizeKey?.(event, 'logs')}
			></button>
			<div class="studio-shell__logs-panel-body">
				{@render logs?.()}
			</div>
		</section>
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
		grid-template-columns:
			3rem var(--studio-tree-track) var(--studio-tree-resizer-track) minmax(0, 1fr)
			var(--studio-tools-resizer-track) var(--studio-tools-track);
		grid-template-rows: minmax(0, 1fr) auto;
		grid-template-areas:
			'activity tree tree-resizer main tools-resizer tools'
			'activity logs logs logs logs logs';
	}

	.studio-shell__tree[hidden],
	.studio-shell__tools[hidden],
	.studio-shell__logs-panel[hidden],
	.studio-resizer[hidden] {
		display: none;
	}

	.studio-shell__activity {
		grid-area: activity;
		min-height: 0;
	}

	.studio-shell__tree {
		grid-area: tree;
		min-width: 0;
		min-height: 0;
	}

	.studio-shell__tree :global(.studio__tree-panel) {
		height: 100%;
	}

	.studio-shell__main {
		--studio-panel-bg: var(--studio-main-bg);
		grid-area: main;
		min-width: 0;
		min-height: 0;
		overflow: hidden;
		background: var(--studio-main-bg);
	}

	.studio-shell__tools {
		grid-area: tools;
		min-width: 0;
		min-height: 0;
	}

	/* A resizer is the line between two panels, easier to grab than its width */
	.studio-resizer {
		position: relative;
		min-width: 0;
		min-height: 0;
		border: 0;
		background: var(--studio-line);
		padding: 0;
		transition: background 0.14s ease;
	}

	.studio-resizer::before {
		position: absolute;
		content: '';
	}

	.studio-resizer:hover,
	.studio-resizer:focus-visible {
		outline: none;
		background: var(--color-primary-500);
	}

	.studio-resizer--vertical {
		cursor: col-resize;
	}

	.studio-resizer--vertical::before {
		inset: 0 -3px;
	}

	.studio-resizer--tree {
		grid-area: tree-resizer;
	}

	.studio-resizer--tools {
		grid-area: tools-resizer;
	}

	.studio-resizer--logs {
		position: absolute;
		top: 0;
		right: 0;
		left: 0;
		height: 1px;
		cursor: row-resize;
	}

	.studio-resizer--logs::before {
		inset: -3px 0;
	}

	.studio-shell__logs-panel {
		position: relative;
		grid-area: logs;
		display: grid;
		height: min(var(--studio-logs-height), calc(100vh - 10rem));
		min-width: 0;
		min-height: 0;
		grid-template-rows: minmax(0, 1fr);
		overflow: hidden;
		background: var(--studio-panel-bg);
	}

	.studio-shell__logs-panel-body {
		min-width: 0;
		min-height: 0;
		overflow: hidden;
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

	/* On a large screen, the panels are cards on the window, as in Claude Desktop */
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
			row-gap: var(--studio-gutter);
		}

		.studio-shell__tree,
		.studio-shell__main,
		.studio-shell__tools,
		.studio-shell__logs-panel {
			overflow: hidden;
			border: 1px solid var(--studio-line);
			border-radius: var(--studio-card-radius);
			background: var(--studio-panel-bg);
			box-shadow: 0 1px 2px light-dark(rgb(0 0 0 / 0.06), rgb(0 0 0 / 0.4));
		}

		.studio-shell__main {
			background: var(--studio-main-bg);
		}

		.studio-resizer--vertical {
			background: transparent;
		}

		.studio-resizer--vertical::after {
			position: absolute;
			inset: 0.8rem calc(50% - 1px);
			border-radius: 1px;
			background: transparent;
			content: '';
			transition: background 0.14s ease;
		}

		.studio-resizer--vertical:hover,
		.studio-resizer--vertical:focus-visible {
			background: transparent;
		}

		.studio-resizer--vertical:hover::after,
		.studio-resizer--vertical:focus-visible::after {
			background: var(--color-primary-500);
		}

		.studio-resizer--logs {
			background: transparent;
		}
	}

	@media (max-width: 980px) {
		.studio-shell {
			height: auto;
			min-height: 100vh;
			min-height: 100dvh;
		}

		.studio-shell__workspace {
			grid-template-columns: minmax(0, 1fr) !important;
			grid-template-areas:
				'tree'
				'tools'
				'main'
				'logs';
			grid-template-rows: auto auto auto auto;
			align-content: start;
		}

		.studio-shell__activity {
			display: none;
		}

		.studio-shell__tree,
		.studio-shell__tools,
		.studio-shell__main {
			width: 100%;
			min-width: 0;
			min-height: 0;
			border-bottom: 1px solid var(--studio-line);
		}

		.studio-shell__tree {
			height: min(22rem, 42vh);
			height: min(22rem, 42dvh);
			min-height: 12rem;
		}

		.studio-shell__tools {
			height: min(30rem, 55vh);
			height: min(30rem, 55dvh);
			min-height: 16rem;
		}

		.studio-shell__main {
			height: min(44rem, 78vh);
			height: min(44rem, 78dvh);
			min-height: 22rem;
		}

		.studio-resizer--vertical {
			display: none;
		}

		.studio-shell--tree-hidden .studio-shell__workspace {
			grid-template-areas:
				'tools'
				'main'
				'logs';
			grid-template-rows: auto auto auto;
		}

		.studio-shell--tools-hidden .studio-shell__workspace {
			grid-template-areas:
				'tree'
				'main'
				'logs';
			grid-template-rows: auto auto auto;
		}

		.studio-shell--tree-hidden.studio-shell--tools-hidden .studio-shell__workspace {
			grid-template-areas:
				'main'
				'logs';
			grid-template-rows: auto auto;
		}

		.studio-shell__logs-panel {
			height: min(var(--studio-logs-height), 60vh);
			height: min(var(--studio-logs-height), 60dvh);
		}
	}

	@media (max-width: 520px) {
		.studio-shell__tree {
			height: min(20rem, 40vh);
			height: min(20rem, 40dvh);
		}

		.studio-shell__tools {
			height: min(28rem, 54vh);
			height: min(28rem, 54dvh);
		}

		.studio-shell__main {
			height: min(42rem, 76vh);
			height: min(42rem, 76dvh);
		}
	}
</style>
