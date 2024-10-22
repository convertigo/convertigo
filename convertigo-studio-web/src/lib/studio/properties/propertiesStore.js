import { writable } from 'svelte/store';
import { selectedId } from '$lib/studio/treeview/treeStore';
import { call } from '$lib/utils/service';

export const ionProp = {
	category: '@Properties',
	description: '',
	editor: '',
	kind: 'ion',
	label: 'IonBean',
	mode: 'plain',
	name: 'IonBean',
	type: 'string',
	value: false,
	values: [false, true]
};

export const dboProp = {
	category: 'Base properties',
	displayName: 'DatabaseObject',
	editorClass: '',
	isDisabled: false,
	isExpert: false,
	isHidden: false,
	isMasked: false,
	isMultiline: true,
	name: 'DatabaseObject',
	shortDescription: '',
	value: 'Please select a field',
	class: 'java.lang.String',
	kind: 'dbo',
	label: 'DatabaseObject',
	values: []
};

export const properties = writable({
	init: dboProp
});

selectedId.subscribe(async (id) => {
	if (id == '') {
		return;
	}
	try {
		let treeData = await call('studio.properties.Get', { id });
		properties.set(treeData.properties);
	} catch (e) {
		console.log(e);
	}
});

/**
 * @param {string} id - the id of the target dbo in tree
 * @param {string} prop - the dbo property as json string (name, mode, value)
 */
export async function setDboProp(id = '', prop = '') {
	let result = await call('studio.properties.Set', { id, prop });
	return result;
}
