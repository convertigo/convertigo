<script>
	let cls = '';
	export { cls as class };
</script>

<div class={`flex flex-wrap gap-3 ${cls}`}>
	<slot />
</div>
