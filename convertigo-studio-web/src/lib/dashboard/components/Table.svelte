<script>
	/** @type {{columns?: any, data?: any}} */
	let { columns = [], data = [] } = $props();
</script>

<table class="min-w-full divide-y divide-surface-200 dark:divide-surface-600">
	<thead class="bg-surface-100-900">
		<tr>
			{#each columns as column (column)}
				<th
					class="text px-6 py-3 text-left text-xs font-medium tracking-wider text-surface-500 uppercase"
					>{column}</th
				>
			{/each}
		</tr>
	</thead>
	<tbody class="divide-y divide-surface-200 bg-surface-50-950 dark:divide-surface-600">
		{#each data as row, rowIndex (rowIndex)}
			<tr>
				{#each row as cell, i (i)}
					<td class="text px-6 py-4 text-sm whitespace-nowrap text-surface-900-100">
						{cell}
					</td>
				{/each}
			</tr>
		{/each}
	</tbody>
</table>
