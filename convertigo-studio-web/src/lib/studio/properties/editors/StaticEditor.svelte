<script>
	/** @type {{Record<string, any>}} */
	let { name, value, ...rest } = $props();

	if (rest) {
	}
</script>

<input
	id={name + '-static'}
	class="input dark:bg-surface-900 text-[11.5px] border-[0.5px] rounded-[4px]"
	type="text"
	disabled
	{value}
/>

<style lang="postcss">
	input[type='text'][disabled] {
		cursor: auto !important;
	}
</style>
