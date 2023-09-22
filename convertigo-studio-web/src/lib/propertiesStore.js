import { writable } from 'svelte/store';

export const properties = writable({
	init: 'Please select a field'
});
