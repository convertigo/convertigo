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
			console.error('Error fetching Convertigo Server local URL:', error);
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

	function toggleXsrfAdmin() {
		xsrfAdminEnabled = !xsrfAdminEnabled;
	}

	function toggleXsrfApi() {
		xsrfApiEnabled = !xsrfApiEnabled;
	}
</script>

<div>
	<h2 class="mt-5">Convertigo Server Local URL</h2>
	<input
		type="text"
		class="text-black w-80 mt-2"
		placeholder="server local url"
		bind:value={convertigoServerLocalUrl}
		on:blur={handleUpdateUrl}
	/>

	<h2 class="mt-5">Convertigo Server End point URL</h2>
	<input
		type="text"
		class="text-black w-80 mt-2"
		placeholder="server endpoint url"
		value={convertigoServerEndpointUrl}
	/>

	<h2 class="mt-5">Maximum number of worker threads</h2>
	<input
		type="text"
		class="text-black w-80 mt-2"
		placeholder="Maximum number of worker threads"
		value={maximumNumberOfWorkerThreads}
	/>

	<h2 class="mt-5">Maximum number of contexts</h2>
	<input
		type="text"
		class="text-black w-80 mt-2"
		placeholder="Maximum number of contexts"
		value={MaximumNumberOfContexts}
	/>

	<h2 class="mt-5">Maximum number of contexts</h2>
	<input
		type="text"
		class="text-black w-80 mt-2"
		placeholder="Maximum number of contexts"
		value={gitContainer}
	/>

	<label class="mt-10">
		<input type="checkbox" bind:checked={xsrfAdminEnabled} on:click={toggleXsrfAdmin} />
		Enable XSRF protection for Administration Console
	</label>

	<label class="mt-5">
		<input type="checkbox" bind:checked={xsrfApiEnabled} on:click={toggleXsrfApi} />
		Enable XSRF protection for projects
	</label>
</div>
