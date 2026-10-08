<script>
	import AccordionSection from '#lib/common/components/AccordionSection.svelte';

	/** @type {{
	 * class?: string;
	 * triggerClass?: string;
	 * panelClass?: string;
	 * titleClass?: string;
	 * indicatorSize?: number;
	 * panel?: import('svelte').Snippet;
	 * children?: import('svelte').Snippet;
	 * [key: string]: any;
	 * }} */
	let {
		class: cls = '',
		triggerClass = '',
		panelClass = '',
		titleClass = '',
		indicatorSize = 4,
		panel,
		children,
		...rest
	} = $props();

	let sectionClass = $derived(['studio-section-item', cls].filter(Boolean).join(' '));
	let sectionTriggerClass = $derived(
		['studio-section-trigger', triggerClass].filter(Boolean).join(' ')
	);
	let sectionPanelClass = $derived(['studio-section-panel', panelClass].filter(Boolean).join(' '));
	let sectionTitleClass = $derived(['studio-section-title', titleClass].filter(Boolean).join(' '));
</script>

<AccordionSection
	{...rest}
	class={sectionClass}
	triggerClass={sectionTriggerClass}
	panelClass={sectionPanelClass}
	titleClass={sectionTitleClass}
	{indicatorSize}
	{panel}
>
	{@render children?.()}
</AccordionSection>

<style>
	:global(.studio-section-item) {
		border-bottom: 1px solid var(--studio-line, var(--color-surface-200-800));
		background: var(--studio-panel-bg, var(--color-surface-50-950));
	}

	:global(.studio-section-item:first-child) {
		border-top: 0;
	}

	/* A section header of a side bar of Cursor: on the panel, in small capitals */
	:global(.studio-section-trigger) {
		min-height: 2rem;
		background: var(
			--studio-panel-header-bg,
			color-mix(in oklab, var(--color-surface-100-900) 88%, transparent)
		);
		color: var(--studio-text, var(--color-surface-800-200));
		padding: 0.25rem 0.6rem 0.25rem 0.85rem;
		font-size: 0.7rem;
		font-weight: 700;
		letter-spacing: 0.02em;
		text-transform: uppercase;
	}

	:global(.studio-section-trigger:hover:not(:disabled)) {
		background: var(
			--studio-hover-bg,
			color-mix(in oklab, var(--color-primary-500) 9%, transparent)
		);
		color: var(--color-surface-950-50);
	}

	:global(.studio-section-trigger [data-state]) {
		color: var(--color-surface-700-300);
	}

	:global(.studio-section-trigger:hover:not(:disabled) [data-state]) {
		color: var(--color-surface-950-50);
	}

	:global(.studio-section-trigger svg) {
		width: 1rem;
		height: 1rem;
	}

	:global(.studio-section-title) {
		overflow: hidden;
		color: currentcolor;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	:global(.studio-section-panel) {
		background: color-mix(in oklab, var(--color-surface-50-950) 78%, transparent);
		padding: 0;
	}
</style>
