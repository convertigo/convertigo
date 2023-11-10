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

<select bind:this={select} class="select" id={name + '-select'} {value} on:change={onChange}>
	<option value="true">true</option>
	<option value="false">false</option>
</select>
