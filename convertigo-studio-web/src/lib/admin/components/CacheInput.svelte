<script module>
	let cpt = 0;
</script>

<script>
	/** @type {{Record<string, any>}} */
	let {
		label = '',
		id = `cache-input-${cpt++}`,
		type = 'text',
		placeholder = 'Enter value ..',
		value = $bindable(),
		...rest
	} = $props();
</script>

<div class="border-common mt-5">
	<label for={id} class="text-input">{label}</label>
	{#if type == 'password'}
		<input {id} type="password" {placeholder} class="input-common" bind:value {...rest} />
	{:else}
		<input {id} type="text" {placeholder} class="input-common" bind:value {...rest} />
	{/if}
</div>
