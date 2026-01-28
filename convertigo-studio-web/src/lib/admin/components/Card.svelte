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

<div
	class="layout-y-p-stretch {bg} rounded-container border border-color shadow-follow {cls}"
	{...rest}
>
	{#if title == null || title?.length > 0 || cornerOption}
		<div class="layout-x-wrap w-full items-center gap-3">
			{#if title == null}
				<AutoPlaceholder class="max-w-48" loading={true} />
			{/if}
			{#if title?.length > 0}
				<span class="text-lg font-medium">{title}</span>
			{/if}
			{#if cornerOption}
				<div class="ml-auto flex items-center justify-end">
					{@render cornerOption()}
				</div>
			{/if}
		</div>
	{/if}
	{@render children?.()}
</div>
