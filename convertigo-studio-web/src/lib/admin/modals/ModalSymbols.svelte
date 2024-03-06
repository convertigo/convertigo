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
	<Card>
		<form class="flex flex-col p-5">
			<h1 class="text-xl mb-5 text-center text-token">Import global symbols</h1>
			<RadioGroup>
				<RadioItem
					bind:group={importAction}
					name="action-import"
					value="clear-import"
					active="variant-filled-surface text-white">Clear & import</RadioItem
				>
				<RadioItem
					bind:group={importAction}
					name="action-import"
					value=""
					active="variant-filled-surface text-white">Merge symbols</RadioItem
				>
			</RadioGroup>
			{#if importAction == ''}
				<p class="mt-10 text-[14px] mb-5 text-center">In case of name conflict :</p>
				<RadioGroup>
					<RadioItem
						bind:group={importPriority}
						name="priority"
						value="priority-server "
						active="variant-filled-surface text-white">Priority Server</RadioItem
					>
					<RadioItem
						bind:group={importPriority}
						name="priority"
						value="priority-import"
						active="variant-filled-surface text-white">Priority import</RadioItem
					>
				</RadioGroup>
			{/if}

			<div class="flex flex-wrap gap-5">
				<div class="flex-1">
					<input
						type="file"
						name="userfile"
						id="symbolUploadFile"
						accept=".properties"
						class="hidden"
						on:change={importSymbol}
					/>
					<label for="symbolUploadFile" class="btn bg-primary-400-500-token mt-5 w-full">Import</label>
				</div>
				<div class="flex-1">
					<button class="mt-5 bg-error-400-500-token w-full" on:click={() => modalStore.close()}
						>Cancel</button
					>
				</div>
			</div>
		</form>
	</Card>
{:else}
	<Card>
		<form on:submit={addGlobalSymbol} class="flex flex-col p-5">
			<h1 class="text-2xl mb-5">Add a new {prefix}symbol</h1>
			{#if mode == 'secret'}
				<p class="mb-5">
					In secret mode, the value is stored ciphered and the key automatically ends with .secret
				</p>
			{/if}
			<div class="flex gap-5">
				<label class="border-common">
					<p class="input-name">Enter {prefix}symbol name</p>
					<input placeholder="{prefix}name" name="symbolName" class="input-common" />
					{#if mode == 'secret'}
						<span>.secret</span>
					{/if}
				</label>
				<label class="border-common">
					<p class="input-name">Enter {prefix}symbol value</p>
					<input placeholder="{prefix}value" {type} name="symbolValue" class="input-common" />
				</label>
			</div>

			<div class="flex flex-wrap gap-5">
				<div class="flex-1">
					<button type="submit" class="mt-5 w-full bg-primary-400-500-token">Confirm</button>
				</div>

				<div class="flex-1">
					<button class="mt-5 w-full bg-error-400-500-token" on:click={() => modalStore.close()}
						>Cancel</button
					>
				</div>
			</div>
		</form>
	</Card>
{/if}

<style lang="postcss">
</style>
