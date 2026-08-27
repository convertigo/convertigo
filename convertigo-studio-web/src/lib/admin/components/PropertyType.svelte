<script module>
	let cpt = 0;
</script>

<script>
	import { Portal, SegmentedControl, Switch, Tooltip } from '@skeletonlabs/skeleton-svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import { checkArray } from '$lib/utils/service';
	import Button from './Button.svelte';
	import CheckState from './CheckState.svelte';

	/** @type {{value: string, mode?: string, checked?: boolean, label?: string, description?: string, name?: string, item?: any, type?: string, defaultValue?:string, originalValue?:string, defaultMode?: string, originalMode?: string, loading?:boolean, placeholder?: string, multiple?: boolean, rows?: number, adaptiveTextarea?: boolean, segmentCompact?: boolean}|any} */
	let {
		value = $bindable(''),
		mode = $bindable('plain'),
		checked = $bindable(false),
		label: _label,
		description,
		name,
		item,
		type: _type = 'text',
		defaultValue,
		originalValue,
		defaultMode,
		originalMode,
		loading = false,
		placeholder = 'Enter value …',
		fit = false,
		rows = undefined,
		adaptiveTextarea = false,
		segmentCompact = false,
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
	let inputType = $derived(isPasswordType ? 'text' : type);
	let inputAutocomplete = $derived(rest.autocomplete ?? 'off');
	let inputStyle = $derived(
		[rest.style, isPasswordType ? '-webkit-text-security: disc' : undefined]
			.filter(Boolean)
			.join('; ')
	);
	let isSmartType = $derived(type === 'smarttype');
	let smartMode = $derived(['plain', 'script', 'source'].includes(mode) ? mode : 'plain');
	let smartValue = $derived(stringifyValue(value));
	let smartRows = $derived.by(() => {
		if (rows) {
			return rows;
		}
		const lines = smartValue ? smartValue.split(/\r\n|\r|\n/).length : 1;
		return Math.min(Math.max(lines, 1), 3);
	});
	let restores = $derived.by(() => {
		const r = [];
		if (originalValue != null || (isSmartType && originalMode != null)) {
			r.push({
				icon: 'mdi:arrow-u-left-top',
				val: originalValue,
				modeVal: originalMode,
				title: 'Reset to original value'
			});
		}
		if (!isPasswordType && (defaultValue != null || (isSmartType && defaultMode != null))) {
			r.push({
				icon: 'mdi:backup-restore',
				val: defaultValue,
				modeVal: defaultMode,
				title: 'Restore default value'
			});
		}
		return r;
	});
	let isVerticalSegment = $derived(rest?.orientation == 'vertical');
	let isTinySegment = $derived.by(() => {
		if (!segmentCompact || !Array.isArray(item)) {
			return false;
		}
		return item.every((option) => {
			const val = option?.value ?? option;
			const txt = option?.text ?? option?.['#text'] ?? val;
			return String(txt).length <= 3;
		});
	});
	let id = `property-input-${cpt++}`;
	/** @param {any} value */
	const asAny = (value) => value;

	/**
	 * @param {any} value
	 * @returns {string}
	 */
	function stringifyValue(value) {
		if (value == null) {
			return '';
		}
		if (Array.isArray(value)) {
			return value.join('\n');
		}
		if (typeof value === 'object') {
			return JSON.stringify(value, null, 2);
		}
		return String(value);
	}

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

	/** @param {any} candidate */
	function nativeColorValue(candidate) {
		const text = String(candidate ?? '').trim();
		if (/^#[0-9a-f]{6}$/i.test(text)) return text;
		if (/^#[0-9a-f]{3}$/i.test(text)) {
			return `#${[...text.slice(1)].map((part) => part.repeat(2)).join('')}`;
		}
		const rgb = text.match(
			/^rgba?\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})(?:\s*,[^)]*)?\)$/i
		);
		if (!rgb) return '#000000';
		return `#${rgb
			.slice(1, 4)
			.map((part) => Math.min(255, Number(part)).toString(16).padStart(2, '0'))
			.join('')}`;
	}

	/** @param {any} candidate */
	function semanticColorStyle(candidate) {
		const colors = {
			current: 'currentColor',
			auto: 'currentColor',
			neutral: 'var(--color-surface-500)',
			primary: 'var(--color-primary-500)',
			secondary: 'var(--color-secondary-500)',
			tertiary: 'var(--color-tertiary-500)',
			success: 'var(--color-success-500)',
			warning: 'var(--color-warning-500)',
			danger: 'var(--color-error-500)',
			error: 'var(--color-error-500)',
			muted: 'var(--color-surface-400)'
		};
		return `background-color: ${colors[String(candidate ?? '').toLowerCase()] ?? 'transparent'}`;
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
					{#if type == 'smarttype'}
						<div class="layout-x-wrap-low w-full">
							<div class="min-w-0 flex-1">
								{#if smartMode == 'source'}
									<code
										{id}
										class="flex h-9 input-common min-w-0 items-center overflow-hidden px-3 text-sm text-ellipsis whitespace-nowrap"
										title={smartValue}>{smartValue || placeholder}</code
									>
								{:else if smartMode == 'script'}
									<textarea
										{id}
										{name}
										autocomplete={inputAutocomplete}
										{placeholder}
										rows={smartRows}
										{...rest}
										class={[
											'min-h-9 input-common resize-y px-3 py-2 text-sm leading-tight',
											rest?.class
										]}
										bind:value></textarea>
								{:else}
									<input
										{...rest}
										{id}
										{name}
										autocomplete={inputAutocomplete}
										{placeholder}
										type="text"
										disabled={loading}
										class:animate-pulse={loading}
										class="h-9 input-common px-3 text-sm placeholder:text-surface-600-400 {rest?.class ??
											''}"
										bind:value
									/>
								{/if}
							</div>
							<div class="min-w-fit flex-none">
								<SegmentedControl
									name={name ? `${name}-mode` : undefined}
									value={smartMode}
									onValueChange={(event) => {
										mode = event.value ?? 'plain';
										rest.onModeChange?.(event);
									}}
									class="w-fit max-w-full"
								>
									<SegmentedControl.Control
										class="relative min-h-7 input-common w-fit max-w-full flex-row flex-wrap gap-0.5 overflow-hidden p-[1px] shadow-none"
									>
										<SegmentedControl.Indicator class="rounded-base bg-primary-500 shadow-none" />
										{#each item ?? [] as option (option.value ?? option)}
											{@const val = option.value ?? option}
											{@const txt = option.text ?? option['#text'] ?? val}
											<SegmentedControl.Item
												value={val}
												class="relative w-8 flex-none !gap-0 !px-0 !py-0"
											>
												<SegmentedControl.ItemText
													class={[
														smartMode == val ? 'text-white' : 'text-surface-700-300',
														'flex h-7 !w-8 !min-w-8 items-center justify-center overflow-hidden px-0 text-xs leading-none font-medium text-ellipsis whitespace-nowrap'
													]}
												>
													{txt}
												</SegmentedControl.ItemText>
												<SegmentedControl.ItemHiddenInput />
											</SegmentedControl.Item>
										{/each}
									</SegmentedControl.Control>
								</SegmentedControl>
							</div>
						</div>
					{:else if type == 'segment'}
						<SegmentedControl
							{...rest}
							name={name ?? []}
							{value}
							onValueChange={(event) => {
								value = event.value ?? '';
								rest.onValueChange?.(event);
							}}
							class={fit || segmentCompact ? 'w-fit max-w-full' : 'w-full'}
						>
							<SegmentedControl.Control
								class={[
									'relative',
									'input-common',
									segmentCompact ? 'min-h-7' : isVerticalSegment ? 'h-auto' : 'h-9',
									'gap-0.5',
									'p-[1px]',
									'shadow-none',
									'overflow-hidden',
									segmentCompact ? 'w-fit max-w-full flex-wrap' : undefined,
									isVerticalSegment ? 'flex-col' : 'flex-row'
								]}
							>
								<SegmentedControl.Indicator class="rounded-base bg-primary-500 shadow-none" />
								{#each item as option (option.value ?? option)}
									{@const val = option.value ?? option}
									{@const txt = option.text ?? option['#text'] ?? val}
									<SegmentedControl.Item
										value={val}
										class={[
											'relative',
											segmentCompact ? '!gap-0 !px-0 !py-0' : undefined,
											segmentCompact
												? isTinySegment
													? 'w-8 flex-none'
													: 'min-w-0 flex-[1_1_0]'
												: isVerticalSegment
													? 'w-full flex-none'
													: 'flex-1'
										]}
									>
										<SegmentedControl.ItemText
											class={[
												value == val ? 'text-white' : 'text-surface-700-300',
												'flex items-center justify-center leading-none font-medium',
												segmentCompact
													? isTinySegment
														? 'h-7 !w-8 !min-w-8 overflow-hidden px-0 text-xs text-ellipsis whitespace-nowrap'
														: 'h-7 max-w-full min-w-8 overflow-hidden px-1 text-xs text-ellipsis whitespace-nowrap'
													: 'px-3 py-1 text-[14px]',
												!segmentCompact && !isVerticalSegment && 'h-full'
											]}
										>
											{txt}
										</SegmentedControl.ItemText>
										<SegmentedControl.ItemHiddenInput />
									</SegmentedControl.Item>
								{/each}
							</SegmentedControl.Control>
						</SegmentedControl>
					{:else if type == 'color-combo'}
						<div class="layout-x-low w-full items-center">
							<span
								class="h-6 w-6 flex-none rounded-full border border-surface-300-700 shadow-sm"
								style={semanticColorStyle(value)}
								aria-hidden="true"
							></span>
							<select
								{...rest}
								{name}
								class="select input-common h-9 min-w-0 flex-1 px-3 text-sm {rest?.class ?? ''}"
								{id}
								bind:value
							>
								{#each item as option (option.value ?? option)}
									{@const val = option.value ?? option}
									{@const txt = option.text ?? option['#text'] ?? val}
									<option class="ig-select" value={val}>{txt}</option>
								{/each}
							</select>
						</div>
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
					{:else if type == 'color'}
						<div class="layout-x-low w-full items-center">
							<input
								type="color"
								value={nativeColorValue(value)}
								disabled={loading}
								aria-label={label ? `${label} color picker` : 'Color picker'}
								class="h-9 w-10 flex-none cursor-pointer rounded-base border border-surface-300-700 bg-transparent p-1"
								oninput={(event) => (value = event.currentTarget.value)}
							/>
							<input
								{...rest}
								{id}
								{name}
								autocomplete={inputAutocomplete}
								{placeholder}
								type="text"
								disabled={loading}
								class="h-9 input-common min-w-0 flex-1 px-3 text-sm placeholder:text-surface-600-400 {rest?.class ??
									''}"
								bind:value
							/>
						</div>
					{:else if type == 'array' || type == 'textarea'}
						<textarea
							{id}
							{name}
							autocomplete={inputAutocomplete}
							{placeholder}
							{rows}
							{...rest}
							class={[
								adaptiveTextarea ? 'min-h-9 resize-y leading-tight' : 'min-h-24',
								'input-common px-3 py-2 text-sm',
								rest?.class
							]}
							bind:value></textarea>
					{:else}
						<input
							{...rest}
							{id}
							{name}
							autocomplete={inputAutocomplete}
							{placeholder}
							type={inputType}
							style={inputStyle}
							autocapitalize={isPasswordType ? 'none' : rest.autocapitalize}
							spellcheck={isPasswordType ? false : rest.spellcheck}
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
			{#each restores as { icon, val, modeVal, title }, idx (idx)}
				{@const displayVal = val == null ? '' : String(val)}
				{@const label = displayVal.length ? `${title}: ${displayVal}` : title}
				<Button
					full={false}
					size={4}
					disabled={value == val && (!isSmartType || modeVal == null || mode == modeVal)}
					{icon}
					title={label}
					ariaLabel={label}
					onclick={() => {
						if (isSmartType && modeVal != null) {
							mode = modeVal;
						}
						value = val;
					}}
					class="inline-flex h-7 w-7 items-center justify-center rounded-base p-0! text-primary-500 transition-surface hover:text-primary-600 disabled:pointer-events-none disabled:text-surface-600-400"
				/>
			{/each}
			{#each buttons as { disabled, onclick, title, icon, active, ariaExpanded }, idx (idx)}
				{@const label = title ?? icon ?? 'action'}
				<Button
					full={false}
					size={4}
					{disabled}
					{icon}
					title={label}
					ariaLabel={label}
					aria-expanded={ariaExpanded}
					{onclick}
					class={[
						'inline-flex h-7 w-7 items-center justify-center rounded-base p-0! text-primary-500 transition-surface hover:text-primary-600 disabled:pointer-events-none disabled:text-surface-600-400',
						active && 'bg-primary-500/12 ring-1 ring-primary-500/40'
					]}
				/>
			{/each}
		</div>
	{/if}
</div>
