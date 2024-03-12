<script>
	import { getModalStore } from '@skeletonlabs/skeleton';
	import Card from '../components/Card.svelte';
	import { call } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import TableAutoCard from '../components/TableAutoCard.svelte';
	import { getEnvironmentVar } from '../stores/symbolsStore';
	import ResponsiveContainer from '../components/ResponsiveContainer.svelte';

	const modalStore = getModalStore();
	const { mode } = $modalStore[0].meta;
	let javaSystemProp = '';
	let data = Array(10).fill({ '@_name': null, '@_value': null });
	onMount(() => {
		console.log('mount mode', mode); // props or env
		if (mode == 'props') {
			call('engine.GetJavaSystemPropertiesJson').then((res) => {
				data = res.properties;
			});
		} else {
			getEnvironmentVar().then((res) => {
				data = res;
			});
		}
	});
</script>

<Card title={mode == 'env' ? 'Environment Variables' : 'Java System Properties'}>
	<div class="p-5">
		<ResponsiveContainer
			scrollable={true}
			smCols="sm:grid-cols-1"
			mdCols="md:grid-cols-1"
			lgCols="lg:grid-cols-1"
		>
			<TableAutoCard
				definition={[
					{ name: 'Name', key: 'name' },
					{ name: 'Value', key: 'value' }
				]}
				{data}
			></TableAutoCard>
		</ResponsiveContainer>

		<button on:click={() => modalStore.close()} class="variant-filled-surface mt-10">Close</button>
	</div>
</Card>
