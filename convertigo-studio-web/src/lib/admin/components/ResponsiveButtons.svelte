<script>
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{ buttons: {label?: string, icon?: string, cls?: string, disabled?: boolean, value?: string, href?: string, onclick?: () => void}[], size?: string, class?: string }} */
	let { buttons, size = 'btn', class: cls = 'max-w-sm' } = $props();
	let onlyIcons = buttons.every(({ label }) => !label);
</script>

<div class="ml-auto {cls}">
	<div class:layout-grid-low-5={onlyIcons} class:layout-grid-low-24={!onlyIcons}>
		{#each buttons as { label, icon, cls, disabled, value, href, onclick }}
			{#if href}
				<a {href} class="{cls} text-wrap min-h-fit w-full h-full">
					{#if icon}<span><Ico {icon} {size} /></span>{/if}{#if label}<span>{label}</span>{/if}</a
				>
			{:else}
				<button {onclick} {disabled} class="{cls} text-wrap min-h-fit w-full h-full" {value}>
					{#if icon}<span><Ico {icon} {size} /></span>{/if}{#if label}<span>{label}</span
						>{/if}</button
				>
			{/if}
		{/each}
	</div>
</div>
