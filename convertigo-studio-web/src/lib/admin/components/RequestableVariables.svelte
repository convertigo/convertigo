<script>
	import Ico from '$lib/utils/Ico.svelte';
	import PropertyType from './PropertyType.svelte';
	import TableAutoCard from './TableAutoCard.svelte';

	let { requestable = $bindable() } = $props();
	let multiples = $state({});

	function parse(row) {
		try {
			multiples[row.name] = JSON.parse(row.value);
		} catch (e) {
			multiples[row.name] = [];
		}
	}

	$effect(() => {
		for (const row of requestable.variable) {
			if (row.isMultivalued == 'true') {
				parse(row);
			}
		}
	});
</script>

{#snippet property({ row, value, defaultValue = undefined })}
	<PropertyType
		type={row.isFileUpload == 'true' ? 'file' : row.isMasked == 'true' ? 'password' : 'text'}
		{value}
		{defaultValue}
		name={row.name}
		onfocus={() => (row.send = 'true')}
	/>
{/snippet}

<TableAutoCard
	definition={[
		{ name: 'Name', key: 'name', class: 'font-bold', th: 'w-0' },
		{ name: 'Send', custom: true, th: 'w-0' },
		{ name: 'Value', custom: true, th: 'min-w-40' },
		{ name: 'Comment', custom: true, class: 'max-w-40' }
	]}
	data={requestable.variable}
>
	{#snippet children({ row, def })}
		{#if def.name == 'Send'}
			<PropertyType type="boolean" bind:value={row.send} fit={true} />
		{:else if def.name == 'Value'}
			<div class:opacity-50={row.send == 'false'}>
				{#if row.isMultivalued == 'true'}
					<div class="layout-x-low">
						<button
							type="button"
							class="btn btn-sm bg-surface-200-800 !w-fit"
							onclick={() => multiples[row.name].push('')}><Ico icon="grommet-icons:add" /></button
						><button type="button" onclick={() => parse(row)} class="btn btn-sm bg-surface-200-800">
							<Ico icon="mdi:backup-restore" />
						</button>
					</div>
					{#each multiples[row.name] as value, i}
						<div class="layout-x-low">
							{@render property({ row, value })}
							<button
								type="button"
								class="btn btn-sm bg-surface-200-800 !w-fit"
								onclick={() => multiples[row.name].splice(i, 1)}
								><Ico icon="mingcute:delete-line" /></button
							>
						</div>
					{/each}
				{:else}
					{@render property({ row, value: row.value, defaultValue: row.value })}
				{/if}
			</div>
		{:else if def.name == 'Comment'}
			<div class="overflow-hidden max-h-12">{row.comment}</div>
		{/if}
	{/snippet}
</TableAutoCard>
