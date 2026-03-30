<script module>
	let cpt = 0;
</script>

<script>
	import { Portal, SegmentedControl, Switch, Tooltip } from '@skeletonlabs/skeleton-svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import { checkArray } from '$lib/utils/service';
	import Button from './Button.svelte';
	import CheckState from './CheckState.svelte';

	/** @type {{value: string, checked?: boolean, label?: string, description?: string, name?: string, item?: any, type?: string, defaultValue?:string, originalValue?:string, loading?:boolean, placeholder?: string, multiple?: boolean}|any} */
	let {
		value = $bindable(''),
		checked = $bindable(false),
		label: _label,
		description,
		name,
		item,
		type: _type = 'text',
		defaultValue,
		originalValue,
		loading = false,
		placeholder = 'Enter value …',
		fit = false,
		buttons = [],
		actionsHorizontal = false,
		title,
		tooltip,
		tooltipPlacement = 'top',
		...rest
	} = $props();
	let label = $derived(description ?? _label);
	let labelLines = $derived.by(() => (label ? String(label).split('\n') : []));
	let tooltipText = $derived((tooltip ?? title ?? '').trim());
	let hasTooltip = $derived(tooltipText.length > 0);
	let isMultiSelect = $derived.by(() => Boolean(rest.multiple) || Number(rest.size) > 1);
	let rawType = $derived((_type ?? 'text').toLocaleLowerCase());
	let isPasswordType = $derived(rawType.startsWith('password'));
	let type = $derived(isPasswordType ? 'password' : rawType);
	let restores = $derived.by(() => {
		const r = [];
		if (originalValue != null) {
			r.push({
				icon: 'mdi:arrow-u-left-top',
				val: originalValue,
				title: 'Reset to original value'
			});
		}
		if (!isPasswordType && defaultValue != null) {
			r.push({
				icon: 'mdi:backup-restore',
				val: defaultValue,
				title: 'Restore default value'
			});
		}
		return r;
	});
	let isVerticalSegment = $derived(rest?.orientation == 'vertical');
	let id = `property-input-${cpt++}`;
	/** @param {any} value */
	const asAny = (value) => value;

	function handleMultiple(e) {
		e.preventDefault();
		const select = e.target.parentElement;
		value = checkArray(value);
		if (e.target?.selected) {
			value = value.filter((v) => v != e.target?.value);
		} else {
			value.push(e.target?.value);
		}
		const scroll = select.scrollTop;
		setTimeout(() => {
			select.scrollTop = scroll;
		}, 1);
		rest.onchange?.({ target: select, detail: { value } });
	}
</script>

<div class="layout-y-low sm:layout-x-low" class:w-fit={fit} class:w-full={!fit}>
	<div class="max-sm:self-stretch sm:grow">
		{#if type == 'boolean'}
			<CheckState {name} {...rest} bind:value tooltip={tooltipText} {tooltipPlacement}
				>{label}</CheckState
			>
		{:else if type == 'check'}
			{#snippet switchControl()}
				<Switch
					{...rest}
					{name}
					{value}
					{checked}
					onCheckedChange={(e) => {
						checked = e.checked;
						rest.onCheckedChange?.(e);
					}}
					class="inline-flex min-w-10 items-center gap-low"
				>
					<Switch.Control class="c8o-switch transition-surface {rest?.class ?? ''}">
						<Switch.Thumb />
					</Switch.Control>
					<Switch.Label class="text-sm leading-tight font-medium text-current">{label}</Switch.Label
					>
					<Switch.HiddenInput />
				</Switch>
			{/snippet}
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
					<Portal>
						<Tooltip.Positioner class="z-[120]" style="z-index: 120;">
							<Tooltip.Content
								class="card preset-filled-surface-950-50 p-2 text-xs leading-none break-all whitespace-pre-line"
							>
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
			{:else}
				{@render switchControl()}
			{/if}
		{:else}
			{@const autocomplete = 'one-time-code'}
			<div class="layout-y-stretch-none gap-1">
				{#if label}
					<AutoPlaceholder {loading}>
						<label class="label-common" for={id}>
							{#each labelLines as line, idx (line + idx)}
								{line}
								{#if idx < labelLines.length - 1}<br />{/if}
							{/each}
						</label>
					</AutoPlaceholder>
				{/if}
				{#snippet fieldControl()}
					{#if type == 'segment'}
						<SegmentedControl
							{...rest}
							name={name ?? []}
							{value}
							onValueChange={(event) => {
								value = event.value ?? '';
								rest.onValueChange?.(event);
							}}
							class={fit ? 'w-fit' : 'w-full'}
						>
							<SegmentedControl.Control
								class={[
									'relative',
									'input-common',
									isVerticalSegment ? 'h-auto' : 'h-9',
									'gap-0.5',
									'p-[1px]',
									'shadow-none',
									'overflow-hidden',
									isVerticalSegment ? 'flex-col' : 'flex-row'
								]}
							>
								<SegmentedControl.Indicator class="rounded-base bg-primary-500 shadow-none" />
								{#each item as option (option.value ?? option)}
									{@const val = option.value ?? option}
									{@const txt = option.text ?? option['#text'] ?? val}
									<SegmentedControl.Item
										value={val}
										class={['relative', isVerticalSegment ? 'w-full flex-none' : 'flex-1']}
									>
										<SegmentedControl.ItemText
											class={[
												value == val ? 'text-white' : 'text-surface-700-300',
												'flex items-center px-3 py-1 text-[14px] leading-none font-medium',
												!isVerticalSegment && 'h-full'
											]}
										>
											{txt}
										</SegmentedControl.ItemText>
										<SegmentedControl.ItemHiddenInput />
									</SegmentedControl.Item>
								{/each}
							</SegmentedControl.Control>
						</SegmentedControl>
					{:else if type == 'combo'}
						<select
							{...rest}
							{name}
							class={`select input-common overflow-auto px-3 text-sm ${
								isMultiSelect ? 'h-auto min-h-24 py-2' : 'h-9'
							} ${rest?.class ?? ''}`}
							{id}
							bind:value
						>
							{#each item as option (option.value ?? option)}
								{@const val = option.value ?? option}
								{@const txt = option.text ?? option['#text'] ?? val}
								{#if rest.multiple ?? true}
									<option class="ig-select" value={val} onmousedown={handleMultiple}>{txt}</option>
								{:else}
									<option class="ig-select" value={val}>{txt}</option>
								{/if}
							{/each}
						</select>
					{:else if type == 'array' || type == 'textarea'}
						<textarea
							{id}
							{name}
							{autocomplete}
							{placeholder}
							{...rest}
							class="min-h-24 input-common px-3 py-2 text-sm {rest?.class ?? ''}"
							bind:value
						></textarea>
					{:else}
						<input
							{...rest}
							{id}
							{name}
							{autocomplete}
							{placeholder}
							{type}
							disabled={loading}
							class:animate-pulse={loading}
							class="h-9 input-common px-3 text-sm placeholder:text-surface-600-400 {rest?.class ??
								''}"
							bind:value
						/>
					{/if}
				{/snippet}
				{#if hasTooltip}
					<Tooltip positioning={{ placement: tooltipPlacement }}>
						<Tooltip.Trigger>
							{#snippet element(attributes)}
								{@const triggerAttributes = asAny(attributes)}
								<span {...triggerAttributes} class="inline-flex w-full">
									{@render fieldControl()}
								</span>
							{/snippet}
						</Tooltip.Trigger>
						<Portal>
							<Tooltip.Positioner class="z-[120]" style="z-index: 120;">
								<Tooltip.Content
									class="card preset-filled-surface-950-50 p-2 text-xs leading-none break-all whitespace-pre-line"
								>
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
				{:else}
					{@render fieldControl()}
				{/if}
			</div>
		{/if}
	</div>
	{#if restores.length > 0 || buttons.length > 0}
		<div
			class={actionsHorizontal
				? 'layout-x-low h-fit items-center justify-start'
				: 'layout-x-low justify-around! sm:layout-y-low sm:h-full'}
		>
			{#each restores as { icon, val, title }, idx (idx)}
				{@const displayVal = val == null ? '' : String(val)}
				{@const label = displayVal.length ? `${title}: ${displayVal}` : title}
				<Button
					full={false}
					size={4}
					disabled={value == val}
					{icon}
					title={label}
					ariaLabel={label}
					onclick={() => (value = val)}
					class="inline-flex h-7 w-7 items-center justify-center rounded-base p-0! text-primary-500 transition-surface hover:text-primary-600 disabled:pointer-events-none disabled:text-surface-600-400"
				/>
			{/each}
			{#each buttons as { disabled, onclick, title, icon }, idx (idx)}
				{@const label = title ?? icon ?? 'action'}
				<Button
					full={false}
					size={4}
					{disabled}
					{icon}
					title={label}
					ariaLabel={label}
					{onclick}
					class="inline-flex h-7 w-7 items-center justify-center rounded-base p-0! text-primary-500 transition-surface hover:text-primary-600 disabled:pointer-events-none disabled:text-surface-600-400"
				/>
			{/each}
		</div>
	{/if}
</div>
