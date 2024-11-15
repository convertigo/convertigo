<script>
	let start = 0;
	let startDelta = 0;
	/** @type {{
	 * delta?: number,
	 * class?: string,
	 * min?: number,
	 * coef?: number,
	 * dragging?: boolean,
	 * axis?: 'x' | 'y',
	 * children?: import('svelte').Snippet,
	}} */
	let {
		delta = $bindable(0),
		class: cls = '',
		min = 5,
		coef = 1,
		dragging = $bindable(false),
		axis = 'x',
		children,
		...rest
	} = $props();
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

<span class="draggable {cls}" onmousedown={startDrag} ontouchstart={startDrag} {...rest}
	>{@render children?.()}</span
>

<style>
	.draggable {
		display: inline-block;
		user-select: none; /* Prevent text selection */
	}
</style>
