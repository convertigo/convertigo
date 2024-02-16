<script>
	import { onMount } from 'svelte';
	import Card from '$lib/admin-console/admin-components/Card.svelte';
	import {
		getEnvironmentVar,
		environmentVariables,
		globalSymbols,
		globalSymbolsList,
		defaultSymbolList
	} from '$lib/admin-console/stores/symbolsStore.js';
	import Icon from '@iconify/svelte';
	import { call } from '$lib/utils/service';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import ModalAddSymbol from '$lib/admin-console/modals/ModalAddSymbol.svelte';
	import Tables from '$lib/admin-console/admin-components/Tables.svelte';

	onMount(() => {
		getEnvironmentVar();
		globalSymbols();
	});

	let customCard = 'margin-top: 20px';
	const symbolModalStore = getModalStore();

	async function symbolsDelete(symbolId) {
		const successModalSettings = {
			title: 'Delete Successful'
		};
		const failureModalSettings = {
			title: 'Delete Failed'
		};

		let formData = new FormData();
		formData.append('symbolName', symbolId);
		//globalSymbols();
		try {
			// @ts-ignore
			const response = await call('global_symbols.Delete', formData);
			globalSymbols();

			if (response.includes('success')) {
				// @ts-ignore
				symbolModalStore.trigger(successModalSettings);
			} else {
				// @ts-ignore
				symbolModalStore.trigger(failureModalSettings);
			}
		} catch (error) {
			console.error(error);
		}
	}

	async function deleteAllSymbols() {
		const res = await call('global_symbols.DeleteAll');
		console.log('delete all', res);
		globalSymbols();
	}

	async function addDefaultSymbol(defaultSymbol) {
		if (!defaultSymbol || !defaultSymbol['@_name'] || !defaultSymbol['@_value']) {
			console.error('symbole name or value is empty');

			return;
		}

		let fd = new FormData();
		fd.append('symbolName', defaultSymbol['@_name']);
		fd.append('symbolValue', defaultSymbol['@_value']);

		try {
			//@ts-ignore
			const response = await call('global_symbols.Add', fd);
			console.log(response);
			globalSymbols();
		} catch (err) {
			console.error(err);
		}
	}

	export function confirmSymbolDeletion(symbolId) {
		const modalSettings = {
			type: 'confirm',
			title: 'Please Confirm',
			body: 'Are you sure you wish to proceed ?',
			response: (confirmed) => {
				if (confirmed) {
					symbolsDelete(symbolId);
				}
			}
		};
		// @ts-ignore
		symbolModalStore.trigger(modalSettings);
	}

	function confirmDeleteAll() {
		const modalDeleteAll = {
			type: 'confirm',
			title: 'Please Confirm',
			body: 'Are you sure you wish to proceed ?',
			response: (confirmed) => {
				if (confirmed) {
					deleteAllSymbols();
				}
			}
		};
		// @ts-ignore
		symbolModalStore.trigger(modalDeleteAll);
	}

	function openAddGlobalSymbolModal() {
		symbolModalStore.trigger({
			type: 'component',
			component: { ref: ModalAddSymbol },
			meta: { mode: 'add' }
		});
	}

	function openAddSecretSymbols() {
		symbolModalStore.trigger({
			type: 'component',
			component: { ref: ModalAddSymbol },
			meta: { mode: 'secret' }
		});
	}

	function openImportSymbols() {
		symbolModalStore.trigger({
			type: 'component',
			component: { ref: ModalAddSymbol },
			meta: { mode: 'import' }
		});
	}
</script>

<Card title="Global Symbols">
	<div class="flex gap-5 mb-10">
		<button class="btn bg-buttons text-white" on:click={openAddGlobalSymbolModal}>
			<Icon icon="material-symbols-light:add" class="w-7 h-7 mr-3" />
			Add symbols</button
		>
		<button class="btn bg-buttons text-white" on:click={openAddSecretSymbols}>
			<Icon icon="material-symbols-light:key-outline" class="w-7 h-7 mr-3" />
			Add secret symbols
		</button>

		<button class="btn bg-buttons text-white" on:click={openImportSymbols}
			><Icon icon="solar:import-line-duotone" class="w-7 h-7 mr-3" />import symbols</button
		>
		<button class="btn bg-buttons text-white"
			><Icon icon="solar:export-line-duotone" class="w-7 h-7 mr-3" />export symbols</button
		>
		<button class="btn bg-buttons text-white" on:click={confirmDeleteAll}
			><Icon icon="material-symbols-light:delete-outline" class="w-7 h-7 mr-3" />Delete symbols</button
		>
	</div>

	<p class="dark:text-surface-100 text-surface-900 font-bold mb-10 w-[70%]">
		Global Symbols values can be fixed string, another Global Symbols or Environment Variables. If a
		symbol is defined for the Default value or if it contains a closing curly braces it must be
		escaped with a backslash.
	</p>

	<Tables headers={['Name', 'Value', 'Edit', 'Delete']}>
		{#each $globalSymbolsList as globalSymbols}
			<tr>
				<td class="align">{globalSymbols['@_name']}</td>
				<td class="align">{globalSymbols['@_value']}</td>
				<td class="align"><Icon icon="bitcoin-icons:edit-outline" class="w-7 h-7" /></td>

				<td class="align">
					<button
						class="btn p-1 px-2 shadow-md"
						on:click={() => confirmSymbolDeletion(globalSymbols['@_name'])}
					>
						<Icon icon="material-symbols-light:delete-outline" class="w-7 h-7" />
					</button>
				</td>
			</tr>
		{/each}
	</Tables>
	<p class="dark:text-surface-100 text-surface-700 font-bold mt-20 w-[70%]">
		List of global symbols with default value currently used. You can import them as regular symbol.
	</p>

	{#if $defaultSymbolList.length > 0}
		<Tables
			customStyle="margin-top: 20px; margin-bottom: 20px; "
			headers={['Project', 'Name', 'Value', 'Add']}
		>
			{#each $defaultSymbolList as defaultSymbol}
				<tr>
					<td class="align">{defaultSymbol['@_project']}</td>
					<td class="align">{defaultSymbol['@_name']}</td>
					<td class="align">{defaultSymbol['@_value']}</td>

					<td>
						<button class="btn p-1 px-2 shadow-md" on:click={() => addDefaultSymbol(defaultSymbol)}>
							<Icon icon="material-symbols-light:add" class="w-7 h-7" />
						</button>
					</td>
				</tr>
			{/each}
		</Tables>
	{:else}
		no data
	{/if}
</Card>

<Card customStyle={customCard} title="Environment Variables">
	<Tables headers={['Name', 'Value']}>
		{#each $environmentVariables as env}
			<tr>
				<td>{env['@_name']}</td>
				<td>{env['@_value']}</td>
			</tr>
		{/each}
	</Tables>
</Card>

<style lang="postcss">
</style>
