<script>
	import { debounce } from '$lib/utils/service';
	import { onMount, tick } from 'svelte';
	import { fromAction } from 'svelte/attachments';
	import { Tween } from 'svelte/motion';

	/** @type {{height: any, children?: import('svelte').Snippet}} */
	let { height = $bindable(), children } = $props();

	const tHeight = new Tween(height, {
		duration: 100,
		easing: (t) => t
	});

	$effect(() => {
		height = tHeight.current;
	});

	let element = $state();
	const attachElement = $derived(fromAction(bindElement));

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
					const lastScrollHeight = ancestor.scrollHeight;
					if (diff <= 0) {
						break;
					}
					height = Math.max(200, height - diff);
					await tick();
					const scrollDiff = lastScrollHeight - ancestor.scrollHeight;
					if (diff > scrollDiff) {
						height =
							scrollDiff > 0 ? (height = Math.max(200, lastHeight - scrollDiff)) : lastHeight;
						await tick();
					}
				}
			} while (ancestor && lastHeight != height);
			tHeight.target = height;
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

	/** @param {HTMLDivElement} node */
	function bindElement(node) {
		element = node;
		return {
			destroy() {
				if (element === node) {
					element = undefined;
				}
			}
		};
	}
</script>

<div class="min-w-full" {@attach attachElement}>
	{@render children?.()}
</div>
