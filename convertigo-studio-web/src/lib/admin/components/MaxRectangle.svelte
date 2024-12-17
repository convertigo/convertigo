<script>
	import { debounce } from '$lib/utils/service';
	import { onMount, tick } from 'svelte';

	let {
		clientHeight = $bindable(),
		clientWidth = $bindable(),
		enabled = true,
		delay = -1,
		children,
		...rest
	} = $props();
	let calc = $state(false);
	let div = $state();

	function toInt(val) {
		return Math.floor(val.replace('px', ''));
	}

	const doCalc = debounce(() => {
		div.style.display = 'none';
		tick().then(() => {
			const { paddingTop, paddingBottom, paddingLeft, paddingRight, height, width } =
				getComputedStyle(div.parentElement);

			clientHeight = toInt(height) - toInt(paddingTop) - toInt(paddingBottom);
			clientWidth = toInt(width) - toInt(paddingLeft) - toInt(paddingRight);
			div.style.display = null;
			calc = false;
		});
	}, delay);

	$effect(() => {
		if (!enabled) {
			div.style.height = div.style.maxHeight = div.style.width = div.style.maxWidth = null;
			return;
		}
		if (calc) {
			doCalc();
		} else {
			div.style.height = `${clientHeight}px`;
			div.style.maxHeight = `${clientHeight}px`;
			div.style.width = `${clientWidth}px`;
			div.style.maxWidth = `${clientWidth}px`;
		}
	});

	onMount(() => {
		const observer = new ResizeObserver(() => (calc = true));
		observer.observe(div.parentElement);
		observer.observe(window.document.body);
		return () => observer.disconnect();
	});
</script>

<div bind:this={div} {...rest}>
	{@render children?.()}
</div>
