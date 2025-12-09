<script>
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
		title = label,
		ariaLabel = title,
		full = true,
		...rest
	} = $props();
	let cls = $derived(_cls ?? _cls2 ?? '');
</script>

{#if !hidden}
	{#if href && !disabled}
		<a
			{href}
			class={[cls, 'h-full min-h-fit text-wrap', full && 'w-full']}
			{title}
			aria-label={ariaLabel}
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
			{title}
			aria-label={ariaLabel}
			{...rest}
		>
			{#if icon}<span><Ico {icon} {size} /></span>{/if}{#if label}<span>{label}</span>{/if}</button
		>
	{/if}
{/if}
