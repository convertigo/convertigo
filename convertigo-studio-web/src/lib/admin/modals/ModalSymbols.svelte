<script>
	import { call } from '$lib/utils/service';
	import { RadioGroup, RadioItem, getModalStore } from '@skeletonlabs/skeleton';
	import { globalSymbols } from '../stores/symbolsStore';
	import Card from '../components/Card.svelte';
	import ModalButtons from '../components/ModalButtons.svelte';
	import ButtonsContainer from '../components/ButtonsContainer.svelte';

	const modalStore = getModalStore();
	const { mode, row } = $modalStore[0].meta;
	const prefix = mode == 'secret' ? 'secret ' : '';
	const type = mode == 'secret' ? 'password' : 'text';
	export let parent;
	let importAction = '';
	let importPriority = 'priority-import';


	let binds = {
		symbolName: row?.['@_name'] ?? '',
		symbolValue: row?.['@_value'] ?? ''
	};
	console.log('row', binds);

	async function addGlobalSymbol(e) {
		const fd = new FormData(e.target);
		if (mode === 'secret') {
			fd.set('symbolName', fd.get('symbolName') + '.secret');
		}
		await call('global_symbols.Add', fd);
		modalStore.close();
		globalSymbols();
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

			<ButtonsContainer>
				<button class="mt-5 w-full cancel-button" on:click={() => modalStore.close()}>Cancel</button
				>
				<input
					type="file"
					name="userfile"
					id="symbolUploadFile"
					accept=".properties"
					class="hidden"
					on:change={importSymbol}
				/>
				<label for="symbolUploadFile" class="btnStyle confirm-button w-full">Import</label>
			</ButtonsContainer>
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
