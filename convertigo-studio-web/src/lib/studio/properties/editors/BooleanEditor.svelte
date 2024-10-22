<script>
	import { createEventDispatcher } from 'svelte';

	const dispatch = createEventDispatcher();

	/** @type {{Record<string, any>}} */
	let { name, value, ...rest } = $props();

	let select = $state();

	if (rest) {
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
	onchange={onChange}
>
	<option value="true">true</option>
	<option value="false">false</option>
</select>
