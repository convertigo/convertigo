import { writable } from 'svelte/store';
import { selectedId } from '$lib/treeview/treeStore';
import { call } from '$lib/utils/service';

export const categories = writable([]);
export const reusables = writable([]);
export const draggedItem = writable();

selectedId.subscribe(async (id) => {
	if (id == '') {
		return;
	}
	try {
		let paletteData = await call('studio.palette.Get', { id });
		categories.set(paletteData.categories);
	} catch (e) {}
});
