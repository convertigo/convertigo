<script>
	export let customStyle = '';
</script>

<div
	style={customStyle}
	class="flex flex-col bg-surface-800 h-auto p-4 font-extralight text-[13.5px] border-[0.5px] border-surface-600 rounded-none"
>
	<slot />
</div>
