<script>
	import { onMount, tick } from 'svelte';

	let { clientHeight = $bindable(0), clientWidth = $bindable(0), children } = $props();
	let calc = $state(false);
	let div = $state();

	function toInt(val) {
		return Math.floor(val.replace('px', ''));
	}

	$effect(() => {
		if (calc) {
			console.log('calc');
			div.style.display = 'none';
			tick().then(async () => {
				const { paddingTop, paddingBottom, paddingLeft, paddingRight, height, width } =
					getComputedStyle(div.parentElement);

				clientHeight = toInt(height) - toInt(paddingTop) - toInt(paddingBottom);
				clientWidth = toInt(width) - toInt(paddingLeft) - toInt(paddingRight);
				div.style.display = 'block';
				calc = false;
			});
		}
	});

	onMount(() => {
		const observer = new ResizeObserver((e) => {
			if (!calc) {
				calc = true;
			}
		});
		observer.observe(div.parentElement);
		observer.observe(window.document.body);
		return () => observer.disconnect();
	});

	// let updating = false;
	// async function updateHeight(e) {
	// 	if (div && !updating) {
	// 		updating = true;
	// 		for (const { height, prop, corner } of [
	// 			{ height: true, prop: 'Height', corner: 'top' },
	// 			{ height: false, prop: 'Width', corner: 'left' }
	// 		]) {
	// 			const rect = div.getBoundingClientRect();
	// 			let val = Math.floor(window[`inner${prop}`] - rect[corner]);
	// 			if (height) {
	// 				clientHeight = val;
	// 			} else {
	// 				clientWidth = val;
	// 			}
	// 			await tick();

	// 			let ancestor;
	// 			let lastVal;
	// 			do {
	// 				lastVal = val;
	// 				ancestor = div;
	// 				while (ancestor && ancestor[`scroll${prop}`] <= ancestor[`client${prop}`]) {
	// 					ancestor = ancestor.parentElement;
	// 				}

	// 				if (ancestor) {
	// 					const diff = Math.ceil(ancestor[`scroll${prop}`] - ancestor[`client${prop}`]);
	// 					const lastScroll = ancestor[`scroll${prop}`];
	// 					if (diff <= 0) {
	// 						break;
	// 					}
	// 					val = Math.max(200, val - diff);
	// 					if (height) {
	// 						clientHeight = val;
	// 					} else {
	// 						clientWidth = val;
	// 					}
	// 					await tick();
	// 					const scrollDiff = lastScroll - ancestor[`scroll${prop}`];
	// 					if (diff > scrollDiff) {
	// 						val = scrollDiff > 0 ? (val = Math.max(200, lastVal - scrollDiff)) : lastVal;
	// 						if (height) {
	// 							clientHeight = val;
	// 						} else {
	// 							clientWidth = val;
	// 						}
	// 						await tick();
	// 					}
	// 				}
	// 			} while (ancestor && lastVal != val);
	// 			updating = false;
	// 		}
	// 		// const rect = div.getBoundingClientRect();
	// 		// clientHeight = Math.floor(window.innerHeight - rect.top);
	// 		// clientWidth = Math.floor(window.innerWidth - rect.left);
	// 		// await tick();

	// 		// let ancestor;
	// 		// let lastHeight;
	// 		// do {
	// 		// 	lastHeight = clientHeight;
	// 		// 	ancestor = div;
	// 		// 	while (ancestor && ancestor.scrollHeight <= ancestor.clientHeight) {
	// 		// 		ancestor = ancestor.parentElement;
	// 		// 	}

	// 		// 	if (ancestor) {
	// 		// 		const diff = Math.ceil(ancestor.scrollHeight - ancestor.clientHeight);
	// 		// 		const lastScrollHeight = ancestor.scrollHeight;
	// 		// 		if (diff <= 0) {
	// 		// 			break;
	// 		// 		}
	// 		// 		clientHeight = Math.max(200, clientHeight - diff);
	// 		// 		await tick();
	// 		// 		const scrollDiff = lastScrollHeight - ancestor.scrollHeight;
	// 		// 		if (diff > scrollDiff) {
	// 		// 			clientHeight =
	// 		// 				scrollDiff > 0 ? (clientHeight = Math.max(200, lastHeight - scrollDiff)) : lastHeight;
	// 		// 			await tick();
	// 		// 		}
	// 		// 	}
	// 		// } while (ancestor && lastHeight != clientHeight);
	// 	}
	// }
	// let lastTop;
	// onMount(() => {
	// 	const poll = setInterval(() => {
	// 		updateHeight();
	// 		// const top = div.getBoundingClientRect().top;
	// 		// if (top != lastTop) {
	// 		// 	updateHeight();
	// 		// 	lastTop = top;
	// 		// }
	// 	}, 1000);
	// });
	// $effect(() => {
	// 	if (calc) {
	// 		div.style.display = null;
	// 		calc = false;
	// 	}
	// });
</script>

<div bind:this={div}>
	{@render children?.()}
</div>
