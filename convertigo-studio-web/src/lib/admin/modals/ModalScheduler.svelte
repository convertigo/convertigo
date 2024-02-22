<script>
	import { RadioGroup, RadioItem, getModalStore } from '@skeletonlabs/skeleton';

	import { call } from '$lib/utils/service';
	import Card from '../components/Card.svelte';

	const modalStore = getModalStore();

	let enable = 'false';
	let writeOutput = 'false';
	const { mode } = $modalStore[0].meta;

	async function createScheduledElements(e) {
		e.preventDefault();
		const fd = new FormData(e.target.form);
		//@ts-ignore
		const res = await call('scheduler.CreateScheduledElements', fd);
	}
</script>

{#if mode == 'newTransaction'}
	<Card title="New transaction" class="p-10">
		<form class="flex flex-col" on:submit={createScheduledElements} name="type">
			<div class="flex gap-5 mb-10">
				<label class="border-common">
					<p class="input-name">Name :</p>
					<input name="name" class="input-common" />
				</label>

				<label class="border-common">
					<p class="input-name">description :</p>
					<input name="description" class="input-common" />
				</label>
			</div>

			<p class="input-name">Enable :</p>
			<RadioGroup>
				<RadioItem name="enabled" bind:group={enable} value="true">Yes</RadioItem>
				<RadioItem name="enabled" bind:group={enable} value="false">No</RadioItem>
			</RadioGroup>

			<label class="border-common mt-10">
				<p class="input-name">Context :</p>
				<input name="context" class="input-common" />
			</label>

			<p class="input-name mt-5">Write output :</p>
			<RadioGroup>
				<RadioItem name="writeOutput" bind:group={writeOutput} value="true">Yes</RadioItem>
				<RadioItem name="writeOutput" bind:group={writeOutput} value="false">No</RadioItem>
			</RadioGroup>

			<p class="input-name mt-10">Project :</p>
			<select name="project">
				<option></option>
			</select>

			<p class="input-name mt-5">Connector :</p>
			<select name="connector">
				<option></option>
			</select>

			<p class="input-name mt-5">Transaction :</p>
			<select name="transaction">
				<option></option>
			</select>

			<div class="flex gap-10 mt-10">
				<button type="submit" class="btn bg-buttons w-40 text-white">Confirm</button>

				<button class="btn bg-buttons w-40 text-white" on:click={() => modalStore.close()}
					>Cancel</button
				>
			</div>
		</form>
	</Card>
{/if}

{#if mode == 'newSequence'}
	new sequence
{/if}

{#if mode == 'newJobGroup'}
	new job group
{/if}
