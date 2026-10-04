<script>
	import Button from '$lib/admin/components/Button.svelte';
	import StudioOrderedList from './StudioOrderedList.svelte';

	/** @type {{ label: string, values: string[], choices: string[], disabled?: boolean, onChange: (values: string[]) => void }} */
	let { label, values, choices, disabled = false, onChange } = $props();
	let selected = $state('');
	let available = $derived(choices.filter((value) => !values.includes(value)));
	let missing = $derived(values.filter((value) => !choices.includes(value)));
	function add() {
		if (!disabled && available.includes(selected)) onChange([...values, selected]);
		selected = '';
	}
	/** @param {number} index @param {number} offset */
	function move(index, offset) {
		if (disabled || index + offset < 0 || index + offset >= values.length) return;
		const next = [...values];
		[next[index], next[index + offset]] = [next[index + offset], next[index]];
		onChange(next);
	}
</script>

<fieldset class="layout-y-low" {disabled}>
	<legend>{label}</legend>
	<div class="layout-x-low">
		<label class="layout-y-low grow">
			Choose a reference
			<select class="select" bind:value={selected} disabled={disabled || !available.length}>
				<option value="">Choose…</option>
				{#each available as value (value)}<option {value}>{value}</option>{/each}
			</select>
		</label>
		<Button
			label="Add"
			cls="button-secondary"
			full={false}
			disabled={disabled || !available.includes(selected)}
			onclick={add}
		/>
	</div>
	<StudioOrderedList
		{values}
		labelFor={(value) => value}
		ariaLabel={label}
		{disabled}
		onMove={move}
		onRemove={(index) => onChange(values.filter((_, position) => position !== index))}
	/>
	{#if missing.length}
		<p role="alert">
			Unavailable references: {missing.join(', ')}. Remove them or restore their definitions.
		</p>
	{/if}
</fieldset>
