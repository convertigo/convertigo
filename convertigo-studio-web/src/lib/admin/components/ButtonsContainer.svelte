<script>
	let cls = '';
	export { cls as class };
</script>

<div class={`flex flex-wrap gap-3 justify-end mb-3 ${cls}`}>
	<slot />
</div>
