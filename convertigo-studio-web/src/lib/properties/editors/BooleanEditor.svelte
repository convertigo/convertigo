<script>
	import { createEventDispatcher } from 'svelte';

	const dispatch = createEventDispatcher();

	export let name;
	export let value;

	let select;

	if ($$restProps) {
	}

	function onChange() {
		dispatch('valueChanged', {
			name: name,
			value: select.value
		});
	}
</script>

<select
	bind:this={select}
	class="select dark:bginput text-[11.5px] rounded-[4px] border-[0.5px] py-0"
	id={name + '-select'}
	{value}
	on:change={onChange}
>
	<option value="true">true</option>
	<option value="false">false</option>
</select>

<style lang="postcss">
	.bginput {
		@apply bg-surface-900;
	}
</style>
