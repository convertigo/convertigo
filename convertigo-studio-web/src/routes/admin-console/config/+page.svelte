<script>
	import Icon from '@iconify/svelte';
	import {
		Accordion,
		AccordionItem,
		initializeStores,
		localStorageStore,
		Modal,
		getModalStore
	} from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';

	import {
		refreshConfigurations,
		configurations,
		updateConfiguration
	} from '$lib/admin-console/stores/configurationStore';
	import PropertyType from '$lib/admin-console/admin-components/PropertyType.svelte';
	import Card from '$lib/admin-console/admin-components/Card.svelte';
	import { get } from 'svelte/store';

	initializeStores();

	const modalStoreSaveConfig = getModalStore();

	let theme = localStorageStore('studio.theme', 'skeleton');
	let selectedIndex = 0;
	let hasUnsavedChanges = false;
	let hasChanges;
	let id;

	onMount(() => {
		refreshConfigurations();
		updateConfiguration();
		changeTheme($theme);
		document.body.setAttribute('data-theme', 'dark-theme');
	});

	function changeTheme(e) {
		$theme = typeof e == 'string' ? e : e.target?.value;
		document.body.setAttribute('data-theme', $theme);
	}

	function saveChanges() {
		const successSavingConfig = {
			title: 'New configurations saved with success'
		};
		const faileSavingConfig = {
			title: 'A problem occured while saving'
		};
		const currentConfigurations = get(configurations);

		currentConfigurations.admin.category.forEach((category, categoryIndex) => {
			category.property.forEach((property, propertyIndex) => {
				if (isValid(property['@_value'])) {
					updateConfiguration(categoryIndex, propertyIndex, property['@_value']);
				} else {
					// @ts-ignore
					modalStoreSaveConfig.trigger(faileSavingConfig);
				}
			});
		});
		// @ts-ignore
		modalStoreSaveConfig.trigger(successSavingConfig);
		hasUnsavedChanges = false;
	}

	function changeCategory(index) {
		if (hasUnsavedChanges) {
			const confirmLeave = confirm('You have unsaved changes. Are you sure you want to continue?');
			if (confirmLeave) {
				hasUnsavedChanges = false;
				selectedIndex = index;
			} else {
				selectedIndex = index;
			}
		} else {
			selectedIndex = index;
		}
	}

	function isValid(value) {
		return true;
	}
</script>

<Modal class="text-center" />
{#if 'admin' in $configurations}
	{@const category = $configurations?.admin?.category[selectedIndex]}
	{@debug $configurations}
	<div class="flex flex-col h-full p-10 w-full">
		<div class="flex flex-col grid md:grid-cols-6 gap-10">
			<div class="flex flex-col h-auto md:col-span-5">
				<Card>
					<div class="flex justify-between p-2 items-center border-1 border-b border-surface-100">
						<h1 class="text-[15px]">{category['@_displayName']}</h1>
						<button type="button" class="btn p-1 pl-5 pr-5 bg-surface-600" on:click={saveChanges}>
							<span><Icon icon="material-symbols-light:save-as-outline" class="w-6 h-6" /></span>
							<span class="text-[13px] font-light">Save changes</span>
						</button>
					</div>

					<div class="flex grid md:grid-cols-2 grid-cols-1 gap-5">
						{#each category.property as property, propertyIndex}
							{#if property['@_isAdvanced'] !== 'true'}
								<PropertyType {property} {selectedIndex} {propertyIndex} bind:hasUnsavedChanges />
							{/if}
						{/each}
					</div>
				</Card>

				<div class="mt-10">
					<Card>
						<Accordion>
							<AccordionItem class=" md:w-[90%] bg-surface-900">
								<svelte:fragment slot="lead"
									><Icon icon="game-icons:level-three-advanced" />
								</svelte:fragment>
								<svelte:fragment slot="summary">
									<p>Advanced properties</p>
								</svelte:fragment>
								<svelte:fragment slot="content">
									<div class="bg-surface-900 md:p-2 flex grid md:grid-cols-2">
										{#each category.property as property, propertyIndex}
											{#if property['@_isAdvanced'] == 'true'}
												<PropertyType
													{property}
													{selectedIndex}
													{propertyIndex}
													bind:hasUnsavedChanges
												/>
											{:else}{/if}
										{/each}
									</div>
								</svelte:fragment>
							</AccordionItem>
						</Accordion>
					</Card>
				</div>
			</div>
			<Card>
				<div class="flex flex-col h-auto md:col-span-1 rounded-2xl">
					{#each $configurations?.admin?.category as category, index}
						<button class="navbutton" on:click={() => changeCategory(index)}>
							<Icon icon="uil:arrow-up" rotate={3} class="text-xl mr-2" />
							{category['@_displayName']}
						</button>
					{/each}
				</div>
			</Card>
		</div>
	</div>
{/if}

<style>
	.navbutton {
		@apply flex text-[12px] font-light text-start p-2 border-b-[0.5px] border-b-surface-500 bg-surface-800 items-center;
	}
</style>
