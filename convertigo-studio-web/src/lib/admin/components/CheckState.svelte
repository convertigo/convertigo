<script>
	import { Switch } from '@skeletonlabs/skeleton-svelte';

	/** @type {{name?: string, values?: any[], value: string, class?: string, disabled?: boolean, onchange?: any, children?: import('svelte').Snippet}}*/
	let {
		name = '',
		values = ['false', 'true'],
		value = $bindable(values[0]),
		class: classes = '',
		disabled = false,
		children
	} = $props();
</script>

<Switch
	{classes}
	{name}
	{disabled}
	value={values[1]}
	controlClasses="min-w-10"
	thumbInactive="bg-white"
	thumbActive="bg-white"
	controlActive="preset-filled-success-200-800"
	controlInactive="preset-filled-error-200-800"
	checked={value == values[1]}
	onCheckedChange={(e) => (value = e.checked ? values[1] : values[0])}
>
	<span class="block cursor-pointer break-words">{@render children?.()}</span>
	{#if value != values[1] && Array.isArray(values)}
		<input type="hidden" {name} value={values[0]} />
	{/if}
</Switch>
