<script>
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{ buttons: {label: string, icon?: string, cls?: string, disabled?: boolean, value?: string, onclick: () => void}[], class?: string }} */
	let { buttons, class: cls = 'max-w-sm' } = $props();
</script>

<div class="ml-auto {cls}">
	<div class="layout-grid-low-[100px]">
		{#each buttons as { label, icon, cls, disabled, value, onclick }}
			<button {onclick} {disabled} class="{cls} text-wrap min-h-fit w-full" {value}>
				{#if icon}<span><Ico {icon} size="btn" /></span>{/if}<span>{label}</span></button
			>
		{/each}
	</div>
</div>
