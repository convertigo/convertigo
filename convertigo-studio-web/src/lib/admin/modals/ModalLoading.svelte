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
{:else if mode === 'Insufficient right'}
	<Card>
		<div class="flex flex-col justify-center items-center">
			<p class="text-xl font-bold">Insufficient rights ..</p>
			<Icon icon="game-icons:stone-block" style="color: #ff5900" class="w-20 h-20 mt-5" />
		</div>
	</Card>
{/if}
