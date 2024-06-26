<script>
	import Card from '$lib/admin/components/Card.svelte';
	import ModalButtons from '$lib/admin/components/ModalButtons.svelte';
	import { getModalStore } from '@skeletonlabs/skeleton';

	const modalStore = getModalStore();
	const { mode } = $modalStore[0].meta ?? {};
</script>

{#if mode == 'Confirm'}
	<Card class="min-w-[30vw]">
		{#if $modalStore[0]}
			<header class="text-2xl font-bold mb-5">{$modalStore[0].title}</header>
			<article class="mb-10">{$modalStore[0].body}</article>
			<ModalButtons />
		{/if}
	</Card>
{/if}
