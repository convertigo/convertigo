<script>
	/** @type {{Record<string, any>}} */
	let { name, value, ...rest } = $props();

	if (rest) {
	}
</script>

<input
	id={name + '-static'}
	class="input rounded-[4px] border-[0.5px] text-[11.5px] dark:bg-surface-900"
	type="text"
	disabled
	{value}
/>

<style>
	input[type='text'][disabled] {
		cursor: auto !important;
	}
</style>
