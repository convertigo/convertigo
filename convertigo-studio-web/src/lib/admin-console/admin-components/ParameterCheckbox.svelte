<script>
	export let label = '';
	export let checked = false;
	export let onclick = () => {};
</script>

<label class="checkbox-label">
	<input type="checkbox" bind:checked on:click={onclick} />
	<p>{label}</p>
</label>

<style>
	.checkbox-label {
		@apply mt-5 flex items-center;
	}
	.checkbox-label p {
		@apply ml-5;
	}
</style>
