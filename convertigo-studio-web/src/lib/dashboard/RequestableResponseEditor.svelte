<script>
	import Button from '$lib/admin/components/Button.svelte';
	import Editor from '$lib/studio/editor/Editor.svelte';
	import { tick } from 'svelte';
	import { fromAction } from 'svelte/attachments';

	let { content, language = 'json', theme = 'vs-dark', loading = false } = $props();

	const compactMaxHeight = 480;
	const minEditorHeight = 48;
	const topOffset = 72;

	let fitViewport = $state(false);
	let viewportHeight = $state(compactMaxHeight + topOffset);
	let contentHeight = $state(compactMaxHeight);
	let shell = $state();
	let naturalHeight = $derived(Math.max(minEditorHeight, Math.ceil(contentHeight) + 2));
	let compactHeight = $derived(Math.min(compactMaxHeight, naturalHeight));
	let viewportMaxHeight = $derived(Math.max(minEditorHeight, viewportHeight - topOffset));
	let canFitViewport = $derived(naturalHeight > compactHeight && viewportMaxHeight > compactHeight);
	let editorHeight = $derived(
		fitViewport && canFitViewport ? Math.min(naturalHeight, viewportMaxHeight) : compactHeight
	);
	let toggleLabel = $derived(fitViewport ? 'Use compact result height' : 'Fit result to viewport');
	const attachShell = $derived(fromAction(registerShell));

	async function toggleFitViewport() {
		fitViewport = !fitViewport;
		if (!fitViewport || !shell) return;

		await tick();
		window.scrollTo({
			top: window.scrollY + shell.getBoundingClientRect().top - topOffset,
			behavior: 'smooth'
		});
	}

	/** @param {HTMLDivElement} node */
	function registerShell(node) {
		shell = node;
		return {
			destroy() {
				if (shell === node) {
					shell = undefined;
				}
			}
		};
	}
</script>

<svelte:window bind:innerHeight={viewportHeight} />

<div
	class="group relative overflow-hidden rounded-lg border border-surface-200-800/60 bg-surface-50-950 shadow-inner transition-[height] duration-150 ease-out"
	class:animate-pulse={loading}
	style:height={`${editorHeight}px`}
	{@attach attachShell}
>
	{#if canFitViewport}
		<Button
			full={false}
			icon={fitViewport ? 'mdi:minus' : 'mdi:fullscreen'}
			title={toggleLabel}
			ariaLabel={toggleLabel}
			class="absolute top-2 right-2 z-20 button-primary h-8! min-h-0! w-8 min-w-8 justify-center p-0! opacity-75 shadow-md transition-opacity hover:opacity-100 focus:opacity-100"
			onclick={toggleFitViewport}
		/>
	{/if}
	<Editor {content} {language} {theme} bind:contentHeight scrollBeyondLastLine={false} />
</div>
