<script>
	import Card from '$lib/admin/components/Card.svelte';
	import { call } from '$lib/utils/service';
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
				<PropertyType
					name="cacheType"
					item={[
						{ text: 'File', value: 'com.twinsoft.convertigo.engine.cache.FileCacheManager' },
						{ text: 'Database', value: 'com.twinsoft.convertigo.engine.cache.DatabaseCacheManager' }
					]}
					type="segment"
					bind:value={Cache.conf.cacheType}
					loading={Cache.loading}
					defaultValue={Cache.confDefault?.cacheType}
					originalValue={Cache.confOriginal?.cacheType}
				/>
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
							disabled: !Cache.hasChanged,
							onclick: Cache.cancel
						}
					]}
				/>
			</div>
		</Card>

		{#if Cache.conf?.cacheType.endsWith('DatabaseCacheManager')}
			{@const item = [
				{ value: 'sqlserver', text: 'SQLServer' },
				{ value: 'mysql', text: 'MySQL' },
				{ value: 'mariadb', text: 'MariaDB' },
				{ value: 'postgresql', text: 'PostgreSQL' },
				{ value: 'oracle', text: 'Oracle' }
			]}
			<div class="grid gap grid-cols-1 md:grid-cols-2" transition:slide>
				<Card title="Database Used" class="!items-stretch">
					<PropertyType
						name="databaseType"
						{item}
						type="segment"
						bind:value={Cache.conf.databaseType}
						orientation="vertical"
						loading={Cache.loading}
						defaultValue={Cache.confDefault?.databaseType}
						originalValue={Cache.confOriginal?.databaseType}
					/>
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
							{#each fields as field}
								<PropertyType
									{...field}
									loading={Cache.loading}
									defaultValue={Cache.confDefault[field.name]}
									originalValue={Cache.confOriginal[field.name]}
									bind:value={Cache.conf[field.name]}
								/>
							{/each}
						</Card>
					{/each}
				{/if}
			</div>
		{/if}
	</fieldset>
</form>
