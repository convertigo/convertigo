<script>
	import { ProgressRadial } from '@skeletonlabs/skeleton';
</script>

<div class="flex h-full items-center justify-center">
	<div class="w-1/2">
		<ProgressRadial class="w-full"></ProgressRadial>
		<span class="loading animate-pulse">checking authentication …</span>
	</div>
</div>

<style lang="postcss">
	.loading {
		position: fixed;
		top: 50%;
		left: 50%;
		transform: translate(-50%, -50%);
	}
</style>
