<script>
	export let headers = [];
	export let data = [];
	export let showHeaders = false;

	$: dataWithStyle = data.map((item) => ({
		...item,
		rowClass: item.Name === 'Engine State' && item.Value === 'Running' ? 'bg-green' : ''
	}));
</script>

<table class="table">
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
					<td class="font-normal">
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
