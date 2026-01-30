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
	import { fromAction } from 'svelte/attachments';
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
	const attachRoot = $derived(fromAction(setRoot));

	/** @param {HTMLDivElement} node */
	function setRoot(node) {
		root = node;
		return {
			destroy() {
				if (root === node) root = undefined;
			}
		};
	}

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
		if (!view?.setExpandedValue) return;
		const expandedSet = new Set(view.expandedValue ?? []);
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
			class: 'min-w-40 text-xs text-right text-surface-600-400'
		},
		{ key: 'value', name: 'Value', custom: true, class: 'w-full' }
	];

	let treeview = $state();
</script>

<div class="m-low layout-y items-stretch lg:layout-x lg:items-start {cls}" {@attach attachRoot}>
	<TreeView
		bind:apiRef={treeview}
		{rootNode}
		onExpandedChange={handleExpandedChange}
		{onSelectionChange}
		expandOnClick={false}
		defaultExpandedValue={[project]}
		defaultSelectedValue={[project]}
		base="rounded-container border border-color preset-filled-surface-100-900 p-3 shadow-follow min-w-0 max-w-full w-full lg:flex-[0_1_35%] lg:max-w-[42%]"
		classes="overflow-hidden break-words select-none"
		textClass="text-sm font-medium text-strong"
		indicatorClass="order-first transition-transform duration-200 data-[state=open]:rotate-90"
		childrenClass="border-l border-color pl-3"
		controlClass="layout-x-low w-full items-center gap-2 rounded-base px-2 py-1 transition-soft hover:bg-surface-200-800"
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
	<div class="mb-low layout-y-low min-w-0 flex-1 items-stretch">
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
			class="space-y-3"
			value={openedCategories.length ? openedCategories : getDefaultOpenedCategories()}
			onValueChange={({ value }) => {
				openedCategories = value;
				clickedCategories = value;
			}}
			multiple
		>
			{#each categories as { category, properties } (category)}
				{@const total = properties.length}
				<AccordionSection
					value={category}
					class="overflow-hidden rounded-container border border-color preset-filled-surface-100-900 shadow-follow"
					triggerClass="px py text-left bg-surface-100-900 border-b border-color data-[state=open]:bg-surface-100-900"
					panelClass="px pb pt-low bg-surface-100-900"
					disabled={total == 0}
					title={category}
					count={total}
				>
					{#snippet panel()}
						{#if total === 0}
							<p
								class="rounded-xl border border-dashed border-surface-300-700/60 bg-surface-100-900/40 px-4 py-6 text-center text-sm text-muted"
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
								data={properties}
								class="table-no-frame"
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

	:global([data-scope='accordion']) {
		transition: margin-top 0.3s ease-in-out;
	}

	:global(.table-no-frame .table-frame) {
		border: 0;
		border-radius: 0;
	}
</style>
