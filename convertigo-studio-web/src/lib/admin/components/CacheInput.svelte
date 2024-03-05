<script context="module">
	let cpt = 0;
</script>

<script>
	export let label = '';
	export let id = `cache-input-${cpt++}`;
	export let type = 'text';
	export let placeholder = 'Enter value ..';
	export let value;
</script>

<div class="border-common mt-5">
	<label for={id} class="text-input">{label}</label>
	{#if type == 'password'}
		<input {id} type="password" {placeholder} class="input-common" bind:value {...$$restProps} />
	{:else}
		<input {id} type="text" {placeholder} class="input-common" bind:value {...$$restProps} />
	{/if}
</div>
