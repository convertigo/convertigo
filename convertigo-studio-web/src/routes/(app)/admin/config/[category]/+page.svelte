<script>
	import { Accordion } from '@skeletonlabs/skeleton-svelte';
	import { beforeNavigate, goto } from '$app/navigation';
	import { page } from '$app/state';
	import Card from '$lib/admin/components/Card.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import Configuration from '$lib/admin/Configuration.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { getContext, onDestroy } from 'svelte';
	import { fade, fly } from 'svelte/transition';
	import RightPart from '../../RightPart.svelte';
	import Last from '../Last.svelte';

	let { categories, refresh, updateConfigurations, init } = $derived(Configuration);

	let selectedIndex = $derived(
		Math.max(
			0,
			categories.findIndex(({ name }) => name == page.params.category)
		)
	);
	let selectedIndexLast = $state(-1);
	let category = $derived(categories[selectedIndex] ?? {});

	RightPart.snippet = rightPart;
	onDestroy(() => {
		RightPart.snippet = undefined;
		Configuration.stop();
	});

	beforeNavigate(async (nav) => {
		if (nav.type == 'goto') {
			return;
		}
		if (hasChanges) {
			nav.cancel();
			if (
				await modalYesNo.open({
					title: 'You have unsaved changes!',
					message: 'Are you sure you want to continue?'
				})
			) {
				refresh();
				goto(nav.to?.url ?? '');
			} else {
				return;
			}
		}
		selectedIndexLast = selectedIndex;
	});

	$effect(() => {
		Last.category = page.params.category;
	});

	async function saveChanges(event) {
		const toSave = categories[selectedIndex]?.property
			?.filter(({ value, originalValue }) => value != originalValue)
			.map(({ name, value }) => ({
				'@_key': name,
				'@_value': value
			}));
		const confirmed = await modalYesNo.open({
			event,
			title: `Are you sure you want to save ${toSave.length} propert${toSave.length == 1 ? 'y' : 'ies'}?`
		});
		if (confirmed) {
			updateConfigurations(toSave);
		}
	}

	let hasChanges = $derived(
		categories[selectedIndex]?.property?.some(({ value, originalValue }) => value != originalValue)
	);

	let modalYesNo = getContext('modalYesNo');
</script>

{#snippet rightPart()}
	<nav
		class="h-full border-r-[0.5px] border-color preset-filled-surface-50-950 p-low max-md:layout-grid-[100px]"
	>
		{#each categories as { name, displayName }, i}
			<a
				href="../{name ? name : '_'}/"
				class="relative layout-x-p-low min-w-36 gap! rounded-sm py-2 shadow-surface-900-100 hover:bg-surface-200-800 hover:shadow-md/20"
			>
				{#if i == selectedIndex}
					<span
						in:fly={{ y: (selectedIndexLast - selectedIndex) * 50 }}
						out:fade
						class="absolute inset-0 rounded-sm preset-filled-primary-500 opacity-40 shadow-md/50 shadow-primary-900-100 hover:shadow-md/80"
					></span>
				{/if}
				<AutoPlaceholder loading={displayName == null}>
					<span class="z-10 text-[13px] font-{i == selectedIndex ? 'medium' : 'light'}"
						>{displayName}</span
					>
				</AutoPlaceholder>
			</a>
		{/each}
	</nav>
{/snippet}

{#key selectedIndex}
	<div class="layout-y-stretch" in:fade>
		<Card title={category.displayName}>
			{#snippet cornerOption()}
				<ResponsiveButtons
					buttons={[
						{
							label: 'Save changes',
							icon: 'material-symbols-light:save-as-outline',
							cls: 'button-success',
							disabled: !hasChanges,
							onclick: saveChanges
						},
						{
							label: 'Cancel changes',
							icon: 'material-symbols-light:cancel-outline',
							cls: 'button-error',
							disabled: !hasChanges,
							onclick: refresh
						}
					]}
					disabled={!init}
				/>
			{/snippet}

			<div class="layout-cols-2 w-full">
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
				<Accordion
					collapsible
					value={Last.advanced}
					onValueChange={(e) => (Last.advanced = e.value)}
				>
					<Accordion.Item value="" panelPadding="py" controlPadding="">
						{#snippet lead()}<Ico icon="game-icons:level-three-advanced" />{/snippet}
						{#snippet control()}Advanced Properties{/snippet}
						{#snippet panel()}
							<div class="layout-cols-2 w-full">
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
