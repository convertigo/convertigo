<script>
	import { onMount } from 'svelte';
	import { writable } from 'svelte/store';
	import { call } from '$lib/utils/service';

	let certificates = writable([]);
	onMount(() => {});

	async function certificatesList() {
		const res = await call('certificates.List');
		certificates.set(res);
		console.log('certificatesList res', res);
	}
</script>
