<script>
	import Card from '../components/Card.svelte';
	import { call } from '$lib/utils/service';
	import TableAutoCard from '../components/TableAutoCard.svelte';
	import { Modal } from '@skeletonlabs/skeleton-svelte';
	import EnvironmentVariables from '$lib/common/EnvironmentVariables.svelte';

	/** @type {{ mode: string, open: boolean }}*/
	let { mode, open = $bindable(false) } = $props();

	let data = $state(Array(10).fill({ '@_name': null, '@_value': null }));
	$effect(() => {
		if (mode == 'props') {
			call('engine.GetJavaSystemPropertiesJson').then((res) => {
				data = res.properties;
			});
		} else if (mode == 'env') {
			data = EnvironmentVariables.variables;
		}
	});
</script>

<Modal bind:open triggerBase="hidden" contentBase="w-full">
	{#snippet content()}
		<Card title={mode == 'env' ? 'Environment Variables' : 'Java System Properties'}>
			<TableAutoCard
				definition={[
					{ name: 'Name', key: 'name' },
					{ name: 'Value', key: 'value' }
				]}
				{data}
				class="max-h-[80vh]"
			></TableAutoCard>

			<div class="w-full layout-x justify-end">
				<button onclick={() => (open = false)} class="cancel-button">Close</button>
			</div>
		</Card>
	{/snippet}
</Modal>
