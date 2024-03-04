<script>
	import AutoGrid from '$lib/admin/components/AutoGrid.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import { call } from '$lib/utils/service';
	import { RadioGroup, RadioItem } from '@skeletonlabs/skeleton';
	import { writable } from 'svelte/store';
	import { Accordion, AccordionItem, getModalStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import CacheInput from '$lib/admin/components/CacheInput.svelte';
	import Icon from '@iconify/svelte';

	/** @type {import('svelte/store').Writable<any>}*/
	let conf = writable({});

	export const modalStoreCache = getModalStore();

	let databaseConfigurations = [
		{ label: 'Server Name', name: 'serverName', value: $conf.serverName ?? '' },
		{ label: 'Access Port', name: 'serverPort', value: $conf.serverPort ?? '' },
		{ label: 'Database / Service Name', name: 'databaseName', value: $conf.databaseName ?? '' }
	];

	let identificationConfigurations = [
		{ label: 'User Name:', name: 'cacheUserName', value: $conf.userName ?? '' },
		{
			label: 'User Password:',
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
		$conf.databaseDriver = 'sqlserver';

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
			<p class="mt-5">Choose the desired cache type</p>
			<div class="flex mt-5">
				<div class="flex items-center">
					<RadioGroup>
						<RadioItem
							bind:group={cacheType}
							name="cacheType"
							value="com.twinsoft.convertigo.engine.cache.FileCacheManager"
							id="cacheTypeFile"
							active="variant-filled-surface text-white "
						>
							<label for="cacheTypeFile" class="text-[14px]">file</label>
						</RadioItem>
						<RadioItem
							bind:group={cacheType}
							name="cacheType"
							value="com.twinsoft.convertigo.engine.cache.DatabaseCacheManager"
							id="cacheTypeDatabase"
							active="variant-filled-surface text-white"
						>
							<label for="cacheTypeDatabase" class="text-[14px]">database</label>
						</RadioItem>
					</RadioGroup>

					<button type="submit" class="ml-5 bg-primary-400">Apply</button>
				</div>
			</div>
		</Card>

		{#if cacheType === 'com.twinsoft.convertigo.engine.cache.DatabaseCacheManager'}
			<Card title="Database configurations" class="mt-5">
				<Accordion class="rounded-token" caretOpen="rotate-0" caretClosed="-rotate-90">
					<AccordionItem
						class="rounded-token"
						open={cacheType === 'com.twinsoft.convertigo.engine.cache.DatabaseCacheManager'}
					>
						<svelte:fragment slot="summary">
							<div class="flex items-center">
								<Icon icon="bi:database-fill-gear" class="mr-2 h-7 w-7" />
								Configurations
							</div>
						</svelte:fragment>

						<svelte:fragment slot="content">
							<AutoGrid class="mt-5">
								<Card title="Database Used">
									<RadioGroup
										class="flex flex-col mt-5 p-5 variant-filled-success text-token"
										active="variant-filled-success"
									>
										<RadioItem
											bind:group={$conf.databaseDriver}
											name="databaseDriver"
											value="sqlserver"
											id="driverSqlserver">SQL Server</RadioItem
										>
										<RadioItem
											bind:group={$conf.databaseDriver}
											name="databaseDriver"
											value="oracle"
											id="driverOracle">Oracle</RadioItem
										>
										<RadioItem
											bind:group={$conf.databaseDriver}
											name="databaseDriver"
											value="mysql"
											id="driverMysql">MySQL</RadioItem
										>
									</RadioGroup>
								</Card>

								<Card title="Access Configuration" customStyle="">
									{#each databaseConfigurations as config}
										<CacheInput {...config} />
									{/each}
								</Card>
							</AutoGrid>

							<div class="mt-3">
								<AutoGrid>
									<Card title="Configuration of the Identification">
										{#each identificationConfigurations as identificationConfig}
											<CacheInput {...identificationConfig} />
										{/each}
									</Card>

									<Card title="Cache Table">
										{#each cacheTableConfigurations as cacheTable}
											<CacheInput {...cacheTable} />
										{/each}
									</Card>
								</AutoGrid>
							</div>

							<div class="mt-3">
								<Card>
									<div class="flex justify-center">
										<button type="submit">Create table and apply</button>
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
