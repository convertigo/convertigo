<script lang="ts">
	import { writable } from 'svelte/store';
	import { keysCheck, categoryStore } from '$lib/admin-console/stores/keysStore';
	import { call, callXml } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import {
		localStorageStore,
		getModalStore,
		Modal,
		initializeStores
	} from '@skeletonlabs/skeleton';
	import Card from '$lib/admin-console/admin-components/Card.svelte';
	import AutoGrid from '$lib/admin-console/admin-components/AutoGrid.svelte';
	import Icon from '@iconify/svelte';

	initializeStores();

	const keyModalStore = getModalStore();

	let keys = writable('');
	let newKey = '';

	onMount(() => {
		keysCheck();
		changeTheme($theme);
		document.body.setAttribute('data-theme', 'dark-theme');
	});

	let theme = localStorageStore('studio.theme', 'skeleton');

	function changeTheme(e) {
		$theme = typeof e == 'string' ? e : e.target?.value;
		document.body.setAttribute('data-theme', $theme);
	}

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
		const successDeletingKeys = {
			title: 'Key deleted with success'
		};
		const faileDeletingKeys = {
			title: 'A problem occurred while deleting key'
		};

		const payload = `<?xml version="1.0" encoding="UTF-8"?>
<admin service="keys.Remove">
  <keys>
    <key text="${keyText}"/>
  </keys>
</admin>`;

		try {
			const headers = {
				'Content-Type': 'application/xml',
				Accept: 'application/xml'
			};
			const response = await callXml('keys.Remove', payload, headers);
			if (response) {
			}
			// @ts-ignore
			keyModalStore.trigger(successDeletingKeys);
		} catch (error) {
			console.error(error);
			// @ts-ignore
			keyModalStore.trigger(faileDeletingKeys);
		}
	}
</script>

<Modal class="text-center" />

<div class="p-10">
	<h1 class="mb-5">Keys</h1>

	<Card>
		<div class="flex items-center">
			<form on:submit|preventDefault>
				<input type="text" placeholder="Enter a new key" />
				<button type="submit">Add Key</button>
			</form>
		</div>
	</Card>

	{#if $categoryStore.length >= 0}
		{#each $categoryStore as category}
			<div class="mt-5">
				<Card>
					<h1 class="text-start mb-2">{category['@_name']}</h1>
					<table>
						<thead>
							<tr>
								<th class="px-4 py-2">key</th>
								<th class="px-4 py-2">Total</th>
								<th class="px-4 py-2">Expiration date</th>
								<th class="px-4 py-2">Expired</th>
								<th class="px-4 py-2">Remaining</th>
								<th class="px-4 py-2">In use</th>
								<th class="px-4 py-2">Delete</th>
							</tr>
						</thead>

						<tbody>
							{#each category.keys as key}
								<tr>
									<td class="border px-4 py-2">{key['@_text']}</td>
									<td class="border px-4 py-2">{key['@_value']}</td>
									{#if key['@_expiration'] === '0'}
										<td class="border px-4 py-2 bg-green-400 text-black"
											>{formatExpiration(key['@_expiration'])}</td
										>
									{:else}
										<td class="border px-4 py-2">{formatExpiration(key['@_expiration'])}</td>
									{/if}
									{#if key['@_expired'] === 'false'}
										<td class="border px-4 py-2 bg-green-500 text-black">{key['@_expired']}</td>
									{:else}
										<td class="border px-4 py-2 bg-red-400">{key['@_expired']}</td>
									{/if}
									<td class="border px-4 py-2">{category['@_remaining']}</td>
									<td class="border px-4 py-2">{category['@_remaining']}</td>
									<td class="border px-4 py-2"
										><button on:click={() => deleteKey(key['@_text'])}
											><Icon icon="material-symbols-light:delete-outline" class="w-7 h-7" /></button
										></td
									>
								</tr>
							{/each}
						</tbody>
					</table>
				</Card>
			</div>
		{/each}
	{:else}
		Loading
	{/if}
</div>

<style>
	th,
	td {
		border: 1px solid #616161;
		padding: 4px;
		text-align: left;
		font-weight: 300;
		font-size: 13px;
	}

	table {
		border-collapse: collapse;
	}
</style>
