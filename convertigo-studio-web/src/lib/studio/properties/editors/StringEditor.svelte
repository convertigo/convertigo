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
		class="input rounded-[4px] border-[0.5px] text-[11.5px] text-surface-300 dark:bg-surface-900"
		type="text"
		autocomplete="off"
		aria-autocomplete="none"
		id={name + '-input'}
		{value}
		onchange={onChange}
	/>
</div>
