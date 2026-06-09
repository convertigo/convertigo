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

<section class={`studio-panel ${cls}`} class:studio-panel--collapsed={collapsed}>
	<header class="studio-panel__header">
		<div class="studio-panel__title">
			{#if icon}
				<Ico {icon} size={4} />
			{/if}
			<span>{title}</span>
		</div>
		{#if actions}
			<div class="studio-panel__actions">
				{@render actions?.()}
			</div>
		{/if}
		{#if collapsible}
			<button
				type="button"
				class="studio-panel__collapse"
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
		<div class={`studio-panel__content ${contentClass}`}>
			{@render children?.()}
		</div>
	{/if}
</section>

<style>
	.studio-panel {
		display: flex;
		min-width: 0;
		min-height: 0;
		flex-direction: column;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: var(--studio-panel-bg, var(--color-surface-50-950));
		overflow: hidden;
	}

	.studio-panel__header {
		display: flex;
		min-height: 2.45rem;
		align-items: center;
		justify-content: space-between;
		gap: 0.75rem;
		border-bottom: 1px solid var(--color-surface-200-800);
		background: var(
			--studio-panel-header-bg,
			color-mix(in oklab, var(--color-surface-100-900) 88%, transparent)
		);
		padding: 0.45rem 0.65rem;
	}

	.studio-panel__title,
	.studio-panel__actions,
	.studio-panel__collapse {
		display: flex;
		align-items: center;
		gap: 0.45rem;
	}

	.studio-panel__title {
		min-width: 0;
		color: var(--color-surface-800-200);
		font-size: 0.78rem;
		font-weight: 700;
		text-transform: uppercase;
	}

	.studio-panel__title span {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-panel__actions {
		margin-left: auto;
	}

	.studio-panel__collapse {
		width: 1.6rem;
		height: 1.6rem;
		justify-content: center;
		border: 0;
		border-radius: 0.3rem;
		background: transparent;
		color: var(--color-surface-700-300);
		padding: 0;
	}

	.studio-panel__collapse:hover {
		background: color-mix(in oklab, var(--color-surface-300-700) 45%, transparent);
		color: var(--color-surface-950-50);
	}

	.studio-panel__content {
		min-height: 0;
		flex: 1;
		overflow: auto;
	}

	.studio-panel--collapsed {
		min-width: 0;
		min-height: 0;
	}

	.studio-panel--collapsed .studio-panel__header {
		height: 100%;
		min-height: 2.45rem;
		padding-inline: 0.5rem;
	}

	.studio-panel--collapsed .studio-panel__title span {
		display: none;
	}
</style>
