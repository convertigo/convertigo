import { call, checkArray } from '$lib/utils/service';
import { writable } from 'svelte/store';

export let logs = writable([]);
let rowHeights = [];
let rowData = [];
export async function logsList() {
	const res = await call('logs.Get');
	//@ts-ignore
	logs.set(checkArray(res?.lines));
	rowHeights = rowData.map(() => Math.random() * (155 - 50) + 50); // Update heights for new data
}
