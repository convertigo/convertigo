<script>
	import { onMount } from 'svelte';
	import {
		statusCheck,
		product,
		licenceType,
		licenceNumber,
		licenceExpired
	} from '../stores/statusStore';
	import Table from '../components/Table.svelte';
	export let time;
	export let startTime;
	export let engineState;

	let headers = ['Name', 'Value'];

	let showHeaders = false;
	/** @type {{Name: string, Value: string|null}[]} */
	let data;

	$: {
		data = [
			{ Name: 'Engine State', Value: engineState ? 'Running' : 'Stopped' },
			{ Name: 'Convertigo Version', Value: $product },
			{ Name: 'Last Startup', Value: new Date(startTime).toLocaleString() },
			{ Name: 'Uptime', Value: new Date(time - startTime).toLocaleTimeString() },
			{ Name: 'License Type', Value: $licenceType },
			{ Name: 'License n°', Value: $licenceNumber },
			{ Name: 'License expiration date', Value: $licenceExpired }
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

<div>
	<Table {headers} {data} {showHeaders} />
</div>
