<script>
	import { call } from '$lib/utils/service';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { globalSymbols } from '../stores/symbolsStore';

	let newSymbol = '';

	const modalStore = getModalStore();

	onMount(() => {});

	async function addGlobalSymbol(newSymbol) {
		const successModal = {
			title: 'Added with success'
		};
		const failedModal = {
			title: 'failed adding new symbol'
		};
		newSymbol.preventDefault();
		let formData = new FormData(newSymbol.target);

		if (newSymbol.submitter.textContent == 'Confirm') {
			//@ts-ignore
			const response = await call('global_symbols.Add', formData);
			//@ts-ignore
			modalStore.close(successModal);
			globalSymbols();
			if (response) {
				//@ts-ignore
				modalStore.trigger(successModal);
			} else {
				//@ts-ignore
				modalStore.trigger(failedModal);
			}
		}
	}
</script>

<form on:submit={addGlobalSymbol} class="p-10 rounded-xl">
	<h1 class="text-2xl mb-5">Add a new symbol</h1>
	<div class="flex gap-5">
		<label>
			<p class="font-light text-[14px]">Enter Symbol name :</p>
			<input placeholder="Name" name="symbolName" class="text-black" />
		</label>
		<label>
			<p class="font-light text-[14px]">Enter Symbol Value :</p>
			<input placeholder="Value" name="symbolValue" class="text-black" />
		</label>
	</div>
	<button type="submit" class="mt-5 btn bg-white text-black font-light">Confirm</button>
</form>

<style>
	.background {
		background-color: black;
		opacity: calc(70);
	}
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
		background-color: rgba(255, 255, 255, 1); /* Couleur de fond légèrement transparente */
	}
</style>
