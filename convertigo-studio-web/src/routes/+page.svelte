<script>
	import { ProgressRing } from '@skeletonlabs/skeleton-svelte';
</script>

<div class="layout-x h-screen w-full justify-center">
	<ProgressRing value={null} size="size-64">
		<span class="animate-pulse">checking authentication …</span>
	</ProgressRing>
</div>
