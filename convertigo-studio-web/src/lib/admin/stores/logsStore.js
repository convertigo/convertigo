import { call, checkArray } from '$lib/utils/service';
import { writable } from 'svelte/store';

export let logs = writable([]);
let rowHeights = [];
let rowData = [];
let moreResults = false;

export async function logsList() {
	const res = await call('logs.Get', { moreResults });
	moreResults = res?.hasMoreResults;
	//@ts-ignore
	logs.update((l) => [...l, ...checkArray(res?.lines)]);
	//rowHeights = rowData.map(() => Math.random() * (155 - 50) + 50); // Update heights for new data
}
