<script>
	import { Accordion } from '@skeletonlabs/skeleton-svelte';
	import { onDestroy, tick } from 'svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import { browser } from '$app/environment';
	import Ico from '$lib/utils/Ico.svelte';
	import { fade, fly } from 'svelte/transition';
	import RightPart from '../RightPart.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import Configuration from '$lib/admin/Configuration.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import ModalYesNo from '$lib/common/components/ModalYesNo.svelte';

	let selectedIndex = $state(0);
	let selectedIndexLast = $state(-1);
	let advanced = $state([]);
	let category = $derived(Configuration.categories[selectedIndex] ?? {});

	RightPart.snippet = rightPart;
	onDestroy(() => {
		RightPart.snippet = undefined;
	});

	$effect(() => {
		if (browser) {
			if (selectedIndex != selectedIndexLast) {
				if (category?.['@_name']) {
					tick().then(() => {
						location.hash = category['@_name'];
					});
				}
			} else {
				const name = location.hash.substring(1);
				selectedIndex = Math.max(
					0,
					Configuration.categories.findIndex(
						(/** @type {{ [x: string]: string; }} */ c) => c['@_name'] == name
					)
				);
			}
		}
	});

	async function saveChanges() {
		const toSave = Configuration.categories[selectedIndex]?.property
			?.filter((/** @type {{ [x: string]: any; }} */ p) => p['@_value'] != p['@_originalValue'])
			.map((/** @type {{ [x: string]: any; }} */ p) => ({
				'@_key': p['@_name'],
				'@_value': p['@_value']
			}));
		const confirmed = await modalSave.open({
			title: `Are you sure you want to save ${toSave.length} propert${toSave.length == 1 ? 'y' : 'ies'}?`
		});
		if (confirmed) {
			Configuration.updateConfigurations(toSave);
		}
	}

	async function changeCategory() {
		if (hasChanges) {
			if (await modalUnsaved.open()) {
				Configuration.refresh();
			} else {
				return false;
			}
		}
		return true;
	}

	let hasChanges = $derived(
		Configuration.categories[selectedIndex]?.property?.some(
			(/** @type {{ [x: string]: any; }} */ p) => p['@_value'] != p['@_originalValue']
		)
	);

	let modalSave;
	let modalUnsaved;
</script>

{#snippet rightPart()}
	<nav
		class="bg-surface-200-800 border-r-[0.5px] border-color p-low h-full max-md:layout-grid-[100px]"
	>
		{#each Configuration.categories as category, i}
			<a
				href="#{category['@_name']}"
				class="relative layout-x-p-low !gap py-2 hover:bg-surface-200-800 rounded min-w-36"
				onclick={async () => {
					if (await changeCategory()) {
						selectedIndexLast = selectedIndex;
						selectedIndex = i;
					}
				}}
			>
				{#if i == selectedIndex}
					<span
						in:fly={{ y: (selectedIndexLast - selectedIndex) * 50 }}
						out:fade
						class="absolute inset-0 preset-filled-primary-500 opacity-40 rounded"
					></span>
				{/if}
				<AutoPlaceholder loading={category['@_displayName'] == null}>
					<span class="text-[13px] z-10 font-{i == selectedIndex ? 'medium' : 'light'}"
						>{category['@_displayName']}</span
					>
				</AutoPlaceholder>
			</a>
		{/each}
	</nav>
{/snippet}

<ModalYesNo bind:this={modalSave} />
<ModalYesNo
	bind:this={modalUnsaved}
	title="You have unsaved changes!"
	message="Are you sure you want to continue?"
/>

{#key selectedIndex}
	<div class="layout-y !items-stretch" in:fade>
		<Card title={category['@_displayName']}>
			{#snippet cornerOption()}
				<ResponsiveButtons
					buttons={[
						{
							label: 'Save changes',
							icon: 'material-symbols-light:save-as-outline',
							cls: 'basic-button',
							disabled: !hasChanges,
							onclick: saveChanges
						},
						{
							label: 'Cancel changes',
							icon: 'material-symbols-light:cancel-outline',
							cls: 'yellow-button',
							disabled: !hasChanges,
							onclick: Configuration.refresh
						}
					]}
				/>
			{/snippet}

			<div class="w-full layout-cols-2">
				{#each category.property as property}
					{#if property['@_isAdvanced'] != 'true'}
						<PropertyType {property} />
					{/if}
				{/each}
			</div>
		</Card>

		{#if category.property?.filter((/** @type {{ [x: string]: string; }} */ p) => p['@_isAdvanced'] == 'true').length > 0}
			<Card>
				<Accordion collapsible bind:value={advanced}>
					<Accordion.Item value="" panelPadding="py" controlPadding="">
						{#snippet lead()}<Ico icon="game-icons:level-three-advanced" />{/snippet}
						{#snippet control()}Advanced Properties{/snippet}
						{#snippet panel()}
							<div class="w-full layout-cols-2">
								{#each category.property as property}
									{#if property['@_isAdvanced'] == 'true'}
										<PropertyType {property} />
									{/if}
								{/each}
							</div>
						{/snippet}
					</Accordion.Item>
				</Accordion>
			</Card>
		{/if}
	</div>
{/key}
