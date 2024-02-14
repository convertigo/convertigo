<script>
	import AutoGrid from '$lib/admin-console/admin-components/AutoGrid.svelte';
	import Card from '$lib/admin-console/admin-components/Card.svelte';
	import { call } from '$lib/utils/service';
	import { RadioGroup, RadioItem } from '@skeletonlabs/skeleton';
	import { writable } from 'svelte/store';
	import { Accordion, AccordionItem, getModalStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import PropertyType from '$lib/admin-console/admin-components/PropertyType.svelte';
	import CacheInput from '$lib/admin-console/admin-components/CacheInput.svelte';
	import Icon from '@iconify/svelte';

	let conf = writable(/** @type {any}*/ {});

	export const modalStoreCache = getModalStore();

	let databaseConfigurations = [
		{ label: 'Server Name', name: 'serverName', value: $conf.serverName ?? '' },
		{ label: 'Access port:', name: 'serverPort', value: $conf.serverPort ?? '' },
		{ label: 'Database/Service name:', name: 'databaseName', value: $conf.databaseName ?? '' }
	];

	let identificationConfigurations = [
		{ label: 'User name:', name: 'cacheUserName', value: $conf.userName ?? '' },
		{
			label: 'User password:',
			name: 'userPassword',
			type: 'password',
			value: $conf.userPassword ?? ''
		},
		{ label: 'Confirmation:', name: 'userPasswordConfirmation', type: 'password', value: '' }
	];

	let cacheTableConfigurations = [
		{ label: 'Table Name', name: 'cacheTableName', value: $conf.cacheTableName ?? '' }
	];

	onMount(() => {
		call('cache.ShowProperties').then((response) => {
			cacheType = response.admin.cacheType;
			$conf = response.admin;
			console.log($conf);
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
			console.log(response);
			// @ts-ignore
			modalStoreCache.trigger(successModalapplied);
		} catch (error) {
			// @ts-ignore
			modalStoreCache.trigger(failedModalapplied);
		}
	}

	let cacheType = 'com.twinsoft.convertigo.engine.cache.FileCacheManager';
</script>

{#if $conf}
	<form on:submit={handlesubmit}>
		<Card title="Cache type">
			<p class="mt-5">Choose the desired cache type :</p>
			<div class="flex mt-5">
				<div class="flex items-center">
					<RadioGroup>
						<RadioItem
							bind:group={cacheType}
							name="cacheType"
							value="com.twinsoft.convertigo.engine.cache.FileCacheManager"
							id="cacheTypeFile"
							active="bg-buttons text-white"
						>
							<label for="cacheTypeFile" class="text-[14px]">file</label>
						</RadioItem>
						<RadioItem
							bind:group={cacheType}
							name="cacheType"
							value="com.twinsoft.convertigo.engine.cache.DatabaseCacheManager"
							id="cacheTypeDatabase"
							active="bg-buttons text-white"
						>
							<label for="cacheTypeDatabase" class="text-[14px]">database</label>
						</RadioItem>
					</RadioGroup>

					<button type="submit" class="ml-5 btn bg-buttons text-white">Apply</button>
				</div>
			</div>
		</Card>

		{#if cacheType === 'com.twinsoft.convertigo.engine.cache.DatabaseCacheManager'}
			<Card title="Database configurations" customStyle="margin-top: 20px;">
				<Accordion class="dark:border-surface-600 border-[1px] rounded-xl ">
					<AccordionItem
						class="dark:bg-surface-800 bg-white rounded-xl"
						open={cacheType === 'com.twinsoft.convertigo.engine.cache.DatabaseCacheManager'}
					>
						<svelte:fragment slot="summary">
							<div class="flex items-center">
								<Icon icon="bi:database-fill-gear" class="mr-2 h-7 w-7" />
								Configurations
							</div>
						</svelte:fragment>
						<svelte:fragment slot="content">
							<AutoGrid>
								<Card title="Database used">
									<select
										name="databaseDriver"
										id="databaseUsed"
										class="text-surface-800 mt-5 text-[13px] rounded-xl border-surface-200"
										value={$conf.databaseDriver ?? 'sqlserver'}
									>
										<option value="sqlserver" class="text-[13px]">SQLServer</option>
										<option value="oracle" class="text-[13px]">Oracle</option>
										<option value="mysql" class="text-[13px]">MySQL</option>
									</select>
								</Card>

								<Card title="Access configuration" customStyle="">
									{#each databaseConfigurations as config}
										<CacheInput {...config} />
									{/each}
								</Card>
							</AutoGrid>

							<div class="mt-3">
								<AutoGrid>
									<Card title="Configuration of the identification">
										{#each identificationConfigurations as identificationConfig}
											<CacheInput {...identificationConfig} />
										{/each}
									</Card>

									<Card title="Cache table">
										{#each cacheTableConfigurations as cacheTable}
											<CacheInput {...cacheTable} />
										{/each}
									</Card>
								</AutoGrid>
							</div>

							<div class="mt-3">
								<Card>
									<div class="flex justify-center">
										<button type="submit" class="btn bg-buttons text-white"
											>Create table and apply</button
										>
									</div>
								</Card>
							</div>
						</svelte:fragment>
					</AccordionItem>
				</Accordion>
			</Card>
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
