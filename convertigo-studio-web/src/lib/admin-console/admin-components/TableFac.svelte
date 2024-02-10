<script>
	import Icon from '@iconify/svelte';
	export let headers = [];
	export let rows = [];
	export let formatExpiration;
	export let onDelete;
</script>

<table>
	<thead>
		<tr>
			{#each headers as header}
				<th>{header}</th>
			{/each}
			<th>Actions</th>
		</tr>
	</thead>
	<tbody>
		{#each rows as row}
			<tr>
				<td>{row.text}</td>
				<td>{row.value}</td>
				<td>
					{#if row.expiration === '0'}
						<span class="bg-green-400 text-black">{formatExpiration(row.expiration)}</span>
					{:else}
						{formatExpiration(row.expiration)}
					{/if}
				</td>
				<td>
					{#if row.expired === 'false'}
						<span class="bg-green-500 text-black">No</span>
					{:else}
						<span class="bg-red-400">Yes</span>
					{/if}
				</td>
				<td>{row.remaining}</td>
				<td>{row.inUse}</td>
				<td>
					<button class="bg-red-700 px-4 py-1 ml-4 rounded-xl" on:click={() => onDelete(row.text)}>
						<Icon icon="material-symbols-light:delete-outline" class="w-7 h-7" />
					</button>
				</td>
			</tr>
		{/each}
	</tbody>
</table>
