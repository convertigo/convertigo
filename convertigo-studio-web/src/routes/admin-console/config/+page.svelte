<script>
	import Icon from '@iconify/svelte';
	import {
		Accordion,
		AccordionItem,
		ListBox,
		ListBoxItem,
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

	const modalStoreSaveConfig = getModalStore();

	let selectedIndex = 0;
	let hasUnsavedChanges = false;

	onMount(() => {
		refreshConfigurations();
		updateConfiguration();
	});

	function saveChanges() {
		const successSavingConfig = {
			title: 'New configurations saved with success'
		};
		const failedSavingConfig = {
			title: 'A problem occured while saving'
		};

		const currentConfigurations = get(configurations);
		// for the moment it's not working. We have to implement the modal in the following logic
		// Modal to ask confirmation to update config data's
		const modalConfirmation = {
			type: 'confirm',
			title: 'Please confirm',
			body: 'Are your sure you want to proceed ?',
			response: (confirmed) => {
				if (confirmed) {
					console.log('config updated');
				}
			}
		};

		currentConfigurations.admin.category.forEach((category, categoryIndex) => {
			category.property.forEach((property, propertyIndex) => {
				if (isValid(property['@_value'])) {
					updateConfiguration(categoryIndex, propertyIndex, property['@_value']);
				} else {
					// @ts-ignore
					modalStoreSaveConfig.trigger(failedSavingConfig);
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

{#if 'admin' in $configurations}
	{@const category = $configurations?.admin?.category[selectedIndex]}
	{@debug $configurations}
	<div class="flex flex-col grid md:grid-cols-5 gap-5">
		<div class="flex flex-col h-auto md:col-span-4">
			<Card title={category['@_displayName']}>
				<button
					type="button"
					class="btn p-1 pl-5 pr-5 mb-5 w-80 variant-filled"
					on:click={saveChanges}
				>
					<span><Icon icon="material-symbols-light:save-as-outline" class="w-6 h-6" /></span>
					<span class="text-[13px] font-light">Save changes</span>
				</button>

				<div class="flex grid md:grid-cols-2 grid-cols-1 gap-5">
					{#each category.property as property, propertyIndex}
						{#if property['@_isAdvanced'] !== 'true'}
							<PropertyType {property} {selectedIndex} {propertyIndex} bind:hasUnsavedChanges />
						{/if}
					{/each}
				</div>
			</Card>

			<div class="mt-10">
				<Card title="Advanced properties">
					<Accordion>
						<AccordionItem class="dark:bg-surface-800 bg-white rounded-xl">
							<svelte:fragment slot="lead"
								><Icon icon="game-icons:level-three-advanced" />
							</svelte:fragment>
							<svelte:fragment slot="summary">
								<p>Advanced properties</p>
							</svelte:fragment>
							<svelte:fragment slot="content">
								<div class="md:p-2 flex grid md:grid-cols-2 gap-10">
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
				<ListBox active="dark:bg-surface-600 bg-surface-50">
					{#each $configurations?.admin?.category as category, index}
						<ListBoxItem bind:group={selectedIndex} name="category" value={index} class="flex">
							<div class="flex">
								<Icon icon="uil:arrow-up" rotate={3} class="text-xl mr-2" />
								{category['@_displayName']}
							</div>
						</ListBoxItem>
					{/each}
				</ListBox>
			</div>
		</Card>
	</div>
{/if}

<style lang="postcss">
	.navbutton {
		@apply flex text-[12px] font-light text-start p-2 border-b-[0.5px] border-b-surface-500 dark:bg-surface-800 bg-white items-center;
	}
</style>
