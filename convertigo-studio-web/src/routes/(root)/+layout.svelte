<script>
	import LightSwitch from '$lib/common/components/LightSwitch.svelte';

	/** @type {{children?: import('svelte').Snippet}} */
	let { children } = $props();
</script>

<div class="h-screen">
	<span class="fixed top-0 right-0 p-low"><LightSwitch /></span>
	<div class="bg layout-y h-full justify-center">
		{@render children?.()}
	</div>
</div>

<style lang="postcss">
	@property --angle {
		syntax: '<angle>';
		inherits: false;
		initial-value: 150deg;
	}
	@keyframes gradient {
		0% {
			background-position: 0% 50%;
			--angle: 150deg;
		}
		50% {
			background-position: 100% 50%;
			--angle: 250deg;
		}
		100% {
			background-position: 0% 50%;
			--angle: 150deg;
		}
	}
	.bg {
		background-image: linear-gradient(
			var(--angle),
			var(--color-tertiary-50),
			var(--color-secondary-50),
			var(--color-primary-100),
			var(--color-surface-50) 90%
		);
		background-size: 250% 250%;
		animation: gradient 20s ease infinite;
	}
	:global(.dark) .bg {
		background-image: linear-gradient(
			var(--angle),
			var(--color-tertiary-950),
			var(--color-secondary-950),
			var(--color-primary-900),
			var(--color-surface-950) 90%
		);
	}
</style>
