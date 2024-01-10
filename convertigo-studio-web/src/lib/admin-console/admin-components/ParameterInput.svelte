<script>
	export let label = '';
	export let value = '';
	export let onblur = () => {};
</script>

<div class="input-group">
	<h2>{label}</h2>
	<input type="text" class="input" bind:value on:blur={onblur} />
</div>

<style>
	.input-group h2 {
		@apply text-[14px] mt-5;
	}
	.input {
		@apply text-black w-[60%] mt-2 p-1 text-[14px] text-[12px] bg-white;
	}
</style>
