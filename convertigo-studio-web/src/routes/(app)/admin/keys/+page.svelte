<script>
	import KeysService from '$lib/admin/Keys.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import ModalYesNo from '$lib/common/components/ModalYesNo.svelte';
	import Icon from '@iconify/svelte';

	let newKey = $state('');
	let modalDelete = $state();

	let { categories, nbValidKeys, firstStartDate, deleteKey, addKey, formatExpiration } =
		$derived(KeysService);
</script>

<Card title="Keys Management">
	{#snippet cornerOption()}
		<div class="flex items-center gap-4 justify-end">
			<form
				onsubmit={(e) => {
					e.preventDefault();
					if (newKey.trim()) {
						KeysService.addKey(newKey.trim());
						newKey = '';
					}
				}}
				class="flex items-center gap-2 w-1/2"
			>
				<input
					id="newKey"
					type="text"
					class="search-input"
					bind:value={newKey}
					placeholder="Enter a new key"
				/>
				<button type="submit" class="basic-button">
					<Icon icon="vaadin:key-o" />
					Add Key
				</button>
			</form>

			<ResponsiveButtons
				class="max-w-4xl"
				buttons={[
					{
						label: `Total Valid Keys: ${nbValidKeys}`,
						cls: 'gray-button'
					},
					{
						label: `First Start Date: ${new Date(parseInt(firstStartDate)).toDateString()}`,
						cls: 'gray-button'
					}
				]}
			/>
		</div>
	{/snippet}
</Card>

{#if categories?.length > 0}
	{#each categories as category}
		<Card title={category.name} class="mt-5">
			<TableAutoCard
				definition={[
					{ name: 'Key', key: 'text' },
					{ name: 'Value', key: 'value' },
					{ name: 'Expiration Date', key: 'expiration', custom: true },
					{ name: 'Expired', key: 'expired', custom: true },
					{ name: 'Actions', custom: true }
				]}
				data={category.keys.key}
			>
				{#snippet children({ row, def })}
					{#if def.name === 'Expiration Date'}
						<div
							class:bg-success-400-500={row.expiration === '0'}
							class:bg-tertiary-400-500={row.expiration !== '0'}
						>
							{formatExpiration(row.expiration)}
						</div>
					{:else if def.name === 'Expired'}
						<button class:green-button={!row.expired} class:yellow-button={row.expired}>
							{row.expired ? 'Yes' : 'No'}
						</button>
					{:else if def.name === 'Actions'}
						<ResponsiveButtons
							buttons={[
								{
									icon: 'mingcute:delete-line',
									label: 'Delete',
									cls: 'delete-button',
									onclick: async () => {
										if (
											await modalDelete.open({
												title: 'Delete Key',
												message: `Are you sure you want to delete key ${row.text}?`
											})
										) {
											deleteKey(row.text);
										}
									}
								}
							]}
						/>
					{:else}
						{row[def.key]}
					{/if}
				{/snippet}
			</TableAutoCard>
		</Card>
	{/each}
{:else}
	<p>No categories found.</p>
{/if}

<ModalYesNo bind:this={modalDelete} />

<style lang="postcss">
</style>
