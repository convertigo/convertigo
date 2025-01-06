<script>
	import { Switch } from '@skeletonlabs/skeleton-svelte';
	/** @type {{name?: string, values?: any[], value: string, class?: string, disabled?: boolean, onchange?: any, children?: import('svelte').Snippet}}*/
	let {
		name = '',
		values = ['false', 'true'],
		value = $bindable(values[0]),
		class: classes = '',
		disabled = false,
		onchange,
		children
	} = $props();

	let checked = $state(value == values[1]);
	let last;
	$effect(() => {
		value;
		if (checked != last) {
			const changed = value != (checked ? values[1] : values[0]);
			value = checked ? values[1] : values[0];
			if (typeof last != 'undefined' && changed) {
				onchange?.({ target: { name, value } });
			}
			last = checked;
		} else {
			checked = value == values[1];
		}
	});
</script>

<Switch
	{classes}
	{name}
	{disabled}
	value={values[1]}
	controlClasses="min-w-10"
	thumbInactive="bg-white"
	thumbActive="bg-white"
	controlActive="preset-filled-success-500"
	controlInactive="preset-filled-warning-500"
	bind:checked
>
	<span class="block cursor-pointer break-words">{@render children?.()}</span>
	{#if !checked && Array.isArray(values)}
		<input type="hidden" {name} value={values[0]} />
	{/if}
</Switch>
