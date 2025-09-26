<script module>
	let ids = 0;
</script>

<script>
	import Ico from '$lib/utils/Ico.svelte';

	const join = (...classes) => classes.filter(Boolean).join(' ');
	const baseGroup = 'input-group w-full grid-cols-[auto_1fr_auto] items-center';
	const baseLabel = 'px-3 text-surface-500';
	const baseInput = 'placeholder:text-surface-500 focus-visible:outline-none focus:ring-0';
	const baseActions = 'layout-x-none items-center gap-[1px]!';

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
	let inputId = $derived(id ?? name ?? `input-group-${ids++}`);
	const setValue = (next) => (value = next);
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
	<span class={join('ig-actions', baseActions, actionsClass)}>
		{@render actions?.({ value, setValue, disabled })}
		{#if rightIcon}
			<Ico icon={rightIcon} size={rightIconSize} class={rightIconClass} />
		{/if}
	</span>
</div>

<style>
	:global(.ig-actions:empty) {
		display: none;
	}
	:global(.input-group .ig-input:focus-visible) {
		outline: none;
		box-shadow: none;
	}
</style>
