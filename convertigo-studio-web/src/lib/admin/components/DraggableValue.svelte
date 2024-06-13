<script>
	export let deltaX = 0;
	let cls = '';
	export { cls as class };
	export let min = 5;
	export let coef = 0.5;
	let isDragging = false;
	let startX = 0;
	let startDeltaX = 0;

	function startDrag(event) {
		isDragging = true;
		startX = event.type === 'mousedown' ? event.clientX : event.touches[0].clientX;
		startDeltaX = deltaX;
		window.addEventListener('mousemove', onMouseMove);
		window.addEventListener('mouseup', endDrag);
		window.addEventListener('touchmove', onMouseMove);
		window.addEventListener('touchend', endDrag);
	}

	function onMouseMove(event) {
		if (!isDragging) return;
		let clientX = event.type === 'mousemove' ? event.clientX : event.touches[0].clientX;
		deltaX = Math.max(min, Math.round(startDeltaX + (clientX - startX) * coef));
	}

	function endDrag() {
		isDragging = false;
		window.removeEventListener('mousemove', onMouseMove);
		window.removeEventListener('mouseup', endDrag);
		window.removeEventListener('touchmove', onMouseMove);
		window.removeEventListener('touchend', endDrag);
	}
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<span class={`draggable ${cls}`} on:mousedown={startDrag} on:touchstart={startDrag} {...$$restProps}
	><slot /></span
>

<style>
	.draggable {
		display: inline-block;
		user-select: none; /* Prevent text selection */
	}
</style>
