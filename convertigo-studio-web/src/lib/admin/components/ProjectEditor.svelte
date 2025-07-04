<script>
	import { Accordion } from '@skeletonlabs/skeleton-svelte';
	import TreeView from '$lib/common/components/TreeView.svelte';
	import { createDatabaseObjectProperties } from '$lib/common/DatabaseObjectProperties.svelte.js';
	import { createProjectTree } from '$lib/common/ProjectsTree.svelte.js';
	import AutoSvg from '$lib/utils/AutoSvg.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { getUrl } from '$lib/utils/service';
	import { getContext, onMount, untrack } from 'svelte';
	import PropertyType from './PropertyType.svelte';
	import ResponsiveButtons from './ResponsiveButtons.svelte';
	import TableAutoCard from './TableAutoCard.svelte';

	let { project, class: cls = '' } = $props();
	let { rootNode, addProject, checkChildren, onExpandedChange } = $derived(createProjectTree());
	let {
		id,
		properties,
		categories,
		onSelectionChange: onChange,
		hasChanges,
		getChanges,
		save,
		cancel
	} = $derived(createDatabaseObjectProperties());

	let root = $state();
	let openedCategories = $state([]);
	let clickedCategories = $state([]);
	let modalYesNo = getContext('modalYesNo');

	$effect(() => {
		// properties;

		// openedCategories = [];
		// window.setTimeout(() => {
		let open = categories.filter(
			({ category, properties }) => clickedCategories.includes(category) && properties.length > 0
		);
		if (open.length == 0) {
			const some = categories.find(({ properties }) => properties.length > 0);
			open = some ? [some] : [];
		}
		openedCategories = open.map(({ category }) => category);
		// }, 200);
	});

	async function onSelectionChange(e) {
		if (e.selectedValue[0] == id) {
			return;
		}
		if (hasChanges) {
			if (
				!(await modalYesNo.open({
					title: 'You have unsaved changes!',
					message: 'Are you sure you want to continue?'
				}))
			) {
				treeview.setSelectedValue([id]);
				return;
			}
		}
		onChange(e);
		const ry = root.getBoundingClientRect().y - 40;
		root.querySelector('[data-spacer]').style.marginTop = `${Math.max(0, -1 * ry)}px`;
	}

	onMount(() => {
		addProject(project).then(() => {
			checkChildren(rootNode.children[0]);
			onSelectionChange({ selectedValue: [project] });
		});
	});

	async function onNodeIndicator(e, api, nodeState) {
		e.stopPropagation();
		api[nodeState.expanded ? 'collapse' : 'expand'](nodeState.value);
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

	function onSwitchSymbols(row) {
		row.value = `{${project}.${row.name}=${row.value}}`;
		row.symbols = true;
	}

	let treeview = $state();
</script>

<div bind:this={root} class="m-low layout-y items-stretch lg:layout-x lg:items-start {cls}">
	<TreeView
		bind:this={treeview}
		{rootNode}
		{onExpandedChange}
		{onSelectionChange}
		expandOnClick={false}
		defaultExpandedValue={[project]}
		defaultSelectedValue={[project]}
		base="preset-outlined-surface-500 py-low px-[20px] rounded-base overflow-clip"
		classes="text-surface-700-300 min-w-80 overflow-hidden break-all select-none"
		textClass="text-surface-800-200"
		indicatorClass="order-first text-surface-600-400 -ml-[14px]"
	>
		{#snippet nodeIcon({ api, node, nodeState, indexPath })}
			{#if node.icon?.includes('?')}
				<AutoSvg class="h-6 w-6 p-1" fill="currentColor" src="{getUrl()}{node.icon}" alt="ico" />
			{:else if node.icon == 'file'}
				<Ico icon="material-symbols:unknown-document-outline" class="h-6 w-6" />
			{:else if node.icon == 'folder'}
				<Ico icon="material-symbols:folder-outline" class="h-6 w-6" />
			{:else}
				<Ico icon="convertigo:logo" class="h-6 w-6" />
			{/if}
		{/snippet}
		{#snippet nodeText({ node })}
			<span>{node.name}</span>
		{/snippet}
		{#snippet nodeIndicator({ api, nodeState })}
			<button
				onclick={(e) => onNodeIndicator(e, api, nodeState)}
				class:opened={nodeState.expanded}
				class:closed={!nodeState.expanded}>❯</button
			>
		{/snippet}
	</TreeView>
	<div class="layout-y-low w-full items-stretch">
		<span data-spacer class="max-lg:hidden"></span>
		{#snippet saveCancel()}
			<ResponsiveButtons
				class="w-full max-w-100"
				buttons={[
					{
						label: 'Save changes',
						icon: 'material-symbols-light:save-as-outline',
						cls: 'button-success',
						disabled: !hasChanges,
						onclick: async () => {
							const len = getChanges().length;
							if (
								await modalYesNo.open({
									title: 'Do you confirm saving?',
									message: `Are will save ${len} propert${len > 1 ? 'ies' : 'y'}?`
								})
							) {
								await save();
							}
						}
					},
					{
						label: 'Cancel changes',
						icon: 'material-symbols-light:cancel-outline',
						cls: 'button-error',
						disabled: !hasChanges,
						onclick: cancel
					}
				]}
				disabled={properties.length == 0}
			/>
		{/snippet}
		{@render saveCancel()}
		<Accordion
			value={openedCategories}
			onValueChange={(e) => (openedCategories = clickedCategories = e.value)}
			classes="rounded overflow-hidden"
			multiple
			spaceY=""
		>
			{#each categories as { category, properties }}
				<Accordion.Item
					value={category}
					base="even:preset-filled-primary-100-900 odd:preset-filled-primary-200-800"
					panelPadding=""
					panelClasses="bg-surface-100-900"
					controlClasses="font-bold"
					disabled={properties.length == 0}
				>
					{#snippet control()}{category}{/snippet}
					{#snippet panel()}
						<TableAutoCard
							showHeaders={false}
							showNothing={false}
							trClass="preset-filled-surface-50-950 odd:bg-surface-50-950/50 hover:preset-filled-surface-100-900"
							definition={[
								{ key: 'displayName', name: 'Name', class: 'min-w-40' },
								{ key: 'value', name: 'Value', custom: true, class: 'w-full' }
							]}
							animationProps={{ duration: 100 }}
							data={properties}
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
										buttons={['text', 'array'].includes(type)
											? []
											: [
													{
														icon: 'material-symbols:hotel-class-outline',
														title: 'edit symbols',
														onclick: () => onSwitchSymbols(row)
													}
												]}
									/>
								{:else}
									<span class="font-mon max-w-40 break-all">{row.value}</span>
								{/if}
							{/snippet}
						</TableAutoCard>
					{/snippet}
				</Accordion.Item>
			{/each}
		</Accordion>
		{@render saveCancel()}
	</div>
</div>

<style lang="postcss">
	@reference "../../../app.css";

	:global([data-selected] > [data-part='branch-text']) {
		@apply rounded-base preset-filled-primary-100-900 px-low;
	}

	.closed {
		rotate: 0deg;
		transition: rotate 0.2s ease-in-out;
	}

	.opened {
		rotate: 90deg;
		transition: rotate 0.2s ease-in-out;
	}

	:global([data-scope='accordion']) {
		transition: margin-top 0.3s ease-in-out;
	}
</style>
