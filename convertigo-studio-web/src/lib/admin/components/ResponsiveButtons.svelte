<script>
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{ buttons: {label?: string, icon?: string, cls?: string, disabled?: boolean, value?: string, onclick: () => void}[], class?: string }} */
	let { buttons, class: cls = 'max-w-sm' } = $props();
	let onlyIcons = buttons.every(({ label }) => !label);
</script>

<div class="ml-auto {cls}">
	<div class:layout-grid-low-5={onlyIcons} class:layout-grid-low-24={!onlyIcons}>
		{#each buttons as { label, icon, cls, disabled, value, onclick }}
			<button {onclick} {disabled} class="{cls} text-wrap min-h-fit w-full h-full" {value}>
				{#if icon}<span><Ico {icon} size="btn" /></span>{/if}{#if label}<span>{label}</span
					>{/if}</button
			>
		{/each}
	</div>
</div>
