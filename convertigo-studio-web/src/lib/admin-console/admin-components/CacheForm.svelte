<script lang="ts">
	import { Accordion } from '@skeletonlabs/skeleton';
	import Card from './Card.svelte';
	import AutoGrid from './AutoGrid.svelte';
	import { cacheType } from '../stores/cacheStore';

	let databaseUsed = 'SQLServer';
	let serverName = '';
	let accessPort = '';
	let databaseServiceName = '';
	let userName = '';
	let userPassword = '';
	let userPasswordConfirmation = '';
	let tableName = '';
	let createTable = false;

	export async function submitCacheConfiguration(config) {
		try {
			const response = await call('cache.Configure', config);
			console.log('Response from cache.Configure:', response);
		} catch (error) {
			console.error('Error submitting cache configuration:', error);
		}
	}

	async function handleSubmit() {
		//prepar config object to be send
		const config = {
			cacheType: $cacheType,
			databaseUsed,
			serverName,
			accessPort,
			databaseServiceName,
			userName,
			userPassword,
			tableName,
			createTable
		};

		await submitCacheConfiguration;
	}
</script>

<form on:submit|preventDefault={handleSubmit}>
	<AutoGrid>
		<Card>
			<label class="bg-surface-800 p-1 text-[14px]" for="databaseUsed">Database used:</label>
			<select id="databaseUsed" class="text-black mt-5 text-[13px]" bind:value={databaseUsed}>
				<option value="SQLServer" class="text-[13px]">SQLServer</option>
				<option value="Oracle" class="text-[13px]">Oracle</option>
				<option value="MySQL" class="text-[13px]">MySQL</option>
			</select>
		</Card>

		<Card>
			<h2 class="bg-surface-800 text-[14px]">Access configuration :</h2>
			<label for="serverName">Server name:</label>
			<input id="serverName" class="text-black" type="text" bind:value={serverName} />

			<label for="accessPort">Access port:</label>
			<input id="accessPort" type="text" bind:value={accessPort} />

			<label for="databaseServiceName">Database/Service name:</label>
			<input id="databaseServiceName" type="text" bind:value={databaseServiceName} />
		</Card>
	</AutoGrid>

	<div class="mt-3">
		<AutoGrid>
			<Card>
				<h2 class="bg-surface-800 text-[14px]">Configuration of the identification :</h2>
				<label for="userName">User name:</label>
				<input id="userName" type="text" bind:value={userName} />

				<label for="userPassword">User password:</label>
				<input id="userPassword" type="password" bind:value={userPassword} />

				<label for="userPasswordConfirmation">Confirmation:</label>
				<input
					id="userPasswordConfirmation"
					type="password"
					bind:value={userPasswordConfirmation}
				/>
			</Card>

			<Card>
				<h2 class="bg-surface-800 text-[14px]">Cache table</h2>
				<label for="tableName">Table name:</label>
				<input id="tableName" type="text" bind:value={tableName} />
			</Card>
		</AutoGrid>
	</div>

	<div class="mt-3">
		<Card>
			<div class="flex justify-center h-5">
				<label for="createTable" class="flex items-center mr-5">
					<input id="createTable" type="checkbox" bind:checked={createTable} />
					<p class="ml-2">Create table and apply</p>
				</label>

				<button type="submit" class="ml-5 bg-surface-500 pl-4 pr-4">Submit</button>
			</div>
		</Card>
	</div>
</form>

<style>
	input {
		@apply text-[13px] h-8 text-black;
	}
</style>
