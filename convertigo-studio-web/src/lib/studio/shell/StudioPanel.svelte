<script>
	import Card from '$lib/admin/components/Card.svelte';
	import Ico from '$lib/utils/Ico.svelte';

	/**
	 * @type {{
	 *  title: string;
	 *  icon?: string;
	 *  class?: string;
	 *  contentClass?: string;
	 *  scroll?: boolean;
	 *  actions?: import('svelte').Snippet;
	 *  children?: import('svelte').Snippet;
	 * }}
	 */
	let {
		title,
		icon,
		class: cls = '',
		contentClass = 'p-low',
		scroll = true,
		actions,
		children
	} = $props();

	const contentOverflow = $derived(scroll ? 'overflow-auto' : 'overflow-hidden');
</script>

<Card bg="preset-filled-surface-50-950" class={`h-full min-h-0 gap-none! p-none! ${cls}`}>
	<div class="layout-x-between-none items-center border-b-[0.5px] border-color py-low px-low">
		<div class="layout-x-low items-center">
			{#if icon}
				<Ico {icon} size={5} />
			{/if}
			<span class="text-sm font-medium">{title}</span>
		</div>
		{#if actions}
			<div class="layout-x-low items-center">{@render actions?.()}</div>
		{/if}
	</div>

	<div class={`min-h-0 grow ${contentOverflow} ${contentClass}`}>
		{@render children?.()}
	</div>
</Card>
