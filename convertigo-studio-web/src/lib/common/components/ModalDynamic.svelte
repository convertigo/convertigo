<script>
	import { Modal } from '@skeletonlabs/skeleton-svelte';
	import { onDestroy } from 'svelte';
	/** @type {{class?: string, children?: import('svelte').Snippet<[any]>}} */
	let { class: cls = '', children } = $props();
	let opened = $state(false);
	let result = $state();
	let params = $state();
	let resolve;
	let reject;

	export async function open(p) {
		p?.event?.currentTarget?.blur?.();
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

	export function setResult(value) {
		result = value;
	}

	export function close(value) {
		if (typeof result == 'undefined' || typeof value != 'undefined') {
			result = value;
		}
		opened = false;
	}
</script>

<Modal bind:open={opened} triggerBase="hidden" contentBase={cls}>
	{#snippet content()}
		{@render children?.({ setResult, close, params })}
	{/snippet}
</Modal>
