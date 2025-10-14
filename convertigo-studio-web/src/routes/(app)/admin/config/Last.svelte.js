import { persistedState } from 'svelte-persisted-state';

let categoryState = persistedState('admin.config.category', 'Main', { syncTabs: false });
let advancedState = persistedState('admin.config.advanced', false, { syncTabs: false });

export default {
	get category() {
		const next = categoryState.current ?? 'Main';
		if (categoryState.current !== next) {
			categoryState.current = next;
		}
		return next;
	},
	set category(value) {
		categoryState.current = value ?? 'Main';
	},
	get advanced() {
		return advancedState.current;
	},
	set advanced(value) {
		advancedState.current = value;
	}
};
