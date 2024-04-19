<script>
	import { RadioGroup, RadioItem, getModalStore } from '@skeletonlabs/skeleton';
	import { call } from '$lib/utils/service';
	import Card from '../components/Card.svelte';
	import AutoGrid from '../components/AutoGrid.svelte';
	import { onMount } from 'svelte';
	import { schedulerList } from '../stores/schedulerStore';
	import { projectsCheck, projectsStore } from '../stores/projectsStore';
	import {
		getProjectTestPlatform,
		connectorsStore,
		transactionsStore
	} from '../stores/testPlatformStore';

	const modalStore = getModalStore();

	let enable = 'false';
	let writeOutput = 'false';
	let selectedProjectId;
	let projectConnector;
	let projectTransaction;

	const { mode } = $modalStore[0].meta;

	onMount(async () => {
		await schedulerList();
		await projectsCheck();
		selectedProjectId = $projectsStore[0]['@_name'];
		await getProjectTestPlatform(selectedProjectId);
	});

	function handleProjectChange(event) {
		selectedProjectId = event.target.value;
		getProjectTestPlatform(selectedProjectId);

		console.log('selectedProject', selectedProjectId);
	}

	//Service do not include any response fro that
	async function createScheduledElements(e) {
		e.preventDefault();
		const fd = new FormData(e.target);
		fd.append('enabled', enable);
		fd.append('writeOutput', writeOutput);
		//type actually not dynamic, have to update that
		fd.append('type', 'schedulerNewTransactionConvertigoJob');

		try {
			await call('scheduler.CreateScheduledElements', fd);
			await schedulerList();
			modalStore.close();
		} catch (err) {
			console.error(err);
		}
	}
</script>

{#if mode == 'newJobs'}
	<Card title="New Sequence">
		<form class="p-5" on:submit={createScheduledElements}>
			<div class="grid grid-cols-2 gap-10">
				<div class="col-span-1 flex flex-col gap-5">
					<label class="border-common">
						<p class="label-common">Name:</p>
						<input name="name" value="" class="input-common" />
					</label>
					<label class="border-common">
						<p class="label-common">Description:</p>
						<input name="description" value="" class="input-common" />
					</label>
					<p class="label-common">Enable:</p>
					<RadioGroup>
						<RadioItem
							name="enabled"
							bind:group={enable}
							active="bg-success-400-500-token"
							value="true">Yes</RadioItem
						>
						<RadioItem
							name="enabled"
							bind:group={enable}
							active="bg-error-400-500-token"
							value="false">No</RadioItem
						>
					</RadioGroup>
					<p class="label-common">Write Output:</p>
					<RadioGroup>
						<RadioItem
							name="writeOutput"
							active="bg-success-400-500-token"
							bind:group={writeOutput}
							value="true">Yes</RadioItem
						>
						<RadioItem
							name="writeOutput"
							active="bg-error-400-500-token"
							bind:group={writeOutput}
							value="false">No</RadioItem
						>
					</RadioGroup>
				</div>

				<div class="col-span-1 flex flex-col gap-5">
					<label class="border-common">
						<p class="label-common">Context:</p>
						<input name="context" value="" class="input-common" />
					</label>
					<div class="border-common">
						<p class="label-common w-full">Project:</p>
						<select
							name="project"
							bind:value={selectedProjectId}
							class="input-common"
							on:change={handleProjectChange}
						>
							{#each $projectsStore as project}
								<option value={project['@_name']} selected={selectedProjectId}
									>{project['@_name']}</option
								>
							{/each}
						</select>
					</div>

					<div class="border-common mt-5">
						<p class="label-common w-full">Connector:</p>
						{#if $connectorsStore.length > 0}
							<select name="connector" bind:value={projectConnector} class="input-common">
								{#each $connectorsStore as connector}
									<option value={connector['@_name']} selected={projectConnector}
										>{connector['@_name']}</option
									>
								{/each}
							</select>
						{:else}
							No connectors
						{/if}
					</div>

					<div class="border-common mt-5">
						<p class="label-common w-full">Transaction:</p>
						{#if $transactionsStore.length > 0}
							<select name="transaction" bind:value={projectTransaction} class="input-common">
								{#each $transactionsStore as transaction}
									<option value={transaction['@_name']} selected={projectTransaction}
										>{transaction['@_name']}</option
									>
								{/each}
							</select>
						{:else}
							No transaction
						{/if}
					</div>
				</div>

				<div class="flex gap-5 mt-10">
					<button
						class="bg-surface-400-500-token w-40"
						on:click|preventDefault={() => modalStore.close()}>Cancel</button
					>
					<button type="submit" class="bg-primary-400-500-token w-40">Confirm</button>
				</div>
			</div>
		</form>
	</Card>
{/if}
