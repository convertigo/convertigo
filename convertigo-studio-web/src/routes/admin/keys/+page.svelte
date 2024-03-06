<script>
	import { keysCheck, categoryStore } from '$lib/admin/stores/keysStore';
	import { call } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import Card from '$lib/admin/components/Card.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';

	const keyModalStore = getModalStore();

	let newKey = '';

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
			if (response) {
				keysCheck();
			}
		} catch (error) {
			console.error(error);
		}
	}

	function openModal(keyText) {
		const confirmDeleted = {
			title: 'Key deleted with success'
		};
		const modalOptions = {
			type: 'confirm',
			title: 'Please confirm',
			body: 'Are you sure you want to proceed ?',
			response: (confirmed) => {
				if (confirmed) {
					deleteKey(keyText);
					console.log('key deleted :', { keyText });
					//@ts-ignore
					keyModalStore.trigger(confirmDeleted);
				}
			}
		};
		// @ts-ignore
		keyModalStore.trigger(modalOptions);
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
			if (resUpdate) {
				console.log(resUpdate);
				keysCheck();
			}
		} catch (err) {
			console.error(err);
		}
	}

	async function handleFormSubmit() {
		const modalSuccess = {
			title: 'key added with success'
		};
		try {
			await keysUpdate(newKey);
			newKey = '';

			//@ts-ignore
			keyModalStore.trigger(modalSuccess);
		} catch (err) {
			console.error(err);
		}
	}
</script>

<Card>
	<div class="">
		<form on:submit|preventDefault={handleFormSubmit} class="space-x-10">
			<input type="text" bind:value={newKey} class="input-new-key" placeholder="Enter a new key" />
			<button type="submit" class="bg-primary-400-500-token">Add Key</button>
		</form>
	</div>
</Card>

{#if $categoryStore.length > 0}
	{#each $categoryStore as category}
		<div class="mt-5">
			<Card title={category['@_name']}>
				<TableAutoCard
					definition={[
						{ name: 'Key', key: '@_text' },
						{ name: 'Expiration Date', key: '@_expiration', custom: true },
						{ name: 'Expired', key: '@_expired', custom: true },
						{ name: 'Delete', custom: true }
					]}
					data={category.keys}
					let:row
					let:def
				>
					{#if def.custom}
						{#if def.name === 'Expiration Date'}
							{#if row[def.key] === '0'}
								<div class="bg-success-400-500-token p-2 rounded-xl text-token">
									{formatExpiration(row[def.key])}
								</div>
							{:else}
								{formatExpiration(row[def.key])}
							{/if}
						{:else if def.name === 'Expired'}
							{#if row[def.key] === 'false'}
								<div class="bg-success-400-500-token p-2 rounded-xl text-token">{row[def.key]}</div>
							{:else}
								<div class="bg-red-400">{row[def.key]}</div>
							{/if}
						{:else if def.name === 'Delete'}
							<button
								class="shadow-md p-1 px-2 ring-outline-token bg-error-400-500-token"
								on:click={() => openModal(row['@_text'])}
							>
								<Ico icon="material-symbols-light:delete-outline" class="h-7 w-7 " />
							</button>
						{/if}
					{:else}
						<td>{row[def.key]}</td>
					{/if}
				</TableAutoCard>
			</Card>
		</div>
	{/each}
{:else}
	Loading...
{/if}

<style lang="postcss">
	.input-new-key {
		@apply min-w-80 dark:text-white text-token placeholder:text-surface-300 rounded-token w-[60%] dark:bg-surface-500 bg-white dark:border-surface-400 border-surface-200;
		max-width: 400px;
	}
</style>
