import { writable } from 'svelte/store';

export const loading = writable(0);
export const authenticated = writable(false);
