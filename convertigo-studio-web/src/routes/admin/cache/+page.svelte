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
	//
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
		console.log('cache res', response);
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
	<form on:submit|preventDefault={handlesubmit}>
		<Card title="Cache Type">
			<div slot="cornerOption">
				<div class="flex-1">
					<button class="bg-tertiary-400-500-token" on:click={cacheClear}
						><Ico icon="material-symbols-light:delete-outline" class="mr-2 h-7 w-7" />Clear entries</button
					>
				</div>
			</div>

			<p class="mt-5">Choose the desired cache type</p>
			<div class="flex flex-wrap gap-5 mt-5">
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
				<div class="flex-1">
					<button type="submit" class="bg-primary-400-500-token w-full" {disabled}>Apply</button>
				</div>
				<div class="flex-1">
					<button type="submit" class="bg-primary-400-500-token w-full" {disabled}
						>Create Table and Apply</button
					>
				</div>
				<div class="flex-1">
					<button
						class="bg-error-400-500-token w-full"
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
						class="flex flex-col p-5 variant-filled-success text-token"
						active="bg-secondary-400-500-token"
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
		{/if}
	</form>
{:else}
	Loading
{/if}
