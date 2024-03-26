<script>
	import { onMount } from 'svelte';
	import {
		statusCheck,
		product,
		licenceType,
		licenceNumber,
		licenceExpired
	} from '../stores/statusStore';
	import TableAutoCard from '../components/TableAutoCard.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	export let time;
	export let startTime;
	export let engineState;

	let cls = '';
	export { cls as class };

	/** @type {{Name: string, Value: string|null}[]} */
	let data;

	$: {
		data = [
			{ Name: 'Engine State', Value: engineState ? 'Running' : 'Stopped' },
			{ Name: 'Convertigo Version', Value: $product },
			{ Name: 'Last Startup', Value: new Date(startTime).toLocaleString() },
			{ Name: 'Uptime', Value: new Date(time - startTime).toLocaleTimeString() },
			{ Name: 'License Type', Value: $licenceType },
			{ Name: 'License N°', Value: $licenceNumber },
			{ Name: 'License Expiration Date', Value: $licenceExpired }
		];
		if ($product == '' || !startTime) {
			for (let d of data) {
				d.Value = null;
			}
		}
	}
	onMount(() => {
		statusCheck();
	});
</script>

<TableAutoCard
	class={`statusTable ${cls}`}
	showHeaders={false}
	definition={[{ key: 'Name' }, { key: 'Value', custom: true }]}
	{data}
	let:row
	let:def
>
	{#if row[def.key] == 'Running'}
		<span class="on" />
	{:else if row[def.key] == 'Stopped'}
		<span class="off" />
	{/if}
	<AutoPlaceholder loading={row[def.key] == null}>{row[def.key] ?? ''}</AutoPlaceholder>
</TableAutoCard>

<style lang="postcss">
	:global(.statusTable td:has(> .on)) {
		@apply bg-success-500;
	}
	:global(.statusTable td:has(> .off)) {
		@apply bg-error-500;
	}
</style>
