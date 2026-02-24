<script>
	import { getAdminPageDocHref } from '$lib/admin/AdminDocumentation.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import Keys from '$lib/admin/Keys.svelte';
	import InputGroup from '$lib/common/components/InputGroup.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import { formatDate } from '$lib/utils/time';
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
	const keysDocHref = getAdminPageDocHref('/admin/keys');

	const keyCellClass = ({ expired }, { name }) => {
		const tone = expired == 'true' ? 'text-error-900-100 font-semibold ' : '';
		if (name == 'Key') return tone + 'min-w-90 max-w-[64ch] break-all';
		if (name == 'Avalaible') return tone + 'w-42 whitespace-nowrap text-left';
		if (name == 'Expiration Date') return tone + 'w-44 whitespace-nowrap text-left';
		return tone;
	};
</script>

<Card title="Keys" docHref={keysDocHref}>
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
				<InputGroup
					type="search"
					name="key"
					icon="mdi:key-outline"
					placeholder="Enter a new key"
					size="40"
					autofocus
				/>
				<Button label="Add" class="button-primary sm:w-fit!" icon="mdi:key-outline" type="submit" />
			</fieldset>
		</form>
	{/snippet}
	<AutoPlaceholder {loading}>
		You have {nbValidKeys} valid key{nbValidKeys > 1 ? 's' : ''}{#if nbInvalidKeys > 0}
			and
			{nbInvalidKeys} invalid key{nbInvalidKeys > 1 ? 's' : ''}{/if}. {#if firstStartDate > 0}
			The first key was created the {formatDate(firstStartDate)}.{/if}
	</AutoPlaceholder>
	{#each categories as { keys: data, name, total, remaining, overflow }, iCat (`${name ?? 'category'}-${iCat}`)}
		{@const classCat = [
			['preset-filled-success-50-950', 'preset-filled-success-100-900'],
			['preset-filled-warning-50-950', 'preset-filled-warning-100-900'],
			['preset-filled-error-50-950', 'preset-filled-error-100-900']
		]}
		{@const ratioCat = total > 1 ? remaining / total : 1}
		{@const iClass = ratioCat < 0.5 ? 1 : ratioCat < 0.2 ? 2 : 0}
		<TableAutoCard
			class="mb-2"
			definition={[
				{ name: 'Key', custom: true, class: keyCellClass },
				{ name: 'Avalaible', key: 'value', class: keyCellClass },
				{ name: 'Expiration Date', key: 'expiration', class: keyCellClass },
				{ name: 'Actions', custom: true, class: 'w-14' }
			]}
			{data}
		>
			{#snippet thead({ definition: { length: colspan } })}
				<thead>
					<tr class="preset-filled-surface-100-900">
						<th {colspan}>
							<AutoPlaceholder {loading}>
								<div class="layout-x-wrap justify-between p-low">
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
						title="Delete key"
						class="button-ico-primary"
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
