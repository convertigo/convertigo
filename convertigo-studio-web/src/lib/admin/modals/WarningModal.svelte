<script>
	import { getModalStore } from '@skeletonlabs/skeleton';
	import Card from '../components/Card.svelte';
	import { keysCheck } from '../stores/keysStore';

	const modalStore = getModalStore();
	const { mode } = $modalStore[0].meta;

	async function deleteKey(keyText) {
		try {
			// @ts-ignore
			const response = await call('keys.Remove', {
				'@_xml': true,
				admin: {
					'@_service': 'keys.Remove',
					keys: {
						key: {
							'@_text': keyText
						}
					}
				}
			});
			if (response) {
				keysCheck();
			}
		} catch (error) {
			console.error(error);
		}
	}
</script>

{#if mode == 'Delete'}
	<Card>
		<h1 class="text-2xl font-bold">Please Confirm</h1>

		<p class="">Are you sure you want to proceed ?</p>

		<div class="flex flex-wrap">
			<div class="flex-1">
				<button on:click={() => modalStore.close()} class="bg-error-400-500-token"> Cancel </button>
			</div>

			<div class="flex-1">
				<button type="button" on:click={deleteKey} class="bg-primary-400-500-token">
					Confirm
				</button>
			</div>
		</div>
	</Card>
{/if}
