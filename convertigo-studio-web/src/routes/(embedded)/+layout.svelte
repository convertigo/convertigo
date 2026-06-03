<script>
	import { browser } from '$app/environment';

	/** @type {{children?: import('svelte').Snippet}} */
	let { children } = $props();

	$effect(() => {
		if (!browser) {
			return;
		}
		const { body, documentElement } = document;
		const previousBodyOverflow = body.style.overflow;
		const previousHtmlOverflow = documentElement.style.overflow;
		body.style.overflow = 'hidden';
		documentElement.style.overflow = 'hidden';
		return () => {
			body.style.overflow = previousBodyOverflow;
			documentElement.style.overflow = previousHtmlOverflow;
		};
	});
</script>

<main class="h-screen min-h-0 w-full overflow-hidden">
	{@render children?.()}
</main>
