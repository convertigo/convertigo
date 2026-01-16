<script>
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import SaveCancelButtons from '$lib/admin/components/SaveCancelButtons.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import AccordionGroup from '$lib/common/components/AccordionGroup.svelte';
	import AccordionSection from '$lib/common/components/AccordionSection.svelte';
	import { createDatabaseObjectProperties } from '$lib/common/DatabaseObjectProperties.svelte.js';
	import { selectedId } from '$lib/studio/treeview/treeStore';
	import { onDestroy, onMount, untrack } from 'svelte';

	let openedCategories = $state(/** @type {string[]} */ ([]));
	let clickedCategories = $state(/** @type {string[]} */ ([]));

	let { id, properties, categories, onSelectionChange, hasChanges, save, cancel } = $derived(
		createDatabaseObjectProperties()
	);

	let unsubscribeSelectedId;

	onMount(() => {
		unsubscribeSelectedId = selectedId.subscribe((nextId) => {
			if (!nextId || nextId === id) return;
			void onSelectionChange({ selectedValue: [nextId] });
		});
	});

	onDestroy(() => unsubscribeSelectedId?.());

	const propertyTableDefinition = [
		{
			key: 'displayName',
			name: 'Name',
			class: 'min-w-40 text-xs uppercase text-surface-600-400'
		},
		{ key: 'value', name: 'Value', custom: true, class: 'w-full' }
	];

	function getDefaultOpenedCategories() {
		const open = categories.filter(
			({ category, properties: rows }) => clickedCategories.includes(category) && rows.length > 0
		);
		if (open.length > 0) {
			return open.map(({ category }) => category);
		}
		const first = categories.find(({ properties: rows }) => rows.length > 0);
		return first ? [first.category] : [];
	}

	function getType(row) {
		let { class: cls, value, values } = row;
		if (row.symbols && value != row.originalValue) {
			return 'text';
		} else {
			untrack(() => (row.symbols = false));
		}
		if (values && values.includes(value)) {
			return values.length < 4 ? 'segment' : 'combo';
		} else if (row.isMultiline) {
			return 'array';
		} else if (cls?.endsWith('Boolean')) {
			return 'boolean';
		} else if (cls?.endsWith('Integer') || cls?.endsWith('Long')) {
			return 'number';
		}
		return 'text';
	}
</script>

<div class="layout-y-none h-full min-h-0 items-stretch">
	<div class="pt-low px-low">
		<SaveCancelButtons
			class="w-full"
			onSave={save}
			onCancel={cancel}
			changesPending={hasChanges}
			disabled={properties.length == 0}
		/>
	</div>

	<div class="min-h-0 grow overflow-auto px-low pb-low">
		<AccordionGroup
			class="w-full space-y-3"
			value={openedCategories.length ? openedCategories : getDefaultOpenedCategories()}
			onValueChange={({ value }) => {
				openedCategories = value;
				clickedCategories = value;
			}}
			multiple
		>
			{#each categories as { category, properties: rows } (category)}
				{@const total = rows.length}
				<AccordionSection
					value={category}
					class="w-full overflow-hidden rounded-container border border-surface-200-800 preset-filled-surface-50-950 shadow-follow"
					triggerClass="px py text-left bg-surface-100-900/70 border-b border-surface-200-800 data-[state=open]:bg-surface-100-900/90"
					panelClass="px pb pt-low bg-surface-50-950"
					disabled={total == 0}
					title={category}
					count={total}
				>
					{#snippet panel()}
						{#if total === 0}
							<p
								class="rounded-xl border border-dashed border-surface-300-700/60 bg-surface-100-900/40 px-4 py-6 text-center text-sm text-surface-500"
							>
								No properties available for this section.
							</p>
						{:else}
							<TableAutoCard
								showHeaders={false}
								showNothing={false}
								trClass="border-b border-surface-200-800/70 last:border-0 transition-surface hover:bg-surface-100-900/70"
								definition={propertyTableDefinition}
								animationProps={{ duration: 120 }}
								data={rows}
							>
								{#snippet children({ row })}
									{@const { class: cls, value, originalValue, values } = row}
									{#if category == 'Information'}
										<span>{value}</span>
									{:else if cls?.startsWith('java.lang.')}
										{@const type = getType(row)}
										<PropertyType
											{type}
											bind:value={() => value, (v) => (row.value = v)}
											item={values}
											{originalValue}
											buttons={[]}
										/>
									{:else}
										<span class="font-mon max-w-40 break-all">{row.value}</span>
									{/if}
								{/snippet}
							</TableAutoCard>
						{/if}
					{/snippet}
				</AccordionSection>
			{/each}
		</AccordionGroup>
	</div>
</div>
