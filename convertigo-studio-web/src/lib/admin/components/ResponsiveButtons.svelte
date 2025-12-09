<script>
	import ActionBar from './ActionBar.svelte';
	import Button from './Button.svelte';

	/** @type {{ buttons: {label?: string, icon?: string, cls?: string, disabled?: boolean, value?: string, hidden?: boolean, href?: string, onclick?: () => void}[]|any[], size?: string, class?: string, disabled?: boolean }} */
	let { buttons, size = 'btn', class: cls = 'max-w-md', disabled = false } = $props();
	let layout = $derived(buttons.every(({ label }) => !label) ? 'layout-grid-low-5' : undefined);
</script>

<div class="ml-auto {cls}">
	<ActionBar {layout}>
		{#each buttons as button}
			<Button {disabled} {...button} {size} icon={button.icon} full={false} />
		{/each}
	</ActionBar>
</div>
