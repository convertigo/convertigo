<script>
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{buttons: {label: string, icon: string, cls?: string, disabled?: boolean?, onclick: () => void}[]}}*/
	let { buttons } = $props();
</script>

<div class="ml-auto max-w-sm">
	<div class="layout-grid-low-[100px]">
		{#each buttons as { label, icon, cls, disabled, onclick }}
			<button {onclick} {disabled} class="{cls} text-wrap min-h-fit w-full"
				><span><Ico {icon} size="btn" /></span><span>{label}</span></button
			>
		{/each}
	</div>
</div>
