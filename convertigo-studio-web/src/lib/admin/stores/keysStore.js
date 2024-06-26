import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';

export let categoryStore = writable([]);

export async function keysCheck() {
	const response = await call('keys.List');

	if (response?.admin?.category) {
		let categories = response.admin.category;

		if (!Array.isArray(categories)) {
			categories = [categories];
		}

		const categoriesWithKeys = categories.map((category) => {
			let keys = category.keys.key;
			if (!Array.isArray(keys)) {
				keys = [keys];
			}
			return { ...category, keys };
		});

		categoryStore.set(categoriesWithKeys);
	}
}
