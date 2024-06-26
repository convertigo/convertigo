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
	import { Tab, TabGroup, getModalStore } from '@skeletonlabs/skeleton';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';
	import Ico from '$lib/utils/Ico.svelte';

	let tabSet = 0;
	let selectRow = false;
	let selectedUsers = new Set();
	let allSelected = false;

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
	<div slot="cornerOption">
		<div class="flex-1">
			<button class="delete-button" on:click={openConfirmDeleteAll}
				><Ico icon="mingcute:delete-line" />
				<p>Delete symbols</p>
			</button>
		</div>
	</div>

	<ButtonsContainer class="mb-10">
		{#each Object.entries(symbolsActions) as [type, { name, icon }]}
			<button class="basic-button" on:click={() => openSymbolModal(type)}>
				<p>{name}</p>
				<Ico {icon} />
			</button>
		{/each}

		<button class={selectRow ? 'delete-button' : 'basic-button'} on:click={DisplaySelectRow}>
			<p>{selectRow ? 'Cancel Export' : 'Export Symbols'}</p>
			<Ico icon={selectRow ? 'material-symbols-light:cancel-outline' : 'bytesize:export'} />
		</button>

		{#if selectRow}
			<button class="green-button" on:click={exportUserFile}>
				<p>Validate export</p>
				<Ico icon="bytesize:export" />
			</button>
			<button class="yellow-button" on:click={selectAllUsersFunction}>
				<p>Select all users</p>
				<Ico icon="bytesize:export" />
			</button>
		{/if}
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
						{ name: 'Export', custom: true },
						{ name: 'Name', key: '@_name' },
						{ name: 'Value', key: '@_value', class: 'truncate max-w-80' },
						{ name: 'Edit', custom: true },
						{ name: 'Delete', custom: true }
					].filter((elt) => selectRow || elt.name != 'Export')}
					data={$globalSymbolsList}
					let:row
					let:def
				>
					{#if def.custom}
						{#if def.name === 'Edit'}
							<button class="yellow-button" on:click={() => openSymbolModal('edit', row)}>
								<Ico icon="mdi:edit-outline" />
							</button>
						{:else if def.name === 'Delete'}
							<button class="delete-button" on:click={() => confirmSymbolDeletion(row['@_name'])}>
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
							<input type="checkbox" on:change={() => toggleUserSelection(row)} />
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
							<button class="green-button" on:click={() => addDefaultSymbol(row)}>
								<Ico icon="grommet-icons:add" />
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
