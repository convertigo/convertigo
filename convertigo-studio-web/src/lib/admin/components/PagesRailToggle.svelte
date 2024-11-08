<script>
	import Ico from '$lib/utils/Ico.svelte';

	let { state = $bindable(), class: cls = '' } = $props();
</script>

<button class={cls} onclick={() => (state = !state)}
	><Ico icon="iconamoon:menu-burger-horizontal-thin" size={8} /></button
>
