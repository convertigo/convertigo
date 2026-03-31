<script>
	import AccordionGroup from '$lib/common/components/AccordionGroup.svelte';
	import AccordionSection from '$lib/common/components/AccordionSection.svelte';
	import { flip } from 'svelte/animate';
	import { fly } from 'svelte/transition';
	import Button from './Button.svelte';
	import PropertyType from './PropertyType.svelte';
	import TableAutoCard from './TableAutoCard.svelte';

	let { requestable = $bindable(), trClass = undefined } = $props();
	let testcase = $derived(
		requestable.tc
			? requestable.tc.variable.reduce((acc, val) => {
					acc[val.name] = val.value;
					return acc;
				}, {})
			: {}
	);
	let hasComments = $derived.by(() =>
		requestable.variable?.some(({ comment }) => String(comment ?? '').trim().length > 0)
	);
	let selectedCount = $derived.by(
		() => requestable.variable?.filter(({ send }) => send == 'true').length ?? 0
	);
	let allSelected = $derived.by(
		() =>
			Boolean(requestable.variable?.length) &&
			requestable.variable.every(({ send }) => send == 'true')
	);

	function parse(row, key = 'value') {
		try {
			row.multipleValues = JSON.parse(row[key]).map((val) => ({ val }));
		} catch {
			row.multipleValues = [];
		}
	}

	function setAll(checked) {
		for (const variable of requestable.variable ?? []) {
			variable.send = checked ? 'true' : 'false';
		}
	}

	$effect(() => {
		if (requestable.variablesOpened == null) {
			requestable.variablesOpened = (requestable.variable?.length ?? 0) <= 6;
		}
		for (const variable of requestable.variable) {
			if (variable.send != 'true' && variable.send != 'false') {
				variable.send = variable.send ? 'true' : 'false';
			}
			if (variable.name in testcase) {
				variable.val = testcase[variable.name];
				variable.send = 'true';
			} else {
				variable.val = variable.value;
			}
			if (variable.isMultivalued == 'true') {
				try {
					variable.multipleValues = JSON.parse(variable.val).map((val) => ({ val }));
				} catch {
					variable.multipleValues = [];
				}
			}
		}
	});
</script>

{#snippet property({ row, obj, defaultValue = undefined })}
	<PropertyType
		type={row.isFileUpload == 'true' ? 'file' : row.isMasked == 'true' ? 'password' : 'text'}
		bind:value={() => obj.val ?? '', (v) => (obj.val = v)}
		{defaultValue}
		name={row.name}
		actionsHorizontal={true}
		onfocus={() => (row.send = 'true')}
	/>
{/snippet}

<AccordionGroup
	collapsible
	value={requestable.variablesOpened ? ['variables'] : []}
	onValueChange={({ value }) => {
		requestable.variablesOpened = (value ?? []).includes('variables');
	}}
	class="rounded-md border border-surface-200-800/40 bg-surface-50-950/45"
>
	<AccordionSection
		value="variables"
		class="rounded-md"
		title={`Variables (${requestable.variable?.length ?? 0})`}
		trailingText={`${selectedCount} selected`}
		triggerClass="w-full rounded-md px-2.5 py-2"
		panelClass="bg-transparent px-2.5 pb-2 pt-0"
	>
		{#snippet panel()}
			<div class="space-y-2">
				{#if (requestable.variable?.length ?? 0) > 1}
					<div class="flex justify-start pl-1">
						<PropertyType
							type="check"
							fit={true}
							label="Send all"
							checked={allSelected}
							onCheckedChange={(event) => setAll(event.checked)}
						/>
					</div>
				{/if}
				<TableAutoCard
					showHeaders={false}
					definition={[
						{ name: 'Variable', custom: true, class: 'w-0' },
						{ name: 'Value', custom: true, class: hasComments ? 'min-w-40' : 'w-full min-w-40' },
						{ name: 'Comment', custom: true, class: 'max-w-40' }
					].filter(({ name }) => hasComments || name != 'Comment')}
					{trClass}
					data={requestable.variable}
					class="-mx-1"
				>
					{#snippet children({ row, def })}
						{#if def.name == 'Variable'}
							<PropertyType
								type="boolean"
								bind:value={row.send}
								fit={true}
								label={row.name}
								tooltip="Send value"
								class="font-medium"
							/>
						{:else if def.name == 'Value'}
							<div class:opacity-50={row.send == 'false'}>
								{#if row.isMultivalued == 'true'}
									<div class="layout-x-low">
										<Button
											full={false}
											type="button"
											icon="mdi:plus"
											title="Add value"
											class="button-ico-primary h-7 w-7 min-w-7 justify-center p-0!"
											onclick={() => row.multipleValues?.push({ val: '' })}
										/>
										<Button
											full={false}
											type="button"
											icon="mdi:backup-restore"
											title="Restore list"
											class="button-ico-primary h-7 w-7 min-w-7 justify-center p-0!"
											onclick={() => parse(row)}
										/>
									</div>
									{#each row.multipleValues ?? [] as obj, i (obj)}
										<div
											class="layout-x-low"
											animate:flip={{ duration: 200 }}
											transition:fly={{ duration: 200, y: -50 }}
										>
											{@render property({ row, obj })}
											<Button
												full={false}
												type="button"
												icon="mdi:delete-outline"
												title="Delete value"
												class="button-ico-primary h-7 w-7 min-w-7 justify-center p-0!"
												onclick={() => row.multipleValues?.splice(i, 1)}
											/>
										</div>
									{/each}
								{:else}
									{@render property({ row, obj: row, defaultValue: row.value })}
								{/if}
							</div>
						{:else if def.name == 'Comment'}
							<button
								type="button"
								class="w-full cursor-pointer text-left"
								title={row.commentExpanded ? 'Click to collapse' : 'Click to expand'}
								aria-expanded={row.commentExpanded}
								onclick={() => (row.commentExpanded = !row.commentExpanded)}
							>
								<div
									class="overflow-hidden text-xs break-words whitespace-pre-wrap text-surface-600-400 transition-[max-height] duration-180 ease-out"
									style:max-height={row.commentExpanded
										? `${Math.max(row.commentElement?.scrollHeight ?? 0, 48)}px`
										: '3rem'}
									bind:this={row.commentElement}
								>
									{row.comment}
								</div>
							</button>
						{/if}
					{/snippet}
				</TableAutoCard>
			</div>
		{/snippet}
	</AccordionSection>
</AccordionGroup>
