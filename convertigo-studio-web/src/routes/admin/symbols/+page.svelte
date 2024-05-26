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
	import { Tab, TabGroup, getModalStore } from '@skeletonlabs/skeleton';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';

	let tabSet = 0;

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

	function openSymbolModal(mode, row) {
		symbolModalStore.trigger({
			type: 'component',
			component: 'modalSymbols',
			meta: { mode, row },
			title: row ? `Edit Symbol` : `New Symbol`
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

	<ButtonsContainer marginB="mb-10">
		<button class="bg-primary-400-500-token text-[12px]" on:click={() => openSymbolModal('add')}>
			<Icon icon="material-symbols-light:add" class="w-7 h-7" />
			Add Symbols</button
		>
		<button class="bg-primary-400-500-token" on:click={() => openSymbolModal('secret')}>
			<Icon icon="material-symbols-light:key-outline" class="w-7 h-7" />
			Add Secret Symbols
		</button>
		<button class="bg-primary-400-500-token" on:click={() => openSymbolModal('import')}
			><Icon icon="solar:import-line-duotone" class="w-7 h-7" />Import Symbols</button
		>
		<button class="bg-primary-400-500-token"
			><Icon icon="solar:export-line-duotone" class="w-7 h-7" />Export Symbols</button
		>
	</ButtonsContainer>

	<TabGroup>
		<Tab
			bind:group={tabSet}
			name="tab1"
			value={0}
			active="dark:bg-surface-500 bg-surface-200"
			class="font-bold">Symbols</Tab
		>
		<Tab
			bind:group={tabSet}
			name="tab2"
			value={1}
			active="dark:bg-surface-500 bg-surface-200"
			class="font-bold">Default symbols</Tab
		>
		<Tab
			bind:group={tabSet}
			name="tab3"
			value={2}
			active="dark:bg-surface-500 bg-surface-200"
			class="font-bold">Environment Variables</Tab
		>
		<svelte:fragment slot="panel">
			{#if tabSet === 0}
				<TableAutoCard
					comment="Global Symbols values can be fixed string, another Global Symbols or Environment Variables. If a
			symbol is defined for the Default value or if it contains a closing curly braces it must be
			escaped with a backslash."
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
							<button
								class="btn p-1 px-2 shadow-md bg-tertiary-400-500-token"
								on:click={() => openSymbolModal('edit', row)}
							>
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
			{:else if tabSet === 1}
				{#if $defaultSymbolList.length >= 0}
					<TableAutoCard
						comment="These symbols are defined in projects with default values. You can modify these values by adding them to the symbols list"
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
			{:else if tabSet === 2}
				<TableAutoCard
					comment="These environment variables can be used in Global Symbols values, using the following syntax: %variable_name[=default_value]%, default_value is optional."
					definition={[
						{ name: 'Name', key: 'name' },
						{ name: 'Value', key: 'value' }
					]}
					data={$environmentVariables}
				></TableAutoCard>
			{/if}
		</svelte:fragment>
	</TabGroup>
</Card>
