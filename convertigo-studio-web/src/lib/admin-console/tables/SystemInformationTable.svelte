<script>
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
	import Table from '../admin-components/Table.svelte';
	export let memoryMaximal;
	export let memoryTotal;
	export let memoryUsed;

	let headers = ['Name', 'Value'];
	/** @type {{Name: string, Value: string|null}[]} */
	let data = [];

	let showHeaders = false;

	onMount(() => {
		statusCheck();
	});

	$: {
		data = [
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
			for (let d of data) {
				d.Value = null;
			}
		}
	}
</script>

<div>
	<Table {headers} {data} {showHeaders} />
</div>
