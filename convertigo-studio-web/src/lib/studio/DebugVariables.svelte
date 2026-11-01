<script>
	/** @typedef {{name: string, value: string, children?: Variable[], limited?: boolean}} Variable */
	/** @type {{variables?: Variable[]}} */
	let { variables = [] } = $props();
</script>

{#snippet tree(items)}
	{#each items as variable, index (index)}
		{#if variable.children?.length}
			<details class="pl-2">
				<summary><span>{variable.name}</span> <code>{variable.value}</code></summary>
				{@render tree(variable.children)}
				{#if variable.limited}<small>Inspection limit reached</small>{/if}
			</details>
		{:else}
			<div class="layout-x-between-low pl-2">
				<span>{variable.name}</span><code class="truncate" title={variable.value}
					>{variable.value}</code
				>
				{#if variable.limited}<small>Inspection limit reached</small>{/if}
			</div>
		{/if}
	{/each}
{/snippet}

{@render tree(variables)}
