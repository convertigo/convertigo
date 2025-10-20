<script>
	import { Accordion } from '@skeletonlabs/skeleton-svelte';

	/** @type {{
		children?: import('svelte').Snippet;
		class?: string;
		multiple?: boolean;
		value?: string[];
	} & Record<string, any>} */
	let {
		multiple = false,
		value = $bindable([]),
		class: cls = '',
		children,
		onValueChange,
		...rest
	} = $props();

	const content = children ?? (() => null);
	const groupClasses = [cls].filter(Boolean).join(' ');

	function handleValueChange(details) {
		value = details?.value ?? [];
		onValueChange?.({ ...details });
	}
</script>

<Accordion {multiple} {value} class={groupClasses} onValueChange={handleValueChange} {...rest}>
	{@render content()}
</Accordion>
