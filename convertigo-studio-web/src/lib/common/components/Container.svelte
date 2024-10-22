<script lang="ts">
	interface Props {
		flex?: boolean;
		flexCol?: boolean;
		grid?: boolean;
		cols?: number;
		gap?: string;
		class?: string;
		children?: import('svelte').Snippet;
	}

	let {
		flex = false,
		flexCol = false,
		grid = false,
		cols = 1,
		gap = '0',
		class: cls = '',
		children
	}: Props = $props();
</script>

<div
	class={`${
		flex ? (flexCol ? 'flex flex-col' : 'flex') : grid ? `grid grid-cols-${cols}` : ''
	} gap-${gap} ${cls}`}
>
	{@render children?.()}
</div>
