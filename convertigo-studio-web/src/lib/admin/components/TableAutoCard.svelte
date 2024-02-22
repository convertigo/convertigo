<script>
	export let definition;
	export let data;
	let cls = '';
	export { cls as class };
</script>

<table class="rounded-xl table ${cls}">
	<thead class="rounded-xl">
		<tr>
			{#each definition as def}
				<th class="header">{def.name}</th>
			{/each}
		</tr>
	</thead>
	<tbody>
		{#each data as row}
			<tr>
				{#each definition as def}
					<td data-label={def.name}>
						{#if def.custom}
							<slot {row} {def}>{row[def.key] ?? ''}</slot>
						{:else}
							{row[def.key] ?? ''}
						{/if}
					</td>
				{/each}
			</tr>
		{/each}
	</tbody>
</table>

<style lang="postcss">
	th {
		@apply dark:bg-surface-800 bg-white border-b-[1px] dark:border-primary-100 border-primary-100 dark:text-surface-100 text-surface-800 font-normal;
	}
</style>
