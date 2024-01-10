<script>
	import { onMount } from 'svelte';
	import { fetchMainParameters, updateServerSetting } from '../stores/Store';

	const settingsKeys = {
		convertigoServerLocalUrl: 'APPLICATION_SERVER_CONVERTIGO_URL',
		convertigoServerEndpointUrl: 'APPLICATION_SERVER_CONVERTIGO_ENDPOINT',
		maximumNumberOfWorkerThreads: 'DOCUMENT_THREADING_MAX_WORKER_THREADS',
		MaximumNumberOfContexts: 'CONVERTIGO_MAX_CONTEXTS',
		gitContainer: 'GIT_CONTAINER',
		xsrfAdminEnabled: 'XSRF_ADMIN',
		xsrfApiEnabled: 'XSRF_API'
	};

	let settings = Object.fromEntries(
		Object.keys(settingsKeys).map((key) => [key, key.includes('xsrf') ? false : ''])
	);

	onMount(async () => {
		try {
			const response = await fetchMainParameters();
			const mainParamsCategory = response.admin.category.find(
				(cat) => cat['@_displayName'] === 'Main parameters'
			);

			mainParamsCategory.property.forEach((prop) => {
				const key = Object.keys(settingsKeys).find((key) => settingsKeys[key] === prop['@_name']);
				if (key) {
					settings[key] = prop['@_value'];
				}
			});
		} catch (error) {
			console.error('Error fetching Convertigo Server:', error);
		}
	});

	async function handleUpdateSetting(key) {
		const success = await updateServerSetting(settingsKeys[key], settings[key]);
		if (success) {
			console.log(`${key} updated successfully.`);
		} else {
			console.log(`Failed to update ${key}.`);
		}
	}

	function toggleXsrfSetting(key) {
		settings[key] = !settings[key];
		handleUpdateSetting(key);
	}
</script>

<div>
	<h1 class="text-[15px]">Main parameters</h1>
	{#each Object.entries(settings) as [key, value]}
		<h2 class="mt-5 text-[14px]">{key}</h2>
		{#if typeof value === 'string'}
			<input
				type="text"
				class="text-black w-[60%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
				placeholder={key}
				bind:value={settings[key]}
				on:blur={() => handleUpdateSetting(key)}
			/>
		{:else}
			<label class="mt-10 items-center flex">
				<input
					type="checkbox"
					bind:checked={settings[key]}
					on:click={() => toggleXsrfSetting(key)}
				/>
				<p class="ml-5">
					Enable XSRF protection for {key === 'xsrfAdminEnabled'
						? 'Administration Console'
						: 'projects'}
				</p>
			</label>
		{/if}
	{/each}
</div>
