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
		updateConfigurations
	} from '$lib/admin/stores/configurationStore';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import { browser } from '$app/environment';
	import Ico from '$lib/utils/Ico.svelte';
	import ResponsiveContainer from '$lib/admin/components/ResponsiveContainer.svelte';

	const modalStore = getModalStore();

	let selectedIndex = -1;

	onMount(() => {
		const name = window.location.hash.substring(1);
		refreshConfigurations().then(() => {
			selectedIndex = Math.max(
				0,
				$configurations?.admin?.category?.findIndex(
					(/** @type {{ [x: string]: string; }} */ c) => c['@_name'] == name
				)
			);
		});
	});

	function saveChanges() {
		const toSave = $configurations?.admin?.category?.[selectedIndex]?.property
			?.filter((/** @type {{ [x: string]: any; }} */ p) => p['@_value'] != p['@_originalValue'])
			.map((/** @type {{ [x: string]: any; }} */ p) => ({
				'@_key': p['@_name'],
				'@_value': p['@_value']
			}));
		modalStore.trigger({
			type: 'component',
			component: 'modalWarning',
			meta: { mode: 'Confirm' },
			title: `Are you sure you want to save ${toSave.length} propert${toSave.length == 1 ? 'y' : 'ies'}?`,
			response: /** @param {boolean} confirmed */ async (confirmed) => {
				if (confirmed) {
					updateConfigurations(toSave);
				}
			}
		});
	}

	/**
	 * @param { any } event
	 */
	async function changeCategory(event) {
		if (hasChanges) {
			event.preventDefault();
			const confirm = await new Promise((resolve) => {
				modalStore.trigger({
					type: 'component',
					component: 'modalWarning',
					meta: { mode: 'Confirm' },
					title: 'You have unsaved changes',
					body: ' Are you sure you want to continue ?',
					response: (confirmed) => {
						if (confirmed) {
							resolve(confirmed);
						}
					}
				});
			});
			if (confirm) {
				await refreshConfigurations();
				selectedIndex = event.target?.value;
			}
		}
	}

	$: hasChanges = $configurations?.admin?.category?.[selectedIndex]?.property?.some(
		(/** @type {{ [x: string]: any; }} */ p) => p['@_value'] != p['@_originalValue']
	);

	$: if (browser && 'admin' in $configurations) {
		window.location.hash = $configurations?.admin?.category?.[selectedIndex]?.['@_name'];
	}
</script>

{#if selectedIndex > -1}
	{@const category = $configurations?.admin?.category[selectedIndex]}
	<div class="grid md:grid-cols-5 gap-5">
		<div class="h-auto md:col-span-4">
			<Card title={category['@_displayName']}>
				<div slot="cornerOption" class="flex flex-wrap gap-5 mb-10">
					<div class="flex-1">
						<button
							type="button"
							disabled={!hasChanges}
							class="bg-primary-400-500-token w-full"
							on:click={saveChanges}
						>
							<span><Ico icon="material-symbols-light:save-as-outline" class="w-6 h-6" /></span>
							<span>Save changes</span>
						</button>
					</div>

					<div class="flex-1">
						<button
							type="button"
							disabled={!hasChanges}
							class="bg-tertiary-400-500-token w-full"
							on:click={refreshConfigurations}
						>
							<span><Ico icon="material-symbols-light:cancel-outline" class="w-6 h-6" /></span>
							<span class="">Cancel changes</span>
						</button>
					</div>
				</div>

				<ResponsiveContainer
					scrollable={false}
					maxHeight="h-auto"
					smCols="sm:grid-cols-1"
					mdCols="md:grid-cols-1"
					lgCols="lg:grid-cols-2"
				>
					{#each category.property as property}
						{#if property['@_isAdvanced'] != 'true'}
							<PropertyType {property} />
						{/if}
					{/each}
				</ResponsiveContainer>
			</Card>

			{#if category.property.filter((/** @type {{ [x: string]: string; }} */ p) => p['@_isAdvanced'] == 'true').length > 0}
				<Card class="mt-5">
					<Accordion caretOpen="rotate-0" caretClosed="-rotate-90">
						<AccordionItem>
							<svelte:fragment slot="summary">
								<div class="flex items-center">
									<Icon icon="game-icons:level-three-advanced" />
									<p class="ml-4">Advanced Properties</p>
								</div>
							</svelte:fragment>

							<svelte:fragment slot="content">
								<ResponsiveContainer
									scrollable={false}
									maxHeight="h-auto"
									class="mt-10"
									smCols="sm:grid-cols-1"
									mdCols="md:grid-cols-1"
									lgCols="lg:grid-cols-2"
								>
									{#each category.property as property}
										{#if property['@_isAdvanced'] == 'true'}
											<PropertyType {property} />
										{/if}
									{/each}
								</ResponsiveContainer>
							</svelte:fragment>
						</AccordionItem>
					</Accordion>
				</Card>
			{/if}
		</div>
		<Card class="flex flex-col h-auto md:col-span-1 rounded-2xl">
			<ListBox active="dark:bg-surface-600 bg-surface-50">
				{#each $configurations?.admin?.category as category, index}
					<ListBoxItem
						bind:group={selectedIndex}
						name="category"
						value={index}
						class="flex"
						on:click={changeCategory}
					>
						<div class="flex font-light">
							{category['@_displayName']}
						</div>
					</ListBoxItem>
				{/each}
			</ListBox>
		</Card>
	</div>
{:else}
	<div class="flex flex-col grid md:grid-cols-5 gap-5">
		<div class="flex flex-col h-auto md:col-span-4">
			<Card>
				<div class="text-xl mb-5 font-bold placeholder animate-pulse"></div>
				<div class="flex flex-row space-x-5">
					<button
						type="button"
						disabled={true}
						class="btn p-1 pl-5 pr-5 mb-5 w-80 font-normal rounded-full font-medium"
					>
						<span
							><Icon
								icon="material-symbols-light:save-as-outline"
								class="w-6 h-6 text-white"
							/></span
						>
						<span class="text-[13px] text-white">Save Changes</span>
					</button>
					<button
						type="button"
						disabled={true}
						class="btn p-1 pl-5 pr-5 mb-5 w-80 variant-filled-error font-normal rounded-full font-medium"
					>
						<span
							><Icon
								icon="material-symbols-light:cancel-outline"
								class="w-6 h-6 text-white"
							/></span
						>
						<span class="text-[13px] text-white">Cancel Changes</span>
					</button>
				</div>
				<div class="grid md:grid-cols-2 grid-cols-1 gap-5">
					{#each Array(10) as _}
						<div class="my-2 h-8 placeholder animate-pulse"></div>
					{/each}
				</div>
			</Card>

			<Card title="Advanced properties" class="mt-5">
				<Accordion class="rounded-token">
					<AccordionItem class="rounded-token">
						<svelte:fragment slot="lead"
							><Ico icon="game-icons:level-three-advanced" />
						</svelte:fragment>
						<svelte:fragment slot="summary">
							<p>Advanced properties</p>
						</svelte:fragment>
					</AccordionItem>
				</Accordion>
			</Card>
		</div>
		<Card class="flex flex-col h-auto md:col-span-1 rounded-token">
			{#each Array(12) as _}
				<div class="placeholder animate-pulse my-3 h-8"></div>
			{/each}
		</Card>
	</div>
{/if}
