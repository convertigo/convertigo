<script>
	import Button from './Button.svelte';

	/** @type {{ row: { value?: string, hasMaskedValue?: boolean, displayName: string, isDisabled?: boolean }, onChange: (value: string) => void }} */
	let { row, onChange } = $props();
</script>

<div class="layout-x-low w-full items-center">
	<input
		type="password"
		autocomplete="new-password"
		aria-label={row.displayName}
		placeholder={row.hasMaskedValue && row.value === undefined ? 'Value set' : 'Enter value …'}
		value={row.value ?? ''}
		disabled={row.isDisabled}
		oninput={(event) => onChange(event.currentTarget.value)}
		class="h-9 input-common min-w-0 flex-1 px-3 text-sm"
	/>
	{#if row.hasMaskedValue && row.value !== '' && !row.isDisabled}
		<Button
			label="Clear"
			ariaLabel="Clear {row.displayName}"
			class="button-secondary"
			full={false}
			onclick={() => onChange('')}
		/>
	{/if}
</div>
