<script>
	import { onMount } from 'svelte';
	import { fetchMainParameters, updateServerSetting } from '../stores/Store';

	let cacheManagerClass = '';
	let cacheManagerClassDisplayName = 'Cache Manager Class';
	let fileCacheDirectory = '';
	let fileCacheDirectoryDisplayName = 'File Cache Directory';
	let cacheScanDelay = '';
	let cacheScanDelayDisplayName = 'Cache Scan Delay';

	onMount(async () => {
		try {
			const response = await fetchMainParameters();
			const cacheCategory = response.admin.category.find((cat) => cat['@_displayName'] === 'Cache');

			// Assuming 'property' is an array of objects with '@_name' and '@_value' keys.
			cacheCategory.property.forEach((prop) => {
				if (prop['@_name'] === 'CACHE_MANAGER_CLASS') {
					cacheManagerClass = prop['@_value'];
				} else if (prop['@_name'] === 'CACHE_MANAGER_FILECACHE_DIRECTORY') {
					fileCacheDirectory = prop['@_value'];
				} else if (prop['@_name'] === 'CACHE_MANAGER_SCAN_DELAY') {
					cacheScanDelay = prop['@_value'];
				}
			});
		} catch (error) {
			console.error('Error fetching Convertigo Server:', error);
		}
	});

	async function handleUpdateSetting(settingKey, value) {
		const success = await updateServerSetting(settingKey, value);
		if (success) {
			console.log(`${settingKey} updated successfully.`);
		} else {
			console.log(`Failed to update ${settingKey}.`);
		}
	}
</script>

<h1 class="text-[15px]">Cache</h1>

<h2 class="mt-5 text-[14px]">{cacheManagerClassDisplayName}</h2>
<input
	type="text"
	class="text-black w-[60%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
	placeholder="Admin Username"
	bind:value={cacheManagerClass}
	on:blur={() => handleUpdateSetting('CACHE_MANAGER_CLASS', cacheManagerClass)}
/>

<h2 class="mt-5 text-[14px]">{fileCacheDirectoryDisplayName}</h2>
<input
	type="text"
	class="text-black w-[60%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
	placeholder="Admin Username"
	bind:value={fileCacheDirectory}
	on:blur={() => handleUpdateSetting('CACHE_MANAGER_FILECACHE_DIRECTORY', fileCacheDirectory)}
/>

<h2 class="mt-5 text-[14px]">{cacheScanDelayDisplayName}</h2>
<input
	type="text"
	class="text-black w-[60%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
	placeholder="Admin Username"
	bind:value={cacheScanDelay}
	on:blur={() => handleUpdateSetting('CACHE_MANAGER_SCAN_DELAY', cacheScanDelay)}
/>
