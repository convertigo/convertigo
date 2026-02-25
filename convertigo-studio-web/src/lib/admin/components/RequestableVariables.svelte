<script>
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

	function parse(row, key = 'value') {
		try {
			row.multipleValues = JSON.parse(row[key]).map((val) => ({ val }));
		} catch {
			row.multipleValues = [];
		}
	}

	$effect(() => {
		for (const variable of requestable.variable) {
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

<TableAutoCard
	showHeaders={false}
	definition={[
		{ name: 'Variable', custom: true, class: 'w-0' },
		{ name: 'Value', custom: true, class: hasComments ? 'min-w-40' : 'w-full min-w-40' },
		{ name: 'Comment', custom: true, class: 'max-w-40' }
	].filter(({ name }) => hasComments || name != 'Comment')}
	{trClass}
	data={requestable.variable}
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
			<div class="max-h-12 overflow-hidden">{row.comment}</div>
		{/if}
	{/snippet}
</TableAutoCard>
