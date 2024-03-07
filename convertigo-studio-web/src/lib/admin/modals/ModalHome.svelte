<script>
	import { getModalStore } from '@skeletonlabs/skeleton';
	import Card from '../components/Card.svelte';
	import { call } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import TableAutoCard from '../components/TableAutoCard.svelte';
	import { environmentVariables, getEnvironmentVar } from '../stores/symbolsStore';
	import ResponsiveContainer from '../components/ResponsiveContainer.svelte';

	const modalStore = getModalStore();
	const { mode } = $modalStore[0].meta;

	let javaSystemProp = '';

	onMount(() => {
		getJavaSystemProp();
		getEnvironmentVar();
	});
	async function getJavaSystemProp() {
		try {
			const res = await call('engine.GetJavaSystemProperties');
			console.log('java system prop', res);
			if (res?.admin) {
				javaSystemProp = res.admin['#text'];
			}
		} catch (err) {
			console.error(err);
		}
	}
</script>

{#if mode == 'Java System Prop'}
	<Card>
		<div class="p-5">
			<h1 class="text-2xl font-bold mb-5">Java System Properties</h1>
			<ResponsiveContainer smCols="sm:grid-cols-1" mdCols="md:grid-cols-1" lgCols="lg:grid-cols-1">
				<textarea
					class="responsive-textarea dark:bg-surface-500 bg-surface-50 border-none rounded-token"
					name="javaSystemProp"
					placeholder="Java System Properties here ..."
				>
					{javaSystemProp}
				</textarea>
			</ResponsiveContainer>

			<button on:click={() => modalStore.close()} class="variant-filled-surface mt-10">Ok</button>
		</div>
	</Card>
{:else if mode == 'Environment Variables'}
	<Card title="Environment Variables">
		<div class="p-5">
			<ResponsiveContainer
				scrollable={true}
				smCols="sm:grid-cols-1"
				mdCols="md:grid-cols-1"
				lgCols="lg:grid-cols-1"
			>
				<TableAutoCard
					definition={[
						{ name: 'Name', key: '@_name' },
						{ name: 'Value', key: '@_value' }
					]}
					data={$environmentVariables}
				></TableAutoCard>
			</ResponsiveContainer>

			<button on:click={() => modalStore.close()} class="variant-filled-surface mt-10">Ok</button>
		</div>
	</Card>
{/if}

<style>
	.responsive-textarea {
		width: 60vw;
		height: 50vh;
		min-height: 100px;
		max-height: 500px;
		padding: 1em;
		box-sizing: border-box;
		resize: vertical;
	}
</style>
