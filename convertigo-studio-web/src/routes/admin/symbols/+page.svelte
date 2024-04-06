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
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';

	onMount(() => {
		getEnvironmentVar();
		globalSymbols();
	});

	const symbolModalStore = getModalStore();

	async function symbolsDelete(symbolId) {
		let formData = new FormData();
		formData.append('symbolName', symbolId);
		//globalSymbols();
		try {
			// @ts-ignore
			const response = await call('global_symbols.Delete', formData);
			globalSymbols();
			console.log('symbols delete', response);
		} catch (error) {
			console.error(error);
		}
	}

	async function deleteAllSymbols() {
		const res = await call('global_symbols.DeleteAll');
		globalSymbols();
	}

	async function addDefaultSymbol(defaultSymbol) {
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
		symbolModalStore.trigger({
			type: 'component',
			component: 'modalWarning',
			title: 'Please Confirm',
			body: 'Are you sure you want to delete this Symbol ?',
			meta: { mode: 'Confirm' },
			response: (confirmed) => {
				if (confirmed) {
					symbolsDelete(symbolId);
				}
			}
		});
	}

	function openConfirmDeleteAll() {
		symbolModalStore.trigger({
			type: 'component',
			component: 'modalWarning',
			meta: { mode: 'Confirm' },
			title: 'Please Confirm',
			body: 'Are you sure you want to delete All Symbols ?',
			response: (confirmed) => {
				if (confirmed) {
					deleteAllSymbols();
				}
			}
		});
	}

	function openAddGlobalSymbolModal() {
		symbolModalStore.trigger({
			type: 'component',
			component: 'modalSymbols',
			meta: { mode: 'add' }
		});
	}

	function openAddSecretSymbols() {
		symbolModalStore.trigger({
			type: 'component',
			component: 'modalSymbols',
			meta: { mode: 'secret' }
		});
	}

	function openImportSymbols() {
		symbolModalStore.trigger({
			type: 'component',
			component: 'modalSymbols',
			meta: { mode: 'import' }
		});
	}
</script>

<Card title="Global Symbols">
	<div slot="cornerOption">
		<div class="flex-1">
			<button class="bg-error-400-500-token w-full" on:click={openConfirmDeleteAll}
				><Icon icon="material-symbols-light:delete-outline" class="w-7 h-7 mr-3" />Delete symbols</button
			>
		</div>
	</div>
	<div class="flex flex-wrap gap-5 mb-10 mt-10">
		<div class="flex-1">
			<button class="w-full bg-primary-400-500-token" on:click={openAddGlobalSymbolModal}>
				<Icon icon="material-symbols-light:add" class="w-7 h-7 mr-3" />
				Add Symbols</button
			>
		</div>
		<div class="flex-1">
			<button class="w-full bg-primary-400-500-token" on:click={openAddSecretSymbols}>
				<Icon icon="material-symbols-light:key-outline" class="w-7 h-7 mr-3" />
				Add Secret Symbols
			</button>
		</div>
		<div class="flex-1">
			<button class="w-full bg-primary-400-500-token" on:click={openImportSymbols}
				><Icon icon="solar:import-line-duotone" class="w-7 h-7 mr-3" />Import Symbols</button
			>
		</div>
		<div class="flex-1">
			<button class="w-full bg-primary-400-500-token"
				><Icon icon="solar:export-line-duotone" class="w-7 h-7 mr-3" />Export Symbols</button
			>
		</div>
	</div>

	<p class="dark:text-surface-100 text-surface-900 font-bold mb-5">
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
				<button class="btn p-1 px-2 shadow-md bg-tertiary-400-500-token">
					<Icon icon="bitcoin-icons:edit-outline" class="w-7 h-7" />
				</button>
			{:else if def.name === 'Delete'}
				<button
					class="btn p-1 px-2 shadow-md bg-error-400-500-token"
					on:click={() => confirmSymbolDeletion(row['@_name'])}
				>
					<Icon icon="material-symbols-light:delete-outline" class="w-7 h-7" />
				</button>
			{/if}
		{/if}
	</TableAutoCard>

	<p class="font-bold mt-20 mb-5">
		List of global symbols with default value currently used. You can import them as regular symbol.
	</p>

	{#if $defaultSymbolList.length >= 0}
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
				<button
					class="btn p-1 px-2 shadow-md bg-secondary-400-500-token"
					on:click={() => addDefaultSymbol(row)}
				>
					<Icon icon="material-symbols-light:add" class="w-7 h-7" />
				</button>
			{/if}
		</TableAutoCard>
	{:else}
		no data
	{/if}
</Card>

<Card class="mt-5" title="Environment Variables">
	<TableAutoCard
		definition={[
			{ name: 'Name', key: 'name' },
			{ name: 'Value', key: 'value' }
		]}
		data={$environmentVariables}
	></TableAutoCard>
</Card>
