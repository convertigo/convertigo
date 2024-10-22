<script>
	import { run } from 'svelte/legacy';

	import { onMount } from 'svelte';
	import {
		statusCheck,
		javaVersion,
		javaClassVersion,
		javaVendor,
		hostName,
		osName,
		osVersion,
		osArchitecture,
		osAvailableProcessors,
		browser
	} from '../stores/statusStore';
	import TableAutoCard from '../components/TableAutoCard.svelte';

	/** @type {{Name: string, Value: string|null}[]} */
	let data = $state([]);

	/** @type {{memoryMaximal: any, memoryTotal: any, memoryUsed: any, class?: string}} */
	let { memoryMaximal, memoryTotal, memoryUsed, class: cls = '' } = $props();

	onMount(() => {
		statusCheck();
	});

	$effect(() => {
		/** @type {{Name: string, Value: string|null}[]} */
		let newdata = [
			{ Name: 'Host Name', Value: $hostName },
			{ Name: 'CPU', Value: `${$osArchitecture} architecture ${$osAvailableProcessors} processor` },
			{ Name: 'OS', Value: `${$osName} ${$osVersion}` },
			{ Name: 'Java Vendor', Value: $javaVendor },
			{ Name: 'Java', Value: `${$javaVersion} (classes version: ${$javaClassVersion})` },
			{ Name: 'Used Memory', Value: `${memoryUsed[memoryUsed.length - 1] ?? '...'} MB` },
			{ Name: 'Total Memory', Value: `${memoryTotal[memoryTotal.length - 1] ?? '...'} MB` },
			{ Name: 'Maximum Memory', Value: `${memoryMaximal[memoryMaximal.length - 1] ?? '...'} MB` },
			{ Name: 'Your Browser', Value: $browser }
		];
		if ($javaVendor == '') {
			for (let d of newdata) {
				d.Value = null;
			}
		}
		data = newdata;
	});
</script>

<TableAutoCard
	class={`statusTable ${cls}`}
	showHeaders={false}
	definition={[{ key: 'Name' }, { key: 'Value' }]}
	{data}
>
	{#snippet children(row, def)}
		{#if def.key === 'Name'}
			<span class="font-normal">{row.Name}</span>
		{/if}
	{/snippet}
</TableAutoCard>
