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
	});

	async function saveChanges() {
		const modalConfirmation = {
			type: 'confirm',
			title: 'Please confirm',
			body: 'Are you sure you want to proceed?',
			response: /** @param {boolean} confirmed */ async (confirmed) => {
				if (confirmed) {
					const currentConfigurations = get(configurations);
					for (const [
						categoryIndex,
						category
					] of currentConfigurations.admin?.category?.entries()) {
						for (const [propertyIndex, property] of category.property?.entries()) {
							if (property['@_value']) {
								await updateConfiguration(categoryIndex, propertyIndex, property['@_value']);
							}
						}
					}
					// @ts-ignore
					modalStoreSaveConfig.trigger({
						title: 'New configurations saved with success'
					});
					hasUnsavedChanges = false;
				} else {
					// we can handle cancellation logic here if needed
				}
			}
		};
		// @ts-ignore
		modalStoreSaveConfig.trigger(modalConfirmation);
	}

	function changeCategory(index) {
		if (hasUnsavedChanges) {
			const confirmLeave = confirm('You have unsaved changes. Are you sure you want to continue?');
			if (confirmLeave) {
				hasUnsavedChanges = false;
				selectedIndex = index;
			}
		} else {
			selectedIndex = index;
		}
	}
</script>

{#if 'admin' in $configurations}
	{@const category = $configurations?.admin?.category[selectedIndex]}
	<div class="flex flex-col grid md:grid-cols-5 gap-5">
		<div class="flex flex-col h-auto md:col-span-4">
			<Card title={category['@_displayName']}>
				<button
					type="button"
					class="btn p-1 pl-5 pr-5 mb-5 w-80 bg-buttons font-normal rounded-full font-medium"
					on:click={saveChanges}
				>
					<span
						><Icon icon="material-symbols-light:save-as-outline" class="w-6 h-6 text-white" /></span
					>
					<span class="text-[13px] text-white">Save changes</span>
				</button>

				<div class="grid md:grid-cols-2 grid-cols-1 gap-5">
					{#each category.property as property, propertyIndex}
						{#if property['@_isAdvanced'] !== 'true'}
							<PropertyType {property} {selectedIndex} {propertyIndex} bind:hasUnsavedChanges />
						{/if}
					{/each}
				</div>
			</Card>

			<Card title="Advanced properties" class="mt-5">
				<Accordion class="dark:border-surface-600 border-[1px] rounded-xl">
					<AccordionItem class="dark:bg-surface-800 bg-white rounded-xl">
						<svelte:fragment slot="lead"
							><Icon icon="game-icons:level-three-advanced" />
						</svelte:fragment>
						<svelte:fragment slot="summary">
							<p>Advanced properties</p>
						</svelte:fragment>
						<svelte:fragment slot="content">
							<div class="md:p-2 flex grid md:grid-cols-2 gap-5">
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
		<Card class="flex flex-col h-auto md:col-span-1 rounded-2xl">
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
		</Card>
	</div>
{/if}

<style lang="postcss">
	.navbutton {
		@apply flex text-[12px] font-light text-start p-2 border-b-[0.5px] border-b-surface-500 dark:bg-surface-800 bg-white items-center;
	}
</style>
