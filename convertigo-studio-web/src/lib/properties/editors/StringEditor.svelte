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

<div class="text-[11.5px]">
	<input
		bind:this={input}
		class="input text-[11.5px] border-[0.5px] rounded-[4px] dark:bginput text-surface-300"
		type="text"
		autocomplete="off"
		aria-autocomplete="none"
		id={name + '-input'}
		{value}
		on:change={onChange}
	/>
</div>

<style lang="postcss">
	.bginput {
		@apply bg-surface-900;
	}
</style>
