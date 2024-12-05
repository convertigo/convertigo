<script>
	import { Switch } from '@skeletonlabs/skeleton-svelte';
	/** @type {{name?: string, values?: any[], value: string, class?: string, onchange?: any, children?: import('svelte').Snippet}}*/
	let {
		name = '',
		values = ['false', 'true'],
		value = $bindable(values[0]),
		class: classes = '',
		onchange,
		children
	} = $props();

	let checked = $state(value == values[1]);
	let last;
	$effect(() => {
		value;
		if (checked != last) {
			value = checked ? values[1] : values[0];
			if (typeof last != 'undefined') {
				onchange?.({ target: { name, value } });
			}
			last = checked;
		} else {
			checked = value == values[1];
		}
	});
</script>

<Switch {classes} {name} value={values[1]} controlClasses="min-w-10" bind:checked>
	<span class="block cursor-pointer break-words">{@render children?.()}</span>
	{#if !checked && Array.isArray(values)}
		<input type="hidden" {name} value={values[0]} />
	{/if}
</Switch>
