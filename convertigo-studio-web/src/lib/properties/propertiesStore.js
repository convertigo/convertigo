import { writable } from 'svelte/store';
import { selectedId } from '$lib/treeview/treeStore';
import { call } from '$lib/utils/service';

export const properties = writable({
	init: 'Please select a field'
});

selectedId.subscribe(async (id) => {
	if (id == '') {
		return;
	}
	try {
		let treeData = await call('studio.properties.Get', { id });
		properties.set(treeData.properties);
	} catch (e) {}
});
