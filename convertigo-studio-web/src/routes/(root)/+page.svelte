<script>
	import { Progress } from '@skeletonlabs/skeleton-svelte';
</script>

<div class="preset-glass-primary relative grid place-items-center rounded-full p-low">
	<Progress value={null} class="relative grid place-items-center">
		<Progress.Circle style="--size: 18rem; --thickness: 10px;">
			<Progress.CircleTrack class="stroke-surface-200-800/50" />
			<Progress.CircleRange class="stroke-primary-500" />
		</Progress.Circle>
		<span class="absolute animate-pulse text-sm font-medium">Checking authentication …</span>
	</Progress>
</div>
