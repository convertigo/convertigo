<script>
	import Button from './Button.svelte';

	let { state = $bindable(), class: cls = '' } = $props();
</script>

<Button
	size={8}
	class={cls}
	icon="mdi:menu"
	title="Toggle navigation"
	onclick={() => (state = !state)}
/>
