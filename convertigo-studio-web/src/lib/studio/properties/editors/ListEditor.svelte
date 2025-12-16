<script>
	import { createEventDispatcher } from 'svelte';

	const dispatch = createEventDispatcher();

	/** @type {{name?: string, value?: any, values?: any[], editable?: boolean}} */
	let { name = '', value = '', values = [], editable = false } = $props();

	function commit(next) {
		dispatch('valueChanged', {
			name,
			value: next
		});
	}
</script>

<div class="list-editor">
	{#if editable}
		<input
			class="list-editor__input input"
			type="text"
			autocomplete="off"
			aria-autocomplete="none"
			value={value ?? ''}
			onchange={(e) => commit(e.currentTarget.value)}
			list={`${name}-list`}
		/>
		<datalist id={`${name}-list`}>
			{#each values as v (String(v))}
				<option value={v}></option>
			{/each}
		</datalist>
	{:else}
		<select
			class="list-editor__select select"
			value={value ?? ''}
			onchange={(e) => commit(e.currentTarget.value)}
		>
			{#each values as v (String(v))}
				<option value={v}>{v}</option>
			{/each}
		</select>
	{/if}
</div>

<style>
	.list-editor {
		display: flex;
	}
	.list-editor__input,
	.list-editor__select {
		width: 100%;
		font-size: 12px;
	}
</style>
