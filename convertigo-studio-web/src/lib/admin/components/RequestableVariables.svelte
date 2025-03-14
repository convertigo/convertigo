<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { untrack } from 'svelte';
	import { flip } from 'svelte/animate';
	import { fly } from 'svelte/transition';
	import PropertyType from './PropertyType.svelte';
	import TableAutoCard from './TableAutoCard.svelte';

	let { requestable = $bindable() } = $props();
	let multiples = $state({});
	let testcase = $derived(
		requestable.tc
			? requestable.tc.variable.reduce((acc, val) => {
					acc[val.name] = val.value;
					return acc;
				}, {})
			: {}
	);

	function parse(row, key = 'value') {
		try {
			multiples[row.name] = JSON.parse(row[key]).map((val) => ({ val }));
		} catch (e) {
			multiples[row.name] = [];
		}
	}

	$effect(() => {
		multiples = {};
		for (const variable of requestable.variable) {
			if (variable.name in testcase) {
				variable.val = testcase[variable.name];
				variable.send = 'true';
			} else {
				variable.val = variable.value;
			}
			if (variable.isMultivalued == 'true') {
				untrack(() => parse(variable, 'val'));
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
		onfocus={() => (row.send = 'true')}
	/>
{/snippet}

<TableAutoCard
	showHeaders={false}
	definition={[
		{ name: 'Variable', custom: true, class: 'w-0' },
		{ name: 'Value', custom: true, class: 'min-w-40' },
		{ name: 'Comment', custom: true, class: 'max-w-40' }
	]}
	data={requestable.variable}
>
	{#snippet children({ row, def })}
		{#if def.name == 'Variable'}
			<PropertyType
				type="boolean"
				bind:value={row.send}
				fit={true}
				label={row.name}
				class="font-bold"
			/>
		{:else if def.name == 'Value'}
			<div class:opacity-50={row.send == 'false'}>
				{#if row.isMultivalued == 'true'}
					<div class="layout-x-low">
						<button
							type="button"
							class="btn w-fit! bg-surface-200-800 btn-sm"
							onclick={() => multiples[row.name].push({ val: '' })}
							><Ico icon="grommet-icons:add" /></button
						><button type="button" onclick={() => parse(row)} class="btn bg-surface-200-800 btn-sm">
							<Ico icon="mdi:backup-restore" />
						</button>
					</div>
					{#each multiples[row.name] as obj, i (obj)}
						<div
							class="layout-x-low"
							animate:flip={{ duration: 200 }}
							transition:fly={{ duration: 200, y: -50 }}
						>
							{@render property({ row, obj })}
							<button
								type="button"
								class="btn w-fit! bg-surface-200-800 btn-sm"
								onclick={() => multiples[row.name].splice(i, 1)}
								><Ico icon="mingcute:delete-line" /></button
							>
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
