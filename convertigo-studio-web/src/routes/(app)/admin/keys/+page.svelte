<script>
	import Keys from '$lib/admin/Keys.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import { getContext } from 'svelte';

	let modalYesNo = getContext('modalYesNo');

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

	function expired({ expired }) {
		return expired == 'true' ? 'text-error-900-100 font-semibold' : '';
	}
</script>

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
	{#each categories as { keys: data, name, total, remaining }}
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
			{#snippet tr({ row, rowIdx, tr, definition: { length: colspan } })}
				{#if rowIdx == 0}
					<tr class="border-surface-900-100 border-b-[0.5px]">
						<th {colspan}>
							<AutoPlaceholder {loading}>
								<div class="layout-x justify-between flex-wrap">
									<div>
										{name}
										{#if row.overflow == 'true'}
											<span class="ml text-xs">(overflow)</span>
										{/if}
									</div>
									<div>
										used {total - remaining} / {total}<span class="ml text-xs"
											>({remaining} left)</span
										>
									</div>
								</div>
							</AutoPlaceholder>
						</th>
					</tr>
					<tr></tr>
				{/if}
				{@render tr({ row, rowIdx })}
			{/snippet}
			{#snippet children({ row: { text, evaluation, expired }, def: { name } })}
				{#if name == 'Actions'}
					<Button
						size="4"
						icon="mingcute:delete-line"
						class="delete-button"
						onclick={async (event) => {
							if (
								await modalYesNo.open({
									event,
									title: 'Delete Key',
									message: 'Are you sure you want to delete this key?',
									onYes: () => deleteKey(text)
								})
							) {
								deleteKey(text);
							}
						}}
					/>
				{:else if name == 'Key'}
					<AutoPlaceholder loading={text == null}
						>{text}
						{#if evaluation == 'true'}<span class="ml text-xs">(demo)</span
							>{/if}{#if expired == 'true'}<span class="ml text-xs">(expired)</span
							>{/if}</AutoPlaceholder
					>
				{/if}
			{/snippet}
		</TableAutoCard>
	{/each}
</Card>
