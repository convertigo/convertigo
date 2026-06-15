<script>
	import LightSwitch from '$lib/common/components/LightSwitch.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { resolve } from '$lib/utils/route';
	import StudioIconButton from './StudioIconButton.svelte';

	/** @type {{
	 * profile: string;
	 * profiles: { id: string, label: string, icon: string, description?: string }[];
	 * collapsedPanels: { tree?: boolean, tools?: boolean };
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
		breadcrumbs = [],
		showFlowOverview = false,
		onSelectBreadcrumb,
		onSetProfile,
		onTogglePanel,
		onShowFlow
	} = $props();
</script>

<header class="studio-topbar gap-low p-low">
	<div class="studio-topbar__brand layout-x-low">
		<span class="studio-topbar__logo studio-icon-tile">
			<Ico icon="convertigo:logo" size={5} />
		</span>
		<div class="studio-topbar__title">
			<strong class="studio-ellipsis">Convertigo Studio</strong>
		</div>
		<div class="studio-topbar__button-group layout-x-low" aria-label="Studio views">
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
				title={collapsedPanels.tools
					? 'Show palette and properties'
					: 'Hide palette and properties'}
				ariaLabel={collapsedPanels.tools
					? 'Show palette and properties'
					: 'Hide palette and properties'}
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
			<StudioIconButton
				href={resolve('/admin/')}
				icon="mdi:lock-outline"
				title="Admin console"
				ariaLabel="Admin console"
				size="md"
			/>
		</div>
	</div>
</header>

<style>
	.studio-topbar {
		display: grid;
		grid-template-columns: minmax(11rem, auto) minmax(0, 1fr) auto;
		align-items: center;
		border-bottom: 1px solid var(--color-surface-200-800);
		background: color-mix(in oklab, var(--studio-panel-bg) 92%, transparent);
	}

	.studio-topbar__brand {
		min-width: 0;
	}

	.studio-topbar__logo {
		width: 2rem;
		height: 2rem;
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
		font-size: 0.9rem;
		line-height: 1.1;
	}

	.studio-topbar__profiles {
		flex: 0 0 auto;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: var(--studio-shell-bg);
		padding: 0.16rem;
	}

	.studio-topbar__profile {
		height: 2rem;
		border: 0;
		border-radius: 0.3rem;
		background: transparent;
		color: var(--color-surface-700-300);
		padding: 0 0.65rem;
		font-size: 0.78rem;
		font-weight: 700;
	}

	.studio-topbar__profile:hover {
		color: var(--color-surface-950-50);
		background: color-mix(in oklab, var(--color-surface-300-700) 40%, transparent);
	}

	.studio-topbar__profile--active {
		background: var(--color-primary-500);
		color: var(--color-primary-contrast-500);
	}

	.studio-topbar__actions {
		min-width: 0;
		justify-content: flex-end;
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

		.studio-topbar__breadcrumb {
			display: none;
		}
	}

	@media (max-width: 720px) {
		.studio-topbar__profile span,
		.studio-topbar__title {
			display: none;
		}
	}
</style>
