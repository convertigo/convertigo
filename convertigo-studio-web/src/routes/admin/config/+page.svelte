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

	const modalStore = getModalStore();

	let selectedIndex = -1;

	onMount(() => {
		const name = window.location.hash.substring(1);
		refreshConfigurations().then(() => {
			console.log(name);
			selectedIndex = Math.max(
				0,
				$configurations?.admin?.category?.findIndex(
					(/** @type {{ [x: string]: string; }} */ c) => c['@_name'] == name
				)
			);
		});
		refreshConfigurations();
	});

	function saveChanges() {
		const toSave = $configurations?.admin?.category?.[selectedIndex]?.property
			?.filter((/** @type {{ [x: string]: any; }} */ p) => p['@_value'] != p['@_originalValue'])
			.map((/** @type {{ [x: string]: any; }} */ p) => ({
				'@_key': p['@_name'],
				'@_value': p['@_value']
			}));
		modalStore.trigger({
			type: 'confirm',
			title: 'Please confirm',
			body: `Are you sure you want to save ${toSave.length} propert${toSave.length == 1 ? 'y' : 'ies'}?`,
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
					type: 'confirm',
					title: 'Please Confirm',
					body: 'You have unsaved changes. Are you sure you want to continue?',
					response: (r) => {
						resolve(r);
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
				<div class="flex flex-row justify-start gap-10 flex-wrap">
					<button
						type="button"
						disabled={!hasChanges}
						class="btn p-1 pl-5 pr-5 w-80 bg-buttons font-normal font-medium mb-5"
						on:click={saveChanges}
					>
						<span
							><Ico
								icon="material-symbols-light:save-as-outline"
								class="w-6 h-6 text-white"
							/></span
						>
						<span class="text-[13px] text-white">Save changes</span>
					</button>
					<button
						type="button"
						disabled={!hasChanges}
						class="btn p-1 pl-5 pr-5 w-80 variant-filled-error font-normal font-medium mb-5"
						on:click={refreshConfigurations}
					>
						<span
							><Ico icon="material-symbols-light:cancel-outline" class="w-6 h-6 text-white" /></span
						>
						<span class="text-[13px] text-white">Cancel changes</span>
					</button>
				</div>
				<div class="grid md:grid-cols-2 grid-cols-1 gap-5">
					{#each category.property as property}
						{#if property['@_isAdvanced'] != 'true'}
							<PropertyType {property} />
						{/if}
					{/each}
				</div>
			</Card>

			{#if category.property.filter((p) => p['@_isAdvanced'] == 'true').length > 0}
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
									{#each category.property as property}
										{#if property['@_isAdvanced'] == 'true'}
											<PropertyType {property} />
										{/if}
									{/each}
								</div>
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
						<div class="flex">
							<Icon icon="uil:arrow-up" rotate={3} class="text-xl mr-2" />
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
						class="btn p-1 pl-5 pr-5 mb-5 w-80 bg-buttons font-normal rounded-full font-medium"
					>
						<span
							><Icon
								icon="material-symbols-light:save-as-outline"
								class="w-6 h-6 text-white"
							/></span
						>
						<span class="text-[13px] text-white">Save changes</span>
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
						<span class="text-[13px] text-white">Cancel changes</span>
					</button>
				</div>
				<div class="grid md:grid-cols-2 grid-cols-1 gap-5">
					{#each Array(10) as _}
						<div class="my-2 h-8 placeholder animate-pulse"></div>
					{/each}
				</div>
			</Card>

			<Card title="Advanced properties" class="mt-5">
				<Accordion class="dark:border-surface-600 border-[1px] rounded-xl">
					<AccordionItem class="dark:bg-surface-800 bg-white rounded-xl">
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
		<Card class="flex flex-col h-auto md:col-span-1 rounded-2xl">
			{#each Array(12) as _}
				<div class="placeholder animate-pulse my-3 h-8"></div>
			{/each}
		</Card>
	</div>
{/if}
