<script>
	export let delta = 0;
	let cls = '';
	export { cls as class };
	export let min = 5;
	export let coef = 1;
	export let dragging = false;
	let start = 0;
	let startDelta = 0;
	export let axis = 'x';
	const events = ['mousemove', 'touchmove', 'mouseup', 'touchend'];

	function getClient(event) {
		const key = axis == 'x' ? 'clientX' : 'clientY';
		return event.touches?.[0] ? event.touches[0][key] : event[key];
	}

	function startDrag(event) {
		dragging = true;
		start = getClient(event);
		startDelta = delta;
		for (const evt of events) {
			window.addEventListener(evt, evt.endsWith('move') ? onMouseMove : endDrag);
		}
	}

	function onMouseMove(event) {
		if (!dragging) {
			return;
		}
		const client = getClient(event);
		delta = Math.max(min, Math.round(startDelta + (client - start) * coef));
	}

	function endDrag() {
		dragging = false;
		for (const evt of events) {
			window.removeEventListener(evt, evt.endsWith('move') ? onMouseMove : endDrag);
		}
	}
</script>

<span class={`draggable ${cls}`} on:mousedown={startDrag} on:touchstart={startDrag} {...$$restProps}
	><slot /></span
>

<style>
	.draggable {
		display: inline-block;
		user-select: none; /* Prevent text selection */
	}
</style>
