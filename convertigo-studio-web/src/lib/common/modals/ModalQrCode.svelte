<script>
	import Card from '$lib/admin/components/Card.svelte';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { qrCodeUrlStore, appUrlStore } from '../stores/urlStore';
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';

	const modalStore = getModalStore();
	export let qrCodeUrl = '';
</script>

<Card title="Qr Code" class="gap-5 w-72">
	{#if $qrCodeUrlStore}
		<a href={$appUrlStore} target="_blank">
			<img src={$qrCodeUrlStore} alt="QR code" title="QR code" />
		</a>
	{/if}
	<ButtonsContainer>
		<button class="cancel-button" on:click={() => modalStore.close()}>Close</button>
	</ButtonsContainer>
</Card>
