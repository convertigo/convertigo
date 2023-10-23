<script>
	export let disabled = false;
	
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<!-- svelte-ignore a11y-click-events-have-key-events -->
<div class="{disabled ? 'disabledItem':''}" on:click>
	<slot />
</div>

<style>
	.disabledItem {
		pointer-events: none;
		opacity: 0.4;
	}
</style>
