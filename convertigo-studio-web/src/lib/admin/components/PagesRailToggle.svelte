<script>
	import { getDrawerStore } from '@skeletonlabs/skeleton';
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{open?: boolean}} */
	let { open = true } = $props();

	const drawerStore = getDrawerStore();
	function togglePagesDrawer() {
		if (open) {
			drawerStore.open({
				id: 'pagesRail',
				position: 'left',
				bgDrawer: 'dark:bg-surface-800 bg-white',
				bgBackdrop: 'bg-black/50',
				width: 'w-auto'
			});
		} else {
			drawerStore.close();
		}
	}
</script>

<button class="{open ? 'md:hidden' : ''} ms:pl-2 sx:pt-2" onclick={togglePagesDrawer}
	><Ico icon="iconamoon:menu-burger-horizontal-thin" class="w-8 h-8" /></button
>
