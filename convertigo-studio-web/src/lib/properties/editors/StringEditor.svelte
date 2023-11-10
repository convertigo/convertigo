<script>
	import { createEventDispatcher } from 'svelte';

	const dispatch = createEventDispatcher();

	export let prop;

	let input;
	let name = prop.name;
	let value = prop.value;

	function onChange() {
		dispatch('valueChanged', {
			name: name,
			value: input.value
		});
	}
</script>

<input
	bind:this={input}
	class="input"
	type="text"
	id={name + '-input'}
	{value}
	on:change={onChange}
/>
