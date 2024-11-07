<script>
	import { preventDefault } from 'svelte/legacy';

	import { keysCheck, categoryStore } from '$lib/admin/stores/keysStore';
	import { call } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import Card from '$lib/admin/components/Card.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';
	import Icon from '@iconify/svelte';

	const keyModalStore = getModalStore();

	let newKey = $state('');

	onMount(() => {
		keysCheck();
	});

	function formatExpiration(expirationCode) {
		if (expirationCode === '0') {
			return 'Unlimited';
		} else {
			let year = parseInt(expirationCode.substring(0, 2), 10);
			let dayOfYear = parseInt(expirationCode.substring(2), 10);
			year += year < 70 ? 2000 : 1900;
			let date = new Date(year, 0);

			date.setDate(date.getDate() + dayOfYear - 1);
			return date.toDateString();
		}
	}

	/**
	 * @param {any} keyText
	 */
	async function deleteKey(keyText) {
		try {
			// @ts-ignore
			const response = await call('keys.Remove', {
				'@_xml': true,
				admin: {
					'@_service': 'keys.Remove',
					keys: {
						key: {
							'@_text': keyText
						}
					}
				}
			});
			keysCheck();
		} catch (error) {
			console.error(error);
		}
	}

	function openModalDeleteKey(keyText) {
		// @ts-ignore
		keyModalStore.trigger({
			type: 'component',
			component: 'modalWarning',
			title: 'Please Confirm',
			body: 'Are you sure you want to delete the key ?',
			meta: { mode: 'Confirm' },
			response: (confirmed) => {
				if (confirmed) {
					deleteKey(keyText);
				}
			}
		});
	}

	async function keysUpdate(keyText) {
		try {
			const resUpdate = await call('keys.Update', {
				'@_xml': true,
				admin: {
					'@_service': 'keys.Update',
					keys: {
						key: {
							'@_text': keyText
						}
					}
				}
			});
			const errorMessage = resUpdate?.admin?.keys?.key['@_errorMessage'];
			keysCheck();
		} catch (err) {
			console.error(err);
		}
	}

	async function handleFormSubmit() {
		try {
			const res = await keysUpdate(newKey);
			newKey = '';
		} catch (err) {
			console.error(err);
		}
	}
</script>

<Card title="Keys">
	<form onsubmit={preventDefault(handleFormSubmit)} class="space-x-0 flex gap-2 items-center">
		<input type="text" bind:value={newKey} class="input-new-key" placeholder="Enter a new key" />
		<button type="submit" class="basic-button">
			<Ico icon="vaadin:key-o" />
			<p>Add Key</p>
		</button>
	</form>
</Card>

{#if $categoryStore.length >= 0}
	{#each $categoryStore as category}
		<div class="mt-5">
			<Card title={category['@_name']}>
				<TableAutoCard
					definition={[
						{ name: 'Key', key: '@_text' },
						{
							name: 'In use',
							key: '@_total',
							custom: true
						},
						{
							name: 'Remaining',
							key: '@_remaining',
							custom: true
						},
						{
							name: 'Expiration Date',
							key: '@_expiration',
							custom: true
							/*class: (row) =>
								row['@_expiration'] === '0'
									? 'bg-success-400-500 border-r-[1px] border-surface-100-800'
									: 'bg-tertiary-400-500 border-r-[1px] border-surface-100-800'*/
						},
						{
							name: 'Expired',
							key: '@_expired',
							custom: true
							/* class: (row) =>
								row['@_expired'] === 'false'
									? 'bg-success-400-500'
									: 'bg-tertiary-400-500'*/
						},
						{ name: 'Delete', custom: true }
					]}
					data={category.keys}
				>
					{#snippet children(row, def)}
						{#if def.custom}
							{#if def.name === 'Expiration Date'}
								{#if row[def.key] === '0'}
									<div class="bg-success-400-500 rounded py-1 px-1 text">
										{formatExpiration(row[def.key])}
									</div>
								{:else}
									<div class="bg-tertiary-400-500 rounded py-1 px-1 text">
										{formatExpiration(row[def.key])}
									</div>
								{/if}
							{:else if def.name === 'In use'}
								<span class="">{category['@_total']}</span>
							{:else if def.name === 'Remaining'}
								<span class="">{category['@_remaining']}</span>
							{:else if def.name === 'Expired'}
								{#if row[def.key] === 'false'}
									<div class="bg-success-400-500 rounded py-1 px-1 text">
										{row[def.key]}
									</div>
								{:else}
									<div class="bg-red-400">{row[def.key]}</div>
								{/if}
							{:else if def.name === 'Delete'}
								<button class="delete-button" onclick={() => openModalDeleteKey(row['@_text'])}>
									<Icon icon="mingcute:delete-line" class="h-4 w-4 " />
								</button>
							{/if}
						{:else}
							<td>{row[def.key]}</td>
						{/if}
					{/snippet}
				</TableAutoCard>
			</Card>
		</div>
	{/each}
{/if}

<style lang="postcss">
	.input-new-key {
		@apply dark:text-white w-80 text placeholder:text-surface-300 rounded w-[60%] dark:bg-surface-500 bg-white dark:border-surface-400 border-surface-200;
		max-width: 400px;
	}
</style>
