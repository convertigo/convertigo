<script>
	import StudioTabs from './StudioTabs.svelte';

	/** @type {{
	 * items: { id: string, label: string, icon?: string, disabled?: boolean }[];
	 * active: string;
	 * panes: Record<string, import('svelte').Snippet>;
	 * ariaLabel?: string;
	 * panelLabel?: string;
	 * class?: string;
	 * bodyClass?: string;
	 * paneClass?: string;
	 * tabClass?: string;
	 * fillIds?: string[];
	 * lazyIds?: string[];
	 * isDisabled?: (id: string, item: any) => boolean;
	 * onSelect?: (id: string, item: any) => void;
	 * }} */
	let {
		items = [],
		active = '',
		panes = {},
		ariaLabel = 'Views',
		panelLabel = '',
		class: cls = '',
		bodyClass = '',
		paneClass = '',
		tabClass = '',
		fillIds = [],
		lazyIds = [],
		isDisabled,
		onSelect
	} = $props();

	let activeItem = $derived(items.find((item) => item.id === active));
</script>

<section class={['studio-tabbed-frame', cls].filter(Boolean).join(' ')}>
	<StudioTabs {items} {active} {ariaLabel} {isDisabled} {onSelect} {tabClass} />
	<div
		class={['studio-tabbed-frame-body', bodyClass].filter(Boolean).join(' ')}
		role="tabpanel"
		aria-label={panelLabel || activeItem?.label || 'View'}
	>
		{#each items as item (item.id)}
			{@const isActive = active === item.id}
			{@const shouldRender = isActive || !lazyIds.includes(item.id)}
			{@const pane = panes[item.id]}
			{#if shouldRender}
				<div
					class={[
						'studio-tabbed-frame-pane',
						fillIds.includes(item.id) && 'overflow-hidden!',
						paneClass
					]
						.filter(Boolean)
						.join(' ')}
					role="tabpanel"
					aria-label={item.label}
					hidden={!isActive}
				>
					{@render pane?.()}
				</div>
			{/if}
		{/each}
	</div>
</section>

<style>
	.studio-tabbed-frame {
		display: grid;
		height: 100%;
		min-width: 0;
		min-height: 0;
		grid-template-rows: auto minmax(0, 1fr);
		overflow: hidden;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: var(--studio-panel-bg);
	}

	.studio-tabbed-frame-body {
		min-width: 0;
		min-height: 0;
		overflow: hidden;
	}

	.studio-tabbed-frame-pane {
		height: 100%;
		min-width: 0;
		min-height: 0;
		overflow: auto;
	}

	.studio-tabbed-frame-pane[hidden] {
		display: none;
	}
</style>
