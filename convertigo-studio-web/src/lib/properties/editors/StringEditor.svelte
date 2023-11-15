<script>
	import { createEventDispatcher } from 'svelte';

	const dispatch = createEventDispatcher();

	export let name;
	export let value;

	let input;

	if ($$restProps) {
	}

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
	autocomplete="off"
	aria-autocomplete="none"
	id={name + '-input'}
	{value}
	on:change={onChange}
/>
