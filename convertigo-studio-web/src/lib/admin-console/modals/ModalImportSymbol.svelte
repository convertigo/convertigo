<script>
	import { call } from '$lib/utils/service';
	import { RadioGroup, RadioItem } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';

	let value = 0;

	let file;

	onMount(() => {
		importSymbol();
	});

	async function importSymbol(e) {
		e.preventdefault();
		let formData = new FormData(e.target);

		formData.append('file', file);

		try {
			//@ts-ignore
			const response = await call('global_symbols.Import', formData);
			console.log(response);
		} catch (err) {
			console.error(err);
		}
	}
</script>

<form on:submit={importSymbol} class="p-10 rounded-xl glass flex flex-col">
	<h1 class="text-xl mb-5 text-center">Import global symbols</h1>

	<RadioGroup>
		<RadioItem bind:group={value} name="justify" value="ClearImport">Clear & import</RadioItem>
		<RadioItem bind:group={value} name="justify" value="MergeSymbols">Merge symbols</RadioItem>
	</RadioGroup>
	<input type="file" />
	<p class="mt-10 text-[14px] mb-5 text-center">In case of name conflict :</p>

	<RadioGroup>
		<RadioItem bind:group={value} name="justify" value="PriorityServer">Priority Server</RadioItem>
		<RadioItem bind:group={value} name="justify" value="PriorityImport">Priority import</RadioItem>
	</RadioGroup>

	<button type="submit" class="btn variant-filled mt-5">Import</button>
</form>

<style lang="postcss">
	.glass {
		background: rgba(255, 255, 255, 0.15);
		box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
		backdrop-filter: blur(2px);
		-webkit-backdrop-filter: blur(4px);
		border-radius: 10px;
		border: 2px solid rgba(138, 138, 138, 0.18);
	}

	form {
		background-color: rgba(255, 255, 255, 0.1); /* Couleur de fond légèrement transparente */
	}
</style>
