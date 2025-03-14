import { selectedId } from '$lib/studio/treeview/treeStore';
import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';

export const categories = writable([]);
export const reusables = writable([]);

selectedId.subscribe(async (id) => {
	if (id == '') {
		return;
	}
	try {
		let paletteData = await call('studio.palette.Get', { id });
		categories.set(paletteData.categories);
	} catch (e) {}
});
