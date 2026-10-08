<script>
	import { Progress } from '@skeletonlabs/skeleton-svelte';
	import Authentication from '#lib/common/Authentication.svelte.js';
	import { resolve } from '#lib/utils/route.js';
	import { browser } from '$app/env';
	import { goto } from '$app/navigation';

	if (browser) {
		Authentication.logout().finally(() => {
			goto(resolve('/login/'));
		});
	}
</script>

<div class="preset-glass-primary relative grid place-items-center rounded-full p-low">
	<Progress value={null} class="relative grid place-items-center">
		<Progress.Circle style="--size: 18rem; --thickness: 10px;">
			<Progress.CircleTrack class="stroke-surface-200-800/50" />
			<Progress.CircleRange class="stroke-primary-500" />
		</Progress.Circle>
		<span class="absolute animate-pulse text-center text-sm font-medium"
			>Disconnecting from<br />Convertigo …</span
		>
	</Progress>
</div>
