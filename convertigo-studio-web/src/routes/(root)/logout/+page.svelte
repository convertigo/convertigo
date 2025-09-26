<script>
	import { ProgressRing } from '@skeletonlabs/skeleton-svelte';
	import { browser } from '$app/environment';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import Authentication from '$lib/common/Authentication.svelte';

	if (browser) {
		Authentication.logout().finally(() => {
			goto(resolve('/login/'));
		});
	}
</script>

<div class="preset-glass-primary rounded-full">
	<ProgressRing value={null} size="size-72">
		<span class="animate-pulse text-center">Disconnecting from<br />Convertigo …</span>
	</ProgressRing>
</div>
