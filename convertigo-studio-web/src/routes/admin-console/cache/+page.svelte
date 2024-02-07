<script>
	import AutoGrid from '$lib/admin-console/admin-components/AutoGrid.svelte';
	import Card from '$lib/admin-console/admin-components/Card.svelte';
	import { call } from '$lib/utils/service';
	import { RadioGroup, RadioItem } from '@skeletonlabs/skeleton';
	import { writable } from 'svelte/store';
	import { Accordion, AccordionItem, getModalStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';

	let conf = writable(/** @type {any}*/ {});

	export const modalStoreCache = getModalStore();

	onMount(() => {
		call('cache.ShowProperties').then((response) => {
			cacheType = response.admin.cacheType;
			$conf = response.admin;
		});
	});

	async function handlesubmit(e) {
		const successModalapplied = {
			title: 'Applied with succes'
		};
		const failedModalapplied = {
			title: 'Applied with succes'
		};

		e.preventDefault();
		let formData = new FormData(e.target);
		if (e.submitter.textContent == 'Create table and apply') {
			formData.append('create', '');
		}
		try {
			// @ts-ignore
			const response = await call('cache.Configure', formData);
			// @ts-ignore
			modalStoreCache.trigger(successModalapplied);
		} catch (error) {
			// @ts-ignore
			modalStoreCache.trigger(failedModalapplied);
		}
	}

	let cacheType = 'com.twinsoft.convertigo.engine.cache.FileCacheManager';
</script>

<h1 class="mb-5 pb-2 border-1 border-b border-surface-100">Cache</h1>

{#if $conf}
	<form on:submit={handlesubmit}>
		<Card>
			<h2>Cache type</h2>
			<p class="mt-5">Choose the desired cache type :</p>
			<div class="flex mt-5">
				<div class="flex items-center">
					<RadioGroup active="variant-filled-primary" hover="hover:variant-soft-primary">
						<RadioItem
							bind:group={cacheType}
							name="cacheType"
							value="com.twinsoft.convertigo.engine.cache.FileCacheManager"
							id="cacheTypeFile"
						>
							<label for="cacheTypeFile" class="text-[14px]">file</label>
						</RadioItem>
						<RadioItem
							bind:group={cacheType}
							name="cacheType"
							value="com.twinsoft.convertigo.engine.cache.DatabaseCacheManager"
							id="cacheTypeDatabase"
						>
							<label for="cacheTypeDatabase" class="text-[14px]">database</label>
						</RadioItem>
					</RadioGroup>

					<button type="submit" class="ml-10 p-0 bg-surface-100 pl-4 pr-4 btn variant-filled"
						>Apply</button
					>
				</div>
			</div></Card
		>

		{#if cacheType === 'com.twinsoft.convertigo.engine.cache.DatabaseCacheManager'}
			<Accordion width="w-[100%] mt-10 bg-surface-700">
				<AccordionItem
					open={cacheType === 'com.twinsoft.convertigo.engine.cache.DatabaseCacheManager'}
				>
					<svelte:fragment slot="summary">Configurations</svelte:fragment>
					<svelte:fragment slot="content">
						<AutoGrid>
							<Card>
								<label class="bg-surface-800 p-1 text-[14px]" for="databaseUsed"
									>Database used:</label
								>
								<select
									name="databaseDriver"
									id="databaseUsed"
									class="text-black mt-5 text-[13px]"
									value={$conf.databaseDriver ?? 'sqlserver'}
								>
									<option value="sqlserver" class="text-[13px]">SQLServer</option>
									<option value="oracle" class="text-[13px]">Oracle</option>
									<option value="mysql" class="text-[13px]">MySQL</option>
								</select>
							</Card>

							<Card>
								<h2 class="bg-surface-800 text-[14px]">Access configuration :</h2>
								<label for="serverName">Server name:</label>
								<input
									id="serverName"
									name="serverName"
									class="text-black"
									type="text"
									value={$conf.serverName ?? ''}
								/>

								<label for="accessPort">Access port:</label>
								<input
									id="accessPort"
									type="text"
									name="serverPort"
									value={$conf.serverPort ?? ''}
								/>

								<label for="databaseServiceName">Database/Service name:</label>
								<input
									id="databaseServiceName"
									type="text"
									name="databaseName"
									value={$conf.databaseName ?? ''}
								/>
							</Card>
						</AutoGrid>

						<div class="mt-3">
							<AutoGrid>
								<Card>
									<h2 class="bg-surface-800 text-[14px]">Configuration of the identification :</h2>
									<label for="userName">User name:</label>
									<input
										id="userName"
										type="text"
										name="cacheUserName"
										value={$conf.userName ?? ''}
									/>

									<label for="userPassword">User password:</label>
									<input
										id="userPassword"
										type="password"
										name="userPassword"
										value={$conf.userPassword ?? ''}
									/>

									<label for="userPasswordConfirmation">Confirmation:</label>
									<input id="userPasswordConfirmation" type="password" />
								</Card>

								<Card>
									<h2 class="bg-surface-800 text-[14px]">Cache table</h2>
									<label for="tableName">Table name:</label>
									<input
										id="tableName"
										type="text"
										name="cacheTableName"
										value={$conf.cacheTableName ?? ''}
									/>
								</Card>
							</AutoGrid>
						</div>

						<div class="mt-3">
							<Card>
								<div class="flex justify-center">
									<button type="submit" class="p-0 bg-surface-100 w-80 btn variant-filled"
										>Create table and apply</button
									>
								</div>
							</Card>
						</div>
					</svelte:fragment>
				</AccordionItem>
			</Accordion>
		{/if}
	</form>
{:else}
	Loading
{/if}

<style lang="postcss">
	input {
		@apply text-[13px] h-8 text-black;
	}
</style>
