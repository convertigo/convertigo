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
	class={`items-start gap-low ${cls}`.trim()}
	checked={value == values[1]}
	onCheckedChange={(e) => (value = e.checked ? values[1] : values[0])}
>
	<Switch.Control class="preset-filled-error-200-800 motif-error min-w-12 rounded-full p-1 transition-colors duration-150 data-[state=checked]:preset-filled-success-200-800">
		<Switch.Thumb class="grid h-5 w-5 place-items-center rounded-full bg-white text-surface-900 transition-transform duration-150 data-[state=checked]:translate-x-5">
			<span class="text-xs font-medium">
				{value == values[1] ? 'ON' : 'OFF'}
			</span>
		</Switch.Thumb>
	</Switch.Control>
	<span class="block cursor-pointer break-words leading-tight">{@render children?.()}</span>
	<Switch.HiddenInput />
	{#if value != values[1] && Array.isArray(values)}
		<input type="hidden" {name} value={values[0]} />
	{/if}
</Switch>
