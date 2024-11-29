<script>
	import Card from '$lib/admin/components/Card.svelte';
	import { call } from '$lib/utils/service';
	import { Segment } from '@skeletonlabs/skeleton-svelte';
	import CacheInput from '$lib/admin/components/CacheInput.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import Cache from '$lib/admin/Cache.svelte';
	import { slide } from 'svelte/transition';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';

	/** @param {any} e */
	async function handlesubmit(e) {
		e.preventDefault();
		let formData = new FormData(e.target);
		if (e.submitter.textContent == 'Create Table and Apply') {
			formData.append('create', '');
		}
		await call('cache.Configure', formData);
		await Cache.refresh();
	}
</script>

<form onsubmit={handlesubmit}>
	<fieldset
		disabled={Cache.loading}
		class="layout-y !items-stretch"
		class:animate-pulse={Cache.loading}
	>
		<Card title="Cache Type" class="!items-start">
			{#snippet cornerOption()}
				<ResponsiveButtons
					buttons={[
						{
							label: 'Clear entries',
							icon: 'mingcute:delete-line',
							cls: 'delete-button',
							onclick: Cache.clear
						}
					]}
				/>
			{/snippet}
			<p>Choose the desired cache type</p>
			<div class="layout-x flex-wrap !justify-around w-full">
				<Segment name="cacheType" bind:value={Cache.conf.cacheType}>
					{#each [{ text: 'File', value: 'com.twinsoft.convertigo.engine.cache.FileCacheManager' }, { text: 'Database', value: 'com.twinsoft.convertigo.engine.cache.DatabaseCacheManager' }] as { text, value }}
						<Segment.Item {value} stateFocused="preset-filled-surface text-white">
							{text}
						</Segment.Item>
					{/each}
				</Segment>
				<ResponsiveButtons
					class="grow h-full"
					buttons={[
						{
							label: 'Apply',
							icon: 'solar:mask-happly-line-duotone',
							cls: 'green-button'
						},
						{
							label: 'Create Table and Apply',
							icon: 'lets-icons:table-light',
							cls: 'basic-button',
							hidden: !Cache.conf?.cacheType.endsWith('DatabaseCacheManager')
						},
						{
							label: 'Cancel',
							icon: 'material-symbols-light:cancel-outline',
							cls: 'cancel-button',
							onclick: Cache.cancel
						}
					]}
				/>
			</div>
		</Card>

		{#if Cache.conf?.cacheType.endsWith('DatabaseCacheManager')}
			<div class="grid gap grid-cols-1 md:grid-cols-2" transition:slide>
				<Card title="Database Used" class="!items-stretch">
					<Segment name="databaseType" bind:value={Cache.conf.databaseType} orientation="vertical">
						{@const data = [
							{ value: 'sqlserver', text: 'SQLServer' },
							{ value: 'mysql', text: 'MySQL' },
							{ value: 'mariadb', text: 'MariaDB' },
							{ value: 'postgresql', text: 'PostgreSQL' },
							{ value: 'oracle', text: 'Oracle' }
						]}
						{#each data as { value, text }}
							<Segment.Item {value}>{text}</Segment.Item>
						{/each}
					</Segment>
				</Card>

				{#if true}
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
						<Card {title} class="!items-stretch">
							{#each fields as { label, name, type = 'Text' }}
								<!-- <CacheInput {label} {name} {type} bind:value={Cache.conf[name]} /> -->
								<PropertyType
									property={{ name, type, description: label, value: Cache.conf[name] }}
								/>
							{/each}
						</Card>
					{/each}
				{/if}
			</div>
		{/if}
	</fieldset>
</form>
