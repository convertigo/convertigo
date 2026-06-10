<script>
	import { debounce } from '$lib/utils/service';
	import { onMount, tick } from 'svelte';
	import { SvelteSet } from 'svelte/reactivity';

	let {
		clientHeight = $bindable(),
		clientWidth = $bindable(),
		minHeight = 100,
		enabled = true,
		delay = -1,
		children,
		...rest
	} = $props();
	let calc = $state(true);
	let div = $state();
	let resizeObserver;
	let mutationObserver;
	let calcFrame;

	const marginBottom = 8;
	function toInt(val) {
		return Math.floor(Number.parseFloat(val) || 0);
	}

	function requestCalc() {
		if (calc) {
			return;
		}
		if (calcFrame) {
			return;
		}
		calcFrame = requestAnimationFrame(() => {
			calcFrame = undefined;
			calc = true;
		});
	}

	function observeLayoutSources() {
		if (!div || !resizeObserver) {
			return;
		}

		resizeObserver.disconnect();
		mutationObserver?.disconnect();
		resizeObserver.observe(window.document.body);

		const parents = new SvelteSet();
		let node = div;
		while (node?.parentElement && node !== window.document.body) {
			const parent = node.parentElement;
			parents.add(parent);
			resizeObserver.observe(parent);
			for (const child of parent.children) {
				if (child !== node) {
					resizeObserver.observe(child);
				}
			}
			node = parent;
		}

		for (const parent of parents) {
			mutationObserver?.observe(parent, { childList: true });
		}
	}

	const doCalc = $derived(
		debounce(() => {
			if (!div?.parentElement) {
				return;
			}
			tick().then(() => {
				if (!div?.parentElement) {
					return;
				}
				const { paddingTop, paddingBottom, paddingLeft, paddingRight } = getComputedStyle(
					div.parentElement
				);
				const parentHeight =
					div.parentElement.clientHeight - toInt(paddingTop) - toInt(paddingBottom);
				const parentWidth =
					div.parentElement.clientWidth - toInt(paddingLeft) - toInt(paddingRight);

				const rect = div.getBoundingClientRect();
				const viewportAvail = Math.max(0, Math.floor(window.innerHeight - rect.top - marginBottom));
				const nHeight = Math.min(parentHeight, viewportAvail || parentHeight);
				clientHeight = Math.max(minHeight, nHeight);
				clientWidth = Math.max(0, parentWidth);
				calc = false;
			});
		}, delay)
	);

	$effect(() => {
		if (!div) {
			return;
		}
		if (!enabled) {
			div.style.height = div.style.maxHeight = div.style.width = div.style.maxWidth = '';
			calc = true;
			return;
		}
		if (calc || clientHeight == null || clientWidth == null) {
			doCalc();
		} else {
			div.style.height = `${clientHeight}px`;
			div.style.maxHeight = `${clientHeight}px`;
			div.style.width = `${clientWidth}px`;
			div.style.maxWidth = `${clientWidth}px`;
		}
	});

	onMount(() => {
		resizeObserver = new ResizeObserver(requestCalc);
		mutationObserver = new MutationObserver(() => {
			observeLayoutSources();
			requestCalc();
		});
		observeLayoutSources();
		requestCalc();
		return () => {
			resizeObserver?.disconnect();
			mutationObserver?.disconnect();
			if (calcFrame) {
				cancelAnimationFrame(calcFrame);
			}
		};
	});
</script>

<div bind:this={div} {...rest}>
	{@render children?.()}
</div>
