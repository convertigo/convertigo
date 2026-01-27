<script>
	import { fade, fly } from 'svelte/transition';

	/** @type {{ delta?: number, rounded?: string, extraClass?: string }} */
	let { delta = 0, rounded = 'rounded-sm', extraClass = '' } = $props();
</script>

<span
	in:fly={{ y: delta * 50 }}
	out:fade
	class={`absolute inset-0 ${rounded} bg-surface-50-950 shadow-sm/10 shadow-surface-900-100 ${extraClass}`.trim()}
></span>
