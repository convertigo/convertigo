<script>
	import AutoGrid from '$lib/admin/components/AutoGrid.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import { call, copyObj, equalsObj } from '$lib/utils/service';
	import { RadioGroup, RadioItem } from '@skeletonlabs/skeleton';
	import { writable } from 'svelte/store';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import CacheInput from '$lib/admin/components/CacheInput.svelte';
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {import('svelte/store').Writable<any>}*/
	let conf = writable({});
	let oriConf = null;

	export const modalStoreCache = getModalStore();

	async function update() {
		const response = await call('cache.ShowProperties');
		$conf = {
			databaseType: 'mariadb',
			serverName: 'dbhost',
			port: 3306,
			databaseName: 'c8ocache',
			userName: 'cache_user',
			userPassword: '',
			cacheTableName: 'c8ocache',
			...response.admin
		};
		oriConf = copyObj($conf);
	}

	onMount(() => {
		update();
	});

	/** @param {any} e */
	async function handlesubmit(e) {
		let formData = new FormData(e.target);
		if (e.submitter.textContent == 'Create Table and Apply') {
			formData.append('create', '');
		}
		try {
			// @ts-ignore
			const response = await call('cache.Configure', formData);
			console.log(response);
			// @ts-ignore
			modalStoreCache.trigger({
				title: 'Applied with success'
			});
		} catch (error) {
			// @ts-ignore
			modalStoreCache.trigger({
				title: 'Error'
			});
		}
		update();
	}

	async function cacheClear() {
		await call('cache.Clear');
	}
</script>

{#if oriConf}
	{@const disabled = equalsObj($conf, oriConf)}
	<Card class="mb-4">
		<button class="variant-filled-error" on:click={cacheClear}
			><Ico icon="material-symbols-light:delete-outline" />Clear entries</button
		>
	</Card>
	<form on:submit|preventDefault={handlesubmit}>
		<Card title="Cache Type">
			<p class="mt-5">Choose the desired cache type</p>
			<div class="flex mt-5">
				<div class="flex items-center gap-4">
					<RadioGroup>
						{#each [{ text: 'File', value: 'com.twinsoft.convertigo.engine.cache.FileCacheManager' }, { text: 'Database', value: 'com.twinsoft.convertigo.engine.cache.DatabaseCacheManager' }] as { text, value }}
							<RadioItem
								bind:group={$conf.cacheType}
								name="cacheType"
								{value}
								active="variant-filled-surface text-white"
							>
								{text}
							</RadioItem>
						{/each}
					</RadioGroup>
					<button type="submit" class="bg-primary-400" {disabled}>Apply</button>
					<button
						class="variant-filled-error"
						{disabled}
						on:click={() => ($conf = copyObj(oriConf))}>Cancel</button
					>
				</div>
			</div>
		</Card>

		{#if $conf.cacheType === 'com.twinsoft.convertigo.engine.cache.DatabaseCacheManager'}
			<AutoGrid class="mt-5">
				<Card title="Database Used">
					<RadioGroup
						class="flex flex-col mt-5 p-5 variant-filled-success text-token"
						active="variant-filled-success"
					>
						{@const data = [
							{ value: 'sqlserver', text: 'SQLServer' },
							{ value: 'mysql', text: 'MySQL' },
							{ value: 'mariadb', text: 'MariaDB' },
							{ value: 'postgresql', text: 'PostgreSQL' },
							{ value: 'oracle', text: 'Oracle' }
						]}
						{#each data as { value, text }}
							<RadioItem bind:group={$conf.databaseType} name="databaseDriver" {value}
								>{text}</RadioItem
							>
						{/each}
					</RadioGroup>
				</Card>

				{@const sections = [
					{
						title: 'Access Configuration',
						fields: [
							{ label: 'Server Name', name: 'serverName' },
							{ label: 'Access Port', name: 'port' }
						]
					},
					{
						title: 'Configuration of the Identification',
						fields: [
							{ label: 'User Name', name: 'userName' },
							{ label: 'User Password', name: 'userPassword', type: 'password' }
						]
					},
					{
						title: 'Cache Table',
						fields: [
							{ label: 'Database / Service Name', name: 'databaseName' },
							{ label: 'Table Name', name: 'cacheTableName' }
						]
					}
				]}

				{#each sections as { title, fields }}
					<Card {title}>
						{#each fields as { label, name, type }}
							<CacheInput {label} {name} {type} bind:value={$conf[name]} />
						{/each}
					</Card>
				{/each}
			</AutoGrid>

			<div class="mt-3">
				<Card>
					<div class="flex flex-row flex-wrap justify-center gap-4">
						<button type="submit" {disabled}>Create Table and Apply</button>
						<button
							class="variant-filled-error"
							{disabled}
							on:click={() => ($conf = copyObj(oriConf))}>Cancel</button
						>
					</div>
				</Card>
			</div>
		{/if}
	</form>
{:else}
	Loading
{/if}
