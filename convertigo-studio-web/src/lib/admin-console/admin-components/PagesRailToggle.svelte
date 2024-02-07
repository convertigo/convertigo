<script>
	import { getDrawerStore } from '@skeletonlabs/skeleton';
	import Icon from '@iconify/svelte';

	export let open = true;

	const drawerStore = getDrawerStore();
	function togglePagesDrawer() {
		if (open) {
			drawerStore.open({
				id: 'pagesRail',
				position: 'left',
				bgDrawer: 'bg-surface-800',
				bgBackdrop: 'bg-black/50',
				width: 'w-auto'
			});
		} else {
			drawerStore.close();
		}
	}
</script>

<button class="{open ? 'md:hidden' : ''} pl-2 pt-2" on:click={togglePagesDrawer}
	><Icon icon="iconamoon:menu-burger-horizontal-thin" class="w-8 h-8" /></button
>
