import { call, checkArray } from '$lib/utils/service';
import { writable } from 'svelte/store';

export let logs = writable([]);
export let startDate = writable('');
export let endDate = writable('');
export let realtime = writable(false);
export let nbLines = writable(100);

let _startDate, _endDate, _realtime, _nbLines;
startDate.subscribe((value) => (_startDate = value));
endDate.subscribe((value) => (_endDate = value));
realtime.subscribe((value) => (_realtime = value));
nbLines.subscribe((value) => (_nbLines = value));

let moreResults = false;

export function formatDate(timestamp) {
	if (timestamp === null) return '';
	return new Date(timestamp).toISOString().split('T')[0];
}

export function formatTime(timestamp) {
	if (timestamp === null) return '';
	return new Date(timestamp).toISOString().split('T')[1].split('Z')[0].replace('.', ',');
}

export async function logsList(clear = false) {
	if (clear) {
		logs.set([]);
		moreResults = false;
	}
	const res = await call('logs.Get', {
		moreResults,
		startDate: _startDate,
		endDate: _endDate,
		realtime: _realtime,
		nbLines: _nbLines
	});
	moreResults = res?.hasMoreResults;
	//@ts-ignore
	logs.update((l) => [...l, ...checkArray(res?.lines)]);
}
