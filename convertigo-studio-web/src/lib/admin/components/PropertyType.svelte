<script module>
	let cpt = 0;
</script>

<script>
	import { SegmentedControl, Switch } from '@skeletonlabs/skeleton-svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { checkArray } from '$lib/utils/service';
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
		...rest
	} = $props();
	let label = $derived(description ?? _label);
	let labelLines = $derived.by(() => (label ? String(label).split('\n') : []));
	let isMultiSelect = $derived.by(() => Boolean(rest.multiple) || Number(rest.size) > 1);
	let restores = $derived.by(() => {
		const r = [];
		if (originalValue != null) {
			r.push({ icon: 'mdi:arrow-u-left-top', val: originalValue, title: 'reset' });
		}
		if (defaultValue != null) {
			r.push({ icon: 'mdi:backup-restore', val: defaultValue, title: 'restore' });
		}
		return r;
	});
	let type = $derived(_type?.toLocaleLowerCase());
	let id = `property-input-${cpt++}`;

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
		rest.onchange?.({ target: select });
	}
</script>

<div class="layout-y-low sm:layout-x-low" class:w-fit={fit} class:w-full={!fit}>
	<div class="max-sm:self-stretch sm:grow">
		{#if type == 'boolean'}
			<CheckState {name} {...rest} bind:value>{label}</CheckState>
		{:else if type == 'check'}
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
				<Switch.Label class="text-sm leading-tight font-medium text-current">{label}</Switch.Label>
				<Switch.HiddenInput />
			</Switch>
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
								'gap-0.5',
								'rounded-base',
								'border',
								'border-surface-100-900',
								'bg-surface-200-800',
								'p-0.5',
								'shadow-none',
								'overflow-hidden',
								rest?.orientation == 'vertical' ? 'flex-col' : 'flex-row'
							]}
						>
							<SegmentedControl.Indicator class="rounded-[0.3rem] bg-primary-600 shadow-none" />
							{#each item as option (option.value ?? option)}
								{@const val = option.value ?? option}
								{@const txt = option.text ?? option['#text'] ?? val}
								<SegmentedControl.Item value={val} class="relative flex-1">
									<SegmentedControl.ItemText
										class={[
											value == val ? 'text-white' : 'text-muted',
											'px-3 py-1.5 text-sm font-medium'
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
			</div>
		{/if}
	</div>
	{#if restores.length > 0 || buttons.length > 0}
		<div class="layout-x-low justify-around! sm:layout-y-low sm:h-full">
			{#each restores as { icon, val, title }, idx (idx)}
				{@const displayVal = val == null ? '' : String(val)}
				{@const label = displayVal.length ? `${title}: ${displayVal}` : title}
				<button
					disabled={value == val}
					type="button"
					onclick={() => (value = val)}
					title={label}
					aria-label={label}
					class="btn bg-surface-200-800 btn-sm"
				>
					<Ico {icon} />
				</button>
			{/each}
			{#each buttons as { disabled, onclick, title, icon }, idx (idx)}
				{@const label = title ?? icon ?? 'action'}
				<button
					{disabled}
					type="button"
					{onclick}
					title={label}
					aria-label={label}
					class="btn bg-surface-200-800 btn-sm"
				>
					<Ico {icon} />
				</button>
			{/each}
		</div>
	{/if}
</div>
