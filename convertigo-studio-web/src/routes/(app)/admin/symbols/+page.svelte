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
	import { call } from '$lib/utils/service';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';

	let tabSet = $state(0);
	let selectRow = $state(false);
	let selectedUsers = new Set();
	let allSelected = false;

	onMount(() => {
		getEnvironmentVar();
		globalSymbols();
	});

	async function symbolsDelete(symbolId) {
		let formData = new FormData();
		formData.append('symbolName', symbolId);
		//globalSymbols();
		try {
			// @ts-ignore
			const response = await call('global_symbols.Delete', formData);
			globalSymbols();
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
		// symbolModalStore.trigger({
		// 	type: 'component',
		// 	component: 'modalWarning',
		// 	meta: { mode: 'Confirm' },
		// 	title: 'Please Confirm',
		// 	body: 'Are you sure you want to delete All Symbols ?',
		// 	response: (confirmed) => {
		// 		if (confirmed) {
		// 			deleteAllSymbols();
		// 		}
		// 	}
		// });
	}

	function openSymbolModal(mode, row) {
		// symbolModalStore.trigger({
		// 	type: 'component',
		// 	component: 'modalSymbols',
		// 	meta: { mode, row },
		// 	title: row ? `Edit Symbol` : `New Symbol`
		// });
	}

	const symbolsActions = {
		add: {
			name: 'Add Symbols',
			icon: 'grommet-icons:add'
		},
		secret: {
			name: 'Add secret symbol',
			icon: 'vaadin:key-o'
		},
		import: {
			name: 'Import Symbols',
			icon: 'bytesize:import'
		}
		// export: {
		// 	name: 'Export Symbols',
		// 	icon: 'bytesize:export'
		// }
	};

	function DisplaySelectRow() {
		selectRow = !selectRow;
	}

	function toggleUserSelection(user) {
		if (selectedUsers.has(user)) {
			selectedUsers.delete(user);
		} else {
			selectedUsers.add(user);
		}
	}

	function selectAllUsersFunction() {
		if (allSelected) {
			selectedUsers.clear();
			document
				.querySelectorAll('input[type="checkbox"]')
				//@ts-ignore
				.forEach((checkbox) => (checkbox.checked = false));
		} else {
			$globalSymbolsList.forEach((user) => selectedUsers.add(user));
			document
				.querySelectorAll('input[type="checkbox"]')
				//@ts-ignore
				.forEach((checkbox) => (checkbox.checked = true));
		}
		allSelected = !allSelected; // Toggle the selection state
	}

	function exportUserFile() {
		const usersArray = Array.from(selectedUsers);
		const jsonString = `data:text/json;chatset=utf-8,${encodeURIComponent(
			JSON.stringify(usersArray)
		)}`;
		const link = document.createElement('a');
		link.href = jsonString;
		link.download = 'symbols_export.json';
		link.click();
	}
</script>

<Card title="Global Symbols">
	{#snippet cornerOption()}
		<ResponsiveButtons
			class="max-w-4xl"
			buttons={[
				{
					label: 'Add Symbols',
					icon: 'grommet-icons:add',
					cls: 'basic-button',
					onclick: () => openSymbolModal('add', {})
				},
				{
					label: 'Add secret symbol',
					icon: 'vaadin:key-o',
					cls: 'basic-button',
					onclick: () => open
				},
				{
					label: 'Import Symbols',
					icon: 'bytesize:import',
					cls: 'basic-button',
					onclick: () => openSymbolModal('import', {})
				},
				{
					label: 'Export Symbols',
					icon: 'bytesize:export',
					cls: 'basic-button',
					onclick: () => openSymbolModal('export', {})
				},
				{
					label: 'Delete All Symbols',
					icon: 'mingcute:delete-line',
					cls: 'delete-button',
					onclick: openConfirmDeleteAll
				}
			]}
		/>
	{/snippet}
	<!-- <TabGroup>
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
		<svelte:fragment slot="panel"> -->
	{#if tabSet === 0}
		<TableAutoCard
			comment="Global Symbols values can be fixed string, another Global Symbols or Environment Variables. If a
			symbol is defined for the Default value or if it contains a closing curly braces it must be
			escaped with a backslash."
			definition={[
				{ name: 'Export', custom: true },
				{ name: 'Name', key: '@_name' },
				{ name: 'Value', key: '@_value', class: 'truncate max-w-80' },
				{ name: 'Edit', custom: true },
				{ name: 'Delete', custom: true }
			].filter((elt) => selectRow || elt.name != 'Export')}
			data={$globalSymbolsList}
		>
			{#snippet children({ row, def })}
				{#if def.custom}
					{#if def.name === 'Edit'}
						<button class="yellow-button" onclick={() => openSymbolModal('edit', row)}>
							<Ico icon="mdi:edit-outline" />
						</button>
					{:else if def.name === 'Delete'}
						<button class="delete-button" onclick={() => confirmSymbolDeletion(row['@_name'])}>
							<Ico icon="mingcute:delete-line" />
						</button>
					{:else if def.name === 'Export'}
						<!-- <SlideToggle
								active="min-w-12 bg-success-400 dark:bg-success-700"
								background="min-w-12 bg-error-400 dark:bg-error-700"
								name="slide"
								bind:checked={value}
								size="sm"
							/> -->
						<input type="checkbox" onchange={() => toggleUserSelection(row)} />
					{/if}
				{/if}
			{/snippet}
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
			>
				{#snippet children({ row, def })}
					{#if def.name === 'Add'}
						<button class="green-button" onclick={() => addDefaultSymbol(row)}>
							<Ico icon="grommet-icons:add" />
						</button>
					{/if}
				{/snippet}
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
	<!-- </svelte:fragment>
	</TabGroup> -->
</Card>
