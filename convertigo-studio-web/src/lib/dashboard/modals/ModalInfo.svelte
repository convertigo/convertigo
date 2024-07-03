<script>
	import Card from '$lib/admin/components/Card.svelte';
	import { getModalStore } from '@skeletonlabs/skeleton';

	const modalStore = getModalStore();
	const { mode } = $modalStore[0].meta ?? {};
</script>

{#if mode === 'Sequence Info'}
	<Card>hello</Card>
{/if}
