<script>
	import Card from '$lib/admin/components/Card.svelte';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';
	import QrCode from '../components/QrCode.svelte';

	const modalStore = getModalStore();
</script>

<Card title="Qr Code" class="gap-5 w-72">
	{#if $modalStore?.[0]?.meta?.href}
		<QrCode href={$modalStore[0].meta.href} />
	{/if}
	<ButtonsContainer>
		<button class="cancel-button" on:click={() => modalStore.close()}>Close</button>
	</ButtonsContainer>
</Card>
