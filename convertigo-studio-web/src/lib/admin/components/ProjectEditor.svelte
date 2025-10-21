<script>
	import AccordionGroup from '$lib/common/components/AccordionGroup.svelte';
	import AccordionSection from '$lib/common/components/AccordionSection.svelte';
	import TreeView from '$lib/common/components/TreeView.svelte';
	import { createDatabaseObjectProperties } from '$lib/common/DatabaseObjectProperties.svelte.js';
	import { createProjectTree } from '$lib/common/ProjectsTree.svelte.js';
	import AutoSvg from '$lib/utils/AutoSvg.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { getUrl } from '$lib/utils/service';
	import { getContext, onMount, tick, untrack } from 'svelte';
	import Button from './Button.svelte';
	import PropertyType from './PropertyType.svelte';
	import SaveCancelButtons from './SaveCancelButtons.svelte';
	import TableAutoCard from './TableAutoCard.svelte';

	let { project, class: cls = '' } = $props();
	let { rootNode, addProject, checkChildren } = $derived(createProjectTree());
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
		addProject(project).then(async () => {
			const [projectNode] = rootNode.children;
			if (projectNode) {
				await checkChildren(projectNode);
				await autoExpandNodes([projectNode]);
			}
			onSelectionChange({ selectedValue: [project] });
		});
	});

	async function ensureTreeviewReady() {
		if (!treeview) {
			await tick();
		}
		return treeview;
	}

	async function expandChain(node, expandedSet, visited) {
		let current = node;
		let mutated = false;
		while (current && !visited.has(current.id)) {
			visited.add(current.id);
			if (!Array.isArray(current.children) || current.children.length !== 1) {
				break;
			}
			const [child] = current.children;
			if (!child || child.id === current.id || !child.children) {
				break;
			}
			const beforeSize = expandedSet.size;
			expandedSet.add(child.id);
			mutated ||= expandedSet.size !== beforeSize;
			if (!Array.isArray(child.children)) {
				await checkChildren(child);
			}
			if (!Array.isArray(child.children) || child.children.length === 0) {
				break;
			}
			current = child;
		}
		return mutated;
	}

	async function autoExpandNodes(nodes = []) {
		if (!nodes.length) return;
		const view = await ensureTreeviewReady();
		if (!view?.getExpandedValue || !view?.setExpandedValue) return;
		const expandedSet = new Set(view.getExpandedValue() ?? []);
		const visited = new Set();
		let mutated = false;
		for (const node of nodes) {
			if (!node) continue;
			mutated ||= await expandChain(node, expandedSet, visited);
		}
		if (mutated) {
			view.setExpandedValue([...expandedSet]);
		}
	}

	async function handleExpandedChange(details) {
		const nodes = details?.expandedNodes ?? [];
		await Promise.all(nodes.map((node) => checkChildren(node)));
		await autoExpandNodes(nodes);
	}

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

	function getDefaultOpenedCategories() {
		const open = categories.filter(
			({ category, properties }) => clickedCategories.includes(category) && properties.length > 0
		);
		if (open.length > 0) {
			return open.map(({ category }) => category);
		}
		const first = categories.find(({ properties }) => properties.length > 0);
		return first ? [first.category] : [];
	}

	const propertyTableDefinition = [
		{
			key: 'displayName',
			name: 'Name',
			class: 'min-w-40 text-xs uppercase text-surface-600-400'
		},
		{ key: 'value', name: 'Value', custom: true, class: 'w-full' }
	];

	let treeview = $state();
</script>

<div bind:this={root} class="m-low layout-y items-stretch lg:layout-x lg:items-start {cls}">
	<TreeView
		bind:this={treeview}
		{rootNode}
		onExpandedChange={handleExpandedChange}
		{onSelectionChange}
		expandOnClick={false}
		defaultExpandedValue={[project]}
		defaultSelectedValue={[project]}
		base="rounded-container preset-filled-surface-50-950 p-3 shadow-follow min-w-fit"
		classes="overflow-hidden break-words select-none"
		textClass="text-sm font-medium"
		indicatorClass="order-first transition-transform duration-200 data-[state=open]:rotate-90"
		childrenClass="border-l border-surface-200-800 pl-2"
		controlClass="layout-x-low rounded-base py-1 transition-colors duration-200 hover:bg-surface-200-800"
	>
		{#snippet nodeIcon({ api, node, nodeState, indexPath })}
			{#if node.icon?.includes('?')}
				<AutoSvg class="h-6 w-6 p-1" fill="currentColor" src="{getUrl()}{node.icon}" alt="ico" />
			{:else if node.icon == 'file'}
				<Ico icon="mdi:file-question-outline" class="h-6 w-6" />
			{:else if node.icon == 'folder'}
				<Ico icon="mdi:folder-outline" class="h-6 w-6" />
			{:else}
				<Ico icon="convertigo:logo" class="h-6 w-6" />
			{/if}
		{/snippet}
		{#snippet nodeText({ node })}
			<span>{node.name}</span>
		{/snippet}
		{#snippet nodeIndicator({ api, nodeState })}
			<Button
				aria-label={nodeState.expanded ? 'Collapse' : 'Expand'}
				onclick={(e) => onNodeIndicator(e, api, nodeState)}
				class="grid size-6 place-items-center"
				icon="mdi:chevron-right"
			/>
		{/snippet}
	</TreeView>
	<div class="layout-y-low w-full items-stretch">
		<span data-spacer class="max-lg:hidden"></span>
		{#snippet saveCancel()}
			<SaveCancelButtons
				class="w-full max-w-100"
				onSave={async () => {
					const len = getChanges().length;
					if (
						await modalYesNo.open({
							title: 'Do you confirm saving?',
							message: `Are will save ${len} propert${len > 1 ? 'ies' : 'y'}?`
						})
					) {
						await save();
					}
				}}
				onCancel={cancel}
				changesPending={hasChanges}
				disabled={properties.length == 0}
			/>
		{/snippet}
		{@render saveCancel()}
		<AccordionGroup
			value={openedCategories.length ? openedCategories : getDefaultOpenedCategories()}
			onValueChange={({ value }) => {
				openedCategories = value;
				clickedCategories = value;
			}}
			multiple
		>
			{#each categories as { category, properties } (category)}
				<AccordionSection
					value={category}
					class="rounded-container preset-filled-surface-50-950 shadow-follow"
					triggerClass="layout-x-low items-center justify-between rounded-container px py-low text-left transition-colors duration-200 hover:bg-surface-200-800"
					panelClass="px-3 pb-4 bg-transparent"
					disabled={properties.length == 0}
				>
					{#snippet control()}
						<div class="layout-x-wrap w-full items-center justify-between">
							<span class="text-sm font-semibold text-surface-900 dark:text-surface-50"
								>{category}</span
							>
							{#if properties.length}
								<span
									class="rounded-full border border-surface-300-700/60 px-2 py-1 text-[11px] font-semibold tracking-wide text-surface-500 uppercase"
									>{properties.length} item{properties.length > 1 ? 's' : ''}</span
								>
							{:else}
								<span
									class="text-surface-500-300 rounded-full border border-dashed border-surface-300-700/60 px-2 py-1 text-[11px] tracking-wide uppercase"
									>Empty</span
								>
							{/if}
						</div>
					{/snippet}
					{#snippet panel()}
						{#if properties.length === 0}
							<p
								class="rounded-xl border border-dashed border-surface-300-700/60 bg-surface-100-900/40 px-4 py-6 text-center text-sm text-surface-500"
							>
								No properties available for this section.
							</p>
						{:else}
							<TableAutoCard
								showHeaders={false}
								showNothing={false}
								trClass="transition-colors duration-150 hover:bg-surface-200-800"
								definition={propertyTableDefinition}
								animationProps={{ duration: 120 }}
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
															icon: 'mdi:star-outline',
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
						{/if}
					{/snippet}
				</AccordionSection>
			{/each}
		</AccordionGroup>
		{@render saveCancel()}
	</div>
</div>

<style lang="postcss">
	@reference "../../../app.css";

	:global([data-part='branch-control'][data-selected]),
	:global([data-part='item'][data-selected]) {
		@apply preset-filled-primary-200-800 pr-2;
	}

	:global([data-part='branch-control'][data-selected] [data-part='branch-text']),
	:global([data-part='item'][data-selected] [data-part='item-text']) {
		@apply font-semibold;
	}

	:global([data-scope='accordion']) {
		transition: margin-top 0.3s ease-in-out;
	}
</style>
