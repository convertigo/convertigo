<script>
	import { call } from '$lib/utils/service';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { globalSymbols } from '../stores/symbolsStore';

	let newSymbol = '';

	const modalStore = getModalStore();

	onMount(() => {});

	async function addSecretSymbol(newSecretSymbol) {
		const successModal = {
			title: 'Added with success'
		};
		const failedModal = {
			title: 'failed adding new symbol'
		};
		newSecretSymbol.preventDefault();
		let formData = new FormData(newSecretSymbol.target);

		let symbolName = formData.get('symbolName') + '.secret';
		formData.set('symbolName', symbolName);

		if (newSecretSymbol.submitter.textContent == 'Confirm') {
			//@ts-ignore
			const response = await call('global_symbols.Add', formData);
			if (response.includes('success')) {
				//@ts-ignore
				modalStore.close(successModal);
				//@ts-ignore
				modalStore.trigger(successModal);
			} else {
				//@ts-ignore
				modalStore.trigger(failedModal);
			}
		}
	}
</script>

<form on:submit={addSecretSymbol} class="p-10 rounded-xl glass">
	<h1 class="text-2xl mb-5">Add a new secret symbol</h1>
	<p>In secret mode, the value is stored ciphered and the key automatically ends with .secret</p>
	<div class="flex gap-5 mt-5">
		<label>
			<p class="font-light text-[14px]">Enter secret symbol name :</p>
			<input placeholder="Secret name" name="symbolName" class="text-black" />
		</label>
		<label>
			<p class="font-light text-[14px]">Enter secret symbol Value :</p>
			<input placeholder="Secret value" type="password" name="symbolValue" class="text-black" />
		</label>
	</div>
	<button type="submit" class="mt-5 btn bg-white text-black font-light">Confirm</button>
</form>

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
		background-color: rgba(255, 255, 255, 0.1); /* Couleur de fond légèrement transparente */
	}
</style>
