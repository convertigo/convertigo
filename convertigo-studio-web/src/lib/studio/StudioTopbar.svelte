<script>
	import LightSwitch from '$lib/common/components/LightSwitch.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { resolve } from '$lib/utils/route';
	import StudioIconButton from './StudioIconButton.svelte';

	/** @type {{
	 * profile: string;
	 * profiles: { id: string, label: string, icon: string, description?: string }[];
	 * collapsedPanels: { tree?: boolean, tools?: boolean };
	 * toolsLabel?: string;
	 * breadcrumbs: { id: string, label: string, title?: string }[];
	 * showFlowOverview?: boolean;
	 * onSelectBreadcrumb?: (id: string) => void;
	 * onSetProfile?: (profile: string) => void;
	 * onTogglePanel?: (panel: 'tree' | 'tools') => void;
	 * onShowFlow?: () => void;
	 * }} */
	let {
		profile,
		profiles = [],
		collapsedPanels = {},
		toolsLabel = 'palette and properties',
		breadcrumbs = [],
		showFlowOverview = false,
		onSelectBreadcrumb,
		onSetProfile,
		onTogglePanel,
		onShowFlow
	} = $props();
</script>

<header class="studio-topbar gap-low">
	<div class="studio-topbar__brand layout-x-low">
		<span class="studio-topbar__logo studio-icon-tile">
			<Ico icon="convertigo:logo" size={5} />
		</span>
		<div class="studio-topbar__title">
			<strong class="studio-ellipsis">Convertigo Studio</strong>
		</div>
		<!-- the activity bar shows and hides the panels, these buttons do it on a narrow screen -->
		<div
			class="studio-topbar__button-group studio-topbar__panel-toggles layout-x-low"
			aria-label="Studio views"
		>
			<StudioIconButton
				icon="mdi:folder-outline"
				title={collapsedPanels.tree ? 'Show projects' : 'Hide projects'}
				ariaLabel={collapsedPanels.tree ? 'Show projects' : 'Hide projects'}
				active={!collapsedPanels.tree}
				size="md"
				onclick={() => onTogglePanel?.('tree')}
			/>
			<StudioIconButton
				icon="mdi:tune-vertical-variant"
				title={`${collapsedPanels.tools ? 'Show' : 'Hide'} ${toolsLabel}`}
				ariaLabel={`${collapsedPanels.tools ? 'Show' : 'Hide'} ${toolsLabel}`}
				active={!collapsedPanels.tools}
				size="md"
				onclick={() => onTogglePanel?.('tools')}
			/>
		</div>
	</div>

	<nav class="studio-topbar__breadcrumb layout-x-low" aria-label="Selection path">
		{#if breadcrumbs.length}
			{#each breadcrumbs as item, index (item.id)}
				{#if index > 0}
					<Ico icon="mdi:chevron-right" size={3} />
				{/if}
				<button
					type="button"
					class="studio-topbar__breadcrumb-item studio-ellipsis"
					title={item.title}
					onclick={() => onSelectBreadcrumb?.(item.id)}
				>
					{item.label}
				</button>
			{/each}
		{:else}
			<span class="studio-topbar__breadcrumb-item studio-ellipsis">Projects</span>
		{/if}
	</nav>

	<div class="studio-topbar__actions layout-x-low">
		<a
			class="studio-topbar__star layout-x-low"
			href="https://github.com/convertigo/convertigo"
			target="_blank"
			rel="noopener noreferrer"
			title="Star Convertigo on GitHub"
		>
			<Ico icon="mdi:github" size={4} />
			<span class="studio-topbar__star-label">Star us</span>
			<Ico icon="mdi:star-outline" size={3.5} class="studio-topbar__star-icon" />
		</a>
		<div
			class="studio-topbar__profiles layout-x-none"
			role="radiogroup"
			aria-label="Studio profile"
		>
			{#each profiles as item (item.id)}
				<button
					type="button"
					role="radio"
					aria-checked={profile === item.id}
					aria-label={item.label}
					class={[
						'studio-topbar__profile layout-x-low',
						profile === item.id && 'studio-topbar__profile--active'
					]
						.filter(Boolean)
						.join(' ')}
					title={item.description}
					onclick={() => onSetProfile?.(item.id)}
				>
					<Ico icon={item.icon} size={4} />
					<span class="studio-ellipsis">{item.label}</span>
				</button>
			{/each}
		</div>
		<div class="studio-topbar__button-group layout-x-low">
			<span class="studio-topbar__theme-switch layout-x-center-none">
				<LightSwitch />
			</span>
			{#if showFlowOverview}
				<StudioIconButton
					icon="mdi:source-branch"
					title="Show flow"
					ariaLabel="Show flow"
					size="md"
					onclick={onShowFlow}
				/>
			{/if}
			<!-- the admin console opens beside the Studio, which a window without address bar could not come back to -->
			<StudioIconButton
				href={resolve('/admin/')}
				target="_blank"
				rel="noopener"
				icon="mdi:lock-outline"
				title="Admin console"
				ariaLabel="Admin console"
				size="md"
			/>
		</div>
	</div>
</header>

<style>
	/* A flat title bar, as the one of Cursor */
	.studio-topbar {
		display: grid;
		min-height: 2.6rem;
		grid-template-columns: minmax(11rem, auto) minmax(0, 1fr) auto;
		align-items: center;
		border-bottom: 1px solid var(--studio-line, var(--color-surface-200-800));
		background: var(--studio-chrome-bg, var(--studio-panel-bg));
		padding: 0.2rem 0.5rem 0.2rem 0.6rem;
	}

	.studio-topbar__panel-toggles {
		display: none;
	}

	.studio-topbar__brand {
		min-width: 0;
	}

	.studio-topbar__logo {
		width: 1.75rem;
		height: 1.75rem;
	}

	.studio-topbar__title {
		display: grid;
		min-width: 0;
		gap: 0.08rem;
	}

	.studio-topbar__button-group {
		flex: 0 0 auto;
	}

	.studio-topbar__title strong {
		font-size: 0.82rem;
		font-weight: 600;
		line-height: 1.1;
	}

	/* The profiles are a quiet segmented control, the active one lit with the accent on its icon */
	.studio-topbar__profiles {
		flex: 0 0 auto;
		gap: 0.1rem;
		border: 1px solid var(--studio-line, var(--color-surface-200-800));
		border-radius: 0.4rem;
		background: transparent;
		padding: 0.12rem;
	}

	.studio-topbar__profile {
		height: 1.65rem;
		border: 0;
		border-radius: 0.28rem;
		background: transparent;
		color: var(--studio-text-idle, var(--color-surface-700-300));
		padding: 0 0.6rem;
		font-size: 0.75rem;
		font-weight: 600;
	}

	.studio-topbar__profile:hover {
		color: var(--studio-text-strong, var(--color-surface-950-50));
		background: var(--studio-hover-bg, transparent);
	}

	.studio-topbar__profile--active {
		background: var(--studio-selection-bg, var(--color-primary-500));
		color: var(--studio-text-strong, var(--color-primary-contrast-500));
	}

	.studio-topbar__profile--active :global(svg) {
		color: var(--color-primary-500);
	}

	.studio-topbar__actions {
		min-width: 0;
		justify-content: flex-end;
	}

	/* Star us on GitHub, as in the top bar of the console */
	.studio-topbar__star {
		flex: none;
		height: 1.9rem;
		align-items: center;
		gap: 0.3rem;
		border: 1px solid var(--studio-control-line, var(--color-surface-200-800));
		border-radius: 999px;
		color: var(--studio-text-strong);
		padding: 0 0.65rem;
		font-size: 0.74rem;
		font-weight: 600;
		text-decoration: none;
		white-space: nowrap;
	}

	.studio-topbar__star:hover {
		border-color: color-mix(in oklab, #e3b341 60%, transparent);
		background: color-mix(in oklab, #e3b341 12%, transparent);
	}

	.studio-topbar__star :global(.studio-topbar__star-icon) {
		color: #e3b341;
	}

	@media (max-width: 1180px) {
		.studio-topbar__star-label {
			display: none;
		}
	}

	.studio-topbar__breadcrumb {
		min-width: 0;
		justify-self: stretch;
		color: var(--color-surface-600-400);
		font-size: 0.76rem;
	}

	.studio-topbar__breadcrumb-item {
		max-width: 14rem;
		border: 0;
		border-radius: 0.3rem;
		background: transparent;
		color: inherit;
		padding: 0.28rem 0.32rem;
		font-size: inherit;
		font-weight: 650;
		text-align: left;
	}

	button.studio-topbar__breadcrumb-item:hover {
		background: color-mix(in oklab, var(--color-primary-500) 10%, transparent);
		color: var(--color-primary-700-300);
	}

	.studio-topbar__theme-switch {
		flex: 0 0 auto;
		width: 2.25rem;
		height: 2.25rem;
	}

	@media (max-width: 980px) {
		.studio-topbar {
			grid-template-columns: minmax(0, 1fr) auto;
		}

		.studio-topbar__panel-toggles {
			display: flex;
		}

		.studio-topbar__breadcrumb {
			display: none;
		}
	}

	@media (max-width: 720px) {
		.studio-topbar {
			grid-template-columns: auto minmax(0, 1fr);
		}

		.studio-topbar__actions {
			justify-content: flex-end;
		}

		.studio-topbar__profiles,
		.studio-topbar__button-group {
			min-width: 0;
		}

		.studio-topbar__profile span,
		.studio-topbar__title {
			display: none;
		}
	}

	@media (max-width: 420px) {
		.studio-topbar__profile {
			padding: 0 0.5rem;
		}

		.studio-topbar__logo,
		.studio-topbar__theme-switch {
			width: 2rem;
			height: 2rem;
		}

		:global(.studio-action-button) {
			--studio-action-size: 2rem !important;
		}
	}
</style>
