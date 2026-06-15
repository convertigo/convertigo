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
	 * }} */
	let {
		items = [],
		active = '',
		ariaLabel = 'Tabs',
		class: cls = '',
		tabClass = '',
		iconSize = 4,
		isDisabled,
		onSelect
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
</div>

<style>
	.studio-tab-strip {
		display: grid;
		grid-auto-columns: minmax(0, 1fr);
		grid-auto-flow: column;
		gap: 0.18rem;
		border-bottom: 1px solid var(--color-surface-200-800);
		background: var(--studio-panel-header-bg);
		padding: 0.22rem;
	}

	.studio-tab {
		min-width: 0;
		height: 2.15rem;
		justify-content: center;
		border: 1px solid transparent;
		border-radius: 0.3rem;
		background: transparent;
		color: var(--color-surface-700-300);
		padding: 0 0.45rem;
		font-size: 0.72rem;
		font-weight: 750;
		text-transform: uppercase;
	}

	.studio-tab:hover:not(:disabled),
	.studio-tab--active {
		border-color: color-mix(in oklab, var(--color-primary-500) 38%, transparent);
		background: color-mix(in oklab, var(--color-primary-500) 11%, transparent);
		color: var(--color-primary-600-400);
	}

	.studio-tab:disabled {
		color: var(--color-surface-500);
		cursor: not-allowed;
	}

	.studio-tab span {
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
</style>
