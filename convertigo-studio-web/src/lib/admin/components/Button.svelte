<script>
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{ label?: string, icon?: string, size?: string, cls?: string, class?: string, disabled?: boolean, value?: string, hidden?: boolean, href?: string, type?: string } | any} */
	let {
		label,
		icon,
		size = 'btn',
		cls,
		class: _cls,
		disabled,
		value,
		hidden,
		href,
		type = 'button',
		...rest
	} = $props();
	cls ??= _cls;
</script>

{#if !hidden}
	{#if href && !disabled}
		<a {href} class="{cls} h-full min-h-fit w-full text-wrap" {...rest}>
			{#if icon}<span><Ico {icon} {size} /></span>{/if}{#if label}<span>{label}</span>{/if}</a
		>
	{:else}
		<button {disabled} class="{cls} h-full min-h-fit w-full text-wrap" {type} {value} {...rest}>
			{#if icon}<span><Ico {icon} {size} /></span>{/if}{#if label}<span>{label}</span>{/if}</button
		>
	{/if}
{/if}
