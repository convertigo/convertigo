<script>
	/** @type {{columns?: any, data?: any}} */
	let { columns = [], data = [] } = $props();
</script>

<table class="min-w-full divide-y divide-surface-200 dark:divide-surface-600">
	<thead class="bg-blue-50 dark:bg-primary-900">
		<tr>
			{#each columns as column}
				<th
					class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider text"
					>{column}</th
				>
			{/each}
		</tr>
	</thead>
	<tbody class="bg-white divide-y divide-surface-200 dark:divide-surface-700">
		{#each data as row}
			<tr>
				{#each row as cell, i}
					<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900 text">
						{@html cell}
					</td>
				{/each}
			</tr>
		{/each}
	</tbody>
</table>
