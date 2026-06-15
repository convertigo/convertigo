<script>
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{
	 * profile: string;
	 * collapsedPanels: { tree?: boolean, tools?: boolean };
	 * workspaceStyle?: string;
	 * logsPanelOpen?: boolean;
	 * onResizeStart?: (event: PointerEvent, target: 'tree' | 'tools' | 'logs') => void;
	 * onResizeKey?: (event: KeyboardEvent, target: 'tree' | 'tools' | 'logs') => void;
	 * onOpenLogs?: () => void;
	 * topbar?: import('svelte').Snippet;
	 * tree?: import('svelte').Snippet;
	 * main?: import('svelte').Snippet;
	 * tools?: import('svelte').Snippet;
	 * logs?: import('svelte').Snippet;
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
		tree,
		main,
		tools,
		logs
	} = $props();
</script>

<section
	class={[
		'studio-shell [--studio-shell-gap:--spacing(1.5)] md:[--studio-shell-gap:--spacing(3)]',
		`studio-shell--${profile}`,
		collapsedPanels.tree && 'studio-shell--tree-hidden',
		collapsedPanels.tools && 'studio-shell--tools-hidden'
	]
		.filter(Boolean)
		.join(' ')}
	style={workspaceStyle}
>
	{@render topbar?.()}

	<div class="studio-shell__workspace p-low">
		<div class="studio-shell__tree" hidden={collapsedPanels.tree}>
			{@render tree?.()}
		</div>
		<button
			type="button"
			class={['studio-resizer', 'studio-resizer--vertical', 'studio-resizer--tree']
				.filter(Boolean)
				.join(' ')}
			hidden={collapsedPanels.tree}
			aria-label="Resize projects panel"
			title="Resize projects panel"
			onpointerdown={(event) => onResizeStart?.(event, 'tree')}
			onkeydown={(event) => onResizeKey?.(event, 'tree')}
		></button>

		<main class="studio-shell__main">
			{@render main?.()}
		</main>

		<aside class="studio-shell__tools" hidden={collapsedPanels.tools}>
			{@render tools?.()}
		</aside>
		<button
			type="button"
			class={['studio-resizer', 'studio-resizer--vertical', 'studio-resizer--tools']
				.filter(Boolean)
				.join(' ')}
			hidden={collapsedPanels.tools}
			aria-label="Resize tools panel"
			title="Resize tools panel"
			onpointerdown={(event) => onResizeStart?.(event, 'tools')}
			onkeydown={(event) => onResizeKey?.(event, 'tools')}
		></button>
	</div>

	{#if logsPanelOpen}
		<section class="studio-shell__logs-panel mx-low mb-low" aria-label="Logs">
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
	{:else}
		<button
			type="button"
			class="studio-shell__logs-bar mx-low mb-low layout-x-between-low p-low"
			aria-expanded={false}
			onclick={onOpenLogs}
		>
			<span class="layout-x-low"><Ico icon="mdi:file-document-box-outline" size={4} />Logs</span>
			<Ico icon="mdi:chevron-up" size={4} />
		</button>
	{/if}
</section>

<style>
	.studio-shell {
		--studio-shell-bg: color-mix(
			in oklab,
			var(--color-surface-100-900) 82%,
			var(--color-surface-200-800)
		);
		--studio-panel-bg: var(--color-surface-50-950);
		--studio-panel-header-bg: color-mix(in oklab, var(--color-surface-100-900) 88%, transparent);
		display: grid;
		width: 100%;
		height: 100vh;
		min-width: 0;
		min-height: 0;
		grid-template-rows: auto minmax(0, 1fr) auto;
		background: var(--studio-shell-bg);
		color: var(--color-surface-950-50);
	}

	.studio-shell__workspace {
		display: grid;
		min-width: 0;
		min-height: 0;
	}

	.studio-shell--backend .studio-shell__workspace,
	.studio-shell--frontend .studio-shell__workspace {
		grid-template-columns:
			var(--studio-tree-track) var(--studio-tree-resizer-track) var(--studio-tools-track)
			var(--studio-tools-resizer-track)
			minmax(0, 1fr);
		grid-template-areas: 'tree tree-resizer tools tools-resizer main';
	}

	.studio-shell__tree[hidden],
	.studio-shell__tools[hidden],
	.studio-resizer[hidden] {
		display: none;
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
		grid-area: main;
		min-width: 0;
		min-height: 0;
		overflow: hidden;
	}

	.studio-shell__tools {
		grid-area: tools;
		min-width: 0;
		min-height: 0;
	}

	.studio-resizer {
		position: relative;
		min-width: 0;
		min-height: 0;
		border: 0;
		background: transparent;
		padding: 0;
	}

	.studio-resizer::before {
		position: absolute;
		border-radius: 999px;
		background: transparent;
		content: '';
		transition:
			background 0.14s ease,
			inset 0.14s ease;
	}

	.studio-resizer:hover::before,
	.studio-resizer:focus-visible::before {
		background: color-mix(in oklab, var(--color-primary-500) 42%, transparent);
	}

	.studio-resizer--vertical {
		cursor: col-resize;
	}

	.studio-resizer--vertical::before {
		inset: 0.35rem 0.16rem;
	}

	.studio-resizer--vertical:hover::before,
	.studio-resizer--vertical:focus-visible::before {
		inset: 0.15rem 0.08rem;
	}

	.studio-resizer--tree {
		grid-area: tree-resizer;
	}

	.studio-resizer--tools {
		grid-area: tools-resizer;
	}

	.studio-resizer--logs {
		position: absolute;
		z-index: 2;
		top: -0.28rem;
		right: 0;
		left: 0;
		height: 0.55rem;
		cursor: row-resize;
	}

	.studio-resizer--logs::before {
		inset: 0.2rem 48%;
	}

	.studio-resizer--logs:hover::before,
	.studio-resizer--logs:focus-visible::before {
		inset: 0.12rem 44%;
	}

	.studio-shell__logs-bar {
		min-height: 2.45rem;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: var(--studio-panel-header-bg);
		color: var(--color-surface-800-200);
		font-size: 0.78rem;
		font-weight: 700;
		text-transform: uppercase;
	}

	.studio-shell__logs-bar:hover {
		background: color-mix(in oklab, var(--color-primary-500) 9%, transparent);
		color: var(--color-surface-950-50);
	}

	.studio-shell__logs-panel {
		position: relative;
		display: grid;
		height: min(var(--studio-logs-height), calc(100vh - 10rem));
		min-width: 0;
		min-height: 0;
		grid-template-rows: minmax(0, 1fr);
		overflow: hidden;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: var(--studio-panel-bg);
		box-shadow: 0 -0.75rem 1.75rem color-mix(in oklab, var(--color-surface-950) 8%, transparent);
	}

	.studio-shell__logs-panel-body {
		min-width: 0;
		min-height: 0;
		overflow: hidden;
	}

	@media (max-width: 980px) {
		.studio-shell {
			height: auto;
			min-height: 100vh;
		}

		.studio-shell__workspace {
			grid-template-columns: minmax(0, 1fr) !important;
			grid-template-areas:
				'tree'
				'tools'
				'main';
		}

		.studio-shell__tree,
		.studio-shell__tools,
		.studio-shell__main {
			min-height: 16rem;
		}

		.studio-resizer {
			display: none;
		}

		.studio-shell__logs-panel {
			height: min(var(--studio-logs-height), 70vh);
		}
	}
</style>
