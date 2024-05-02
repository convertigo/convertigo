<script>
	import { getModalStore } from '@skeletonlabs/skeleton';

	const modalStore = getModalStore();
</script>

<div class="w-full flex justify-end mt-10">
	<div class="flex flex-wrap gap-2">
		<div class="flex-1">
			<button class="gray-button w-60" on:click|preventDefault={() => modalStore.close()}
				>Cancel</button
			>
		</div>
		<div class="flex-1">
			<button type="submit" class="bg-primary-400-500-token w-60">Confirm</button>
		</div>
	</div>
</div>
