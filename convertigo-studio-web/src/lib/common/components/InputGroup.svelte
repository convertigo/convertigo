<script module>
	let ids = 0;
</script>

<script>
	import Ico from '$lib/utils/Ico.svelte';

	const join = (...classes) => classes.filter(Boolean).join(' ');
	const baseGroup =
		'input-group w-full min-h-9 grid-cols-[auto_1fr_auto] items-center gap-0 rounded-base border border-transparent bg-surface-200-800 text-strong transition-surface focus-within:outline focus-within:outline-1 focus-within:outline-primary-500 focus-within:outline-offset-[-1px]';
	const baseLabel = 'flex h-full items-center px-3 text-surface-700-300 cursor-text';
	const baseInput =
		'h-full w-full min-w-0 border-none bg-transparent px-3 text-sm text-inherit placeholder:text-surface-600-400 focus-visible:outline-none focus:ring-0';
	const baseActions = 'layout-x-none h-full items-center gap-[1px]! border-l-0!';

	let {
		class: className = '',
		icon = '',
		iconSize = 'nav',
		iconClass = '',
		labelClass = '',
		inputClass = '',
		actionsClass = '',
		placeholder = '',
		type = 'text',
		id = '',
		name = '',
		value = $bindable(''),
		disabled = false,
		rightIcon = '',
		rightIconSize = 'nav',
		rightIconClass = '',
		leading = undefined,
		actions = undefined,
		...rest
	} = $props();
	let inputId = $derived(id || name || `input-group-${ids++}`);
	const setValue = (next) => (value = next);
	const showClear = $derived(
		type === 'search' && (value?.length ?? 0) > 0 && !actions && !rightIcon
	);
	const hasActions = $derived(!!actions || !!rightIcon || showClear);
</script>

<div class={join(baseGroup, className)}>
	<label for={inputId} class={join('ig-cell', baseLabel, labelClass)}>
		{#if leading}
			{@render leading({ id: inputId, value, setValue })}
		{:else if icon}
			<Ico {icon} size={iconSize} class={iconClass} />
		{/if}
	</label>
	<input
		id={inputId}
		{name}
		{type}
		{placeholder}
		{disabled}
		class={join('ig-input', baseInput, inputClass)}
		bind:value
		{...rest}
	/>
	{#if hasActions}
		<span class={join('ig-actions', baseActions, actionsClass)}>
			{@render actions?.({ value, setValue, disabled })}
			{#if rightIcon}
				<Ico icon={rightIcon} size={rightIconSize} class={rightIconClass} />
			{/if}
			{#if showClear}
				<button
					type="button"
					class="button-ico-secondary h-7 w-7 p-0! text-surface-700-300 hover:text-surface-800-200"
					onclick={() => setValue('')}
				>
					<Ico icon="mdi:close" size="nav" />
				</button>
			{/if}
		</span>
	{/if}
</div>

<style>
	:global(.ig-actions:empty) {
		display: none;
	}
	:global(.input-group .ig-actions) {
		border-left: 0 !important;
	}
	:global(.input-group input[type='search']::-webkit-search-cancel-button),
	:global(.input-group input[type='search']::-webkit-search-decoration),
	:global(.input-group input[type='search']::-webkit-search-results-button),
	:global(.input-group input[type='search']::-webkit-search-results-decoration) {
		display: none;
	}
	:global(.input-group input[type='search']::-ms-clear) {
		display: none;
	}
	:global(.input-group .ig-input:focus-visible) {
		outline: none;
		box-shadow: none;
	}
</style>
