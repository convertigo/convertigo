<script>
	import { keysCheck, categoryStore } from '$lib/admin-console/stores/keysStore';
	import { callXml } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import { Table, getModalStore } from '@skeletonlabs/skeleton';
	import Card from '$lib/admin-console/admin-components/Card.svelte';
	import Icon from '@iconify/svelte';
	import Tables from '$lib/admin-console/admin-components/Tables.svelte';

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

	async function deleteKey(keyText) {
		const payload = `<?xml version="1.0" encoding="UTF-8"?>
<admin service="keys.Remove">
  <keys>
    <key text="${keyText}"/>
  </keys>
</admin>`;

		try {
			// @ts-ignore
			const response = await callXml('keys.Remove', payload);
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
		const payload = `<?xml version="1.0" encoding="UTF-8"?>
		<admin service="keys.Update">
			<keys>
				<key text ="${keyText}"/>
			</keys>
		</admin>`;

		try {
			const headers = {
				'Content-Type': 'text/xml',
				Accept: 'application/xml'
			};
			// @ts-ignore
			const resUpdate = await callXml('keys.Update', payload, headers);
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
	<div class="flex items-center">
		<form on:submit|preventDefault={handleFormSubmit}>
			<input
				type="text"
				bind:value={newKey}
				class="dark:text-black text-surface-800 placeholder:text-surface-200 rounded-xl w-[400px] dark:bg-surface-500 bg-white border-surface-200"
				placeholder="Enter a new key"
			/>
			<button type="submit" class="btn bg-buttons text-white ml-5">Add Key</button>
		</form>
	</div>
</Card>

{#if $categoryStore.length > 0}
	{#each $categoryStore as category}
		<div class="mt-5">
			<Card title={category['@_name']}>
				<Tables
					headers={['Key', 'Total', 'Expiration Date', 'Expired', 'Remaining', 'In use', 'Delete']}
				>
					{#each category.keys as key}
						<tr>
							<td>{key['@_text']}</td>
							<td>{key['@_value']}</td>
							{#if key['@_expiration'] === '0'}
								<td class="border bg-green-500 text-black"
									>{formatExpiration(key['@_expiration'])}</td
								>
							{:else}
								<td>{formatExpiration(key['@_expiration'])}</td>
							{/if}
							{#if key['@_expired'] === 'false'}
								<td class="bg-green-500 text-black">{key['@_expired']}</td>
							{:else}
								<td class="bg-red-400">{key['@_expired']}</td>
							{/if}
							<td>{category['@_remaining']}</td>
							<td>{category['@_remaining']}</td>

							<td>
								<button class="shadow-md p-1 px-2 btn" on:click={() => openModal(key['@_text'])}
									><Icon icon="material-symbols-light:delete-outline" class="h-7 w-7" />
								</button>
							</td>
						</tr>
					{/each}
				</Tables>
			</Card>
		</div>
	{/each}
{:else}
	Loading...
{/if}
