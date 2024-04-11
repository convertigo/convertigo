<script>
	import { getModalStore } from '@skeletonlabs/skeleton';
	import Card from '../components/Card.svelte';
	import Icon from '@iconify/svelte';

	const modalStore = getModalStore();
	const { mode } = $modalStore[0].meta;
</script>

{#if mode === 'Loading'}
	<Card>
		<Icon icon="eos-icons:three-dots-loading" class="w-10 h-10" />
	</Card>
{/if}
