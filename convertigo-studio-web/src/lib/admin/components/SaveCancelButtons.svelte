<script>
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';

	/** @type {{
		onSave?: (event?: any) => any,
		onCancel?: (event?: any) => any,
		changesPending?: boolean,
		disabled?: boolean,
		saveLabel?: string,
		cancelLabel?: string,
		saveIcon?: string,
		cancelIcon?: string,
		saveClass?: string,
		cancelClass?: string,
		class?: string
	}} */
	let {
		onSave = () => {},
		onCancel = () => {},
		changesPending = true,
		disabled = false,
		saveLabel = 'Save changes',
		cancelLabel = 'Cancel changes',
		saveIcon = 'mdi:content-save-edit-outline',
		cancelIcon = 'mdi:close-circle-outline',
		saveClass = 'button-primary',
		cancelClass = 'button-secondary',
		class: cls = ''
	} = $props();

	const handleSave = (event) => {
		onSave?.(event);
	};

	const handleCancel = (event) => {
		onCancel?.(event);
	};
</script>

<ResponsiveButtons
	class={cls}
	{disabled}
	buttons={[
		{
			label: saveLabel,
			icon: saveIcon,
			cls: saveClass,
			disabled: !changesPending,
			onclick: handleSave
		},
		{
			label: cancelLabel,
			icon: cancelIcon,
			cls: cancelClass,
			disabled: !changesPending,
			onclick: handleCancel
		}
	]}
/>
