<script>
	import { call } from '$lib/utils/service';
	import { RadioGroup, RadioItem, getModalStore } from '@skeletonlabs/skeleton';
	import { globalSymbols } from '../stores/symbolsStore';
	import Card from '../components/Card.svelte';
	import ModalButtons from '../components/ModalButtons.svelte';

	const modalStore = getModalStore();
	const { mode, row } = $modalStore[0].meta;
	const prefix = mode == 'secret' ? 'secret ' : '';
	const type = mode == 'secret' ? 'password' : 'text';
	export let parent;
	let importAction = '';
	let importPriority = 'priority-import';

	console.log('row', row);
	let binds = {
		symbolName: row?.['@_name'] ?? '',
		symbolValue: row?.['@_value'] ?? ''
	};

	/**
	 * @param {SubmitEvent} event
	 */

	async function addGlobalSymbol(event) {
		event.preventDefault();
		//@ts-ignore
		const fd = new FormData(event.target);

		try {
			if (row) {
				// No need to delete the existing symbol, just call Add service to update
				if (mode === 'secret') {
					fd.set('symbolName', fd.get('symbolName') + '.secret');
				}
				await call('global_symbols.Add', fd);
			} else {
				// Add new symbol
				if (mode === 'secret') {
					fd.set('symbolName', fd.get('symbolName') + '.secret');
				}
				await call('global_symbols.Add', fd);
			}
			modalStore.close();
			globalSymbols();
		} catch (err) {
			console.error(err);
		}
	}

	/**
	 * @param {Event} e
	 */
	async function importSymbol(e) {
		try {
			// @ts-ignore
			await call('global_symbols.Import', new FormData(e.target.form));
			modalStore.close();
			globalSymbols();
		} catch (err) {
			console.error(err);
		}
	}
</script>

{#if mode == 'import'}
	<Card title="Import global symbols">
		<form class="flex flex-col p-5">
			<RadioGroup active="bg-secondary-400-500-token">
				<RadioItem bind:group={importAction} name="action-import" value="clear-import"
					>Clear & import</RadioItem
				>
				<RadioItem bind:group={importAction} name="action-import" value="">Merge symbols</RadioItem>
			</RadioGroup>
			{#if importAction == ''}
				<p class="mt-10 text-[14px] mb-5 text-center">In case of name conflict :</p>
				<RadioGroup active="bg-secondary-400-500-token">
					<RadioItem bind:group={importPriority} name="priority" value="priority-server "
						>Priority Server</RadioItem
					>
					<RadioItem bind:group={importPriority} name="priority" value="priority-import"
						>Priority import</RadioItem
					>
				</RadioGroup>
			{/if}

			<div class="flex flex-wrap gap-5 mt-5">
				<div class="flex-1">
					<button class="mt-5 w-full cancel-button" on:click={() => modalStore.close()}
						>Cancel</button
					>
				</div>
				<div class="flex-1">
					<input
						type="file"
						name="userfile"
						id="symbolUploadFile"
						accept=".properties"
						class="hidden"
						on:change={importSymbol}
					/>
					<label for="symbolUploadFile" class="btn confirm-button mt-5 w-full">Import</label>
				</div>
			</div>
		</form>
	</Card>
{:else}
	<Card title={row ? `Edit ${prefix}symbol` : `Add a new ${prefix}symbol`}>
		<form on:submit={addGlobalSymbol} class="flex flex-col p-5">
			{#if mode == 'secret'}
				<p class="mb-5">
					In secret mode, the value is stored ciphered and the key automatically ends with .secret
				</p>
			{/if}
			<div class="flex gap-5">
				<label class="border-common">
					<p class="label-name">Enter {prefix}symbol name</p>
					<input
						placeholder="{prefix}name"
						name="symbolName"
						class="input-common"
						bind:value={binds.symbolName}
					/>
					{#if mode == 'secret'}
						<span>.secret</span>
					{/if}
				</label>
				<label class="border-common">
					<p class="label-name">Enter {prefix}symbol value</p>
					<input
						placeholder="{prefix}value"
						name="symbolValue"
						class="input-common"
						bind:value={binds.symbolValue}
					/>
				</label>
			</div>
			<ModalButtons />
		</form>
	</Card>
{/if}

<style lang="postcss">
</style>
