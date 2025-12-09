<script>
	/** @type {{
		layout?: string;
		justify?: string;
		full?: boolean;
		wrap?: boolean;
		class?: string;
		disabled?: boolean;
		children?: import('svelte').Snippet;
	}} */
	let {
		layout = 'layout-x-low',
		justify = 'end',
		full = true,
		wrap = true,
		class: cls = '',
		disabled,
		children
	} = $props();

	let layoutClass = $derived(
		wrap && layout.startsWith('layout-x') ? layout.replace('layout-x', 'layout-x-wrap') : layout
	);
	let classes = $derived(
		[layoutClass, full ? 'w-full' : '', justify ? `justify-${justify}` : '', cls]
			.filter(Boolean)
			.join(' ')
	);
</script>

<fieldset class={classes} {disabled}>
	{@render children?.()}
</fieldset>
