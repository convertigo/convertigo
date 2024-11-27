<script>
	import { SvelteFlowProvider } from '@xyflow/svelte';
	import DnDProvider from '$lib/studio/flow/DnDProvider.svelte';
	import Flow from '$lib/studio/flow/Flow.svelte';
</script>

<SvelteFlowProvider>
	<DnDProvider>
		<Flow />
	</DnDProvider>
</SvelteFlowProvider>
