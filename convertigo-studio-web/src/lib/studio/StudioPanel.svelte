<script>
	import Ico from '$lib/utils/Ico.svelte';

	/**
	 * @type {{
	 *  title: string,
	 *  icon?: string,
	 *  class?: string,
	 *  contentClass?: string,
	 *  collapsible?: boolean,
	 *  collapsed?: boolean,
	 *  actions?: import('svelte').Snippet,
	 *  children?: import('svelte').Snippet
	 * }}
	 */
	let {
		title,
		icon,
		class: cls = '',
		contentClass = '',
		collapsible = false,
		collapsed = $bindable(false),
		actions,
		children
	} = $props();
</script>

<section class={['studio-panel-shell', cls, collapsed && 'studio-panel--collapsed']}>
	<header class="studio-panel-header layout-x-between-none">
		<div class="studio-panel-title layout-x-low">
			{#if icon}
				<Ico {icon} size={4} />
			{/if}
			<span class="studio-panel-title-text">{title}</span>
		</div>
		{#if actions}
			<div class="ml-auto layout-x-low">
				{@render actions?.()}
			</div>
		{/if}
		{#if collapsible}
			<button
				type="button"
				class="studio-icon-button layout-x-low"
				title={collapsed ? 'Expand panel' : 'Collapse panel'}
				aria-label={collapsed ? 'Expand panel' : 'Collapse panel'}
				aria-expanded={!collapsed}
				onclick={() => (collapsed = !collapsed)}
			>
				<Ico icon={collapsed ? 'mdi:chevron-right' : 'mdi:chevron-down'} size={4} />
			</button>
		{/if}
	</header>
	{#if !collapsed}
		<div class={['studio-panel-content', contentClass]}>
			{@render children?.()}
		</div>
	{/if}
</section>

<style>
	.studio-panel-shell {
		display: flex;
		min-width: 0;
		min-height: 0;
		flex-direction: column;
		overflow: hidden;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: var(--studio-panel-bg, var(--color-surface-50-950));
	}

	.studio-panel-header {
		min-height: 2.45rem;
		gap: 0.75rem;
		border-bottom: 1px solid var(--color-surface-200-800);
		background: var(
			--studio-panel-header-bg,
			color-mix(in oklab, var(--color-surface-100-900) 88%, transparent)
		);
		padding: 0.45rem 0.65rem;
	}

	.studio-panel-title {
		min-width: 0;
		color: var(--color-surface-800-200);
		font-size: 0.78rem;
		font-weight: 700;
		text-transform: uppercase;
	}

	.studio-panel-title-text {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-panel-content {
		min-height: 0;
		flex: 1;
		overflow: auto;
	}

	.studio-icon-button {
		width: 1.6rem;
		height: 1.6rem;
		justify-content: center;
		border: 0;
		border-radius: 0.3rem;
		background: transparent;
		color: var(--color-surface-700-300);
		padding: 0;
	}

	.studio-icon-button:hover,
	.studio-icon-button:focus-visible {
		background: color-mix(in oklab, var(--color-surface-300-700) 45%, transparent);
		color: var(--color-surface-950-50);
	}

	.studio-panel--collapsed {
		min-width: 0;
		min-height: 0;
	}

	.studio-panel--collapsed .studio-panel-header {
		height: 100%;
		min-height: 2.45rem;
		padding-inline: 0.5rem;
	}

	.studio-panel--collapsed .studio-panel-title-text {
		display: none;
	}
</style>
