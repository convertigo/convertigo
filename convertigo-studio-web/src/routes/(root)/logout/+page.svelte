<script>
	import { ProgressRing } from '@skeletonlabs/skeleton-svelte';
	import { browser } from '$app/environment';
	import { goto } from '$app/navigation';
	import { base } from '$app/paths';
	import Authentication from '$lib/common/Authentication.svelte';

	if (browser) {
		Authentication.logout().finally(() => {
			goto(`${base}/login`);
		});
	}
</script>

<h1 class="text-center text-4xl">
	<p>Disconnecting from Convertigo</p>
</h1>
<ProgressRing value={null} size="size-72">
	<span class="animate-pulse text-center">Disconnecting from<br />Convertigo …</span>
</ProgressRing>
