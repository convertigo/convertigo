<script lang="ts">
	export let flex = false;
	export let flexCol = false;
	export let grid = false;
	export let cols = 1;
	export let gap = '0';
	let cls = '';
	export { cls as class };
</script>

<div
	class={`${
		flex ? (flexCol ? 'flex flex-col' : 'flex') : grid ? `grid grid-cols-${cols}` : ''
	} gap-${gap} ${cls}`}
>
	<slot />
</div>
