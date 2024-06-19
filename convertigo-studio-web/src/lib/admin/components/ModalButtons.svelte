<!-- ModalButtons.svelte -->
<script>
	import { getModalStore } from '@skeletonlabs/skeleton';

	const modalStore = getModalStore();
	export let showConfirmBtn = true;
</script>

<div class="w-full flex justify-end mt-10">
	<div class="flex flex-wrap gap-2">
		{#if showConfirmBtn}
			<div class="flex-1">
				<button class="cancel-button w-40" on:click|preventDefault={() => modalStore.close()}>
					Cancel
				</button>
			</div>
			<div class="flex-1">
				<button type="submit" class="basic-button w-40">Confirm</button>
			</div>
		{:else}
			<div class="flex-1">
				<button class="cancel-button w-40" on:click|preventDefault={() => modalStore.close()}>
					Cancel
				</button>
			</div>
		{/if}
	</div>
</div>
