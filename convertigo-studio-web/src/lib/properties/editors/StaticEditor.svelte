<script>
	export let name;
	export let value;

	if ($$restProps) {
	}
</script>

<input id={name + '-static'} class="input text-[11.5px]" type="text" disabled {value} />

<style>
	input[type='text'][disabled] {
		cursor: auto !important;
	}
</style>
