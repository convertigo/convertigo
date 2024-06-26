<script>
	import { debounce } from '$lib/utils/service';
	import { onMount, tick } from 'svelte';
	import { tweened } from 'svelte/motion';

	export let height;

	const tHeight = tweened(height, {
		duration: 100,
		easing: (t) => t
	});

	tHeight.subscribe((value) => {
		height = value;
	});

	let element;

	async function updateHeight(e) {
		if (element) {
			const rect = element.getBoundingClientRect();
			height = Math.floor(window.innerHeight - rect.top);

			await tick();

			let ancestor;
			let lastHeight;
			do {
				lastHeight = height;
				ancestor = element;
				while (ancestor && ancestor.scrollHeight <= ancestor.clientHeight) {
					ancestor = ancestor.parentElement;
				}

				if (ancestor) {
					const diff = Math.ceil(ancestor.scrollHeight - ancestor.clientHeight);
					if (diff <= 0) {
						break;
					}
					height = Math.max(200, height - diff);
					await tick();
				}
			} while (ancestor && lastHeight != height);
			tHeight.set(height);
		}
	}

	const debouncedUpdateHeight = debounce(updateHeight, 100);

	onMount(() => {
		debouncedUpdateHeight();

		let lastTop = element.getBoundingClientRect().top;
		const poll = setInterval(() => {
			const top = element.getBoundingClientRect().top;
			if (top != lastTop) {
				debouncedUpdateHeight();
				lastTop = top;
			}
		}, 250);

		window.addEventListener('resize', debouncedUpdateHeight);

		return () => {
			window.removeEventListener('resize', debouncedUpdateHeight);
			clearInterval(poll);
		};
	});
</script>

<div class="min-w-full" bind:this={element}>
	<slot></slot>
</div>
