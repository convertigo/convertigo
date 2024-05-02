<script>
	import { call } from '$lib/utils/service';
	import { RadioGroup, RadioItem, getModalStore } from '@skeletonlabs/skeleton';
	import { globalSymbols } from '../stores/symbolsStore';
	import Card from '../components/Card.svelte';

	const modalStore = getModalStore();
	const { mode } = $modalStore[0].meta;
	const prefix = mode == 'secret' ? 'secret ' : '';
	const type = mode == 'secret' ? 'password' : 'text';

	let importAction = '';
	let importPriority = 'priority-import';
	/**
	 * @param {SubmitEvent} event
	 */
	export async function addGlobalSymbol(event) {
		event.preventDefault();
		if (event.submitter?.textContent == 'Confirm') {
			//@ts-ignore
			const fd = new FormData(event.target);
			if (mode == 'secret') {
				fd.set('symbolName', fd.get('symbolName') + '.secret');
			}
			//@ts-ignore
			const response = await call('global_symbols.Add', fd);
			modalStore.close();
			globalSymbols();
		}
	}

	/**
	 * @param {Event} e
	 */
	async function importSymbol(e) {
		try {
			// @ts-ignore
			const response = await call('global_symbols.Import', new FormData(e.target.form));
			modalStore.close();
			globalSymbols();
			console.log(response);
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
	<Card title="Add a new {prefix}symbol">
		<form on:submit={addGlobalSymbol} class="flex flex-col p-5">
			{#if mode == 'secret'}
				<p class="mb-5">
					In secret mode, the value is stored ciphered and the key automatically ends with .secret
				</p>
			{/if}
			<div class="flex gap-5">
				<label class="border-common">
					<p class="label-name">Enter {prefix}symbol name</p>
					<input placeholder="{prefix}name" name="symbolName" class="input-common" />
					{#if mode == 'secret'}
						<span>.secret</span>
					{/if}
				</label>
				<label class="border-common">
					<p class="label-name">Enter {prefix}symbol value</p>
					<input placeholder="{prefix}value" {type} name="symbolValue" class="input-common" />
				</label>
			</div>

			<div class="flex flex-wrap gap-5">
				<div class="flex-1">
					<button class="mt-5 w-full cancel-button" on:click={() => modalStore.close()}
						>Cancel</button
					>
				</div>
				<div class="flex-1">
					<button type="submit" class="mt-5 w-full confirm-button">Confirm</button>
				</div>
			</div>
		</form>
	</Card>
{/if}

<style lang="postcss">
</style>
