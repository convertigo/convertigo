<script>
	import { preventDefault } from 'svelte/legacy';

	import { SlideToggle, getModalStore } from '@skeletonlabs/skeleton';
	import Card from '../components/Card.svelte';
	import { checkArray } from '$lib/utils/service';

	const modalStore = getModalStore();
	const { category, filters, mode, ts } = $modalStore[0].meta;
	let { value, not } = $state($modalStore[0].meta);

	/** @type {{parent: any}} */
	let { parent } = $props();

	function submit(e) {
		filters.update((f) => {
			let array = checkArray(f[category]);
			const val = {
				mode: e.submitter.value,
				value,
				not,
				ts
			};
			if (mode) {
				array[array.findIndex((o) => o.ts == ts)] = val;
			} else {
				array.push(val);
			}
			f[category] = array;
			return f;
		});
		modalStore.close();
	}
</script>

<Card title="{mode ? 'Edit' : 'Add'} log filter for {category}">
	<form onsubmit={preventDefault(submit)} class="flex flex-col gap-2">
		{#if category == 'Message'}
			<textarea
				class="textarea overflow-auto"
				bind:value
				wrap="off"
				rows={Math.min(10, value.split('\n').length)}
			></textarea>
		{:else}
			<input class="input" bind:value />
		{/if}
		<SlideToggle name="negate" bind:checked={not} active="bg-error-400 dark:bg-error-700"
			>{not ? 'not' : 'is'}</SlideToggle
		>
		<div class="flex flex-wrap gap-2">
			{#each ['startsWith', 'equals', 'includes', 'endsWith'] as _mode}
				<button
					type="submit"
					class="btn"
					class:variant-filled-primary={mode != _mode}
					class:variant-filled-success={mode == _mode}
					value={_mode}
				>
					{_mode}
				</button>
			{/each}
		</div>
	</form>
</Card>

<style lang="postcss">
</style>
