<script>
	export let headers = [];
	export let data = [];
	export let showHeaders = false;
</script>

<div class="table-container">
<table class="table ">
	{#if showHeaders && headers.length > 0}
		<thead>
			<tr>
				{#each headers as header}
					<th class="">{header}</th>
				{/each}
			</tr>
		</thead>
	{/if}
	<tbody>
		{#each data as row}
			<tr>
				{#each headers as header}
					<td
						class="font-normal"
						class:bg-success-500={row.Name == 'Engine State' &&
							header == 'Value' &&
							row.Value == 'Running'}
						class:bg-error-500={row.Name == 'Engine State' &&
							header == 'Value' &&
							row.Value == 'Stopped'}
					>
						{#if row[header] != null}
							{row[header]}
						{:else}
							<div class="placeholder animate-pulse w-full min-w-32"></div>
						{/if}
					</td>
				{/each}
			</tr>
		{/each}
	</tbody>
</table>
</div>

<style lang="postcss">
	.table-container {
		overflow-x: auto;
		-webkit-overflow-scrolling: touch;
	}

	@media (max-width: 640px) {
		th,
		td {
			@apply text-sm;
		}
	}
</style>
