<script>
	import { debounce } from '$lib/utils/service';
	import { onMount, tick } from 'svelte';

	let {
		clientHeight = $bindable(),
		clientWidth = $bindable(),
		minHeight = 100,
		enabled = true,
		delay = -1,
		children,
		...rest
	} = $props();
	let calc = $state(false);
	let div = $state();

	const marginBottom = 8;
	function toInt(val) {
		return Math.floor(val.replace('px', ''));
	}

	const doCalc = $derived(
		debounce(() => {
			if (!div) {
				return;
			}
			tick().then(() => {
				const { paddingTop, paddingBottom, paddingLeft, paddingRight, height, width } =
					getComputedStyle(div.parentElement);
				const parentHeight = toInt(height) - toInt(paddingTop) - toInt(paddingBottom);
				const parentWidth = toInt(width) - toInt(paddingLeft) - toInt(paddingRight);

				const rect = div.getBoundingClientRect();
				const viewportAvail = Math.max(0, Math.floor(window.innerHeight - rect.top - marginBottom));
				const nHeight = Math.min(parentHeight, viewportAvail || parentHeight);
				clientHeight = nHeight < clientHeight ? minHeight : nHeight;
				clientWidth = parentWidth;
				calc = false;
			});
		}, delay)
	);

	$effect(() => {
		if (!div) {
			return;
		}
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
		return () => {
			observer.disconnect();
		};
	});
</script>

<div bind:this={div} {...rest}>
	{@render children?.()}
</div>
