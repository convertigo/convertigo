<script>
	import { beforeNavigate, goto } from '$app/navigation';
	import { page } from '$app/state';
	import Card from '$lib/admin/components/Card.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import SaveCancelButtons from '$lib/admin/components/SaveCancelButtons.svelte';
	import Configuration from '$lib/admin/Configuration.svelte';
	import AccordionGroup from '$lib/common/components/AccordionGroup.svelte';
	import AccordionSection from '$lib/common/components/AccordionSection.svelte';
	import SelectionHighlight from '$lib/common/components/SelectionHighlight.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { getContext, onDestroy } from 'svelte';
	import { fade } from 'svelte/transition';
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
		Last.category = page.params.category ?? '_';
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
		class="layout-y-stretch-none h-full border-l-[0.5px] border-color preset-filled-surface-50-950 max-md:layout-grid-[100px] md:text-right"
	>
		{#each categories as { name, displayName }, i}
			<a
				href="../{name ? name : '_'}/"
				class="relative layout-x-p-low w-full min-w-36 justify-end !gap rounded py-2 shadow-surface-900-100 hover:bg-surface-200-800 hover:shadow-md/10"
			>
				{#if i == selectedIndex}
					<SelectionHighlight delta={selectedIndexLast - selectedIndex} />
				{/if}
				<AutoPlaceholder loading={displayName == null}>
					<span
						class="z-10 w-full text-right text-[13px] font-{i == selectedIndex
							? 'medium'
							: 'normal'}">{displayName}</span
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
				<SaveCancelButtons
					onSave={saveChanges}
					onCancel={refresh}
					changesPending={hasChanges}
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
			<Card class="pt-low">
				<AccordionGroup
					collapsible
					value={Last.advanced}
					onValueChange={(e) => (Last.advanced = e.value)}
				>
					<AccordionSection
						value=""
						surface="bg-transparent border-t border-surface-200-800/40"
						panelPadding="py"
						controlPadding="p-low"
						controlHover="hover:bg-surface-50-950/60 rounded-base"
						panelClasses="bg-transparent"
					>
						{#snippet lead()}<Ico icon="mdi:star-three-points-outline" />{/snippet}
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
					</AccordionSection>
				</AccordionGroup>
			</Card>
		{/if}
	</div>
{/key}
