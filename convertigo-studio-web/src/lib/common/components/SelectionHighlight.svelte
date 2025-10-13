<script>
	import { fade, fly } from 'svelte/transition';

	/** @type {{ delta?: number, rounded?: string, extraClass?: string }} */
	let { delta = 0, rounded = 'rounded-sm', extraClass = '' } = $props();
</script>

<span
	in:fly={{ y: delta * 50 }}
	out:fade
	class={`absolute inset-0 ${rounded} preset-filled-primary-500 opacity-40 shadow-md/30 shadow-primary-900-100 ${extraClass}`.trim()}
></span>
