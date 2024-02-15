<script>
	export let customStyle = '';
	export let title = '';
	let cls = '';
	export { cls as class };
</script>

<div
	style={customStyle}
	class="flex flex-col dark:bg-surface-800 bg-white h-auto p-4 font-extralight text-[13.5px] border-[0.5px] dark:border-surface-500 border-surface-50 rounded-xl {cls}"
>
	{#if title.length > 0}
		<h1 class="text-xl mb-5 font-bold">{title}</h1>
	{/if}
	<slot />
</div>
