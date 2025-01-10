<script>
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

<div class="h-full justify-center">
	<h1 class="text-4xl text-center">
		<p>Disconnecting from Convertigo</p>
	</h1>
</div>
