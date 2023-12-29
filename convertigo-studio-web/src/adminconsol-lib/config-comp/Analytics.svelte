<script>
	import { onMount } from 'svelte';
	import { fetchMainParameters, updateServerSetting } from '../stores/Store';
	import { Accordion, AccordionItem } from '@skeletonlabs/skeleton';

	let enablePersistenceAnalitcs = false;
	let enablePersistenceAnalitcsDisplayName = '';
	let enableGoogleAnalytics = false;
	let enableGoogleAnalyticsDisplayName = '';

	let persistenceSQLDialect = '';
	let persistenceSQLDialectDisplayName = '';
	let persistenceJDBCdriver = '';
	let persistenceJDBCdriverDisplayName = '';
	let persistenceJDBCpassword = '';
	let persistenceJDBCpasswordDisplayName = '';
	let persistenceJDBCURL = '';
	let persistenceJDBCURLDisplayName = '';
	let persistenceJDBCusername = '';
	let persistenceJDBCusernameDisplayName = '';
	let JDBCmaxRetryConnectionFailed = '';
	let JDBCmaxRetryConnectionFailedDisplayName = '';
	let googleAnalyticsID = '';
	let GoogleAnalyticsIDDisplayName = '';

	onMount(async () => {
		try {
			const response = await fetchMainParameters();
			const mainParamsCategory = response.admin.category.find(
				(cat) => cat['@_displayName'] === 'Analytics'
			);

			const enablePersistenceAnalitcsProperty = mainParamsCategory.property.find(
				(prop) => prop['@_name'] === 'ANALYTICS_PERSISTENCE_ENABLED'
			);
			enablePersistenceAnalitcs = enablePersistenceAnalitcsProperty['@_value'] === '';
			enablePersistenceAnalitcsDisplayName = enablePersistenceAnalitcsProperty['@_description'];

			const enableGoogleAnalyticsProperty = mainParamsCategory.property.find(
				(prop) => prop['@_name'] === 'ANALYTICS_GOOGLE_ENABLED'
			);
			enableGoogleAnalytics = enableGoogleAnalyticsProperty['@_value'] === '';
			enableGoogleAnalyticsDisplayName = enableGoogleAnalyticsProperty['@_description'];

			const persistenceSQLDialectProperty = mainParamsCategory.property.find(
				(prop) => prop['@_name'] === 'ANALYTICS_PERSISTENCE_DIALECT'
			);
			persistenceSQLDialect = persistenceSQLDialectProperty['@_value'];
			persistenceSQLDialectDisplayName = persistenceSQLDialectProperty['@_description'];
		} catch (error) {
			console.error('Error fetching Convertigo Server:', error);
		}
	});

	async function updateEnableAnalitcs(settingKey, settingValue) {
		const success = await updateServerSetting(settingKey, settingValue.toString());
		if (success) {
			console.log(`${settingKey} updated succesflully.`);
			if (settingKey === 'ANALYTICS_PERSISTENCE_ENABLED') {
				enablePersistenceAnalitcs = settingValue;
				console.log('success ANALYTICS_PERSISTENCE');
				if (settingKey === 'ANALYTICS_GOOGLE_ENABLED') {
					enableGoogleAnalytics = settingValue;
					console.log('success ANALYTICS_GOOGLE');
				}
			} else {
				console.log(`failed to update ${settingKey}.`);
			}
		}
	}

	function toggleEnablePersistenceAnalitcs() {
		updateEnableAnalitcs('ANALYTICS_PERSISTENCE_ENABLED', !enablePersistenceAnalitcs);
	}

	function toggleEnableGoogleAnalytics() {
		updateEnableAnalitcs('ANALYTICS_GOOGLE_ENABLED', !enableGoogleAnalytics);
	}

	async function handleUpdatepersistenceSQLDialect() {
		const success = await updateServerSetting(
			'ANALYTICS_PERSISTENCE_DIALECT',
			persistenceSQLDialect
		);
		if (success) {
			console.log('persistenceSQLDialect mise à jour avec succés');
		} else {
			console.log('Echec de la mise à jour de persistenceSQLDialect');
		}
	}
</script>

<div class="">
	<h1 class="text-[15px]">Analytics</h1>

	<label class="mt-10 w-[100%] flex items-center">
		<input
			type="checkbox"
			bind:checked={enablePersistenceAnalitcs}
			on:click={toggleEnablePersistenceAnalitcs}
		/>
		<p class="ml-5 text-[14px]">{enablePersistenceAnalitcsDisplayName}</p>
	</label>

	<label class="mt-2 w-[100%] flex items-center">
		<input
			type="checkbox"
			bind:checked={enableGoogleAnalytics}
			on:click={toggleEnableGoogleAnalytics}
		/>
		<p class="ml-5 text-[14px]">{enableGoogleAnalyticsDisplayName}</p>
	</label>

	<Accordion width="w-[100%] mt-10 bg-surface-700">
		<AccordionItem open >
			<svelte:fragment slot="summary">Advanced Properties</svelte:fragment>
			<svelte:fragment slot="content">
				<div class="flex flex-col grid grid-cols-2 gap-20 pb-5">
					<div>
						<h2 class="mt-5 text-[14px]">{persistenceSQLDialectDisplayName}</h2>
						<input
							type="text"
							class="text-black w-[100%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
							placeholder="admin password"
							bind:value={persistenceSQLDialect}
							on:blur={handleUpdatepersistenceSQLDialect}
						/>

						<h2 class="mt-5 text-[14px]">Test Platform Username</h2>
						<input
							type="text"
							class="text-black w-[100%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
							placeholder="Test Platform Username"
						/>

						<h2 class="mt-5 text-[14px]">Test Platform Password</h2>
						<input
							type="text"
							class="text-black w-[100%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
							placeholder="Test Platform Platform"
						/>

						<h2 class="mt-5 text-[14px]">Test Platform Password</h2>
						<input
							type="text"
							class="text-black w-[100%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
							placeholder="Test Platform Platform"
						/>
					</div>

                    <div>
                        <h2 class="mt-5 text-[14px]">Test Platform Password</h2>
                        <input
                            type="text"
                            class="text-black w-[100%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
                            placeholder="Test Platform Platform"
                        />
                        <h2 class="mt-5 text-[14px]">Test Platform Password</h2>
                        <input
                            type="text"
                            class="text-black w-[100%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
                            placeholder="Test Platform Platform"
                        />
                        <h2 class="mt-5 text-[14px]">Test Platform Password</h2>
                        <input
                            type="text"
                            class="text-black w-[100%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
                            placeholder="Test Platform Platform"
                        />
                    </div>

					
				</div>
			</svelte:fragment>
		</AccordionItem>
	</Accordion>
</div>
