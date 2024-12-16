<script>
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';

	/**
	 * @type {{
	 * 	title?: string,
	 * 	class?: string,
	 *  bg?: string,
	 * 	cardStyle?: string,
	 * 	cardBorder?: string,
	 * 	cornerOption?: import('svelte').Snippet,
	 * 	children?: import('svelte').Snippet
	 * }|any}
	 */
	let {
		title = '',
		class: cls = '',
		bg = 'bg-surface-100-900',
		cornerOption,
		children,
		...rest
	} = $props();
</script>

<div class="layout-y-p-stretch {bg} border-[0.5px] border-color rounded-container {cls}" {...rest}>
	{#if title == null || title?.length > 0 || cornerOption}
		<div class="layout-x flex-wrap w-full">
			{#if title == null}
				<AutoPlaceholder class="max-w-48" loading={true} />
			{/if}
			{#if title?.length > 0}
				<span class="text-xl font-bold">{title}</span>
			{/if}
			{#if cornerOption}
				<div class="grow">
					{@render cornerOption()}
				</div>
			{/if}
		</div>
	{/if}
	{@render children?.()}
</div>
