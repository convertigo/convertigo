<script>
	import { Portal, Tooltip } from '@skeletonlabs/skeleton-svelte';
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{ label?: string, icon?: string, size?: string, cls?: string, class?: string, disabled?: boolean, value?: string, hidden?: boolean, href?: string, type?: string } | any} */
	let {
		label,
		icon,
		size = 'btn',
		cls: _cls,
		class: _cls2,
		disabled,
		value,
		hidden,
		href,
		type = 'button',
		title,
		tooltip,
		ariaLabel,
		full = true,
		tooltipPlacement = 'top',
		...rest
	} = $props();
	let cls = $derived(_cls ?? _cls2 ?? '');
	let tooltipText = $derived((tooltip ?? title ?? '').trim());
	let hasTooltip = $derived(tooltipText.length > 0);
	let isLink = $derived(Boolean(href && !disabled));
	let useSkeletonTooltip = $derived(hasTooltip);
	let nativeTitle = $derived(useSkeletonTooltip ? undefined : (title ?? label));
	let computedAriaLabel = $derived(ariaLabel ?? (tooltipText || title || label));
	/** @param {any} value */
	const asAny = (value) => value;
</script>

{#if !hidden}
	{#if useSkeletonTooltip}
		<Tooltip positioning={{ placement: tooltipPlacement }}>
			<Tooltip.Trigger {...rest}>
				{#snippet element(attributes)}
					{@const triggerAttributes = asAny(attributes)}
					{#if isLink}
						<a
							{...triggerAttributes}
							{href}
							class={[cls, 'h-full min-h-fit text-wrap', full && 'w-full']}
							aria-label={computedAriaLabel}
						>
							{#if icon}<span><Ico {icon} {size} /></span>{/if}{#if label}<span>{label}</span
								>{/if}</a
						>
					{:else}
						<button
							{...triggerAttributes}
							{disabled}
							class={[cls, 'h-full min-h-fit text-wrap', full && 'w-full']}
							{type}
							{value}
							aria-label={computedAriaLabel}
						>
							{#if icon}<span><Ico {icon} {size} /></span>{/if}{#if label}<span>{label}</span
								>{/if}</button
						>
					{/if}
				{/snippet}
			</Tooltip.Trigger>
			<Portal>
				<Tooltip.Positioner class="z-[120]" style="z-index: 120;">
					<Tooltip.Content class="card preset-filled-surface-950-50 p-2 text-xs leading-none">
						<span>{tooltipText}</span>
						<Tooltip.Arrow
							class="[--arrow-background:var(--color-surface-950-50)] [--arrow-size:--spacing(2)]"
						>
							<Tooltip.ArrowTip />
						</Tooltip.Arrow>
					</Tooltip.Content>
				</Tooltip.Positioner>
			</Portal>
		</Tooltip>
	{:else if isLink}
		<a
			{href}
			class={[cls, 'h-full min-h-fit text-wrap', full && 'w-full']}
			title={nativeTitle}
			aria-label={computedAriaLabel}
			{...rest}
		>
			{#if icon}<span><Ico {icon} {size} /></span>{/if}{#if label}<span>{label}</span>{/if}</a
		>
	{:else}
		<button
			{disabled}
			class={[cls, 'h-full min-h-fit text-wrap', full && 'w-full']}
			{type}
			{value}
			title={nativeTitle}
			aria-label={computedAriaLabel}
			{...rest}
		>
			{#if icon}<span><Ico {icon} {size} /></span>{/if}{#if label}<span>{label}</span>{/if}</button
		>
	{/if}
{/if}
