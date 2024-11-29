<script>
	import { Accordion } from '@skeletonlabs/skeleton-svelte';
	import { onDestroy } from 'svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { fade, fly } from 'svelte/transition';
	import RightPart from '../../RightPart.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import Configuration from '$lib/admin/Configuration.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import ModalYesNo from '$lib/common/components/ModalYesNo.svelte';
	import { page } from '$app/stores';
	import { beforeNavigate, goto } from '$app/navigation';
	import Last from '../Last.svelte';

	let selectedIndex = $derived(
		Math.max(
			0,
			Configuration.categories.findIndex(({ name }) => name == $page.params.category)
		)
	);
	let selectedIndexLast = $state(-1);
	let category = $derived(Configuration.categories[selectedIndex] ?? {});

	RightPart.snippet = rightPart;
	onDestroy(() => {
		RightPart.snippet = undefined;
	});

	beforeNavigate(async (nav) => {
		if (nav.type == 'goto') {
			return;
		}
		if (hasChanges) {
			nav.cancel();
			if (await modalUnsaved.open()) {
				Configuration.refresh();
				goto(nav.to?.url ?? '');
			} else {
				return;
			}
		}
		selectedIndexLast = selectedIndex;
	});

	$effect(() => {
		Last.category = $page.params.category;
	});

	async function saveChanges(event) {
		const toSave = Configuration.categories[selectedIndex]?.property
			?.filter(({ value, originalValue }) => value != originalValue)
			.map(({ name, value }) => ({
				'@_key': name,
				'@_value': value
			}));
		const confirmed = await modalSave.open({
			event,
			title: `Are you sure you want to save ${toSave.length} propert${toSave.length == 1 ? 'y' : 'ies'}?`
		});
		if (confirmed) {
			Configuration.updateConfigurations(toSave);
		}
	}

	let hasChanges = $derived(
		Configuration.categories[selectedIndex]?.property?.some(
			({ value, originalValue }) => value != originalValue
		)
	);

	let modalSave;
	let modalUnsaved;
</script>

{#snippet rightPart()}
	<nav
		class="bg-surface-200-800 border-r-[0.5px] border-color p-low h-full max-md:layout-grid-[100px]"
	>
		{#each Configuration.categories as { name, displayName }, i}
			<a
				href="../{name ? name : '_'}/"
				class="relative layout-x-p-low !gap py-2 hover:bg-surface-200-800 rounded min-w-36"
			>
				{#if i == selectedIndex}
					<span
						in:fly={{ y: (selectedIndexLast - selectedIndex) * 50 }}
						out:fade
						class="absolute inset-0 preset-filled-primary-500 opacity-40 rounded"
					></span>
				{/if}
				<AutoPlaceholder loading={name == null}>
					<span class="text-[13px] z-10 font-{i == selectedIndex ? 'medium' : 'light'}"
						>{displayName}</span
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
		<Card title={category.displayName}>
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
					{#if property.isAdvanced != 'true'}
						<PropertyType
							{...property}
							bind:value={property.value}
							loading={property.description == null}
						/>
					{/if}
				{/each}
			</div>
		</Card>

		{#if category.property?.filter(({ isAdvanced }) => isAdvanced == 'true').length > 0}
			<Card>
				<Accordion collapsible bind:value={Last.advanced}>
					<Accordion.Item value="" panelPadding="py" controlPadding="">
						{#snippet lead()}<Ico icon="game-icons:level-three-advanced" />{/snippet}
						{#snippet control()}Advanced Properties{/snippet}
						{#snippet panel()}
							<div class="w-full layout-cols-2">
								{#each category.property as property}
									{#if property.isAdvanced == 'true'}
										<PropertyType
											{...property}
											bind:value={property.value}
											loading={property.description == null}
										/>
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
