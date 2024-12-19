<script>
	import Keys from '$lib/admin/Keys.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import ModalYesNo from '$lib/common/components/ModalYesNo.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';

	let modalDelete = $state();

	let {
		categories,
		nbValidKeys,
		nbInvalidKeys,
		firstStartDate,
		deleteKey,
		addKey,
		loading,
		calling
	} = $derived(Keys);

	let data = $derived(
		categories.map((category) => [{ ...category, keys: undefined }, ...category.keys]).flat()
	);

	function expired(row) {
		return row.expired == 'true' ? 'text-error-900-100 font-semibold' : '';
	}
</script>

<ModalYesNo bind:this={modalDelete} />
<Card title="Keys">
	{#snippet cornerOption()}
		<form
			onsubmit={(/** @type {any} */ e) => {
				e.preventDefault();
				addKey(e.target.key.value);
				e.target.reset();
			}}
		>
			<fieldset class="layout-x max-sm:flex-wrap" disabled={calling || loading}>
				<PropertyType type="text" name="key" placeholder="Enter a new key" />
				<Button
					label="Add"
					class="basic-button sm:!w-fit"
					icon="material-symbols:key-outline-rounded"
				/>
			</fieldset>
		</form>
	{/snippet}
	<AutoPlaceholder {loading}>
		You have {nbValidKeys} valid key{nbValidKeys > 1 ? 's' : ''}{#if nbInvalidKeys > 0}
			{' and'} {nbInvalidKeys} invalid key{nbInvalidKeys > 1 ? 's' : ''}{/if}. The first key was
		created the {new Date(firstStartDate).toISOString().split('T')[0]}.
	</AutoPlaceholder>
	<TableAutoCard
		showHeaders={false}
		definition={[
			{ name: 'Actions', custom: true },
			{ name: 'Key', custom: true, class: expired },
			{ name: 'Avalaible', key: 'value', class: expired },
			{ name: 'Expiration Date', key: 'expiration', class: expired }
		]}
		{data}
	>
		{#snippet tr({ row, rowIdx, tr, definition })}
			{#if row.name}
				<tr>
					<td colspan={definition.length} class="preset-filled-secondary-200-800 font-bold text-lg">
						<AutoPlaceholder {loading}>
							<div class="layout-x justify-between flex-wrap">
								<div>
									{row.name}
									{#if row.overflow == 'true'}
										<span class="ml text-xs">(overflow)</span>
									{/if}
								</div>
								<div>
									used {row.total - row.remaining} / {row.total}<span class="ml text-xs"
										>({row.remaining} left)</span
									>
								</div>
							</div>
						</AutoPlaceholder>
					</td>
				</tr>
			{:else}
				{@render tr({ row, rowIdx })}
			{/if}
		{/snippet}
		{#snippet children({ row, def })}
			{#if def.name == 'Actions'}
				<Button
					class=""
					size="4"
					icon="mingcute:delete-line"
					cls="delete-button"
					onclick={async (event) => {
						if (
							await modalDelete.open({
								title: 'Delete Key',
								message: 'Are you sure you want to delete this key?',
								onYes: () => deleteKey(row.text)
							})
						) {
							deleteKey(row.text);
						}
					}}
				/>
			{:else if def.name == 'Key'}
				<AutoPlaceholder loading={row.text == null}
					>{row.text}
					{#if row.evaluation == 'true'}<span class="ml text-xs">(demo)</span
						>{/if}{#if row.expired == 'true'}<span class="ml text-xs">(expired)</span
						>{/if}</AutoPlaceholder
				>
			{/if}
		{/snippet}
	</TableAutoCard>
</Card>
