<script>
	import { Dialog, Portal } from '@skeletonlabs/skeleton-svelte';
	import { onDestroy } from 'svelte';

	/** @type {{class?: string, children?: import('svelte').Snippet<[any]>}} */
	let { class: cls = '', children } = $props();
	let opened = $state(false);
	let result = $state();
	let params = $state();
	let resolve;
	let reject;

	export async function open(p) {
		p?.currentTarget?.blur?.();
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

<Dialog open={opened} onOpenChange={(e) => (opened = e.open)}>
	<Dialog.Trigger class="hidden" />
	<Portal>
		<Dialog.Backdrop class="fixed inset-0 z-50 bg-surface-50-950/60 backdrop-blur-sm" />
		<Dialog.Positioner class="fixed inset-0 z-50 layout-x-none justify-center p-low">
			<Dialog.Content class={`relative max-h-full overflow-auto ${cls}`}>
				{#if params}
					{@render children?.({ setResult, close, params })}
				{/if}
			</Dialog.Content>
		</Dialog.Positioner>
	</Portal>
</Dialog>
