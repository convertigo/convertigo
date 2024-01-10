<script>
	import { AppBar } from '@skeletonlabs/skeleton';
	import { monitorCheck, time } from '../stores/monitorStore';
	import { onMount } from 'svelte';

	onMount(() => {
		monitorCheck();
	});
</script>

<AppBar background="bg-surface-800 border-b-[0.5px] border-surface-800"
	>{new Date($time).toLocaleString()}</AppBar
>
