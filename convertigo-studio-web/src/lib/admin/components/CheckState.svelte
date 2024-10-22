<script>
	import { SlideToggle } from '@skeletonlabs/skeleton';
	/** @type {{name: any, value?: any, checked?: boolean, size?: string, class?: string, children?: import('svelte').Snippet}} */
	let {
		name,
		value = ['false', 'true'],
		checked = $bindable(false),
		size = 'sm',
		class: cls = '',
		children
	} = $props();
</script>

<SlideToggle
	class={cls}
	{size}
	{name}
	value={Array.isArray(value) ? value[1] : value}
	active="min-w-12 bg-success-400 dark:bg-success-700"
	background="min-w-12 bg-error-400 dark:bg-error-700"
	bind:checked
	on:change
>
	<span class="block cursor-pointer break-words">{@render children?.()}</span>
	{#if !checked && Array.isArray(value)}
		<input type="hidden" {name} value={value[0]} />
	{/if}
</SlideToggle>
