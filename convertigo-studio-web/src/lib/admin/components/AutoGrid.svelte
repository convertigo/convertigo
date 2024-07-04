<script>
	let cls = '';
	export { cls as class };
</script>

<div class="flex flex-col grid md:grid-cols-2 grid-cols-1 gap-5 {cls}">
	<slot />
</div>
