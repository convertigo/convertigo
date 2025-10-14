import { persistedState } from 'svelte-persisted-state';

const tabStore = persistedState('admin.logs.tab', 'view', { syncTabs: false });

export default {
	get tab() {
		return tabStore.current ?? 'view';
	},
	set tab(value) {
		tabStore.current = value ?? 'view';
	},
	store: tabStore
};
