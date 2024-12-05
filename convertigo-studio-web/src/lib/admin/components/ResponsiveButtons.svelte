<script>
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{ buttons: {label?: string, icon?: string, cls?: string, disabled?: boolean, value?: string, hidden?: boolean, href?: string, onclick?: () => void}[], size?: string, class?: string, disabled?: boolean }} */
	let { buttons, size = 'btn', class: cls = 'max-w-md', disabled = false } = $props();
	let onlyIcons = buttons.every(({ label }) => !label);
</script>

<div class="ml-auto {cls}">
	<fieldset class:layout-grid-low-5={onlyIcons} class:layout-grid-low-24={!onlyIcons} {disabled}>
		{#each buttons as { label, icon, cls, disabled, value, hidden, href, onclick }}
			{#if !hidden}
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
			{/if}
		{/each}
	</fieldset>
</div>
