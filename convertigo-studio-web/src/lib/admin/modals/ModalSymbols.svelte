<script>
	import { call } from '$lib/utils/service';
	import { FileDropzone, RadioGroup, RadioItem, getModalStore } from '@skeletonlabs/skeleton';
	import { globalSymbols } from '../stores/symbolsStore';
	import Card from '../components/Card.svelte';
	import ModalButtons from '../components/ModalButtons.svelte';
	import Ico from '$lib/utils/Ico.svelte';

	const modalStore = getModalStore();
	const { mode, row } = $modalStore[0].meta;
	const prefix = mode == 'secret' ? 'secret ' : '';
	const type = mode == 'secret' ? 'password' : 'text';
	/** @type {{parent: any}} */
	let { parent } = $props();
	let importAction = $state('');
	let importPriority = $state('priority-import');

	let binds = $state({
		symbolName: row?.['@_name'] ?? '',
		symbolValue: row?.['@_value'] ?? ''
	});

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
			<RadioGroup active="dark:bg-primary-800 bg-primary-500" class="font-normal">
				<RadioItem bind:group={importAction} name="action-import" value="clear-import"
					><p class="text-[12px]">Clear & import</p></RadioItem
				>
				<RadioItem bind:group={importAction} name="action-import" value=""
					><p class="text-[12px]">Merge symbols</p></RadioItem
				>
			</RadioGroup>
			{#if importAction == ''}
				<p class="mt-3 text-[11px] mb-3 font-normal">In case of name conflict :</p>
				<RadioGroup active="dark:bg-primary-800 bg-primary-500" class="font-normal">
					<RadioItem bind:group={importPriority} name="priority" value="priority-server "
						><p class="text-[12px]">Priority Server</p></RadioItem
					>
					<RadioItem bind:group={importPriority} name="priority" value="priority-import"
						><p class="text-[12px]">Priority ServerPriority import</p></RadioItem
					>
				</RadioGroup>
			{/if}
			<FileDropzone
				name="userfile"
				id="symbolUploadFile"
				accept=".properties"
				on:change={importSymbol}
				class="mt-5"
			>
				<svelte:fragment slot="message">
					<div class="flex flex-col items-center">
						<Ico icon="icon-park:application-one" class="w-10 h-10" />
						Upload your project or drag and drop
					</div>
				</svelte:fragment>
				<svelte:fragment slot="meta">.properties files</svelte:fragment>
			</FileDropzone>

			<ModalButtons showConfirmBtn={false} />

			<!-- <ButtonsContainer>
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
			</ButtonsContainer> -->
		</form>
	</Card>
{:else}
	<Card title={row ? `Edit ${prefix}symbol` : `Add a new ${prefix}symbol`}>
		<form onsubmit={addGlobalSymbol} class="flex flex-col p-5">
			{#if mode == 'secret'}
				<p class="mb-5">
					In secret mode, the value is stored ciphered and the key automatically ends with .secret
				</p>
			{/if}
			<div class="flex gap-10">
				<label class="border-common">
					<p class="label-name">Enter {prefix}symbol name</p>
					<div class="flex items-center">
						<input
							placeholder="{prefix}name"
							name="symbolName"
							class="input-common"
							bind:value={binds.symbolName}
						/>
						{#if mode == 'secret'}
							<span class="ml-2">.secret</span>
						{/if}
					</div>
				</label>
				<label class="border-common">
					<p class="label-name">Enter {prefix}symbol value</p>
					<textarea
						placeholder="{prefix}value"
						name="symbolValue"
						class="input-common"
						bind:value={binds.symbolValue}
						rows="1"
						cols="50"
					></textarea>
				</label>
			</div>
			<ModalButtons />
		</form>
	</Card>
{/if}

<style lang="postcss">
</style>
