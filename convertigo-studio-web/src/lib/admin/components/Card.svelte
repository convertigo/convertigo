<script>
	import Button from '$lib/admin/components/Button.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';

	/**
	 * @type {{
	 * 	title?: string,
	 * 	class?: string,
	 *  bg?: string,
	 * 	cardStyle?: string,
	 * 	cardBorder?: string,
	 * 	docHref?: string,
	 * 	docLabel?: string,
	 * 	cornerOption?: import('svelte').Snippet,
	 * 	children?: import('svelte').Snippet
	 * }|any}
	 */
	let {
		title = '',
		class: cls = '',
		bg = 'bg-surface-100-900',
		cornerOptionClass = '',
		docHref = '',
		docLabel = 'Open documentation',
		cornerOption,
		children,
		...rest
	} = $props();
	let hasDocHref = $derived(String(docHref ?? '').trim().length > 0);
</script>

<div class="layout-y-p-stretch surface-card-shell {bg} {cls}" {...rest}>
	{#if title == null || title?.length > 0 || hasDocHref || cornerOption}
		<div class="layout-x-wrap w-full items-center gap-3">
			{#if title == null}
				<AutoPlaceholder class="max-w-48" loading={true} />
			{/if}
			{#if title?.length > 0}
				<span class="text-lg font-medium">{title}</span>
			{/if}
			{#if hasDocHref}
				<Button
					full={false}
					href={docHref}
					target="_blank"
					rel="noopener noreferrer"
					icon="mdi:file-question-outline"
					title={docLabel}
					ariaLabel={docLabel}
					class="button-ico-primary h-auto! min-h-0! w-8 min-w-8 justify-center p-0!"
				/>
			{/if}
			{#if cornerOption}
				<div class="ml-auto flex items-center justify-end gap-2 {cornerOptionClass}">
					{@render cornerOption()}
				</div>
			{/if}
		</div>
	{/if}
	{@render children?.()}
</div>
