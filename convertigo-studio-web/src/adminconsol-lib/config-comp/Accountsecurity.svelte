<script>
	import { onMount } from 'svelte';
	import { fetchMainParameters, updateServerSetting } from '../stores/Store';
	import { Accordion, AccordionItem } from '@skeletonlabs/skeleton';

	let adminUsername = '';
	let adminPassword = '';
	let testPlatformUsername = '';
	let testPlatformPassword = '';
	let advancedProperties = '';
	let securityFilter = false;

	onMount(async () => {
		try {
			const response = await fetchMainParameters();
			const mainParamsCategory = response.admin.category.find(
				(cat) => cat['@_displayName'] === 'Accounts and security'
			);

			const adminUsernameProperty = mainParamsCategory.property.find(
				(prop) => prop['@_name'] === 'ADMIN_USERNAME'
			);
			adminUsername = adminUsernameProperty['@_value'];

			const adminPasswordProperty = mainParamsCategory.property.find(
				(prop) => prop['@_name'] === 'ADMIN_PASSWORD'
			);
			adminPassword = adminPasswordProperty['@_value'];

			const testPlatformUsernameProperty = mainParamsCategory.property.find(
				(prop) => prop['@_name'] === 'TEST_PLATFORM_USERNAME'
			);
			testPlatformUsername = testPlatformUsernameProperty['@_value'];

			const testPlatformPasswordProperty = mainParamsCategory.property.find(
				(prop) => prop['@_name'] === 'TEST_PLATFORM_PASSWORD'
			);
			testPlatformPassword = testPlatformPasswordProperty['@_value'];

			const advancedPropertiesProperty = mainParamsCategory.property.find(
				(prop) => prop['@_name'] === 'USER_PASSWORD_REGEX'
			);
			advancedProperties = advancedPropertiesProperty['@_value'];

			const securityFilterProperty = mainParamsCategory.property.find(
				(prop) => prop['@_name'] === 'SECURITY_FILTER'
			);
			securityFilter = securityFilterProperty['@_value'] === '';
		} catch (error) {
			console.error('Error fetching Convertigo Server:', error);
		}
	});

	async function handleUpdateAdminUsername() {
		const success = await updateServerSetting('ADMIN_USERNAME', adminUsername);
		if (success) {
			console.log('admin username mise à jour avec succés');
		} else {
			console.log('Echec de la mise à jour de admin username');
		}
	}

	async function handleUpdateAdminPassword() {
		const success = await updateServerSetting('ADMIN_PASSWORD', adminPassword);
		if (success) {
			console.log('admin username mise à jour avec succés');
		} else {
			console.log('Echec de la mise à jour de admin username');
		}
	}

	async function handleUpdateTestPlatformUsername() {
		const success = await updateServerSetting('TEST_PLATFORM_USERNAME', testPlatformUsername);
		if (success) {
			console.log('test platform username mise à jour avec succés');
		} else {
			console.log('Echec de la mise à jour de test platform username');
		}
	}

	async function handleUpdateTestPlatformPassword() {
		const success = await updateServerSetting('TEST_PLATFORM_PASSWORD', testPlatformPassword);
		if (success) {
			console.log('test platform Password mise à jour avec succés');
		} else {
			console.log('Echec de la mise à jour de test platform Password');
		}
	}

	async function handleUpdateAdvancedProperties() {
		const success = await updateServerSetting('USER_PASSWORD_REGEX', advancedProperties);
		if (success) {
			console.log('advanced properties mise à jour avec succés');
		} else {
			console.log('Echec de la mise à jour de advanced properties');
		}
	}

	async function updateSecurityFilter(settingKey, settingValue) {
		const success = await updateServerSetting(settingKey, settingValue.toString());
		if (success) {
			console.log(`${settingKey} updated successfully.`);
			if (settingKey === 'SECURITY_FILTER') {
				securityFilter = settingValue;
				console.log('success security filter');
			} else {
				console.log(`Failed to update ${settingKey}.`);
			}
		}
	}

	function togglesecurityFilter() {
		updateSecurityFilter('SECURITY_FILTER', !securityFilter);
	}
</script>

<div>
	<h1 class="text-[15px]">Accounts and security</h1>

	<h2 class="mt-5 text-[14px]">Admin Username</h2>
	<input
		type="text"
		class="text-black w-[60%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
		placeholder="Admin Username"
		bind:value={adminUsername}
		on:blur={handleUpdateAdminUsername}
	/>

	<h2 class="mt-5 text-[14px]">Admin Password</h2>
	<input
		type="text"
		class="text-black w-[60%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
		placeholder="admin password"
		bind:value={adminPassword}
		on:blur={handleUpdateAdminPassword}
	/>

	<h2 class="mt-5 text-[14px]">Test Platform Username</h2>
	<input
		type="text"
		class="text-black w-[60%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
		placeholder="Test Platform Username"
		bind:value={testPlatformUsername}
		on:blur={handleUpdateTestPlatformUsername}
	/>

	<h2 class="mt-5 text-[14px]">Test Platform Password</h2>
	<input
		type="text"
		class="text-black w-[60%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
		placeholder="Test Platform Platform"
		bind:value={testPlatformPassword}
		on:blur={handleUpdateTestPlatformPassword}
	/>

	<Accordion width="w-[60%] mt-10 bg-surface-500">
		<AccordionItem open>
			<svelte:fragment slot="summary">Advanced Properties</svelte:fragment>
			<svelte:fragment slot="content">
				<label class="mt-2">
					<input type="checkbox" bind:checked={securityFilter} on:click={togglesecurityFilter} />
					Security filter
				</label>

				<h2 class="mt-5 text-[14px]">
					RegularExpression used to validate password change for Admin accounts
				</h2>
				<input
					type="text"
					class="text-black w-[100%] mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
					placeholder="Test Platform"
					bind:value={advancedProperties}
					on:blur={handleUpdateAdvancedProperties}
				/>
			</svelte:fragment>
		</AccordionItem>
	</Accordion>
</div>
