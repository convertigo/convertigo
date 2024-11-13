<script>
	import { Switch } from '@skeletonlabs/skeleton-svelte';
	/** @type {{name: string, values?: string[], value: string, class?: string, children: import('svelte').Snippet}}*/
	let {
		name,
		values = ['false', 'true'],
		value = $bindable(values[0]),
		class: classes = '',
		children
	} = $props();

	let checked = $state(value == values[1]);
	$effect(() => {
		value = checked ? values[1] : values[0];
	});
	$effect(() => {
		checked = value == values[1];
	});
</script>

<Switch
	{classes}
	controlClasses="min-w-10"
	{name}
	value={Array.isArray(value) ? value[1] : value}
	bind:checked
>
	<span class="block cursor-pointer break-words">{@render children?.()}</span>
	{#if !checked && Array.isArray(values)}
		<input type="hidden" {name} value={value[0]} />
	{/if}
</Switch>
