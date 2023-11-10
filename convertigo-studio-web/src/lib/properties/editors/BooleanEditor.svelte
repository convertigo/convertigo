<script>
	import { createEventDispatcher } from 'svelte';

	const dispatch = createEventDispatcher();

	export let prop;

	let select;
	let name = prop.name;
	let value = prop.value;

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
