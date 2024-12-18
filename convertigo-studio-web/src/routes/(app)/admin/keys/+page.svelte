<script>
	import Keys from '$lib/admin/Keys.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import ModalYesNo from '$lib/common/components/ModalYesNo.svelte';
	import Icon from '@iconify/svelte';
	import Ico from '$lib/utils/Ico.svelte';

	let newKey = $state('');
	let modalDelete = $state();

	let { categories, nbValidKeys, firstStartDate, deleteKey, addKey, formatExpiration } =
		$derived(Keys);
</script>

<ModalYesNo bind:this={modalDelete} />
<div class="layout-y-stretch">
	<Card title="Keys Management">
		{#snippet cornerOption()}
			<div class="flex items-center gap-4 justify-end">
				<form
					onsubmit={(e) => {
						e.preventDefault();
						if (newKey.trim()) {
							addKey(newKey.trim());
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
	<div class="grid grid-cols-1 lg:grid-cols-2 gap">
		{#each categories as category}
			<Card title={category.name} class="">
				<TableAutoCard
					definition={[
						{ name: 'Key', key: 'text' },
						{ name: 'Value', key: 'value' },
						{ name: 'Expiration Date', key: 'expiration', custom: true },
						{ name: 'Expired', key: 'expired', custom: true },
						{ name: 'Delete', custom: true }
					]}
					data={category.keys}
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
						{:else if def.name === 'Delete'}
							<button
								class="delete-button"
								onclick={async () => {
									if (
										await modalDelete.open({
											title: 'Delete user',
											message: `Are you sure you want to delete ${row.text}?`
										})
									) {
										deleteKey(row.text);
									}
								}}
							>
								<Ico icon="mingcute:delete-line" />
							</button>
						{:else}
							{row[def.key]}
						{/if}
					{/snippet}
				</TableAutoCard>
			</Card>
		{/each}
	</div>
</div>
