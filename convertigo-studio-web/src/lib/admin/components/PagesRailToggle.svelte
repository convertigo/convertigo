<script>
	import Button from './Button.svelte';

	let { state = $bindable(), class: cls = '' } = $props();
</script>

<Button
	size={8}
	class={cls}
	icon="iconamoon:menu-burger-horizontal-thin"
	onclick={() => (state = !state)}
/>
