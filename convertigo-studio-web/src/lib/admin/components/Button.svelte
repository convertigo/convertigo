<script>
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{ label?: string, icon?: string, size?: string, cls?: string, class?: string, disabled?: boolean, value?: string, hidden?: boolean, href?: string } | any} */
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
		...rest
	} = $props();
	cls ??= _cls;
</script>

{#if !hidden}
	{#if href && !disabled}
		<a {href} class="{cls} text-wrap min-h-fit w-full h-full" {...rest}>
			{#if icon}<span><Ico {icon} {size} /></span>{/if}{#if label}<span>{label}</span>{/if}</a
		>
	{:else}
		<button {disabled} class="{cls} text-wrap min-h-fit w-full h-full" {value} {...rest}>
			{#if icon}<span><Ico {icon} {size} /></span>{/if}{#if label}<span>{label}</span>{/if}</button
		>
	{/if}
{/if}
