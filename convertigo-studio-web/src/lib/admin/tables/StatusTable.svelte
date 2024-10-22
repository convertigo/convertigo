<script>
	import { onMount } from 'svelte';
	import {
		statusCheck,
		product,
		licenceType,
		licenceNumber,
		licenceExpired,
		endpoint
	} from '../stores/statusStore';
	import TableAutoCard from '../components/TableAutoCard.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';

	/** @type {{time: any, startTime: any, engineState: any, class?: string}} */
	let { time, startTime, engineState, class: cls = '' } = $props();

	/** @type {{Name: string, Value: string|null}[]} */
	let data = $state([]);

	$effect(() => {
		/** @type {{Name: string, Value: string|null}[]} */
		let newData = [
			{ Name: 'Engine State', Value: engineState ? 'Running' : 'Stopped' },
			{ Name: 'Convertigo Version', Value: $product },
			{ Name: 'Last Startup', Value: new Date(startTime).toLocaleString() },
			{ Name: 'Uptime', Value: new Date(time - startTime).toLocaleTimeString() },
			{ Name: 'License Type', Value: $licenceType },
			{ Name: 'License N°', Value: $licenceNumber },
			{ Name: 'License Expiration Date', Value: $licenceExpired },
			{ Name: 'Endpoint', Value: $endpoint }
		];
		if ($product == '' || !startTime) {
			for (let d of newData) {
				d.Value = null;
			}
		}
		data = newData;
	});
	onMount(() => {
		statusCheck();
	});
</script>

<TableAutoCard
	class="statusTable {cls}"
	showHeaders={false}
	definition={[
		{ key: 'Name', custom: true },
		{ key: 'Value', custom: true }
	]}
	{data}
>
	{#snippet children(row, def)}
		{#if def.key === 'Name'}
			<span class="font-normal">{row.Name}</span>
		{:else}
			{#if row[def.key] == 'Running'}
				<span class="on"></span>
			{:else if row[def.key] == 'Stopped'}
				<span class="off"></span>
			{/if}
			<AutoPlaceholder loading={row[def.key] == null}>{row[def.key] ?? ''}</AutoPlaceholder>
		{/if}
	{/snippet}
</TableAutoCard>

<style lang="postcss">
	:global(.statusTable td:has(> .on)) {
		@apply bg-success-500;
	}
	:global(.statusTable td:has(> .off)) {
		@apply bg-error-500;
	}
</style>
