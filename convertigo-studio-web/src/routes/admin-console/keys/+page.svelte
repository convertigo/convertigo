<script lang="ts">
	import { writable } from 'svelte/store';
	import { keysCheck, categoryStore } from '$lib/admin-console/stores/keysStore';
	import { call, callXml } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import {
		localStorageStore,
		getModalStore,
	} from '@skeletonlabs/skeleton';
	import Card from '$lib/admin-console/admin-components/Card.svelte';
	import Icon from '@iconify/svelte';

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
		const modalOptions = {
			type: 'confirm',
			title: 'Please confirm',
			body: 'Are you sure you want to proceed ?',
			response: (confirmed) => {
				if (confirmed) {
					deleteKey(keyText);
					console.log('key deleted :', { keyText });
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
		await keysUpdate(newKey);
		newKey = '';
	}
</script>

<div class="p-10">
	<h1 class="mb-5">Keys</h1>

	<Card>
		<div class="flex items-center">
			<form on:submit|preventDefault={handleFormSubmit}>
				<input
					type="text"
					bind:value={newKey}
					class="text-black placeholder:text-surface-300"
					placeholder="Enter a new key"
				/>
				<button type="submit" class="btn variant-filled">Add Key</button>
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

									<button
										class="bg-red-700 px-4 py-1 ml-4 rounded-xl"
										on:click={() => openModal(key['@_text'])}
										><Icon icon="material-symbols-light:delete-outline" class="w-7 h-7" />
									</button>
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
