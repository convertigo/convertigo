<script>
	import Button from './Button.svelte';

	/** @type {{ buttons: {label?: string, icon?: string, cls?: string, disabled?: boolean, value?: string, hidden?: boolean, href?: string, onclick?: () => void}[]|any[], size?: string, class?: string, disabled?: boolean }} */
	let { buttons, size = 'btn', class: cls = 'max-w-md', disabled = false } = $props();
	let onlyIcons = buttons.every(({ label }) => !label);
</script>

<div class="ml-auto {cls}">
	<fieldset class:layout-grid-low-5={onlyIcons} class:layout-grid-low-24={!onlyIcons} {disabled}>
		{#each buttons as button}
			<Button {...button} {size} icon={button.icon} />
		{/each}
	</fieldset>
</div>
