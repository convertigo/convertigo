<script>
	import { call } from '$lib/utils/service';
	import { RadioGroup, RadioItem, getModalStore } from '@skeletonlabs/skeleton';
	import { globalSymbols } from '../stores/symbolsStore';

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
			const fd = new FormData(event.target);
			if (mode == 'secret') {
				fd.set('symbolName', fd.get('symbolName') + '.secret');
			}
			//@ts-ignore
			const response = await call('global_symbols.Add', fd);
			modalStore.close();
			globalSymbols();
			if (response) {
				modalStore.trigger({
					type: 'alert',
					title: 'Added with success'
				});
			} else {
				modalStore.trigger({
					type: 'alert',
					title: 'failed adding new symbol'
				});
			}
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
	<form class="p-10 rounded-xl glass flex flex-col">
		<h1 class="text-xl mb-5 text-center">Import global symbols</h1>
		<RadioGroup>
			<RadioItem bind:group={importAction} name="action-import" value="clear-import"
				>Clear & import</RadioItem
			>
			<RadioItem bind:group={importAction} name="action-import" value="">Merge symbols</RadioItem>
		</RadioGroup>
		{#if importAction == ''}
			<p class="mt-10 text-[14px] mb-5 text-center">In case of name conflict :</p>
			<RadioGroup>
				<RadioItem bind:group={importPriority} name="priority" value="priority-server"
					>Priority Server</RadioItem
				>
				<RadioItem bind:group={importPriority} name="priority" value="priority-import"
					>Priority import</RadioItem
				>
			</RadioGroup>
		{/if}
		<input
			type="file"
			name="userfile"
			id="symbolUploadFile"
			accept=".properties"
			class="hidden"
			on:change={importSymbol}
		/>
		<label for="symbolUploadFile" class="btn variant-filled mt-5">Import</label>
		<button class="mt-5 btn bg-white text-black font-light" on:click={() => modalStore.close()}
			>Cancel</button
		>
	</form>
{:else}
	<form on:submit={addGlobalSymbol} class="p-10 rounded-xl glass">
		<h1 class="text-2xl mb-5">Add a new {prefix}symbol</h1>
		{#if mode == 'secret'}
			<p class="mb-5">
				In secret mode, the value is stored ciphered and the key automatically ends with .secret
			</p>
		{/if}
		<div class="flex gap-5">
			<label>
				<p class="font-light text-[14px]">Enter {prefix}symbol name :</p>
				<input placeholder="{prefix}name" name="symbolName" class="text-black" />
				{#if mode == 'secret'}
					<span>.secret</span>
				{/if}
			</label>
			<label>
				<p class="font-light text-[14px]">Enter {prefix}symbol value :</p>
				<input placeholder="{prefix}value" {type} name="symbolValue" class="text-black" />
			</label>
		</div>
		<button type="submit" class="mt-5 btn bg-white text-black font-light">Confirm</button>
		<button class="mt-5 btn bg-white text-black font-light" on:click={() => modalStore.close()}
			>Cancel</button
		>
	</form>
{/if}

<style>
	.glass {
		background: rgba(255, 255, 255, 0.15);
		box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
		backdrop-filter: blur(2px);
		-webkit-backdrop-filter: blur(4px);
		border-radius: 10px;
		border: 2px solid rgba(138, 138, 138, 0.18);
	}

	input {
		margin: 10px 0;
		padding: 10px;
		background: rgba(255, 255, 255, 1);
		border: none;
		border-radius: 5px;
	}

	form {
		background-color: rgba(255, 255, 255, 1);
	}
</style>
