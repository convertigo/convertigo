<script>
	import { onMount } from 'svelte';
	import Card from '$lib/admin-console/admin-components/Card.svelte';
	import {
		getEnvironmentVar,
		environmentVariables,
		globalSymbols,
		globalSymbolsList
	} from '$lib/admin-console/stores/symbolsStore.js';
	import Icon from '@iconify/svelte';
	import { call } from '$lib/utils/service';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import ModalAddSymbol from '$lib/admin-console/modals/ModalAddSymbol.svelte';
	import ModalAddSecretSymbol from '$lib/admin-console/modals/ModalAddSecretSymbol.svelte';
	import ModalImportSymbol from '$lib/admin-console/modals/ModalImportSymbol.svelte';
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

	async function handlesubmti() {
		const response = await call('global_symbols.List');
		console.log(response);
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

	function openAddGlobalSymbolModal() {
		const ModalComponent = { ref: ModalAddSymbol };
		const modalSymbolAdd = {
			type: 'component',
			component: ModalComponent
		};
		//@ts-ignore
		symbolModalStore.trigger(modalSymbolAdd);
	}

	function openAddSecretSymbols() {
		const ModalComponentSecret = { ref: ModalAddSecretSymbol };
		const modalSecretSymbolAdd = {
			type: 'component',
			component: ModalComponentSecret
		};
		//@ts-ignore
		symbolModalStore.trigger(modalSecretSymbolAdd);
	}

	function openImportSymbols() {
		const ModalComponentImport = { ref: ModalImportSymbol };
		const modalImportSettings = {
			type: 'component',
			component: ModalComponentImport
		};
		//@ts-ignore
		symbolModalStore.trigger(modalImportSettings);
	}
</script>

<Card title="Global Symbols">
	<div class="flex gap-5 mb-10">
		<button class="btn variant-filled" on:click={openAddGlobalSymbolModal}>
			<Icon icon="material-symbols-light:add" class="w-7 h-7 mr-3" />
			Add symbols</button
		>
		<button class="btn variant-filled" on:click={openAddSecretSymbols}>
			<Icon icon="material-symbols-light:key-outline" class="w-7 h-7 mr-3" />
			Add secret symbols
		</button>

		<button class="btn variant-filled" on:click={openImportSymbols}
			><Icon icon="solar:import-line-duotone" class="w-7 h-7 mr-3" />import symbols</button
		>
		<button class="btn variant-filled"
			><Icon icon="solar:export-line-duotone" class="w-7 h-7 mr-3" />export symbols</button
		>
		<button class="btn variant-filled"
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
				<td>{globalSymbols['@_name']}</td>
				<td>{globalSymbols['@_value']}</td>
				<td><Icon icon="bitcoin-icons:edit-outline" class="w-7 h-7" /></td>

				<td>
					<button
						class="btn variant-filled px-4 py-1 rounded-xl"
						on:click={() => confirmSymbolDeletion(globalSymbols['@_name'])}
					>
						<Icon icon="material-symbols-light:delete-outline" class="w-4 h-4" />
					</button>
				</td>
			</tr>
		{/each}
	</Tables>
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
