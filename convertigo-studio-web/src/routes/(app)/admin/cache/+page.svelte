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
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';

	/** @type {import('svelte/store').Writable<any>}*/
	let conf = writable({});
	let oriConf = $state(null);
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
	}

	onMount(() => {
		update();
	});

	/** @param {any} e */
	async function handlesubmit(e) {
		e.preventDefault();
		let formData = new FormData(e.target);
		if (e.submitter.textContent == 'Create Table and Apply') {
			formData.append('create', '');
		}
		try {
			// @ts-ignore
			const response = await call('cache.Configure', formData);
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
	<form onsubmit={handlesubmit}>
		<Card title="Cache Type">
			{#snippet cornerOption()}
				<ButtonsContainer>
					<button class="delete-button" onclick={cacheClear}>
						<Ico icon="mingcute:delete-line" />
						<p>Clear entries</p>
					</button>
				</ButtonsContainer>
			{/snippet}

			<p class="mt-5 mb-2 font-normal">Choose the desired cache type</p>
			<div class="flex items-center justify-between gap-5">
				<RadioGroup>
					{#each [{ text: 'File', value: 'com.twinsoft.convertigo.engine.cache.FileCacheManager' }, { text: 'Database', value: 'com.twinsoft.convertigo.engine.cache.DatabaseCacheManager' }] as { text, value }}
						<RadioItem
							bind:group={$conf.cacheType}
							name="cacheType"
							{value}
							active="preset-filled-surface text-white"
						>
							{text}
						</RadioItem>
					{/each}
				</RadioGroup>
				<ButtonsContainer>
					<button type="submit" class="green-button" {disabled}>
						<Ico icon="solar:mask-happly-line-duotone" />
						<p>Apply</p></button
					>
					<button type="submit" class="basic-button" {disabled}
						><Ico icon="lets-icons:table-light" />
						<p>Create Table and Apply</p></button
					>
					<button class="delete-button" {disabled} onclick={() => ($conf = copyObj(oriConf))}>
						<Ico icon="material-symbols-light:cancel-outline" />
						<p>Cancel</p></button
					>
				</ButtonsContainer>
			</div>
		</Card>

		{#if $conf.cacheType === 'com.twinsoft.convertigo.engine.cache.DatabaseCacheManager'}
			<AutoGrid class="mt-5">
				<Card title="Database Used">
					<RadioGroup
						class="flex flex-col p-5 preset-filled-success text"
						active="bg-secondary-400-500"
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
