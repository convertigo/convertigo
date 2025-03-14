<script>
	import { createBubbler } from 'svelte/legacy';

	const bubble = createBubbler();

	/** @type {{disabled?: boolean, children?: import('svelte').Snippet}} */
	let { disabled = false, children } = $props();
</script>

<!-- svelte-ignore a11y_no_static_element_interactions -->
<!-- svelte-ignore a11y_click_events_have_key_events -->
<div class={disabled ? 'disabledItem' : ''} onclick={bubble('click')}>
	{@render children?.()}
</div>

<style>
	.disabledItem {
		pointer-events: none;
		opacity: 0.4;
	}
</style>
