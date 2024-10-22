<script>
	import { createEventDispatcher } from 'svelte';

	const dispatch = createEventDispatcher();

	/** @type {{Record<string, any>}} */
	let { name, value, ...rest } = $props();

	let input = $state();

	if (rest) {
	}

	function onChange() {
		dispatch('valueChanged', {
			name: name,
			value: input.value
		});
	}
</script>

<div class="text-[11.5px]">
	<input
		bind:this={input}
		class="input text-[11.5px] border-[0.5px] rounded-[4px] dark:bg-surface-900 text-surface-300"
		type="text"
		autocomplete="off"
		aria-autocomplete="none"
		id={name + '-input'}
		{value}
		onchange={onChange}
	/>
</div>
