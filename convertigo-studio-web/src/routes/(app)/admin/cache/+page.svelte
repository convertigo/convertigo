<script>
	import Card from '$lib/admin/components/Card.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import Cache from '$lib/admin/Cache.svelte';
	import { slide } from 'svelte/transition';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import { onDestroy } from 'svelte';

	let { clear, cancel, configure, conf, confDefault, confOriginal, loading, hasChanged } =
		$derived(Cache);

	onDestroy(Cache.stop);
</script>

<form onsubmit={configure}>
	<fieldset disabled={loading} class="layout-y-stretch">
		<Card title="Cache Type">
			{#snippet cornerOption()}
				<ResponsiveButtons
					buttons={[
						{
							label: 'Clear entries',
							icon: 'mingcute:delete-line',
							cls: 'delete-button',
							onclick: clear
						}
					]}
				/>
			{/snippet}
			<p>Choose the desired cache type</p>
			<div class="layout-x flex-wrap justify-around! w-full">
				<PropertyType
					name="cacheType"
					item={[
						{ text: 'File', value: 'com.twinsoft.convertigo.engine.cache.FileCacheManager' },
						{ text: 'Database', value: 'com.twinsoft.convertigo.engine.cache.DatabaseCacheManager' }
					]}
					type="segment"
					bind:value={conf.cacheType}
					{loading}
					defaultValue={confDefault?.cacheType}
					originalValue={confOriginal?.cacheType}
					fit={true}
				/>
				<ResponsiveButtons
					class="grow h-full"
					buttons={[
						{
							label: 'Apply',
							type: 'submit',
							icon: 'solar:mask-happly-line-duotone',
							cls: 'green-button'
						},
						{
							label: 'Create Table and Apply',
							type: 'submit',
							icon: 'lets-icons:table-light',
							cls: 'basic-button',
							hidden: !conf?.cacheType.endsWith('DatabaseCacheManager')
						},
						{
							label: 'Cancel',
							icon: 'material-symbols-light:cancel-outline',
							cls: 'cancel-button',
							disabled: !hasChanged,
							onclick: cancel
						}
					]}
				/>
			</div>
		</Card>

		{#if conf?.cacheType.endsWith('DatabaseCacheManager')}
			{@const item = [
				{ value: 'sqlserver', text: 'SQLServer' },
				{ value: 'mysql', text: 'MySQL' },
				{ value: 'mariadb', text: 'MariaDB' },
				{ value: 'postgresql', text: 'PostgreSQL' },
				{ value: 'oracle', text: 'Oracle' }
			]}
			<div class="grid gap grid-cols-1 md:grid-cols-2" transition:slide>
				<Card title="Database Used">
					<PropertyType
						name="databaseType"
						{item}
						type="segment"
						bind:value={conf.databaseType}
						orientation="vertical"
						{loading}
						defaultValue={confDefault?.databaseType}
						originalValue={confOriginal?.databaseType}
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
						<Card {title}>
							{#each fields as field}
								<PropertyType
									{...field}
									{loading}
									defaultValue={confDefault[field.name]}
									originalValue={confOriginal[field.name]}
									bind:value={conf[field.name]}
								/>
							{/each}
						</Card>
					{/each}
				{/if}
			</div>
		{/if}
	</fieldset>
</form>
