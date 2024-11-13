<script>
	import { Modal } from '@skeletonlabs/skeleton-svelte';
	import { onDestroy } from 'svelte';
	/** @type {{children?: import('svelte').Snippet<[any]>}} */
	let { children } = $props();
	let opened = $state(false);
	let result = $state();
	let params = $state();
	let resolve;
	let reject;

	export async function open(p) {
		params = p;
		opened = true;
		return await new Promise((ok) => {
			resolve = ok;
		});
	}

	$effect(() => {
		if (!opened && resolve) {
			resolve(result);
			resolve = reject = undefined;
		}
	});

	onDestroy(() => {
		if (reject) {
			reject();
			resolve = reject = undefined;
		}
	});

	function setResult(value) {
		result = value;
	}

	function close() {
		opened = false;
	}
</script>

<Modal bind:open={opened} contentBase="w-full" triggerBase="hidden">
	{#snippet content()}
		{@render children?.({ setResult, close, params })}
	{/snippet}
</Modal>
