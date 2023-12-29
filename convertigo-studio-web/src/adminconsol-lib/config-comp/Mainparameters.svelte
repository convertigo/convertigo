<script>
	import { onMount } from 'svelte';
	import { fetchMainParameters } from '../stores/Store';
	import { updateServerSetting } from '../stores/Store';

	let convertigoServerLocalUrl = '';
	let convertigoServerEndpointUrl = '';
	let maximumNumberOfWorkerThreads = '';
	let MaximumNumberOfContexts = '';
	let gitContainer = '';
	let xsrfAdminEnabled = false;
	let xsrfApiEnabled = false;

	onMount(async () => {
		try {
			const response = await fetchMainParameters();
			const mainParamsCategory = response.admin.category.find(
				(cat) => cat['@_displayName'] === 'Main parameters'
			);

			const convertigoUrlProperty = mainParamsCategory.property.find(
				(prop) => prop['@_name'] === 'APPLICATION_SERVER_CONVERTIGO_URL'
			);
			convertigoServerLocalUrl = convertigoUrlProperty['@_value'];

			const convertigoEndpointProperty = mainParamsCategory.property.find(
				(prop) => prop['@_name'] === 'APPLICATION_SERVER_CONVERTIGO_ENDPOINT'
			);
			convertigoServerEndpointUrl = convertigoEndpointProperty['@_value'];

			const maxWorkerThreadsProperty = mainParamsCategory.property.find(
				(prop) => prop['@_name'] === 'DOCUMENT_THREADING_MAX_WORKER_THREADS'
			);
			maximumNumberOfWorkerThreads = maxWorkerThreadsProperty['@_value'];

			const convertigoMaxContextsProperty = mainParamsCategory.property.find(
				(prop) => prop['@_name'] === 'CONVERTIGO_MAX_CONTEXTS'
			);
			MaximumNumberOfContexts = convertigoMaxContextsProperty['@_value'];

			const gitContainerProperty = mainParamsCategory.property.find(
				(prop) => prop['@_name'] === 'GIT_CONTAINER'
			);
			gitContainer = gitContainerProperty['@_value'];

			const xsrfAdminProperty = mainParamsCategory.property.find(
				(prop) => prop['@_name'] === 'XSRF_ADMIN'
			);
			xsrfAdminEnabled = xsrfAdminProperty['@_value'] === '';

			const xsrfApiProperty = mainParamsCategory.property.find(
				(prop) => prop['@_name'] === 'XSRF_API'
			);
			xsrfApiEnabled = xsrfApiProperty['@_value'] === '';
		} catch (error) {
			console.error('Error fetching Convertigo Server:', error);
		}
	});

	// La fonction appelée lorsque l'utilisateur modifie l'URL et sort du champ (on:blur)
	async function handleUpdateUrl() {
		const success = await updateServerSetting(
			'APPLICATION_SERVER_CONVERTIGO_URL',
			convertigoServerLocalUrl
		);
		if (success) {
			console.log('URL mise à jour avec succès.');
		} else {
			console.log("Échec de la mise à jour de l'URL.");
		}
	}

	async function handleUpdateEndpointUrl() {
		const success = await updateServerSetting(
			'APPLICATION_SERVER_CONVERTIGO_ENDPOINT',
			convertigoServerEndpointUrl
		);
		if (success) {
			console.log('Endpoint URL updated successfully.');
		} else {
			console.log('Failed to update Endpoint URL.');
		}
	}

	async function handleUpdateMaxWorkerThreads() {
		const success = await updateServerSetting(
			'DOCUMENT_THREADING_MAX_WORKER_THREADS',
			maximumNumberOfWorkerThreads
		);
		if (success) {
			console.log('Max worker threads updated successfully.');
		} else {
			console.log('Failed to update Max worker threads.');
		}
	}

	async function handleUpdateMaxContexts() {
		const success = await updateServerSetting('CONVERTIGO_MAX_CONTEXTS', MaximumNumberOfContexts);
		if (success) {
			console.log('Max contexts updated successfully.');
		} else {
			console.log('Failed to update Max contexts.');
		}
	}

	async function handleUpdateGitContainer() {
		const success = await updateServerSetting('GIT_CONTAINER', gitContainer);
		if (success) {
			console.log('Git container updated successfully.');
		} else {
			console.log('Failed to update Git container.');
		}
	}

	async function updateXsrfSetting(settingKey, settingValue) {
		const success = await updateServerSetting(settingKey, settingValue.toString());
		if (success) {
			console.log(`${settingKey} updated successfully.`);
			if (settingKey === 'XSRF_ADMIN') {
				xsrfAdminEnabled = settingValue;
                console.log("success xsrf admin")
			} else if (settingKey === 'XSRF_API') {
				xsrfApiEnabled = settingValue;
                console.log("success xsrf api")
			}
		} else {
			console.log(`Failed to update ${settingKey}.`);
		}
	}

	function toggleXsrfAdmin() {
		updateXsrfSetting('XSRF_ADMIN', !xsrfAdminEnabled);
	}

	function toggleXsrfApi() {
		updateXsrfSetting('XSRF_API', !xsrfApiEnabled);
	}
</script>

<div>
	<h1 class="text-[15px]">Main parameters</h1>
	<h2 class="mt-5 text-[14px]">Convertigo Server Local URL</h2>
	<input
		type="text"
		class="text-black w-[60%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
		placeholder="server local url"
		bind:value={convertigoServerLocalUrl}
		on:blur={handleUpdateUrl}
	/>

	<h2 class="mt-5 text-[14px]">Convertigo Server End point URL</h2>
	<input
		type="text"
		class="text-black w-[60%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
		placeholder="server endpoint url"
		bind:value={convertigoServerEndpointUrl}
		on:blur={handleUpdateEndpointUrl}
	/>

	<h2 class="mt-5 text-[14px]">Maximum number of worker threads</h2>
	<input
		type="text"
		class="text-black w-[60%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
		placeholder="Maximum number of worker threads"
		bind:value={maximumNumberOfWorkerThreads}
		on:blur={handleUpdateMaxWorkerThreads}
	/>

	<h2 class="mt-5 text-[14px]">Maximum number of contexts</h2>
	<input
		type="text"
		class="text-black w-[60%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
		placeholder="Maximum number of contexts"
		bind:value={MaximumNumberOfContexts}
		on:blur={handleUpdateMaxContexts}
	/>

	<h2 class="mt-5 text-[14px]">Maximum number of contexts</h2>
	<input
		type="text"
		class="text-black w-[60%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
		placeholder="Git container path"
		bind:value={gitContainer}
		on:blur={handleUpdateGitContainer}
	/>

	<label class="mt-10 items-center flex">
		<input type="checkbox" bind:checked={xsrfAdminEnabled} on:click={toggleXsrfAdmin} />
		<p class="ml-5">Enable XSRF protection for Administration Console</p>
	</label>

	<label class="mt-5 flex items-center">
		<input type="checkbox" bind:checked={xsrfApiEnabled} on:click={toggleXsrfApi} />
		<p class="ml-5">Enable XSRF protection for projects</p>
	</label>
</div>
