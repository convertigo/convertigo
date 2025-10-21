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
	countLabel?: (value: number) => string;
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
		countLabel = (value) => `${value} item${value === 1 ? '' : 's'}`,
		leadingIcon,
		leadingIconClass = 'text-surface-500',
		trailingText,
		trailingTextClass = 'text-xs text-surface-500',
		...rest
	} = $props();

	const triggerClasses = [
		'layout-x-between w-full rounded-sm px-low py-low text-left',
		triggerClass
	]
		.filter(Boolean)
		.join(' ');

	const panelClasses = ['px-low pb-low', panelClass].filter(Boolean).join(' ');
	const header = control ?? defaultHeader;
	const body = panel ?? children;
	const showDefaultHeader =
		!control &&
		(title || subtitle || typeof count === 'number' || meta || leadingIcon || trailingText);
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
				<span class="shrink-0 text-surface-500">
					<Accordion.ItemIndicator element={indicator} />
				</span>
			</div>
		{:else if showDefaultHeader}
			<div class="layout-x-between w-full">
				<div class="layout-x-start-low min-w-0 grow">
					{#if leadingIcon}
						<span aria-hidden="true" class={`mt-0.5 ${leadingIconClass}`}>{leadingIcon}</span>
					{/if}
					<div class="layout-y-low min-w-0">
						{#if title}
							<span class="text-sm font-semibold text-surface-900 dark:text-surface-50"
								>{title}</span
							>
						{/if}
						{#if subtitle}
							<span class="text-surface-500-300 text-xs">{subtitle}</span>
						{/if}
						{#if meta}
							<span class="text-surface-500-300 text-xs">{meta}</span>
						{/if}
					</div>
				</div>
				<div class="layout-x-low shrink-0">
					{#if typeof count === 'number'}
						<span
							class="rounded-full border border-surface-300-700/60 px-2 py-1 text-[11px] font-semibold tracking-wide text-surface-500 uppercase"
						>
							{countLabel(count)}
						</span>
					{/if}
					{#if trailingText}
						<span class={trailingTextClass}>{trailingText}</span>
					{/if}
					<span class="shrink-0 text-surface-500">
						<Accordion.ItemIndicator element={indicator} />
					</span>
				</div>
			</div>
		{:else}
			{@render header()}
			<span class="shrink-0 text-surface-500">
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
