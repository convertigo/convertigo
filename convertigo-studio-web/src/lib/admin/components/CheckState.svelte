<script>
	import { SlideToggle } from '@skeletonlabs/skeleton';
	export let name;
	export let value = ['false', 'true'];
	export let checked = false;
	export let size = 'sm';
	let cls = '';
	export { cls as class };
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
	<span class="block cursor-pointer break-words"><slot /></span>
	{#if !checked && Array.isArray(value)}
		<input type="hidden" {name} value={value[0]} />
	{/if}
</SlideToggle>
