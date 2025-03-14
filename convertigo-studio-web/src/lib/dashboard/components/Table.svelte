<script>
	/** @type {{columns?: any, data?: any}} */
	let { columns = [], data = [] } = $props();
</script>

<table class="min-w-full divide-y divide-surface-200 dark:divide-surface-600">
	<thead class="bg-blue-50 dark:bg-primary-900">
		<tr>
			{#each columns as column}
				<th
					class="text px-6 py-3 text-left text-xs font-medium tracking-wider text-gray-500 uppercase"
					>{column}</th
				>
			{/each}
		</tr>
	</thead>
	<tbody class="divide-y divide-surface-200 bg-white dark:divide-surface-700">
		{#each data as row}
			<tr>
				{#each row as cell, i}
					<td class="text px-6 py-4 text-sm whitespace-nowrap text-gray-900">
						{@html cell}
					</td>
				{/each}
			</tr>
		{/each}
	</tbody>
</table>
