<script>
	import { onMount } from 'svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import {
		getEnvironmentVar,
		environmentVariables,
		globalSymbols,
		globalSymbolsList,
		defaultSymbolList
	} from '$lib/admin/stores/symbolsStore.js';
	import Icon from '@iconify/svelte';
	import { call } from '$lib/utils/service';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import ModalSymbols from '$lib/admin/modals/ModalSymbols.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';

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
			component: { ref: ModalSymbols },
			meta: { mode: 'add' }
		});
	}

	function openAddSecretSymbols() {
		symbolModalStore.trigger({
			type: 'component',
			component: { ref: ModalSymbols },
			meta: { mode: 'secret' }
		});
	}

	function openImportSymbols() {
		symbolModalStore.trigger({
			type: 'component',
			component: { ref: ModalSymbols },
			meta: { mode: 'import' }
		});
	}
</script>

<Card title="Global Symbols">
	<div class="flex flex-wrap gap-5 mb-10">
		<div class="flex-1">
			<button class="w-full" on:click={openAddGlobalSymbolModal}>
				<Icon icon="material-symbols-light:add" class="w-7 h-7 mr-3" />
				Add Symbols</button
			>
		</div>
		<div class="flex-1">
			<button class="w-full" on:click={openAddSecretSymbols}>
				<Icon icon="material-symbols-light:key-outline" class="w-7 h-7 mr-3" />
				Add Secret Symbols
			</button>
		</div>
		<div class="flex-1">
			<button class="w-full" on:click={openImportSymbols}
				><Icon icon="solar:import-line-duotone" class="w-7 h-7 mr-3" />Import Symbols</button
			>
		</div>
		<div class="flex-1">
			<button class="w-full"
				><Icon icon="solar:export-line-duotone" class="w-7 h-7 mr-3" />Export Symbols</button
			>
		</div>
		<div class="flex-1">
			<button class="w-full" on:click={confirmDeleteAll}
				><Icon icon="material-symbols-light:delete-outline" class="w-7 h-7 mr-3" />Delete symbols</button
			>
		</div>
	</div>

	<p class="dark:text-surface-100 text-surface-900 font-bold mb-5 w-[70%]">
		Global Symbols values can be fixed string, another Global Symbols or Environment Variables. If a
		symbol is defined for the Default value or if it contains a closing curly braces it must be
		escaped with a backslash.
	</p>

	<TableAutoCard
		definition={[
			{ name: 'Name', key: '@_name' },
			{ name: 'Value', key: '@_value' },
			{ name: 'Edit', custom: true },
			{ name: 'Delete', custom: true }
		]}
		data={$globalSymbolsList}
		let:row
		let:def
	>
		{#if def.custom}
			{#if def.name === 'Edit'}
				<button class="btn p-1 px-2 shadow-md">
					<Icon icon="bitcoin-icons:edit-outline" class="w-7 h-7" />
				</button>
			{:else if def.name === 'Delete'}
				<button
					class="btn p-1 px-2 shadow-md"
					on:click={() => confirmSymbolDeletion(globalSymbols['@_name'])}
				>
					<Icon icon="material-symbols-light:delete-outline" class="w-7 h-7" />
				</button>
			{/if}
		{/if}
	</TableAutoCard>

	<p class="dark:text-surface-100 text-surface-700 font-bold mt-20 mb-5 w-[70%]">
		List of global symbols with default value currently used. You can import them as regular symbol.
	</p>

	{#if $defaultSymbolList.length > 0}
		<TableAutoCard
			definition={[
				{ name: 'Project', key: '@_project' },
				{ name: 'Name', key: '@_name' },
				{ name: 'Value', key: '@_value' },
				{ name: 'Add', custom: true, key: 'add' }
			]}
			data={$defaultSymbolList}
			let:row
			let:def
		>
			{#if def.name === 'Add'}
				<button class="btn p-1 px-2 shadow-md" on:click={() => addDefaultSymbol(row.defaultSymbol)}>
					<Icon icon="material-symbols-light:add" class="w-7 h-7" />
				</button>
			{/if}
		</TableAutoCard>
	{:else}
		no data
	{/if}
</Card>

<Card customStyle={customCard} title="Environment Variables">
	<TableAutoCard
		definition={[
			{ name: 'Name', key: '@_name' },
			{ name: 'Value', key: '@_value' }
		]}
		data={$environmentVariables}
	></TableAutoCard>
</Card>
