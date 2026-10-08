<script>
	import Button from '#lib/admin/components/Button.svelte';

	/** @type {{ values: string[], labelFor: (value: string) => string, disabled?: boolean, ariaLabel?: string, onMove: (index: number, offset: number) => void, onRemove?: (index: number) => void }} */
	let {
		values,
		labelFor,
		disabled = false,
		ariaLabel = 'Ordered values',
		onMove,
		onRemove
	} = $props();
</script>

<ol class="layout-y-low" aria-label={ariaLabel}>
	{#each values as value, index (value)}
		<li class="layout-x-between-low">
			<span>{index + 1}. {labelFor(value)}</span>
			<div class="layout-x-low">
				{#if onRemove}
					<Button
						icon="mdi:close"
						ariaLabel={`Remove ${labelFor(value)}`}
						cls="button-secondary"
						full={false}
						{disabled}
						onclick={() => onRemove?.(index)}
					/>
				{/if}
				<Button
					icon="mdi:arrow-up"
					ariaLabel={`Move ${labelFor(value)} up`}
					cls="button-secondary"
					full={false}
					disabled={disabled || index === 0}
					onclick={() => onMove(index, -1)}
				/>
				<Button
					icon="mdi:arrow-down"
					ariaLabel={`Move ${labelFor(value)} down`}
					cls="button-secondary"
					full={false}
					disabled={disabled || index === values.length - 1}
					onclick={() => onMove(index, 1)}
				/>
			</div>
		</li>
	{:else}
		<li>No assigned values.</li>
	{/each}
</ol>
