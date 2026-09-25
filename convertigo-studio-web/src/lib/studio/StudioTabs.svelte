<script>
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{
	 * items: { id: string, label: string, icon?: string, disabled?: boolean }[];
	 * active: string;
	 * ariaLabel?: string;
	 * class?: string;
	 * tabClass?: string;
	 * iconSize?: number;
	 * isDisabled?: (id: string, item: any) => boolean;
	 * onSelect?: (id: string, item: any) => void;
	 * trail?: import('svelte').Snippet;
	 * }} */
	let {
		items = [],
		active = '',
		ariaLabel = 'Tabs',
		class: cls = '',
		tabClass = '',
		iconSize = 4,
		isDisabled,
		onSelect,
		trail
	} = $props();

	/** @param {{ id: string, disabled?: boolean }} item */
	function disabled(item) {
		return Boolean(item.disabled || isDisabled?.(item.id, item));
	}
</script>

<div
	class={['studio-tab-strip', cls].filter(Boolean).join(' ')}
	role="tablist"
	aria-label={ariaLabel}
>
	{#each items as item (item.id)}
		{@const isActive = active === item.id}
		<button
			type="button"
			role="tab"
			class={['studio-tab layout-x-low', isActive && 'studio-tab--active', tabClass]
				.filter(Boolean)
				.join(' ')}
			aria-selected={isActive}
			disabled={disabled(item)}
			title={item.label}
			onclick={() => onSelect?.(item.id, item)}
		>
			{#if item.icon}
				<Ico icon={item.icon} size={iconSize} />
			{/if}
			<span>{item.label}</span>
		</button>
	{/each}
	{#if trail}
		<div class="studio-tab-strip__trail">
			{@render trail()}
		</div>
	{/if}
</div>

<style>
	/* Flat tabs, as the panel tabs of Cursor: the selected one is underlined with the accent */
	.studio-tab-strip {
		display: flex;
		min-width: 0;
		gap: 0.25rem;
		overflow: hidden;
		background: var(--studio-panel-header-bg);
		padding: 0 0.5rem;
	}

	.studio-tab {
		position: relative;
		min-width: 0;
		height: 2.5rem;
		flex: 0 1 auto;
		justify-content: center;
		border: 0;
		background: transparent;
		color: var(--studio-text-idle, var(--color-surface-700-300));
		padding: 0 0.5rem;
		font-size: 0.72rem;
		font-weight: 600;
		letter-spacing: 0.02em;
		text-transform: uppercase;
	}

	.studio-tab:hover:not(:disabled) {
		color: var(--studio-text-strong, var(--color-surface-950-50));
	}

	.studio-tab--active {
		color: var(--studio-text-strong, var(--color-surface-950-50));
	}

	.studio-tab--active::after {
		position: absolute;
		right: 0.5rem;
		bottom: 0.3rem;
		left: 0.5rem;
		height: 1px;
		background: var(--color-primary-500);
		content: '';
	}

	.studio-tab-strip__trail {
		display: flex;
		margin-left: auto;
		align-items: center;
		gap: 0.2rem;
	}

	.studio-tab:disabled {
		opacity: 0.45;
		cursor: not-allowed;
	}

	.studio-tab span {
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
</style>
