<script>
	import { Switch } from '@skeletonlabs/skeleton-svelte';

	/** @type {{name?: string, values?: any[], value: string, class?: string, disabled?: boolean, onchange?: any, children?: import('svelte').Snippet}}*/
	let {
		name = '',
		values = ['false', 'true'],
		value = $bindable(values[0]),
		class: cls = '',
		disabled = false,
		children
	} = $props();
</script>

<Switch
	{name}
	{disabled}
	value={values[1]}
	class={`inline-flex items-center gap-low ${cls}`.trim()}
	checked={value == values[1]}
	onCheckedChange={(e) => (value = e.checked ? values[1] : values[0])}
>
	<Switch.Control
		class="flex h-6 w-11 items-center rounded-full bg-error-600/60 transition-colors duration-150 data-[state=checked]:bg-success-500"
	>
		<Switch.Thumb
			class="h-5 w-5 rounded-full bg-white shadow-sm transition-transform duration-150 data-[state=checked]:translate-x-5"
		/>
	</Switch.Control>
	<Switch.Label class="text-sm leading-tight font-medium text-current"
		>{@render children?.()}</Switch.Label
	>
	<Switch.HiddenInput />
	{#if value != values[1] && Array.isArray(values)}
		<input type="hidden" {name} value={values[0]} />
	{/if}
</Switch>
