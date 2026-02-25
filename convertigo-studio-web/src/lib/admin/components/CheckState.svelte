<script>
	import { Portal, Switch, Tooltip } from '@skeletonlabs/skeleton-svelte';

	/** @type {{name?: string, values?: any[], value: string, class?: string, disabled?: boolean, title?: string, ariaLabel?: string, 'aria-label'?: string, onchange?: any, children?: import('svelte').Snippet} | any}*/
	let {
		name = '',
		values = ['false', 'true'],
		value = $bindable(values[0]),
		class: cls = '',
		disabled = false,
		title,
		tooltip,
		ariaLabel,
		tooltipPlacement = 'top',
		children,
		...rest
	} = $props();

	let tooltipText = $derived((tooltip ?? title ?? '').trim());
	let hasTooltip = $derived(tooltipText.length > 0);
	let nativeTitle = $derived(title);
	let computedAriaLabel = $derived(ariaLabel ?? rest?.['aria-label'] ?? title ?? tooltipText);
	/** @param {any} value */
	const asAny = (value) => value;
</script>

{#snippet tooltipPanel()}
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
{/snippet}

{#snippet switchControl()}
	<Switch.Control class="c8o-switch min-w-10 transition-surface">
		<Switch.Thumb />
	</Switch.Control>
{/snippet}

{#snippet switchElement(attributes = {})}
	<Switch
		{...attributes}
		{...rest}
		{name}
		{disabled}
		value={values[1]}
		class={cls}
		checked={value == values[1]}
		title={nativeTitle}
		aria-label={computedAriaLabel}
		onCheckedChange={(e) => {
			value = e.checked ? values[1] : values[0];
			rest.onCheckedChange?.(e);
		}}
	>
		{#if hasTooltip}
			<Tooltip positioning={{ placement: tooltipPlacement }}>
				<Tooltip.Trigger>
					{#snippet element(attributes)}
						{@const triggerAttributes = asAny(attributes)}
						<span {...triggerAttributes} class="inline-flex">
							{@render switchControl()}
						</span>
					{/snippet}
				</Tooltip.Trigger>
				{@render tooltipPanel()}
			</Tooltip>
		{:else}
			{@render switchControl()}
		{/if}
		<Switch.Label class="text-sm leading-tight font-medium text-current"
			>{@render children?.()}</Switch.Label
		>
		<Switch.HiddenInput />
		{#if value != values[1] && Array.isArray(values)}
			<input type="hidden" {name} value={values[0]} />
		{/if}
	</Switch>
{/snippet}

{@render switchElement()}
