<script>
	import { ico } from '$lib/utils/Ico.svelte';
	import Ico from '$lib/utils/Ico.svelte';
</script>

<div class="flex flex-wrap gap-2 p-2 h-full overflow-y-auto">
	{#each Object.keys(ico) as icon}
		<span class="card p-2"><Ico {icon} size="28" />&nbsp; {icon}</span>
	{/each}
</div>
