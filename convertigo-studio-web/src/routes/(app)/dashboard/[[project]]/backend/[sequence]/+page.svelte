<script>
	import { browser } from '$app/environment';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import { onMount } from 'svelte';

	let FlowProvider;
	let DragProvider;
	let FlowComponent;
	let loadError = null;

	onMount(async () => {
		if (!browser) return;
		try {
			const [{ SvelteFlowProvider }, { default: DnDProvider }, { default: Flow }] =
				await Promise.all([
					import('@xyflow/svelte'),
					import('$lib/studio/flow/DnDProvider.svelte'),
					import('$lib/studio/flow/Flow.svelte')
				]);
			FlowProvider = SvelteFlowProvider;
			DragProvider = DnDProvider;
			FlowComponent = Flow;
		} catch (error) {
			loadError = error;
			console.error('Failed to load Flow editor', error);
		}
	});
</script>

{#if FlowProvider && DragProvider && FlowComponent}
	<svelte:component this={FlowProvider}>
		<svelte:component this={DragProvider}>
			<svelte:component this={FlowComponent} />
		</svelte:component>
	</svelte:component>
{:else if loadError}
	<p class="text-error-500">Unable to load flow editor.</p>
{:else}
	<AutoPlaceholder class="h-64" loading />
{/if}
