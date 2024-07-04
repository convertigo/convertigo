<script>
	import Card from '$lib/admin/components/Card.svelte';
	import ModalButtons from '$lib/admin/components/ModalButtons.svelte';
	import { getModalStore } from '@skeletonlabs/skeleton';

	const modalStore = getModalStore();
	const { mode } = $modalStore[0].meta ?? {};
	export let parent = '';
</script>

{#if mode === 'Rename Project'}
	<Card title="Rename Project" class="w-96">
		<label class="border-common">
			<input
				type="text"
				placeholder="Rename Project"
				class="border rounded-md input-common w-full"
			/>
		</label>
	</Card>
{:else if mode === 'Delete Project'}
	<Card class="w-[30vw] min-w-96">
		{#if $modalStore[0]}
			<h2 class="text-2xl font-bold mb-5">Please Confirm</h2>
			<p>Are you sure you want to delete the project ?</p>
			<ModalButtons />
		{/if}
	</Card>
{:else}
	<Card title="Unknown Action" class="w-96">
		<p>Unknown mode</p>
	</Card>
{/if}
