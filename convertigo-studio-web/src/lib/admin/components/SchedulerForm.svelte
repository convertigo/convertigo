<script>
	import { RadioGroup, RadioItem, getModalStore } from '@skeletonlabs/skeleton';

	const modalStore = getModalStore();

	let enable = false;
	let writeOutput = false;

	const inputNames = {
		name: 'name',
		description: 'description'
	};
</script>

<div class="col-span-2 flex flex-col gap-5">
	<label class="border-common">
		<p class="label-common">Name</p>
		<input name={inputNames.name} value="" class="input-common" />
	</label>
	<label class="border-common">
		<p class="label-common">Description</p>
		<input name={inputNames.description} value="" class="input-common" />
	</label>
	<p class="label-common">Enable</p>
	<RadioGroup>
		<RadioItem name="enabled" bind:group={enable} active="bg-success-400-500-token" value={true}
			>Yes</RadioItem
		>
		<RadioItem name="enabled" bind:group={enable} active="bg-error-400-500-token" value={false}
			>No</RadioItem
		>
	</RadioGroup>
	{#if $modalStore[0]?.meta?.mode === 'TransactionConvertigoJob' || 'SequenceConvertigoJob'}
		<p class="label-common">Write Output</p>
		<RadioGroup>
			<RadioItem
				name="writeOutput"
				active="bg-success-400-500-token"
				bind:group={writeOutput}
				value={true}>Yes</RadioItem
			>
			<RadioItem
				name="writeOutput"
				active="bg-error-400-500-token"
				bind:group={writeOutput}
				value={false}>No</RadioItem
			>
		</RadioGroup>
	{/if}
</div>
