<script>
	import { Accordion } from '@skeletonlabs/skeleton-svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { slide } from 'svelte/transition';

	/** @type {{
	value?: any;
	class?: string;
	triggerClass?: string;
	panelClass?: string;
	control?: import('svelte').Snippet;
	panel?: import('svelte').Snippet;
	children?: import('svelte').Snippet;
	disabled?: boolean;
	title?: string;
	subtitle?: string;
	meta?: string;
	count?: number;
	countVariant?: 'items' | 'number' | 'none';
	countItemLabel?: string;
	countEmptyLabel?: string;
	lead?: import('svelte').Snippet;
	trail?: import('svelte').Snippet;
	leadingIcon?: string;
	leadingIconClass?: string;
	trailingText?: string;
	trailingTextClass?: string;
} & Record<string, any>} */
	let {
		value,
		class: cls = '',
		triggerClass = '',
		panelClass = '',
		control,
		panel,
		children,
		disabled = false,
		title,
		subtitle,
		meta,
		count,
		countVariant = 'items',
		countItemLabel = 'Item',
		countEmptyLabel = 'Empty',
		lead,
		trail,
		leadingIcon,
		leadingIconClass = 'text-muted',
		trailingText,
		trailingTextClass = 'text-xs text-muted',
		...rest
	} = $props();

	const countClass =
		'rounded-full border border-surface-200-800 bg-surface-100-900/70 px-2 py-0.5 text-[10px] font-semibold tracking-wide uppercase text-surface-600-400';
	const countText = $derived(
		typeof count === 'number' && countVariant !== 'none'
			? countVariant === 'number'
				? `${count}`
				: count === 0
					? countEmptyLabel
					: `${count} ${count === 1 ? countItemLabel : `${countItemLabel}s`}`
			: null
	);

	const triggerClasses = $derived(
		['layout-x-between w-full px-low py-low text-left transition-surface', triggerClass]
			.filter(Boolean)
			.join(' ')
	);

	const panelClasses = $derived(
		['px-low pb-low transition-surface', panelClass].filter(Boolean).join(' ')
	);
	const header = $derived(control ?? defaultHeader);
	const body = $derived(panel ?? children);
	const resolvedLead = $derived(lead ?? (leadingIcon ? defaultLead : undefined));
	const resolvedTrail = $derived(trail ?? defaultTrail);
	const hasTrailContent = $derived(Boolean(countText || trailingText));
</script>

{#snippet indicator(attrs)}
	{@const state = attrs?.['data-state']}
	{@const merged = {
		...attrs,
		class: [attrs?.class, 'inline-flex items-center transition-transform duration-200']
			.filter(Boolean)
			.join(' ')
	}}
	{@const iconClasses = ['transition-transform duration-200', state === 'open' ? 'rotate-90' : '']
		.filter(Boolean)
		.join(' ')}
	<span {...merged} aria-hidden="true">
		<Ico icon="mdi:chevron-right" size={3} class={iconClasses} />
	</span>
{/snippet}

{#snippet defaultHeader()}
	<span class="font-semibold">Section</span>
{/snippet}

{#snippet defaultLead({ leadingIcon, leadingIconClass })}
	<span aria-hidden="true" class={`mt-0.5 ${leadingIconClass}`}>{leadingIcon}</span>
{/snippet}

{#snippet defaultTrail({ count, countText, countClass, trailingText, trailingTextClass })}
	{#if countText}
		<span class={`${countClass} ${count === 0 ? 'border-dashed text-surface-600-400' : ''}`}
			>{countText}</span
		>
	{/if}
	{#if trailingText}
		<span class={trailingTextClass}>{trailingText}</span>
	{/if}
{/snippet}

<Accordion.Item
	{value}
	class={cls}
	aria-disabled={disabled || undefined}
	data-disabled={disabled ? '' : undefined}
	{...rest}
>
	<Accordion.ItemTrigger class={triggerClasses} {disabled}>
		{#if control}
			<div class="layout-x-between w-full">
				<div class="min-w-0 grow">
					{@render control?.()}
				</div>
				<span class="shrink-0 text-muted">
					<Accordion.ItemIndicator element={indicator} />
				</span>
			</div>
		{:else if !control && (Boolean(resolvedLead) || title || subtitle || meta || hasTrailContent || Boolean(trail))}
			<div class="layout-x-between w-full">
				<div class="layout-x-start-low min-w-0 grow items-center">
					{@render resolvedLead?.({
						leadingIcon,
						leadingIconClass
					})}
					<div class="layout-y-start-low min-w-0">
						{#if title}
							<span class="text-sm font-semibold text-strong">
								{title}
							</span>
						{/if}
						{#if subtitle}
							<span class="text-xs text-surface-600-400">{subtitle}</span>
						{/if}
						{#if meta}
							<span class="text-xs text-surface-600-400">{meta}</span>
						{/if}
					</div>
				</div>
				<div class="layout-x-low shrink-0 items-center">
					{@render resolvedTrail?.({
						count,
						countText,
						countClass,
						trailingText,
						trailingTextClass
					})}
					<span class="shrink-0 text-muted">
						<Accordion.ItemIndicator element={indicator} />
					</span>
				</div>
			</div>
		{:else}
			{@render header()}
			<span class="shrink-0 text-muted">
				<Accordion.ItemIndicator element={indicator} />
			</span>
		{/if}
	</Accordion.ItemTrigger>
	<Accordion.ItemContent>
		{#snippet element(attributes)}
			{#if !attributes.hidden}
				<div {...attributes} class={panelClasses} transition:slide>
					{@render body?.()}
				</div>
			{/if}
		{/snippet}
	</Accordion.ItemContent>
</Accordion.Item>
