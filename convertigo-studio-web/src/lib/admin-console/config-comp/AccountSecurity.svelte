<script>
	import { onMount } from 'svelte';
	import { fetchMainParameters, updateServerSetting } from '../stores/Store';

	const settingsKeys = {
		adminUsername: 'ADMIN_USERNAME',
		adminPassword: 'ADMIN_PASSWORD',
		testPlatformUsername: 'TEST_PLATFORM_USERNAME',
		testPlatformPassword: 'TEST_PLATFORM_PASSWORD',
		advancedProperties: 'USER_PASSWORD_REGEX',
		securityFilter: 'SECURITY_FILTER'
	};

	let settings = Object.fromEntries(
		Object.keys(settingsKeys).map((key) => [key, key === 'securityFilter' ? false : ''])
	);

	onMount(async () => {
		try {
			const response = await fetchMainParameters();
			const securityParamsCategory = response.admin.category.find(
				(cat) => cat['@_displayName'] === 'Accounts and security'
			);

			securityParamsCategory.property.forEach((prop) => {
				const key = Object.keys(settingsKeys).find((key) => settingsKeys[key] === prop['@_name']);
				if (key) {
					settings[key] = key === 'securityFilter' ? prop['@_value'] === 'true' : prop['@_value'];
				}
			});
		} catch (error) {
			console.error('Error fetching Convertigo Server:', error);
		}
	});

	async function handleUpdateSetting(settingKey) {
		const value = settings[settingKey];
		const success = await updateServerSetting(settingsKeys[settingKey], value);
		if (success) {
			console.log(`${settingKey} updated successfully.`);
		} else {
			console.log(`Failed to update ${settingKey}.`);
		}
	}

	function toggleSetting(key) {
		settings[key] = !settings[key];
		handleUpdateSetting(key);
	}
</script>

<div>
	<h1 class="text-[15px]">Accounts and security</h1>
	{#each Object.entries(settings) as [key, value]}
		{#if typeof value === 'boolean'}
			<label class="mt-5">
				<input type="checkbox" bind:checked={value} on:click={() => toggleSetting(key)} />
				{key}
			</label>
		{:else}
			<div class="mt-5">
				<label>{key}</label>
				<input
					type="text"
					class="text-black w-[60%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
					placeholder={key}
					bind:value
					on:blur={() => handleUpdateSetting(key)}
				/>
			</div>
		{/if}
	{/each}
</div>
