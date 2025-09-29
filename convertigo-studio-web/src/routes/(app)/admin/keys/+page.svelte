<script>
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import Keys from '$lib/admin/Keys.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
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

	const expired = ({ expired }, { name }) =>
		(expired == 'true' ? 'text-error-900-100 font-semibold ' : '') +
		(name == 'Key' ? 'w-120' : name == 'Avalaible' ? 'w-40' : 'text-right');
</script>

<Card title="Keys">
	{#snippet cornerOption()}
		<form
			onsubmit={async (/** @type {any} */ e) => {
				e.preventDefault();
				const res = await addKey(e.target.key.value);
				if (res?.isError) {
					return;
				}
				e.target.reset();
			}}
		>
			<fieldset class="layout-x max-sm:flex-wrap" disabled={calling || loading}>
				<PropertyType type="text" name="key" placeholder="Enter a new key" />
				<Button label="Add" class="button-primary sm:w-fit!" icon="mdi:key-outline" type="submit" />
			</fieldset>
		</form>
	{/snippet}
	<AutoPlaceholder {loading}>
		You have {nbValidKeys} valid key{nbValidKeys > 1 ? 's' : ''}{#if nbInvalidKeys > 0}
			{' and'} {nbInvalidKeys} invalid key{nbInvalidKeys > 1 ? 's' : ''}{/if}. {#if firstStartDate > 0}
			The first key was created the {new Date(firstStartDate).toISOString().split('T')[0]}.{/if}
	</AutoPlaceholder>
	{#each categories as { keys: data, name, total, remaining, overflow }, iCat}
		{@const classCat = [
			['preset-filled-success-50-950', 'preset-filled-success-100-900'],
			['preset-filled-warning-50-950', 'preset-filled-warning-100-900'],
			['preset-filled-error-50-950', 'preset-filled-error-100-900']
		]}
		{@const ratioCat = total > 1 ? remaining / total : 1}
		{@const iClass = ratioCat < 0.5 ? 1 : ratioCat < 0.2 ? 2 : 0}
		<TableAutoCard
			showHeaders={true}
			definition={[
				{ name: 'Actions', custom: true, class: 'w-14' },
				{ name: 'Key', custom: true, class: expired },
				{ name: 'Avalaible', key: 'value', class: expired },
				{ name: 'Expiration Date', key: 'expiration', class: expired }
			]}
			{data}
		>
			{#snippet thead({ definition: { length: colspan } })}
				<thead>
					<tr class="preset-filled-surface-100-900">
						<th {colspan}>
							<AutoPlaceholder {loading}>
								<div class="layout-x flex-wrap justify-between p-low">
									<div>
										{name}
										{#if overflow == 'true'}
											<span class="ml text-xs">(overflow)</span>
										{/if}
									</div>
									<div
										class="rounded-container px-4 py-1 {classCat[iClass][
											iCat % classCat[iClass].length
										]}"
									>
										used {total - remaining} / {total}<span class="ml text-xs"
											>({remaining} left)</span
										>
									</div>
								</div>
							</AutoPlaceholder>
						</th>
					</tr>
				</thead>
			{/snippet}
			{#snippet children({ row: { text, evaluation, expired }, def: { name } })}
				{#if name == 'Actions'}
					<Button
						size="6"
						icon="mdi:delete-outline"
						class="button-ico-error"
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
